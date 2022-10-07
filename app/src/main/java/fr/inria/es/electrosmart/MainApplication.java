/*
 * BSD 3-Clause License
 *
 *       Copyright (c) 2014-2022, Arnaud Legout (arnaudlegout), centre Inria de
 *       l'Université Côte d'Azur, France. Contact: arnaud.legout@inria.fr
 *       All rights reserved.
 *
 *       Redistribution and use in source and binary forms, with or without
 *       modification, are permitted provided that the following conditions are met:
 *
 *       1. Redistributions of source code must retain the above copyright notice, this
 *       list of conditions and the following disclaimer.
 *
 *       2. Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 *       3. Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 *       THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *       AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *       IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *       DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *       FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *       DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *       SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *       CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *       OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *       OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fr.inria.es.electrosmart;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;

import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DbHelper;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.monitors.DeviceInfoMonitor;
import fr.inria.es.electrosmart.monitors.IdleStateBroadcastReceiver;
import fr.inria.es.electrosmart.monitors.OnBootBroadcastReceiver;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;
import fr.inria.es.electrosmart.serversync.SyncUtils;

public class MainApplication extends Application {
    private static final String TAG = "MainApplication";
    public static DbHelper dbHelper;
    private static Context context;
    private static ResultReceiver resultReceiver;
    // If the application crashed only once, we run the UncaughtExceptionHandler, any subsequent
    // crash will exit the app to avoid any loop (which is possible if crashes happen in the handler
    // itself).
    private static boolean hasAlreadyCrashed;

    // This flag should be set to true to enable VectorDrawable support for API < 21
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    /*
    The UncaughtExceptionHandler will get all uncaught exceptions.
    We use this handler to revert the BT and Wifi cards to their initial state.
    WARNING: enabling a card is an asynchronous process. So the state of the card will not change
    immediately after enabling it. So, in case the crash is right after the enabling,
    the state of the card will not be correctly reverted back. We can improve the situation
    by implementing listeners for the cards state change, but this is some effort
    for a corner case.

    The rational of this exception handling is the following.
    When set in onCreate()
    defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    which means that we save the default handler for later use.
    Then we set
    Thread.setDefaultUncaughtExceptionHandler(handler);
    to change the handler of when there is an uncaught exception.

    The exception handler defined below has the following logic.
    If there is an uncaught exception we try to revert all cards and stop the service. The goal
    is to revert back the states and to prevent any exception loop due to the service. Finally
    we rethrow the uncaught exception with the defaultUncaughtExceptionHandler.

    In case, the above logic generates a crash by itself (that is reverting back the cards or
    stopping the service) we enter the else clause at the second crash in which we only rethrow
    the exception. The rational is to avoid to enter an exception loop due to a bug in
    reverting back the cards or stopping the service. This is the most catastrophic and unlikely
    case, but we handle it anyway because an exception loop is the worst that can happen.
    */
    private Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    private Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread thread, Throwable ex) {
            if (!hasAlreadyCrashed) {
                hasAlreadyCrashed = true;
                Log.e(TAG, "Uncaught exception ! ");
                Log.e(TAG, "Reverting BT and Wifi card states");
                MeasurementScheduler.restoreInitialCardStateAndStopBroadcastReceivers(false);

                // In case of uncaught exception, we stop the service to do not loop into crashes if
                // ever the crash is in the service.
                MeasurementScheduler.stopMeasurementScheduler();

                // We also cancel sync alarm and unregister WiFiStateChangedBroadcastListener
                SyncUtils.cancelSyncAlarmAndUnregisterBroadcastReceiver();
                defaultUncaughtExceptionHandler.uncaughtException(thread, ex);
            } else {
                Log.d(TAG, "More than one crash, we direct the exception to the default handler and stop here.");
                defaultUncaughtExceptionHandler.uncaughtException(thread, ex);
            }
        }
    };

    public static Context getContext() {
        return MainApplication.context;
    }

    public static void setReceiver(ResultReceiver rReceiver) {
        resultReceiver = rReceiver;
    }

    public static ResultReceiver getResultReceiver() {
        return resultReceiver;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "in onCreate()");

        /*
        DEBUGGING CODE TO DETECT UI BLOCKING OPERATION

        Enables the strictMode. In this mode, the application report in logcat any blocking
        operation that slowdown the UI. This must only be used for debugging purposes.
        See https://developer.android.com/reference/android/os/StrictMode.html
         */
        if (!Const.IS_RELEASE_BUILD) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        hasAlreadyCrashed = false;

        // 1) Handle all uncaught exceptions (to revert back BT and Wifi cards)
        // Redirect all uncaught exceptions to a specific UncaughtExceptionHandler that revert
        // the BT and Wifi card to their initial state in case of crash.
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);

        createNotificationChannels();

        /*
        2) Register the broadcast receiver to receive Doze states and ACTION_POWER_CONNECTED,
        ACTION_POWER_DISCONNECTED events. Note that starting with Android O, we cannot
        anymore register these actions statically in the manifest (they have no effect).

        The main reason is that if you register ACTION_POWER_CONNECTED statically in the manifest,
        and you kill the application process, the intent ACTION_POWER_CONNECTED will recreate the
        process each time the intent is received. This might be a battery and CPU hog in case
        it is not an intended behavior.

        With the intent dynamically registered in the main application, if the application process
        is killed, then the intent will not be received (and will not restart the app).

        In our context, it is not an issue as restarting the main process (MainApplication) will not
        start the MeasurementScheduler.
        */
        boolean isIntent = false;
        IntentFilter filter = new IntentFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
            isIntent = true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            isIntent = true;
        }
        if (isIntent) {
            IdleStateBroadcastReceiver receiver = new IdleStateBroadcastReceiver();
            registerReceiver(receiver, filter);
        }

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        OnBootBroadcastReceiver onBootBroadcastReceiver = new OnBootBroadcastReceiver();
        registerReceiver(onBootBroadcastReceiver, filter);

        // 3) Dump logcat in a file
        /*
        this code enables to dump of logcat to a file in order to make offline collection
        the logs can be retrieved with the script pushLog.py that contains
        import os
        os.system('adb shell run-as fr.inria.es.electrosmart cp /storage/emulated/0/Android/data/fr.inria.es.electrosmart/files/esLogCat.txt /sdcard/.')
        os.system('adb.exe pull /sdcard/esLogCat.txt C:\Temp\.')
        */
        if (Const.DO_DUMP_LOGCAT_ON_FILE && !Const.IS_RELEASE_BUILD) {
            File rootLogFile;
            /*
            getExternalFilesDir() returns a path on /sdcard that can be accessed without any special
            permissions.
             */
            rootLogFile = this.getExternalFilesDir(null);
            if (rootLogFile != null) {
                Log.d(TAG, "rootLogFile: " + rootLogFile);
                File logFile = new File(rootLogFile, "esLogCat.txt");
                Log.d(TAG, "logFile.getAbsolutePath(): " + logFile.getAbsolutePath());
                try {
                    // -v to have the timestamp in the log
                    // -r <b> to keep the log size to at most b bytes (rotating log)
                    // -f to write in a file
                    //Runtime.getRuntime().exec(new String[]{"logcat", "-v", "time", "-f", logFile.getAbsolutePath(), "-r", "16", "-n", "2"});
                    Runtime.getRuntime().exec(new String[]{"logcat", "-v", "time", "-f", logFile.getAbsolutePath()});
                    Log.d(TAG, "logcat just launched");

                } catch (IOException e) {
                    Log.e(TAG, "Cannot redirect logcat to a file");
                }
            } else {
                Log.d(TAG, "rootLogFile is null, no available external storage.");
            }
        }

        // 4) Initialize static variables used in many places in the app
        context = getApplicationContext();
        dbHelper = new DbHelper(context);

        // 5) upgrade the app if necessary
        upgradeApp();

        /*
        Until this point, we have not put anything in the database. Before we do that, we set the
        DATE_FIRST_ENTRY_IN_DB to the current timestamp if there is no existing value (which means
        it is a fresh install).

        With DATE_OLDEST_SIGNAL_IN_DB we have a slightly different strategy.  This preference has
        been added after DATE_FIRST_ENTRY_IN_DB in the code, therefore it is possible that this
        preference is not set, but DATE_OLDEST_SIGNAL_IN_DB is set. In that case,
        DATE_OLDEST_SIGNAL_IN_DB must be initialized to DATE_FIRST_ENTRY_IN_DB.

        We do this because this is the reference timestamp used by the data uploader with respect
        to which it fetches the data from the database and creates the JSON.
        */
        SharedPreferences settings = getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();
        String now = Long.toString(System.currentTimeMillis());
        if (Tools.firstEntryInDb() == 0) {
            edit.putString(Const.DATE_FIRST_ENTRY_IN_DB, now).apply();
        }

        if (Tools.oldestSignalInDb() == 0) {
            edit.putString(Const.DATE_OLDEST_SIGNAL_IN_DB, Long.toString(Tools.firstEntryInDb())).apply();
        }

        // 6) update the database with new device info if anything changed. In particular, we update
        //    here the application version
        DeviceInfoMonitor.run(false);

        // 7) When we restart the app, we reset the SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR to true,
        //    unless DONT_SHOW_APP_BACKGROUND_LOCATION_ERROR is true.
        edit.putBoolean(Const.SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR,
                !settings.getBoolean(Const.DONT_SHOW_APP_BACKGROUND_LOCATION_ERROR, false)).apply();

        DbRequestHandler.dumpEventToDatabase(Const.EVENT_APP_STARTED);

        // 8) set the dark or light mode according to the settings
        Tools.setDarkMode(SettingsPreferenceFragment.get_PREF_KEY_DARK_MODE());
    }

    /**
     * This method is in charge of all upgrade operations for the app, apart from the DB upgrade
     * that is specifically handled in DbHelper.onUpgrade()
     * <p>
     * We use build's version code along with the version code of the app that we store in the db
     * for each upgrade. This may not be a 100% foolproof method, as a user may clear the app's
     * data before doing an upgrade for which this way of detecting an upgrade will not work.
     * <p>
     * We could also detect an app upgrade using a BroadcastReceiver for the actions
     * ACTION_MY_PACKAGE_REPLACED. However, we have no guarantee that this intent will always be
     * delivered. So we cannot rely on it for a mission critical upgrade. Indeed, in case this
     * intent is not delivered and the upgrade is critical (that is, if not applied, the app will
     * crash), the lack of upgrade may make the app crash when the service is
     * restarted. The cost of calling upgradeApp at each application start is low enough and
     * perfectly reliable, this is why we preferred this solution over the intent.
     */
    private void upgradeApp() {
        Log.d(TAG, "in upgradeApp()");
        // get the current version of the app in the DB, in case of upgrade this version
        // will be lower than the one in currentVersionCode (the package version code)
        int latestVersionCodeInDb = DbRequestHandler.getLatestAppVersionInDB();
        int currentVersionCode = Tools.getAppVersionNumber();
        if (latestVersionCodeInDb == 0) {
            Log.d(TAG, "upgradeApp: This is a clean install or the app data has been reset. " +
                    "latestVersionCodeInDb: " + latestVersionCodeInDb + " currentVersionCode: " +
                    currentVersionCode);
        } else if (latestVersionCodeInDb < currentVersionCode) {
            Log.d(TAG, "upgradeApp: The app has been upgraded. latestVersionCodeInDb: " +
                    latestVersionCodeInDb + " currentVersionCode: " + currentVersionCode);
            // BEGIN Upgrade tests (we perform here test on the version code to run the correct
            //                      upgrade procedure)
            if (latestVersionCodeInDb <= 57) {
                SyncUtils.removePeriodicSync();
            }

            // users with an installed app version below 62 will continue using dbm as the
            // default exposition metric, and will have the advanced mode per default.
            if (latestVersionCodeInDb < 62) {
                Log.d(TAG, "upgradeApp: latestVersionCodeInDb < 62");
                PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                        .edit().putString(MainApplication.getContext().getString(R.string.PREF_KEY_EXPOSURE_METRIC),
                                MainApplication.getContext().getString(R.string.PREF_VALUE_DBM_METRIC)).apply();

                PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                        .edit().putBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_ADVANCED_MODE),
                                true).apply();
            }

            if (latestVersionCodeInDb < 68) {
                // Users with an installed app version below 68 will have to go once again through
                // the agreement flow. Indeed, this is the first on-boarding version and we have a
                // new RGPD compliant terms of use
                Log.d(TAG, "we reset IS_TERMS_OF_USE_ACCEPTED");
                context.getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE)
                        .edit().putBoolean(Const.IS_TERMS_OF_USE_ACCEPTED, false).apply();

            }
            //END Upgrade tests
        } else {
            Log.d(TAG, "No need to upgrade. latestVersionCodeInDb: " +
                    latestVersionCodeInDb + " currentVersionCode: " + currentVersionCode);
        }
    }

    /**
     * A utility method that creates a notification channel as required by API 26+ devices.
     * <p>
     * See <a href="https://developer.android.com/training/notify-user/channels">
     * https://developer.android.com/training/notify-user/channels
     * </a>
     * <p>
     * Create the NotificationChannel, but only on API 26+ because the NotificationChannel
     * class is new and not in the support library
     * <p>
     * Creating an existing notification channel with its original values performs no operation,
     * so it's safe to call this code when starting an app.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Channel for the foreground service
            NotificationChannel channel = new NotificationChannel(
                    Const.FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name_foreground_service),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(
                    getString(R.string.notification_channel_description_foreground_service));
            manager.createNotificationChannel(channel);

            // Channel for the exposure notification
            channel = new NotificationChannel(
                    Const.NOTIFICATION_EXPOSURE_CHANNEL_ID,
                    getString(R.string.notification_channel_name_exposure),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(
                    getString(R.string.notification_channel_description_exposure));
            manager.createNotificationChannel(channel);

            // Channel for the daily statistics notification
            channel = new NotificationChannel(
                    Const.NOTIFICATION_FEATURE_DISCOVERY_CHANNEL_ID,
                    getString(R.string.notification_channel_name_feature_discovery),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(
                    getString(R.string.notification_channel_description_feature_discovery));
            manager.createNotificationChannel(channel);


            // Channel for the new source detected in top5 signals notification
            channel = new NotificationChannel(
                    Const.NOTIFICATION_NEW_SOURCE_DETECTED_CHANNEL_ID,
                    getString(R.string.notification_channel_name_new_source_detected),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(
                    getString(R.string.notification_channel_description_new_source_detected));
            manager.createNotificationChannel(channel);
        }
    }
}

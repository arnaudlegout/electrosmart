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

package fr.inria.es.electrosmart.serversync;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.util.Random;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;

/**
 * A utility class that contains most helper functions used in the data synchronization.
 * <p>
 * Here is the overall flowchart of the data synchronization strategy.
 * <p>
 * <p>
 * <pre>
 * //                             +
 * //                             |
 * //                             v
 * //      +-------------------------------------------------------------------------+
 * //      |    Create an alarm at time T such that                                  |
 * //      |  T = [MIN_DURATION_FOR_NEXT_SYNC, MIN_DURATION_FOR_NEXT_SYNC + RANDOM]  |
 * //      +----------------------+---------+----------------------------------------+
 * //                             |
 * //            ON ALARM TRIGGER v
 * //   +----------->------------>|
 * //   |                         v
 * //   |               +-------------------+
 * //   |               | +---------------+ |            NO     +-----------------------+
 * //   |               | |WiFi Connected?| +------------->-- > |       Register        |
 * //   |               | +---------------+ |                   | NETWORK_STATE_CHANGED |
 * //   |               +-------------------+                   +------------+----------+
 * //   ^                         |                                          |
 * //   |                     YES v                                          | ON NETWORK_STATE_CHANGED
 * //   |                         |<-------------------<---+                 v    && WIFI_CONNECTED
 * //   |                         |                        |    +-----------------------+
 * //   |                         |                        |    |      Unregister       |
 * //   |                         |                        ^    | NETWORK_STATE_CHANGED |
 * //   |                         v                        |    +-----------------------+
 * //   |  +-------------------------------------------+   |                 |
 * //   |  |    Create an alarm at time T such that    |   +- ---<-----------v
 * //   -<-+  T = [MIN_DURATION_FOR_NEXT_SYNC, MIN_DURATION_FOR_NEXT_SYNC + RANDOM]  |
 * //      +--------------------+-----------+----------+
 * //                           |
 * //                           |
 * //                           |
 * //                           v
 * //                   +-------------+
 * //                   |             |
 * //                   |   Do Sync   |
 * //                   |             |
 * //                   +-------------+
 * </pre>
 * <p>
 * <p>
 * We create the sync alarm in the following cases
 * 1. The first time we start the services with the call to MeasurementFragment.startAllServices()
 * 2. When the device boot, we schedule the sync alarm in OnBootBroadcastReceiver class (note
 * ...that all alarms are removed on device boot)
 */

public class SyncUtils {
    private static final String TAG = "SyncUtils";
    private static WiFiStateChangeBroadcastReceiver wiFiStateChangeBroadcastReceiver;

    private static Random random = new Random();

    /**
     * Creates an alarm after triggerAtSecondsFromNow for data synchronization.
     * <p>
     * Note that as we use  alarmManager.set() to schedule the alarm, any newly scheduled
     * sync alarm will automatically cancel the previous one.
     * https://developer.android.com/reference/android/app/AlarmManager.html#set(int,%20long,%20android.app.PendingIntent)
     *
     * @param triggerAtSecondsFromNow the time in seconds from now
     */
    public static void createSyncAlarm(long triggerAtSecondsFromNow) {
        Log.d(TAG, "in createSyncAlarm");
        long now = SystemClock.elapsedRealtime();

        // get access to the AlarmManager
        AlarmManager alarmManager = (AlarmManager) MainApplication.getContext()
                .getSystemService(Context.ALARM_SERVICE);

        // Create the intent used by the AlarmManager to schedule a new data synchronization
        Intent intent = new Intent(MainApplication.getContext(), SyncAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainApplication.getContext(),
                Const.REQUEST_SYNC_ALARM, intent, Tools.flag_immutable());
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                now + (triggerAtSecondsFromNow * 1000), pendingIntent);
        Log.d(TAG, "Scheduled a data synchronization in " + triggerAtSecondsFromNow + " seconds " +
                " (" + triggerAtSecondsFromNow / (60 * 60.0) + " hours)");
    }

    /**
     * Cancels the sync alarm if it exists.
     */
    private static void cancelSyncAlarm() {
        Log.d(TAG, "in cancelSyncAlarm()");
        if (doesSyncAlarmExist()) {
            AlarmManager alarmManager = (AlarmManager) MainApplication.getContext().
                    getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(MainApplication.getContext(), SyncAlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainApplication.getContext(),
                    Const.REQUEST_SYNC_ALARM, intent, Tools.flag_immutable());
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "canceled sync alarm");
        } else {
            Log.d(TAG, "cancelSyncAlarm: no alarm to cancel");
        }
    }

    /**
     * A utility function that cancels the sync alarm if it exists and unregisters the
     * WiFiStateChangeBroadcastReceiver. We need this for graceful exit in case of an application
     * crash.
     */
    public static void cancelSyncAlarmAndUnregisterBroadcastReceiver() {
        Log.d(TAG, "in cancelSyncAlarmAndUnregisterBroadcastReceiver()");
        cancelSyncAlarm();
        unregisterWiFiStateChangeBroadcastReceiver();
    }

    /**
     * Unregisters the wiFiStateChangeBroadcastReceiver and sets it to null
     */
    static void unregisterWiFiStateChangeBroadcastReceiver() {
        Log.d(TAG, "in unregisterWiFiStateChangeBroadcastReceiver");
        if (wiFiStateChangeBroadcastReceiver != null) {
            MainApplication.getContext().unregisterReceiver(wiFiStateChangeBroadcastReceiver);
            wiFiStateChangeBroadcastReceiver = null;
            Log.d(TAG, "unregistered wiFiStateChangeBroadcastReceiver and set it to null.");
        }
    }

    /**
     * Registers the wiFiStateChangeBroadcastReceiver to receive and change in WiFi state.
     */
    public static void registerWiFiStateChangeBroadcastReceiver() {
        Log.d(TAG, "in registerWiFiStateChangeBroadcastReceiver");
        IntentFilter connectivityFilter = new IntentFilter();
        if (wiFiStateChangeBroadcastReceiver == null) {
            wiFiStateChangeBroadcastReceiver = new WiFiStateChangeBroadcastReceiver();
        }
        connectivityFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        MainApplication.getContext().registerReceiver(wiFiStateChangeBroadcastReceiver, connectivityFilter);
    }

    /**
     * A helper function to check if the data synchronization alarm already exists.
     * <p>
     * We check the existence of a scheduled alarm by checking if the pendingIntent used
     * for the alarm exists. If it does not exist (in that case, getBroadcast() with the flag
     * PendingIntent.FLAG_NO_CREATE will return null), it means that there is no scheduled alarm.
     * <p>
     * It is an indirect way to check the existence of an alarm, but there is no API in the
     * AlarmManager to check currently scheduled alarms.
     *
     * @return true if sync alarm exists, false otherwise
     */
    public static boolean doesSyncAlarmExist() {
        Intent intent = new Intent(MainApplication.getContext(), SyncAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainApplication.getContext(),
                Const.REQUEST_SYNC_ALARM, intent, PendingIntent.FLAG_NO_CREATE | Tools.flag_immutable());
        if (pendingIntent == null) {
            Log.d(TAG, "doesSyncAlarmExist: pendingIntent is null. So, there is no alarm running.");
            return false;
        } else {
            Log.d(TAG, "doesSyncAlarmExist: pendingIntent is NOT null. So, there is an alarm running.");
            return true;
        }
    }

    /**
     * Returns a random sync time in [min_interval, min_interval + randomization]
     * <p>
     * [MIN_DURATION_FOR_NEXT_SYNC, MIN_DURATION_FOR_NEXT_SYNC + SYNC_RANDOMIZATION]
     *
     * @param min_interval  minimum time in second to select the random sync time
     * @param randomization randomization time in second. The random sync time will be selected in
     *                      [min_interval, min_interval + randomization]
     * @return random sync time in seconds
     */
    public static long getRandomSyncTime(long min_interval, long randomization) {
        Log.d(TAG, "in getRandomSyncTime(). min_interval: " + min_interval +
                " randomization: " + randomization);
        return min_interval + (long) (random.nextDouble() * randomization);
    }

    /**
     * Checks whether the device can make a synchronization on the current data connection.
     * <p>
     * The rules are the following:
     * - if connected over Wi-Fi always returns true
     * - if connected over cellular, returns true iif the setting PREF_KEY_SYNC_CELLULAR is true,
     * that is the users allowed to sync on a cellular connection
     * <p>
     * Requires android permission ACCESS_NETWORK_STATE.
     *
     * @param context the application context.
     * @return true - if sync is possible on the current data connection (Wi-Fi or cellular),
     * false - otherwise.
     */
    public static boolean isSyncPossibleOnCurrentDataConnection(Context context) {
        Log.d(TAG, "in isSyncPossibleOnCurrentDataConnection()");
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo;
        if (cm != null) {
            netInfo = cm.getActiveNetworkInfo();
        } else {
            return false;
        }

        if ((netInfo != null) &&
                netInfo.isConnected() &&
                (netInfo.getType() == ConnectivityManager.TYPE_WIFI)) {
            Log.d(TAG, "isSyncPossibleOnCurrentDataConnection: we are connected to Wi-Fi, " +
                    "returns true.");
            return true;
        } else {
            Log.d(TAG, "isSyncPossibleOnCurrentDataConnection: not connected on Wi-Fi, " +
                    "cellular sync: " + SettingsPreferenceFragment.get_PREF_KEY_SYNC_CELLULAR());
            return SettingsPreferenceFragment.get_PREF_KEY_SYNC_CELLULAR();
        }
    }

    /**
     * A helper function that does a manual data sync operation.
     * <p>
     * <p>
     * WARNING: before calling this method you must be sure that we can sync on the current data
     * connection by calling SyncUtils.isSyncPossibleOnCurrentDataConnection()
     * <p>
     * We pass the SYNC_EXTRAS_EXPEDITED only if requested explicitly (isAlarmSync is false).
     * For instance when we want to sync using an onclick event.
     *
     * @param isAlarmSync when set true, it means the sync request is the result of an alarm sync,
     *                    otherwise it is the result of a user action
     */
    public static void requestManualDataSync(boolean isAlarmSync) {
        Log.d(TAG, "in requestManualDataSync()");
        // We sync the data with the server
        // Pass the settings flags by inserting them in a bundle
        Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(
                ContentResolver.SYNC_EXTRAS_MANUAL, true);
        if (!isAlarmSync) {
            settingsBundle.putBoolean(
                    ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        }

        ContentResolver.requestSync(SyncManager.getSyncAccount(MainApplication.getContext()),
                Const.AUTHORITY,
                settingsBundle);

        /*
        write in a shared preference the time (in ms) of the last requested sync fired by an alarm
        (isAlarmSync false) and not by the about activity (isAlarmSync true)
        */
        if (isAlarmSync) {
            SharedPreferences.Editor edit = MainApplication.getContext().
                    getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE).edit();
            long now = SystemClock.elapsedRealtime();
            edit.putString(Const.LAST_REQUESTED_SYNC,
                    Long.toString(now)).apply();

        }
        Log.d(TAG, "Completed requestManualDataSync()");
    }

    /**
     * A helper function that removes the periodic sync associated with the app.
     */
    public static void removePeriodicSync() {
        Log.d(TAG, "in removePeriodicSync()");
        android.accounts.Account account = SyncManager.getSyncAccount(MainApplication.getContext());
        ContentResolver.removePeriodicSync(account, Const.AUTHORITY, Bundle.EMPTY);
    }

    /**
     * A broadcast receiver to handle data synchronization alarm
     * <p>
     * According to the data synchronization strategy, we do a data sync if we are
     * connected over WiFi and ready to pass data, else if we are not connected over WiFi, we
     * register a WiFiStateChangeBroadcastReceiver.
     * <p>
     * After we do a sync, we create a new sync alarm for the next synchronization.
     */
    public static class SyncAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "in SyncAlarmReceiver.onReceive()");

            /*
            Up to Android 7.1 there is a bug in the SyncManager. Each time we create an account
            a periodic sync is automatically created with a 24h period. This issue is documented in
            https://bitbucket.org/es-inria/es-android/issues/176/syncadapter-framework-creates-a-default

            As we are managing sync events with our own sync alarm, we do not want to have in
            parallel a periodic sync. To workaround this bug, we systematically try to remove
            periodic syncs when the sync alarm is triggered.
            */
            SyncUtils.removePeriodicSync();

            //  if isSyncPossibleOnCurrentDataConnection() is true, start the sync
            // and create a new SyncAlarm
            if (isSyncPossibleOnCurrentDataConnection(context)) {
                Log.d(TAG, "isSyncPossibleOnCurrentDataConnection is true. So, we do a sync.");
                requestManualDataSync(true);
                SyncUtils.createSyncAlarm(SyncUtils.getRandomSyncTime(
                        Const.MIN_DURATION_FOR_NEXT_SYNC,
                        Const.SYNC_RANDOMIZATION));
            } else {
                /*if not, register a broadcast receiver for WifiManager.NETWORK_STATE_CHANGED_ACTION

                  We note that we reschedule and sync alarm immediately. The reason is that if we
                  rely solely on the wifiStateChangeReceiver to reschedule the alarm when there is
                  Wi-Fi connectivity, it is not robust. Indeed, if you kill the app (not a force
                  kill, but a regular task manager of OS kill) the wifiStateChangeReceiver will
                  be unregistered and the implicit intent will have no way to reach our receiver
                  and to trigger the schedule of a new sync alarm. If there is no pending sync
                  alarm, there will be no way to sync afterward unless the app is restarted.

                  Our strategy is to immediately reschedule a sync alarm. If ever the
                  wifiStateChangeReceiver receive a wifi connected state, then we will reschedule
                  again the sync alarm (indeed, a rescheduled alarm with set() will automatically
                  cancel the previous one
                  https://developer.android.com/reference/android/app/AlarmManager.html#set(int,%20long,%20android.app.PendingIntent)

                  If ever the wifiStateChangeReceiver is unregistered, we still have a scheduled
                  sync alarm.

                 */
                SyncUtils.createSyncAlarm(SyncUtils.getRandomSyncTime(
                        Const.MIN_DURATION_FOR_NEXT_SYNC,
                        Const.SYNC_RANDOMIZATION));
                Log.d(TAG, "isSyncPossibleOnCurrentDataConnection is false. So, we create a " +
                        "WiFiStateChangeBroadcastReceiver.");
                registerWiFiStateChangeBroadcastReceiver();
            }
        }
    }
}

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

package fr.inria.es.electrosmart.scheduling;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.monitors.BluetoothMonitor;
import fr.inria.es.electrosmart.monitors.CellularMonitor;
import fr.inria.es.electrosmart.monitors.DeviceInfoMonitor;
import fr.inria.es.electrosmart.monitors.LocationMonitor;
import fr.inria.es.electrosmart.monitors.WifiMonitor;
import fr.inria.es.electrosmart.serversync.SyncUtils;
import fr.inria.es.electrosmart.services.ForegroundScanService;
import fr.inria.es.electrosmart.signalhandler.RawSignalHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;

/*
 In this documentation I explain how to schedule recurring event in Android.
 Two different cases must be considered:
 1) the application is foreground (Activity created and displayed)
 2) the application is background (any other state)

  When the application is foreground, we need to update the measurements MEASUREMENT_CYCLE_FOREGROUND
  that is usually every 5 seconds. This is a very short period that requires a low overhead
  and accurate scheduling. In that context, a Handler is the best option to choose. A Handler is
  light weight and accurate. This is the solution we are using and there is no difficulty here.

  When the application is background, we cannot use anymore a Handler because the post() and
  postDelayed() methods rely on SystemClock.uptimeMillis() that will be suspended when the device
  goes to sleep (screen off, no user interaction). Note that this notion of sleep is different from
  Doze and existed before M. Here is the documentation that explain the different system clocks in
  Android and their takeoffs
  http://developer.android.com/reference/android/os/SystemClock.html
  https://developer.android.com/training/scheduling/alarms.html

  elapsedRealtime() returns the time since the system was booted, and include deep sleep. This clock
  is guaranteed to be monotonic, and continues to tick even when the CPU is in power saving modes,
  so is the recommend basis for general purpose interval timing.

  The AlarmManager is using elapsedRealtime() (if scheduled with ELAPSED_REALTIME) so it is the
  only reliable method to schedule measurement when the device is sleeping.
  See the documentation of the AlarmManager() here
  http://developer.android.com/reference/android/app/AlarmManager.html
  https://developer.android.com/training/scheduling/alarms.html

  However, Doze brings restriction to the AlarmManager:
   - alarms are deferred to all run during a maintenance window that can be far away (up to 6 hours)
   - alarms can be triggered out of the maintenance window using setAndAllowWhileIdle() or
     setExactAndAllowWhileIdle(), but at most one such alarm every 9 minutes is possible.

   The Android code that defines the 9 minutes limit is here
   https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/AlarmManagerService.java

  Also, the AlarmManager will schedule an alarm that will wake-up the device when the
  Intent sent by the alarm is received. We need for that a Broadcast receiver. The application will
  keep a wake-lock in the onReceive() method of the Broadcast receiver. But as we have
  several synchronous operations (wifi and BT scans) the onReceive() method will return before
  we have a chance to complete the scan. If ever the device goes to sleep immediately after
  onReceive() returns, we will never get the result of the scans. In practice, it does not seem to
  be a problem as we correctly get all signals even in background during Doze.

   See the BroadcastReceiver documentation  on the Receiver and Process Lifecycle
   http://developer.android.com/reference/android/content/BroadcastReceiver.html
   https://developer.android.com/training/scheduling/wakelock.html

   Here is a discussion of the respective merits of Handler and AlarmManager
   https://groups.google.com/forum/?fromgroups#!searchin/android-developers/postdelayed/android-developers/iDiyWbSrnQo/ePZssY7e3-UJ
 */
public final class MeasurementScheduler {
    private static final String TAG = "MeasurementScheduler";

    // A set to keep track of the monitors that completed their measurement
    private static final Set<MonitorType> mMonitorsCompleted = new HashSet<>(3);
    private static final Handler handler = new Handler();
    // The list that keeps track of the old notification results to be used for the update
    public static List<BaseProperty> sHighExposureNotificationHistory =
            new ArrayList<>(Const.MAXIMUM_NOTIFICATION_HISTORY_SIZE);
    // current mode of the scheduler
    public static SchedulerMode schedulerMode = SchedulerMode.BACKGROUND;
    // The signal with the greatest dbm value we retrieved from the monitors to show as a
    // the notification
    private static BaseProperty sHighExposureNotificationSignal;
    // date of the last Bluetooth scan. This is used to guarantee at most one scan per 30 seconds
    private static Calendar lastBtScan = Calendar.getInstance();
    //RawSignalHandler rawSignalHandler;
    private static Runnable runnableAllMonitors;
    // Time in milliseconds since epoch when the last Wi-Fi scan was done
    private static long lastWiFiScanTimeMillis = 0;
    // this variable is set to true only when the runAllMonitors() is called for the first time
    // when entering foreground.
    private static boolean isFirstForegroundRun = false;

    static {
        // we initialize the lastBtScan to the EPOCH
        lastBtScan.setTimeInMillis(0);
    }

    private MeasurementScheduler() {
    }

    /*
     * Schedule the next background measurement from now
     *
     * @param triggerAtMillisFromNow Time to start the measurement from now in milliseconds
     */
    public static void scheduleBackgroundMeasurement(long triggerAtMillisFromNow) {
        Log.d(TAG, "in scheduleBackgroundMeasurement()");
        long now = SystemClock.elapsedRealtime();

        /*
        write in a shared preference the time (in ms) when the next alarm has been scheduled.
        This can be used by a watchdog to check that the alarm is still running.
        */
        SharedPreferences.Editor edit = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE).edit();
        edit.putString(Const.LAST_SCHEDULED_ALARM,
                Long.toString(now + triggerAtMillisFromNow)).apply();

        Context context = MainApplication.getContext();

        // get access to the AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create the intent used by the AlarmManager to schedule periodic scans in the background
        Intent intent = new Intent(context, MeasurementScheduler.AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                Tools.flag_immutable());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    now + triggerAtMillisFromNow, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    now + triggerAtMillisFromNow, pendingIntent);
        }
        Log.d(TAG, "Scheduled a new measurement in " + triggerAtMillisFromNow / 1000 + " seconds");
    }

    public static void runAllMonitors(MeasurementScheduler.SchedulerMode schedulerMode) {
        Log.d(TAG, "in runAllMonitors()");
        if (schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
            Log.d(TAG, "in runAllMonitors FOREGROUND");
            Log.d(TAG, "runAllMonitors: isFirstForegroundRun: " + isFirstForegroundRun);


            // At the first call of this method in foreground, we get the latest Wi-Fi scan result
            // in order to speed up the first display of Wi-Fi
            if (isFirstForegroundRun) {
                Log.d(TAG, "runAllMonitors: first call of runAllMonitors when entering foreground");
                WifiMonitor.run(false);
                CellularMonitor.run(false);
                isFirstForegroundRun = false;
            }

            // even on the first foreground run we request a scan to get as fast as possible the
            // most up to date data.
            CellularMonitor.run(true);

            // Android P and Q limits Wi-Fi scans to 4 scans every 2 minutes in foreground.
            // To mitigate this issue, we spread evenly the scans on the 2 minutes,
            // one scan every 30 seconds
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                long currentTimeMillis = System.currentTimeMillis();
                if ((currentTimeMillis - lastWiFiScanTimeMillis) > Const.MEASUREMENT_CYCLE_FOREGROUND_WIFI_ANDROID_P) {
                    Log.d(TAG, "runAllMonitors: Android P+ device. Time for a foreground WiFi-scan.");
                    lastWiFiScanTimeMillis = currentTimeMillis;
                    WifiMonitor.run(true);
                } else {
                    Log.d(TAG, "runAllMonitors: Android P+ constrained. We are " +
                            (int) ((currentTimeMillis - lastWiFiScanTimeMillis) / 1000) + " seconds " +
                            "from previous foreground Wi-Fi scan. So, we wait.");
                    WifiMonitor.run(false);
                }
            } else {
                Log.d(TAG, "runAllMonitors: No Wi-Fi in foreground constraints");
                WifiMonitor.run(true);
            }

            // the BT discovery cycle is 13 seconds, we cannot run the monitor at a lower
            // frequency. we select a minimum of 30 seconds to be on the safe side. Note
            // that I also tried to shift by 3 seconds the BT scan in order to do not be
            // synchronized with wifi scans, but it does not seem to improve the lost of
            // some wifi signals (my simple tests show that it is even worse, but I have no
            // clue why).
            Log.d(TAG, " before running the BT scan. lastBtScan =  " + lastBtScan.getTime() +
                    " now = " + Calendar.getInstance().getTime() + "\n seconds between: " +
                    Tools.secondsBetween(lastBtScan.getTime(), Calendar.getInstance().getTime()));

            if (Tools.secondsBetween(lastBtScan.getTime(), Calendar.getInstance().getTime()) >=
                    Const.MIN_TIME_BETWEEN_CONSECUTIVE_BT_SCAN) {
                Log.d(TAG, " running the BT scan. lastBtScan =  " + lastBtScan.getTime() +
                        " now = " + Calendar.getInstance().getTime());
                lastBtScan = Calendar.getInstance();
                BluetoothMonitor.run();
            }
        } else {
            Log.d(TAG, "in runAllMonitors BACKGROUND");

            // prepare the notification data structures for this new measurement run
            mMonitorsCompleted.clear();
            sHighExposureNotificationSignal = new WifiProperty(false);

            DeviceInfoMonitor.run(false);
            CellularMonitor.run(true);
            WifiMonitor.run(true);
            BluetoothMonitor.run();
            // we clean the database by removing the oldest entries
            DbRequestHandler.deleteOldestDbEntries();
        }
    }

    public static void stopMeasurementScheduler() {
        Log.d(TAG, "in stopMeasurementScheduler()");
        Log.d(TAG, "cancel all the pending Alarms and Handlers");
        Context context = MainApplication.getContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Create the intent used by the AlarmManager to schedule periodic scans in the background
        Intent intent = new Intent(context, MeasurementScheduler.AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                Tools.flag_immutable());

        alarmManager.cancel(pendingIntent);
        if (runnableAllMonitors != null) {
            // cancel the pending runnable used for the foreground task
            handler.removeCallbacks(runnableAllMonitors);
        }
    }

    /**
     * This method restores the Wifi and BT cards in their initial state, stop all broadcast
     * receivers, and stop the location monitor.
     * <p/>
     * This method must be called each time we want to restore the phone in its initial state
     * without any battery consuming on-going task
     *
     * @param isForeground set to true is the method is called when the app is in foreground
     */
    public static void restoreInitialCardStateAndStopBroadcastReceivers(boolean isForeground) {
        Log.d(TAG, "in restoreInitialCardStateAndStopBroadcastReceivers(" + isForeground + ")");
        // restore the initial state of the Wifi and BT cards
        WifiMonitor.restoreInitialWifiCardState(isForeground);
        BluetoothMonitor.restoreInitialBTCardState(isForeground);

        // unregister the broadcast receivers
        BluetoothMonitor.unregisterAllBluetoothReceivers();
        WifiMonitor.unregisterWifiReceiver();
        CellularMonitor.unregisterMyPhoneStateListener();

        // stop the location monitor
        LocationMonitor.stopLocationMonitor();
    }

    /**
     * This method is used by the alarm receiver to known (when not in Doze) whether the alarm
     * receiver must start a location scan or a signal scan. When it returns true, it means that
     * a location scan was on-going, so the alarm receiver must start a signal scan.
     *
     * @return true when an alarm has been triggered to make a location scan, false otherwise
     */
    public static boolean isLocationScanRunning() {
        SharedPreferences settings = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return settings.getBoolean(Const.IS_LOCATION_SCAN_RUNNING, false);
    }

    /**
     * This method is setting a shared preference to true when an alarm has been triggered to make
     * a location scan, and to false when an alarm has been triggered to make a signal scan.
     *
     * @param isLocationScanRunning true if an alarm has been triggered to make a location scan,
     *                              false otherwise
     */
    private static void setLocationScanRunning(boolean isLocationScanRunning) {
        Log.d(TAG, "setLocationScanRunning: isLocationScanRunning=" + isLocationScanRunning);
        SharedPreferences.Editor edit = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE).edit();
        if (isLocationScanRunning) {
            edit.putBoolean(Const.IS_LOCATION_SCAN_RUNNING, true).apply();

        } else {
            edit.putBoolean(Const.IS_LOCATION_SCAN_RUNNING, false).apply();
        }
    }

    /**
     * Entry point to start the scheduler in both foreground and background mode. The correct mode
     * to handle is given with an intent
     *
     * @param schedulerStartMode : the mode in which we start the scheduler
     * @param signalDataReceiver : the signal receiver
     */
    public static void startMeasurementScheduler(SchedulerMode schedulerStartMode,
                                                 MainActivity.SignalDataReceiver signalDataReceiver) {

        Log.d(TAG, "in startMeasurementScheduler()");
        // 1) Set the scheduler mode from the passed intent
        Log.d(TAG, "startMeasurementScheduler: schedulerStartMode is: " + schedulerStartMode);

        // create an instance of the signalDataReceiver used by processRawSignals to send the
        // newly received signals to the MeasurementFragment. This receiver is only used in current mode.
        if (signalDataReceiver != null) {
            MainApplication.setReceiver(signalDataReceiver);
        }


        /*
        In case schedulerStartMode is null, we set schedulerMode to SchedulerMode.BACKGROUND.
        The intent can be null (thus schedulerStartMode) when the scheduler is automatically
        started after the system stopped the process to, e.g., free memory

        If schedulerStartMode is not ON_DEVICE_BOOT, then it is called from the MeasurementFragment
        so we set the schedulerMode to the schedulerStartMode.

        If schedulerStartMode is ON_DEVICE_BOOT, we don't do anything.
        The reason is that either the scheduler is already started, and
        we don't want to change its mode (it is possible if you start the application right
        after the boot before the ACTION_BOOT_COMPLETED intent is received), or it is not already
        started, but the call to startMeasurementScheduler() created the class MeasurementScheduler
        if it is not already created and the schedulerMode static field that is initialized during
        class creation to SchedulerMode.BACKGROUND.
        */
        if (schedulerStartMode == null) {
            schedulerMode = MeasurementScheduler.SchedulerMode.BACKGROUND;
        } else if (schedulerStartMode != MeasurementScheduler.SchedulerMode.ON_DEVICE_BOOT) {
            if (schedulerStartMode == SchedulerMode.FOREGROUND) {
                Log.d(TAG, "startMeasurementScheduler: set isFirstForegroundRun to true");
                isFirstForegroundRun = true;
            }
            schedulerMode = schedulerStartMode;
        }
        Log.d(TAG, "schedulerMode: " + schedulerMode.toString());

        if (runnableAllMonitors != null) {
            // cancel the pending runnable used for the foreground task
            handler.removeCallbacks(runnableAllMonitors);
        }

        // 2) create a runnable to make periodic scans in Foreground
        runnableAllMonitors = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "in runnableAllMonitors.run()");
                // if we are in FOREGROUND we run all monitors right away and schedule the next
                // run after Const.MEASUREMENT_CYCLE_FOREGROUND. The location monitor is managed
                // in the life cycle of MeasurementFragment

                if (schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
                    runAllMonitors(schedulerMode);
                    // update the user interface
                    RawSignalHandler.getInstance().updateUIwithCachedRawSignals();
                    handler.postDelayed(this, Const.MEASUREMENT_CYCLE_FOREGROUND);

                    // if we are in BACKGROUND:
                    //     - start the location monitor and schedule runAllMonitors after 30 seconds
                    //     - after the 30 seconds, run all monitors and stop the location monitor
                } else {
                    Log.w(TAG, "in runnableAllMonitors.run(), but we are in Background");
                }
            }
        };

        // 3) Schedule the scans

        // when we are in background mode, we start the measurement after the first
        // SettingsPreferenceFragment.get_MEASUREMENT_CYCLE_BACKGROUND() in order to avoid the
        // overhead of an immediate measurement whereas we just left the foreground mode.
        if (schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
            DbRequestHandler.dumpEventToDatabase(Const.EVENT_SCHEDULER_FOREGROUND);
            Context context = MainApplication.getContext();
            //Cancel the alarm manager used for the background tasks
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            // Create the intent used by the AlarmManager to schedule periodic scans in the background
            Intent alarmIntent = new Intent(context, MeasurementScheduler.AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent,
                    Tools.flag_immutable());

            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Alarm manager cancelled");
            Log.d(TAG, "start a handler for the foreground scan");
            handler.post(runnableAllMonitors);
        } else {
            DbRequestHandler.dumpEventToDatabase(Const.EVENT_SCHEDULER_BACKGROUND);

            /*
            We start the alarm manager to schedule measurements in the background.
            We set the location scan alarm to false so that the first alarm received triggers a
            location scan.
             */
            setLocationScanRunning(false);
            Log.d(TAG, "runnableAllMonitors was already running. Cancel it to start a background AlarmManager");
            Log.d(TAG, "start an AlarmManager for the background scan");
            scheduleBackgroundMeasurement(SettingsPreferenceFragment.get_MEASUREMENT_CYCLE_BACKGROUND());

            /*
            Create an alarm for data synchronization if it does not already exist. Either the
            has been forced kill or it is a first start.
            */
            if (!SyncUtils.doesSyncAlarmExist()) {
                if (schedulerStartMode == MeasurementScheduler.SchedulerMode.ON_DEVICE_BOOT) {
                    SyncUtils.createSyncAlarm(SyncUtils.getRandomSyncTime(
                            Const.MIN_DURATION_FOR_NEXT_SYNC_ON_BOOT,
                            Const.SYNC_RANDOMIZATION_ON_NEW_SYNC_ALARM));
                } else {

                    SharedPreferences settings = MainApplication.getContext().
                            getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);

                    /*
                    We retrieve the last requested sync, which means that the sync alarm correctly
                    fired and that we had Wi-Fi connectivity. It does not mean that the sync was
                    successful (we might have server or network issues), but that is was correctly
                    initiated. As the logic below is to work around alarm sync issues (and not
                    synchronization issues due to server or network problems), this is the correct
                    value to consider.
                     */
                    long last_requested_sync = Long.
                            parseLong(settings.getString(Const.LAST_REQUESTED_SYNC, "0"));
                    long now = SystemClock.elapsedRealtime();
                    long last_request_sync_interval = (now - last_requested_sync) / 1000; // in seconds
                    long average_sync_interval = Const.MIN_DURATION_FOR_NEXT_SYNC +
                            Const.SYNC_RANDOMIZATION / 2;

                    Log.d(TAG, "last_requested_sync (ms): " + last_requested_sync +
                            ", now (ms): " + now +
                            ", last_request_sync_interval (s): " + last_request_sync_interval +
                            ", average_sync_interval: " + average_sync_interval);

                    if (last_request_sync_interval > average_sync_interval) {
                        /*
                        the last requested sync it too old and either it is a fresh install or
                        the app process has been killed. In all cases, we should start a sync
                        process in a short randomized interval from now
                        */
                        SyncUtils.createSyncAlarm(SyncUtils.getRandomSyncTime(
                                0, Const.SYNC_RANDOMIZATION_ON_NEW_SYNC_ALARM));
                    } else {
                        /*
                        the last request sync is not too old, so we we schedule a new sync alarm
                        with a min duration that is
                        max(0, Const.MIN_DURATION_FOR_NEXT_SYNC - last_request_sync_interval)
                        */
                        SyncUtils.createSyncAlarm(SyncUtils.getRandomSyncTime(
                                Math.max(0, Const.MIN_DURATION_FOR_NEXT_SYNC - last_request_sync_interval),
                                Const.SYNC_RANDOMIZATION));
                    }
                }
            }
        }
    }

    /**
     * Called each time a monitor has completed its execution.
     *
     * @param monitor The MonitorType of the monitor
     * @param signals The list of signals received by the monitor or {@code null} if the monitor
     *                couldn't execute
     */
    public static void monitorCompletedMeasurement(MonitorType monitor, @Nullable List<BaseProperty> signals) {
        Log.d(TAG, "monitorCompletedMeasurement: monitor id:"
                + monitor);
        Log.d(TAG, "monitorCompletedMeasurement: signals:"
                + signals);
        Log.d(TAG, "monitorCompletedMeasurement: sHighExposureNotificationSignal:"
                + sHighExposureNotificationSignal);

        // We compute the highest power signal and put it in in sHighExposureNotificationSignal
        if (signals != null && !signals.isEmpty()) {
            BaseProperty signal = Collections.max(signals);
            if (signal != null && sHighExposureNotificationSignal != null &&
                    signal.compareTo(sHighExposureNotificationSignal) > 0) {
                sHighExposureNotificationSignal = signal;
            }
        }

        // we add the monitor to the list of monitors that completed their measurement
        mMonitorsCompleted.add(monitor);

        // we are ready making the notification
        if (sHighExposureNotificationSignal != null &&
                SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_EXPOSURE() && // notif. pref. enabled
                mMonitorsCompleted.size() == 3 &&                               // all monitor returned
                sHighExposureNotificationSignal.isDbmValueInRange() &&
                sHighExposureNotificationSignal.dbm >=
                        SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD()) {
            Log.d(TAG, "monitorCompletedMeasurement: Notify");

            Context context = MainApplication.getContext();

            sHighExposureNotificationHistory.add(sHighExposureNotificationSignal);

            // We are building the notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                    Const.NOTIFICATION_EXPOSURE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_es_notification)
                    .setContentTitle(context.getString(R.string.notification_exposure_title))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true); // delete the notification when we touch it.

            // action when we dismiss the notification (by sliding it)
            Intent notificationDeletedIntent = new Intent(context,
                    NotificationDeletedBroadcastReceiver.class);
            Bundle notificationDeletedExtras = new Bundle();
            notificationDeletedExtras.putInt(Const.NOTIFICATION_ARG_KEY, Const.NOTIFICATION_HIGH_EXPOSURE);
            notificationDeletedIntent.putExtras(notificationDeletedExtras);
            PendingIntent notificationDeletedPendingIntent = PendingIntent.getBroadcast(context,
                    Const.NOTIFICATION_DELETED_PENDING_INTENT_REQUEST_CODE_HIGH_EXPOSURE,
                    notificationDeletedIntent, PendingIntent.FLAG_CANCEL_CURRENT | Tools.flag_immutable());

            notificationBuilder.setDeleteIntent(notificationDeletedPendingIntent);

            // action when we touch the notification (jump to measurement activity)
            Intent notificationClickedIntent = new Intent(context,
                    NotificationClickedBroadcastReceiver.class);
            Bundle notificationClickedExtras = new Bundle();
            notificationClickedExtras.putInt(Const.NOTIFICATION_ARG_KEY,
                    Const.NOTIFICATION_HIGH_EXPOSURE);
            notificationClickedIntent.putExtras(notificationClickedExtras);

            PendingIntent notificationClickedPendingIntent = PendingIntent.getBroadcast(context,
                    Const.NOTIFICATION_CLICKED_PENDING_INTENT_REQUEST_CODE_HIGH_EXPOSURE,
                    notificationClickedIntent, PendingIntent.FLAG_CANCEL_CURRENT | Tools.flag_immutable());

            notificationBuilder.setContentIntent(notificationClickedPendingIntent);
            setNotificationImportance(notificationBuilder, context,
                    SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_EXPOSURE_IMPORTANCE());

            /*
            we build the notification content
            When there is a single notification to display, we show this notification in the summary.
            When there are multiple notifications to display, we show each notification in a line,
            and we generate a notification summary. We can also expend/collapse when there are
            multiple notifications.
            */
            if (sHighExposureNotificationHistory.size() == 1) {
                notificationBuilder.setContentText(notificationLineText(context,
                        sHighExposureNotificationSignal));
            } else {
                // InboxStyle enable multiple lines expendable notifications
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                // We generate the notification content, that is the lines in the notification
                Set<CharSequence> signalNames = new HashSet<>(Const.MAXIMUM_NOTIFICATION_HISTORY_SIZE);
                for (BaseProperty signal : sHighExposureNotificationHistory) {
                    style.addLine(notificationLineText(context, signal));
                    signalNames.add(signal.friendlySourceName(context));
                }
                notificationBuilder.setStyle(style);

                /*
                We generate the notification summary
                The summary contains the list of unique signal names separated by a comma that
                does not exceed 32 chars, if we have remaining signal names to display we show
                them with a +2
                For example: Inria, Eduroam... +4

                Note that the maximum notification summary length is 40.
                We restrict the summary to 32 so that with '... +4' it is at most 40.
                */
                StringBuilder notificationSummary = new StringBuilder();
                Iterator<CharSequence> iterator = signalNames.iterator();
                int signalsLeft = sHighExposureNotificationHistory.size();
                CharSequence signalName = iterator.next();
                do {
                    signalsLeft--;
                    notificationSummary.append(signalName);
                    if (iterator.hasNext()) {
                        notificationSummary.append(", ");
                        signalName = iterator.next();
                    } else {
                        break;
                    }
                } while (notificationSummary.length() + signalName.length() < 32);

                if (signalsLeft > 0) {
                    notificationSummary.append("... +").append(signalsLeft);
                }

                notificationBuilder.setContentText(notificationSummary);
            }

            // We send the built notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(Const.EXPOSURE_NOTIFICATION_ID,
                    notificationBuilder.build());

            if (sHighExposureNotificationHistory.size() == Const.MAXIMUM_NOTIFICATION_HISTORY_SIZE) {
                sHighExposureNotificationHistory.remove(0);
            }
        } else {
            Log.d(TAG, "monitorCompletedMeasurement: Not notify: mMonitorsCompleted: " +
                    mMonitorsCompleted);
        }
    }

    /**
     * Generates the text to put in the notification to represent a signal source
     *
     * @param context The context used to retrieve the string resources
     * @param signal  The signal to generate the text for
     * @return the generated text
     */
    private static CharSequence notificationLineText(Context context, BaseProperty signal) {
        return String.format(context.getString(R.string.notification_line_text_format),
                signal.friendlySourceName(context),
                Tools.getExpositionInCurrentMetric(signal.dbm, true));
    }

    /**
     * This method contains all the logic to run a measurement in background. This method is called
     * by a foreground service to improve reliability.
     */
    public static void runMeasurementsInForegroundService() {
        // Things to do in the service here under
        PowerManager powerManager = (PowerManager) MainApplication.getContext().
                getSystemService(Context.POWER_SERVICE);

        Log.d(TAG, "runMeasurementsInForegroundService: Initial location state - latitude: " +
                LocationMonitor.getLatitude() + " longitude: " + LocationMonitor.getLongitude());

        Log.d(TAG, "runMeasurementsInForegroundService: " +
                "SettingsPreferenceFragment.get_MEASUREMENT_CYCLE_BACKGROUND()=" +
                SettingsPreferenceFragment.get_MEASUREMENT_CYCLE_BACKGROUND());

            /*
            Here is the logic of this code:
            1) In doze, Alarms cannot be scheduled less than 9 minutes away. I did not find any
            satisfactory way to let the location run for 30 seconds and then make all measurements.
            So, in Doze, we do not wait for 30 seconds, but make location measurements and signal
            measurements in the same alarm. The location might not be accurate, but this is the best
            I found. Note that we cannot afford to wait 9 minutes with the location monitor on, as
            it will drain the battery.

            2) If not in Doze, we start the location monitor for 30 seconds (this is another alarm
            scheduled in Const.LOCATION_DISCOVERY_DURATION (equal to 30 seconds) that will stop the
            location monitor). Then we make all signal measurements and stop the location monitor.
             */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager.isDeviceIdleMode()) {
            Log.d(TAG, "runMeasurementsInForegroundService: start background measurements in Doze");
            //getWakeLock(60);
            CellularMonitor.registerMyPhoneStateListener();
            LocationMonitor.startLocationMonitor(false);
            runAllMonitors(schedulerMode);
            LocationMonitor.stopLocationMonitor();
                /*
                if we enter doze, we must reset location scan alarm to false, because the next time
                we exit Doze, we must start with a location measurement.
                 */
            setLocationScanRunning(false);
            CellularMonitor.unregisterMyPhoneStateListener();
            scheduleBackgroundMeasurement(SettingsPreferenceFragment.get_MEASUREMENT_CYCLE_BACKGROUND());
            Log.d(TAG, "runMeasurementsInForegroundService: all measurements launched");
        } else {
            if (!isLocationScanRunning()) {
                Log.d(TAG, "runMeasurementsInForegroundService: start the location monitor for " +
                        Const.LOCATION_DISCOVERY_DURATION / 1000 + " seconds");
                // we acquire the wakelock for one minute assuming that all measurements can
                // be performed during that period.
                //getWakeLock(60);
                CellularMonitor.registerMyPhoneStateListener();
                // start the location monitor and let it run for Const.LOCATION_DISCOVERY_DURATION
                LocationMonitor.startLocationMonitor(false);
                setLocationScanRunning(true);
                scheduleBackgroundMeasurement(Const.LOCATION_DISCOVERY_DURATION);
            } else {
                Log.d(TAG, "runMeasurementsInForegroundService: runAllMonitors and stop location monitor in Background");
                Log.d(TAG, "runMeasurementsInForegroundService: Latest location state - latitude: " +
                        LocationMonitor.getLatitude() + " longitude: " + LocationMonitor.getLongitude());
                runAllMonitors(schedulerMode);
                LocationMonitor.stopLocationMonitor();
                setLocationScanRunning(false);
                CellularMonitor.unregisterMyPhoneStateListener();
                scheduleBackgroundMeasurement(SettingsPreferenceFragment.get_MEASUREMENT_CYCLE_BACKGROUND());
            }
        }
    }

    /**
     * Creates an alarm to be triggered at 7 am the next day so that the first time statistics
     * notification can be shown to the user
     */
    public static void createFirstTimeStatisticsNotificationAlarm() {
        Log.d(TAG, "createFirstTimeStatisticsNotificationAlarm: ");
        Calendar calendar = Calendar.getInstance();
        // get tomorrow's date
        calendar.add(Calendar.DATE, 1);

        // Notification to be shown at 7 am tomorrow
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Log.d(TAG, "createFirstTimeStatisticsNotificationAlarm: Creating an alarm at " + calendar.toString());

        Context context = MainApplication.getContext();
        Intent alarmIntent = new Intent(context, FirstTimeStatisticsAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | Tools.flag_immutable());
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        /*
        I use setExact and setExactAndAllowWhileIdle even though they are battery intensive
        because I assume that we want so. My assumptions are that, we have only one such alarm
        and we call this only once and hence the consumption of battery should not be so worrying

        Also I suspect that the android 9 power manager makes setAndAllowWhileIdle to not
        function well
        */
        int ALARM_TYPE = AlarmManager.RTC_WAKEUP;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(ALARM_TYPE, calendar.getTimeInMillis(), pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(ALARM_TYPE, calendar.getTimeInMillis(), pendingIntent);
        } else {
            am.set(ALARM_TYPE, calendar.getTimeInMillis(), pendingIntent);
        }
        Log.d(TAG, "createFirstTimeStatisticsNotificationAlarm: Done");
    }

    public static void showNewSourceDetectedNotification(int numberOfNewSourcesDetected) {
        Context context = MainApplication.getContext();

        // We are building the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                Const.NOTIFICATION_NEW_SOURCE_DETECTED_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_es_notification)
                .setContentTitle(context.getResources().getQuantityString(
                        R.plurals.notification_new_source_detected_title, numberOfNewSourcesDetected, numberOfNewSourcesDetected))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true); // delete the notification when we touch it.

        // action when we touch the notification (jump to statistics fragment)
        Intent notificationClickedIntent = new Intent(context,
                NotificationClickedBroadcastReceiver.class);
        Bundle notificationClickedExtras = new Bundle();
        notificationClickedExtras.putInt(Const.NOTIFICATION_ARG_KEY,
                Const.NOTIFICATION_NEW_SOURCE_DETECTED);
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DATE, -1);
        notificationClickedExtras.putLong(Const.NOTIFICATION_DATE_TIMEMILLIS_ARG_KEY,
                yesterdayCal.getTimeInMillis()); // we want to show stats of yesterday
        notificationClickedIntent.putExtras(notificationClickedExtras);

        PendingIntent notificationClickedPendingIntent = PendingIntent.getBroadcast(context,
                Const.NOTIFICATION_CLICKED_PENDING_INTENT_REQUEST_CODE_NEW_SOURCE_DETECTED,
                notificationClickedIntent, PendingIntent.FLAG_CANCEL_CURRENT | Tools.flag_immutable());

        notificationBuilder.setContentIntent(notificationClickedPendingIntent);

        // action when we dismiss the notification (by sliding it)
        Intent notificationDeletedIntent = new Intent(context,
                NotificationDeletedBroadcastReceiver.class);
        Bundle notificationDeletedExtras = new Bundle();
        notificationDeletedExtras.putInt(Const.NOTIFICATION_ARG_KEY, Const.NOTIFICATION_NEW_SOURCE_DETECTED);
        notificationDeletedIntent.putExtras(notificationDeletedExtras);
        PendingIntent notificationDeletedPendingIntent = PendingIntent.getBroadcast(context,
                Const.NOTIFICATION_DELETED_PENDING_INTENT_REQUEST_CODE_NEW_SOURCE_DETECTED,
                notificationDeletedIntent, PendingIntent.FLAG_CANCEL_CURRENT | Tools.flag_immutable());

        notificationBuilder.setDeleteIntent(notificationDeletedPendingIntent);

        setNotificationImportance(notificationBuilder, context,
                SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_NEW_SOURCE_IMPORTANCE());

        // We send the built notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(Const.STATISTICS_NEW_SOURCE_DETECTED_NOTIFICATION_ID,
                notificationBuilder.build());

        Log.d(TAG, "showNewSourceDetectedNotification: Done notifying the user");
    }

    /**
     * Manage the notification importance when before O, after O it is automatically managed
     * by the notification channel. Calling this method for O+ will have no impact.
     *
     * @param notificationBuilder The notification builder used to set the importance
     * @param context             The app context
     * @param soundPreference     A string representing the notification sound importance set in the
     *                            preference set by the user
     */
    public static void setNotificationImportance(NotificationCompat.Builder notificationBuilder,
                                                 Context context,
                                                 String soundPreference) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (context.getString(
                    R.string.PREF_VALUE_NOTIFICATION_IMPORTANCE_VIBRATION_ONLY).equals(soundPreference)) {
                notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else if (context.getString(
                    R.string.PREF_VALUE_NOTIFICATION_IMPORTANCE_SOUND_ONLY).equals(soundPreference)) {
                notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
            } else if (context.getString(
                    R.string.PREF_VALUE_NOTIFICATION_IMPORTANCE_VIBRATION_AND_SOUND).equals(soundPreference)) {
                notificationBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
            }
        }
    }


    // enum to identify the monitor that completed its measurement
    public enum MonitorType {
        CELLULAR_MONITOR,
        WIFI_MONITOR,
        BLUETOOTH_MONITOR
    }

    /**
     * Enum to set target to know which mode is running at the moment
     */
    public enum SchedulerMode {
        BACKGROUND,
        FOREGROUND,
        // The ON_DEVICE_BOOT mode is only used by the OnBootBroadcastReceiver,
        // we give details of the logic in startMeasurementScheduler()
        ON_DEVICE_BOOT
    }

    public static class NotificationDeletedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: in NotificationDeletedBroadcastReceiver");
            int notificationType = intent.getIntExtra(Const.NOTIFICATION_ARG_KEY, -1);

            if (notificationType == Const.NOTIFICATION_FIRST_TIME_STATISTICS) {
                // statistics notification deleted
                DbRequestHandler.dumpEventToDatabase(Const.EVENT_NOTIFICATION_FIRST_TIME_STATISTICS_DELETE);
            } else if (notificationType == Const.NOTIFICATION_HIGH_EXPOSURE) {
                // high exposure notification deleted
                sHighExposureNotificationHistory.clear();
                DbRequestHandler.dumpEventToDatabase(Const.EVENT_NOTIFICATION_HIGH_EXPOSURE_DELETE);
            } else if (notificationType == Const.NOTIFICATION_NEW_SOURCE_DETECTED) {
                Log.d(TAG, "onReceive: NotificationDeletedBroadcastReceiver notificationType = NOTIFICATION_NEW_SOURCE_DETECTED");
                DbRequestHandler.dumpEventToDatabase(Const.EVENT_NOTIFICATION_NEW_SOURCE_DETECTED_DELETE);
            }
        }
    }

    public static class NotificationClickedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: NotificationClickedBroadcastReceiver");
            int notificationType = intent.getIntExtra(Const.NOTIFICATION_ARG_KEY, -1);
            Log.d(TAG, "onReceive: NotificationClickedBroadcastReceiver notificationType = " + notificationType);

            if (notificationType == Const.NOTIFICATION_FIRST_TIME_STATISTICS) {
                Log.d(TAG, "onReceive: NotificationClickedBroadcastReceiver notificationType = statistics");
                // We came from statistics notification => We should take the user to statistics
                DbRequestHandler.dumpEventToDatabase(Const.EVENT_NOTIFICATION_FIRST_TIME_STATISTICS_CLICK);
            } else if (notificationType == Const.NOTIFICATION_HIGH_EXPOSURE) {
                Log.d(TAG, "onReceive: NotificationClickedBroadcastReceiver notificationType = exposure");
                // the classic exposure notification
                DbRequestHandler.dumpEventToDatabase(Const.EVENT_NOTIFICATION_HIGH_EXPOSURE_CLICK);
            } else if (notificationType == Const.NOTIFICATION_NEW_SOURCE_DETECTED) {
                Log.d(TAG, "onReceive: NotificationClickedBroadcastReceiver notificationType = NOTIFICATION_NEW_SOURCE_DETECTED");
                // We came from statistics notification => We should take the user to statistics
                DbRequestHandler.dumpEventToDatabase(Const.EVENT_NOTIFICATION_NEW_SOURCE_DETECTED_CLICK);
            }

            Intent mainActivityIntent = new Intent(context, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mainActivityIntent.putExtras(intent);
            context.startActivity(mainActivityIntent);
            Log.d(TAG, "onReceive: NotificationClickedBroadcastReceiver Done");
        }
    }

    public static class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "in AlarmReceiver.onReceive()");
            Log.d(TAG, "in AlarmReceiver.onReceive: intent.getAction()=" + intent.getAction() +
                    " intent.toString()=" + intent.toString());
            Log.d(TAG, "in AlarmReceiver.onReceive: extras=" + intent.getExtras());

            /*
            When we receive the alarm, we start a foreground service to run the measurements.

            The foreground service will call the method runMeasurementsInForegroundService
             */
            Intent serviceIntent = new Intent(context, ForegroundScanService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

        }
    }

    /**
     * An alarm receiver that is triggered at 7 am of the next day of the installation of the app.
     */
    public static class FirstTimeStatisticsAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "in FirstTimeStatisticsAlarmReceiver.onReceive()");
            Log.d(TAG, "in FirstTimeStatisticsAlarmReceiver.onReceive: intent.getAction()=" + intent.getAction() +
                    " intent.toString()=" + intent.toString());
            Log.d(TAG, "in FirstTimeStatisticsAlarmReceiver.onReceive: extras=" + intent.getExtras());

            /*
            When we receive the statistics alarm, we build a statistics notification and
            notify it to the user.
             */
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                    Const.NOTIFICATION_FEATURE_DISCOVERY_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_es_notification)
                    .setContentTitle(context.getString(R.string.notification_statistics_first_time_title))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true); // delete the notification when we touch it.

            // action when we touch the notification (jump to statistics fragment)
            Intent notificationClickedIntent = new Intent(context, NotificationClickedBroadcastReceiver.class);
            Bundle notificationClickedExtras = new Bundle();
            notificationClickedExtras.putInt(Const.NOTIFICATION_ARG_KEY, Const.NOTIFICATION_FIRST_TIME_STATISTICS);
            Calendar yesterdayCal = Calendar.getInstance();
            yesterdayCal.add(Calendar.DATE, -1);
            notificationClickedExtras.putLong(Const.NOTIFICATION_DATE_TIMEMILLIS_ARG_KEY,
                    yesterdayCal.getTimeInMillis());
            notificationClickedIntent.putExtras(notificationClickedExtras);
            PendingIntent notificationClickedPendingIntent = PendingIntent.getBroadcast(context,
                    Const.NOTIFICATION_CLICKED_PENDING_INTENT_REQUEST_CODE_FIRST_TIME_STAT,
                    notificationClickedIntent, PendingIntent.FLAG_CANCEL_CURRENT | Tools.flag_immutable());
            notificationBuilder.setContentIntent(notificationClickedPendingIntent);

            // action when we dismiss the notification (by sliding it)
            Intent notificationDeletedIntent = new Intent(context, NotificationDeletedBroadcastReceiver.class);
            Bundle notificationDeletedExtras = new Bundle();
            notificationDeletedExtras.putInt(Const.NOTIFICATION_ARG_KEY, Const.NOTIFICATION_FIRST_TIME_STATISTICS);
            notificationDeletedIntent.putExtras(notificationDeletedExtras);
            PendingIntent notificationDeletedPendingIntent = PendingIntent.getBroadcast(context,
                    Const.NOTIFICATION_DELETED_PENDING_INTENT_REQUEST_CODE_FIRST_TIME_STAT,
                    notificationDeletedIntent, PendingIntent.FLAG_CANCEL_CURRENT | Tools.flag_immutable());
            notificationBuilder.setDeleteIntent(notificationDeletedPendingIntent);

            setNotificationImportance(notificationBuilder, context,
                    context.getString(R.string.PREF_VALUE_NOTIFICATION_IMPORTANCE_NONE));

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(Const.STATISTICS_FIRST_NOTIFICATION_ID, notificationBuilder.build());

            // As we've shown the first Statistics to the user, we set the shared preference to
            // true so that this notification is no more triggered
            MainApplication.getContext().getSharedPreferences(Const.SHARED_PREF_FILE,
                            Context.MODE_PRIVATE).edit()
                    .putBoolean(Const.IS_FIRST_TIME_STATISTICS_NOTIFICATION_SHOWN, true).apply();
            Log.d(TAG, "onReceive: IS_FIRST_TIME_STATISTICS_NOTIFICATION_SHOWN set to true");

            Log.d(TAG, "FirstTimeStatisticsAlarmReceiver onReceive: Done notifying the user");
        }
    }
}

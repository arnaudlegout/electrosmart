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

package fr.inria.es.electrosmart.services;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;

public class ForegroundScanService extends Service {
    private static final String TAG = "ForegroundScanService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "in onStartCommand");

        Notification notification = new NotificationCompat.Builder(this,
                Const.FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.foreground_service_notification_title))
                .setContentText(getString(R.string.foreground_service_notification_text))
                .setSmallIcon(R.drawable.ic_es_notification)
                .setOngoing(true)
                .build();
        startForeground(Const.FOREGROUND_SERVICE_ID, notification);
        Log.d(TAG, "Foreground service started");


        // If this is the first run of the foreground service of the day, we compute the
        // top five signals of yesterday and notify in case there are new top 5 signals.
        if (!DateUtils.isToday(Tools.getLastForegroundScanServiceRunAtTimeStamp())) {
            Log.d(TAG, "onStartCommand: This is the first foreground service run of the day");

            // 1. - Compute the top5Signals of yesterday and store them in the DB
            int numberOfNewTop5Signals = DbRequestHandler.computeTop5SignalsAndDailyStatSummaryOnYesterday();

            // 2. Notify the user if a new source has been detected
            if (numberOfNewTop5Signals > 0 && SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_NEW_SOURCE()) {
                Log.d(TAG, "onStartCommand: numberOfNewTop5Signals > 0 and setting to show new source is ON. So, showing new source notif");
                MeasurementScheduler.showNewSourceDetectedNotification(numberOfNewTop5Signals);
            }
        }

        // store the current timestamp in a shared preference, so that we can check when to run
        // detect new sources in the top 5 signals
        SharedPreferences.Editor edit = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE).edit();
        edit.putLong(Const.LAST_FOREGROUND_SCAN_SERVICE_RUN_AT, System.currentTimeMillis()).apply();

        MeasurementScheduler.runMeasurementsInForegroundService();
        Log.d(TAG, "Stopping foreground service...");
        stopSelf();
        Log.d(TAG, "stopped");
        // If the service is stopped by the system due to resource constraints, it will not get
        // restarted by the system and waits for the new alarm to start it again.
        return START_NOT_STICKY;
    }
}

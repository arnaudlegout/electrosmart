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

package fr.inria.es.electrosmart.monitors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;


/**
 * This broadcast receiver is registered in the manifest file to receive broadcasts
 * - on boot: to start again the service
 * - when the power cable is plugged in or out: it is only logged, we don't take action on these
 * events for now.
 */
public class OnBootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "OnBootBroadcastReceiver";

    /*
    We can test any broadcast from the adb shell with
    > adb shell
    > am broadcast -a android.intent.action.BOOT_COMPLETED
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "in onReceive()");
        String str = intent.getAction();
        Log.d(TAG, "intent received: " + intent);

        /*
        ACTION_BOOT_COMPLETED is not received if the application is on a first install and
        has never be launched by the user. This is for security reasons to prevent unwanted
        apps to start automatically without the user knowing it
        https://commonsware.com/blog/2011/07/05/boot-completed-regression.html
        This is not a relevant issue in our case.

        Also if the app is installed on an external storage, the ACTION_BOOT_COMPLETED is not
        received (to test). In that case, we must write in the manifest file, that the app
        cannot be installed on an external storage: android:installLocation="internalOnly"
        */
        if (Intent.ACTION_BOOT_COMPLETED.equals(str)) {
            DbRequestHandler.dumpEventToDatabase(Const.EVENT_DEVICE_BOOTED);

            Log.d(TAG, "ACTION_BOOT_COMPLETED");
            SharedPreferences settings = MainApplication.getContext().
                    getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
            if (settings.getBoolean(Const.IS_AGREEMENT_FLOW_DONE, false)) {
                Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is true, we start the service ON_DEVICE_BOOT" +
                        " and create a sync alarm");
                MeasurementScheduler.startMeasurementScheduler(
                        MeasurementScheduler.SchedulerMode.ON_DEVICE_BOOT,
                        null);
            } else {
                Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is false, we DO NOT start the " +
                        "service ON_DEVICE_BOOT.");
            }
            // The ACTION_POWER_CONNECTED and ACTION_POWER_DISCONNECTED can be tested with the
            // following adb commands
            // > adb shell dumpsys battery unplug
            // > adb shell dumpsys battery reset
        } else if (Intent.ACTION_POWER_CONNECTED.equals(str)) {
            Log.d(TAG, "ACTION_POWER_CONNECTED");
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(str)) {
            /*
            When the power is disconnected and we are in background, we must stop all battery
            consuming activities because when the power is connected we might start in background
            power consuming activities, such as the GPS, that must be stopped when there is no
            more power sources.
            */
            Log.d(TAG, "ACTION_POWER_DISCONNECTED");
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                Log.d(TAG, "We are in background mode, so we stop power consuming activities");
                LocationMonitor.stopLocationMonitor();
                CellularMonitor.unregisterMyPhoneStateListener();
            }
        } else {
            Log.d(TAG, "Received unknown intent: " + intent);
        }
    }
}

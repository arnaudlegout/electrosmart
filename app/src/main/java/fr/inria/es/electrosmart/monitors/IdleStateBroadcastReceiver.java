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
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import fr.inria.es.electrosmart.MainApplication;

/**
 * This broadcast receiver is registered dynamically in MainApplication (see explanation below).
 * It received broadcasts when
 * - Doze state changes
 * - Power save mode changes
 * <p/>
 * For now, we only log the events, we do not take actions.
 * <p/>
 * The PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED and PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
 * are only received by registered BroadcastReceiver, that is receivers dynamically registered.
 * We can identify such Intents in the source code because they have the flag
 * intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY). See DeviceIdleController.java that manages
 * Doze.
 * <p/>
 * Registering a Broadcast receiver in the Manifest file is not working for such broadcasts.
 */
public class IdleStateBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "IdleStateBroadcastRcv";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "in IdleStateBroadcastReceiver.onReceive()");

        PowerManager pm = (PowerManager) MainApplication.getContext().getSystemService(Context.POWER_SERVICE);


        String str = intent.getAction();
        Log.d(TAG, "intent received: " + intent);

            /*
            ACTION_DEVICE_IDLE_MODE_CHANGED is enabled when the device enter or exit Doze.

            NOTE: Doze and IDLE mode are equivalent terms.

            The ACTION_DEVICE_IDLE_MODE_CHANGED can be tested with the following adb command
            > adb shell dumpsys battery unplug (then switch off the screen)
            > adb shell dumpsys deviceidle step (multiple times until the returned state is IDLE)

            To disable IDLE mode we can use the command line:
            > adb shell dumpsys deviceidle disable

            To see all commands run:
            > adb shell dumpsys deviceidle -h

            Note that the step command enables to go through the different states of Doze
            > adb shell dumpsys deviceidle step

            STATE_ACTIVE  Device is currently active.
            STATE_INACTIVE Device is inactive (screen off, no motion) and we are waiting to for idle.
            STATE_IDLE_PENDING Device is past the initial inactive period, and waiting for the next idle period.
            STATE_SENSING Device is currently sensing motion.
            STATE_LOCATING Device is currently finding location (and may still be sensing).
            STATE_IDLE Device is in the idle state, trying to stay asleep as much as possible.
            STATE_IDLE_MAINTENANCE Device is in the idle state, but temporarily out of idle to do regular maintenance.

            The implementation of Doze is in DeviceIdleController.java
            */
        if (PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED.equals(str)) {
            Log.d(TAG, "ACTION_DEVICE_IDLE_MODE_CHANGED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "isDeviceIdleMode: " + pm.isDeviceIdleMode());
                /*
                When we enter idle mode, we must stop all battery consuming activities because our
                service will be paused during the entire idle mode, but the registered services
                (such as location of phoneStateListener) will still be registered an might consume
                battery.

                The broadcast ACTION_DEVICE_IDLE_MODE_CHANGED is received on IDLE and
                IDLE_MAINTENANCE, but the method isDeviceIdleMode() returns true only on IDLE.
                 */
                if (pm.isDeviceIdleMode()) {
                    LocationMonitor.stopLocationMonitor();
                    CellularMonitor.unregisterMyPhoneStateListener();
                }
            }

              /*
                ACTION_POWER_SAVE_MODE_CHANGED is enabled when the device enter or exit the
                Battery saver (settings -> Battery -> Battery saver.
               */
        } else if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(str)) {
            Log.d(TAG, "ACTION_POWER_SAVE_MODE_CHANGED");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "isPowerSaveMode: " + pm.isPowerSaveMode());
            }
        }

    }
}
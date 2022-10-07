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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import fr.inria.es.electrosmart.Const;

/**
 * A broadcast listener that listens for {@link android.net.wifi.WifiManager#NETWORK_STATE_CHANGED_ACTION}
 * <p>
 * When we are connected over WiFi and we are ready to upload data, we do the following:
 * 1. unregister the receiver
 * 2. start the sync
 * 3. create a new alarm
 */
public class WiFiStateChangeBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "WiFiStateChangeReceiver";

    public WiFiStateChangeBroadcastReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            Log.d(TAG, "onReceive()");
            NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            /*
            netInfo.isConnected() returns true when we are connected to the network (in our case,
            Wi-Fi). However, this method does not detect when we are connected to a captive portal
            and not yet authenticated. In that case, it will return true even if we cannot actually
            upload data to our server. This is the simplest strategy we are considering as taking
            into account the case of captive portal would require a much more sophisticated logic
            such as test of connectivity to www.google.com and periodic schedule of this test
            as long as it is false.

            Test results whether NETWORK_STATE_CHANGED_ACTION can detect the presence of captive portal.

            - The NETWORK_STATE_CHANGED_ACTION can not detect between events such as connected to
            a network like INRIA and connected to INRIA and having Internet connectivity. However,
            INRIA access point, in my opinion may not be a common implementation for captive portal.

            - There is a new API (android.net.NetworkCapabilities) available since android 21 that
             can probably tell whether we have an active Internet connection or not. An example of
             this is:

                ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Network = cm.getActiveNetwork();
                final NetworkCapabilities capabilities =
                        connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null && capabilities.hasCapability(NET_CAPABILITY_INTERNET)){
                    // we have Internet
                }

                See https://developer.android.com/reference/android/net/NetworkCapabilities.html

             */
            if (netInfo != null && netInfo.isConnected()) {
                Log.d(TAG, "onReceive(): isConnected() == true. Ready to do a sync.");
                SyncUtils.unregisterWiFiStateChangeBroadcastReceiver();
                SyncUtils.requestManualDataSync(true);
                SyncUtils.createSyncAlarm(SyncUtils.getRandomSyncTime(
                        Const.MIN_DURATION_FOR_NEXT_SYNC,
                        Const.SYNC_RANDOMIZATION));
            }
        }
    }
}

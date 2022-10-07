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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;
import fr.inria.es.electrosmart.signalhandler.RawSignalHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;

/*
    TEST CASES (test on API 16 and API >= 18)
    TC1 : 0) initial setup: no wifi scan in background
                            Const.DO_AGGRESSIVE_WIFI_CARD_SCAN = true,
                            Const.MEASUREMENT_CYCLE_BACKGROUND = 60 * 1000,
                            Wifi card OFF
          1) start ES -> wifi on + get wifi signals
          2) stop ES -> wifi off
          3) wait for BACKGROUND service -> wifi on during measurement that goes off when
             measurement done + get wifi signals
          4) wait for BACKGROUND service, start ES app when wifi is on, stop ES -> wifi is off
          5) wifi on, start ES, stop ES -> wifi is on
          6) wifi on, wait for the BACKGROUND service -> after the BACKGROUND measurement, wifi is on

    TC2 : 0) initial setup: wifi scan in background,
                            Const.DO_AGGRESSIVE_WIFI_CARD_SCAN = true,
                            Const.MEASUREMENT_CYCLE_BACKGROUND = 60 * 1000,
                            Wifi card OFF
          1) start ES -> wifi off + get wifi signals
          2) stop ES -> wifi off
          3) wait for BACKGROUND service -> wifi off during measurement that stays off when
             measurement done + get wifi signals
          5) wifi on, start ES, stop ES -> wifi is on
          6) wifi on, wait for the BACKGROUND service -> after the BACKGROUND measurement, wifi is on

    TC3 : 0) initial setup: no wifi scan in background,
                            Const.DO_AGGRESSIVE_WIFI_CARD_SCAN = false,
                            Const.MEASUREMENT_CYCLE_BACKGROUND = 60 * 1000,
                            Wifi card OFF
          1) start ES -> wifi on + get wifi signals
          2) stop ES -> wifi off
          3) wait for BACKGROUND service -> wifi off + no wifi signals
          4) wait for BACKGROUND service to start measurement, start ES app -> wifi is on
          5) stop ES -> wifi is off
          6) wifi on, start ES, stop ES -> wifi is on
          7) wifi on, wait for the BACKGROUND service -> after the BACKGROUND measurement, wifi is on

    TC4 : 0) initial setup: wifi scan in background,
                            Const.DO_AGGRESSIVE_WIFI_CARD_SCAN = false,
                            Const.MEASUREMENT_CYCLE_BACKGROUND = 60 * 1000,
                            Wifi card OFF
          1) start ES -> wifi off + get wifi signals
          2) stop ES -> wifi off
          3) wait for BACKGROUND service -> wifi off + get wifi signals
          4) wait for BACKGROUND service to start measurement, start ES app -> wifi is off
          5) stop ES -> wifi is off
          6) wifi on, start ES, stop ES -> wifi is on
          7) wifi on, wait for the BACKGROUND service -> after the BACKGROUND measurement, wifi is on

 */
public final class WifiMonitor {
    private static final int WIFI_ALREADY_ENABLED = 0;
    private static final int WIFI_ENABLED = 1;
    private static final int WIFI_NOT_ENABLED = 2;
    private static final int WIFI_SCAN_ALWAYS_AVAILABLE = 3;
    private static final String TAG = "WifiMonitor";
    private static WifiReceiver wifiReceiver;

    private WifiMonitor() {
    }

    /**
     * Starting with Marshmallow, if the GPS location is turned off (the location setting in the phone),
     * then the Wifi scan will return an empty list.
     * See this bug report that specify it is a design choice of Android
     * https://code.google.com/p/android/issues/detail?id=185370
     *
     * @param doRequestScan if set to true, we request a Wi-Fi scan, otherwise, we just get the
     *                      result for the latest Wi-Fi scan. This is mainly used for Android P and
     *                      Q for which we cannot make frequent scans, but system apps can make them
     *                      and we can benefit from them.
     */
    public static void run(boolean doRequestScan) {
        Log.d(TAG, "in run() for  WifiMonitor. doRequestScan=" + doRequestScan);

        if (Tools.isAccessFineLocationGranted(MainApplication.getContext())) {
            WifiManager manager = (WifiManager) (MainApplication.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE));
            /*
            If we are in background, call enableWifi and start the scan.
            If we are in Foreground, check if the wifi card is enabled (in foreground mode this is
            the MeasurementFragment that is in charge of enabling the wifi card)
            */
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                int wifiCardState = enableWifi(false);
                // A wifi scan usually requires in the order of 3 seconds to complete and call the
                // broadcast receiver
                boolean isScanSuccessful = startScan(manager);
                Log.d(TAG, "starting a wifi scan in Background");
                Log.d(TAG, "wifiCardState: " + wifiCardState);
                Log.d(TAG, "isScanSuccessful: " + isScanSuccessful);

                if (isScanSuccessful && wifiCardState != WIFI_NOT_ENABLED) {
                    SharedPreferences.Editor edit = MainApplication.getContext()
                            .getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE).edit();
                    Log.d(TAG, "ON_GOING_WIFI_BACKGROUND_SCAN set to true");
                    edit.putBoolean(Const.ON_GOING_WIFI_BACKGROUND_SCAN, true).apply();
                } else {
                    // either the startScan returned a false, implying that the WiFiScan was
                    // unsuccessful or wifi card is not enabled or both.
                    // For all these cases, we say that the measurement is completed
                    MeasurementScheduler.monitorCompletedMeasurement(MeasurementScheduler.MonitorType.WIFI_MONITOR, null);
                }

            } else {
/*                Log.d(TAG, "android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2: " +
                        (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2));
                Log.d(TAG, " manager.isScanAlwaysAvailable(): " +  manager.isScanAlwaysAvailable());*/
                // in FOREGROUND the Wifi card must be switched on by MeasurementFragment.
                if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED ||
                        manager.getWifiState() == WifiManager.WIFI_STATE_ENABLING ||
                        (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                                manager.isScanAlwaysAvailable())) {
                    Log.d(TAG, "starting a wifi scan in foreground");
                    if (doRequestScan) {
                        Log.d(TAG, "run: we request a Wi-Fi scan");
                        startScan(manager);
                    } else {
                        Log.d(TAG, "run: we just get the result for the latest Wi-Fi scan");
                        getAndProcessScanResults();
                    }
                } else {
                    Log.w(TAG, "we are in FOREGROUND, but wifi card not enabled and isScanAlwaysAvailable() is false!");
                }
            }
        } else {
            // The permission is not granted and if we are in background, so we say the
            // measurement is completed
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                MeasurementScheduler.monitorCompletedMeasurement(MeasurementScheduler.MonitorType.WIFI_MONITOR, null);
            }
            Log.d(TAG, "Permission fine location not granted");
        }
    }

    /**
     * Start a scan if another scan is not already running.
     *
     * @param manager the WifiManager used to start the scan
     * @return true is the scan is successful (it does not mean that the scan will return data,
     * but that the call to the method did not produce an error), false otherwise
     */
    private static boolean startScan(WifiManager manager) {
        registerWifiReceiver();
        boolean isScanSuccessful = manager.startScan();
        if (isScanSuccessful) {
            Log.d(TAG, "Wifi scan starting");
        } else {
            Log.d(TAG, "Wifi scan FAILED");
        }
        return isScanSuccessful;
    }

    /**
     * This method must be called before a scan to register the broadcast receiver that
     * will receive the results of the scan.
     * <p/>
     * The receiver is registered only once. All subsequent calls to this method will no more
     * register the receiver. The receiver will be unregistered in the stopMeasurementScheduler()
     * method of {@link MeasurementScheduler} and when a scan is complete (that is the onReceive
     * method of the broadcast receiver is returning)
     */
    private static void registerWifiReceiver() {
        Log.d(TAG, "in registerWifiReceiver");
        if (wifiReceiver == null) {
            Log.d(TAG, "registering wifiReceiver");
            wifiReceiver = new WifiReceiver();
            MainApplication.getContext().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        } else {
            Log.d(TAG, "wifiReceiver already registered");
        }
    }

    /**
     * Unregister the WifiReceiver
     */
    public static void unregisterWifiReceiver() {
        Log.d(TAG, "in unregisterWifiReceiver");
        // unregister the Wifi broadcast receiver
        if (wifiReceiver != null) {
            MainApplication.getContext().unregisterReceiver(wifiReceiver);
            wifiReceiver = null;
        }
    }

    /**
     * This method enable the wifi card if required and set the initial state of the variable
     * WAS_WIFI_ENABLED_BEFORE_SCAN that will be used to revert the wifi card state when
     * restoreInitialWifiCardState will be called. This method must always be called before making
     * a Wifi scan, the method will handle all special cases (such as isScanAlwaysAvailable).
     * <p/>
     * The logic is simple and articulates around enableWifi() and restoreInitialWifiCardState().
     * 1) If when enableWifi() is called the wifi card if enabled (or enabling) we don't take any
     * action, and restoreInitialWifiCardState() will not change the state of the wifi card when
     * called. This way, even if a user decide to switch off the wifi card,
     * restoreInitialWifiCardState() will keep this user choice. Also, if  when switching to
     * current mode, a scan was enabled in background mode, we keep the state of
     * WAS_WIFI_ENABLED_BEFORE_SCAN so that we correctly revert to the initial user state
     * <p/>
     * 2) If when enableWifi() is called the wifi card is disabled (or disabling) AND
     * isScanAlwaysAvailable is false, we enable the wifi card. In that case,
     * restoreInitialWifiCardState() will disable the wifi card when called.
     * <p/>
     * 3) If the wifi card is disabled and isScanAlwaysAvailable is true, we don't change the state
     * of the wifi card in either methods.
     *
     * @param isForeground set to true is the method is called when the app is in foreground
     * @return WIFI_ALREADY_ENABLED is wifi is enabled before the call to this method or if
     * isScanAlwaysAvailable() is true, WIFI_ENABLED if the card has been enabled, and
     * if the wifi scan is possible (that is, wifi is already enabled or wifi was enabled),
     * WIFI_NOT_ENABLED if the card has not been enabled
     */
    public static int enableWifi(boolean isForeground) {
        Log.d(TAG, "in enableWifi()");
        WifiManager manager = (WifiManager) (MainApplication.getContext().getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE));
        SharedPreferences settings = MainApplication.getContext()
                .getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();

        Log.d(TAG, "airplane mode: " + Tools.isAirplaneModeActivated(MainApplication.getContext()));
        Log.d(TAG, "get_DO_AGGRESSIVE_WIFI_CARD_SCAN: " + SettingsPreferenceFragment.get_DO_AGGRESSIVE_WIFI_CARD_SCAN());
        // check is the Wifi card is disabled or disabling
        if (manager.getWifiState() == WifiManager.WIFI_STATE_DISABLED ||
                manager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {

            // this variable must be set for both FOREGROUND and BACKGROUND modes
            edit.putBoolean(Const.WAS_WIFI_ENABLED_BEFORE_SCAN, false).apply();

            // check is isScanAlwaysAvailable() is true
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                    manager.isScanAlwaysAvailable()) {
                Log.d(TAG, "isScanAlwaysAvailable() is true");
                return WIFI_SCAN_ALWAYS_AVAILABLE;
            } else {
                Log.d(TAG, "MeasurementScheduler.schedulerMode: " + MeasurementScheduler.schedulerMode);
                if ((isForeground || SettingsPreferenceFragment.get_DO_AGGRESSIVE_WIFI_CARD_SCAN()) &&
                        !Tools.isAirplaneModeActivated(MainApplication.getContext())) {
                    Log.d(TAG, "enabling Wifi");
                    // if all conditions are true, we enable wifi
                    manager.setWifiEnabled(true);
                    return WIFI_ENABLED;
                } else {
                    Log.d(TAG, "we don't enable the wifi card!");
                    return WIFI_NOT_ENABLED;
                }
            }
        } else {
            // Wifi card is ENABLED or ENABLING, we don't have anything to do
            Log.d(TAG, "Wifi already enabled");
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                /*
                if the wifi card is already enabled, but a BACKGROUND scan is pending, we don't
                override the WAS_WIFI_ENABLED_BEFORE_SCAN (because is the card might be enabled
                due to the pending background scan and not due to a user decision.
                 */
                if (!settings.getBoolean(Const.ON_GOING_WIFI_BACKGROUND_SCAN, false)) {
                    Log.d(TAG, "set WAS_WIFI_ENABLED_BEFORE_SCAN to true");
                    edit.putBoolean(Const.WAS_WIFI_ENABLED_BEFORE_SCAN, true).apply();
                    //edit.putBoolean(Const.ON_GOING_WIFI_BACKGROUND_SCAN, true).apply();
                }
            }
            return WIFI_ALREADY_ENABLED;
        }
    }

    /**
     * This method disable the wifi card if required. It must be called when we don't need to scan
     * wifi anymore. The details of the logic are given in the enableWifi() documentation.
     */
    public static void restoreInitialWifiCardState(boolean isForeground) {
        Log.d(TAG, "in restoreInitialWifiCardState()");
        SharedPreferences settings = MainApplication.getContext().getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean(Const.ON_GOING_WIFI_BACKGROUND_SCAN, false).apply();
        Log.d(TAG, "ON_GOING_WIFI_BACKGROUND_SCAN set to false");
        Log.d(TAG, "was_wifi_enabled_before_scan: " + settings.getBoolean(Const.WAS_WIFI_ENABLED_BEFORE_SCAN, true));
        WifiManager manager = (WifiManager) (MainApplication.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE));
        Log.d(TAG, "manager.getWifiState(): " + manager.getWifiState());
        if (!settings.getBoolean(Const.WAS_WIFI_ENABLED_BEFORE_SCAN, true)) {
            if ((android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !manager.isScanAlwaysAvailable())
                    || android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (isForeground || SettingsPreferenceFragment.get_DO_AGGRESSIVE_WIFI_CARD_SCAN()) {
                    Log.d(TAG, "disabling Wifi card");
                    manager.setWifiEnabled(false);
                }
            }
        }
    }

    /**
     * This broadcast receiver is used to receive the result of a Wifi scan, and call the
     * processRawSignals method
     * <p>
     * <p>
     * In case on exception during the process, this is the UncaughtExceptionHandler of
     * the MainApplication that will stop cleanly the application by reverting the cards and
     * unregistering all broadcast receivers.
     */
    static class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            Log.d(TAG, "Wifi results received in the WifiReceiver (BroadcastReceiver)");


            if (intent != null && WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                List<BaseProperty> wifiSignals = getAndProcessScanResults();

                // We are done! We revert back card state and unregister the listener when in background
                if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                    restoreInitialWifiCardState(false);
                    unregisterWifiReceiver();
                    MeasurementScheduler.monitorCompletedMeasurement(MeasurementScheduler.MonitorType.WIFI_MONITOR, wifiSignals);
                }
            }
        }
    }

    /**
     * This method gets the latest Wi-Fi scan result, process it, and send it to the RawSignalHandler
     *
     * @return wifiSignals the list of the Wi-Fi signals as WifiProperty objects.
     */
    private static List<BaseProperty> getAndProcessScanResults() {
        Log.d(TAG, "in getAndProcessScanResults()");

        // We call the orientation monitor to get the orientation measurement
        // as close as possible to the signal reception
        OrientationMonitor.getInstance().start();

        List<BaseProperty> wifiSignals = new ArrayList<>();
        WifiManager manager = (WifiManager) (MainApplication.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE));
        List<ScanResult> scanResults = null;

        // we retrieve the last Wi-Fi scan results
        if (manager != null) {
            scanResults = manager.getScanResults();
        }

        // If there is any Wi-Fi AP detected...
        if (scanResults != null) {
            Log.d(TAG, "getAndProcessScanResults: Nb received Wifi signals: " + scanResults.size());

            // we update the scan location
            LocationMonitor.updateCoordinatesWithLastKnownLocation();
            double latitude = LocationMonitor.getLatitude();
            double longitude = LocationMonitor.getLongitude();

            long now = System.currentTimeMillis();
            Log.d(TAG, "getAndProcessScanResults - latitude: " + latitude +
                    " longitude: " + longitude + " measured_time: " + now);

            /*
            We get the currently connected Wi-Fi network and extract from this network
            the SSID, BSSID, and frequency that we use to identify in the scanned Wi-Fi
            (below in the scanResults) the one to which we are connected.

            It is not clear from the documentation what is returning manager.getConnectionInfo()
            when there is no Wi-Fi connexion. Therefore, I handle the case of a null value
            to be robust to corner cases
            */
            WifiInfo currentWifi = manager.getConnectionInfo();

            /*
            the WifiInfo.getSSID() is poorly implemented and we need to make some heuristics
            around it to deal with this poor implementation.

            If the SSID can be decoded as UTF-8, it will be returned surrounded by double
            quotation marks. Otherwise, it is returned as a string of hex digits (without
            quotes). In case there is no connected network the SSID will be <unknown ssid>
            (without quotes).

            In the following, we only handle what I believe to be the regular and most
            frequent case, that is, an SSID that can be decoded as UTF8 and that is
            surrounded by quotes.
             */
            String currentSSID = "";
            String currentBSSID = "";
            int currentFrequency = -1;

            /*
            currentWifi.getSSID() and currentWifi.getBSSID() are never supposed to be null,
            but we suspect that bug #184 might be due to a wrong implementation of these
            methods that return null in some cases

            If ever bug #184 is later identified to be due to another issue, we can safely
            remove these tests and only test for (currentWifi != null)
            */
            if (currentWifi != null && currentWifi.getSSID() != null) {
                currentSSID = currentWifi.getSSID();
                if (currentSSID.startsWith("\"") && currentSSID.endsWith("\"")) {
                    currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
                }
            }

            if (currentWifi != null && currentWifi.getBSSID() != null) {
                currentBSSID = currentWifi.getBSSID();
            }

            if (currentWifi != null) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    currentFrequency = currentWifi.getFrequency();
                }
            }

            for (ScanResult result : scanResults) {
                if (result != null) {
                    /*
                    if the scanned network has the same SSID, same BSSID, and same frequency
                    (if available for API 21 and higher) we deem the scanned wifi is connected
                     */
                    boolean isConnected = false;
                    if (result.SSID.equals(currentSSID) && result.BSSID.equals(currentBSSID)
                            && (currentFrequency == -1 || result.frequency == currentFrequency)) {
                        isConnected = true;
                    }
                    String operatorFriendlyName = "";
                    String venueName = "";
                    int isPasspointNetwork = Const.INVALID_IS_PASSPOINT_NETWORK;
                    int centerFreq0 = Const.INVALID_CENTERFREQ0;
                    int centerFreq1 = Const.INVALID_CENTERFREQ1;
                    int channelWidth = Const.INVALID_CHANNELWIDTH;
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (result.operatorFriendlyName != null) {
                            operatorFriendlyName = result.operatorFriendlyName.toString();
                        }
                        if (result.venueName != null) {
                            venueName = result.venueName.toString();
                        }
                        isPasspointNetwork = result.isPasspointNetwork() ? 1 : 0;
                        centerFreq0 = result.centerFreq0;
                        centerFreq1 = result.centerFreq1;
                        channelWidth = result.channelWidth;
                        /*
                        Log.d(TAG, "onReceive:  centerFreq0 " + result.centerFreq0 +
                                " centerFreq1 " + result.centerFreq1 + " channelWidth " + result.channelWidth +
                                " operatorFriendlyName " + result.operatorFriendlyName +
                                " venueName " + result.venueName +
                                " isPasspointNetwork " + result.isPasspointNetwork());
                        Log.d(TAG, "onReceive: " + result);
                        */
                    }

                    int wifiStandard = Const.INVALID_WIFI_STANDARD;
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        wifiStandard = result.getWifiStandard();
                    }

                    WifiProperty wifiProperty = new WifiProperty(
                            result.SSID,
                            result.BSSID,
                            "",
                            operatorFriendlyName,
                            venueName,
                            isPasspointNetwork,
                            result.frequency,
                            centerFreq0,
                            centerFreq1,
                            channelWidth,
                            result.capabilities,
                            wifiStandard,
                            result.level,
                            isConnected,
                            latitude,
                            longitude,
                            true,
                            now);

                    //Log.d(TAG, "getAndProcessScanResults: wifiProperty: " + wifiProperty);
                    wifiSignals.add(wifiProperty);
                }
            }
        }

        // If there are scan results, we send them to processRawSignals()
        if (!wifiSignals.isEmpty()) {
            Log.d(TAG, "getAndProcessScanResults: we detected Wi-Fi signals");
            RawSignalHandler dataHandler = RawSignalHandler.getInstance();
            dataHandler.processRawSignals(wifiSignals, RawSignalHandler.SignalMonitorType.WIFI);
        } else {
            Log.d(TAG, "getAndProcessScanResults: no detected Wi-Fi signals");
            /*
            If WifiSignals is empty, we might be in a Wi-Fi white zone.

            NOTE: the detection of white zones is buggy for Wi-Fi as it is possible
            (starting with Android O, see BF214, to have regular scans with 0 detected
            signals). We don't know any way to detect that we are in such a case.
             */
            if (Tools.isAccessFineLocationGranted(MainApplication.getContext())) {
                if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
                    new DbRequestHandler.addWhiteZoneToDBAsyncTask().execute(Const.WHITE_ZONE_WIFI);
                } else {
                    Log.d(TAG, "onSensorChanged: we are in BACKGROUND, we addWhiteZoneToDB in current thread!");
                    DbRequestHandler.addWhiteZoneToDB(Const.WHITE_ZONE_WIFI);
                }
            }
        }
        return wifiSignals;
    }
}
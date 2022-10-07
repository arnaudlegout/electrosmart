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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;
import fr.inria.es.electrosmart.signalhandler.RawSignalHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.BluetoothProperty;

/*
    TEST CASES
    TC1 : 0) initial setup: Const.DO_AGGRESSIVE_BT_CARD_SCAN = true,
                            Const.MEASUREMENT_CYCLE_BACKGROUND = 60 * 1000,
                            BT card OFF
          1) start ES -> BT on + get BT signals
          2) stop ES -> BT off
          3) wait for BACKGROUND service -> BT on during measurement that goes off when
             measurement done + get BT signals
          4) wait for BACKGROUND service, start ES app when BT is on, stop ES -> BT is off
          5) BT on, start ES, stop ES -> BT is on
          6) BT on, wait for the BACKGROUND service -> after the BACKGROUND measurement, BT is on

    TC2 : 0) initial setup: Const.DO_AGGRESSIVE_BT_CARD_SCAN = false,
                            Const.MEASUREMENT_CYCLE_BACKGROUND = 60 * 1000,
                            BT card OFF
          1) start ES -> BT on + get BT signals
          2) stop ES -> BT off
          3) wait for BACKGROUND service -> BT off + no BT signals
          4) wait for BACKGROUND service to start measurement, start ES app -> BT is on
          5) stop ES -> BT is off
          6) BT on, start ES, stop ES -> BT is on
          7) BT on, wait for the BACKGROUND service -> get BT signals, after the BACKGROUND
          measurement, BT is on
 */

public final class BluetoothMonitor {

    private static final int BT_ALREADY_ENABLED = 0;
    private static final int BT_ENABLED = 1;
    private static final int BT_NOT_ENABLED = 2;
    private static final int NO_BT_ADAPTER = 3;
    private static final String TAG = "BluetoothMonitor";
    private static BluetoothReceiver bluetoothReceiver;
    private static BluetoothCardStateReceiver bluetoothCardStateReceiver;

    private BluetoothMonitor() {
    }

    /**
     * This method is the entry point of the monitor. The {@link MeasurementScheduler} calls this
     * method when a scan must be performed.
     */
    public static void run() {
        Log.d(TAG, "run BluetoothMonitor");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Tools.isAccessFineLocationGranted(MainApplication.getContext()) && bluetoothAdapter != null) {
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                Log.d(TAG, " starting a BT scan in background mode");
                /*
                The BluetoothCardStateReceiver will automatically start a scan when the BT card
                is enabled. We use it to deal with the delay between the time we enable the bluetooth
                adapter and the time it is actually enabled and ready for a scan.
                 */
                registerBluetoothCardStateReceiver();
                int btCardState = enableBluetooth(false);
                Log.d(TAG, "btCardState: " + btCardState);
                // If the card is not already enabled, the scan will be called when it will be
                // enabled by the BT card broadcast receiver
                if (btCardState == BT_ALREADY_ENABLED) {
                    boolean isScanSuccessful = startScan(bluetoothAdapter);
                } else {
                    MeasurementScheduler.monitorCompletedMeasurement(MeasurementScheduler.MonitorType.BLUETOOTH_MONITOR, null);
                }
            } else {
                // in FOREGROUND the BT card must be switched on by MeasurementFragment.
                if (bluetoothAdapter.isEnabled()) {
                    Log.d(TAG, " starting a BT scan in foreground mode");
                    startScan(bluetoothAdapter);
                } else {
                    Log.w(TAG, "we are in FOREGROUND, but BT card not enabled!");
                }
            }
        } else {
            if (!Tools.isAccessFineLocationGranted(MainApplication.getContext())) {
                Log.d(TAG, "Permission fine location not granted");
            }
            if (bluetoothAdapter == null) {
                Log.d(TAG, "bluetoothAdapter is null, no available BT adapter!");
            }
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                MeasurementScheduler.monitorCompletedMeasurement(MeasurementScheduler.MonitorType.BLUETOOTH_MONITOR, null);
            }
        }
    }

    /**
     * Start a scan if another scan is not already running.
     * <p>
     * WARNING: from
     * https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#startDiscovery()
     * Device discovery is a heavyweight procedure. New connections to remote Bluetooth devices
     * should not be attempted while discovery is in progress, and existing connections will
     * experience limited bandwidth and high latency.
     * <p>
     * We should consider the fact that background BT scans might disrupt existing BT connections.
     * For now, we are not taking any step to prevent these issues and we wait for possible user
     * feedback. It is also possible that with newer BT versions, the bandwidth is larger and
     * that a scan does no create any perceivable disruption.
     *
     * @param bluetoothAdapter the BluetoothAdapter used to start the scan
     * @return true is the scan is successful (it does not mean that the scan will return data,
     * but that the call to the method did not produce an error), false otherwise
     */
    private static boolean startScan(BluetoothAdapter bluetoothAdapter) {
        registerBluetoothReceiver();
        boolean isScanSuccessful = bluetoothAdapter.startDiscovery();
        if (isScanSuccessful) {
            /*
            We must set ON_GOING_BT_BACKGROUND_SCAN to true iif the bt card is enabled, because
            otherwise, the scan will not start. However, we don't need to test for the BT card
            state here because startScan is always called in the context in which the BT card
            is enabled. Note that this is different for the WifiMonitor, this is the reason why
            we manage ON_GOING_WIFI_BACKGROUND_SCAN in another place, directly in the run method.
            */
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                SharedPreferences.Editor edit = MainApplication.getContext()
                        .getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE).edit();
                edit.putBoolean(Const.ON_GOING_BT_BACKGROUND_SCAN, true).apply();
                Log.d(TAG, "ON_GOING_BT_BACKGROUND_SCAN set to true");
            }
            Log.d(TAG, "BT scan starting");
        } else {
            Log.d(TAG, "BT scan FAILED");
        }
        return isScanSuccessful;
    }

    /**
     * This method must be called before startScan() to register the broadcast receiver that
     * will receive the results of the scan.
     * <p/>
     * The receiver is registered only once. All subsequent calls to this method will no more
     * register the receiver. The receiver will be unregistered in the stopMeasurementScheduler()
     * method of {@link MeasurementScheduler}
     */
    private static void registerBluetoothReceiver() {
        Log.d(TAG, "in registerBluetoothReceiver");
        // we register the BluetoothReceiver for all events required for a BT scan
        if (bluetoothReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            bluetoothReceiver = new BluetoothReceiver();
            MainApplication.getContext().registerReceiver(bluetoothReceiver, filter);
            Log.d(TAG, "bluetoothReceiver created");
        } else {
            Log.d(TAG, "bluetoothReceiver already created");
        }
    }

    /**
     * Register the BluetoothCardStateReceiver broadcast receiver to monitor if the BT
     * card is switched on. When the card will be switched on, the scan will be
     * triggered by the broadcast receiver.
     * <p/>
     * The receiver is registered only once. All subsequent calls to this method will no more
     * register the receiver. The receiver will be unregistered in the stopMeasurementScheduler()
     * method of {@link MeasurementScheduler}
     */
    private static void registerBluetoothCardStateReceiver() {
        Log.d(TAG, "in registerBluetoothCardStateReceiver");
        if (bluetoothCardStateReceiver == null) {
            bluetoothCardStateReceiver = new BluetoothCardStateReceiver();
            MainApplication.getContext().registerReceiver(bluetoothCardStateReceiver,
                    new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            Log.d(TAG, "bluetoothCardStateReceiver created");
        } else {
            Log.d(TAG, "bluetoothCardStateReceiver already created");
        }
    }

    /**
     * This method unregister the BluetoothCardStateReceiver and the BluetoothReceiver
     */
    public static void unregisterAllBluetoothReceivers() {
        Log.d(TAG, "in unregisterAllBluetoothReceivers");
        // unregister the BT broadcast receivers
        Context context = MainApplication.getContext();
        if (bluetoothReceiver != null) {
            context.unregisterReceiver(bluetoothReceiver);
            bluetoothReceiver = null;
        }
        if (bluetoothCardStateReceiver != null) {
            context.unregisterReceiver(bluetoothCardStateReceiver);
            bluetoothCardStateReceiver = null;
        }
    }

    /**
     * This method enable the bluetooth card if required and set the initial state of the variable
     * was_bt_enabled_before_scan that will be used to revert the bluetooth card state when restoreInitialBTCardState
     * will be called. This method must always be called before making a Bluetooth scan, the method will
     * handle all special cases.
     * <p/>
     * The logic is simple and articulates around enableBluetooth() and restoreInitialBTCardState().
     * 1) If when enableBluetooth() is called the BT card if enabled (or enabling) we don't take any
     * action, and restoreInitialBTCardState() will not change the state of the BT card when called. This way,
     * even if a user decide to switch off the BT card,  restoreInitialBTCardState() will keep this user choice.
     * <p/>
     * 2) If when enableBluetooth() is called the BT card is disabled (or disabling), we enable
     * the BT card. In that case, restoreInitialBTCardState() will disable the BT card when called.
     *
     * @param isForeground set to true is the method is called when the app is in foreground
     * @return BT_ALREADY_ENABLED is BT is enabled before the call to this method,
     * BT_ENABLED if the card has been enabled, and
     * if the BT scan is possible (that is, BT is already enabled or BT was enabled,
     * BT_NOT_ENABLED is the card has not been enabled, NO_BT_ADAPTER if there is no adapter
     */
    public static int enableBluetooth(boolean isForeground) {
        Log.d(TAG, "in enableBluetooth()");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences settings = MainApplication.getContext()
                .getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();

        if (bluetoothAdapter != null) {
            // check if BT card is OFF or turning OFF
            if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF ||
                    bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF) {

                // this variable must be set for both FOREGROUND and BACKGROUND modes
                edit.putBoolean(Const.WAS_BT_ENABLED_BEFORE_SCAN, false).apply();

                Log.d(TAG, "airplane mode: " + Tools.isAirplaneModeActivated(MainApplication.getContext()));
                Log.d(TAG, "get_DO_AGGRESSIVE_BT_CARD_SCAN: " + SettingsPreferenceFragment.get_DO_AGGRESSIVE_BT_CARD_SCAN());

                if ((isForeground || SettingsPreferenceFragment.get_DO_AGGRESSIVE_BT_CARD_SCAN())
                        && !Tools.isAirplaneModeActivated(MainApplication.getContext())) {
                    Log.d(TAG, "enabling BT");
                    bluetoothAdapter.enable();
                    return BT_ENABLED;
                } else {
                    Log.d(TAG, "we don't enable the BT card!");
                    return BT_NOT_ENABLED;
                }
            } else {
                Log.d(TAG, "BT already enabled");
                if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                    /*
                    if the bt card is already enabled, but a BACKGROUND scan is pending, we don't
                    override the WAS_BT_ENABLED_BEFORE_SCAN (because is the card might be enabled
                    due to the pending background scan and not due to a user decision.
                    */
                    if (!settings.getBoolean(Const.ON_GOING_BT_BACKGROUND_SCAN, false)) {
                        Log.d(TAG, "set WAS_BT_ENABLED_BEFORE_SCAN to true in Background");
                        edit.putBoolean(Const.WAS_BT_ENABLED_BEFORE_SCAN, true).apply();
                        //edit.putBoolean(Const.ON_GOING_BT_BACKGROUND_SCAN, true).apply();
                    }
                }
                return BT_ALREADY_ENABLED;
            }
        } else {
            Log.d(TAG, "bluetoothAdapter is null, no available BT adapter!");
            return NO_BT_ADAPTER;
        }
    }


    /**
     * This method disable the BT card if required. It must be called when we don't need to scan
     * BT anymore. The details of the logic are given in the enableBluetooth() documentation.
     */
    public static void restoreInitialBTCardState(boolean isForeground) {
        Log.d(TAG, "restoreInitialBTCardState");
        SharedPreferences settings = MainApplication.getContext().getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean(Const.ON_GOING_BT_BACKGROUND_SCAN, false).apply();
        Log.d(TAG, "ON_GOING_BT_BACKGROUND_SCAN set to false");
        Log.d(TAG, "was_bt_enabled_before_scan: " + settings.getBoolean(Const.WAS_BT_ENABLED_BEFORE_SCAN, true));
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !settings.getBoolean(Const.WAS_BT_ENABLED_BEFORE_SCAN, true)) {
            //    if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON ||
            //            bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) {
            if ((isForeground && SettingsPreferenceFragment.get_PREF_KEY_ENABLE_BT_AUTOMATICALLY())
                    || SettingsPreferenceFragment.get_DO_AGGRESSIVE_BT_CARD_SCAN()) {
                Log.d(TAG, "disabling BT card");
                bluetoothAdapter.disable();
            }
        }
    }

    /**
     * Return the string representation of the given BluetTooth class.
     *
     * @param bt_class The bluetooth class to convert into a string representation
     * @return a string representation of a BT class.
     */
    public static String bTClassToName(int bt_class) {
        switch (bt_class) {
            case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
                return MainApplication.getContext().getResources().getString(R.string.COMPUTER_UNCATEGORIZED);
            case BluetoothClass.Device.COMPUTER_DESKTOP:
                return MainApplication.getContext().getResources().getString(R.string.COMPUTER_DESKTOP);
            case BluetoothClass.Device.COMPUTER_SERVER:
                return MainApplication.getContext().getResources().getString(R.string.COMPUTER_SERVER);
            case BluetoothClass.Device.COMPUTER_LAPTOP:
                return MainApplication.getContext().getResources().getString(R.string.COMPUTER_LAPTOP);
            case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
                return MainApplication.getContext().getResources().getString(R.string.COMPUTER_HANDHELD_PC_PDA);
            case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
                return MainApplication.getContext().getResources().getString(R.string.COMPUTER_PALM_SIZE_PC_PDA);
            case BluetoothClass.Device.COMPUTER_WEARABLE:
                return MainApplication.getContext().getResources().getString(R.string.COMPUTER_WEARABLE);
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                return MainApplication.getContext().getResources().getString(R.string.PHONE_UNCATEGORIZED);
            case BluetoothClass.Device.PHONE_CELLULAR:
                return MainApplication.getContext().getResources().getString(R.string.PHONE_CELLULAR);
            case BluetoothClass.Device.PHONE_CORDLESS:
                return MainApplication.getContext().getResources().getString(R.string.PHONE_CORDLESS);
            case BluetoothClass.Device.PHONE_SMART:
                return MainApplication.getContext().getResources().getString(R.string.PHONE_SMART);
            case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
                return MainApplication.getContext().getResources().getString(R.string.PHONE_MODEM_OR_GATEWAY);
            case BluetoothClass.Device.PHONE_ISDN:
                return MainApplication.getContext().getResources().getString(R.string.PHONE_ISDN);
            case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_UNCATEGORIZED);
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_WEARABLE_HEADSET);
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_HANDSFREE);
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_MICROPHONE);
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_LOUDSPEAKER);
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_HEADPHONES);
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_PORTABLE_AUDIO);
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_CAR_AUDIO);
            case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_SET_TOP_BOX);
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_HIFI_AUDIO);
            case BluetoothClass.Device.AUDIO_VIDEO_VCR:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_VCR);
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_VIDEO_CAMERA);
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_CAMCORDER);
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_VIDEO_MONITOR);
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER);
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_VIDEO_CONFERENCING);
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
                return MainApplication.getContext().getResources().getString(R.string.AUDIO_VIDEO_VIDEO_GAMING_TOY);
            case BluetoothClass.Device.WEARABLE_UNCATEGORIZED:
                return MainApplication.getContext().getResources().getString(R.string.WEARABLE_UNCATEGORIZED);
            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                return MainApplication.getContext().getResources().getString(R.string.WEARABLE_WRIST_WATCH);
            case BluetoothClass.Device.WEARABLE_PAGER:
                return MainApplication.getContext().getResources().getString(R.string.WEARABLE_PAGER);
            case BluetoothClass.Device.WEARABLE_JACKET:
                return MainApplication.getContext().getResources().getString(R.string.WEARABLE_JACKET);
            case BluetoothClass.Device.WEARABLE_HELMET:
                return MainApplication.getContext().getResources().getString(R.string.WEARABLE_HELMET);
            case BluetoothClass.Device.WEARABLE_GLASSES:
                return MainApplication.getContext().getResources().getString(R.string.WEARABLE_GLASSES);
            case BluetoothClass.Device.TOY_UNCATEGORIZED:
                return MainApplication.getContext().getResources().getString(R.string.TOY_UNCATEGORIZED);
            case BluetoothClass.Device.TOY_ROBOT:
                return MainApplication.getContext().getResources().getString(R.string.TOY_ROBOT);
            case BluetoothClass.Device.TOY_VEHICLE:
                return MainApplication.getContext().getResources().getString(R.string.TOY_VEHICLE);
            case BluetoothClass.Device.TOY_DOLL_ACTION_FIGURE:
                return MainApplication.getContext().getResources().getString(R.string.TOY_DOLL_ACTION_FIGURE);
            case BluetoothClass.Device.TOY_CONTROLLER:
                return MainApplication.getContext().getResources().getString(R.string.TOY_CONTROLLER);
            case BluetoothClass.Device.TOY_GAME:
                return MainApplication.getContext().getResources().getString(R.string.TOY_GAME);
            case BluetoothClass.Device.HEALTH_UNCATEGORIZED:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_UNCATEGORIZED);
            case BluetoothClass.Device.HEALTH_BLOOD_PRESSURE:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_BLOOD_PRESSURE);
            case BluetoothClass.Device.HEALTH_THERMOMETER:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_THERMOMETER);
            case BluetoothClass.Device.HEALTH_WEIGHING:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_WEIGHING);
            case BluetoothClass.Device.HEALTH_GLUCOSE:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_GLUCOSE);
            case BluetoothClass.Device.HEALTH_PULSE_OXIMETER:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_PULSE_OXIMETER);
            case BluetoothClass.Device.HEALTH_PULSE_RATE:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_PULSE_RATE);
            case BluetoothClass.Device.HEALTH_DATA_DISPLAY:
                return MainApplication.getContext().getResources().getString(R.string.HEALTH_DATA_DISPLAY);
            // Device.Major classes
            case BluetoothClass.Device.Major.MISC:
                return MainApplication.getContext().getResources().getString(R.string.MISC);
            case BluetoothClass.Device.Major.NETWORKING:
                return MainApplication.getContext().getResources().getString(R.string.NETWORKING);
            case BluetoothClass.Device.Major.PERIPHERAL:
                return MainApplication.getContext().getResources().getString(R.string.PERIPHERAL);
            case BluetoothClass.Device.Major.IMAGING:
                return MainApplication.getContext().getResources().getString(R.string.IMAGING);
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return MainApplication.getContext().getResources().getString(R.string.UNCATEGORIZED);
            default:
                return "";
        }
    }

    /**
     * This broadcast receiver is called when the BT card state changed. We use it to trigger a
     * startDiscovery after switching the BT card on.
     */
    static class BluetoothCardStateReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            Log.d(TAG, "in BluetoothStateChange onReceive()");
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int bluetooth_state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                Log.d(TAG, "bluetooth_state: " + bluetooth_state);
                if (bluetooth_state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "in BluetoothCardStateReceiver, BT state in now ON");
                    // registerBluetoothReceiver();

                    // start the scan. This is an asynchronous event that requires around 12s to complete
                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter != null) {
                        Log.d(TAG, "Starting discovery");
                        startScan(bluetoothAdapter);
                    }
                } else {
                    Log.d(TAG, "Cannot start discovery, the state of the BT adapter is not ON");
                }
            }
        }
    }

    /**
     * This broadcast receiver is called during the three phases of a scan (start, found, finished).
     * At the end of the scan period ACTION_DISCOVERY_FINISHED, we restore the BT card to its
     * initial state and unregister this receiver.
     * <p>
     * In case an exception is raised during the process, this is the UncaughtExceptionHandler of
     * the MainApplication that will stop cleanly the application by reverting the cards and
     * unregistering all broadcast receivers.
     */
    public static class BluetoothReceiver extends BroadcastReceiver {

        // The list that collects all the detected signals
        // and will contain all of them when the action
        // `BluetoothAdapter.ACTION_DISCOVERY_FINISHED` is received
        @NonNull
        private final List<BaseProperty> mSignals = new ArrayList<>();

        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "In BluetoothReceiver onReceive()");

            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "in ACTION_DISCOVERY_STARTED");
                mSignals.clear();
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d(TAG, "in ACTION_FOUND, a new BT bluetoothDevice found");
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass bluetoothClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MAX_VALUE);
                if (rssi == Short.MAX_VALUE) {
                    rssi = Integer.MAX_VALUE;
                }

                /*
                From https://developer.android.com/guide/topics/connectivity/bluetooth.html#FindingDevices
                Note that there is a difference between being paired and being connected:
                   * To be paired means that two devices are aware of each other's existence, have a
                     shared link-key that can be used for authentication, and are capable of
                     establishing an encrypted connection with each other.
                   * To be connected means that the devices currently share an RFCOMM channel and
                     are able to transmit data with each other. The current Android Bluetooth APIs
                     require devices to be paired before an RFCOMM connection can be established.
                     Pairing is automatically performed when you initiate an encrypted connection
                     with the Bluetooth APIs.

                Therefore, we only consider the bounded state in ElectroSmart. The rational is that
                we want to know which device are known by the user, not which device is currently
                connected.

                The method getBondedDevices()
                https://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html#getBondedDevices()
                is another way to find BT devices. It retrieves BT devices already bounded without
                the need to make a scan and even if the device is not discoverable. Therefore, it
                could have been a good complement of a discovery. However, there is
                no way to extract the RSSI from a BluetoothDevice returned by the getBondedDevices()
                method. The only one way to get the RSSI of BT devices is to make a discovery
                and to retrieve the RSSI on the intent received with ACTION_FOUND as made above with
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MAX_VALUE);
                 */
                if (bluetoothDevice != null) {
                    String bt_device_name = bluetoothDevice.getName();

                    String bt_device_name_alias = "";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bt_device_name_alias = bluetoothDevice.getAlias();
                    }

                    Log.d(TAG, "onReceive: bt_device_name_alias: " + bt_device_name_alias);
                    String bt_address = bluetoothDevice.getAddress();
                    int bt_device_class;
                    if (bluetoothClass != null) {
                        bt_device_class = bluetoothClass.getDeviceClass();
                    } else {
                        bt_device_class = Integer.MAX_VALUE;
                    }
                    int bt_bond_state = bluetoothDevice.getBondState();
                    int bt_device_type;
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        bt_device_type = bluetoothDevice.getType();
                    } else {
                        bt_device_type = Integer.MAX_VALUE;
                    }

                    LocationMonitor.updateCoordinatesWithLastKnownLocation();
                    double latitude = LocationMonitor.getLatitude();
                    double longitude = LocationMonitor.getLongitude();
                    long now = System.currentTimeMillis();
                    Log.d(TAG, "BluetoothReceiver.onReceive: latitude: " + latitude +
                            " longitude: " + longitude + " measured_time: " + now);
                    Log.d(TAG, "onReceive: bluetoothDevice: " + bluetoothDevice +
                            " bluetoothClass: " + bluetoothClass);

                    BaseProperty signal = new BluetoothProperty(
                            bt_device_name,
                            bt_device_name_alias,
                            bt_address,
                            bt_device_class,
                            bt_device_type,
                            bt_bond_state,
                            rssi,
                            latitude,
                            longitude,
                            true,
                            now);

                    mSignals.add(signal);

                    // We call the orientation monitor in the receiver to get the orientation
                    // measurement as close as possible to the signal reception
                    OrientationMonitor.getInstance().start();
                    RawSignalHandler dataHandler = RawSignalHandler.getInstance();
                    dataHandler.processRawSignals(Collections.singletonList(signal), RawSignalHandler.SignalMonitorType.BLUETOOTH);
                }
            }

            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "in ACTION_DISCOVERY_FINISHED");
                if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                    restoreInitialBTCardState(false);
                    unregisterAllBluetoothReceivers();
                    MeasurementScheduler.monitorCompletedMeasurement(MeasurementScheduler.MonitorType.BLUETOOTH_MONITOR, mSignals);
                }
            }
        }
    }
}

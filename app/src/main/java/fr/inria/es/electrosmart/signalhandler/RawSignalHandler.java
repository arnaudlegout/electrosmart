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

package fr.inria.es.electrosmart.signalhandler;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.BluetoothProperty;
import fr.inria.es.electrosmart.signalproperties.GsmProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;


public class RawSignalHandler {

    private static final String TAG = "RawSignalHandler";

    /*
    cachedRawSignals contains all raw signals that are cached to be displayed. A signal is cached
    as long its measured time is not older than the hysteresis duration for its signal type.
    For each AntennaDisplay, cachedRawSignals contains a set of all signals that must be displayed.
    The method removeOldCachedSignals() is in charge of removing the signals that are no more
    relevant to display. This is the method that implements the hysteresis.
    */
    private ConcurrentHashMap<MeasurementFragment.AntennaDisplay, HashSet<BaseProperty>> cachedRawSignals = new ConcurrentHashMap<>();

    // A variable to be used for temporary activation of the wardrive mode.
    private static boolean sTempWardrive;

    private RawSignalHandler() {
    }

    /**
     * Returns true if the wardrive mode has been temporarily activated, false otherwise
     *
     * @return true if the wardrive mode has been temporarily activated, false otherwise
     */
    public static boolean isTempWardrive() {
        return sTempWardrive;
    }

    /**
     * Enable the temporary wardrive mode. This mode removes the signals hysteresis and keep the
     * screen always on.
     *
     * @param tempWardrive true to enable the temporary wardrive mode, false otherwise.
     */
    public static void setTempWardrive(boolean tempWardrive) {
        sTempWardrive = tempWardrive;
    }

    /**
     * Method to make esDatahandler a singleton class
     *
     * @return : class instance of RawSignalHandler
     */
    public static RawSignalHandler getInstance() {
        return LazyHolder.instance;
    }

    /**
     * This method receives the raw signals from the services such as WifiMonitor,
     * BluetoothMonitor, etc. puts the rawSignals in the database, and update the cachedRawSignals
     * data structure.
     *
     * @param rawSignals                  : Arraylist containing the raw signals of specific types
     * @param rawSignalsSignalMonitorType : type of the monitor that called this method
     */
    public void processRawSignals(final List<BaseProperty> rawSignals, SignalMonitorType rawSignalsSignalMonitorType) {
        Log.d(TAG, "In processRawSignals called by " + rawSignalsSignalMonitorType);
        //Log.d(TAG, "In processRawSignals rawSignals " + rawSignals);

        // 1) Dump rawSignals to the database
        /*
        If we are in foreground, we update the database in an asynctask to do not block
        the UI. Indeed, on old phones, the StrictMode shows that the update can takes seconds
        to complete, which is way too disruptive. If we are in background, there is no need
        to update the database in another thread, therefore, I prefer to keep the logic
        simpler.
         */
        if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
            Log.d(TAG, "processRawSignals: start DB update async task");
            new dumpRawSignalsToDatabaseAsyncTask().execute(rawSignals);
            Log.d(TAG, "processRawSignals: end DB update async task");
        } else {
            Log.d(TAG, "processRawSignals: we are in BACKGROUND, we dumpRawSignalsToDatabase in current thread!");
            dumpRawSignalsToDatabase(rawSignals);
        }

        if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
            // 2) We modify in place the signals with an invalid dbm value with a new dbm value set
            //    to the minimum dbm value for the signal type minus 1 (so that it stays invalid).
            //    The goal is to display signals with an invalid dbm value to the end of the
            //    list of signals.
            for (BaseProperty es : rawSignals) {
                es.normalizeSignalWithInvalidDbm();
            }

            // 3) We remove from cachedRawSignals all signals that are too old to be displayed
            //Log.d(TAG, "before  removeOldCachedSignals, cachedRawSignals: " + cachedRawSignals);
            removeOldCachedSignals(cachedRawSignals);
            //Log.d(TAG, "after  removeOldCachedSignals, cachedRawSignals: " + cachedRawSignals);

            // 4) update cachedRawSignals with the fresh rawSignals just returned from a monitor
            //Log.d(TAG, "before  updateCachedSignals, cachedRawSignals: " + cachedRawSignals);
            updateCachedSignals(cachedRawSignals, rawSignals);
            //Log.d(TAG, "after  updateCachedSignals, cachedRawSignals: " + cachedRawSignals);
        }
    }

    /**
     * This method is used to update in current mode the UI.
     * <p/>
     * Note that the processes of getting raw signals with processRawSignals and updating the UI
     * with updateUIwithCachedRawSignals are asynchronous.
     */
    public void updateUIwithCachedRawSignals() {
        Log.d(TAG, "in updateUIwithCachedRawSignals()");
        //Log.d(TAG, "in updateUIwithCachedRawSignals(), cachedRawSignals: " + cachedRawSignals);

        // we need to call this method before calling the UI because in case the processRawSignals()
        // method is not called for a long time (e.g., in airplane mode), the cachedRawSignals will
        // not be purged of old signals
        removeOldCachedSignals(cachedRawSignals);

        // we update the UI iif we are in FOREGROUND
        if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {

            ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> allRawSignalsForUI = new ConcurrentHashMap<>();

            // Create the allRawSignalsForUI based on the cachedRawSignals
            for (MeasurementFragment.AntennaDisplay antenna : cachedRawSignals.keySet()) {
                // we copy all BaseProperty objects in the cachedRawSignals so that the objects
                // can be modified afterward (for instance to normalize the powers) without
                // impacting the cached BaseProperty objects.
                List<BaseProperty> signals = new ArrayList<>();
                for (BaseProperty signal : cachedRawSignals.get(antenna)) {
                    BaseProperty copied_signal = signal.copy();
                    signals.add(copied_signal);
                }
                allRawSignalsForUI.put(antenna, signals);
            }

            ResultReceiver resultReceiver = MainApplication.getResultReceiver();
            if (resultReceiver != null) {
                // esChart is requiring filledAntennas to guarantee that each curve can be
                // updated appropriately, otherwise with allRawSignalsForUI esChart will not be able
                // to correctly draw the curves.
                fillObjects(allRawSignalsForUI);

                Bundle bundle = new Bundle();
                bundle.putSerializable("signals", allRawSignalsForUI);
                resultReceiver.send(MainActivity.RECEIVED_RAW_SIGNALS, bundle);
                Log.d(TAG, "sent bundle to result receiver");
            }
        }
    }

    /**
     * remove all signals from cachedRawSignals that are older that a given threshold depending on
     * the signal types.
     *
     * @param cachedRawSignals data structure that will be updated according to the removed signals.
     *                         cachedRawSignals is updated by side effect.
     */
    private void removeOldCachedSignals(ConcurrentHashMap<MeasurementFragment.AntennaDisplay, HashSet<BaseProperty>> cachedRawSignals) {
        Log.d(TAG, "in removeOldCachedSignals()");
        for (MeasurementFragment.AntennaDisplay antennaDisplay : MeasurementFragment.AntennaDisplay.values()) {
            if (cachedRawSignals.get(antennaDisplay) != null) {
                // To remove elements of a Collection while iterating on it, the only safe way is to
                // use an iterator and remove on the iterator, see
                // http://docs.oracle.com/javase/tutorial/collections/interfaces/collection.html
                Iterator<BaseProperty> it = cachedRawSignals.get(antennaDisplay).iterator();
                while (it.hasNext()) {
                    BaseProperty signal = it.next();
                    long now = System.currentTimeMillis();
                    if (antennaDisplay.equals(MeasurementFragment.AntennaDisplay.WIFI)) {
                        if ((now - signal.measured_time > get_wifi_hysteresis_duration())) {
                            it.remove();
                        }
                    } else if (antennaDisplay.equals(MeasurementFragment.AntennaDisplay.BLUETOOTH)) {
                        if ((now - signal.measured_time > get_bt_hysteresis_duration())) {
                            it.remove();
                        }
                    } else {
                        if ((now - signal.measured_time > get_cellular_hysteresis_duration())) {
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the hysteresis duration taking into account the wardrive mode preference. When in
     * wardrive mode we use an hysteresis set to Const.MEASUREMENT_CYCLE_FOREGROUND + 1 second.
     * <p>
     * Indeed, the cachedRawSignals is filled asynchronously by the services, and the data is
     * displayed in the user interface every Const.MEASUREMENT_CYCLE_FOREGROUND ms. So, to be sure
     * we do not delete data before being displayed (unless the user interface is significantly
     * slowed down so that it exceeds the Const.MEASUREMENT_CYCLE_FOREGROUND cycle) we set the
     * hysteresis to the cycle + 1 second (every thing expressed in ms)
     */
    private long get_wifi_hysteresis_duration() {
        return SettingsPreferenceFragment.get_WARDRIVE_MODE() ?
                Const.MEASUREMENT_CYCLE_FOREGROUND + 1000 : Const.WIFI_HYSTERESIS_DURATION * 1000;
    }

    /**
     * Returns the hysteresis duration taking into account the wardrive mode preference
     */
    private long get_bt_hysteresis_duration() {
        return SettingsPreferenceFragment.get_WARDRIVE_MODE() ?
                Const.MEASUREMENT_CYCLE_FOREGROUND + 1000 : Const.BT_HYSTERESIS_DURATION * 1000;
    }

    /**
     * Returns the hysteresis duration taking into account the wardrive mode preference
     */
    private long get_cellular_hysteresis_duration() {
        return SettingsPreferenceFragment.get_WARDRIVE_MODE() ?
                Const.MEASUREMENT_CYCLE_FOREGROUND + 1000 : Const.CELLULAR_HYSTERESIS_DURATION * 1000;
    }

    /**
     * Update the data structure cachedRawSignals (by side effect) with the signals contained in
     * rawSignals.
     *
     * @param cachedRawSignals the data structure to be updated (by side effect)
     * @param rawSignals       the signals to add to cachedRawSignals
     */
    private void updateCachedSignals(ConcurrentHashMap<MeasurementFragment.AntennaDisplay, HashSet<BaseProperty>> cachedRawSignals,
                                     List<BaseProperty> rawSignals) {
        Log.d(TAG, "in updateCachedSignals()");
        if (rawSignals != null) {
            // Add the rawSignals to cachedRawSignals. If a signal is already in cachedRawSignals we
            // replace it with the most up to date. We say that a signal is already in
            // cachedRawSignals if the method equals() on the base properties return true.
            for (BaseProperty raw_signal : rawSignals) {
                HashSet<BaseProperty> signalsSet = cachedRawSignals.get(raw_signal.getAntennaDisplay());
                if (signalsSet == null) {
                    cachedRawSignals.put(raw_signal.getAntennaDisplay(), new HashSet<>(Collections.singletonList(raw_signal)));
                } else {
                    // if an antenna corresponding to raw_signal is already in signalsSet, we remove
                    // it and add the newest raw_signal instead.
                    // Note that removing raw_signal will not remove raw_signal, but the signal
                    // that is equal to raw_signal (but older)
                    if (signalsSet.contains(raw_signal)) {
                        signalsSet.remove(raw_signal);
                    }
                    signalsSet.add(raw_signal);
                }
            }
        }
    }

    private void dumpRawSignalsToDatabase(List<BaseProperty> rawSignals) {
        Log.d(TAG, "in dumpRawSignalsToDatabase");
        //Log.d(TAG, "in dumpRawSignalsToDatabase allRawSignals " + rawSignals);

        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();

        // We save allRawSignals to the database
        for (BaseProperty signal : rawSignals) {
            signal.dumpSignalToDatabase(db);
        }
        Log.d(TAG, "dumpRawSignalsToDatabase: END raw signals logged in the DB");
        //Log.d(TAG, "OUT dump allRawSignals " + allRawSignals);
    }

    /**
     * method to fill fake signal object for each type if particular signal type is not collected
     * at that moment. This method modify the allAntennas object in place for efficiency.
     *
     * @param allAntennas : hashmap containing current collected antennas
     */
    private void fillObjects(final ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> allAntennas) {
        for (MeasurementFragment.AntennaDisplay antenna : MeasurementFragment.AntennaDisplay.values()) {
            if (allAntennas.get(antenna) == null || allAntennas.get(antenna).size() == 0) {
                List<BaseProperty> temp = new ArrayList<>();
                if (antenna == MeasurementFragment.AntennaDisplay.WIFI) {
                    temp.add(new WifiProperty(false));
                } else if (antenna == MeasurementFragment.AntennaDisplay.BLUETOOTH) {
                    temp.add(new BluetoothProperty(false));
                } else if (antenna == MeasurementFragment.AntennaDisplay.CELLULAR) {
                    // we add a cellular signal. We selected arbitrarily a GSM one.
                    temp.add(new GsmProperty(false));
                }
                allAntennas.put(antenna, temp);
            }
        }
    }


    // defines the different types of services that return data
    public enum SignalMonitorType {
        WIFI,
        BLUETOOTH,
        CELLULAR
    }

    /**
     * Initialization-on-demand holder idiom
     * See the link below for more information:
     * https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
     */
    private static class LazyHolder {
        static final RawSignalHandler instance = new RawSignalHandler();
    }

    /*
      Asynchronous task to update the database.

      The order of execution described in
      https://developer.android.com/reference/android/os/AsyncTask.html
      guarantees that starting with HONEYCOMB (3.0) submitted tasks are executed sequentially on a
      single thread.

      I tested that even if the activity that started the asynctask is destroyed, that asynctask
      is correctly completing without exception.

      About the compiler warnings related to varargs methods (or non-reifiable types, or
      heap pollution) here are some explanation
      http://docs.oracle.com/javase/tutorial/java/generics/nonReifiableVarargsType.html
      http://docs.oracle.com/javase/7/docs/technotes/guides/language/non-reifiable-varargs.html

      For short, what I am doing is safe, but the compiler cannot check it at compile time, thus
      the warnings.
     */
    private class dumpRawSignalsToDatabaseAsyncTask extends AsyncTask<List<BaseProperty>, Void, Void> {
        @SafeVarargs
        protected final Void doInBackground(List<BaseProperty>... rawSignals) {
            Log.d(TAG, "doInBackground: dumpRawSignalsToDatabase starting DB dump...");
            dumpRawSignalsToDatabase(rawSignals[0]);
            Log.d(TAG, "doInBackground: dumpRawSignalsToDatabase DONE!");
            return null;
        }
    }
}

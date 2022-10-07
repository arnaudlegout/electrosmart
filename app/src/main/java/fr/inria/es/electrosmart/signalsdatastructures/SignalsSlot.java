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

package fr.inria.es.electrosmart.signalsdatastructures;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.signalhandler.WifiGroupHelper;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.GsmProperty;
import fr.inria.es.electrosmart.signalproperties.WcdmaProperty;

/**
 * Instances of this class represent all signals available for a single time slot. It is used
 * exclusively by the UI.
 */
public class SignalsSlot {
    private static final String TAG = "SignalsSlot";

    // All data structures are given for the single time slot defined by the instance of this class.

    // For child signals, it is always a regular signal for all signal types.
    // For group signals, it is a regular signal for all signal types, but Wi-Fi. For Wi-Fi it is
    // always a WiFiGroupProperty representing a group of Wi-Fi signals for the same antenna. 

    // TODO: NEW CHART if the new chart does not need to have invalid signals for antennas with no measurements
    // we can make all computation from sortedGroupViewSignals and remove the groupViewSignals data structure

    // groupViewSignals represents the highest power signal for each antenna (including Wi-Fi with
    // WiFiGroupProperty signals).
    // This is the data structure to use for the chart as it contains an entry for each antenna.
    // When there is no signal for this antenna, the entry is a BaseProperty with the isValid flag
    // set to false.
    private ConcurrentHashMap<MeasurementFragment.AntennaDisplay, BaseProperty> groupViewSignals;

    // childViewSignals represents the list of all signals for each antenna (excluding Wi-Fi,
    // Wi-Fi signals are in childViewWifiSignals)
    private ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> childViewSignals;

    // childViewWifiSignals represents the list of all Wi-Fi signals. As we aggregate Wi-Fi signals
    // per group, the keys is a WiFiGroupProperty representing the group (its SSID and BSSID
    // are defined according to the group), and the values represent the list of Wi-Fi signals
    // for the given group.
    private ConcurrentHashMap<BaseProperty, List<BaseProperty>> childViewWifiSignals;

    // sortedGroupViewSignals represents the list of the highest signal per antenna (including
    // Wi-Fi with WiFiGroupProperty signals), highest signal first. Note that this data structure
    // is used for the display of the group views, it is not used for the chart as all the invalid
    // signals are removed (note that invalid signals are required by the chart for antenna with
    // no measurements).
    private List<BaseProperty> sortedGroupViewSignals;

    // sortedWifiGroupSignals represents the list of the Wi-Fi groups (i.e., the WiFiGroupProperty
    // representing the Wi-Fi groups), highest signal first.
    private List<BaseProperty> sortedWifiGroupSignals;

    // topSignal represents the highest signal for all antennas (including Wi-Fi with
    // a WiFiGroupProperty).
    private BaseProperty topSignal;

    // Set to true when the SignalsSlot contains valid signals, false otherwise (e.g., when it is
    // created for a pause event)
    private boolean containsValidSignals = false;

    /*
    Contains the sum of all valid and in-range dbm values in this slot. Note that a signal
    can be valid, but its dbm value might be out of range. The notion of valid signal is not
    linked the the dbm value. A valid signal is just a signal that has been measured, yet its
    dbm value might be incorrect.

    As the slotCumulativeTotalDbmValue is the sum of all in-range dbm values, its value is always
    valid even if it might exceeds the maximum displayable dbm value Const.MAX_DISPLAYABLE_DBM
    */
    private int slotCumulativeTotalDbmValue = 0;

    // same as slotCumulativeTotalDbmValue but un-rounded (so with a higher precision)
    private double slotCumulativeTotalMwValue = 0;

    // same as slotCumulativeTotalMwValue, but for Wi-Fi only, Bluetooth only, and cellular only
    private double slotCumulativeWifiMwValue = 0;
    private double slotCumulativeBluetoothMwValue = 0;
    private double slotCumulativeCellularMwValue = 0;

    // contains the five most exposing signals in this slot (for Wi-Fi signals it contains
    // a WiFiGroupProperty)
    private List<BaseProperty> topFiveSignals;

    public SignalsSlot() {
    }

    /**
     * Constructor for a SignalsSlot build on rawSignals.
     * <p>
     * The rawSignals must contain for each antenna type the raw list of signals for this antenna.
     * The constructor will build all the data structures used to display the signals in the UI.
     * <p>
     * Note on the algorithmic of this code: we replicate multiple times loops whereas we could
     * factorize some of them to have a more efficient code. This is intentional as I prefer code
     * clarity on code performance because performance is not a practical issue here.
     *
     * @param rawSignals used to construct a new SignalsSlot
     */
    public SignalsSlot(ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> rawSignals) {

        // we check that the rawSignals contains at least one valid signal, otherwise,
        // the SignalsSlot will be an object with containsValidSignals set to false
        for (MeasurementFragment.AntennaDisplay antenna : rawSignals.keySet()) {
            for (BaseProperty es : rawSignals.get(antenna)) {
                if (es.isValidSignal) {
                    containsValidSignals = true;
                    break;
                }
            }
            if (containsValidSignals) {
                break;
            }
        }

        // if there is at least one valid signal, we build the data structures.
        if (containsValidSignals) {

            // 1) We create the child view data structures
            List<BaseProperty> allRawSignals = new ArrayList<>();
            childViewWifiSignals = new ConcurrentHashMap<>();
            childViewSignals = new ConcurrentHashMap<>();

            for (MeasurementFragment.AntennaDisplay antenna : rawSignals.keySet()) {
                Tools.arraySortDbm(rawSignals.get(antenna), true);
                if (antenna == MeasurementFragment.AntennaDisplay.WIFI) {
                    childViewWifiSignals = WifiGroupHelper.groupWifi(rawSignals.get(antenna));
                } else if (antenna == MeasurementFragment.AntennaDisplay.CELLULAR) {
                    List<BaseProperty> cellularRawSignals = removeBuggyCellularSignals(
                            rawSignals.get(antenna));
                    childViewSignals.put(antenna, cellularRawSignals);
                    // we add all signals but Wi-Fi
                    allRawSignals.addAll(cellularRawSignals);
                } else {
                    childViewSignals.put(antenna, rawSignals.get(antenna));
                    // we add all signals but Wi-Fi
                    allRawSignals.addAll(rawSignals.get(antenna));
                }
            }

            // 2) We create the group view data structures
            sortedWifiGroupSignals = Collections.list(this.childViewWifiSignals.keys());
            // must be sorted as there is no order on HashMap keys
            Tools.arraySortDbm(sortedWifiGroupSignals, true);

            // we add Wi-Fi group signals (to do not duplicate signals belonging to the same
            // Wi-Fi antenna in the topFiveSignals)
            allRawSignals.addAll(sortedWifiGroupSignals);
            // we sort allRawSignals to get the top five signals
            Tools.arraySortDbm(allRawSignals, true);
            if (allRawSignals.size() > 5) {
                topFiveSignals = allRawSignals.subList(0, 5);
            } else {
                topFiveSignals = allRawSignals;
            }

            //Log.d(TAG, "SignalsSlot: topFiveSignals: " + topFiveSignals);
            //Log.d(TAG, "SignalsSlot: allRawSignals: " + allRawSignals);


            groupViewSignals = buildGroupViewSignals(childViewSignals, sortedWifiGroupSignals);
            sortedGroupViewSignals = new ArrayList<>(groupViewSignals.values());
            Tools.arraySortDbm(sortedGroupViewSignals, true);
            // we must remove the invalid signals. They are used in the monitors
            // and in the DbRequest to guarantee alignment of data structures. However, as the
            // SignalsSlot object is used only for displaying a single temporal slot, invalid
            // signals can be safely removed.
            sortedGroupViewSignals = Tools.removeEsPropertiesWithInvalidSignals(sortedGroupViewSignals);

            // 3) We compute the top signal
            if (0 < sortedGroupViewSignals.size()) {
                topSignal = sortedGroupViewSignals.get(0);
            }

            // 4) We compute the sum of all signals that have the dbm value in range.
            // 4.1) we compute the sum for Bluetooth
            //Log.d(TAG, "SignalsSlot: ##: BT");
            List<BaseProperty> childView;
            childView = childViewSignals.get(MeasurementFragment.AntennaDisplay.BLUETOOTH);
            if (childView != null) {
                slotCumulativeBluetoothMwValue = computeCumulativeSignalsPower(childView);
                // we update the top signal cumul_dbm in the group view
                childView.get(0).cumul_dbm = Tools.milliWattToDbm(slotCumulativeBluetoothMwValue);
            }

            // 4.2) we compute the sum for Cellular
            //Log.d(TAG, "SignalsSlot: ##: Cellular");
            childView = childViewSignals.get(MeasurementFragment.AntennaDisplay.CELLULAR);
            if (childView != null) {
                slotCumulativeCellularMwValue = computeCumulativeSignalsPower(childView);
                // we update the top signal cumul_dbm in the group view
                childView.get(0).cumul_dbm = Tools.milliWattToDbm(slotCumulativeCellularMwValue);

            }

            // 4.3) we compute the sum for Wi-Fi. Indeed, for Wi-Fi we should not count
            //    signals that belong to the same physical antenna.
            //Log.d(TAG, "SignalsSlot: ##: WiFi");
            // we compute the cumulative power (note that sortedWifiGroupSignals cannot be null)
            slotCumulativeWifiMwValue = computeCumulativeSignalsPower(sortedWifiGroupSignals);
            // we update the top signal cumul_dbm in the wifi group view
            sortedWifiGroupSignals.get(0).cumul_dbm = Tools.milliWattToDbm(slotCumulativeWifiMwValue);

            // 4.4) we update the global slotCumulativeTotalDbmValue
            slotCumulativeTotalMwValue = slotCumulativeBluetoothMwValue +
                    slotCumulativeCellularMwValue +
                    slotCumulativeWifiMwValue;

            slotCumulativeTotalDbmValue = Tools.milliWattToDbm(slotCumulativeTotalMwValue);

//            Log.d(TAG, "SignalsSlot ##: slotCumulativeTotalDbmValue=" + slotCumulativeTotalDbmValue +
//                    " slotCumulativeTotalMwValue=" + slotCumulativeTotalMwValue +
//                    " Max value=" + topSignal.dbm);
//            Log.d(TAG, "SignalsSlot: slotCumulativeBluetoothMwValue: " + slotCumulativeBluetoothMwValue +
//                    " slotCumulativeCellularMwValue: " + slotCumulativeCellularMwValue +
//                    " cumulativeWifiMw: " + slotCumulativeWifiMwValue);
        }
    }

    /**
     * Remove invalid cellular signals with a -51 dbm power that appear erroneously on some devices.
     * We observe such buggy signals mostly on some Samsung phones.
     * <p>
     * The strategy we use is purely empirical and based on Yanis exploration of -51 signals in the
     * DB. We use the following strategy to remove them:
     * We remove 2G ({@link GsmProperty)} and 3G ({@link WcdmaProperty} signals if
     * - we have at least NB_BUGGY_SOURCES_THRESHOLD neighboring cells with a dbm value at -51
     * <p>
     * We did not notice a significant fraction of buggy signals for 4G or for serving cells, this
     * is why we exclude these kind of signals in the filtering.
     * <p>
     * The rational behind the threshold NB_BUGGY_SOURCES_THRESHOLD (set to 5) is that it is very
     * unlikely to see in a real scenario more than 5 neighboring cells at -51 dbm. Also
     * when we observe the buggy signals, we usually have way more than 5 buggy signals (usually
     * more than 10). This threshold allows to keep valid neighboring cells at -51 dbm, and
     * to remove buggy ones.
     * Note that the threshold of NB_BUGGY_SOURCES_THRESHOLD cells in counted independently
     * for 2G and 3G.
     *
     * @param cellularRawSignals The raw list of cellular signals
     * @return the filtered of cellular signals
     */
    private List<BaseProperty> removeBuggyCellularSignals(List<BaseProperty> cellularRawSignals) {
        List<BaseProperty> filteredCellularRawSignals = new ArrayList<>();

        final int BUGGY_DBM = -51;
        final int NB_BUGGY_SOURCES_THRESHOLD = 5;

        // number of GsmProperty signals in the cellularRawSignals
        int nbGsmProperty = 0;
        // number of nbWcdmaProperty signals in the cellularRawSignals
        int nbWcdmaProperty = 0;
        // number of buggy GsmProperty signals in the cellularRawSignals
        int nbBuggyGsmProperty = 0;
        // number of buggy nbWcdmaProperty signals in the cellularRawSignals
        int nbBuggyWcdmaProperty = 0;

        //  1) We make a first pass on all cellularRawSignals to extract statistics on suspected
        // buggy signals to decide what to do next in the filtering step.
        // A signal is suspected to be buggy if it is
        // (2G OR 3G) AND dbm is -51 AND it is a neighboring cell
        for (BaseProperty signal : cellularRawSignals) {
            Log.d(TAG, "SignalsSlot: signal: " + signal);
            if (signal instanceof GsmProperty) {
                nbGsmProperty++;
                if ((signal.dbm == BUGGY_DBM) && (!signal.connected)) {
                    nbBuggyGsmProperty++;
                    Log.d(TAG, "**** SignalsSlot: signal: " + signal);
                }
            } else if (signal instanceof WcdmaProperty) {
                nbWcdmaProperty++;
                if ((signal.dbm == BUGGY_DBM) && (!signal.connected)) {
                    nbBuggyWcdmaProperty++;
                    Log.d(TAG, "**** SignalsSlot: signal: " + signal);
                }
            }
        }

        Log.d(TAG, "removeBuggyCellularSignals: nbGsmProperty=" + nbGsmProperty +
                " nbBuggyGsmProperty=" + nbBuggyGsmProperty +
                " nbWcdmaProperty=" + nbWcdmaProperty +
                " nbBuggyWcdmaProperty=" + nbBuggyWcdmaProperty);


        // 2) We only keep signals that are not deemed to be buggy.
        for (BaseProperty signal : cellularRawSignals) {
            if (signal instanceof GsmProperty) {
                if ((nbBuggyGsmProperty < NB_BUGGY_SOURCES_THRESHOLD) ||
                        (signal.dbm != BUGGY_DBM) || (signal.connected)) {
                    filteredCellularRawSignals.add(signal);
                }
            } else if ((signal instanceof WcdmaProperty)) {
                if ((nbBuggyWcdmaProperty < NB_BUGGY_SOURCES_THRESHOLD) ||
                        (signal.dbm != BUGGY_DBM) || (signal.connected)) {
                    filteredCellularRawSignals.add(signal);
                }
            } else {
                filteredCellularRawSignals.add(signal);
            }
        }

        return filteredCellularRawSignals;
    }

    /**
     * Build the groupViewSignals that contains the highest power signal for each antenna (including Wi-Fi)
     *
     * @param childViewSignals       represents the list of all signals for each antenna (excluding Wi-Fi)
     * @param sortedWifiGroupSignals represents the list of the Wi-Fi groups, highest signal first.
     * @return the groupViewSignals that has been built
     */
    private ConcurrentHashMap<MeasurementFragment.AntennaDisplay, BaseProperty> buildGroupViewSignals(
            ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> childViewSignals,
            List<BaseProperty> sortedWifiGroupSignals) {

        ConcurrentHashMap<MeasurementFragment.AntennaDisplay, BaseProperty> groupViewSignals = new ConcurrentHashMap<>();
        for (MeasurementFragment.AntennaDisplay antenna : MeasurementFragment.AntennaDisplay.values()) {

            if (antenna == MeasurementFragment.AntennaDisplay.WIFI &&
                    sortedWifiGroupSignals.size() > 0) {
                groupViewSignals.put(antenna, sortedWifiGroupSignals.get(0));
            } else {
                if (childViewSignals.containsKey(antenna)) {
                    groupViewSignals.put(antenna, childViewSignals.get(antenna).get(0));
                }
            }
        }

        return groupViewSignals;
    }


    /**
     * Compute the sum of the signals power (in mW) and returns the result in mW. We exclude all
     * invalid signals, and signals for which the dbm value is not in range.
     *
     * @param signalList list of {@link BaseProperty} objects
     * @return the sum of the signals power for all valid signal with the dbm value in range.
     * The result is returned in mW.
     */
    private double computeCumulativeSignalsPower(List<BaseProperty> signalList) {
        double slotCumulativeMilliWattValue = 0;
        for (BaseProperty es : signalList) {
            if (es.isValidSignal && es.isDbmValueInRange()) {
                slotCumulativeMilliWattValue = slotCumulativeMilliWattValue + Tools.dbmToMilliWatt(es.dbm);
//                Log.d(TAG, "computeCumulativeSignalsPower ##: slotCumulativeMilliWattValue="
//                        + slotCumulativeMilliWattValue +
//                        " dbm=" + es.dbm +
//                        " mw=" + Tools.dbmToMilliWatt(es.dbm) +
//                        " sum=" + Tools.milliWattToDbm(slotCumulativeMilliWattValue));
            }
        }

        return slotCumulativeMilliWattValue;
    }

    /**
     * Returns the groupViewSignals
     * <p>
     * groupViewSignals represents the highest power signal for each antenna (including Wi-Fi)
     * This is the data structure to use for the chart is it contains an entry for each antenna
     * (when this is not signal for this antenna, the entry is a BaseProperty with the isValid flag
     * we to false.
     *
     * @return groupViewSignals
     */
    @Nullable
    public ConcurrentHashMap<MeasurementFragment.AntennaDisplay, BaseProperty> getGroupViewSignals() {
        return groupViewSignals;
    }

    /**
     * returns the childViewSignals
     * <p>
     * childViewSignals represents the list of all signals for each antenna (excluding Wi-Fi,
     * Wi-Fi signals are in childViewWifiSignals)
     *
     * @return childViewSignals
     */
    @Nullable
    public ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> getChildViewSignals() {
        return childViewSignals;
    }

    /**
     * returns the childViewWifiSignals
     * <p>
     * childViewWifiSignals represents the list of all Wi-Fi signals. As we aggregate Wi-Fi signals
     * per group, the keys represent a dummy Wi-Fi signal representing the group (its SSID and BSSID
     * are defined according to the group), and the values represent the list of Wi-Fi signals
     * for the given group.
     *
     * @return childViewWifiSignals
     */
    @Nullable
    public ConcurrentHashMap<BaseProperty, List<BaseProperty>> getChildViewWifiSignals() {
        return childViewWifiSignals;
    }

    /**
     * returns the sortedWifiGroupSignals
     * <p>
     * sortedGroupViewSignals represents the list of the highest signal per antenna (including Wi-Fi),
     * highest signal first. Note that this data structure is used for the display of the group views,
     * it is not used for the chart as all the invalid signals are removed (note that invalid signals
     * are required by the chart for antenna with no measurements).
     *
     * @return sortedWifiGroupSignals
     */
    @Nullable
    public List<BaseProperty> getSortedWifiGroupSignals() {
        return sortedWifiGroupSignals;
    }

    /**
     * returns the sortedGroupViewSignals
     * <p>
     * sortedWifiGroupSignals represents the list of the Wi-Fi groups (i.e., the list of the dummy
     * Wi-Fi signals representing the Wi-Fi groups), highest signal first.
     *
     * @return sortedGroupViewSignals
     */
    @Nullable
    public List<BaseProperty> getSortedGroupViewSignals() {
        return sortedGroupViewSignals;
    }

    /**
     * returns the topSignal
     * <p>
     * topSignal represents the highest signal for all antennas (including Wi-Fi)
     *
     * @return topSignal
     */
    @Nullable
    public BaseProperty getTopSignal() {
        return topSignal;
    }

    /**
     * returns the sum of all valid dbm values in this slot
     *
     * @return slotCumulativeTotalDbmValue
     */
    public int getSlotCumulativeTotalDbmValue() {
        return slotCumulativeTotalDbmValue;
    }

    /**
     * returns the sum of all valid dbm values in this slot in un-rounded mW (higher precision
     * that with getSlotCumulativeTotalDbmValue().
     *
     * @return slotCumulativeTotalMwValue
     */
    public double getSlotCumulativeTotalMwValue() {
        return slotCumulativeTotalMwValue;
    }

    /**
     * returns the sum of all valid dbm values for Wi-Fi signals only, in this slot in un-rounded
     * mW (higher precision that with getSlotCumulativeTotalDbmValue().
     *
     * @return slotCumulativeWifiMwValue
     */
    public double getSlotCumulativeWifiMwValue() {
        return slotCumulativeWifiMwValue;
    }

    /**
     * returns the sum of all valid dbm values for Bluetooth signals only, in this slot in un-rounded
     * mW (higher precision that with getSlotCumulativeTotalDbmValue().
     *
     * @return slotCumulativeBluetoothMwValue
     */
    public double getSlotCumulativeBluetoothMwValue() {
        return slotCumulativeBluetoothMwValue;
    }

    /**
     * returns the sum of all valid dbm values for Cellular signals only, in this slot in un-rounded
     * mW (higher precision that with getSlotCumulativeTotalDbmValue().
     *
     * @return slotCumulativeCellularMwValue
     */
    public double getSlotCumulativeCellularMwValue() {
        return slotCumulativeCellularMwValue;
    }

    /**
     * Set to true when the SignalsSlot contains valid signals, false otherwise (e.g., when it is
     * created for a pause event)
     *
     * @return true is the SignalsSlot contains valid signals
     */
    public boolean containsValidSignals() {
        return containsValidSignals;
    }

    /**
     * Returns the list of the five most exposing signals in this slot, most exposing first.
     * For Wi-Fi signals it contains a WiFiGroupProperty (instead of a WifiProperty)
     *
     * @return The list of the top five most exposing signals (most exposing first).
     */
    public List<BaseProperty> getTopFiveSignals() {
        return topFiveSignals;
    }

    @NonNull
    @Override
    public String toString() {
        return "\n**groupViewSignals**: " + ((groupViewSignals == null) ? "" : groupViewSignals.toString())
                + "\n**childViewSignals**: " + ((childViewSignals == null) ? "" : childViewSignals.toString())
                + "\n**childViewWifiSignals**: " + ((childViewWifiSignals == null) ? "" : childViewWifiSignals.toString())
                + "\n**sortedGroupViewSignals**: " + ((sortedGroupViewSignals == null) ? "" : sortedGroupViewSignals.toString())
                + "\n**topSignal**: " + ((topSignal == null) ? "" : topSignal.toString())
                + "\n**containsValidSignals: " + containsValidSignals
                + "\n**slotCumulativeTotalDbmValue: " + slotCumulativeTotalDbmValue
                + "\n**slotCumulativeTotalMwValue: " + slotCumulativeTotalMwValue
                + "\n**slotCumulativeWifiMwValue: " + slotCumulativeWifiMwValue
                + "\n**slotCumulativeBluetoothMwValue: " + slotCumulativeBluetoothMwValue
                + "\n**slotCumulativeCellularMwValue: " + slotCumulativeCellularMwValue
                + "\n**topFiveSignals: " + topFiveSignals;

    }

}

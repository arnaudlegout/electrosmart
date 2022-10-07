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
package fr.inria.es.electrosmart.signalproperties;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;

public abstract class BaseProperty implements Comparable<BaseProperty> {

    private static final String TAG = "BaseProperty";

    // Constants used when a BaseProperty is exported/imported as a JSON string
    public static final String BASE_PROPERTY_TYPE = "basePropertyType";
    public static final String WIFI_PROPERTY = "WifiProperty";
    public static final String BLUETOOTH_PROPERTY = "BluetoothProperty";
    public static final String GSM_PROPERTY = "GsmProperty";
    public static final String WCDMA_PROPERTY = "WcdmaProperty";
    public static final String LTE_PROPERTY = "LteProperty";
    public static final String NEW_RADIO_PROPERTY = "NewRadioProperty";
    public static final String CDMA_PROPERTY = "CdmaProperty";

    public static final String DBM = "dbm";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String IS_VALID_SIGNAL = "is_valid_signal";
    public static final String MEASURED_TIME = "measured_time";

    /*
     Starting with Android Q (API 29) there is a dedicated field (CellInfo.UNAVAILABLE) that is
     used to notify that a field in unavailable. Before API 29, there was no dedicated field and most
     of the code used a default Integer.MAX_VALUE.

     Note that in API 29, CellInfo.UNAVAILABLE == Integer.MAX_VALUE
    */
    public static final int UNAVAILABLE;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UNAVAILABLE = CellInfo.UNAVAILABLE;
        } else {
            UNAVAILABLE = Integer.MAX_VALUE;
        }
    }

    /*
    true if the passed signal is a real measured one (live or in the DB), false otherwise, that is,
    the signal is a placeholder that do not correspond to a valid measurement.

    Note:
    All signals returned by the monitor are valid, but signals can be forged by the app
    when there is no measured signals to guarantee, for instance, data structures alignment.

    Also, a signal might be valid (that is, the result of a real measurement), but its dbm value
    might be out of range. Therefore, when processing a signal, we expect to test for both
    its validity and the dbm in-range.
    */
    public boolean isValidSignal = true;
    public int type = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    public int dbm = UNAVAILABLE;
    // contains the sum of the signal powers in a given group. This field is set only for the
    // group view elements.
    public int cumul_dbm = Integer.MAX_VALUE;
    public int ber = UNAVAILABLE;
    public int timing_advance = UNAVAILABLE;
    public int lte_rssi = UNAVAILABLE;
    public int lte_rsrp = UNAVAILABLE;
    public int lte_rsrq = UNAVAILABLE;
    public int lte_rssnr = UNAVAILABLE;
    public int lte_cqi = UNAVAILABLE;
    public String ssid = null;
    public String bssid = null;
    public String operator_friendly_name = null;
    public String venue_name = null;
    public int is_passpoint_network = Const.INVALID_IS_PASSPOINT_NETWORK;
    public String starredBssid = null;
    public String capabilities = null;
    public String bt_device_name = null;
    public String bt_device_name_alias = null;
    public String bt_address = null;
    public int bt_device_class = Integer.MAX_VALUE;
    public int bt_bond_state = Integer.MAX_VALUE;
    public int bt_device_type = Integer.MAX_VALUE;
    public int freq = Integer.MAX_VALUE;
    public int center_freq0 = Const.INVALID_CENTERFREQ0;
    public int center_freq1 = Const.INVALID_CENTERFREQ1;
    public int channel_width = Const.INVALID_CHANNELWIDTH;
    public int wifiStandard = Const.INVALID_WIFI_STANDARD;
    public int mnc = UNAVAILABLE;
    public int mcc = UNAVAILABLE;
    public int cid = UNAVAILABLE;
    public int ucid = UNAVAILABLE;
    public int pci = UNAVAILABLE;
    public long nci = UNAVAILABLE;
    public int tac = UNAVAILABLE;
    public int lac = UNAVAILABLE;
    public int psc = UNAVAILABLE;
    public int eci = UNAVAILABLE;
    public int arfcn = UNAVAILABLE;
    public int bsic = UNAVAILABLE;
    public int uarfcn = UNAVAILABLE;
    public int earfcn = UNAVAILABLE;
    public int nrarfcn = UNAVAILABLE;
    public int bandwidth = UNAVAILABLE;
    public int ecno = UNAVAILABLE;
    public int base_station_id = UNAVAILABLE;
    public int cdma_latitude = UNAVAILABLE;
    public int cdma_longitude = UNAVAILABLE;
    public int network_id = UNAVAILABLE;
    public int system_id = UNAVAILABLE;
    public int cdma_dbm = UNAVAILABLE;
    public int cdma_ecio = UNAVAILABLE;
    public int evdo_dbm = UNAVAILABLE;
    public int evdo_snr = UNAVAILABLE;
    public int evdo_ecio = UNAVAILABLE;
    // https://developer.android.com/reference/android/telephony/CellSignalStrengthNr.html
    public int csiRsrp = UNAVAILABLE;
    public int csiRsrq = UNAVAILABLE;
    public int csiSinr = UNAVAILABLE;
    public int ssRsrp = UNAVAILABLE;
    public int ssRsrq = UNAVAILABLE;
    public int ssSinr = UNAVAILABLE;
    // Each time a signal is measured, we set its measured_time. In historical mode, we also
    // retrieve from the DB the time at which the signal has been measured.
    // In current, we use the measured_time for the hysteresis algorithm : when the measured time
    // is larger than a given threshold, this signal will be removed from the list of signals
    // to be displayed in the user interface. The threshold can be different for each signal type.
    public long measured_time = Const.INVALID_TIME;

    // The latitude and longitude correspond to the location the signal has been measured.
    public double latitude = Const.INVALID_LATITUDE;
    public double longitude = Const.INVALID_LONGITUDE;

    // true if the device is connected to the cellular antenna
    public boolean connected = false;

    // Operator name for cellular sources retrieved using the mnc, mcc table
    public String mOperatorName = "";

    /**
     * Test whether two objects hold the same data values or both are null
     * Note: Method copied from CdmaCellLocation
     *
     * @param a first obj
     * @param b second obj
     * @return true if two objects equal or both are null, false otherwise
     */
    private static boolean equalsHandlesNulls(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    /**
     * Prepares {@link #mOperatorName} by requesting the database for the operator name
     * using mnc, mcc values. Only some cellular source types implement this method. Otherwise
     * the call is simply rejected and a log message is printed for debugging.
     */
    public void prepareOperatorName() {
        Log.d(TAG, String.format("%s: prepareOperatorName: Unsupported operation",
                getClass().getSimpleName()));
    }

    /**
     * Returns a human readable source name (SSID for Wi-Fi, device name for Bluetooth
     * and operator name for Cellular)
     *
     * @param context The context to be used to retrieve string resources if needed
     * @return The human readable source name
     */
    public CharSequence friendlySourceName(@NonNull Context context) {
        prepareOperatorName();
        return mOperatorName;
    }

    // written according to the recommendation in
    // http://developer.android.com/reference/java/lang/Object.html#writing_hashCode
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof BaseProperty)) {
            return false;
        }

        BaseProperty sig = (BaseProperty) obj;

        if (sig instanceof WifiGroupProperty && this instanceof WifiGroupProperty) {
            return equalsHandlesNulls(starredBssid, sig.starredBssid) &&
                    freq == sig.freq;
        } else if (sig instanceof WifiProperty && this instanceof WifiProperty) {
            return equalsHandlesNulls(ssid, sig.ssid) &&
                    equalsHandlesNulls(bssid, sig.bssid) &&
                    // equalsHandlesNulls(capabilities, sig.capabilities) &&
                    // equalsHandlesNulls(starredBssid, sig.starredBssid) &&
                    freq == sig.freq;
        } else if (sig instanceof BluetoothProperty) {
            //Log.d(TAG, "in sig instanceof BluetoothProperty");
            return equalsHandlesNulls(bt_device_name, sig.bt_device_name) &&
                    equalsHandlesNulls(bt_address, sig.bt_address) &&
                    bt_device_class == sig.bt_device_class &&
                    bt_device_type == sig.bt_device_type;
            // bt_bond_state == sig.bt_bond_state &&
            // dbm == sig.dbm &&
            // unique_seq == sig.unique_seq;

        } else if (sig instanceof LteProperty) {
            //Log.d(TAG, "in sig instanceof LteProperty");
            return sig.type == type &&
                    sig.eci == eci &&
                    sig.mcc == mcc &&
                    sig.mnc == mnc &&
                    sig.pci == pci &&
                    sig.tac == tac &&
                    sig.earfcn == earfcn &&
                    sig.bandwidth == bandwidth;
            // sig.dbm == dbm &&
            // sig.unique_seq == unique_seq;
        } else if (sig instanceof NewRadioProperty) {
            //Log.d(TAG, "in sig instanceof NewRadioProperty");
            return sig.type == type &&
                    sig.mcc == mcc &&
                    sig.mnc == mnc &&
                    sig.nci == nci &&
                    sig.nrarfcn == nrarfcn &&
                    sig.pci == pci &&
                    sig.tac == tac;
        } else if (sig instanceof GsmProperty) {
            //Log.d(TAG, "in sig instanceof GsmProperty");
            /*
            Note: we do not add the timing advance to the unicity
            of an antenna as timing advance depends on the distance
            of the device to the antenna, therefore it cannot be used
            to compute unicity
            */
            return sig.type == type &&
                    sig.mcc == mcc &&
                    sig.mnc == mnc &&
                    sig.cid == cid &&
                    sig.lac == lac &&
                    sig.arfcn == arfcn &&
                    sig.bsic == bsic;
            // sig.dbm == dbm &&
            // sig.unique_seq == unique_seq;
        } else if (sig instanceof WcdmaProperty) {
            //Log.d(TAG, "in sig instanceof WcdmaProperty");
            return sig.type == type &&
                    sig.mcc == mcc &&
                    sig.mnc == mnc &&
                    sig.ucid == ucid &&
                    sig.lac == lac &&
                    sig.psc == psc &&
                    sig.uarfcn == uarfcn;
            // sig.dbm == dbm &&
            // sig.unique_seq == unique_seq;
        } else if (sig instanceof CdmaProperty && sig.type == TelephonyManager.NETWORK_TYPE_CDMA) {
            //Log.d(TAG, "in sig instanceof CdmaProperty + CDMA");
            return sig.type == type &&
                    sig.network_id == network_id &&
                    sig.system_id == system_id &&
                    sig.base_station_id == base_station_id &&
                    sig.cdma_latitude == cdma_latitude &&
                    sig.cdma_longitude == cdma_longitude;
            // sig.cdma_dbm == cdma_dbm &&
            // sig.cdma_ecio == cdma_ecio &&
            // sig.unique_seq == unique_seq;
        } else if (sig instanceof CdmaProperty &&
                (sig.type == TelephonyManager.NETWORK_TYPE_EVDO_0 ||
                        sig.type == TelephonyManager.NETWORK_TYPE_EVDO_A ||
                        sig.type == TelephonyManager.NETWORK_TYPE_EVDO_B)) {
            //Log.d(TAG, "in sig instanceof CdmaProperty + EVDO");
            return sig.type == type &&
                    sig.network_id == network_id &&
                    sig.system_id == system_id &&
                    sig.base_station_id == base_station_id &&
                    sig.cdma_latitude == cdma_latitude &&
                    sig.cdma_longitude == cdma_longitude;
            // sig.evdo_dbm == evdo_dbm &&
            // sig.evdo_ecio == evdo_ecio &&
            // sig.evdo_snr == evdo_snr &&
            // sig.unique_seq == unique_seq;

        } else {
            return false;
        }
    }

    // written according to the recommendation in
    // http://developer.android.com/reference/java/lang/Object.html#writing_hashCode
    @Override
    public int hashCode() {
        int prime = 31;
        int result = 17;
        if (this instanceof WifiGroupProperty) {
            result = prime * result + (starredBssid == null ? 0 : starredBssid.hashCode());
            result = prime * result + freq;
            return result;
        } else if (this instanceof WifiProperty) {
            result = prime * result + (ssid == null ? 0 : ssid.hashCode());
            result = prime * result + (bssid == null ? 0 : bssid.hashCode());
            // result = prime * result + (capabilities == null ? 0 : capabilities.hashCode());
            // result = prime * result + (starredBssid == null ? 0 : starredBssid.hashCode());
            result = prime * result + freq;
            return result;
        } else if (this instanceof BluetoothProperty) {
            // Log.d(TAG, "hash in sig instanceof BluetoothProperty");
            result = prime * result + (bt_device_name == null ? 0 : bt_device_name.hashCode());
            result = prime * result + (bt_address == null ? 0 : bt_address.hashCode());
            result = prime * result + bt_device_class;
            result = prime * result + bt_device_type;
            // result = prime * result + bt_bond_state;
            // result = prime * result + dbm;
            // result = prime * result + unique_seq;
            return result;
        } else if (this instanceof LteProperty) {
            //Log.d(TAG, "hash in sig instanceof LteProperty");
            result = prime * result + type;
            result = prime * result + mcc;
            result = prime * result + mnc;
            result = prime * result + eci;
            result = prime * result + pci;
            result = prime * result + tac;
            result = prime * result + earfcn;
            result = prime * result + bandwidth;
            // result = prime * result + dbm;
            // result = prime * result + unique_seq;
            return result;
        } else if (this instanceof NewRadioProperty) {
            //Log.d(TAG, "hash in sig instanceof NewRadioProperty");
            result = prime * result + type;
            result = prime * result + mcc;
            result = prime * result + mnc;
            // nci is a long, we cast it to an int, even if the case might lose some information
            // it should not be a problem for the hashCode() method that just require that the
            // casted nci always returns the same value
            result = prime * result + (int) nci;
            result = prime * result + nrarfcn;
            result = prime * result + pci;
            result = prime * result + tac;
            return result;
        } else if (this instanceof GsmProperty) {
            //Log.d(TAG, "hash in sig instanceof GsmProperty");
            result = prime * result + type;
            result = prime * result + mcc;
            result = prime * result + mnc;
            result = prime * result + cid;
            result = prime * result + lac;
            result = prime * result + arfcn;
            result = prime * result + bsic;
            // result = prime * result + dbm;
            // result = prime * result + unique_seq;
            return result;
        } else if (this instanceof WcdmaProperty) {
            // Log.d(TAG, "hash in sig instanceof WcdmaProperty");
            result = prime * result + type;
            result = prime * result + mcc;
            result = prime * result + mnc;
            result = prime * result + ucid;
            result = prime * result + lac;
            result = prime * result + psc;
            result = prime * result + uarfcn;
            // result = prime * result + dbm;
            // result = prime * result + unique_seq;
            return result;
        } else if (this instanceof CdmaProperty && type == TelephonyManager.NETWORK_TYPE_CDMA) {
            // Log.d(TAG, "hash in sig instanceof CdmaProperty + CDMA");
            result = prime * result + type;
            result = prime * result + network_id;
            result = prime * result + system_id;
            result = prime * result + base_station_id;
            result = prime * result + cdma_latitude;
            result = prime * result + cdma_longitude;
            // result = prime * result + cdma_dbm;
            // result = prime * result + cdma_ecio;
            // result = prime * result + unique_seq;
            return result;
        } else if (this instanceof CdmaProperty &&
                (type == TelephonyManager.NETWORK_TYPE_EVDO_0 ||
                        type == TelephonyManager.NETWORK_TYPE_EVDO_A ||
                        type == TelephonyManager.NETWORK_TYPE_EVDO_B)) {
            //Log.d(TAG, "hash in sig instanceof CdmaProperty + EVDO");
            result = prime * result + type;
            result = prime * result + network_id;
            result = prime * result + system_id;
            result = prime * result + base_station_id;
            result = prime * result + cdma_latitude;
            result = prime * result + cdma_longitude;
            // result = prime * result + evdo_dbm;
            // result = prime * result + evdo_ecio;
            // result = prime * result + evdo_snr;
            // result = prime * result + unique_seq;
            return result;
        } else {
            return -1;
        }
    }

    /**
     * The real implementation of toString() is in the subclasses
     */
    @Override
    public abstract String toString();

    /**
     * This method creates the JSON string for the corresponding signal. This string must
     * contain all the required information to recreate the BaseProperty object.
     * <p>
     * The real implementation of toJsonString() is in the subclasses.
     *
     * @return The JSON string corresponding to the signal
     */
    public abstract String toJsonString();

    /**
     * The real implementation of copy() is in the subclasses.
     */
    public abstract BaseProperty copy();

    /**
     * This method returns min possible dbm value for the corresponding signal
     * The real implementation of getMinDbm() is in the subclasses.
     */
    public abstract int getMinDbm();

    /**
     * This method returns max possible dbm value for the corresponding signal
     * The real implementation of getMaxDbm() is in the subclasses.
     */
    public abstract int getMaxDbm();

    /**
     * This method dump the signal to the database
     * The real implementation of dumpSignalToDatabase() is in the subclasses.
     */
    public abstract void dumpSignalToDatabase(SQLiteDatabase db);

    /**
     * Change the dBm value for the BaseProperty with invalid dBm to a value set to the minimum
     * value (minus 1) for the corresponding signal type.
     * The real implementation of normalizeSignalWithInvalidDbm() is in the subclasses.
     */
    public abstract void normalizeSignalWithInvalidDbm();

    /**
     * Return the Antenna display corresponding to this signal
     * The real implementation of getAntennaDisplay() is in the subclasses.
     */
    public abstract MeasurementFragment.AntennaDisplay getAntennaDisplay();


    /**
     * Return true if the dBm value for the signal is within the acceptable range
     *
     * @return true if the dBm value in within [MIN_DBM, MAX_DBM] for this signal, false otherwise
     */
    public boolean isDbmValueInRange() {
        return getMinDbm() <= dbm && dbm <= getMaxDbm();
    }


    /**
     * Compare a BaseProperty dbm value to this BaseProperty dbm value.
     *
     * @param o A BaseProperty to compare with this one
     * @return -1 this BaseProperty dbm is less than o.dbm and +1 0 if this
     * BaseProperty dbm is greater than o.dbm. Return 0 in cas of equality
     */
    @Override
    public int compareTo(@NonNull BaseProperty o) {
        if (isDbmValueInRange() && o.isDbmValueInRange()) {
            if (dbm < o.dbm) {
                return -1;
            } else if (dbm > o.dbm) {
                return 1;
            } else {
                // If we get to this point we already know the `dbm` values are the same
                // We don't have to test it
                return 0;
            }
        } else if (!o.isDbmValueInRange() && isDbmValueInRange()) {
            return 1;
        } else if (o.isDbmValueInRange()) {
            // If we reach this point we already know that `isDbmValueInRange() will return `false`.
            // We don't have to test it
            return -1;
        } else {
            // If we reach this point it means that both o.isDbmValueInRange()
            // and isDbmValueInRange() are false
            return 0;
        }
    }
}
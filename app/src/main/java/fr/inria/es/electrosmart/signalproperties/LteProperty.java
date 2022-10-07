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

import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.database.DbContract;
import fr.inria.es.electrosmart.database.DbHelper;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.monitors.CellularMonitor;

public class LteProperty extends BaseProperty {

    // Constants used when a LteProperty is exported to or imported from a JSON string
    public static final String TYPE = "type";
    public static final String MCC = "mcc";
    public static final String MNC = "mnc";
    public static final String ECI = "eci";
    public static final String PCI = "pci";
    public static final String TAC = "tac";
    public static final String EARFCN = "earfcn";
    public static final String TIMING_ADVANCE = "timing_advance";
    public static final String RSSI = "ss";      //starting with API 29, this attribute is named RSSI
    public static final String RSRP = "rsrp";
    public static final String RSRQ = "rsrq";
    public static final String RSSNR = "rssnr";
    public static final String CQI = "cqi";
    public static final String CONNECTED = "connected";
    // New attribute introduced in API 28
    public static final String BANDWIDTH = "bandwidth";

    private static int MIN_DBM;
    private static int MAX_DBM;

    public LteProperty(int type, int mcc, int mnc, int eci, int pci, int tac, int earfcn,
                       int bandwidth, int timing_advance, int rsrp, int rssi, int rsrq, int rssnr, int cqi,
                       boolean connected, double latitude, double longitude, boolean isValidSignal,
                       long measured_time) {

        this.type = type;

        // available data according to
        // http://developer.android.com/reference/android/telephony/CellIdentityLte.html
        this.mcc = mcc;
        this.mnc = mnc;
        this.eci = eci;
        this.pci = pci;
        this.tac = tac;
        this.earfcn = earfcn;
        this.bandwidth = bandwidth;

        // available data according to
        // http://developer.android.com/reference/android/telephony/CellSignalStrengthLte.html
        // asu_level and level can be computed from the other values see the CellSignalStrengthLte
        // source code
        this.timing_advance = timing_advance;
        this.lte_rsrp = rsrp;
        this.lte_rssi = rssi;    // this is the LTE signal strength value before API 29 (Android Q)
        this.lte_rsrq = rsrq;
        this.lte_rssnr = rssnr;
        this.lte_cqi = cqi;


        /*
         The dbm value is computed by the object to represent the real received power.

         The most accurate dbm value is the RSSI, however, this value is not available for
         android versions older than API 29 (Android Q). For older version, we could get
         it from the SignalStrength field, but this field is not always correct.
         In case the RSSI is not available or not in its validity range, we fallback to the
         RSRP (that is always lower than the RSSI, so a worse estimate than the RSSI).
        */
        if (Const.LTE_MIN_RSSI <= this.lte_rssi && this.lte_rssi <= Const.LTE_MAX_RSSI) {
            this.dbm = CellularMonitor.convertLteRssiAsuToDbm(rssi);
            MIN_DBM = CellularMonitor.convertLteRssiAsuToDbm(Const.LTE_MIN_RSSI);
            MAX_DBM = CellularMonitor.convertLteRssiAsuToDbm(Const.LTE_MAX_RSSI);
        } else {
            this.dbm = rsrp;
            MIN_DBM = Const.LTE_MIN_RSRP;
            MAX_DBM = Const.LTE_MAX_RSRP;
        }

        // set to true if the device is connected to the antenna
        this.connected = connected;

        this.latitude = latitude;
        this.longitude = longitude;
        this.isValidSignal = isValidSignal;
        this.measured_time = measured_time;
    }

    public LteProperty(boolean isValidSignal) {
        this.isValidSignal = isValidSignal;
    }

    public LteProperty copy() {
        return new LteProperty(
                type,
                mcc,
                mnc,
                eci,
                pci,
                tac,
                earfcn,
                bandwidth,
                timing_advance,
                lte_rsrp,
                lte_rssi,
                lte_rsrq,
                lte_rssnr,
                lte_cqi,
                connected,
                latitude,
                longitude,
                isValidSignal,
                measured_time
        );
    }

    /**
     * This method returns max possible dbm value for the corresponding signal
     *
     * @return the maximum possible dbm value for this signal type
     */
    @Override
    public int getMaxDbm() {
        return MAX_DBM;
    }

    /**
     * This method returns min possible dbm value for the corresponding signal
     *
     * @return the minimum possible dbm value for this signal type
     */
    @Override
    public int getMinDbm() {
        return MIN_DBM;
    }

    /**
     * This method dump the signal to the database
     *
     * @param db a SQLiteDatabase obtained with getWritableDatabase()
     */
    @Override
    public void dumpSignalToDatabase(SQLiteDatabase db) {
        db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(this));
    }

    /**
     * Change the dBm value for the BaseProperty with invalid dBm to a value set to the minimum
     * value (minus 1) for the corresponding signal type.
     */
    @Override
    public void normalizeSignalWithInvalidDbm() {
        if (!isDbmValueInRange()) {
            dbm = getMinDbm() - 1;
        }
    }

    /**
     * Return the Antenna display corresponding to this signal
     *
     * @return the Antenna display corresponding to this signal
     */
    @Override
    public MeasurementFragment.AntennaDisplay getAntennaDisplay() {
        return MeasurementFragment.AntennaDisplay.CELLULAR;
    }

    /**
     * Prepares {@link #mOperatorName} by requesting the database for the operator name
     * using mnc, mcc values.
     */
    @Override
    public void prepareOperatorName() {
        if (mOperatorName.isEmpty()) {
            mOperatorName = DbRequestHandler.getOperatorName(mcc, mnc);
        }
    }

    @Override
    public String toString() {
        return LTE_PROPERTY + ":"
                + " " + TYPE + "=" + type
                + " " + MCC + "=" + mcc
                + " " + MNC + "=" + mnc
                + " OperatorName=" + mOperatorName
                + " " + ECI + "=" + eci
                + " " + PCI + "=" + pci
                + " " + TAC + "=" + tac
                + " " + EARFCN + "=" + earfcn
                + " " + BANDWIDTH + "=" + bandwidth
                + " " + TIMING_ADVANCE + "=" + timing_advance
                + " " + DBM + "=" + dbm
                + " " + RSRP + "=" + lte_rsrp
                + " " + RSSI + "=" + lte_rssi
                + " " + RSRQ + "=" + lte_rsrq
                + " " + RSSNR + "=" + lte_rssnr
                + " " + CQI + "=" + lte_cqi
                + " " + CONNECTED + "=" + connected
                + " " + LATITUDE + "=" + latitude
                + " " + LONGITUDE + "=" + longitude
                + " " + IS_VALID_SIGNAL + "isValidSignal=" + isValidSignal
                + " " + MEASURED_TIME + "=" + measured_time;
    }

    @Override
    public String toJsonString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BASE_PROPERTY_TYPE, LTE_PROPERTY);
            jsonObject.put(TYPE, this.type);
            jsonObject.put(MCC, this.mcc);
            jsonObject.put(MNC, this.mnc);
            jsonObject.put(ECI, this.eci);
            jsonObject.put(PCI, this.pci);
            jsonObject.put(TAC, this.tac);
            jsonObject.put(EARFCN, this.earfcn);
            jsonObject.put(BANDWIDTH, this.bandwidth);
            jsonObject.put(TIMING_ADVANCE, this.timing_advance);
            jsonObject.put(RSRP, this.lte_rsrp);
            jsonObject.put(RSSI, this.lte_rssi);
            jsonObject.put(RSRQ, this.lte_rsrq);
            jsonObject.put(RSSNR, this.lte_rssnr);
            jsonObject.put(CQI, this.lte_cqi);
            jsonObject.put(CONNECTED, this.connected);
            jsonObject.put(LATITUDE, this.latitude);
            jsonObject.put(LONGITUDE, this.longitude);
            jsonObject.put(IS_VALID_SIGNAL, this.isValidSignal);
            jsonObject.put(MEASURED_TIME, this.measured_time);
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
        return jsonObject.toString();
    }
}

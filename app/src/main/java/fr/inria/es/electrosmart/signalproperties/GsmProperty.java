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

public class GsmProperty extends BaseProperty {

    // Constants used when a GsmProperty is exported to or imported from a JSON string
    public static final String TYPE = "type";
    public static final String MCC = "mcc";
    public static final String MNC = "mnc";
    public static final String CID = "cid";
    public static final String LAC = "lac";
    public static final String ARFCN = "arfcn";
    public static final String BSIC = "bsic";
    public static final String TIMING_ADVANCE = "timing_advance";
    public static final String BER = "ber";
    public static final String CONNECTED = "connected";

    public GsmProperty(int type, int mcc, int mnc, int cid, int lac, int arfcn, int bsic,
                       int timing_advance, int dbm, int ber, boolean connected,
                       double latitude, double longitude, boolean isValidSignal, long measured_time) {
        this.type = type;

        // the psc value is deprecated and hardcoded to return Integer.MAX_VALUE, so we don't
        // collect it
        this.mcc = mcc;
        this.mnc = mnc;
        this.cid = cid;
        this.lac = lac;
        this.arfcn = arfcn;
        this.bsic = bsic;
        this.timing_advance = timing_advance;

        // asu_level and level can be derived from the dbm value (dBm = -113 + (2 * asu)).
        // ber is a hidden value that we extract from the string representation of the
        // CellSignalStrengthGsm.
        this.dbm = dbm;
        this.ber = ber;

        // set to true if the device is connected to the antenna
        this.connected = connected;

        this.latitude = latitude;
        this.longitude = longitude;
        this.isValidSignal = isValidSignal;
        this.measured_time = measured_time;
    }

    public GsmProperty(boolean isValidSignal) {
        this.isValidSignal = isValidSignal;
    }

    public GsmProperty copy() {
        return new GsmProperty(type, mcc, mnc, cid, lac, arfcn, bsic, timing_advance, dbm,
                ber, connected, latitude, longitude, isValidSignal, measured_time);
    }

    /**
     * This method returns max possible dbm value for the corresponding signal
     *
     * @return the maximum possible dbm value for this signal type
     */
    @Override
    public int getMaxDbm() {
        return Const.GSM_MAX_DBM;
    }

    /**
     * This method returns min possible dbm value for the corresponding signal
     *
     * @return the minimum possible dbm value for this signal type
     */
    @Override
    public int getMinDbm() {
        return Const.GSM_MIN_DBM;
    }

    /**
     * This method dump the signal to the database
     *
     * @param db a SQLiteDatabase obtained with getWritableDatabase()
     */
    @Override
    public void dumpSignalToDatabase(SQLiteDatabase db) {
        db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(this));
    }

    /**
     * Change the dBm value for the BaseProperty with invalid dBm to a value set to the minimum
     * value (minus 1) for the corresponding signal type.
     */
    @Override
    public void normalizeSignalWithInvalidDbm() {
        if (!isDbmValueInRange()) {
            dbm = Const.GSM_MIN_DBM - 1;
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
        return GSM_PROPERTY + ":"
                + " " + TYPE + "=" + type
                + " " + MCC + "=" + mcc
                + " " + MNC + "=" + mnc
                + " OperatorName=" + mOperatorName
                + " " + CID + "=" + cid
                + " " + LAC + "=" + lac
                + " " + ARFCN + "=" + arfcn
                + " " + BSIC + "=" + bsic
                + " " + TIMING_ADVANCE + "=" + timing_advance
                + " " + DBM + "=" + dbm
                + " " + BER + "=" + ber
                + " " + CONNECTED + "=" + connected
                + " " + LATITUDE + "=" + latitude
                + " " + LONGITUDE + "=" + longitude
                + " " + IS_VALID_SIGNAL + "=" + isValidSignal
                + " " + MEASURED_TIME + "=" + measured_time;
    }

    @Override
    public String toJsonString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BASE_PROPERTY_TYPE, GSM_PROPERTY);
            jsonObject.put(TYPE, this.type);
            jsonObject.put(MCC, this.mcc);
            jsonObject.put(MNC, this.mnc);
            jsonObject.put(CID, this.cid);
            jsonObject.put(LAC, this.lac);
            jsonObject.put(ARFCN, this.arfcn);
            jsonObject.put(BSIC, this.bsic);
            jsonObject.put(TIMING_ADVANCE, this.timing_advance);
            jsonObject.put(DBM, this.dbm);
            jsonObject.put(BER, this.ber);
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

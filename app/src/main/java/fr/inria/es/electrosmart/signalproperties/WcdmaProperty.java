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

public class WcdmaProperty extends BaseProperty {
    private final String TAG = "WcdmaProperty";

    // Constants used when a WcdmaProperty is exported to or imported from a JSON string
    public static final String TYPE = "type";
    public static final String MCC = "mcc";
    public static final String MNC = "mnc";
    public static final String UCID = "ucid";
    public static final String LAC = "lac";
    public static final String PSC = "psc";
    public static final String UARFCN = "uarfcn";
    public static final String BER = "ber";
    public static final String ECNO = "ecno";
    public static final String CONNECTED = "connected";

    public WcdmaProperty(int type, int mcc, int mnc, int ucid, int lac, int psc, int uarfcn, int dbm,
                         int ber, int ecno, boolean connected, double latitude, double longitude,
                         boolean isValidSignal, long measured_time) {
        this.type = type;

        this.mcc = mcc;
        this.mnc = mnc;
        this.ucid = ucid;
        this.lac = lac;
        /*
        the primary scrambling code is a static code defined during the
        planning of the cell infrastructure. The PSC of all neighboring cells
        must be different. So, it is a good way to identify different cells
        in a same measurement cycle when cells have an undefined MCC, MNC, CID,
        and LAC
        */
        this.psc = psc;
        this.uarfcn = uarfcn;

        /*
        asu_level and level can be derived from the dbm value (dBm = -113 + (2 * asu)).
        ber is a hidden value that we extract from the string representation of the
        CellSignalStrengthGsm.
        */
        this.dbm = dbm;
        this.ber = ber;
        this.ecno = ecno;

        // set to true if the device is connected to the antenna
        this.connected = connected;

        this.latitude = latitude;
        this.longitude = longitude;
        this.isValidSignal = isValidSignal;
        this.measured_time = measured_time;
    }

    public WcdmaProperty(boolean isValidSignal) {
        this.isValidSignal = isValidSignal;
    }

    public WcdmaProperty copy() {
        return new WcdmaProperty(
                type,
                mcc,
                mnc,
                ucid,
                lac,
                psc,
                uarfcn,
                dbm,
                ber,
                ecno,
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
        return Const.WCDMA_MAX_DBM;
    }

    /**
     * This method returns min possible dbm value for the corresponding signal
     *
     * @return the minimum possible dbm value for this signal type
     */
    @Override
    public int getMinDbm() {
        return Const.WCDMA_MIN_DBM;
    }

    /**
     * This method dump the signal to the database
     *
     * @param db a SQLiteDatabase obtained with getWritableDatabase()
     */
    @Override
    public void dumpSignalToDatabase(SQLiteDatabase db) {
        db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(this));
    }

    /**
     * Change the dBm value for the BaseProperty with invalid dBm to a value set to the minimum
     * value (minus 1) for the corresponding signal type.
     */
    @Override
    public void normalizeSignalWithInvalidDbm() {
        if (!isDbmValueInRange()) {
            dbm = Const.WCDMA_MIN_DBM - 1;
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
        return WCDMA_PROPERTY + ":"
                + " " + TYPE + "=" + type
                + " " + MCC + "=" + mcc
                + " " + MNC + "=" + mnc
                + " OperatorName=" + mOperatorName
                + " " + UCID + "=" + ucid
                + " " + LAC + "=" + lac
                + " " + PSC + "=" + psc
                + " " + UARFCN + "=" + uarfcn
                + " " + DBM + "=" + dbm
                + " " + BER + "=" + ber
                + " " + ECNO + "=" + ecno
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
            jsonObject.put(BASE_PROPERTY_TYPE, WCDMA_PROPERTY);
            jsonObject.put(TYPE, this.type);
            jsonObject.put(MCC, this.mcc);
            jsonObject.put(MNC, this.mnc);
            jsonObject.put(UCID, this.ucid);
            jsonObject.put(LAC, this.lac);
            jsonObject.put(PSC, this.psc);
            jsonObject.put(UARFCN, this.uarfcn);
            jsonObject.put(DBM, this.dbm);
            jsonObject.put(BER, this.ber);
            jsonObject.put(ECNO, this.ecno);
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

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

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.database.DbContract;
import fr.inria.es.electrosmart.database.DbHelper;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;

public class NewRadioProperty extends BaseProperty {
    private static final String TAG = "NewRadioProperty";

    // Constants used when a NewRadioProperty is exported to or imported from a JSON string
    public static final String TYPE = "type";
    public static final String MCC = "mcc";
    public static final String MNC = "mnc";
    public static final String NCI = "nci";
    public static final String NRARFCN = "nrarfcn";
    public static final String PCI = "pci";
    public static final String TAC = "tac";

    public static final String CSI_RSRP = "csi_rsrp";
    public static final String CSI_RSRQ = "csi_rsrq";
    public static final String CSI_SINR = "csi_sinr";
    public static final String SS_RSRP = "ss_rsrp";
    public static final String SS_RSRQ = "ss_rsrq";
    public static final String SS_SINR = "ss_sinr";
    public static final String CONNECTED = "connected";

    public NewRadioProperty(int type,
                            int mcc,
                            int mnc,
                            long nci,
                            int nrarfcn,
                            int pci,
                            int tac,
                            int csiRsrp,
                            int csiRsrq,
                            int csiSinr,
                            int ssRsrp,
                            int ssRsrq,
                            int ssSinr,
                            boolean connected,
                            double latitude,
                            double longitude,
                            boolean isValidSignal,
                            long measured_time) {

        this.type = type;

        // available data according to
        // https://developer.android.com/reference/android/telephony/CellIdentityNr.html
        this.mcc = mcc;
        this.mnc = mnc;
        this.nci = nci;
        this.nrarfcn = nrarfcn;
        this.pci = pci;
        this.tac = tac;

        // available data according to
        // https://developer.android.com/reference/android/telephony/CellSignalStrengthNr.html

        this.csiRsrp = csiRsrp;
        this.csiRsrq = csiRsrq;
        this.csiSinr = csiSinr;
        this.ssRsrp = ssRsrp;
        this.ssRsrq = ssRsrq;
        this.ssSinr = ssSinr;

        // when we access the dbm field, we get the csiRsrp in NR, if we change it we must also
        // update getMinDbm(), getMaxDbm(), Const.MIN_DISPLAYABLE_DBM, and Const.MAX_DISPLAYABLE_DBM
        // The dbm value is used to display the received power for any type of antenna
        this.dbm = csiRsrp;

        // set to true if the device is connected to the antenna
        this.connected = connected;

        this.latitude = latitude;
        this.longitude = longitude;
        this.isValidSignal = isValidSignal;
        this.measured_time = measured_time;
    }

    public NewRadioProperty(boolean isValidSignal) {
        this.isValidSignal = isValidSignal;
    }

    public NewRadioProperty copy() {
        return new NewRadioProperty(type,
                mcc,
                mnc,
                nci,
                nrarfcn,
                pci,
                tac,
                csiRsrp,
                csiRsrq,
                csiSinr,
                ssRsrp,
                ssRsrq,
                ssSinr,
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
        return Const.NR_MAX_CSI_RSRP;
    }

    /**
     * This method returns min possible dbm value for the corresponding signal
     *
     * @return the minimum possible dbm value for this signal type
     */
    @Override
    public int getMinDbm() {
        return Const.NR_MIN_CSI_RSRP;
    }

    /**
     * This method dump the signal to the database
     *
     * @param db a SQLiteDatabase obtained with getWritableDatabase()
     */
    @Override
    public void dumpSignalToDatabase(SQLiteDatabase db) {
        db.insert(DbContract.NEW_RADIO.TABLE_NAME, null, DbHelper.createNewRadioContentValues(this));
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

    @NonNull
    @Override
    public String toString() {
        return NEW_RADIO_PROPERTY + ":"
                + " " + TYPE + "=" + type
                + " " + MCC + "=" + mcc
                + " " + MNC + "=" + mnc
                + " " + NCI + "=" + nci
                + " " + NRARFCN + "=" + nrarfcn
                + " " + PCI + "=" + pci
                + " " + TAC + "=" + tac
                + " " + DBM + "=" + dbm
                + " " + CSI_RSRP + "=" + csiRsrp
                + " " + CSI_RSRQ + "=" + csiRsrq
                + " " + CSI_SINR + "=" + csiSinr
                + " " + SS_RSRP + "=" + ssRsrp
                + " " + SS_RSRQ + "=" + ssRsrq
                + " " + SS_SINR + "=" + ssSinr
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
            jsonObject.put(BASE_PROPERTY_TYPE, NEW_RADIO_PROPERTY);
            jsonObject.put(TYPE, this.type);
            jsonObject.put(MCC, this.mnc);
            jsonObject.put(MNC, this.mcc);
            jsonObject.put(NCI, this.nci);
            jsonObject.put(NRARFCN, this.nrarfcn);
            jsonObject.put(PCI, this.pci);
            jsonObject.put(TAC, this.tac);
            jsonObject.put(CSI_RSRP, this.csiRsrp);
            jsonObject.put(CSI_RSRQ, this.csiRsrq);
            jsonObject.put(CSI_SINR, this.csiSinr);
            jsonObject.put(SS_RSRP, this.ssRsrp);
            jsonObject.put(SS_RSRQ, this.ssRsrq);
            jsonObject.put(SS_SINR, this.ssSinr);
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

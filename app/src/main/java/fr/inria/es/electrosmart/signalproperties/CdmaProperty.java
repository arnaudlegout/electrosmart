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
import fr.inria.es.electrosmart.fragments.MeasurementFragment;

public class CdmaProperty extends BaseProperty {

    // Constants used when a CdmaProperty is exported to or imported from a JSON string
    public static final String TYPE = "type";
    public static final String NETWORK_ID = "network_id";
    public static final String SYSTEM_ID = "system_id";
    public static final String BASE_STATION_ID = "base_station_id";
    public static final String CDMA_LATITUDE = "cdma_latitude";
    public static final String CDMA_LONGITUDE = "cdma_longitude";
    public static final String CDMA_DBM = "cdma_dbm";
    public static final String CDMA_ECIO = "cdma_ecio";
    public static final String EVDO_DBM = "evdo_dbm";
    public static final String EVDO_ECIO = "evdo_ecio";
    public static final String EVDO_SNR = "evdo_snr";
    public static final String CONNECTED = "connected";

    public CdmaProperty(int type, int network_id, int system_id, int base_station_id,
                        int cdma_latitude, int cdma_longitude, int cdma_dbm, int cdma_ecio, int evdo_dbm,
                        int evdo_ecio, int evdo_snr, int dbm, boolean connected, double latitude,
                        double longitude, boolean isValidSignal, long measured_time) {
        this.type = type;
        // available data according to
        // http://developer.android.com/reference/android/telephony/CellIdentityCdma.html
        this.network_id = network_id;
        this.system_id = system_id;
        this.base_station_id = base_station_id;
        this.cdma_latitude = cdma_latitude;
        this.cdma_longitude = cdma_longitude;

        // available data according to
        // http://developer.android.com/reference/android/telephony/CellSignalStrengthCdma.html
        // the value asu_level, level, cdma_level, evdo_level, dbm are all computed base on the
        // other values, so we do not collect them.
        this.cdma_dbm = cdma_dbm;
        this.cdma_ecio = cdma_ecio;
        this.evdo_dbm = evdo_dbm;
        this.evdo_ecio = evdo_ecio;
        this.evdo_snr = evdo_snr;

        // set to true if the device is connected to the antenna
        this.connected = connected;

        /*
        This dbm value is a trick to manipulate the CdmaProperty as any other property. Indeed, all
        properties have a dbm field, but the CdmaProperty that has a cdma_dbm and an evdo_dbm.
        To simplify the code, I create artificially a dbm field for the CdmaProperty that is set to
        the max of cdma_dbm and evdo_dbm using the method Tools.getDbmForCdma(cdma_dbm, evdo_dbm)
        each time I create a CdmaProperty
        The dbm value is only used for the display of the signals, but it is not stored in the database.
        */
        this.dbm = dbm;

        this.latitude = latitude;
        this.longitude = longitude;
        this.isValidSignal = isValidSignal;
        this.measured_time = measured_time;
    }

    public CdmaProperty(boolean isValidSignal) {
        this.isValidSignal = isValidSignal;
    }

    public CdmaProperty copy() {
        return new CdmaProperty(
                type,
                network_id,
                system_id,
                base_station_id,
                cdma_latitude,
                cdma_longitude,
                cdma_dbm,
                cdma_ecio,
                evdo_dbm,
                evdo_ecio,
                evdo_snr,
                dbm,
                connected,
                latitude,
                longitude,
                isValidSignal,
                measured_time);
    }

    /**
     * This method returns max possible dbm value for the corresponding signal
     *
     * @return the maximum possible dbm value for this signal type
     */
    @Override
    public int getMaxDbm() {
        return Const.CDMA_MAX_DBM;
    }

    /**
     * This method returns min possible dbm value for the corresponding signal
     *
     * @return the minimum possible dbm value for this signal type
     */
    @Override
    public int getMinDbm() {
        return Const.CDMA_MIN_DBM;
    }

    /**
     * This method dump the signal to the database
     *
     * @param db a SQLiteDatabase obtained with getWritableDatabase()
     */
    @Override
    public void dumpSignalToDatabase(SQLiteDatabase db) {
        db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(this));
    }

    /**
     * Change the dBm value for the BaseProperty with invalid dBm to a value set to the minimum
     * value (minus 1) for the corresponding signal type.
     */
    @Override
    public void normalizeSignalWithInvalidDbm() {
        if (!isDbmValueInRange()) {
            dbm = Const.CDMA_MIN_DBM - 1;
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

    @Override
    public String toString() {
        return CDMA_PROPERTY + ":"
                + " " + TYPE + "=" + type
                + " " + NETWORK_ID + "=" + network_id
                + " " + SYSTEM_ID + "=" + system_id
                + " " + BASE_STATION_ID + "=" + base_station_id
                + " " + CDMA_LATITUDE + "=" + cdma_latitude
                + " " + CDMA_LONGITUDE + "=" + cdma_longitude
                + " " + CDMA_DBM + "=" + cdma_dbm
                + " " + CDMA_ECIO + "=" + cdma_ecio
                + " " + EVDO_DBM + "=" + evdo_dbm
                + " " + EVDO_ECIO + "=" + evdo_ecio
                + " " + EVDO_SNR + "=" + evdo_snr
                + " " + DBM + "=" + dbm
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
            jsonObject.put(BASE_PROPERTY_TYPE, CDMA_PROPERTY);
            jsonObject.put(TYPE, this.type);
            jsonObject.put(NETWORK_ID, this.network_id);
            jsonObject.put(SYSTEM_ID, this.system_id);
            jsonObject.put(BASE_STATION_ID, this.base_station_id);
            jsonObject.put(CDMA_LATITUDE, this.cdma_latitude);
            jsonObject.put(CDMA_LONGITUDE, this.cdma_longitude);
            jsonObject.put(CDMA_DBM, this.cdma_dbm);
            jsonObject.put(CDMA_ECIO, this.cdma_ecio);
            jsonObject.put(EVDO_DBM, this.evdo_dbm);
            jsonObject.put(EVDO_ECIO, this.evdo_ecio);
            jsonObject.put(EVDO_SNR, this.evdo_snr);
            jsonObject.put(DBM, this.dbm);
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

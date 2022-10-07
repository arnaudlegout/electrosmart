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

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.database.DbContract;
import fr.inria.es.electrosmart.database.DbHelper;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;

public class WifiProperty extends BaseProperty {

    // Constants used when a WifiProperty is exported to or imported from a JSON string
    public static final String SSID = "ssid";
    public static final String BSSID = "bssid";
    public static final String STARRED_BSSID = "starredBssid";
    public static final String OPERATOR_FRIENDLY_NAME = "operator_friendly_name";
    public static final String VENUE_NAME = "venue_name";
    public static final String IS_PASSPOINT_NETWORK = "is_passpoint_network";
    public static final String FREQ = "freq";
    public static final String CENTER_FREQ0 = "center_freq0";
    public static final String CENTER_FREQ1 = "center_freq1";
    public static final String CHANNEL_WIDTH = "channel_width";
    public static final String CAPABILITIES = "capabilities";
    public static final String WIFI_STANDARD = "wifi_standard";
    public static final String CONNECTED = "connected";

    public WifiProperty(String ssid, String bssid, String starredBssid, String operator_friendly_name,
                        String venue_name, int is_passpoint_network, int freq,
                        int center_freq0, int center_freq1, int channel_width,
                        String capabilities, int wifiStandard, int dbm, boolean connected, double latitude,
                        double longitude, boolean isValidSignal, long measured_time) {

        this.ssid = ssid;
        this.bssid = bssid;
        // this starredBssid is a bssid with a star at the beginning or at the end the truncated
        // BSSID that can be used for a display on the application
        this.starredBssid = starredBssid;
        this.operator_friendly_name = operator_friendly_name;
        this.venue_name = venue_name;
        this.is_passpoint_network = is_passpoint_network;
        this.freq = freq;
        this.center_freq0 = center_freq0;
        this.center_freq1 = center_freq1;
        this.channel_width = channel_width;
        this.capabilities = capabilities;
        this.wifiStandard = wifiStandard;

        this.dbm = dbm;

        this.connected = connected;

        this.latitude = latitude;
        this.longitude = longitude;
        this.isValidSignal = isValidSignal;
        this.measured_time = measured_time;
    }

    public WifiProperty(boolean isValidSignal) {
        this.isValidSignal = isValidSignal;
    }

    public WifiProperty copy() {
        return new WifiProperty(
                ssid,
                bssid,
                starredBssid,
                operator_friendly_name,
                venue_name,
                is_passpoint_network,
                freq,
                center_freq0,
                center_freq1,
                channel_width,
                capabilities,
                wifiStandard,
                dbm,
                connected,
                latitude,
                longitude,
                isValidSignal,
                measured_time);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence friendlySourceName(@NonNull Context context) {
        if (ssid.isEmpty()) {
            return context.getString(R.string.hidden_ssid);
        } else {
            return ssid;
        }
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public void setStarredBssid(String starredBssid) {
        this.starredBssid = starredBssid;
    }

    /**
     * This method returns max possible dbm value for the corresponding signal
     *
     * @return the maximum possible dbm value for this signal type
     */
    @Override
    public int getMaxDbm() {
        return Const.WIFI_MAX_DBM;
    }

    /**
     * This method returns min possible dbm value for the corresponding signal
     *
     * @return the minimum possible dbm value for this signal type
     */
    @Override
    public int getMinDbm() {
        return Const.WIFI_MIN_DBM;
    }

    /**
     * This method dump the signal to the database
     *
     * @param db a SQLiteDatabase obtained with getWritableDatabase()
     */
    @Override
    public void dumpSignalToDatabase(SQLiteDatabase db) {
        db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(this));
    }

    /**
     * Change the dBm value for the BaseProperty with invalid dBm to a value set to the minimum
     * value (minus 1) for the corresponding signal type.
     */
    @Override
    public void normalizeSignalWithInvalidDbm() {
        if (!isDbmValueInRange()) {
            dbm = Const.WIFI_MIN_DBM - 1;
        }
    }

    /**
     * Return the Antenna display corresponding to this signal
     *
     * @return the Antenna display corresponding to this signal
     */
    @Override
    public MeasurementFragment.AntennaDisplay getAntennaDisplay() {
        return MeasurementFragment.AntennaDisplay.WIFI;
    }

    @Override
    public String toString() {
        return WIFI_PROPERTY + ":"
                + " " + SSID + "=" + ssid
                + " " + BSSID + "=" + bssid
                + " " + STARRED_BSSID + "=" + starredBssid
                + " " + OPERATOR_FRIENDLY_NAME + "=" + operator_friendly_name
                + " " + VENUE_NAME + "=" + venue_name
                + " " + IS_PASSPOINT_NETWORK + "=" + is_passpoint_network
                + " " + FREQ + "=" + freq
                + " " + CENTER_FREQ0 + "=" + center_freq0
                + " " + CENTER_FREQ1 + "=" + center_freq1
                + " " + CHANNEL_WIDTH + "=" + channel_width
                + " " + CAPABILITIES + "=" + capabilities
                + " " + CAPABILITIES + "=" + capabilities
                + " " + WIFI_STANDARD + "=" + wifiStandard
                + " " + DBM + "=" + dbm
                + " " + CONNECTED + "=" + connected
                + " " + LATITUDE + "=" + latitude
                + " " + LONGITUDE + "=" + longitude
                + " " + IS_VALID_SIGNAL + "=" + isValidSignal
                + " " + MEASURED_TIME + "=" + measured_time;
    }

    public String toJsonString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BASE_PROPERTY_TYPE, WIFI_PROPERTY);
            jsonObject.put(SSID, this.ssid);
            jsonObject.put(BSSID, this.bssid);
            jsonObject.put(STARRED_BSSID, this.starredBssid);
            jsonObject.put(OPERATOR_FRIENDLY_NAME, this.operator_friendly_name);
            jsonObject.put(VENUE_NAME, this.venue_name);
            jsonObject.put(IS_PASSPOINT_NETWORK, this.is_passpoint_network);
            jsonObject.put(FREQ, this.freq);
            jsonObject.put(CENTER_FREQ0, this.center_freq0);
            jsonObject.put(CENTER_FREQ1, this.center_freq1);
            jsonObject.put(CHANNEL_WIDTH, this.channel_width);
            jsonObject.put(CAPABILITIES, this.capabilities);
            jsonObject.put(WIFI_STANDARD, this.wifiStandard);
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

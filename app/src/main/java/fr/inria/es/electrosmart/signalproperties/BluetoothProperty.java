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

public class BluetoothProperty extends BaseProperty {

    // Constants used when a BluetoothProperty is exported to or imported from a JSON string
    public static final String BT_DEVICE_NAME = "bt_device_name";
    public static final String BT_DEVICE_NAME_ALIAS = "bt_device_name_alias";
    public static final String BT_ADDRESS = "bt_address";
    public static final String BT_DEVICE_CLASS = "bt_device_class";
    public static final String BT_BOND_STATE = "bt_bond_state";
    public static final String BT_DEVICE_TYPE = "bt_device_type";

    public BluetoothProperty(String bt_device_name,
                             String bt_device_name_alias,
                             String address,
                             int bt_device_class,
                             int bt_device_type,
                             int bond_state,
                             int dbm,
                             double latitude,
                             double longitude,
                             boolean isValidSignal,
                             long measured_time) {

        this.bt_device_name = bt_device_name;
        this.bt_device_name_alias = bt_device_name_alias;
        this.bt_address = address;
        this.bt_device_class = bt_device_class; // http://developer.android.com/reference/android/bluetooth/BluetoothClass.Device.html
        this.bt_bond_state = bond_state;
        this.bt_device_type = bt_device_type;  // classic, LE (low energy), or dual.

        this.dbm = dbm;

        this.latitude = latitude;
        this.longitude = longitude;
        this.isValidSignal = isValidSignal;
        this.measured_time = measured_time;
    }

    public BluetoothProperty(boolean isValidSignal) {
        this.isValidSignal = isValidSignal;
    }

    public BluetoothProperty copy() {
        return new BluetoothProperty(
                bt_device_name,
                bt_device_name_alias,
                bt_address,
                bt_device_class,
                bt_device_type,
                bt_bond_state,
                dbm,
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
        if (bt_device_name == null || bt_device_name.isEmpty()) {
            return context.getString(R.string.not_available);
        } else {
            return bt_device_name;
        }
    }

    /**
     * This method returns max possible dbm value for the corresponding signal
     *
     * @return the maximum possible dbm value for this signal type
     */
    @Override
    public int getMaxDbm() {
        return Const.BT_MAX_DBM;
    }

    /**
     * This method returns min possible dbm value for the corresponding signal
     *
     * @return the minimum possible dbm value for this signal type
     */
    @Override
    public int getMinDbm() {
        return Const.BT_MIN_DBM;
    }

    /**
     * This method dump the signal to the database
     *
     * @param db a SQLiteDatabase obtained with getWritableDatabase()
     */
    @Override
    public void dumpSignalToDatabase(SQLiteDatabase db) {
        db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(this));
    }

    /**
     * Change the dBm value for the BaseProperty with invalid dBm to a value set to the minimum
     * value (minus 1) for the corresponding signal type.
     */
    @Override
    public void normalizeSignalWithInvalidDbm() {
        if (!isDbmValueInRange()) {
            dbm = Const.BT_MIN_DBM - 1;
        }
    }

    /**
     * Return the Antenna display corresponding to this signal
     *
     * @return the Antenna display corresponding to this signal
     */
    @Override
    public MeasurementFragment.AntennaDisplay getAntennaDisplay() {
        return MeasurementFragment.AntennaDisplay.BLUETOOTH;
    }

    @Override
    public String toString() {
        return BLUETOOTH_PROPERTY + ":"
                + " " + BT_DEVICE_NAME + "=" + bt_device_name
                + " " + BT_DEVICE_NAME_ALIAS + "=" + bt_device_name_alias
                + " " + BT_ADDRESS + "=" + bt_address
                + " " + BT_DEVICE_CLASS + "=" + bt_device_class
                + " " + BT_DEVICE_TYPE + "=" + bt_device_type
                + " " + BT_BOND_STATE + "=" + bt_bond_state
                + " " + DBM + "=" + dbm
                + " " + LATITUDE + "=" + latitude
                + " " + LONGITUDE + "=" + longitude
                + " " + IS_VALID_SIGNAL + "=" + isValidSignal
                + " " + MEASURED_TIME + "=" + measured_time;
    }

    @Override
    public String toJsonString() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(BASE_PROPERTY_TYPE, BLUETOOTH_PROPERTY);
            jsonObject.put(BT_DEVICE_NAME, this.bt_device_name);
            jsonObject.put(BT_DEVICE_NAME_ALIAS, this.bt_device_name_alias);
            jsonObject.put(BT_ADDRESS, this.bt_address);
            jsonObject.put(BT_DEVICE_CLASS, this.bt_device_class);
            jsonObject.put(BT_BOND_STATE, this.bt_bond_state);
            jsonObject.put(BT_DEVICE_TYPE, this.bt_device_type);
            jsonObject.put(DBM, this.dbm);
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

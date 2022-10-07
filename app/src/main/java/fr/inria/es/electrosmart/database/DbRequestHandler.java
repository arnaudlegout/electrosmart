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

package fr.inria.es.electrosmart.database;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.UserProfile;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.fragments.StatisticsFragment;
import fr.inria.es.electrosmart.monitors.LocationMonitor;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.BluetoothProperty;
import fr.inria.es.electrosmart.signalproperties.CdmaProperty;
import fr.inria.es.electrosmart.signalproperties.GsmProperty;
import fr.inria.es.electrosmart.signalproperties.LteProperty;
import fr.inria.es.electrosmart.signalproperties.NewRadioProperty;
import fr.inria.es.electrosmart.signalproperties.WcdmaProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;

public class DbRequestHandler {

    private static final String SELECT = " SELECT ";
    private static final String WHERE = " WHERE ";
    private static final String FROM = " FROM ";
    private static final String GROUP_BY = " GROUP BY ";
    private static final String MAX = " MAX ";
    private static final String MIN = " MIN ";
    private static final String COMMA = ",";
    private static final String LOWER = "  <  ";
    private static final String LOWER_EQ = "  <=  ";
    private static final String LARGER = "  >  ";
    private static final String LARGER_EQ = "  >=  ";
    private static final String LOWER_ARGS = "  <  ? ";
    private static final String LARGER_EQ_ARGS = "  >= ? ";
    private static final String AND = " AND ";
    private static final String ALL = " * ";
    private static final String AS = " AS ";
    private static final String COUNT = " COUNT ";
    private static final String LIMIT1 = " 1 ";
    private static final String _ID = " _id ";
    private static final String DESC = " DESC ";
    private static final String TAG = "DbRequestHandler";

    // used by the getOperator() method to cache DB request results for already searched (mcc, mnc)
    private static HashMap<String, String> operatorCache = new HashMap<>();

    public DbRequestHandler() {
    }

    public static void createDebugDatabase() {
        Runnable runnable = () -> {
            SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
            SharedPreferences.Editor edit = MainApplication.getContext().
                    getSharedPreferences(Const.SHARED_PREF_FILE, 0).edit();
            Log.d(TAG, "############################ BEGIN test database ########################");
            try {
                // in case of failure will be rolled back and installed again at the next start.
                db.beginTransaction();

                BaseProperty es;

                Calendar cal = Calendar.getInstance();
                // shift the test DB by one hour to be sure that no real measurements will be
                // in a time slot of the test DB
                cal.add(Calendar.HOUR, -1);

                // Round to the start of the day
                Tools.roundCalendarAtMidnight(cal);

                // statistics demo. We create fake signals that looks realistic for user tests
                for (int i = 0; i < 24; i++) {
                    cal.add(Calendar.HOUR, -1);
                    //Log.d(TAG, "run: cal: " + cal.getTime());
                    es = new WifiProperty("Ma freebox", "51:a1:ab:99:55:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -38 - 3 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                    es = new WifiProperty("Free Wifi", "51:a1:ab:99:55:54", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -40 - 3 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new WifiProperty("WiFi Fon", "51:a1:ab:" + i + ":55:54", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -80 - 3 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));


                    if (i == 12) {
                        es = new WifiProperty("iPhone Julie", "51:a1:ab:99:50:55", "",
                                "", "", -1, 2447,
                                0, 0, 0, "", 0, -20,
                                false, 0.0, 0.0, true, cal.getTime().getTime());
                        db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                    }

                    es = new BluetoothProperty("Bose speakers",
                            "Bose speakers",
                            "dd:11:ab:99:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -50 - 3 * i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new BluetoothProperty("TV Samsung marc",
                            "TV Samsung marc",
                            "dd:11:ab:99:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -60 - 3 * i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new BluetoothProperty("Casque BT",
                            "Casque BT",
                            "dd:11:ab:" + i + ":55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -85 - 3 * i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new BluetoothProperty("Nest",
                            "Nest",
                            "dd:22:ab:" + i + ":55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -85 - 3 * i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));


                    es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 20,
                            508, 55, 508, 508, 12, 508,
                            -80 - 3 * i, 30, -15, 15,
                            15, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));


                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_EDGE, 208,
                            20, 502, 85098 + i,
                            555678, 97,
                            50, -70 - 3 * i, 5, false,
                            0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));


                    NewRadioProperty newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208,
                            20,
                            1002,
                            3279163,
                            430,
                            4993,
                            -98,
                            -8,
                            -4,
                            -63,
                            -12,
                            11,
                            true,
                            0.0,
                            0.0,
                            true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));
                }

                for (int i = 0; i < 24; i++) {
                    cal.add(Calendar.HOUR, -1);
                    //Log.d(TAG, "run: cal: " + cal.getTime());
                    es = new WifiProperty("Ma freebox", "51:a1:ab:99:55:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, 30 - 3 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                    es = new WifiProperty("Free Wifi", "51:a1:ab:99:55:54", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, 40 - i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new WifiProperty("SFR Wifi", "51:a1:a3:99:55:54", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -50 - 2 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));


                    es = new BluetoothProperty("Bose speakers",
                            "Bose speakers",
                            "dd:11:ab:99:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -50 - i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new BluetoothProperty("Casque audio",
                            "Casque audio",
                            "dd:11:ab:11:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -55 - i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));


                    es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 20,
                            508, 55, 508, 508, 12, 508,
                            -80, 30, -15, 15,
                            15, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_EDGE, 208,
                            20, 502, 85098 + i,
                            555678, 97,
                            50, -70, 5, false,
                            0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));


                    NewRadioProperty newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 20, 1002, 3279163,
                            430, 4993,
                            -98, -8, -4,
                            -63, -12, 11,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));

                }

                //New Statistics tests

                // Statistics
                // test the presence of only one signal in a single day
                //   - Check the number of sources should be 1 and that of the corresponding protocol
                //   - Check most exposing kind of sources should be 100% for the corresponding
                //     protocol
                //   - Check the values are all added at 11pm
                //   - Check the values are as below:
                //          Yesterday (D-1)   WiFi             "Ma freebox" [NEW]      80/100  +20 high
                //                     D-2    Bluetooth        "Bose speakers" [NEW]   60/100  +14 moderate
                //                     D-7    Cellular (5G)    "Bouygues Telecom"      46/100  -12 moderate
                //                     D-3    Cellular (4G)    "Unknown operator"      58/100  +12 moderate
                //                     D-4    Cellular (3G)    "Unknown operator"      46/100  +14 moderate
                //                     D-5    Cellular (2G)    "Nexium Telecommunic."  32/100  -24 low
                //                     D-6    Cellular (CDMA)  "Network ID: 10"        56/100      moderate
                //                     D-8    There is no statistics

                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("Ma freebox", "51:a1:ab:99:55:55", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -18,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                cal.add(Calendar.DAY_OF_MONTH, -1);

                es = new BluetoothProperty("Bose speakers",
                        "Bose speakers",
                        "dd:11:ab:99:55:51",
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                        1,
                        BluetoothDevice.BOND_NONE, -50, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                cal.add(Calendar.DAY_OF_MONTH, -1);

                NewRadioProperty newRadioProperty = new NewRadioProperty(
                        TelephonyManager.NETWORK_TYPE_NR,
                        208, 20, 1002, 3279163,
                        430, 4993,
                        -70, -8, -4,
                        -63, -12, 11,
                        true, 0.0, 0.0, true,
                        cal.getTime().getTime()
                );
                db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                        DbHelper.createNewRadioContentValues(newRadioProperty));

                cal.add(Calendar.DAY_OF_MONTH, -1);

                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                        508, 55, 508, 508, 12, 508,
                        -50, 30, -15, 15,
                        15, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                cal.add(Calendar.DAY_OF_MONTH, -1);

                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 555, 555,
                        555, 555, 55, 555,
                        -70, 5, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                cal.add(Calendar.DAY_OF_MONTH, -1);

                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 505,
                        27, 30, 86098,
                        555678, 97,
                        50, -90, 5, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                cal.add(Calendar.DAY_OF_MONTH, -1);

                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, 10, 20, 30, 31, 32, -55, -56, -58, -60, -62, -62, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));


                cal.add(Calendar.DAY_OF_MONTH, -1);

                Tools.roundCalendarAtMidnight(cal);

                /*
                chart with all signals levels
                To check
                    - the score is 84/100
                    - the score difference is -2 (bottom arrow)
                    - There is a single wifi (signals of the same group are identified as a
                    single top 5)
                    - WiFi is high (red)
                    - BT is moderate (orange)
                    - cellular is low (green)
                    - when expending Wifi, the highlight is red
                    - when expending BT, the highlight is orange
                    - when expending Cellular, the highlight is green
                    - all top 5 are not new
                    - Wi-Fi is 99% and other signals are <1%
                    - There are 1 Wi-Fi, 1 BT, and 1 cellular sources
                    - The top 5 Wifi score is 92, the bluetooth is 70, and the cellular is 42
                */
                for (int i = 0; i < 24; i++) {
                    cal.add(Calendar.HOUR, -1);
                    //Log.d(TAG, "run: cal: " + cal.getTime());
                    es = new WifiProperty("Single Wifi", "51:a1:ab:99:a5:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -1 - 6 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                    es = new WifiProperty("Single Wifi name 2", "51:a1:ab:99:a5:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -2 - 6 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                    es = new WifiProperty("Single Wifi name 3", "51:a1:ab:99:a5:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -3 - 6 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new BluetoothProperty("Single BT",
                            "Test Stats Wi-Fi Bluetooth alias",
                            "dd:11:ab:99:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -35 - 3 * i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 505,
                            501, 502, 85098,
                            555678, 97,
                            50, -76 - 2 * i, 5, false,
                            0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));
                }

                /*
                chart with all signals levels
                To check
                    - the score is 54/100
                    - the score difference is -16 (down arrow)
                    - all three top five signals are moderate (orange)
                    - Wi-Fi and BT are new
                    - Wi-Fi is 33% and BT is 33% and Cellular is 33%
                    - There are 1 Wi-Fi, 1 BT, and 1 cellular sources
                    - The top 5 Wifi score is 58, the bluetooth is 58, and the cellular is 58
                */
                for (int i = 0; i < 24; i++) {
                    cal.add(Calendar.HOUR, -1);
                    //Log.d(TAG, "run: cal: " + cal.getTime());
                    es = new WifiProperty("Single Wifi", "51:a1:ab:99:a5:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -51 - 6 * i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new WifiProperty("Single Wifi 2", "51:a1:ab:99:a5:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -110,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new WifiProperty("Single Wifi 3", "51:a1:ab:99:a5:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -115,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new BluetoothProperty("Single BT",
                            "Test Stats Wi-Fi Bluetooth alias",
                            "dd:11:ab:99:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -51 - 6 * i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 505,
                            501, 522, 85099,
                            557678, 97,
                            50, -51 - 6 * i, 5, false,
                            0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));
                }


                /*
                chart with all signals levels
                To check
                    - the score is 70/100
                    - the score difference is 0 (horizontal arrow)
                    - Wi-Fi and BT signals are red
                    - Two cellular signals are orange and one is green
                    - Wi-Fi is 49% and BT is 49% and Cellular is 1%
                    - There are 1 Wi-Fi, 1 BT, and 3 cellular sources
                    - when selecting the first cellar signal, it expends and highlight the first in the list
                    - when selecting the second cellar signal, it expends and highlight the second in the list
                    - when selecting the third cellar signal, it expends and highlight the third in the list
                */
                for (int i = 0; i < 24; i++) {
                    cal.add(Calendar.HOUR, -1);
                    //Log.d(TAG, "run: cal: " + cal.getTime());
                    es = new WifiProperty("Single Wifi (new)", "51:a1:ac:99:55:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -30 - i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new BluetoothProperty("Single BT",
                            "Test Stats Wi-Fi Bluetooth alias",
                            "dd:11:ac:99:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -30 - i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                            508, 55, 508, 508, 12, 508,
                            -50, 30, -15, 15,
                            15, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                    es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 555, 555,
                            555, 555, 55, 555,
                            -70, 5, BaseProperty.UNAVAILABLE, false, 0.0,
                            0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 505,
                            27, 30, 86098,
                            555678, 97,
                            50, -90 - i, 5, false,
                            0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));
                }

                /*
                chart with all signals levels
                To check
                    - the score is 70/100
                    - the diff is invisible
                    - There are 1 Wi-Fi, 1 BT, and 3 cellular sources (4 with API 29 and higher)
                */
                for (int i = 0; i < 24; i++) {
                    cal.add(Calendar.HOUR, -1);
                    //Log.d(TAG, "run: cal: " + cal.getTime());
                    es = new WifiProperty("Single Wifi (new)", "51:a1:ac:99:55:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -30 - i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new BluetoothProperty("Single BT",
                            "Test Stats Wi-Fi Bluetooth alias",
                            "dd:11:ac:99:55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -30 - i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        newRadioProperty = new NewRadioProperty(
                                TelephonyManager.NETWORK_TYPE_NR,
                                208, 20, 1002, 3279163,
                                430, 4993,
                                -51, -8, -4,
                                -63, -12, 11,
                                true, 0.0, 0.0, true,
                                cal.getTime().getTime()
                        );
                        db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                                DbHelper.createNewRadioContentValues(newRadioProperty));
                    }

                    es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                            508, 55, 508, 508, 12, 508,
                            -50, 29, -15, 15,
                            15, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                    es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 555, 555,
                            555, 555, 55, 555,
                            -70, 5, BaseProperty.UNAVAILABLE, false, 0.0,
                            0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 505,
                            27, 30, 86098,
                            555678, 97,
                            50, -90 - i, 5, false,
                            0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));
                }

                // no data test case.
                cal.add(Calendar.DAY_OF_YEAR, -1);
                cal.add(Calendar.HOUR, -1);

                // test various RSRP, RSSI valid and invalid combinations for LTE
                // must show the RSSI value to -51 dBm
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                        508, 55, 508, 507, 12, 508,
                        Const.LTE_MIN_RSRP - 1, Const.LTE_MAX_RSSI, -15, 15,
                        15, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                // must show the RSSI value to -63 dBm
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                        508, 55, 508, 508, 12, 508,
                        -50, 25, -15, 15,
                        15, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                // must show the RSRP value to -90 dBm
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                        508, 55, 508, 509, 12, 508,
                        -90, Const.LTE_MAX_RSSI + 1, -15, 15,
                        15, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                // must show the no dBm values
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                        508, 55, 508, 510, 12, 508,
                        Const.LTE_MAX_RSRP + 1, Const.LTE_MAX_RSSI + 1, -15, 15,
                        15, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                cal.add(Calendar.HOUR, -23);

                 /*
                To check
                    - There are 24 Wi-Fi, 24 BT, and 72 cellular sources
                */
                for (int i = 0; i < 24; i++) {
                    cal.add(Calendar.HOUR, -1);
                    //Log.d(TAG, "run: cal: " + cal.getTime());
                    es = new WifiProperty("Single Wifi (new)", "51:a1:ac:" + i + ":55:55", "",
                            "", "", -1, 2447,
                            0, 0, 0, "", 0, -30 - i,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    es = new BluetoothProperty("Single BT",
                            "Test Stats Wi-Fi Bluetooth alias",
                            "dd:11:ac:" + i + ":55:51",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY,
                            1,
                            BluetoothDevice.BOND_NONE, -30 - i, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 508, 508,
                            508, 55, 508, i, 12, 508,
                            -50, 30, -15, 15,
                            15, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));


                    es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 555, 555,
                            555, 555, 55, i,
                            -70, 5, BaseProperty.UNAVAILABLE, false, 0.0,
                            0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, i,
                            27, 30, 86098,
                            555678, 97,
                            50, -90 - i, 5, false,
                            0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));
                }


                /*
                To check
                    - check the highlight for each group is of the correct color (marked in the SSID of the group)
                */
                cal.add(Calendar.HOUR, -1);

                //Log.d(TAG, "run: cal: " + cal.getTime());
                es = new WifiProperty("Single Wifi red", "51:a1:ac:ee:55:55", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -30,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                es = new WifiProperty("Single Wifi red", "51:a1:ac:ee:55:56", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -50,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                es = new WifiProperty("Single Wifi red", "51:a1:ac:ee:55:57", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -80,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));


                cal.add(Calendar.HOUR, -1);

                es = new WifiProperty("Single Wifi orange", "51:a1:ac:ab:55:55", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -45,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                es = new WifiProperty("Single Wifi orange", "51:a1:ac:ab:55:56", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -50,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                es = new WifiProperty("Single Wifi orange", "51:a1:ac:ab:55:57", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -80,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                cal.add(Calendar.HOUR, -1);

                es = new WifiProperty("Single Wifi green", "51:a1:ac:cc:55:55", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -80,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                es = new WifiProperty("Single Wifi green", "51:a1:ac:cc:55:56", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -80,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                es = new WifiProperty("Single Wifi green", "51:a1:ac:cc:55:57", "",
                        "", "", -1, 2447,
                        0, 0, 0, "", 0, -80,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                cal.add(Calendar.HOUR, -21);

                // ####################### TEST SUM DBM and EXPOSURE DOT CONSISTENCY ###########

                // ### test when we exceed max displayable value

                cal.add(Calendar.HOUR, -1);
                // 20 wifi signals belonging to the same antenna, the sum must be the max (-1 dbm)
                for (int i = 0; i < 20; i++) {
                    es = new WifiProperty("Test score > 100", "10:10:10:10:10:" + String.format(Locale.US, "%02d", i), "",
                            "", "", -1, 2437,
                            0, 0, 0, "", 0, -1,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                }

                cal.add(Calendar.HOUR, -1);
                // 20 wifi signals belonging to independent antennas. the sum must be 12dbm
                for (int i = 0; i < 20; i++) {
                    es = new WifiProperty("Test score > 100", "10:10:10:" + i + ":10:10", "",
                            "", "", -1, 2437,
                            0, 0, 0, "", 0, -1,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                }

                cal.add(Calendar.HOUR, -1);
                // 20 bt signals. the sum must be 12dbm
                for (int i = 0; i < 20; i++) {
                    es = new BluetoothProperty("Test score > 100",
                            "TEST BT0 ALIAS " + i,
                            "ff:ff:ff:ff:aa:00",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                            1,
                            BluetoothDevice.BOND_NONE, -1, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));
                }

                cal.add(Calendar.HOUR, -1);
                // 20 cellular signals. the sum must be -38 dbm
                for (int i = 0; i < 20; i++) {
                    es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MAX_MCC, Const.MAX_MNC,
                            Const.LTE_MAX_ECI, Const.LTE_MAX_PCI, Const.LTE_MAX_TAC, i, 12, Const.LTE_MAX_TA,
                            Const.LTE_MAX_RSRP, Const.LTE_MAX_RSSI, Const.LTE_MAX_RSRQ, Const.LTE_MAX_RSSNR,
                            Const.LTE_MAX_CQI, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));
                }


                // ### test low signals that sum to moderate

                cal.add(Calendar.HOUR, -1);
                // 5 wifi signals in low, the sum must be moderate
                for (int i = 0; i < 5; i++) {
                    es = new WifiProperty("color dot moderate", "10:10:10:" + i + ":10:10", "",
                            "", "", -1, 2437,
                            0, 0, 0, "", 0, -76,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                }

                // 5 bt signals in low, the sum must be moderate
                for (int i = 0; i < 5; i++) {
                    es = new BluetoothProperty("color dot moderate",
                            "TEST BT0 ALIAS " + i,
                            "ff:ff:ff:ff:aa:00",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                            1,
                            BluetoothDevice.BOND_NONE, -76, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));
                }

                // 5 cellular signals in low, the sum must be moderate
                for (int i = 0; i < 5; i++) {
                    es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MAX_MCC, Const.MAX_MNC,
                            Const.LTE_MAX_ECI, Const.LTE_MAX_PCI, Const.LTE_MAX_TAC, i, 12, Const.LTE_MAX_TA,
                            -76, 19, Const.LTE_MAX_RSRQ, Const.LTE_MAX_RSSNR,
                            Const.LTE_MAX_CQI, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));
                }

                // ### test moderate signals that sum to high
                cal.add(Calendar.HOUR, -1);
                // 5 wifi signals in moderate, the sum must be high
                for (int i = 0; i < 5; i++) {
                    es = new WifiProperty("color dot high", "10:10:10:" + i + ":10:10", "",
                            "", "", -1, 2437,
                            0, 0, 0, "", 0, -35,
                            false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                }

                // 5 bt signals in moderate, the sum must be high
                for (int i = 0; i < 5; i++) {
                    es = new BluetoothProperty("color dot high",
                            "TEST BT0 ALIAS " + i,
                            "ff:ff:ff:ff:aa:00",
                            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                            1,
                            BluetoothDevice.BOND_NONE, -35, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));
                }

                // we only populate data for the last two days from now
                // ####################### TEST WIFI GROUPS PER HOURS ##################
                // test when the first SSID is hidden

                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:10:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -10,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test1_x", "10:10:10:10:10:11", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -15,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test1_y", "10:10:10:10:10:12", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -16,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // test for two hidden ssid first
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:11:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -10,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("", "10:10:10:10:11:11", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -61,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test2_x", "10:10:10:10:11:12", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -62,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // test for only hidden ssid
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:12:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -10,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("", "10:10:10:10:12:11", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -81,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("", "10:10:10:10:12:12", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -81,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // test for only one hidden ssid
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:13:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -10,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // test BSSID aggregation on the last Bytes
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:11:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -10,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test4_x", "11:10:10:10:11:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -80,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test4_y", "12:10:10:10:11:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -100,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // test the color scheme (low exposure, medium exposure, high exposure)
                // The thresholds are given in Tools.dbmToLevel()
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:11:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -10,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:11:11", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -60,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("", "10:10:10:10:11:12", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -80,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                cal.add(Calendar.HOUR, -1); //empty hour slot to test the lack of signals

                // ####################### TEST ALL SIGNALS IN SAME HISTORY ##################
                //test long ssid and name
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("not bold 123456789012345678901234567890123456789012345678989",
                        "11:10:10:10:11:10", "", "",
                        "", -1, 2437, 0,
                        0, 0, "", 0, -32, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("bold 123456789012345678901234567890123456789012345678989",
                        "11:10:10:10:11:11", "", "",
                        "", -1, 2437, 0,
                        0, 0, "", 0, -33, true,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("not bold 123456789012345678901234567890123456789012345678989",
                        "11:10:10:10:11:12", "", "",
                        "", -1, 2437, 0,
                        0, 0, "", 0, -34, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("not bold", "12:10:10:10:12:10",
                        "", "", "",
                        -1, 2437, 0, 0,
                        0, "", 0, -35, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("bold", "11:15:10:10:13:11", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, -36,
                        true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("not bold", "11:10:18:10:14:12",
                        "", "", "",
                        -1, 2437, 0, 0,
                        0, "", 0, -37, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new BluetoothProperty("TEST UNBOUNDED BT0 123456789012345678901234567890123456789012345678989",
                        "TEST BT0 ALIAS",
                        "ff:ff:ff:ff:aa:00",
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                        1,
                        BluetoothDevice.BOND_NONE, -18, 0.0, 0.0, true, cal.getTime().getTime());

                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                es = new BluetoothProperty("TEST BONDED BT1 123456789012345678901234567890123456789012345678989",
                        "TEST BT1 ALIAS",
                        "ff:ff:ff:ff:aa:01",
                        BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER,
                        1,
                        BluetoothDevice.BOND_BONDED, -16, 0.0, 0.0, true, cal.getTime().getTime());

                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                es = new BluetoothProperty("TEST UNBOUNDED BT2 123456789012345678901234567890123456789012345678989",
                        "TEST BT2 ALIAS",
                        "ff:ff:ff:ff:aa:02",
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                        1,
                        BluetoothDevice.BOND_NONE, -20, 0.0, 0.0, true, cal.getTime().getTime());

                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 240, 60, 30, 31, 0, 0, 0, -40, -45, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_EDGE, 208, 2, 31, 31, 0, 0, 0, -41, -45, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 208, 2, 32, 31, 0, 0, 0, -42, -45, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 240, 60, 30, 31, 32, 0, -40, -45, BaseProperty.UNAVAILABLE, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_HSDPA, 208, 2, 31, 31, 32, 0, -45, -45, BaseProperty.UNAVAILABLE, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_HSPAP, 208, 2, 32, 31, 32, 0, -90, -45, BaseProperty.UNAVAILABLE, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 240, 60,
                        30, 31, 32, 0, 12, 33, -50,
                        31, -52, -53, -54, false, 0.0, 0.0,
                        true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 3, 31,
                        31, 32, 0, 12, 33, -55, 31,
                        -52, -53, -54, true, 0.0, 0.0,
                        true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 3, 32,
                        31, 32, 0, 12, 33, -60, 31,
                        -52, -53, -54, false, 0.0, 0.0,
                        true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 20, 1002, 3279163,
                            430, 4993,
                            -98, -8, -4,
                            -63, -12, 11,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 15, 1002, 3279163,
                            430, 4993,
                            -88, -8, -4,
                            -63, -12, 11,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 10, 1002, 3279163,
                            430, 4993,
                            -78, -8, -4,
                            -63, -12, 11,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));
                }

                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, 10, 20, 30, 31, 32, -55, -56, -58, -60, -62, -62, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_EVDO_0, 10, 20, 31, 31, 32, -56, -56, -58, -60, -62, -62, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_EVDO_A, 10, 20, 32, 31, 32, -57, -56, -58, -60, -62, -62, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                //  ********************* Test advanced cellular display *****************
                cal.add(Calendar.HOUR, -1);

                // ### GSM ###
                // exceed max
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, Const.MAX_MCC + 1,
                        Const.MAX_MNC + 1, Const.GSM_MAX_CID + 1, Const.GSM_MAX_LAC + 1,
                        Const.GSM_MAX_ARFCN + 1, Const.GSM_MAX_BSIC + 1,
                        Const.GSM_MAX_TA + 1, Const.GSM_MAX_DBM + 1, Const.GSM_MAX_BER + 1, true,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                // max
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, Const.MAX_MCC,
                        Const.MAX_MNC, Const.GSM_MAX_CID, Const.GSM_MAX_LAC,
                        Const.GSM_MAX_ARFCN, Const.GSM_MAX_BSIC,
                        Const.GSM_MAX_TA, Const.GSM_MAX_DBM, Const.GSM_MAX_BER, true,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                // min
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, Const.MIN_MCC,
                        Const.MIN_MNC, Const.GSM_MIN_CID, Const.GSM_MIN_LAC,
                        Const.GSM_MIN_ARFCN, Const.GSM_MIN_BSIC,
                        Const.GSM_MIN_TA, Const.GSM_MIN_DBM, Const.GSM_MIN_BER, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                // exceed min
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, Const.MIN_MCC - 1,
                        Const.MIN_MNC - 1, Const.GSM_MIN_CID - 1, Const.GSM_MIN_LAC - 1,
                        Const.GSM_MIN_ARFCN - 1, Const.GSM_MIN_BSIC - 1,
                        Const.GSM_MIN_TA - 1, Const.GSM_MIN_DBM - 1, Const.GSM_MIN_BER - 1, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                cal.add(Calendar.HOUR, -1);

                // ### WCDMA ###
                // exceed max
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, Const.MAX_MCC + 1, Const.MAX_MNC + 1,
                        Const.WCDMA_MAX_UCID + 1, Const.WCDMA_MAX_LAC + 1, Const.WCDMA_MAX_PSC + 1, Const.WCDMA_MAX_UARFCN + 1,
                        Const.WCDMA_MAX_DBM + 1, Const.WCDMA_MAX_BER + 1, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                // max
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, Const.MAX_MCC, Const.MAX_MNC,
                        Const.WCDMA_MAX_UCID, Const.WCDMA_MAX_LAC, Const.WCDMA_MAX_PSC, Const.WCDMA_MAX_UARFCN,
                        Const.WCDMA_MAX_DBM, Const.WCDMA_MAX_BER, BaseProperty.UNAVAILABLE, true, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                // min
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, Const.MIN_MCC, Const.MIN_MNC,
                        Const.WCDMA_MIN_UCID, Const.WCDMA_MIN_LAC, Const.WCDMA_MIN_PSC, Const.WCDMA_MIN_UARFCN,
                        Const.WCDMA_MIN_DBM, Const.WCDMA_MIN_BER, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                // exceed min
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, Const.MIN_MCC - 1, Const.MIN_MNC - 1,
                        Const.WCDMA_MIN_UCID - 1, Const.WCDMA_MIN_LAC - 1, Const.WCDMA_MIN_PSC - 1, Const.WCDMA_MIN_UARFCN - 1,
                        Const.WCDMA_MIN_DBM - 1, Const.WCDMA_MIN_BER - 1, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                cal.add(Calendar.HOUR, -1);

                // ### LTE ###
                // exceed max
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MAX_MCC + 1, Const.MAX_MNC + 1,
                        Const.LTE_MAX_ECI + 1, Const.LTE_MAX_PCI + 1, Const.LTE_MAX_TAC + 1,
                        Const.LTE_MAX_EARFCN + 1, Const.LTE_MAX_BANDWIDTH + 1, Const.LTE_MAX_TA + 1,
                        Const.LTE_MAX_RSRP + 1, Const.LTE_MAX_RSSI + 1, Const.LTE_MAX_RSRQ + 1, Const.LTE_MAX_RSSNR + 1,
                        Const.LTE_MAX_CQI + 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                // max
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MAX_MCC, Const.MAX_MNC,
                        Const.LTE_MAX_ECI, Const.LTE_MAX_PCI, Const.LTE_MAX_TAC, Const.LTE_MAX_EARFCN, Const.LTE_MAX_BANDWIDTH, Const.LTE_MAX_TA,
                        Const.LTE_MAX_RSRP, Const.LTE_MAX_RSSI, Const.LTE_MAX_RSRQ, Const.LTE_MAX_RSSNR,
                        Const.LTE_MAX_CQI, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                // min
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MIN_MCC, Const.MIN_MNC,
                        Const.LTE_MIN_ECI, Const.LTE_MIN_PCI, Const.LTE_MIN_TAC, Const.LTE_MIN_EARFCN, Const.LTE_MIN_BANDWIDTH, Const.LTE_MIN_TA,
                        Const.LTE_MIN_RSRP, Const.LTE_MIN_RSSI, Const.LTE_MIN_RSRQ, Const.LTE_MIN_RSSNR,
                        Const.LTE_MIN_CQI, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                // exceed min
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MIN_MCC - 1, Const.MIN_MNC - 1,
                        Const.LTE_MIN_ECI - 1, Const.LTE_MIN_PCI - 1, Const.LTE_MIN_TAC - 1, Const.LTE_MIN_EARFCN - 1,
                        Const.LTE_MIN_BANDWIDTH - 1, Const.LTE_MIN_TA - 1,
                        Const.LTE_MIN_RSRP - 1, Const.LTE_MIN_RSSI - 1, Const.LTE_MIN_RSRQ - 1, Const.LTE_MIN_RSSNR - 1,
                        Const.LTE_MIN_CQI - 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                cal.add(Calendar.HOUR, -1);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 5G
                    // exceed max
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            Const.MAX_MCC + 1, Const.MAX_MNC + 1, Const.NR_MAX_NCI + 1, Const.NR_MAX_NRARFCN + 1,
                            Const.NR_MAX_PCI + 1, Const.LTE_MAX_TAC + 1,
                            Const.NR_MAX_CSI_RSRP + 1, Const.NR_MAX_CSI_RSRQ + 1, Const.NR_MAX_CSI_SINR + 1,
                            Const.NR_MAX_SS_RSRP + 1, Const.NR_MAX_SS_RSRQ + 1, Const.NR_MAX_SS_SINR + 1,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));


                    // max
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            Const.MAX_MCC, Const.MAX_MNC, Const.NR_MAX_NCI, Const.NR_MAX_NRARFCN,
                            Const.NR_MAX_PCI, Const.LTE_MAX_TAC,
                            Const.NR_MAX_CSI_RSRP, Const.NR_MAX_CSI_RSRQ, Const.NR_MAX_CSI_SINR,
                            Const.NR_MAX_SS_RSRP, Const.NR_MAX_SS_RSRQ, Const.NR_MAX_SS_SINR,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));

                    // min
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            Const.MIN_MCC, Const.MIN_MNC, Const.NR_MIN_NCI, Const.NR_MIN_NRARFCN,
                            Const.NR_MIN_PCI, Const.LTE_MIN_TAC,
                            Const.NR_MIN_CSI_RSRP, Const.NR_MIN_CSI_RSRQ, Const.NR_MIN_CSI_SINR,
                            Const.NR_MIN_SS_RSRP, Const.NR_MIN_SS_RSRQ, Const.NR_MIN_SS_SINR,
                            false, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));

                    // exceed min
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            Const.MIN_MCC - 1, Const.MIN_MNC - 1, Const.NR_MIN_NCI - 1, Const.NR_MIN_NRARFCN - 1,
                            Const.NR_MIN_PCI - 1, Const.LTE_MIN_TAC - 1,
                            Const.NR_MIN_CSI_RSRP - 1, Const.NR_MIN_CSI_RSRQ - 1, Const.NR_MIN_CSI_SINR - 1,
                            Const.NR_MIN_SS_RSRP - 1, Const.NR_MIN_SS_RSRQ - 1, Const.NR_MIN_SS_SINR - 1,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));
                }

                cal.add(Calendar.HOUR, -1);

                // ### CDMA ###
                // exceed max (the dbm value is not set to +1 because it will result in -1 that
                // is filtered out for CDMA only signals
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MAX_NID + 1,
                        Const.CDMA_MAX_SID + 1, Const.CDMA_MAX_BSID + 1,
                        Const.CDMA_MAX_LATITUDE + 1, Const.CDMA_MAX_LONGITUDE + 1,
                        Const.CDMA_MAX_DBM, Const.CDMA_MAX_ECIO + 1,
                        Const.CDMA_MAX_DBM, Const.CDMA_MAX_ECIO + 1, Const.CDMA_MAX_SNR + 1,
                        Const.CDMA_MAX_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                // max
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MAX_NID, Const.CDMA_MAX_SID,
                        Const.CDMA_MAX_BSID, Const.CDMA_MAX_LATITUDE, Const.CDMA_MAX_LONGITUDE, Const.CDMA_MAX_DBM,
                        Const.CDMA_MAX_ECIO, Const.CDMA_MAX_DBM, Const.CDMA_MAX_ECIO, Const.CDMA_MAX_SNR,
                        Const.CDMA_MAX_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                // min
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MIN_NID, Const.CDMA_MIN_SID,
                        Const.CDMA_MIN_BSID, Const.CDMA_MIN_LATITUDE, Const.CDMA_MIN_LONGITUDE, Const.CDMA_MIN_DBM,
                        Const.CDMA_MIN_ECIO, Const.CDMA_MIN_DBM, Const.CDMA_MIN_ECIO, Const.CDMA_MIN_SNR,
                        Const.CDMA_MIN_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                // exceed min
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MIN_NID - 1, Const.CDMA_MIN_SID - 1,
                        Const.CDMA_MIN_BSID - 1, Const.CDMA_MIN_LATITUDE - 1, Const.CDMA_MIN_LONGITUDE - 1, Const.CDMA_MIN_DBM - 1,
                        Const.CDMA_MIN_ECIO - 1, Const.CDMA_MIN_DBM - 1, Const.CDMA_MIN_ECIO - 1, Const.CDMA_MIN_SNR - 1,
                        Const.CDMA_MIN_DBM - 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                cal.add(Calendar.HOUR, -1);

                // #### Wi-Fi #####
                es = new WifiProperty("valid", "12:10:10:10:12:09", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MAX_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("invalid", "12:10:10:10:12:10", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MAX_DBM + 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("valid", "12:10:10:10:12:11", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MIN_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("invalid", "12:10:10:10:12:12", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MIN_DBM - 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("invalid", "12:10:10:10:13:10", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MAX_DBM + 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("valid", "12:10:10:10:14:10", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MAX_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("valid", "12:10:10:10:15:11", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MIN_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("invalid", "12:10:10:10:16:12", "", "", "", -1, 2437, 0, 0, 0, "", 0, Const.WIFI_MIN_DBM - 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // ##### Bluetooth #############
                es = new BluetoothProperty("invalid", "",
                        "ff:ff:ff:ff:aa:01",
                        BluetoothClass.Device.TOY_ROBOT,
                        1,
                        BluetoothDevice.BOND_NONE, Const.BT_MAX_DBM + 1, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                es = new BluetoothProperty("valid", "",
                        "ff:ff:ff:ff:aa:02",
                        BluetoothClass.Device.TOY_ROBOT,
                        1,
                        BluetoothDevice.BOND_NONE, Const.BT_MAX_DBM, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                es = new BluetoothProperty("valid", "",
                        "ff:ff:ff:ff:aa:03",
                        BluetoothClass.Device.TOY_ROBOT,
                        1,
                        BluetoothDevice.BOND_NONE, Const.BT_MIN_DBM, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                es = new BluetoothProperty("invalid", "",
                        "ff:ff:ff:ff:aa:04",
                        BluetoothClass.Device.TOY_ROBOT,
                        1,
                        BluetoothDevice.BOND_NONE, Const.BT_MIN_DBM - 1, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                // ********** CDMA/EVDO specific tests **********************
                cal.add(Calendar.HOUR, -1);
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MAX_NID,
                        Const.CDMA_MAX_SID, Const.CDMA_MAX_BSID,
                        Const.CDMA_MAX_LATITUDE, Const.CDMA_MAX_LONGITUDE,
                        -50, -55,
                        -51, -56, Const.CDMA_MAX_SNR,
                        Const.CDMA_MAX_DBM, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_EVDO_0, Const.CDMA_MAX_NID,
                        Const.CDMA_MAX_SID, Const.CDMA_MAX_BSID,
                        Const.CDMA_MAX_LATITUDE, Const.CDMA_MAX_LONGITUDE,
                        -60, -65,
                        -62, -66, Const.CDMA_MAX_SNR,
                        Const.CDMA_MAX_DBM, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_EVDO_A, Const.CDMA_MAX_NID,
                        Const.CDMA_MAX_SID, Const.CDMA_MAX_BSID,
                        Const.CDMA_MAX_LATITUDE, Const.CDMA_MAX_LONGITUDE,
                        -70, -75,
                        -72, -76, Const.CDMA_MAX_SNR,
                        Const.CDMA_MAX_DBM, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_EVDO_B, Const.CDMA_MAX_NID,
                        Const.CDMA_MAX_SID, Const.CDMA_MAX_BSID,
                        Const.CDMA_MAX_LATITUDE, Const.CDMA_MAX_LONGITUDE,
                        -80, -85,
                        -82, -86, Const.CDMA_MAX_SNR,
                        Const.CDMA_MAX_DBM, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));


                // ********** Signals with invalid dbm values only **********************
                cal.add(Calendar.HOUR, -1);
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 208,
                        1, Const.GSM_MAX_CID + 1, Const.GSM_MAX_LAC + 1,
                        Const.GSM_MAX_ARFCN + 1, Const.GSM_MAX_BSIC + 1,
                        Const.GSM_MAX_TA + 1, Const.GSM_MAX_DBM + 1, Const.GSM_MAX_BER + 1, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 208, 1,
                        Const.WCDMA_MAX_UCID, Const.WCDMA_MAX_LAC + 1, Const.WCDMA_MAX_PSC + 1, Const.WCDMA_MAX_UARFCN + 1,
                        Const.WCDMA_MAX_DBM + 1, Const.WCDMA_MAX_BER + 1, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 208, 1,
                        Const.WCDMA_MAX_UCID + 1, Const.WCDMA_MAX_LAC + 1, Const.WCDMA_MAX_PSC + 1, Const.WCDMA_MAX_UARFCN + 1,
                        Const.WCDMA_MAX_DBM + 1, Const.WCDMA_MAX_BER + 1, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 1,
                        Const.LTE_MAX_ECI + 1, Const.LTE_MAX_PCI + 1, Const.LTE_MAX_TAC + 1, Const.LTE_MAX_EARFCN + 1, Const.LTE_MAX_BANDWIDTH, Const.LTE_MAX_TA + 1,
                        Const.LTE_MAX_RSRP + 1, Const.LTE_MAX_RSSI + 1, Const.LTE_MAX_RSRQ + 1, Const.LTE_MAX_RSSNR + 1,
                        Const.LTE_MAX_CQI + 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 1,
                        Const.LTE_MAX_ECI, Const.LTE_MAX_PCI + 1, Const.LTE_MAX_TAC + 1, Const.LTE_MAX_EARFCN + 1, Const.LTE_MAX_BANDWIDTH, Const.LTE_MAX_TA + 1,
                        Const.LTE_MAX_RSRP + 1, Const.LTE_MAX_RSSI + 1, Const.LTE_MAX_RSRQ + 1, Const.LTE_MAX_RSSNR + 1,
                        Const.LTE_MAX_CQI + 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 1,
                        Const.LTE_MAX_ECI - 1, Const.LTE_MAX_PCI + 1, Const.LTE_MAX_TAC + 1, Const.LTE_MAX_EARFCN + 1, Const.LTE_MAX_BANDWIDTH, Const.LTE_MAX_TA + 1,
                        Const.LTE_MAX_RSRP + 1, Const.LTE_MAX_RSSI + 1, Const.LTE_MAX_RSRQ + 1, Const.LTE_MAX_RSSNR + 1,
                        Const.LTE_MAX_CQI + 1, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 20, Const.NR_MAX_NCI + 1, Const.NR_MAX_NRARFCN + 1,
                            Const.NR_MAX_PCI + 1, Const.LTE_MAX_TAC + 1,
                            Const.NR_MAX_CSI_RSRP + 1, Const.NR_MAX_CSI_RSRQ + 1, Const.NR_MAX_CSI_SINR + 1,
                            Const.NR_MAX_SS_RSRP + 1, Const.NR_MAX_SS_RSRQ + 1, Const.NR_MAX_SS_SINR + 1,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));


                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 20, Const.NR_MIN_NCI - 1, Const.NR_MIN_NRARFCN - 1,
                            Const.NR_MIN_PCI - 1, Const.LTE_MIN_TAC - 1,
                            Const.NR_MIN_CSI_RSRP - 1, Const.NR_MIN_CSI_RSRQ - 1, Const.NR_MIN_CSI_SINR - 1,
                            Const.NR_MIN_SS_RSRP - 1, Const.NR_MIN_SS_RSRQ - 1, Const.NR_MIN_SS_SINR - 1,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));
                }

                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MAX_NID,
                        Const.CDMA_MAX_SID, Const.CDMA_MAX_BSID,
                        Const.CDMA_MAX_LATITUDE, Const.CDMA_MAX_LONGITUDE,
                        Const.CDMA_MAX_DBM + 1, -85,
                        Const.CDMA_MAX_DBM + 1, -86, Const.CDMA_MAX_SNR,
                        Const.CDMA_MAX_DBM + 1, true, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                es = new WifiProperty("test1", "11:15:10:10:13:11", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, Const.WIFI_MAX_DBM + 1,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test2", "11:15:10:10:13:10", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, Const.WIFI_MAX_DBM + 1,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test3", "11:15:10:10:13:09", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, Const.WIFI_MAX_DBM + 1,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new WifiProperty("test4", "11:10:10:10:13:09", "",
                        "", "", -1, 2437,
                        0, 0, 0, "", 0, Const.WIFI_MAX_DBM + 1,
                        false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                es = new BluetoothProperty("test bt 1", "",
                        "ff:ff:ff:ff:aa:01",
                        BluetoothClass.Device.TOY_ROBOT,
                        1,
                        BluetoothDevice.BOND_NONE, Const.BT_MAX_DBM + 1, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                // ********** test the max amplitude in the chart for all signals **********************
                // max BT
                cal.add(Calendar.HOUR, -1);
                es = new BluetoothProperty("test bt 1", "",
                        "ff:ff:ff:ff:aa:01",
                        BluetoothClass.Device.TOY_ROBOT,
                        1,
                        BluetoothDevice.BOND_NONE, Const.BT_MAX_DBM, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                //min BT
                cal.add(Calendar.HOUR, -1);
                es = new BluetoothProperty("test bt 2", "",
                        "ff:ff:ff:ff:aa:01",
                        BluetoothClass.Device.TOY_ROBOT,
                        1,
                        BluetoothDevice.BOND_NONE, Const.BT_MIN_DBM, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                // max Wi-Fi
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("test 1", "12:10:10:10:12:10",
                        "", "", "",
                        -1, 2437, 0, 0,
                        0, "", 0, Const.WIFI_MAX_DBM, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // min Wi-Fi
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("test 2", "12:10:10:10:12:10",
                        "", "", "",
                        -1, 2437, 0, 0,
                        0, "", 0, Const.WIFI_MIN_DBM, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // max GSM
                cal.add(Calendar.HOUR, -1);
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, Const.MAX_MCC,
                        Const.MAX_MNC, Const.GSM_MAX_CID, Const.GSM_MAX_LAC,
                        Const.GSM_MAX_ARFCN, Const.GSM_MAX_BSIC,
                        Const.GSM_MAX_TA, Const.GSM_MAX_DBM, Const.GSM_MAX_BER, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                // min GSM
                cal.add(Calendar.HOUR, -1);
                es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, Const.MIN_MCC,
                        Const.MIN_MNC, Const.GSM_MIN_CID, Const.GSM_MIN_LAC,
                        Const.GSM_MIN_ARFCN, Const.GSM_MIN_BSIC,
                        Const.GSM_MIN_TA, Const.GSM_MIN_DBM, Const.GSM_MIN_BER, false,
                        0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                // max WCDMA
                cal.add(Calendar.HOUR, -1);
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, Const.MAX_MCC, Const.MAX_MNC,
                        Const.WCDMA_MAX_UCID, Const.WCDMA_MAX_LAC, Const.WCDMA_MAX_PSC, Const.WCDMA_MAX_UARFCN,
                        Const.WCDMA_MAX_DBM, Const.WCDMA_MAX_BER, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                // min WCDMA
                cal.add(Calendar.HOUR, -1);
                es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, Const.MIN_MCC, Const.MIN_MNC,
                        Const.WCDMA_MIN_UCID, Const.WCDMA_MIN_LAC, Const.WCDMA_MIN_PSC, Const.WCDMA_MIN_UARFCN,
                        Const.WCDMA_MIN_DBM, Const.WCDMA_MIN_BER, BaseProperty.UNAVAILABLE, false, 0.0,
                        0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                // max LTE
                cal.add(Calendar.HOUR, -1);
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MAX_MCC, Const.MAX_MNC,
                        Const.LTE_MAX_ECI, Const.LTE_MAX_PCI, Const.LTE_MAX_TAC, Const.LTE_MAX_EARFCN, Const.LTE_MAX_BANDWIDTH, Const.LTE_MAX_TA,
                        Const.LTE_MAX_RSRP, Const.LTE_MAX_RSSI, Const.LTE_MAX_RSRQ, Const.LTE_MAX_RSSNR,
                        Const.LTE_MAX_CQI, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                // min LTE
                cal.add(Calendar.HOUR, -1);
                es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, Const.MIN_MCC, Const.MIN_MNC,
                        Const.LTE_MIN_ECI, Const.LTE_MIN_PCI, Const.LTE_MIN_TAC, Const.LTE_MIN_EARFCN, Const.LTE_MIN_BANDWIDTH, Const.LTE_MIN_TA,
                        Const.LTE_MIN_RSRP, Const.LTE_MIN_RSSI, Const.LTE_MIN_RSRQ, Const.LTE_MIN_RSSNR,
                        Const.LTE_MIN_CQI, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // max 5G
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 20, Const.NR_MAX_NCI, Const.NR_MAX_NRARFCN,
                            Const.NR_MAX_PCI, Const.LTE_MAX_TAC,
                            Const.NR_MAX_CSI_RSRP, Const.NR_MAX_CSI_RSRQ, Const.NR_MAX_CSI_SINR,
                            Const.NR_MAX_SS_RSRP, Const.NR_MAX_SS_RSRQ, Const.NR_MAX_SS_SINR,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));

                    // min 5G
                    newRadioProperty = new NewRadioProperty(
                            TelephonyManager.NETWORK_TYPE_NR,
                            208, 20, Const.NR_MIN_NCI - 1, Const.NR_MIN_NRARFCN - 1,
                            Const.NR_MIN_PCI - 1, Const.LTE_MIN_TAC - 1,
                            Const.NR_MIN_CSI_RSRP - 1, Const.NR_MIN_CSI_RSRQ - 1, Const.NR_MIN_CSI_SINR - 1,
                            Const.NR_MIN_SS_RSRP - 1, Const.NR_MIN_SS_RSRQ - 1, Const.NR_MIN_SS_SINR - 1,
                            true, 0.0, 0.0, true,
                            cal.getTime().getTime()
                    );
                    db.insert(DbContract.NEW_RADIO.TABLE_NAME, null,
                            DbHelper.createNewRadioContentValues(newRadioProperty));
                }

                // max CDMA
                cal.add(Calendar.HOUR, -1);
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MAX_NID, Const.CDMA_MAX_SID,
                        Const.CDMA_MAX_BSID, Const.CDMA_MAX_LATITUDE, Const.CDMA_MAX_LONGITUDE, Const.CDMA_MAX_DBM,
                        Const.CDMA_MAX_ECIO, Const.CDMA_MAX_DBM, Const.CDMA_MAX_ECIO, Const.CDMA_MAX_SNR,
                        Const.CDMA_MAX_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                // min CMDA
                cal.add(Calendar.HOUR, -1);
                es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, Const.CDMA_MIN_NID, Const.CDMA_MIN_SID,
                        Const.CDMA_MIN_BSID, Const.CDMA_MIN_LATITUDE, Const.CDMA_MIN_LONGITUDE, Const.CDMA_MIN_DBM,
                        Const.CDMA_MIN_ECIO, Const.CDMA_MIN_DBM, Const.CDMA_MIN_ECIO, Const.CDMA_MIN_SNR,
                        Const.CDMA_MIN_DBM, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));


                // ######### create old entries in the DB to test the deleteOldestDbEntries() method ############
                // i corresponds to one hour, 24 * 60 is 20 days in the past
                for (int i = 0; i < 24 * 20; i++) {
                    cal.add(Calendar.HOUR, -1);
                    es = new WifiProperty("AA " + i, "11:10:10:10:11:10", "", "", "", -1, 2437, 0, 0, 0, "", 0, -80, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                    if (i == 0) {
                        // to test the group view exposition dot corresponds to the highest
                        // signal in the list, we create medium and low exposure sources and set
                        // the low exposure source as connected
                        es = new WifiProperty("Medium exposure source", "11:10:10:10:11:20", "", "", "", -1, 2437, 0, 0, 40, "", 0, -70, false, 0.0, 0.0, true, cal.getTime().getTime());
                        db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                        es = new WifiProperty("Low connected source", "11:10:10:10:11:30", "", "", "", -1, 2437, 0, 0, 60, "", 0, -90, true, 0.0, 0.0, true, cal.getTime().getTime());
                        db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));
                    }


                    es = new BluetoothProperty("TEST" + i, "TEST BT1 ALIAS", "ff:ff:ff:ff:aa:01",
                            BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER, 1,
                            BluetoothDevice.BOND_BONDED, -51, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.BLUETOOTH.TABLE_NAME, null, DbHelper.createBluetoothContentValues(es));

                    es = new GsmProperty(TelephonyManager.NETWORK_TYPE_GPRS, 208, 2, 30 + i, 31, 0, 0, 0, Math.max(-30 - i, -120), -45, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.GSM.TABLE_NAME, null, DbHelper.createGsmContentValues(es));

                    es = new WcdmaProperty(TelephonyManager.NETWORK_TYPE_UMTS, 208, 2, 30 + i, 31, 32, 0, Math.max(-40 - i, -120), -45, BaseProperty.UNAVAILABLE, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.WCDMA.TABLE_NAME, null, DbHelper.createWcdmaContentValues(es));

                    if (i == 0) {
                        // for the signal "AA 0", create LTE signal with the connected attribute
                        // set to true so that we can test the exposition dot of the group view
                        // should correspond to the top signal in the list
                        es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 3,
                                30 + i, 31, 32, 0, 12, 33,
                                Math.max(-50 - i, -120), 31, -52, -53, -54,
                                true, 0.0, 0.0, true, cal.getTime().getTime());
                    } else {
                        es = new LteProperty(TelephonyManager.NETWORK_TYPE_LTE, 208, 3,
                                30 + i, 31, 32, 0, 12,
                                33, Math.max(-50 - i, -120), 31, -52,
                                -53, -54, false, 0.0, 0.0,
                                true, cal.getTime().getTime());
                    }

                    db.insert(DbContract.LTE.TABLE_NAME, null, DbHelper.createLteContentValues(es));

                    es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_CDMA, 10, 20, 30 + i, 31, 32, Math.min(-55 + i, -15), -56, -58, -60, -62, -62, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                    es = new CdmaProperty(TelephonyManager.NETWORK_TYPE_EVDO_A, 10, 20, 30 + i, 31, 32, -55, -56, Math.min(-45 + i, -15), -60, -62, -62, false, 0.0, 0.0, true, cal.getTime().getTime());
                    db.insert(DbContract.CDMA.TABLE_NAME, null, DbHelper.createCdmaContentValues(es));

                }

                // ######### populate the WHITE_ZONE table  ##############
                db.insert(DbContract.WHITE_ZONE.TABLE_NAME, null, DbHelper.createWhiteZoneContentValues(Const.WHITE_ZONE_CELLULAR, 0.0, 0.0, cal.getTime().getTime()));
                db.insert(DbContract.WHITE_ZONE.TABLE_NAME, null, DbHelper.createWhiteZoneContentValues(Const.WHITE_ZONE_WIFI, 0.0, 0.0, cal.getTime().getTime()));

                // ######### IMPLEMENT STRESS TESTS WITH LARGE NUMBER OF DUPLICATE SIGNALS ####
                // TODO implement the stress test


                // THIS ENTRY MUST BE THE LAST ONE TO BE CREATED IN ORDER TO BE THE OLDEST ENTRY
                // IN THE DB
                cal.add(Calendar.HOUR, -1);
                es = new WifiProperty("First entry in DB ", "00:00:00:FF:AA:00", "", "", "", -1, 2437, 0, 0, 0, "", 0, -80, false, 0.0, 0.0, true, cal.getTime().getTime());
                db.insert(DbContract.WIFI.TABLE_NAME, null, DbHelper.createWifiContentValues(es));

                // set DATE_FIRST_ENTRY_IN_DB and DATE_OLDEST_SIGNAL_IN_DB to the first created
                // event in the test DB
                String now = Long.toString(cal.getTimeInMillis());
                edit.putString(Const.DATE_FIRST_ENTRY_IN_DB, now).apply();
                edit.putString(Const.DATE_OLDEST_SIGNAL_IN_DB, now).apply();
                /*Log.d(TAG, "firstDateInDb: "  + MainApplication.getContext().
                        getSharedPreferences(Const.SHARED_PREF_FILE, 0).getString(Const.DATE_FIRST_ENTRY_IN_DB, ""));*/

                db.setTransactionSuccessful();
            } finally {
                // end the SQL transaction
                db.endTransaction();
                Log.d(TAG, "############################ END test database ########################");
            }
        };
        new Thread(runnable).start();
    }

    static void createMccMncToOperatorTable(final SQLiteDatabase db) {

        Runnable runnable = () -> {
            Log.i(TAG, "start importing mnc.txt");
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            // I am using a transaction to be sure the database is fully written and not partially
            // if ever the app crash in the middle of the process. If the app crash, the database
            // will be rolled back and installed again at the next start.
            db.beginTransaction();
            try {
                try {
                    AssetManager assetManager = MainApplication.getContext().getAssets();
                    InputStream ims = assetManager.open("mnc.txt");
                    BufferedReader in = new BufferedReader(new InputStreamReader(ims, "UTF-8"));
                    String line;
                    while ((line = in.readLine()) != null) {
                        String[] str = line.split(";");
                        db.insert(DbContract.OPERATORS.TABLE_NAME, null, DbHelper.createOperatorsContentValues(Integer.parseInt(str[0]), Integer.parseInt(str[1]), str[3]));
                    }
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                db.setTransactionSuccessful();
                Log.i(TAG, "finished importing mnc.txt");
            } finally {
                db.endTransaction();
            }
        };
        new Thread(runnable).start();
    }

    /**
     * Return an operator name corresponding to the passed mcc and mnc numbers.
     * <p>
     * This method use a cache to optimize the DB requests. If ever the operatorCache already
     * has an entry for the passed (mnc,mcc) then we return the cached operator name, otherwise
     * we request the DB, update the cache, and return the operator name.
     *
     * @param mcc operator MCC
     * @param mnc operator MNC
     * @return operator name
     */
    private static String getOperator(int mcc, int mnc) {
        /*
         We are using a space between the ints to be sure we generate a unique string even in the
         case 4 18 and 41 8, that would without a space give 418 in both cases, but with a space
         give two distinct strings "4 18" and "41 8"
         */
        String mcc_mnc_key = mcc + " " + mnc;
        String operator = "";
        if (operatorCache.containsKey(mcc_mnc_key)) {
            operator = operatorCache.get(mcc_mnc_key);
            Log.d(TAG, "getOperator: cache hit for " + mcc_mnc_key + " : " + operator);
            return operator;
        } else {
            Log.d(TAG, "getOperator: cache miss for " + mcc_mnc_key);
            SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
            String whereClause = DbContract.OPERATORS.COLUMN_NAME_MCC + "=? AND " + DbContract.OPERATORS.COLUMN_NAME_MNC + "=?";
            String[] whereValues = new String[]{Integer.toString(mcc), Integer.toString(mnc)};
            Cursor cursor = null;

            try {
                cursor = db.query(
                        DbContract.OPERATORS.TABLE_NAME,                // The table to query
                        new String[]{DbContract.OPERATORS.COLUMN_NAME_OPERATOR_NAME}, // The columns to return
                        whereClause,                                // The columns for the WHERE clause
                        whereValues,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        null                                 // don't sort
                );

                boolean hasValue = cursor.moveToFirst();
                if (hasValue) {
                    operator = cursor.getString(
                            cursor.getColumnIndexOrThrow(DbContract.OPERATORS.COLUMN_NAME_OPERATOR_NAME)
                    );

                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            operatorCache.put(mcc_mnc_key, operator);
            return operator;
        }
    }

    /**
     * method to map operator name with mnc and mcc, from local table i.e. Operators
     *
     * @param mcc : mobile country code
     * @param mnc : mobile network code
     * @return : operator name after mapped with mnc & mcc from local table Operators
     */
    public static String getOperatorName(int mcc, int mnc) {
        if (mnc == BaseProperty.UNAVAILABLE && mcc == BaseProperty.UNAVAILABLE) {
            return MainApplication.getContext().getResources().getString(R.string.no_operator);
        }
        String operator = getOperator(mcc, mnc);
        if (operator.isEmpty()) {
            return MainApplication.getContext().getResources().getString(R.string.unknown_operator);
        } else {
            return operator;
        }
    }

    /*
    EXPLANATION OF THE SQL SYNTAX
    an example of requests is the following one
    SELECT ssid, bssid, frequency, capabilities, MAX (dbm) AS dbm FROM
    (SELECT * FROM wifi WHERE created >= 1480079281477 AND  created < 1480084720167)
    GROUP BY ssid, bssid, frequency, capabilities


     "select ssid, bssid, frequency, capabilities, MAX (dbm) AS dbm FROM" means that we return the
      column ssid, bssid, frequency, capabilities and max(dbm) renamed as simply "dbm" from
      the table given just after. Note that the syntax "max(dbm) as dbm" simply means that the
      column max(dbm) will be renamed dbm. Indeed, without this renaming the column name would
      have been max(dbm). We discuss the exact meaning of MAX() when we will discuss GROUP BY.

      "(SELECT * FROM wifi WHERE created >= 1480079281477 AND  created < 1480084720167)" means
      that we select all columns from the table wifi where the created column has value between
      the two dates (represented as an EPOCH).

      "GROUP BY ssid, bssid, frequency, capabilities" means that we group together all columns with
      the same 4-tuple ssid, bssid, frequency, capabilities. So we will not have duplicate rows
      with the same 4-tuple. max(dbm) takes the maximum of the dbm column for all rows with the
      same 4-tuple group.

      ########## DESIGN CHOICE FOR RETURNING LATITUDE, LONGITUDE, CREATED ##################

      the signal objects (child of BaseProperty) have a latitude, longitude, and created field.
      These fields are used to show signals on a map or to filter signals per location.

      For historical views (hourly or daily), we can get the latitude, longitude, and created
      columns for one of the row with a dbm value that corresponds to the max one. Here is
      an example of such a request

     1) SELECT t2.ssid, t2.bssid, t2.frequency, t2.capabilities, t2.latitude, t2.longitude, t2.created, t2.dbm
        FROM
     2) (SELECT * FROM wifi WHERE created  >=  1481011199000 AND created  <  1481014799000) as t2
     3) INNER JOIN
     4)     (SELECT ssid, bssid, frequency, capabilities, MAX (dbm)  AS  dbm  FROM wifi
             WHERE created  >=  1481011199000 AND created  <  1481014799000
             GROUP BY ssid, bssid, frequency, capabilities) as t1
        ON  t1.ssid = t2.ssid AND t1.bssid = t2.bssid AND t1.frequency = t2.frequency AND
        t1.capabilities = t2.capabilities AND t1.dbm = t2.dbm
     5)  GROUP BY t2.ssid, t2.bssid, t2.frequency, t2.capabilities, t2.dbm

      To explain this request I will use the term 4-tuple to refer to ssid, bssid, capabilities,
      frequency and coord to refer to latitude, longitude, created.

      the request 4) is the regular one (get the unique antenna with the max dBm value for each
      of these antenna in a given time range) we use this request to make an inner join with the
      request 1) made on 2). 2) is just the full wifi table on the same time range as 4). We take
      from this table 2) the 4-tuple +coord + dbm for which the 4-tuple +dbm in 2) is the same as the
      4-tuple+dbm in 4). Then we group by the 4-tuple+dbm to have a single line per antenna.

      The result is one row per antenna with the max dbm, latitude, longitude, and creation date.
      Note that the simple request

      SELECT ssid, bssid, frequency, capabilities, MAX (dbm) AS dbm, latitude, longitude, created FROM
      (SELECT * FROM wifi WHERE created >= 1480079281477 AND  created < 1480084720167)
      GROUP BY ssid, bssid, frequency, capabilities

      will not work, because the select columns not specified in a group by clause can come from any
      line. See the following URLs for more details.
      https://www.psce.com/blog/2012/05/15/mysql-mistakes-do-you-use-group-by-correctly/
      http://rpbouman.blogspot.fr/2007/05/debunking-group-by-myths.html

      The new request is two times longer than the simple one (without latitude, longitude, created).
      In addition, it does not make much sense to show on a map a single signal location for a time
      frame of one hour or one day. For these reasons, I decided, to do not change the SQL request
      to include latitude, longitude, and created as it is not currently used in the UI and that even
      if we introduce a map, we would need to make a completely different request in order
      to define a path (possibly by simply adding latitude and longitude in the simple request
      group by clause)

      ########## IDEA FOR FUTURE REQUESTS ######################################################

      For future use, GROUP_CONCAT(column) returns a list of columns for each 'group by' group
      */
    private static List<BaseProperty> getWifiData(Date from, Date to) {
        Log.d(TAG, "start getWifiData for from: " + from + " and to: " + to);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        /*
        The heuristic we use for the connected state is the following. There are two connected
        states
        false coded as 0 in the DB (because there is no boolean in sqlite)
        true coded as 1 in the DB

        We consider that a single signal with a connected state set to true is enough for the
        group of signals it belongs to to be considered as connected. As this true is 1 and false
        is 0, we just compute the max value on the group for the COLUMN_NAME_CONNECTED.

        We use the same heuristic for GSM, WCDMA, LTE, and Wi-Fi
        We did not implement the connected state for CDMA.
        */
        String SELECT_CLAUSE = SELECT + DbContract.WIFI.COLUMN_NAME_SSID + COMMA +
                DbContract.WIFI.COLUMN_NAME_BSSID + COMMA +
                DbContract.WIFI.COLUMN_NAME_FREQ + COMMA +
                MAX + "(" + DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD + ")" + AS
                + " " + DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD + COMMA +
                MAX + "(" + DbContract.WIFI.COLUMN_NAME_DBM + ")" + AS
                + " " + DbContract.WIFI.COLUMN_NAME_DBM + COMMA +
                MAX + "(" + DbContract.WIFI.COLUMN_NAME_CONNECTED + ")" + AS
                + " " + DbContract.WIFI.COLUMN_NAME_CONNECTED +
                FROM + "(" + SELECT + ALL +
                FROM + DbContract.WIFI.TABLE_NAME +
                WHERE + DbContract.WIFI.COLUMN_NAME_CREATED +
                LARGER_EQ + from.getTime() + AND +
                DbContract.WIFI.COLUMN_NAME_CREATED + LOWER + to.getTime() + ")" +
                GROUP_BY + DbContract.WIFI.COLUMN_NAME_SSID + COMMA +
                DbContract.WIFI.COLUMN_NAME_BSSID + COMMA +
                DbContract.WIFI.COLUMN_NAME_FREQ;

        //Log.d(TAG, SELECT_CLAUSE);

        List<BaseProperty> signals = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            Log.d(TAG, "cursor done");

            /*
            We don't take into account the operator_friendly_name, venue_name, is_passpoint_network,
            center_freq0, center_freq1, channel_width in the database retrieval as
            we don't use these fields in the user interface for now and as we have no access point
            to test.
             */
            while (cursor.moveToNext()) {
                WifiProperty es = new WifiProperty(
                        cursor.getString(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_SSID)),
                        Tools.Long2MAC(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_BSSID))),
                        "",
                        "",
                        "",
                        Const.INVALID_IS_PASSPOINT_NETWORK,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_FREQ)),
                        Const.INVALID_CENTERFREQ0,
                        Const.INVALID_CENTERFREQ1,
                        Const.INVALID_CHANNELWIDTH,
                        "",
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_DBM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_CONNECTED)) != 0,
                        Const.INVALID_LATITUDE,
                        Const.INVALID_LONGITUDE,
                        true,
                        Const.INVALID_TIME
                );

                // if the dbm value is out of range, it cannot be displayed. So we change it to
                // WIFI_MIN_DBM so that all signals with dbm values out of range are at the bottom.
                es.normalizeSignalWithInvalidDbm();
                signals.add(es);

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        //Log.d(TAG, "stop getWifiData with " + signals.size() + " entries ");
        return signals;
    }


    private static List<BaseProperty> getBluetoothData(Date from, Date to) {
        Log.d(TAG, "start getBluetoothData for from: " + from + " and to: " + to);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        /*
        The heuristic we use for the bound state is the following. There are three bound states
        defined in https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
        BOND_BONDED with value 12
        BOND_BONDING with value 11
        BOND_NONE with value 10

        We consider that a single signal with a bound state to BOND_BONDED is enough for the
        group of signals it belongs to to be considered as BOND_BONDED. As this BOND_BONDED is the
        largest integer value, we just compute the max value on the group for the
        COLUMN_NAME_BOND_STATE.
         */
        String SELECT_CLAUSE = SELECT + DbContract.BLUETOOTH.COLUMN_NAME_NAME + COMMA +
                DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS + COMMA +
                DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS + COMMA +
                DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS + COMMA +
                DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE + ", " +
                MAX + "(" + DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE + ") " + AS
                + DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE + COMMA +
                MAX + "(" + DbContract.BLUETOOTH.COLUMN_NAME_DBM + ") " + AS
                + DbContract.BLUETOOTH.COLUMN_NAME_DBM +
                FROM + "(" + SELECT + ALL +
                FROM + DbContract.BLUETOOTH.TABLE_NAME +
                WHERE + DbContract.BLUETOOTH.COLUMN_NAME_CREATED + LARGER_EQ +
                from.getTime() + AND +
                DbContract.BLUETOOTH.COLUMN_NAME_CREATED + LOWER + to.getTime() + ")" +
                GROUP_BY + DbContract.BLUETOOTH.COLUMN_NAME_NAME + ", " +
                DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS + COMMA +
                DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS + COMMA +
                DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS + COMMA +
                DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE;


        //Log.d(TAG, SELECT_CLAUSE);

        List<BaseProperty> signals = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                BluetoothProperty es = new BluetoothProperty(
                        cursor.getString(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS)),
                        Tools.Long2MAC(cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS))),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_DBM)),
                        Const.INVALID_LATITUDE,
                        Const.INVALID_LONGITUDE,
                        true,
                        Const.INVALID_TIME
                );

                // if the dbm value is out of range, it cannot be displayed. So we change it to
                // BT_MIN_DBM so that all signals with dbm values out of range are at the bottom.
                es.normalizeSignalWithInvalidDbm();
                signals.add(es);

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        //Log.d(TAG, "stop getBluetoothData with " + signals.size() + " entries ");
        return signals;
    }


    private static List<BaseProperty> getGsmData(Date from, Date to) {
        Log.d(TAG, "start getGsmData for from: " + from + " and to: " + to);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        String SELECT_CLAUSE =
                SELECT + DbContract.GSM.COLUMN_NAME_TYPE + COMMA +
                        DbContract.GSM.COLUMN_NAME_MNC + COMMA +
                        DbContract.GSM.COLUMN_NAME_MCC + COMMA +
                        DbContract.GSM.COLUMN_NAME_CID + COMMA +
                        DbContract.GSM.COLUMN_NAME_LAC + COMMA +
                        DbContract.GSM.COLUMN_NAME_ARFCN + COMMA +
                        DbContract.GSM.COLUMN_NAME_BSIC + COMMA +
                        MAX + "(" + DbContract.GSM.COLUMN_NAME_DBM + ")" +
                        AS + DbContract.GSM.COLUMN_NAME_DBM + COMMA +
                        MAX + "(" + DbContract.GSM.COLUMN_NAME_CONNECTED + ")" +
                        AS + DbContract.GSM.COLUMN_NAME_CONNECTED +
                        FROM + "(" + SELECT + ALL +
                        FROM + DbContract.GSM.TABLE_NAME +
                        WHERE + DbContract.GSM.COLUMN_NAME_CREATED + LARGER_EQ + from.getTime() +
                        AND + DbContract.GSM.COLUMN_NAME_CREATED + LOWER + to.getTime() + ")" +
                        GROUP_BY +
                        DbContract.GSM.COLUMN_NAME_TYPE + COMMA +
                        DbContract.GSM.COLUMN_NAME_MNC + COMMA +
                        DbContract.GSM.COLUMN_NAME_MCC + COMMA +
                        DbContract.GSM.COLUMN_NAME_CID + COMMA +
                        DbContract.GSM.COLUMN_NAME_LAC + COMMA +
                        DbContract.GSM.COLUMN_NAME_ARFCN + COMMA +
                        DbContract.GSM.COLUMN_NAME_BSIC;

        //Log.d(TAG, SELECT_CLAUSE);

        List<BaseProperty> signals = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                GsmProperty es = new GsmProperty(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_TYPE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_MCC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_MNC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_CID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_LAC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_ARFCN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_BSIC)),
                        BaseProperty.UNAVAILABLE, // this value cannot be aggregated as it depends on the distance of the device to the antenna
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_DBM)),
                        BaseProperty.UNAVAILABLE, // this value doesn't make sense as we aggregate based on dBm values
                        //cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_BER))
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_CONNECTED)) != 0,
                        Const.INVALID_LATITUDE,
                        Const.INVALID_LONGITUDE,
                        true,
                        Const.INVALID_TIME
                );

                // if the dbm value is out of range, it cannot be displayed. So we change it to
                // GSM_MIN_DBM so that all signals with dbm values out of range are at the bottom.
                es.normalizeSignalWithInvalidDbm();
                signals.add(es);

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        //Log.d(TAG, "stop getGsmData with " + signals.size() + " entries ");
        return signals;
    }


    private static List<BaseProperty> getWcdmaData(Date from, Date to) {
        Log.d(TAG, "start getWcdmaData for from: " + from + " and to: " + to);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        String SELECT_CLAUSE = SELECT +
                DbContract.WCDMA.COLUMN_NAME_TYPE + COMMA +
                DbContract.WCDMA.COLUMN_NAME_MNC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_MCC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_UCID + COMMA +
                DbContract.WCDMA.COLUMN_NAME_LAC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_PSC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_UARFCN + COMMA +
                MAX + "(" + DbContract.WCDMA.COLUMN_NAME_DBM + ")" +
                AS + DbContract.WCDMA.COLUMN_NAME_DBM + COMMA +
                MAX + "(" + DbContract.WCDMA.COLUMN_NAME_CONNECTED + ")" +
                AS + DbContract.WCDMA.COLUMN_NAME_CONNECTED +
                FROM + "(" + SELECT + ALL +
                FROM + DbContract.WCDMA.TABLE_NAME +
                WHERE + DbContract.WCDMA.COLUMN_NAME_CREATED + LARGER_EQ + from.getTime() +
                AND + DbContract.WCDMA.COLUMN_NAME_CREATED + LOWER + to.getTime() + ")" +
                GROUP_BY +
                DbContract.WCDMA.COLUMN_NAME_TYPE + COMMA +
                DbContract.WCDMA.COLUMN_NAME_MNC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_MCC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_UCID + COMMA +
                DbContract.WCDMA.COLUMN_NAME_LAC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_PSC + COMMA +
                DbContract.WCDMA.COLUMN_NAME_UARFCN;

        //Log.d(TAG, SELECT_CLAUSE);

        List<BaseProperty> signals = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                WcdmaProperty es = new WcdmaProperty(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_TYPE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_MCC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_MNC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_UCID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_LAC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_PSC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_UARFCN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_DBM)),
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        //cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_BER))
                        BaseProperty.UNAVAILABLE, // this value doesn't make sense as we aggregate based on dBm values
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_CONNECTED)) != 0,
                        Const.INVALID_LATITUDE,
                        Const.INVALID_LONGITUDE,
                        true,
                        Const.INVALID_TIME
                );

                // if the dbm value is out of range, it cannot be displayed. So we change it to
                // WCDMA_MIN_DBM so that all signals with dbm values out of range are at the bottom.
                es.normalizeSignalWithInvalidDbm();
                signals.add(es);

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }


        //Log.d(TAG, "stop getWcdmaData with " + signals.size() + " entries ");
        return signals;
    }


    private static List<BaseProperty> getLteData(Date from, Date to) {
        Log.d(TAG, "start getLteData for from: " + from + " and to: " + to);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        /*
        In the case of LTE signals, the correct dbm value can be either the RSSI (preferred one)
        or the RSRP is the RSSI is not available. As a consequence, in the groupby, we take
        the max of the RSSI and the max of the RSRP for each group. Then the LteProperty will
        compute the dbm based on these max. Basically, is the max RSSI is valid, we take it,
        otherwise, we take the max RSRP.
         */
        String SELECT_CLAUSE = SELECT +
                DbContract.LTE.COLUMN_NAME_TYPE + COMMA +
                DbContract.LTE.COLUMN_NAME_MNC + COMMA +
                DbContract.LTE.COLUMN_NAME_MCC + COMMA +
                DbContract.LTE.COLUMN_NAME_ECI + COMMA +
                DbContract.LTE.COLUMN_NAME_PCI + COMMA +
                DbContract.LTE.COLUMN_NAME_TAC + COMMA +
                DbContract.LTE.COLUMN_NAME_EARFCN + COMMA +
                DbContract.LTE.COLUMN_NAME_BANDWIDTH + COMMA +
                MAX + "(" + DbContract.LTE.COLUMN_NAME_RSRP + ")" +
                AS + DbContract.LTE.COLUMN_NAME_RSRP + COMMA +
                MAX + "(" + DbContract.LTE.COLUMN_NAME_RSSI + ")" +
                AS + DbContract.LTE.COLUMN_NAME_RSSI + COMMA +
                MAX + "(" + DbContract.LTE.COLUMN_NAME_CONNECTED + ")" +
                AS + DbContract.LTE.COLUMN_NAME_CONNECTED +
                FROM + "(" + SELECT + ALL +
                FROM + DbContract.LTE.TABLE_NAME +
                WHERE + DbContract.LTE.COLUMN_NAME_CREATED + LARGER_EQ + from.getTime() +
                AND + DbContract.LTE.COLUMN_NAME_CREATED + LOWER + to.getTime() + ")" +
                GROUP_BY +
                DbContract.LTE.COLUMN_NAME_TYPE + COMMA +
                DbContract.LTE.COLUMN_NAME_MNC + COMMA +
                DbContract.LTE.COLUMN_NAME_MCC + COMMA +
                DbContract.LTE.COLUMN_NAME_ECI + COMMA +
                DbContract.LTE.COLUMN_NAME_PCI + COMMA +
                DbContract.LTE.COLUMN_NAME_TAC + COMMA +
                DbContract.LTE.COLUMN_NAME_EARFCN + COMMA +
                DbContract.LTE.COLUMN_NAME_BANDWIDTH;

        //Log.d(TAG, SELECT_CLAUSE);

        List<BaseProperty> signals = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                LteProperty es = new LteProperty(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_TYPE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_MCC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_MNC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_ECI)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_PCI)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_TAC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_EARFCN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_BANDWIDTH)),
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_RSRP)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_RSSI)),
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_CONNECTED)) != 0,
                        Const.INVALID_LATITUDE,
                        Const.INVALID_LONGITUDE,
                        true,
                        Const.INVALID_TIME
                );

                // if the dbm value is out of range, it cannot be displayed. So we change it to
                // MIN_DBM_LTE so that all signals with dbm values out of range are at the bottom.
                es.normalizeSignalWithInvalidDbm();
                signals.add(es);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        //Log.d(TAG, "stop getLteData with " + signals.size() + " entries ");
        return signals;
    }

    private static List<BaseProperty> getNewRadioData(Date from, Date to) {
        Log.d(TAG, "start getNewRadioData for from: " + from + " and to: " + to);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        String SELECT_CLAUSE = SELECT +
                DbContract.NEW_RADIO.COLUMN_NAME_TYPE + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_MNC + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_MCC + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_NCI + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_PCI + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_TAC + COMMA +
                MAX + "(" + DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP + ")" +
                AS + DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP + COMMA +
                MAX + "(" + DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED + ")" +
                AS + DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED +
                FROM + "(" + SELECT + ALL +
                FROM + DbContract.NEW_RADIO.TABLE_NAME +
                WHERE + DbContract.NEW_RADIO.COLUMN_NAME_CREATED + LARGER_EQ + from.getTime() +
                AND + DbContract.NEW_RADIO.COLUMN_NAME_CREATED + LOWER + to.getTime() + ")" +
                GROUP_BY +
                DbContract.NEW_RADIO.COLUMN_NAME_TYPE + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_MNC + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_MCC + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_NCI + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_PCI + COMMA +
                DbContract.NEW_RADIO.COLUMN_NAME_TAC;

        //Log.d(TAG, SELECT_CLAUSE);

        List<BaseProperty> signals = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                NewRadioProperty es = new NewRadioProperty(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_TYPE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_MCC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_MNC)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_NCI)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_PCI)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_TAC)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP)),
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        BaseProperty.UNAVAILABLE,  // this value doesn't make sense as we aggregate based on dBm values
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED)) != 0,
                        Const.INVALID_LATITUDE,
                        Const.INVALID_LONGITUDE,
                        true,
                        Const.INVALID_TIME
                );

                // if the dbm value is out of range, it cannot be displayed. So we change it to
                // MIN_DBM_LTE so that all signals with dbm values out of range are at the bottom.
                es.normalizeSignalWithInvalidDbm();
                signals.add(es);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        //Log.d(TAG, "stop getLteData with " + signals.size() + " entries ");
        return signals;
    }


    /*
    TODO (low): implement the more efficient request for CDMA as for other signals. See below.
    The additional difficulty for CDMA is that we have to consider two DBM values, the one for
    CDMA and the one for EVDO. I did not spent time on this issue as it is unlikely to find
    a user with a lot of CDMA signals. In the worst case, his access to historical data will be
    slower.

    We did not implemented the connected state for CDMA.
    */
    private static List<BaseProperty> getCdmaData(Date from, Date to) {
        Log.d(TAG, "start getCdmaData for from: " + from + " and to: " + to);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String whereClause = DbContract.CDMA.COLUMN_NAME_CREATED + LARGER_EQ_ARGS + AND +
                DbContract.CDMA.COLUMN_NAME_CREATED + LOWER_ARGS;
        String[] whereValues = new String[]{Long.toString(from.getTime()), Long.toString(to.getTime())};

        List<BaseProperty> signals = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DbContract.CDMA.TABLE_NAME,            // The table to query
                    new String[]{ALL},                     // The columns to return
                    whereClause,                           // The columns for the WHERE clause
                    whereValues,                           // The values for the WHERE clause
                    null,                                  // don't group the rows
                    null,                                  // don't filter by row groups
                    null                                   // don't sort
            );
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                int dbm = Tools.getDbmForCdma(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CDMA_DBM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_EVDO_DBM)));

                CdmaProperty es = new CdmaProperty(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_TYPE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_NETWORK_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_SYSTEM_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_BASE_STATION_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_STATION_LATITUDE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_STATION_LONGITUDE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CDMA_DBM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CDMA_ECIO)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_EVDO_DBM)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_EVDO_ECIO)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_EVDO_SNR)),
                        dbm,
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CONNECTED)) != 0,
                        Const.INVALID_LATITUDE,
                        Const.INVALID_LONGITUDE,
                        true,
                        Const.INVALID_TIME
                );

                // if the dbm value is out of range, it cannot be displayed. So we change it to
                // CDMA_MIN_DBM so that all signals with dbm values out of range are at the bottom.
                es.normalizeSignalWithInvalidDbm();
                signals.add(es);

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        //Log.d(TAG, "stop getCdmaData with " + signals.size() + " entries ");
        return signals;
    }


    /**
     * This method retrieve all the signals for the given time slots. We use the following
     * optimization in the request to speedup the retrieval time :
     * - the SQL request retrieves all signals filtering out all duplicate signals
     * (based on the antenna identification that is different for each signal type) and keeping
     * the max DBM value (on all the duplicate signals) for the unique signal. As an example, if we have
     * sig1 -30
     * sig1 -40
     * sig2 -20
     * sig3 -90
     * sig2 -10
     * sig1 -45
     * <p/>
     * we will get from the SQL request
     * sig1 -30
     * sig2 -10
     * sig3 -90
     *
     * @param timeSlotsToGetSignals the time slots on which we must retrieve the signals.
     * @return All signals for the given time slots (all duplicate signals removed)
     */
    public static ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<List<BaseProperty>>> createAllSignalsPerTypePerTimePeriods(List<Pair<Date, Date>> timeSlotsToGetSignals) {
        Log.d(TAG, "in createAllSignalsPerTypePerTimePeriods");
        ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<List<BaseProperty>>> data = new ConcurrentHashMap<>();
        data.put(MeasurementFragment.AntennaDisplay.WIFI, new ArrayList<>());
        data.put(MeasurementFragment.AntennaDisplay.BLUETOOTH, new ArrayList<>());
        data.put(MeasurementFragment.AntennaDisplay.CELLULAR, new ArrayList<>());

        if (timeSlotsToGetSignals != null) {
            for (Pair<Date, Date> timeSlot : timeSlotsToGetSignals) {
                Log.d(TAG, "time slot: " + timeSlot);

                List<BaseProperty> wifiData = getWifiData(timeSlot.first, timeSlot.second);
                if (wifiData != null && wifiData.size() > 0) {
                    data.get(MeasurementFragment.AntennaDisplay.WIFI).add(wifiData);
                } else {
                    data.get(MeasurementFragment.AntennaDisplay.WIFI).add(
                            new ArrayList<>(Collections.singletonList(
                                    new WifiProperty(false))));
                }

                List<BaseProperty> bluetoothData = getBluetoothData(timeSlot.first, timeSlot.second);
                if (bluetoothData != null && bluetoothData.size() > 0) {
                    data.get(MeasurementFragment.AntennaDisplay.BLUETOOTH).add(bluetoothData);
                } else {
                    data.get(MeasurementFragment.AntennaDisplay.BLUETOOTH).add(
                            new ArrayList<>(Collections.singletonList(
                                    new BluetoothProperty(false))));
                }

                List<BaseProperty> all_cellular_signals = new ArrayList<>();
                List<BaseProperty> gsmData = getGsmData(timeSlot.first, timeSlot.second);
                if (gsmData != null && gsmData.size() > 0) {
                    all_cellular_signals.addAll(gsmData);
                }

                List<BaseProperty> wcdmaData = getWcdmaData(timeSlot.first, timeSlot.second);
                if (wcdmaData != null && wcdmaData.size() > 0) {
                    all_cellular_signals.addAll(wcdmaData);
                }

                List<BaseProperty> lteData = getLteData(timeSlot.first, timeSlot.second);
                if (lteData != null && lteData.size() > 0) {
                    all_cellular_signals.addAll(lteData);
                }

                List<BaseProperty> newRadioData = getNewRadioData(timeSlot.first, timeSlot.second);
                if (newRadioData != null && newRadioData.size() > 0) {
                    all_cellular_signals.addAll(newRadioData);
                }

                List<BaseProperty> cdmaData = getCdmaData(timeSlot.first, timeSlot.second);
                if (cdmaData != null && cdmaData.size() > 0) {
                    all_cellular_signals.addAll(cdmaData);
                }

                // in case there is no cellular signal, we add an invalid one. We select arbitrarily
                // a GSM one.
                if (!all_cellular_signals.isEmpty()) {
                    data.get(MeasurementFragment.AntennaDisplay.CELLULAR).add(all_cellular_signals);
                } else {
                    data.get(MeasurementFragment.AntennaDisplay.CELLULAR).add(
                            new ArrayList<>(Collections.singletonList(
                                    new GsmProperty(false))));
                }
            }
        }
        return data;
    }


    /**
     * This method deletes all entries in the DB older than Tools.getNumberOfDaysInHistoryInMs() and
     * updates DATE_OLDEST_SIGNAL_IN_DB accordingly.
     * <p>
     * This method also deletes the DB entries older than Tools.getMaxNumOfDaysOfTop5Signals()
     * for the top 5 signals table.
     */
    public static void deleteOldestDbEntries() {
        Log.d(TAG, "in deleteOldestDbEntries()");
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        SharedPreferences settings = MainApplication.getContext()
                .getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();

        // 1) extract the value of DATE_OLDEST_SIGNAL_IN_DB
        long initial_DATE_OLDEST_SIGNAL_IN_DB = Tools.oldestSignalInDb();
        Log.d(TAG, "previous DATE_OLDEST_SIGNAL_IN_DB: " + initial_DATE_OLDEST_SIGNAL_IN_DB);

        // 2) compute the new DATE_OLDEST_SIGNAL_IN_DB and update the shared preference
        long now = System.currentTimeMillis();
        long oldestAllowedDate = Math.max(now - Tools.getNumberOfDaysInHistoryInMs(), initial_DATE_OLDEST_SIGNAL_IN_DB);
        Log.d(TAG, "now: " + now + " oldestAllowedDate: " + oldestAllowedDate);
        edit.putString(Const.DATE_OLDEST_SIGNAL_IN_DB, Long.toString(oldestAllowedDate)).apply();

        // We round the oldest allowed date to midnight because for the DAILY_STAT_SUMMARY table
        // we round the date to midnight before creating the entry in the table. Therefore,
        // for this specific table, the oldest date can be older (within the same day) than
        // the oldest signal.
        Calendar oldestAllowedDateCal = Calendar.getInstance();
        oldestAllowedDateCal.setTimeInMillis(oldestAllowedDate);
        Tools.roundCalendarAtMidnight(oldestAllowedDateCal);

        oldestAllowedDate = oldestAllowedDateCal.getTimeInMillis();

        /*
         3) clean the database by removing all entries older than oldestAllowedDate

         Note that we do not need to compare with oldestAllowedDate, comparing with
         (now - Tools.getNumberOfDaysInHistoryInMs()) would have been correct.

         COLUMN_NAME_CREATED < (now - Tools.getNumberOfDaysInHistoryInMs()) is equivalent to
         COLUMN_NAME_CREATED < Math.max(now - Tools.getNumberOfDaysInHistoryInMs(),
                                                                 initial_DATE_OLDEST_SIGNAL_IN_DB);
         because COLUMN_NAME_CREATED cannot be larger than initial_DATE_OLDEST_SIGNAL_IN_DB.

         Indeed, if
         Math.max(now - Tools.getNumberOfDaysInHistoryInMs(), initial_DATE_OLDEST_SIGNAL_IN_DB)
         is initial_DATE_OLDEST_SIGNAL_IN_DB, then as COLUMN_NAME_CREATED is not lower than
         initial_DATE_OLDEST_SIGNAL_IN_DB, the test is always false, and would have been false with
         (now - Tools.getNumberOfDaysInHistoryInMs()) which is lower than
         initial_DATE_OLDEST_SIGNAL_IN_DB.

         We reuse  oldestAllowedDate to avoid computing again for each expression
         (now - Tools.getNumberOfDaysInHistoryInMs()).
        */
        int nb_lines_deleted;
        Log.i(TAG, "start cleaning the DB (remove old entries)");
        nb_lines_deleted =
                db.delete(DbContract.GSM.TABLE_NAME, DbContract.GSM.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.WCDMA.TABLE_NAME, DbContract.WCDMA.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.LTE.TABLE_NAME, DbContract.LTE.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.CDMA.TABLE_NAME, DbContract.CDMA.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.WIFI.TABLE_NAME, DbContract.WIFI.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.BLUETOOTH.TABLE_NAME, DbContract.BLUETOOTH.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.ORIENTATION.TABLE_NAME, DbContract.ORIENTATION.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.WHITE_ZONE.TABLE_NAME, DbContract.WHITE_ZONE.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.EVENT.TABLE_NAME, DbContract.EVENT.COLUMN_NAME_CREATED + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.DAILY_STAT_SUMMARY.TABLE_NAME,
                        DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE + LOWER_ARGS,
                        new String[]{Long.toString(oldestAllowedDate)});

        Log.i(TAG, "Start cleaning the Top5Signals table (remove old entries)");
        nb_lines_deleted = nb_lines_deleted +
                db.delete(DbContract.TOP_5_SIGNALS.TABLE_NAME,
                        DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE + LOWER_ARGS,
                        new String[]{Long.toString(System.currentTimeMillis() -
                                (Tools.getMaxNumOfDaysOfTop5Signals()))});
        Log.i(TAG, "Finish cleaning the Top5Signals (remove old entries)");

        Log.i(TAG, "finish cleaning the DB (remove old entries). Total # lines deleted: " + nb_lines_deleted);
    }

    /**
     * Will retrieve all the tables' data between the dates from and to and return the json string ready to be uploaded to the server
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the string representing the entire JSON file
     */
    public static String getJsonForSync(Date from, Date to) {
        JSONObject measurementsObject = new JSONObject();
        JSONObject allDataObject = new JSONObject();
        try {
            allDataObject.put(DbContract.GSM.TABLE_NAME, dumpGSM2jsonArray(from, to));
            allDataObject.put(DbContract.WCDMA.TABLE_NAME, dumpWCDMA2jsonArray(from, to));
            allDataObject.put(DbContract.LTE.TABLE_NAME, dumpLTE2jsonArray(from, to));
            allDataObject.put(DbContract.NEW_RADIO.TABLE_NAME, dumpNewRadio2jsonArray(from, to));
            allDataObject.put(DbContract.CDMA.TABLE_NAME, dumpCDMA2jsonArray(from, to));
            allDataObject.put(DbContract.WIFI.TABLE_NAME, dumpWIFI2jsonArray(from, to));
            allDataObject.put(DbContract.BLUETOOTH.TABLE_NAME, dumpBLUETOOTH2jsonArray(from, to));
            allDataObject.put(DbContract.ORIENTATION.TABLE_NAME, dumpORIENTATION2jsonArray(from, to));
            allDataObject.put(DbContract.DEVICE_INFO.TABLE_NAME, dumpDEVICE_INFO2jsonArray(from, to));
            allDataObject.put(DbContract.OS_INFO.TABLE_NAME, dumpOS_INFO2jsonArray(from, to));
            allDataObject.put(DbContract.SIM_INFO.TABLE_NAME, dumpSIM_INFO2jsonArray(from, to));
            allDataObject.put(DbContract.APP_VERSION.TABLE_NAME, dumpAPP_VERSION2jsonArray(from, to));
            allDataObject.put(DbContract.WHITE_ZONE.TABLE_NAME, dumpWHITE_ZONE2jsonArray(from, to));
            allDataObject.put(DbContract.EVENT.TABLE_NAME, dumpEVENT2jsonArray(from, to));
            allDataObject.put(DbContract.DEVICE_LOCALE.TABLE_NAME, dumpDEVICE_LOCALE2jsonArray(from, to));
            allDataObject.put(DbContract.USER_PROFILE.TABLE_NAME, dumpUSER_PROFILE2jsonArray(from, to));
            measurementsObject.put("measurements", allDataObject);
        } catch (JSONException jsonException) {
            /*
             * The possible causes for {@link org.json.JSONException} as mentioned in the
             * documentation are:
             *
             *   - Attempts to parse or construct malformed documents
             *   - Use of null as a name
             *   - Use of numeric types not available to JSON, such as {@link
             *     Double#isNaN() NaNs} or {@link Double#isInfinite() infinities}.
             *   - Lookups using an out of range index or nonexistent name
             *   - Type mismatches on lookups
             *
             * In case of such exception, we simply create a json containing the details of the error
             * and upload it to the server.
             */
            try {
                JSONObject errorDetailObject = new JSONObject();
                errorDetailObject.put("errorMessage", jsonException.getMessage());
                errorDetailObject.put("stackTrace", Log.getStackTraceString(jsonException));
                JSONObject errorsObject = new JSONObject();
                errorsObject.put("errors", errorDetailObject);
                return errorsObject.toString();
            } catch (JSONException jsonException2) {
                // We should never reach here, but in case, we throw the first exception
                throw new RuntimeException(jsonException);
            }
        }
        return measurementsObject.toString();
    }


    /**
     * Returns the number of rows in the table of name table_name in the database db between dates
     * from and to. The date range [from,to] are compared with the column column_created that gives
     * the creation date of the row.
     *
     * @param from           start date
     * @param to             end date
     * @param db             database to use
     * @param table_name     table to use
     * @param column_created column that contains the creation date of the row
     * @return returns the number of lines
     */
    private static long getNbRowsInTable(Date from, Date to, SQLiteDatabase db, String table_name, String column_created) {
        long totalNumOfLines = 0;
        StringBuilder builder = new StringBuilder();
        String numOfLines = "tmp_nb_lines";  //temporary column name to be used
        String SELECT_CLAUSE = builder.append(SELECT).append(COUNT).append("(").append(ALL).append(")").append(AS).append(numOfLines).append(FROM).append(table_name)
                .append(WHERE).append(column_created).append(LARGER).append(from.getTime())
                .append(AND).append(column_created).append(LOWER_EQ).append(to.getTime()).toString();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            if (cursor.moveToNext()) {
                totalNumOfLines = cursor.getInt(cursor.getColumnIndexOrThrow(numOfLines));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        //Log.d(TAG, "getNbRowsInTable: table_name " + table_name + " nb lines "  + totalNumOfLines);
        return totalNumOfLines;
    }

    /**
     * Will retrieve an estimate of the number of rows to be sent to the server between the given
     * from and to dates.
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the total number of DB rows that will be used to build the JSON
     */
    public static long getNbRowsForAllTables(Date from, Date to) {
//        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy HH:mm", Locale.US);
//        Log.d(TAG, "Building JSON for sync with server from " + sdf.format(from) + " to " + sdf.format(to));
        long totalNumOfLines = 0;    //to be returned
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        //count the rows in the GSM table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.GSM.TABLE_NAME, DbContract.GSM.COLUMN_NAME_CREATED);

        //count the rows in the WCDMA table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.WCDMA.TABLE_NAME, DbContract.WCDMA.COLUMN_NAME_CREATED);

        //count the rows in the LTE table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.LTE.TABLE_NAME, DbContract.LTE.COLUMN_NAME_CREATED);

        //count the rows in the CDMA table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.CDMA.TABLE_NAME, DbContract.CDMA.COLUMN_NAME_CREATED);

        //count the rows in the WIFI table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.WIFI.TABLE_NAME, DbContract.WIFI.COLUMN_NAME_CREATED);

        //count the rows in the BLUETOOTH table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.BLUETOOTH.TABLE_NAME, DbContract.BLUETOOTH.COLUMN_NAME_CREATED);

        //count the rows in the ORIENTATION table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.ORIENTATION.TABLE_NAME, DbContract.ORIENTATION.COLUMN_NAME_CREATED);

        //count the rows in the DEVICE_INFO table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.DEVICE_INFO.TABLE_NAME, DbContract.DEVICE_INFO.COLUMN_NAME_CREATED);

        //count the rows in the OS_INFO table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.OS_INFO.TABLE_NAME, DbContract.OS_INFO.COLUMN_NAME_CREATED);

        //count the rows in the SIM_INFO table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.SIM_INFO.TABLE_NAME, DbContract.SIM_INFO.COLUMN_NAME_CREATED);

        //count the rows in the APP_VERSION table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.APP_VERSION.TABLE_NAME, DbContract.APP_VERSION.COLUMN_NAME_CREATED);

        //count the rows in the WHITE_ZONE table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.WHITE_ZONE.TABLE_NAME, DbContract.WHITE_ZONE.COLUMN_NAME_CREATED);

        //count the rows in the EVENT table
        totalNumOfLines += getNbRowsInTable(from, to, db, DbContract.EVENT.TABLE_NAME, DbContract.EVENT.COLUMN_NAME_CREATED);

        return totalNumOfLines;
    }

    /**
     * Builds the json array for GSM measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */

    private static JSONArray dumpGSM2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.GSM.TABLE_NAME +
                WHERE + DbContract.GSM.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.GSM.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray gsmItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject gsmItem = new JSONObject();
                gsmItem.put(DbContract.GSM.COLUMN_NAME_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_TYPE)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_MNC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_MNC)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_MCC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_MCC)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_CID, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_CID)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_LAC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_LAC)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_ARFCN, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_ARFCN)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_BSIC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_BSIC)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_TIMING_ADVANCE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_TIMING_ADVANCE)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_DBM, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_DBM)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_BER, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_BER)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_CONNECTED, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_CONNECTED)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_LATITUDE)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_LONGITUDE)));
                gsmItem.put(DbContract.GSM.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.GSM.COLUMN_NAME_CREATED)));
                gsmItemsArray.put(gsmItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return gsmItemsArray;
    }

    /**
     * Builds the json array for WCDMA measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */

    private static JSONArray dumpWCDMA2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.WCDMA.TABLE_NAME +
                WHERE + DbContract.WCDMA.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.WCDMA.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray wcdmaItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject wcdmaItem = new JSONObject();
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_TYPE)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_MNC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_MNC)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_MCC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_MCC)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_UCID, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_UCID)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_LAC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_LAC)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_PSC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_PSC)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_UARFCN, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_UARFCN)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_DBM, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_DBM)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_BER, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_BER)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_ECNO, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_ECNO)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_CONNECTED, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_CONNECTED)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_LATITUDE)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_LONGITUDE)));
                wcdmaItem.put(DbContract.WCDMA.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.WCDMA.COLUMN_NAME_CREATED)));
                wcdmaItemsArray.put(wcdmaItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return wcdmaItemsArray;
    }

    /**
     * Builds the json array for LTE measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */

    private static JSONArray dumpLTE2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.LTE.TABLE_NAME +
                WHERE + DbContract.LTE.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.LTE.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray lteItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject lteItem = new JSONObject();
                lteItem.put(DbContract.LTE.COLUMN_NAME_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_TYPE)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_MNC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_MNC)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_MCC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_MCC)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_ECI, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_ECI)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_PCI, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_PCI)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_TAC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_TAC)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_EARFCN, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_EARFCN)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_BANDWIDTH, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_BANDWIDTH)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_TIMING_ADVANCE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_TIMING_ADVANCE)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_RSRP, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_RSRP)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_RSSI, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_RSSI)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_RSRQ, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_RSRQ)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_RSSNR, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_RSSNR)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_CQI, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_CQI)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_CONNECTED, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_CONNECTED)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_LATITUDE)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_LONGITUDE)));
                lteItem.put(DbContract.LTE.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.LTE.COLUMN_NAME_CREATED)));
                lteItemsArray.put(lteItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return lteItemsArray;
    }

    /**
     * Builds the json array for 5G measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */

    private static JSONArray dumpNewRadio2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.NEW_RADIO.TABLE_NAME +
                WHERE + DbContract.NEW_RADIO.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.NEW_RADIO.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray newRadioItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject newRadioItem = new JSONObject();
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_TYPE)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_MCC, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_MCC)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_MNC, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_MNC)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_NCI, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_NCI)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_PCI, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_PCI)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_TAC, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_TAC)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRQ, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRQ)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_CSI_SINR, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_CSI_SINR)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRP, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRP)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRQ, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRQ)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_SS_SINR, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_SS_SINR)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_LATITUDE)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_LONGITUDE)));
                newRadioItem.put(DbContract.NEW_RADIO.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.NEW_RADIO.COLUMN_NAME_CREATED)));
                newRadioItemsArray.put(newRadioItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return newRadioItemsArray;
    }


    /**
     * Builds the json array for CDMA measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpCDMA2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.CDMA.TABLE_NAME +
                WHERE + DbContract.CDMA.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.CDMA.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray cdmaItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject cdmaItem = new JSONObject();
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_TYPE)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_BASE_STATION_ID, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_BASE_STATION_ID)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_NETWORK_ID, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_NETWORK_ID)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_SYSTEM_ID, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_SYSTEM_ID)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_STATION_LATITUDE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_STATION_LATITUDE)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_STATION_LONGITUDE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_STATION_LONGITUDE)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_CDMA_DBM, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CDMA_DBM)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_CDMA_ECIO, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CDMA_ECIO)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_EVDO_DBM, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_EVDO_DBM)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_EVDO_ECIO, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_EVDO_ECIO)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_EVDO_SNR, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_EVDO_SNR)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_CONNECTED, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CONNECTED)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_LATITUDE)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_LONGITUDE)));
                cdmaItem.put(DbContract.CDMA.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.CDMA.COLUMN_NAME_CREATED)));
                cdmaItemsArray.put(cdmaItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return cdmaItemsArray;
    }

    /**
     * Builds the json array for wifi measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpWIFI2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.WIFI.TABLE_NAME +
                WHERE + DbContract.WIFI.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.WIFI.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray wifiItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject wifiItem = new JSONObject();
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_SSID, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_SSID)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_BSSID, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_BSSID)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_OPERATOR_FRIENDLY_NAME, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_OPERATOR_FRIENDLY_NAME)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_VENUE_NAME, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_VENUE_NAME)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_IS_PASSPOINT_NETWORK, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_IS_PASSPOINT_NETWORK)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_FREQ, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_FREQ)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_CENTERFREQ0, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_CENTERFREQ0)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_CENTERFREQ1, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_CENTERFREQ1)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_CHANNELWIDTH, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_CHANNELWIDTH)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_CAPABILITIES, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_CAPABILITIES)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_DBM, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_DBM)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_CONNECTED, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_CONNECTED)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_LATITUDE)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_LONGITUDE)));
                wifiItem.put(DbContract.WIFI.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.WIFI.COLUMN_NAME_CREATED)));
                wifiItemsArray.put(wifiItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return wifiItemsArray;
    }

    /**
     * Builds the json array for Bluetooth measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpBLUETOOTH2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.BLUETOOTH.TABLE_NAME +
                WHERE + DbContract.BLUETOOTH.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.BLUETOOTH.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray bluetoothItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject bluetoothItem = new JSONObject();
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_NAME, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_NAME)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_DBM, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_DBM)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_LATITUDE)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_LONGITUDE)));
                bluetoothItem.put(DbContract.BLUETOOTH.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.BLUETOOTH.COLUMN_NAME_CREATED)));
                bluetoothItemsArray.put(bluetoothItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bluetoothItemsArray;
    }

    /**
     * Builds the json array for Orientation measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpORIENTATION2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.ORIENTATION.TABLE_NAME +
                WHERE + DbContract.ORIENTATION.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.ORIENTATION.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray orientationItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject orientationItem = new JSONObject();
                orientationItem.put(DbContract.ORIENTATION.COLUMN_NAME_X, cursor.getFloat(cursor.getColumnIndexOrThrow(DbContract.ORIENTATION.COLUMN_NAME_X)));
                orientationItem.put(DbContract.ORIENTATION.COLUMN_NAME_Y, cursor.getFloat(cursor.getColumnIndexOrThrow(DbContract.ORIENTATION.COLUMN_NAME_Y)));
                orientationItem.put(DbContract.ORIENTATION.COLUMN_NAME_Z, cursor.getFloat(cursor.getColumnIndexOrThrow(DbContract.ORIENTATION.COLUMN_NAME_Z)));
                orientationItem.put(DbContract.ORIENTATION.COLUMN_NAME_ANGLE, cursor.getFloat(cursor.getColumnIndexOrThrow(DbContract.ORIENTATION.COLUMN_NAME_ANGLE)));
                orientationItem.put(DbContract.ORIENTATION.COLUMN_NAME_ACCURACY, cursor.getFloat(cursor.getColumnIndexOrThrow(DbContract.ORIENTATION.COLUMN_NAME_ACCURACY)));
                orientationItem.put(DbContract.ORIENTATION.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.ORIENTATION.COLUMN_NAME_CREATED)));
                orientationItemsArray.put(orientationItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return orientationItemsArray;
    }

    /**
     * Builds the json array for Device Info measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpDEVICE_INFO2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.DEVICE_INFO.TABLE_NAME +
                WHERE + DbContract.DEVICE_INFO.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.DEVICE_INFO.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray deviceInfoItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject deviceInfoItem = new JSONObject();
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_BRAND, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_BRAND)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_DEVICE, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_DEVICE)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_HARDWARE, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_HARDWARE)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_MANUFACTURER, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_MANUFACTURER)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_MODEL, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_MODEL)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_PRODUCT, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_PRODUCT)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MANUFACTURER, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MANUFACTURER)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MODEL, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MODEL)));
                deviceInfoItem.put(DbContract.DEVICE_INFO.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_CREATED)));
                deviceInfoItemsArray.put(deviceInfoItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return deviceInfoItemsArray;
    }

    /**
     * Builds the json array for OS Info measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpOS_INFO2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.OS_INFO.TABLE_NAME +
                WHERE + DbContract.OS_INFO.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.OS_INFO.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray osInfoItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject osInfoItem = new JSONObject();
                osInfoItem.put(DbContract.OS_INFO.COLUMN_NAME_RELEASE, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OS_INFO.COLUMN_NAME_RELEASE)));
                osInfoItem.put(DbContract.OS_INFO.COLUMN_NAME_SDK_INT, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.OS_INFO.COLUMN_NAME_SDK_INT)));
                osInfoItem.put(DbContract.OS_INFO.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.OS_INFO.COLUMN_NAME_CREATED)));
                osInfoItemsArray.put(osInfoItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return osInfoItemsArray;
    }

    /**
     * Builds the json array for SIM Info measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */

    private static JSONArray dumpSIM_INFO2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.SIM_INFO.TABLE_NAME +
                WHERE + DbContract.SIM_INFO.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.SIM_INFO.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray simInfoItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject simInfoItem = new JSONObject();
                simInfoItem.put(DbContract.SIM_INFO.COLUMN_NAME_SIM_COUNTRY_CODE, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.SIM_INFO.COLUMN_NAME_SIM_COUNTRY_CODE)));
                simInfoItem.put(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR)));
                simInfoItem.put(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR_NAME, cursor.getString(cursor.getColumnIndexOrThrow(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR_NAME)));
                simInfoItem.put(DbContract.SIM_INFO.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.SIM_INFO.COLUMN_NAME_CREATED)));
                simInfoItemsArray.put(simInfoItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return simInfoItemsArray;
    }

    /**
     * Builds the json array for App versions measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpAPP_VERSION2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.APP_VERSION.TABLE_NAME +
                WHERE + DbContract.APP_VERSION.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.APP_VERSION.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray appVersionItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject appVersionItem = new JSONObject();
                appVersionItem.put(DbContract.APP_VERSION.COLUMN_NAME_VERSION, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.APP_VERSION.COLUMN_NAME_VERSION)));
                appVersionItem.put(DbContract.APP_VERSION.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.APP_VERSION.COLUMN_NAME_CREATED)));
                appVersionItemsArray.put(appVersionItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return appVersionItemsArray;
    }

    /**
     * Builds the json array for White zone measurements in the database within the date range [from, to].
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     */
    private static JSONArray dumpWHITE_ZONE2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.WHITE_ZONE.TABLE_NAME +
                WHERE + DbContract.WHITE_ZONE.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.WHITE_ZONE.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray whiteZoneItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject whiteZoneItem = new JSONObject();
                whiteZoneItem.put(DbContract.WHITE_ZONE.COLUMN_NAME_SIGNAL_TYPE, cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.WHITE_ZONE.COLUMN_NAME_SIGNAL_TYPE)));
                whiteZoneItem.put(DbContract.WHITE_ZONE.COLUMN_NAME_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.WHITE_ZONE.COLUMN_NAME_LATITUDE)));
                whiteZoneItem.put(DbContract.WHITE_ZONE.COLUMN_NAME_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(DbContract.WHITE_ZONE.COLUMN_NAME_LONGITUDE)));
                whiteZoneItem.put(DbContract.WHITE_ZONE.COLUMN_NAME_CREATED, cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.WHITE_ZONE.COLUMN_NAME_CREATED)));
                whiteZoneItemsArray.put(whiteZoneItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return whiteZoneItemsArray;
    }

    /**
     * Builds the json array for Event table within the date range [from, to]
     * Note: We added Event table in the release 1.8
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     * @throws JSONException Refer {@link DbRequestHandler#getJsonForSync} for cases when this can
     *                       arise
     */
    private static JSONArray dumpEVENT2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.EVENT.TABLE_NAME +
                WHERE + DbContract.EVENT.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.EVENT.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray eventItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject eventItem = new JSONObject();
                eventItem.put(
                        DbContract.EVENT.COLUMN_NAME_EVENT_TYPE,
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.EVENT.COLUMN_NAME_EVENT_TYPE
                                )
                        )
                );
                eventItem.put(
                        DbContract.EVENT.COLUMN_NAME_CREATED,
                        cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.EVENT.COLUMN_NAME_CREATED
                                )
                        )
                );
                eventItemsArray.put(eventItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return eventItemsArray;
    }

    /**
     * Builds the json array for Device locale table within the date range [from, to]
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     * @throws JSONException Refer {@link DbRequestHandler#getJsonForSync} for cases when this can
     *                       arise
     */
    private static JSONArray dumpDEVICE_LOCALE2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.DEVICE_LOCALE.TABLE_NAME +
                WHERE + DbContract.DEVICE_LOCALE.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.DEVICE_LOCALE.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray deviceLocaleItemsArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject deviceLocaleItem = new JSONObject();
                deviceLocaleItem.put(
                        DbContract.DEVICE_LOCALE.COLUMN_NAME_LOCALE,
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.DEVICE_LOCALE.COLUMN_NAME_LOCALE
                                )
                        )
                );
                deviceLocaleItem.put(
                        DbContract.DEVICE_LOCALE.COLUMN_NAME_CREATED,
                        cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.DEVICE_LOCALE.COLUMN_NAME_CREATED
                                )
                        )
                );
                deviceLocaleItemsArray.put(deviceLocaleItem);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return deviceLocaleItemsArray;
    }


    /**
     * Builds the json array for the USER_PROFILE table within the date range [from, to]
     * Note: We added USER_PROFILE table in the release 1.9
     *
     * @param from date since when the measurements should be taken
     * @param to   date until when the measurements should be taken
     * @return the JSONArray of the result
     * @throws JSONException Refer {@link DbRequestHandler#getJsonForSync} for cases when this can
     *                       arise
     */
    private static JSONArray dumpUSER_PROFILE2jsonArray(Date from, Date to) throws JSONException {
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        String SELECT_CLAUSE = SELECT + ALL + FROM + DbContract.USER_PROFILE.TABLE_NAME +
                WHERE + DbContract.USER_PROFILE.COLUMN_NAME_CREATED + LARGER + from.getTime() +
                AND + DbContract.USER_PROFILE.COLUMN_NAME_CREATED + LOWER_EQ + to.getTime();
        Cursor cursor = null;
        JSONArray userProfilesArray = new JSONArray();
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            while (cursor.moveToNext()) {
                JSONObject userProfile = new JSONObject();
                userProfile.put(
                        DbContract.USER_PROFILE.COLUMN_NAME_NAME,
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.USER_PROFILE.COLUMN_NAME_NAME
                                )
                        )
                );
                userProfile.put(
                        DbContract.USER_PROFILE.COLUMN_NAME_EMAIL,
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.USER_PROFILE.COLUMN_NAME_EMAIL
                                )
                        )
                );
                userProfile.put(
                        DbContract.USER_PROFILE.COLUMN_NAME_AGE,
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.USER_PROFILE.COLUMN_NAME_AGE
                                )
                        )
                );
                userProfile.put(
                        DbContract.USER_PROFILE.COLUMN_NAME_SEX,
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.USER_PROFILE.COLUMN_NAME_SEX
                                )
                        )
                );
                userProfile.put(
                        DbContract.USER_PROFILE.COLUMN_NAME_USER_SEGMENT,
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.USER_PROFILE.COLUMN_NAME_USER_SEGMENT
                                )
                        )
                );
                userProfile.put(
                        DbContract.USER_PROFILE.COLUMN_NAME_CREATED,
                        cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                        DbContract.USER_PROFILE.COLUMN_NAME_CREATED
                                )
                        )
                );
                userProfilesArray.put(userProfile);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return userProfilesArray;
    }

    /**
     * Updated the DEVICE_INFO table if one of the passed value (but currentTime) is different from
     * the values in the last line of the table.
     *
     * @param brand        actual brand
     * @param device       actual device
     * @param hardware     actual hardware
     * @param manufacturer actual manufacturer
     * @param model        actual model
     * @param product      actual product
     * @param currentTime  current time used to update the created time in the table if the table
     *                     needs to be updated
     */
    public static void updateDeviceInfoTable(String brand,
                                             String device,
                                             String hardware,
                                             String manufacturer,
                                             String model,
                                             String product,
                                             String soc_manufacturer,
                                             String soc_model,
                                             long currentTime) {

        // 1) we get the latest values in the DB
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        Cursor cursor = null;
        String db_brand = "";
        String db_device = "";
        String db_hardware = "";
        String db_manufacturer = "";
        String db_model = "";
        String db_product = "";
        String db_soc_manufacturer = "";
        String db_soc_model = "";
        try {
            // get the last entry in the DB
            cursor = db.query(
                    DbContract.DEVICE_INFO.TABLE_NAME,  // The table to query
                    new String[]{ALL},                  // The columns to return
                    null,                               // The columns for the WHERE clause
                    null,                               // The values for the WHERE clause
                    null,                               // don't group the rows
                    null,                               // don't filter by row groups
                    _ID + DESC,                         // ORDER BY
                    LIMIT1                              // LIMIT
            );

            // move to the first table entry and returns false if the table is empty
            if (cursor.moveToFirst()) {
                db_brand = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_BRAND));
                db_device = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_DEVICE));
                db_hardware = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_HARDWARE));
                db_manufacturer = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_MANUFACTURER));
                db_model = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_MODEL));
                db_product = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_PRODUCT));
                db_soc_manufacturer = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MANUFACTURER));
                db_soc_model = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MODEL));

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // 2) we compare the latest values in the DB with the one passed to the method.
        // If a single value is different from what is in the DB, we write a new row with
        // updated values
        if (!db_brand.equals(brand) ||
                !db_device.equals(device) ||
                !db_hardware.equals(hardware) ||
                !db_manufacturer.equals(manufacturer) ||
                !db_model.equals(model) ||
                !db_product.equals(product) ||
                !db_soc_manufacturer.equals(soc_manufacturer) ||
                !db_soc_model.equals(soc_model)) {

            Log.d(TAG, "updateDeviceInfoTable: update the values");
            ContentValues contentValues = DbHelper.createDeviceInfoContentValues(
                    brand, device, hardware, manufacturer, model, product,
                    soc_manufacturer, soc_model, currentTime);

            db.insert(DbContract.DEVICE_INFO.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "updateDeviceInfoTable: nothing to update");
        }
    }

    /**
     * Updated the OS_INFO table if one of the passed value (but currentTime) is different from
     * the values in the last line of the table.
     *
     * @param release     actual release
     * @param sdk_int     actual sdk_int
     * @param currentTime current time used to update the created time in the table if the table
     *                    needs to be updated
     */
    public static void updateOSInfoTable(String release, int sdk_int, long currentTime) {

        // 1) we get the latest values in the DB
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        Cursor cursor = null;
        String db_release = "";
        int db_sdk_int = -1;

        try {
            // get the last entry in the DB
            cursor = db.query(
                    DbContract.OS_INFO.TABLE_NAME,  // The table to query
                    new String[]{ALL},                  // The columns to return
                    null,                               // The columns for the WHERE clause
                    null,                               // The values for the WHERE clause
                    null,                               // don't group the rows
                    null,                               // don't filter by row groups
                    _ID + DESC,                         // ORDER BY
                    LIMIT1                              // LIMIT
            );

            // move to the first table entry and returns false if the table is empty
            if (cursor.moveToFirst()) {
                db_release = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.OS_INFO.COLUMN_NAME_RELEASE));
                db_sdk_int = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.OS_INFO.COLUMN_NAME_SDK_INT));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // 2) we compare the latest values in the DB with the one passed to the method.
        // If a single value is different from what is in the DB, we write a new row with
        // updated values
        if (!db_release.equals(release) || db_sdk_int != sdk_int) {
            Log.d(TAG, "updateOSInfoTable: update the values");
            ContentValues contentValues = DbHelper.createOSInfoContentValues(
                    release, sdk_int, currentTime);

            db.insert(DbContract.OS_INFO.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "updateOSInfoTable: nothing to update");
        }
    }

    /**
     * Updated the SIM_INFO table if one of the passed value (but currentTime) is different from
     * the values in the last line of the table.
     *
     * @param sim_country_code  actual sim_country_code
     * @param sim_operator      actual sim_operator
     * @param sim_operator_name actual sim_operator_name
     * @param currentTime       current time used to update the created time in the table if the table
     *                          needs to be updated
     */
    public static void updateSIMInfoTable(String sim_country_code,
                                          String sim_operator,
                                          String sim_operator_name,
                                          long currentTime) {

        // 1) we get the latest values in the DB
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        Cursor cursor = null;
        String db_sim_country_code = "";
        String db_sim_operator = "";
        String db_sim_operator_name = "";
        try {
            // get the last entry in the DB
            cursor = db.query(
                    DbContract.SIM_INFO.TABLE_NAME,  // The table to query
                    new String[]{ALL},                  // The columns to return
                    null,                               // The columns for the WHERE clause
                    null,                               // The values for the WHERE clause
                    null,                               // don't group the rows
                    null,                               // don't filter by row groups
                    _ID + DESC,                         // ORDER BY
                    LIMIT1                              // LIMIT
            );

            // move to the first table entry and returns false if the table is empty
            if (cursor.moveToFirst()) {
                db_sim_country_code = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.SIM_INFO.COLUMN_NAME_SIM_COUNTRY_CODE));
                db_sim_operator = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR));
                db_sim_operator_name = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR_NAME));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // 2) we compare the latest values in the DB with the one passed to the method.
        // If a single value is different from what is in the DB, we write a new row with
        // updated values
        if (!db_sim_country_code.equals(sim_country_code) ||
                !db_sim_operator.equals(sim_operator) ||
                !db_sim_operator_name.equals(sim_operator_name)) {

            Log.d(TAG, "updateSIMInfoTable: update the values");
            ContentValues contentValues = DbHelper.createSIMInfoContentValues(
                    sim_country_code, sim_operator, sim_operator_name, currentTime);

            db.insert(DbContract.SIM_INFO.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "updateSIMInfoTable: nothing to update");
        }
    }

    /**
     * Updated the APP_INFO table if one of the passed value (but currentTime) is different from
     * the values in the last line of the table.
     *
     * @param version     actual app version
     * @param currentTime current time used to update the created time in the table if the table
     *                    needs to be updated
     */
    public static void updateAppVersionTable(int version, long currentTime) {

        // 1) we get the latest values in the DB
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        Cursor cursor = null;
        int db_version = -1;

        try {
            // get the last entry in the DB
            cursor = db.query(
                    DbContract.APP_VERSION.TABLE_NAME,  // The table to query
                    new String[]{ALL},                  // The columns to return
                    null,                               // The columns for the WHERE clause
                    null,                               // The values for the WHERE clause
                    null,                               // don't group the rows
                    null,                               // don't filter by row groups
                    _ID + DESC,                         // ORDER BY
                    LIMIT1                              // LIMIT
            );

            // move to the first table entry and returns false if the table is empty
            if (cursor.moveToFirst()) {
                db_version = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.APP_VERSION.COLUMN_NAME_VERSION));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // 2) we compare the latest values in the DB with the one passed to the method.
        // If a single value is different from what is in the DB, we write a new row with
        // updated values
        if (db_version != version) {
            Log.d(TAG, "updateAppVersionTable: update the values");
            ContentValues contentValues = DbHelper.createAppVersionContentValues(
                    version, currentTime);

            db.insert(DbContract.APP_VERSION.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "updateAppVersionTable: nothing to update");
        }
    }

    /**
     * Update the DEVICE_LOCALE table if one of the passed value (but currentTime) is different from
     * the values in the last line of the table.
     *
     * @param locale      actual locale
     * @param currentTime current time used to update the created time in the table if the table
     *                    needs to be updated
     */
    public static void updateDeviceLocaleTable(String locale, long currentTime) {

        // 1) we get the latest values in the DB
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        Cursor cursor = null;
        String device_locale = "";

        try {
            // get the last entry in the DB
            cursor = db.query(
                    DbContract.DEVICE_LOCALE.TABLE_NAME,  // The table to query
                    new String[]{ALL},                  // The columns to return
                    null,                               // The columns for the WHERE clause
                    null,                               // The values for the WHERE clause
                    null,                               // don't group the rows
                    null,                               // don't filter by row groups
                    _ID + DESC,                         // ORDER BY
                    LIMIT1                              // LIMIT
            );

            // move to the first table entry and returns false if the table is empty
            if (cursor.moveToFirst()) {
                device_locale = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.DEVICE_LOCALE.COLUMN_NAME_LOCALE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // 2) we compare the latest values in the DB with the one passed to the method.
        // If a single value is different from what is in the DB, we write a new row with
        // updated values
        if (!device_locale.equals(locale)) {
            Log.d(TAG, "updateDeviceLocaleTable: update the values");
            ContentValues contentValues = DbHelper.createDeviceLocaleContentValues(
                    locale, currentTime
            );

            db.insert(DbContract.DEVICE_LOCALE.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "updateDeviceLocaleTable: nothing to update");
        }
    }

    /**
     * This method return the profile ID in the PROFILE_ID table. If the profile does not exist,
     * it returns an empty string.
     *
     * @return the profile ID as a string or an empty string if the profile ID is not yet set.
     */
    public static String getProfileIdFromDB() {

        // 1) We test if there is an entry in the table
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        Cursor cursor = null;
        String id = "";
        try {
            // retrieve all entries in the table to test if it is empty.
            cursor = db.query(
                    DbContract.PROFILE_ID.TABLE_NAME,   // The table to query
                    new String[]{ALL},                  // The columns to return
                    null,                               // The columns for the WHERE clause
                    null,                               // The values for the WHERE clause
                    null,                               // don't group the rows
                    null,                               // don't filter by row groups
                    null,                               // ORDER BY
                    null                                // LIMIT
            );

            // move to the first table entry and returns false if the table is empty
            // We use it to test if there is already a value in the table
            if (cursor.moveToFirst()) {
                id = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.PROFILE_ID.COLUMN_NAME_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    /**
     * This method sets the profile ID in the PROFILE_ID table. If the profile ID is already in the
     * table, the method simply returns without any update. So there is a single profile ID in this
     * table, and once set, it cannot be changed.
     *
     * @param id the profile ID to set in the table.
     */
    public static void setProfileId(String id) {
        // getProfileIdFromDB() returns an empty string if there is no profile set
        if (getProfileIdFromDB().isEmpty()) {
            Log.d(TAG, "setProfileIdTable: update the values to ID: " + id);
            ContentValues contentValues = DbHelper.createProfileIdContentValues(id);
            SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
            db.insert(DbContract.PROFILE_ID.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "setProfileIdTable: nothing to update");
        }
    }

    /**
     * This method must be called each time a white_zone is detected for a given signal type. The
     * method will automatically create a DB entry in the WHITE_ZONE table for the passed
     * signal_type with the current location and time
     *
     * @param signal_type the signal type for which there is a white zone. All signal types are
     *                    defined in Const.java
     */
    public static void addWhiteZoneToDB(int signal_type) {
        Log.d(TAG, "addWhiteZoneToDB: signal_type: " + signal_type);
        LocationMonitor.updateCoordinatesWithLastKnownLocation();
        double latitude = LocationMonitor.getLatitude();
        double longitude = LocationMonitor.getLongitude();
        /*
        it is useless to write to the DB a white zone for which we do not have a valid location.
         */
        if (LocationMonitor.isValidLocation(latitude, longitude)) {
            Log.d(TAG, "addWhiteZoneToDB: location valid");
            long now = System.currentTimeMillis();
            ContentValues contentValues = DbHelper.createWhiteZoneContentValues(
                    signal_type,
                    latitude,
                    longitude,
                    now);
            SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
            db.insert(DbContract.WHITE_ZONE.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "addWhiteZoneToDB: invalid location");
        }
    }

    /**
     * This method should be called for each event that we want to instrument in the app. The
     * method will automatically create a DB entry in the EVENT table for the passed event_type
     * with the current time
     *
     * @param event_type the event type that is to be added. All event types are defined in
     *                   Const.java
     */
    static void addEventToDB(int event_type) {
        Log.d(TAG, "addEventToDB: event_type: " + event_type);
        ContentValues contentValues = DbHelper.createEventContentValues(event_type,
                System.currentTimeMillis());
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        db.insert(DbContract.EVENT.TABLE_NAME, null, contentValues);
    }

    /**
     * This method returns the latest version of the app stored in the database. If this returns 0,
     * it means that this is a new installation as there are no rows in the app_versions table.
     *
     * @return the maximum value of app version stored in the database if it exists; 0 otherwise.
     */
    public static int getLatestAppVersionInDB() {
        Log.d(TAG, "in getLatestAppVersionInDB()");
        int app_version = 0;
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        Cursor cursor = null;
        String SELECT_CLAUSE = String.format(Locale.US,
                SELECT + MAX + "(%1$s) " + AS + " %1$s " + FROM + DbContract.APP_VERSION.TABLE_NAME,
                DbContract.APP_VERSION.COLUMN_NAME_VERSION);
        try {
            cursor = db.rawQuery(SELECT_CLAUSE, null);
            if (cursor.moveToNext()) {
                app_version = cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.APP_VERSION.COLUMN_NAME_VERSION));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return app_version;
    }

    /**
     * This method looks up the integer EventType for the supplied `eventStr` and then saves it to
     * the EVENT table.
     *
     * @param eventStr A string (generally the preference key of the
     *                 {@link SettingsPreferenceFragment}) which is to be saved.
     */
    public static void dumpEventToDatabase(String eventStr) {
        int eventTypeValue = Tools.getEventTypeValueFromString(eventStr);
        dumpEventToDatabase(eventTypeValue);
    }

    /**
     * This method saves the supplied `eventTypeValue` to the EVENT table in an AsyncTask.
     *
     * @param eventTypeValue An integer representing the event that is to be stored.
     */
    public static void dumpEventToDatabase(int eventTypeValue) {
        Log.d(TAG, "Insert " + eventTypeValue + " to event table");
        new DbRequestHandler.addEventToDBAsyncTask().execute(eventTypeValue);
    }

    /**
     * This method returns the current user profile stored in the DB
     *
     * @return a UserProfile object representing the current user profile in the DB if it is present
     * else return null
     */
    @Nullable
    public static UserProfile getUserProfile() {
        UserProfile userProfile = null;
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        Cursor cursor = null;

        try {
            // get the last entry in the DB
            cursor = db.query(
                    DbContract.USER_PROFILE.TABLE_NAME,  // The table to query
                    new String[]{ALL},                  // The columns to return
                    null,                               // The columns for the WHERE clause
                    null,                               // The values for the WHERE clause
                    null,                               // don't group the rows
                    null,                               // don't filter by row groups
                    _ID + DESC,                         // ORDER BY
                    LIMIT1                              // LIMIT
            );

            // move to the first table entry and returns false if the table is empty
            if (cursor.moveToFirst()) {
                userProfile = new UserProfile(
                        cursor.getString(cursor.getColumnIndexOrThrow(DbContract.USER_PROFILE.COLUMN_NAME_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DbContract.USER_PROFILE.COLUMN_NAME_EMAIL)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.USER_PROFILE.COLUMN_NAME_SEX)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.USER_PROFILE.COLUMN_NAME_AGE)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DbContract.USER_PROFILE.COLUMN_NAME_USER_SEGMENT))
                );
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return userProfile;
    }

    /**
     * Create an entry or update the current entry in the table USER_PROFILE. This table must not
     * have more than one line. Any update must erase the previous entry for RGPD compliance.
     *
     * @param name        the profile name
     * @param email       the profile email
     * @param sex         the profile sex
     * @param age         the profile age
     * @param userSegment the profile user segment
     * @param currentTime the insertion time of this new entry
     */
    public static void updateUserProfile(String name,
                                         String email, int sex, int age, int userSegment,
                                         long currentTime) {
        Log.d(TAG, "in updateUserProfile");

        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();

        Log.d(TAG, "updateUserProfile: We save in the DB name:" + name + " email " + email +
                " sex " + sex + " age " + age + " userSegment " + userSegment +
                " currentTime " + currentTime);
        ContentValues contentValues = DbHelper.createUserProfileContentValues(
                name, email, sex, age, userSegment, currentTime);

        // We test if the table is empty. If the table is empty we insert a row, otherwise,
        // we update the table, that is we replace the existing row
        if (DatabaseUtils.queryNumEntries(db, DbContract.USER_PROFILE.TABLE_NAME) == 0) {
            Log.d(TAG, "updateUserProfile: insert");
            db.insert(DbContract.USER_PROFILE.TABLE_NAME, null, contentValues);
        } else {
            Log.d(TAG, "updateUserProfile: update");
            db.update(DbContract.USER_PROFILE.TABLE_NAME, contentValues, null, null);
        }
    }

    /**
     * Queries the database and returns all top5signals from the first top 5 signal in the DB to
     * midnight of the passed date (midnight is the beginning of the passed date). Be careful, that
     * if you pass, e.g., 13 may 2019 10:30, it will return all top 5 signals strictly before
     * 13 may 2019 midnight, so all top 5 signals up to the 12th of may (inclusive, that is
     * 12 may 2019 23:59:59).
     *
     * @param date the method returns all top 5 signals up to midnight of date
     * @return ArrayList of BaseProperty of top 5 signals
     */
    public static HashSet<BaseProperty> getAllTop5SignalsBeforeTheDay(Calendar date) {
        Log.d(TAG, "in getAllTop5SignalsBeforeTheDay()");
        // Obtain midnight of the passed date
        Calendar midnightDate = (Calendar) date.clone();
        Tools.roundCalendarAtMidnight(midnightDate);

        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        String SELECT_QUERY = SELECT + DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS +
                FROM + DbContract.TOP_5_SIGNALS.TABLE_NAME + WHERE +
                DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE + LOWER + midnightDate.getTimeInMillis();
        Log.d(TAG, "getAllTop5SignalsBeforeTheDay: SELECT_QUERY : " + SELECT_QUERY);

        HashSet<BaseProperty> allTop5Signals = new HashSet<>();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(SELECT_QUERY, null);
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                String dayTop5SignalsString = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS));
                try {
                    ArrayList<BaseProperty> top5Signals = getTop5SignalsFromJsonString(dayTop5SignalsString);
                    Log.d(TAG, "getAllTop5SignalsBeforeTheDay: top5Signals size = " + top5Signals.size() + ", value = " + top5Signals.toString());
                    allTop5Signals.addAll(top5Signals);
                } catch (JSONException jsonException) {
                    Log.e(TAG, "getAllTop5SignalsBeforeTheDay: JSONException occurred. Details = " + jsonException.toString());
                    jsonException.printStackTrace();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "Done with getAllTop5SignalsBeforeTheDay: " + allTop5Signals.size() + " entries ");
        Log.d(TAG, "getAllTop5SignalsBeforeTheDay: allTop5Signals = " + allTop5Signals);

        return allTop5Signals;
    }

    /**
     * Queries the database and returns all top5signals for the specified day.
     *
     * @param day the method returns all top 5 signals on the specified day
     * @return ArrayList of BaseProperty of top 5 signals
     */
    public static List<BaseProperty> getAllTop5SignalsOfDay(Calendar day) {
        Log.d(TAG, "in getAllTop5SignalsOfDay()");
        // Obtain midnight of the passed day
        Calendar midnightDate = (Calendar) day.clone();
        Tools.roundCalendarAtMidnight(midnightDate);

        Calendar nextDay = (Calendar) midnightDate.clone();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);

        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        String SELECT_QUERY = SELECT + DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS +
                FROM + DbContract.TOP_5_SIGNALS.TABLE_NAME + WHERE +
                DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE + LARGER_EQ +
                midnightDate.getTimeInMillis() + AND +
                DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE + LOWER +
                nextDay.getTimeInMillis();
        Log.d(TAG, "getAllTop5SignalsOfDay: SELECT_QUERY : " + SELECT_QUERY);

        List<BaseProperty> allTop5Signals = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(SELECT_QUERY, null);
            Log.d(TAG, "cursor done");

            while (cursor.moveToNext()) {
                String dayTop5SignalsString = cursor.getString(cursor.getColumnIndexOrThrow(DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS));
                try {
                    ArrayList<BaseProperty> top5Signals = getTop5SignalsFromJsonString(dayTop5SignalsString);
                    Log.d(TAG, "getAllTop5SignalsOfDay: top5Signals size = " + top5Signals.size() + ", value = " + top5Signals.toString());
                    allTop5Signals.addAll(top5Signals);
                } catch (JSONException jsonException) {
                    Log.e(TAG, "getAllTop5SignalsOfDay: JSONException occurred. Details = " + jsonException.toString());
                    jsonException.printStackTrace();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "Done with getAllTop5SignalsOfDay: " + allTop5Signals.size() + " entries ");
        Log.d(TAG, "getAllTop5SignalsOfDay: allTop5Signals = " + allTop5Signals);

        return allTop5Signals;
    }


    /**
     * Queries the database and returns the daily stat summary for the specified day
     *
     * @param day the day for which method returns daily stat object of
     * @return the DailyStatSummary object belonging to the supplied day
     */
    public static DailyStatSummary getDailyStatSummaryOfDay(Calendar day) {
        // Obtain midnight of the passed day
        Calendar midnightDate = (Calendar) day.clone();
        Tools.roundCalendarAtMidnight(midnightDate);

        Calendar nextDay = (Calendar) midnightDate.clone();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);

        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();

        String SELECT_QUERY = SELECT + DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_DBM + COMMA +
                DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_NUMBER_OF_NEW_SOURCES + COMMA +
                DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE +
                FROM + DbContract.DAILY_STAT_SUMMARY.TABLE_NAME + WHERE +
                DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE + LARGER_EQ +
                midnightDate.getTimeInMillis() + AND +
                DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE + LOWER +
                nextDay.getTimeInMillis();
        Log.d(TAG, "getDailyStatSummaryOfDay: SELECT_QUERY : " + SELECT_QUERY);

        Cursor cursor = null;
        int numberOfNewSources = 0;
        // we take Const.MIN_DBM_FOR_ROOT_SCORE - 1 to represent by default a value that is invalid
        int exposureScoreDbm = Const.MIN_DBM_FOR_ROOT_SCORE - 1;
        long thisDayTimeMillis = 0;
        try {
            cursor = db.rawQuery(SELECT_QUERY, null);
            Log.d(TAG, "cursor done");

            if (cursor.moveToNext()) {
                exposureScoreDbm = cursor.getInt(cursor.getColumnIndexOrThrow(
                        DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_DBM));
                numberOfNewSources = cursor.getInt(cursor.getColumnIndexOrThrow(
                        DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_NUMBER_OF_NEW_SOURCES));
                thisDayTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(
                        DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "Done with getDailyStatSummaryOfDay numberOfNewSources: " + numberOfNewSources +
                ", exposureDbm: " + exposureScoreDbm + ", thisDayTimeMillis: " + thisDayTimeMillis);
        return new DailyStatSummary(exposureScoreDbm, thisDayTimeMillis, numberOfNewSources);
    }

    /**
     * Returns the oldest timestamp available in the {@link DbContract DAILY_STAT_SUMMARY} table
     *
     * @return The oldest timestamp in DAILY_STAT_SUMMARY table or 0 if the table is empty.
     */
    public static long getOldestDailyStatTimeStampDB() {
        String SELECT_QUERY = SELECT + MIN + "(" + DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE + ")" +
                AS + DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE +
                FROM + DbContract.DAILY_STAT_SUMMARY.TABLE_NAME;
        Log.d(TAG, "getOldestDailyStatTimeStampDB: SELECT_QUERY : " + SELECT_QUERY);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        Cursor cursor = null;
        long oldestSignalTimeStamp = 0;
        try {
            cursor = db.rawQuery(SELECT_QUERY, null);
            Log.d(TAG, "getOldestDailyStatTimeStampDB cursor done");

            while (cursor.moveToNext()) {
                oldestSignalTimeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE));
            }
            Log.d(TAG, "getOldestDailyStatTimeStampDB: oldestSignalTimeStamp = " + oldestSignalTimeStamp);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return oldestSignalTimeStamp;
    }

    /**
     * Returns the newest timestamp available in the {@link DbContract DAILY_STAT_SUMMARY} table
     *
     * @return The newest timestamp in DAILY_STAT_SUMMARY table or 0 if the table is empty.
     */
    private static long getNewestDailyStatTimeStampDB() {
        String SELECT_QUERY = SELECT + MAX + "(" + DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE + ")" +
                AS + DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE +
                FROM + DbContract.DAILY_STAT_SUMMARY.TABLE_NAME;
        Log.d(TAG, "getNewestDailyStatTimeStampDB: SELECT_QUERY : " + SELECT_QUERY);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        Cursor cursor = null;
        long newestDailyStatTimeStamp = 0;
        try {
            cursor = db.rawQuery(SELECT_QUERY, null);
            Log.d(TAG, "getNewestDailyStatTimeStampDB cursor done");

            while (cursor.moveToNext()) {
                newestDailyStatTimeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE));
            }
            Log.d(TAG, "getNewestDailyStatTimeStampDB: newestDailyStatTimeStamp = " + newestDailyStatTimeStamp);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return newestDailyStatTimeStamp;
    }


    /**
     * Returns the oldest timestamp available in the {@link DbContract TOP_5_SIGNALS} table
     *
     * @return The oldest timestamp in top5signals table or 0 if the table is empty.
     */
    public static long getOldestTop5SignalsTimeStampDB() {
        Log.d(TAG, "in getOldestTop5SignalsTimeStampDB()");
        String SELECT_QUERY = SELECT + MIN + "(" + DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE + ")" +
                AS + DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE +
                FROM + DbContract.TOP_5_SIGNALS.TABLE_NAME;
        Log.d(TAG, "getOldestTop5SignalsDateDB: SELECT_QUERY : " + SELECT_QUERY);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        Cursor cursor = null;
        long oldestSignalTimeStamp = 0;
        try {
            cursor = db.rawQuery(SELECT_QUERY, null);
            Log.d(TAG, "getOldestTop5SignalsDateDB cursor done");

            while (cursor.moveToNext()) {
                oldestSignalTimeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE));
            }
            Log.d(TAG, "getOldestTop5SignalsDateDB: oldestSignalTimeStamp = " + oldestSignalTimeStamp);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return oldestSignalTimeStamp;
    }


    /**
     * Returns the newest timestamp available in the {@link DbContract TOP_5_SIGNALS} table
     *
     * @return The newest timestamp in top5signals table or 0 if the table is empty.
     */
    private static long getNewestTop5SignalsTimeStampDB() {
        String SELECT_QUERY = SELECT + MAX + "(" + DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE + ")" +
                AS + DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE +
                FROM + DbContract.TOP_5_SIGNALS.TABLE_NAME;
        Log.d(TAG, "getNewestTop5SignalsTimeStampDB: SELECT_QUERY : " + SELECT_QUERY);
        SQLiteDatabase db = MainApplication.dbHelper.getReadableDatabase();
        Cursor cursor = null;
        long newestSignalTimeStamp = 0;
        try {
            cursor = db.rawQuery(SELECT_QUERY, null);
            Log.d(TAG, "getOldestTop5SignalsDateDB cursor done");

            while (cursor.moveToNext()) {
                newestSignalTimeStamp = cursor.getLong(cursor.getColumnIndexOrThrow(DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE));
            }
            Log.d(TAG, "getNewestTop5SignalsTimeStampDB: newestSignalTimeStamp = " + newestSignalTimeStamp);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return newestSignalTimeStamp;
    }


    /**
     * Inserts the given top5SignalsJsonString string of the given date into the  TOP_5_SIGNALS table
     *
     * @param timeMillis            the day represented as millisecond (number of milliseconds since
     *                              Jan. 1, 1970, midnight GMT)
     * @param top5SignalsJsonString the JSON representation of the top 5 signals for the given day
     */
    private static void addTop5SignalsToDB(long timeMillis, String top5SignalsJsonString) {
        Log.d(TAG, "addTop5SignalsToDB: date: " + timeMillis + " top5SignalsJsonString: " + top5SignalsJsonString);
        ContentValues contentValues = DbHelper.createTop5SignalsContentValues(top5SignalsJsonString,
                timeMillis);
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        db.insert(DbContract.TOP_5_SIGNALS.TABLE_NAME, null, contentValues);
    }

    /**
     * Inserts in the DAILY_STAT_SUMMARY a single row entry
     *
     * @param dbm                   the exposure in dbm
     * @param numberOfNewSources    the number of new sources found on this day
     * @param summaryDateTimeMillis the day represented as millisecond (number of milliseconds since
     *                              Jan. 1, 1970, midnight GMT)
     */
    private static void addDailyStatSummaryToDB(int dbm, int numberOfNewSources, long summaryDateTimeMillis) {
        Log.d(TAG, "addDailyStatSummaryToDB: dbm: " + dbm + " summaryDateTimeMillis: " +
                summaryDateTimeMillis + " numberOfNewSources: " + numberOfNewSources);
        ContentValues contentValues = DbHelper.createDailyStatSummaryContentValues(dbm,
                numberOfNewSources, summaryDateTimeMillis);
        SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
        db.insert(DbContract.DAILY_STAT_SUMMARY.TABLE_NAME, null, contentValues);
    }


    /**
     * Construct the top5Signals ArrayList from the JSON string representation
     *
     * @param jsonString String representation of the top5Signals
     * @return ArrayList of BaseProperty in the given jsonString
     * @throws JSONException The jsonString can't be parsed
     */
    private static ArrayList<BaseProperty> getTop5SignalsFromJsonString(String jsonString) throws JSONException {
        ArrayList<BaseProperty> top5Signals = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(jsonString);
        for (int i = 0; i < jsonArray.length(); i++) {
            BaseProperty signal = Tools.getBasePropertyFromJsonString(jsonArray.get(i).toString());
            if (signal != null) {
                top5Signals.add(signal);
            }
        }
        return top5Signals;
    }

    /**
     * Get the top5Signals JSON string representation
     *
     * @param top5Signals Array List of BaseProperty
     * @return json string representation of the top5 signals
     */
    private static String getJsonStringFromTop5Signals(ArrayList<BaseProperty> top5Signals) {
        JSONArray top5SignalsJsonArray = new JSONArray();
        for (BaseProperty signal : top5Signals) {
            top5SignalsJsonArray.put(signal.toJsonString());
        }
        return top5SignalsJsonArray.toString();
    }

    /**
     * This method retrieve from the DB all {@link SignalsSlot} that belong to specific timeSlots,
     * in the returned List we have one {@link SignalsSlot} per timeSlot. So, the returned list
     * can be directly added to a Timeline object.
     *
     * @param timeSlots The timeSlots for which we will retrieve {@link SignalsSlot}
     * @return A list of {@link SignalsSlot}
     */
    static List<SignalsSlot> getSignalsSlotsFromTimeSlots(List<Pair<Date, Date>> timeSlots) {
        Log.d(TAG, "in getSignalsSlotsFromTimeSlots()");

        // Contains all signals per type for multiple time periods. For instance, for the
        // History mode, we have 24 (one per hour) ArrayList per type of signal.
        //
        // For instance, allsignalsPerTypePerTimePeriods.get("Wifi") returns a
        // List<List<BaseProperty>> and allsignalsPerTypePerTimePeriods.get("Wifi").get(0)
        // returns an ArrayList of all the signals for Wifi for the first time period
        ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<List<BaseProperty>>> allsignalsPerTypePerTimePeriods;

        // 1) We populate  allsignalsPerTypePerTimePeriods from the local database, if some
        // signals are missing, we fill them with a fake signal with INVALID_SIGNAL dbm values
        allsignalsPerTypePerTimePeriods = createAllSignalsPerTypePerTimePeriods(timeSlots);

        Log.d(TAG, "allsignalsPerTypePerTimePeriods.size(): " + allsignalsPerTypePerTimePeriods.get(MeasurementFragment.AntennaDisplay.CELLULAR).size());

        Log.d(TAG, "allsignalsPerTypePerTimePeriods: " + allsignalsPerTypePerTimePeriods);
        // 2) based on allsignalsPerTypePerTimePeriods, we create the SignalsSlot and the
        // corresponding timeline that will be appended to the current timeline.
        Log.d(TAG, "START allsignalsPerTypePerTimePeriods processing");

        List<SignalsSlot> timeline = new ArrayList<>();
        for (int i = 0; i < timeSlots.size(); i++) {
            ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> rawSignals = new ConcurrentHashMap<>();
            for (MeasurementFragment.AntennaDisplay antenna : allsignalsPerTypePerTimePeriods.keySet()) {
                rawSignals.put(antenna, allsignalsPerTypePerTimePeriods.get(antenna).get(i));
            }
            timeline.add(new SignalsSlot(rawSignals));
        }

        Log.d(TAG, "END allsignalsPerTypePerTimePeriods processing");
        return timeline;
    }

    /**
     * Computes the top 5 signals and daily stat summaries for yesterday. In case, there are missing
     * top 5 signals before yesterday, we compute them.
     * <p>
     * This method automatically fills missing top 5 signals or missing daily stat summaries in the
     * DB starting for the latest DB entry to yesterday.
     * <p>
     * This computation is made independently for the top 5 signals and for the daily stats. So
     * both tables do not need to be synchronized (that is, the date of the latest computed entry
     * does not need to be the same). Also, this method gracefully handle the case of an empty table.
     * Finally, in case both tables are up to date, this method does not perform useless computation
     * and returns very fast.
     * <p>
     * This method can therefore safely be called each time we start the daily summary stat fragment
     * <p>
     * <p>
     * We use the following logic in this method
     * -----------------------------------------
     * <p>
     * we compute the top five signals and daily summary stats for all days in the interval
     * ] startDayMillis, yesterday ]
     * where startDayMillis = min(newestTop5SignalsTimestampInDB, newestDailyStatTimestampInDB)
     * if the entry in the TOP_5_SIGNALS or DAILY_STAT_SUMMARY does not exist yet
     * <p>
     * yesterday is an upper bound, the actual variable that is incremented by one day at each
     * round of the while loop is nextDay
     * <p>
     * To summarize we have:
     * <p>
     * startDayMillis          nextDay (incremented in the while loop)          yesterday
     * ----------------------------------------------------------------------------------> time
     *
     * @return an integer representing the number of new top 5 signals found on yesterday
     */
    public static int computeTop5SignalsAndDailyStatSummaryOnYesterday() {
        Log.d(TAG, "in computeTop5SignalsAndDailyStatSummaryOnYesterday()");

        // 1. we get yesterday's date at midnight
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        Tools.roundCalendarAtMidnight(yesterday);

        Log.d(TAG, "In computeTop5SignalsAndDailyStatSummaryOnYesterday(). yesterday=" + yesterday.getTime());

        // we set triggerNotificationForNewSource to true if we detect a new source than must
        // be notified to the user.
        boolean triggerNotificationForNewSource = false;


        // 2. we create the calendar objects representing the newest timestamp for the
        //    TOP_5_SIGNALS and DAILY_STAT_SUMMARY tables. The goal is to detect in the for loop
        //    whether we need to update the tables or if the entry is already created. It allows in
        //    a single for loop to update two tables that might not be in sync.

        // we get the timestamp of the latest entry in the TOP_5_SIGNALS table
        long newestTop5SignalsTimestampInDB = getNewestTop5SignalsTimeStampDB();
        // we get the timestamp of the latest entry in the DAILY_STAT_SUMMARY table
        long newestDailyStatTimestampInDB = getNewestDailyStatTimeStampDB();
        // we get the timestamp of the oldest entry in the DB
        long oldestSignalInDb = Tools.oldestSignalInDb();

        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: newestTop5SignalsTimestampInDB: " + newestTop5SignalsTimestampInDB);
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: newestDailyStatTimestampInDB: " + newestDailyStatTimestampInDB);
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: oldestSignalInDb: " + new Date(oldestSignalInDb));


        // we create the calendar object for the TOP_5_SIGNALS handling the case of an empty table
        // with a default value
        Calendar newestTop5SignalsTimestampInDBCal = Calendar.getInstance();
        if (newestTop5SignalsTimestampInDB == 0) {
            int daysSinceFirstSignalInDb = Tools.getNumberOfDaysBetweenTimestamps(
                    oldestSignalInDb, System.currentTimeMillis());
            Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: daysSinceFirstSignalInDb: " + daysSinceFirstSignalInDb);

            newestTop5SignalsTimestampInDBCal.add(Calendar.DAY_OF_MONTH, -Math.min(
                    daysSinceFirstSignalInDb,
                    Const.MAX_NUM_OF_DAYS_TO_COMPUTE_NEW_SOURCES_ON_UPDATE));
            // newestTop5SignalsTimestampInDBCal the latest entry in the table TOP_5_SIGNALS.
            // In case, newestTop5SignalsTimestampInDB == 0, it means there is no entry yet.
            // Therefore, we want to set newestTop5SignalsTimestampInDBCal one day before the day
            // we want to start the computation of the top 5 signals
            newestTop5SignalsTimestampInDBCal.add(Calendar.DAY_OF_MONTH, -1);
        } else {
            newestTop5SignalsTimestampInDBCal.setTimeInMillis(newestTop5SignalsTimestampInDB);
        }
        Tools.roundCalendarAtMidnight(newestTop5SignalsTimestampInDBCal);

        // we create the calendar object for the DAILY_STAT_SUMMARY handling the case of an empty
        // table with a default value
        Calendar newestDailyStatTimestampInDBCal = Calendar.getInstance();
        // Calendar that represents the oldest allowed date  in the DB
        Calendar maxOldestSignalInDbCal = (Calendar) yesterday.clone();
        maxOldestSignalInDbCal.add(Calendar.DAY_OF_MONTH, -Tools.getNumberOfDaysInHistory());
        if (newestDailyStatTimestampInDB == 0) {
            // we take the most recent date between the oldest date in the DB and the maximum
            // allowed oldest date in the DB. Note that it is possible to have signals in the DB
            // that are older than the maximum possible oldest date in the DB because we did not
            // clean the DB yet.
            newestDailyStatTimestampInDBCal.setTimeInMillis(
                    Math.max(oldestSignalInDb, maxOldestSignalInDbCal.getTimeInMillis()));
            // newestDailyStatTimestampInDBCal the latest entry in the table DAILY_STAT_SUMMARY.
            // In case, newestDailyStatTimestampInDB == 0, it means there is no entry yet.
            // Therefore, we want to set newestDailyStatTimestampInDBCal one day before the oldest
            // entry in the DB, that is we don't have entry for the oldest date in the DB so the
            // computation can start with the oldest date in the DB
            newestDailyStatTimestampInDBCal.add(Calendar.DAY_OF_MONTH, -1);
        } else {
            newestDailyStatTimestampInDBCal.setTimeInMillis(newestDailyStatTimestampInDB);
        }
        Tools.roundCalendarAtMidnight(newestDailyStatTimestampInDBCal);

        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: newestTop5SignalsTimestampInDBCal: " + newestTop5SignalsTimestampInDBCal.getTime());
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: newestDailyStatTimestampInDBCal: " + newestDailyStatTimestampInDBCal.getTime());


        // 3. we go day by day from nextDay (incremented by one day at each loop) to yesterday
        //    to compute the daily top 5 signals and the daily summary stats

        // we create a Calendar object nextDay that is the min of the newest entries in both tables
        // (TOP_5_SIGNALS and DAILY_STAT_SUMMARY) and we add one day (to start computing on the
        // next day). Finally, we round it to midnight.
        // This is the day that will be used to start the computation and we will iterate on.
        Calendar nextDay = Calendar.getInstance();
        nextDay.setTimeInMillis(Math.min(newestTop5SignalsTimestampInDBCal.getTimeInMillis(),
                newestDailyStatTimestampInDBCal.getTimeInMillis()));
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        Tools.roundCalendarAtMidnight(nextDay);
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: nextDayDate=" + nextDay.getTime());

        ArrayList<BaseProperty> topFiveSignalsDay = null;
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday for loop. yesterday=" + yesterday.getTime());

        // we iterate on nextDay up to yesterday
        for (; nextDay.before(yesterday) || nextDay.equals(yesterday); nextDay.add(Calendar.DAY_OF_MONTH, 1)) {
            Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday for loop. nextDay=" + nextDay.getTime());
            Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: nextDay.before(yesterday) " + nextDay.before(yesterday));
            Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: nextDay.equals(yesterday) " + nextDay.equals(yesterday));

            // 3.1 we create a timeline object for the day

            // we create the timeSlots object
            final List<Pair<Date, Date>> timeSlots = DbHistoryAsyncTaskRequest.timeSlots(
                    nextDay.getTimeInMillis(),
                    StatisticsFragment.ONE_DAY_WINDOW,
                    Const.STATS_HOUR_TIME_GAP,
                    false);

            // from the timeSlots object we get all SignalsSlot and create a Timeline accordingly
            Timeline timeline = new Timeline(StatisticsFragment.ONE_DAY_WINDOW,
                    Const.STATS_HOUR_TIME_GAP);
            timeline.reset(timeSlots.get(0).first.getTime() / 1000);
            // here we make the real costly DB request
            timeline.addAll(0, getSignalsSlotsFromTimeSlots(timeSlots));

            // 3.2 we compute the topFiveSignalsDay and write them to the DB

            // we iterate through all the hours of the day to store in topFiveSignalsDay all the
            // unique top 5 signals of the day and write it to the DB.
            List<BaseProperty> topFiveSignals;
            topFiveSignalsDay = new ArrayList<>();
            // we compute the topFiveSignalsDay only if nextDay is after
            // newestTop5SignalsTimestampInDBCal, that is, an entry does not exist yet in the table
            if (nextDay.after(newestTop5SignalsTimestampInDBCal)) {
                for (int i = 0; i < StatisticsFragment.ONE_DAY_WINDOW; i++) {
                    topFiveSignals = timeline.get(i).getTopFiveSignals();


                    // We build the topFiveSignalsDay and keep only unique signals.
                    // In case of equality, we keep the highest power signal.
                    if (topFiveSignals != null) {
                        for (BaseProperty signal : topFiveSignals) {
                            // we do not add invalid signals
                            if (signal.isValidSignal) {
                                if (topFiveSignalsDay.contains(signal)) {
                                    int indexOldSignal = topFiveSignalsDay.indexOf(signal);
                                    BaseProperty oldSignal = topFiveSignalsDay.get(indexOldSignal);
                                    if (oldSignal.dbm < signal.dbm) {
                                        topFiveSignalsDay.remove(indexOldSignal);
                                        topFiveSignalsDay.add(signal);
                                    }
                                } else {
                                    topFiveSignalsDay.add(signal);
                                }
                            }
                        }
                    }
                }

                // we sort the topFiveSignalsDay and keep only the top 5 highest power signals
                Tools.arraySortDbm(topFiveSignalsDay, true);
                if (topFiveSignalsDay.size() > 5) {
                    topFiveSignalsDay = new ArrayList<>(topFiveSignalsDay.subList(0, 5));
                }

                // we save to the DB the computed top 5 signals of the day
                String jsonStringTop5SignalsDay = getJsonStringFromTop5Signals(topFiveSignalsDay);
                addTop5SignalsToDB(nextDay.getTimeInMillis(), jsonStringTop5SignalsDay);

                Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: topFiveSignalsDay.size() = " + topFiveSignalsDay.size());
                Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: jsonStringTop5SignalsDay = " + jsonStringTop5SignalsDay);
                Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: jsonStringTop5SignalsDay length = " + jsonStringTop5SignalsDay.length());
            }


            // 3.3 we compute the daily stat summary and write it to the DB

            // we compute the daily stat summary only if nextDay is after
            // newestDailyStatTimestampInDBCal, that is, an entry does not exist yet in the table
            if (nextDay.after(newestDailyStatTimestampInDBCal)) {
                // we iterate through all the hours of the day to compute store dayAverageMwValue.
                double sum = 0;
                int nbValidHours = 0; // count the number of hours in valid SignalsSlot
                for (int i = 0; i < StatisticsFragment.ONE_DAY_WINDOW; i++) {
                    SignalsSlot signalsSlot = timeline.get(i);
                    sum = sum + signalsSlot.getSlotCumulativeTotalMwValue();
                    // we count the number of hours in valid SignalsSlot
                    if (signalsSlot.containsValidSignals()) {
                        nbValidHours = nbValidHours + 1;
                    }
                }
                double dayAverageMwValue = sum / nbValidHours;

                Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: nextDay.after(newestDailyStatTimestampInDB)");
                addDailyStatSummaryToDB(Tools.milliWattToDbm(dayAverageMwValue),
                        numOfSourcesInTop5SignalsOnDay(nextDay, getAllTop5SignalsOfDay(nextDay)), nextDay.getTimeInMillis()
                );
            }
        }

        // 4. We decide whether a notification must be triggered. Note that we only return a flag,
        //    this method does really create the notification

        /*
         We must trigger a notification if a new source has been detected for yesterday, and if we
         have at least MIN_NUM_OF_DAYS_BEFORE_SHOWING_NEW_SOURCE_NOTIF days of daily top 5 sources
         in the DB before yesterday.
        */
        long oldestTop5SignalsTimestampInDB = DbRequestHandler.getOldestTop5SignalsTimeStampDB();
        int newSourcesCount = numOfSourcesInTop5SignalsOnDay(yesterday, getAllTop5SignalsOfDay(yesterday));

        if (newSourcesCount > 0 && oldestTop5SignalsTimestampInDB != 0 &&
                (Tools.getNumberOfDaysBetweenTimestamps(oldestTop5SignalsTimestampInDB,
                        yesterday.getTimeInMillis()) >=
                        Const.MIN_NUM_OF_DAYS_BEFORE_SHOWING_NEW_SOURCE_NOTIF)) {
            triggerNotificationForNewSource = true;
        }

        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: yesterday: " + yesterday.getTime());
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday : newSourcesCount = " + newSourcesCount);
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: number of days since the first day in the DB and yesterday: " +
                Tools.getNumberOfDaysBetweenTimestamps(oldestTop5SignalsTimestampInDB,
                        yesterday.getTimeInMillis()));
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: triggerNotificationForNewSource = " + triggerNotificationForNewSource);
        Log.d(TAG, "computeTop5SignalsAndDailyStatSummaryOnYesterday: DONE!");

        return triggerNotificationForNewSource ? newSourcesCount : 0;
    }

    /**
     * We count the number of new sources within the top five signals of the passed day.
     * <p>
     * newSourcesCount is used to show the NEW label in the daily stat summary views. It must
     * use the same criteria to be displayed as in the StatisticsFragment.
     *
     * @param day               The day on which topFiveSignalsDay is computed
     * @param topFiveSignalsDay the list of top 5 signals for the day, if null, returns 0
     * @return the number of new sources among the top 5 signals for the day
     */
    private static int numOfSourcesInTop5SignalsOnDay(Calendar day,
                                                      List<BaseProperty> topFiveSignalsDay) {
        Log.d(TAG, "in numOfSourcesInTop5SignalsOnDay()");
        int newSourcesCount = 0;

        if (topFiveSignalsDay == null) {
            Log.d(TAG, "numOfSourcesInTop5SignalsOnDay: topFiveSignals is null");
            return newSourcesCount;
        }

        HashSet<BaseProperty> allTop5SignalsBeforeNextDay = getAllTop5SignalsBeforeTheDay(day);
        long oldestTop5SignalsTimestampInDB = DbRequestHandler.getOldestTop5SignalsTimeStampDB();
        if (oldestTop5SignalsTimestampInDB != 0 &&
                (Tools.getNumberOfDaysBetweenTimestamps(oldestTop5SignalsTimestampInDB,
                        day.getTimeInMillis()) >=
                        Const.MIN_NUM_OF_DAYS_BEFORE_SHOWING_NEW_SOURCE_NOTIF)) {
            for (BaseProperty signal : topFiveSignalsDay) {
                if (!allTop5SignalsBeforeNextDay.contains(signal)) {
                    // we do not count new sources for Cellular signals
                    if ((signal instanceof WifiProperty) || (signal instanceof BluetoothProperty)) {
                        newSourcesCount++;
                    }
                }
            }
        }
        Log.d(TAG, "numOfSourcesInTop5SignalsOnDay: newSourcesCount: " + newSourcesCount);
        return newSourcesCount;
    }

    /**
     * Asynchronous task dump white zones to the DB
     * <p>
     * The order of execution described in
     * https://developer.android.com/reference/android/os/AsyncTask.html
     * guarantees that starting with HONEYCOMB (3.0) submitted tasks are executed sequentially on a
     * single thread.
     */
    public static class addWhiteZoneToDBAsyncTask extends AsyncTask<Integer, Void, Void> {
        protected final Void doInBackground(Integer... ints) {
            Log.d(TAG, "doInBackground: addWhiteZoneToDB starting DB dump...");
            addWhiteZoneToDB(ints[0]);
            Log.d(TAG, "doInBackground: addWhiteZoneToDB DONE!");
            return null;
        }
    }

    /**
     * An asynchronous task to dump instrumentation events to the DB
     */
    public static class addEventToDBAsyncTask extends AsyncTask<Integer, Void, Void> {
        protected final Void doInBackground(Integer... ints) {
            Log.d(TAG, "doInBackground: addEventToDBAsyncTask (event table) starting DB dump...");
            addEventToDB(ints[0]);
            Log.d(TAG, "doInBackground: addEventToDBAsyncTask (event table) DONE!");
            return null;
        }
    }


    /**
     * A background async task that exports the following tables in a zip file containing CSV files,
     * one CSV per table:
     * GSM, WCDMA, LTE, NEW_RADIO, CDMA, WIFI, BLUETOOTH
     * <p>
     * Note that it is a full export, the entire tables are exported
     */
    public static class exportCsvAsyncTask extends AsyncTask<Uri, Void, Boolean> {
        private AppCompatActivity context;

        public exportCsvAsyncTask(AppCompatActivity mContext) {
            this.context = mContext;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast toast = Toast.makeText(context, R.string.csv_export_start, Toast.LENGTH_LONG);
            toast.show();
        }

        protected final Boolean doInBackground(Uri... ints) {
            Log.d(TAG, "doInBackground: exportCsvAsyncTask starting DB export...");

            long oldestDateInDb = Tools.oldestSignalInDb();
            long now = System.currentTimeMillis();
            Date startDate = new Date(oldestDateInDb);
            Date endDate = new Date(now);
            boolean isExportCompleted = true;

            try {
                JSONObject allSignalTablesInJSON = new JSONObject();
                allSignalTablesInJSON.put(DbContract.GSM.TABLE_NAME, dumpGSM2jsonArray(startDate, endDate));
                allSignalTablesInJSON.put(DbContract.WCDMA.TABLE_NAME, dumpWCDMA2jsonArray(startDate, endDate));
                allSignalTablesInJSON.put(DbContract.LTE.TABLE_NAME, dumpLTE2jsonArray(startDate, endDate));
                allSignalTablesInJSON.put(DbContract.NEW_RADIO.TABLE_NAME, dumpNewRadio2jsonArray(startDate, endDate));
                allSignalTablesInJSON.put(DbContract.CDMA.TABLE_NAME, dumpCDMA2jsonArray(startDate, endDate));
                allSignalTablesInJSON.put(DbContract.WIFI.TABLE_NAME, dumpWIFI2jsonArray(startDate, endDate));
                allSignalTablesInJSON.put(DbContract.BLUETOOTH.TABLE_NAME, dumpBLUETOOTH2jsonArray(startDate, endDate));

                // App specific temp directory in which we write the CSV files before
                // making them accessible to the user in the shared dir selected by the user using
                // the storage access framework. This app specific temp dir does not require any
                // permission. We can find this app in the Android Studio Device File Explorer under
                // /data/data/fr.inria.es.electrosmart.debug/files
                String csvTempOutputDirPath = MainApplication.getContext().getFilesDir().getPath() +
                        File.separator + "ElectroSmartCsvExport" + File.separator;

                // we clean the directory first. In case the process is killed before the files are
                // deleted, it is possible to have files in the temp dir when we enter
                // doInBackground. For this reason, we clear before generating new CSV to be sure
                // no old files remain.
                CsvFileWriter.cleanDirectory(csvTempOutputDirPath);
                CsvFileWriter.writeJsonToCsv(allSignalTablesInJSON, csvTempOutputDirPath);
                Uri uri = ints[0];
                CsvFileWriter.zipCsvFiles(csvTempOutputDirPath, uri);
                // we clean again when the export is done to save space in the local storage.
                CsvFileWriter.cleanDirectory(csvTempOutputDirPath);

            } catch (JSONException jsonException) {
                Log.d(TAG, "doInBackground: JSONException " + jsonException);
                isExportCompleted = false;
            } catch (IOException ioException) {
                Log.d(TAG, "doInBackground: IOException " + ioException);
                // IOException might bit triggered if there is not enough space to write the files
                isExportCompleted = false;

                // result = e.getMessage();
                // e.printStackTrace();
                // display a dialog to delete files in order to retrieve storage space
                // Intent intent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
                // context.startActivityForResult(intent, 400);
            }
            Log.d(TAG, "doInBackground: exportCsvAsyncTask DONE!");
            return isExportCompleted;
        }

        protected void onPostExecute(Boolean isExportCompleted) {
            Log.d(TAG, "onPostExecute: csvDone");
            if (isExportCompleted) {
                // The export CSV was successful
                Toast toast = Toast.makeText(context, R.string.csv_export_done, Toast.LENGTH_LONG);
                toast.show();
            } else {
                // The export CSV failed
                Toast toast = Toast.makeText(context, R.string.csv_export_error, Toast.LENGTH_LONG);
                toast.show();
            }

        }
    }
}

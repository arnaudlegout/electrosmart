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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;

// IMPORTANT: HOW TO DEBUG THE SQLITE DATABASE
// -------------------------------------------
// run 'adb shell' to connect on the phone (or 'adb -s device_name shell' when device name is
// obtained from 'adb devices'. Then we can access the database in /data/data/fr.inria.es.electrosmart/databases
// However, this directory is write protected to users, but the application fr.inria.es.electrosmart
// So we run 'run-as fr.inria.es.electrosmart'. Then we can access the .db file.
// Then we can copy this file to the /sdcard directory, and from there make an
// 'adb pull /sdcard/es.db C:\...'. I wrote a Python script to automate the process
// import os
// os.system('adb.exe shell run-as fr.inria.es.electrosmart cp /data/data/fr.inria.es.electrosmart/databases/es.db /sdcard/.')
// os.system('adb.exe pull /sdcard/es.db C:\Temp\.')

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = "DbHelper";

    // If you change the database schema, you must increment the database version.
    /*
    DATABASE_VERSION version 2 was released with 1.2R57
    DATABASE_VERSION version 3 was released with 1.2R59
    DATABASE_VERSION version 4 was released with 1.5R61
    DATABASE_VERSION version 5 was released with 1.6R62
    DATABASE_VERSION version 6 was released with 1.7R64
    DATABASE_VERSION version 7 was released with 1.8R66
    DATABASE_VERSION version 8 was released with 1.9R68 :  For onboarding, we add the USER_PROFILE table
    DATABASE_VERSION version 9 was released with 1.11R75:  Add the DEVICE_LOCALE table
    DATABASE_VERSION version 10 was released with 1.17R90: Add the TOP_5_SIGNALS table
    DATABASE_VERSION version 11 was released with 1.20R97: Add the DAILY_STAT_SUMMARY table
    DATABASE_VERSION version 12 was released with .......: Add the NewRadio table and add the bandwidth column to Lte table
    DATABASE_VERSION version 13 was released with 1.28R118: Add the wifi standard in WIFI table, ecno field for WCDMA, soc_manufacturer and soc_model in the DEVICE_INFO table

     */
    private static final int DATABASE_VERSION = 13;
    private static final String DATABASE_NAME = "es.db";

    // define the constants used in the tables creation SQL requests
    private static final String TEXT_TYPE = " TEXT ";
    private static final String INT_TYPE = " INTEGER ";
    private static final String REAL_TYPE = " REAL ";
    private static final String PRIMARY_KEY = " PRIMARY KEY ";
    private static final String COMMA_SEP = ",";
    private static final String SP = " ";
    private static final String PARENTHESIS_START = " (";
    private static final String PARENTHESIS_STOP = ");";
    private static final String CREATE_TABLE = "CREATE TABLE ";
    private static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";
    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD = " ADD ";
    private static final String DEFAULT = " DEFAULT ";
    private static final String DELETE = "DELETE ";
    private static final String FROM = " FROM ";
    private static final String ZERO = "0";

    // define the CREATE TABLE strings for each table
    private static final String SQL_CREATE_GSM =
            CREATE_TABLE + DbContract.GSM.TABLE_NAME + PARENTHESIS_START +
                    DbContract.GSM._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_MNC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_MCC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_CID + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_LAC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_ARFCN + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_BSIC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_TIMING_ADVANCE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_DBM + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_BER + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_CONNECTED + SP + INT_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.GSM.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_WCDMA =
            CREATE_TABLE + DbContract.WCDMA.TABLE_NAME + PARENTHESIS_START +
                    DbContract.WCDMA._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_MNC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_MCC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_UCID + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_LAC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_PSC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_UARFCN + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_DBM + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_BER + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_ECNO + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_CONNECTED + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.WCDMA.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_LTE =
            CREATE_TABLE + DbContract.LTE.TABLE_NAME + PARENTHESIS_START +
                    DbContract.LTE._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_MNC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_MCC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_ECI + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_PCI + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_TAC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_EARFCN + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_BANDWIDTH + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_TIMING_ADVANCE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_RSRP + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_RSSI + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_RSRQ + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_RSSNR + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_CQI + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_CONNECTED + SP + INT_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.LTE.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_NEW_RADIO =
            CREATE_TABLE + DbContract.NEW_RADIO.TABLE_NAME + PARENTHESIS_START +
                    DbContract.NEW_RADIO._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_MCC + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_MNC + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_NCI + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_PCI + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_TAC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRQ + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_CSI_SINR + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRP + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRQ + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_SS_SINR + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED + SP + INT_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.NEW_RADIO.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_CDMA =
            CREATE_TABLE + DbContract.CDMA.TABLE_NAME + PARENTHESIS_START +
                    DbContract.CDMA._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_BASE_STATION_ID + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_NETWORK_ID + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_SYSTEM_ID + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_STATION_LATITUDE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_STATION_LONGITUDE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_CDMA_DBM + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_CDMA_ECIO + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_EVDO_DBM + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_EVDO_ECIO + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_EVDO_SNR + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_CONNECTED + SP + INT_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.CDMA.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_WIFI =
            CREATE_TABLE + DbContract.WIFI.TABLE_NAME + PARENTHESIS_START +
                    DbContract.WIFI._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_SSID + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_BSSID + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_OPERATOR_FRIENDLY_NAME + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_VENUE_NAME + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_IS_PASSPOINT_NETWORK + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_FREQ + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_CENTERFREQ0 + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_CENTERFREQ1 + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_CHANNELWIDTH + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_CAPABILITIES + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_DBM + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_CONNECTED + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.WIFI.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_BLUETOOTH =
            CREATE_TABLE + DbContract.BLUETOOTH.TABLE_NAME + PARENTHESIS_START +
                    DbContract.BLUETOOTH._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_NAME + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS + SP + INT_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS + SP + INT_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_DBM + SP + INT_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_OPERATORS =
            CREATE_TABLE + DbContract.OPERATORS.TABLE_NAME + PARENTHESIS_START +
                    DbContract.OPERATORS._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.OPERATORS.COLUMN_NAME_MNC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.OPERATORS.COLUMN_NAME_MCC + SP + INT_TYPE + COMMA_SEP +
                    DbContract.OPERATORS.COLUMN_NAME_OPERATOR_NAME + SP + TEXT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_ORIENTATION =
            CREATE_TABLE + DbContract.ORIENTATION.TABLE_NAME + PARENTHESIS_START +
                    DbContract.ORIENTATION._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.ORIENTATION.COLUMN_NAME_X + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.ORIENTATION.COLUMN_NAME_Y + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.ORIENTATION.COLUMN_NAME_Z + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.ORIENTATION.COLUMN_NAME_ANGLE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.ORIENTATION.COLUMN_NAME_ACCURACY + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.ORIENTATION.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_DEVICE_INFO =
            CREATE_TABLE + DbContract.DEVICE_INFO.TABLE_NAME + PARENTHESIS_START +
                    DbContract.DEVICE_INFO._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_BRAND + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_DEVICE + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_HARDWARE + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_MANUFACTURER + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_MODEL + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_PRODUCT + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MANUFACTURER + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MODEL + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_INFO.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_OS_INFO =
            CREATE_TABLE + DbContract.OS_INFO.TABLE_NAME + PARENTHESIS_START +
                    DbContract.OS_INFO._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.OS_INFO.COLUMN_NAME_RELEASE + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.OS_INFO.COLUMN_NAME_SDK_INT + SP + INT_TYPE + COMMA_SEP +
                    DbContract.OS_INFO.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_SIM_INFO =
            CREATE_TABLE + DbContract.SIM_INFO.TABLE_NAME + PARENTHESIS_START +
                    DbContract.SIM_INFO._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.SIM_INFO.COLUMN_NAME_SIM_COUNTRY_CODE + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR_NAME + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.SIM_INFO.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_APP_VERSION =
            CREATE_TABLE + DbContract.APP_VERSION.TABLE_NAME + PARENTHESIS_START +
                    DbContract.APP_VERSION._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.APP_VERSION.COLUMN_NAME_VERSION + SP + INT_TYPE + COMMA_SEP +
                    DbContract.APP_VERSION.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_PROFILE_ID =
            CREATE_TABLE + DbContract.PROFILE_ID.TABLE_NAME + PARENTHESIS_START +
                    DbContract.PROFILE_ID._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.PROFILE_ID.COLUMN_NAME_ID + SP + TEXT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_WHITE_ZONE =
            CREATE_TABLE + DbContract.WHITE_ZONE.TABLE_NAME + PARENTHESIS_START +
                    DbContract.WHITE_ZONE._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.WHITE_ZONE.COLUMN_NAME_SIGNAL_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.WHITE_ZONE.COLUMN_NAME_LATITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.WHITE_ZONE.COLUMN_NAME_LONGITUDE + SP + REAL_TYPE + COMMA_SEP +
                    DbContract.WHITE_ZONE.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_EVENT =
            CREATE_TABLE + DbContract.EVENT.TABLE_NAME + PARENTHESIS_START +
                    DbContract.EVENT._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.EVENT.COLUMN_NAME_EVENT_TYPE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.EVENT.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_DEVICE_LOCALE =
            CREATE_TABLE + DbContract.DEVICE_LOCALE.TABLE_NAME + PARENTHESIS_START +
                    DbContract.DEVICE_LOCALE._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.DEVICE_LOCALE.COLUMN_NAME_LOCALE + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.DEVICE_LOCALE.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_USER_PROFILE =
            CREATE_TABLE + DbContract.USER_PROFILE.TABLE_NAME + PARENTHESIS_START +
                    DbContract.USER_PROFILE._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.USER_PROFILE.COLUMN_NAME_NAME + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.USER_PROFILE.COLUMN_NAME_EMAIL + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.USER_PROFILE.COLUMN_NAME_AGE + SP + INT_TYPE + COMMA_SEP +
                    DbContract.USER_PROFILE.COLUMN_NAME_SEX + SP + INT_TYPE + COMMA_SEP +
                    DbContract.USER_PROFILE.COLUMN_NAME_USER_SEGMENT + SP + INT_TYPE + COMMA_SEP +
                    DbContract.USER_PROFILE.COLUMN_NAME_CREATED + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_TOP_5_SIGNALS =
            CREATE_TABLE + DbContract.TOP_5_SIGNALS.TABLE_NAME + PARENTHESIS_START +
                    DbContract.TOP_5_SIGNALS._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS + SP + TEXT_TYPE + COMMA_SEP +
                    DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE + SP + INT_TYPE + PARENTHESIS_STOP;

    private static final String SQL_CREATE_DAILY_STAT_SUMMARY =
            CREATE_TABLE + DbContract.DAILY_STAT_SUMMARY.TABLE_NAME + PARENTHESIS_START +
                    DbContract.DAILY_STAT_SUMMARY._ID + SP + INT_TYPE + PRIMARY_KEY + COMMA_SEP +
                    DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_DBM + SP + INT_TYPE + COMMA_SEP +
                    DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_NUMBER_OF_NEW_SOURCES + SP + INT_TYPE + COMMA_SEP +
                    DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE + SP + INT_TYPE + PARENTHESIS_STOP;

    // Define the DROP Table for each table
    private static final String SQL_DROP_GSM =
            DROP_TABLE_IF_EXISTS + DbContract.GSM.TABLE_NAME;

    private static final String SQL_DROP_WCDMA =
            DROP_TABLE_IF_EXISTS + DbContract.WCDMA.TABLE_NAME;

    private static final String SQL_DROP_LTE =
            DROP_TABLE_IF_EXISTS + DbContract.LTE.TABLE_NAME;

    private static final String SQL_DROP_NEW_RADIO =
            DROP_TABLE_IF_EXISTS + DbContract.NEW_RADIO.TABLE_NAME;

    private static final String SQL_DROP_CDMA =
            DROP_TABLE_IF_EXISTS + DbContract.CDMA.TABLE_NAME;

    private static final String SQL_DROP_WIFI =
            DROP_TABLE_IF_EXISTS + DbContract.WIFI.TABLE_NAME;

    private static final String SQL_DROP_BLUETOOTH =
            DROP_TABLE_IF_EXISTS + DbContract.BLUETOOTH.TABLE_NAME;

    private static final String SQL_DROP_OPERATORS =
            DROP_TABLE_IF_EXISTS + DbContract.OPERATORS.TABLE_NAME;

    private static final String SQL_DROP_ORIENTATION =
            DROP_TABLE_IF_EXISTS + DbContract.ORIENTATION.TABLE_NAME;

    private static final String SQL_DROP_DEVICE_INFO =
            DROP_TABLE_IF_EXISTS + DbContract.DEVICE_INFO.TABLE_NAME;

    private static final String SQL_DROP_OS_INFO =
            DROP_TABLE_IF_EXISTS + DbContract.OS_INFO.TABLE_NAME;

    private static final String SQL_DROP_SIM_INFO =
            DROP_TABLE_IF_EXISTS + DbContract.SIM_INFO.TABLE_NAME;

    private static final String SQL_DROP_APP_VERSION =
            DROP_TABLE_IF_EXISTS + DbContract.APP_VERSION.TABLE_NAME;

    private static final String SQL_DROP_PROFILE_ID =
            DROP_TABLE_IF_EXISTS + DbContract.PROFILE_ID.TABLE_NAME;

    private static final String SQL_DROP_WHITE_ZONE =
            DROP_TABLE_IF_EXISTS + DbContract.WHITE_ZONE.TABLE_NAME;

    private static final String SQL_DROP_EVENT =
            DROP_TABLE_IF_EXISTS + DbContract.EVENT.TABLE_NAME;

    private static final String SQL_DROP_DEVICE_LOCALE =
            DROP_TABLE_IF_EXISTS + DbContract.DEVICE_LOCALE.TABLE_NAME;

    private static final String SQL_DROP_USER_PROFILE =
            DROP_TABLE_IF_EXISTS + DbContract.USER_PROFILE.TABLE_NAME;

    private static final String SQL_DROP_DAY_TOP_5_SIGNALS =
            DROP_TABLE_IF_EXISTS + DbContract.TOP_5_SIGNALS.TABLE_NAME;

    private static final String SQL_DROP_DAILY_STAT_SUMMARY =
            DROP_TABLE_IF_EXISTS + DbContract.DAILY_STAT_SUMMARY.TABLE_NAME;

    // Define ALTER commands used to upgrade the database schema
    // UPGRADE BLOCK 1
    private static final String SQL_ALTER_CREATE_CONNECTED_COLUMN_GSM =
            ALTER_TABLE + DbContract.GSM.TABLE_NAME + ADD + DbContract.GSM.COLUMN_NAME_CONNECTED +
                    INT_TYPE + DEFAULT + ZERO;

    private static final String SQL_ALTER_CREATE_CONNECTED_COLUMN_WCDMA =
            ALTER_TABLE + DbContract.WCDMA.TABLE_NAME + ADD + DbContract.WCDMA.COLUMN_NAME_CONNECTED +
                    INT_TYPE + DEFAULT + ZERO;

    private static final String SQL_ALTER_CREATE_CONNECTED_COLUMN_LTE =
            ALTER_TABLE + DbContract.LTE.TABLE_NAME + ADD + DbContract.LTE.COLUMN_NAME_CONNECTED +
                    INT_TYPE + DEFAULT + ZERO;

    private static final String SQL_ALTER_CREATE_CONNECTED_COLUMN_CDMA =
            ALTER_TABLE + DbContract.CDMA.TABLE_NAME + ADD + DbContract.CDMA.COLUMN_NAME_CONNECTED +
                    INT_TYPE + DEFAULT + ZERO;

    private static final String SQL_ALTER_CREATE_CONNECTED_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD + DbContract.WIFI.COLUMN_NAME_CONNECTED +
                    INT_TYPE + DEFAULT + ZERO;

    private static final String SQL_ALTER_CREATE_OPERATOR_FRIENDLY_NAME_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD +
                    DbContract.WIFI.COLUMN_NAME_OPERATOR_FRIENDLY_NAME + TEXT_TYPE + DEFAULT + "\"\"";

    private static final String SQL_ALTER_CREATE_VENUE_NAME_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD +
                    DbContract.WIFI.COLUMN_NAME_VENUE_NAME + TEXT_TYPE + DEFAULT + "\"\"";

    private static final String SQL_ALTER_CREATE_IS_PASSPOINT_NETWORK_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD +
                    DbContract.WIFI.COLUMN_NAME_IS_PASSPOINT_NETWORK + INT_TYPE + DEFAULT +
                    Const.INVALID_IS_PASSPOINT_NETWORK;

    private static final String SQL_ALTER_CREATE_CENTERFREQ0_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD +
                    DbContract.WIFI.COLUMN_NAME_CENTERFREQ0 + INT_TYPE + DEFAULT +
                    Const.INVALID_CENTERFREQ0;

    private static final String SQL_ALTER_CREATE_CENTERFREQ1_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD +
                    DbContract.WIFI.COLUMN_NAME_CENTERFREQ1 + INT_TYPE + DEFAULT +
                    Const.INVALID_CENTERFREQ1;

    private static final String SQL_ALTER_CREATE_CHANNELWIDTH_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD +
                    DbContract.WIFI.COLUMN_NAME_CHANNELWIDTH + INT_TYPE + DEFAULT +
                    Const.INVALID_CHANNELWIDTH;

    // UPGRADE BLOCK 2
    private static final String SQL_ALTER_CREATE_ARFCN_COLUMN_GSM =
            ALTER_TABLE + DbContract.GSM.TABLE_NAME + ADD + DbContract.GSM.COLUMN_NAME_ARFCN +
                    INT_TYPE + DEFAULT + Integer.MAX_VALUE;

    private static final String SQL_ALTER_CREATE_BSIC_COLUMN_GSM =
            ALTER_TABLE + DbContract.GSM.TABLE_NAME + ADD + DbContract.GSM.COLUMN_NAME_BSIC +
                    INT_TYPE + DEFAULT + Integer.MAX_VALUE;

    private static final String SQL_ALTER_CREATE_TIMING_ADVANCE_COLUMN_GSM =
            ALTER_TABLE + DbContract.GSM.TABLE_NAME + ADD + DbContract.GSM.COLUMN_NAME_TIMING_ADVANCE +
                    INT_TYPE + DEFAULT + Integer.MAX_VALUE;

    private static final String SQL_ALTER_CREATE_UARFCN_COLUMN_WCDMA =
            ALTER_TABLE + DbContract.WCDMA.TABLE_NAME + ADD + DbContract.WCDMA.COLUMN_NAME_UARFCN +
                    INT_TYPE + DEFAULT + Integer.MAX_VALUE;

    private static final String SQL_ALTER_CREATE_EARFCN_COLUMN_LTE =
            ALTER_TABLE + DbContract.LTE.TABLE_NAME + ADD + DbContract.LTE.COLUMN_NAME_EARFCN +
                    INT_TYPE + DEFAULT + Integer.MAX_VALUE;

    private static final String SQL_ALTER_CREATE_BANDWIDTH_COLUMN_LTE =
            ALTER_TABLE + DbContract.LTE.TABLE_NAME + ADD + DbContract.LTE.COLUMN_NAME_BANDWIDTH +
                    INT_TYPE + DEFAULT + Integer.MAX_VALUE;

    //Define the DELETE OPERATORS command to clean the OPERATORS table on upgrade
    private static final String SQL_DELETE_OPERATORS =
            DELETE + FROM + DbContract.OPERATORS.TABLE_NAME;

    private static final String SQL_ALTER_CREATE_WIFI_STANDARD_COLUMN_WIFI =
            ALTER_TABLE + DbContract.WIFI.TABLE_NAME + ADD + DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD +
                    INT_TYPE + DEFAULT + Const.INVALID_WIFI_STANDARD;

    private static final String SQL_ALTER_CREATE_ECNO_COLUMN_WCDMA =
            ALTER_TABLE + DbContract.WCDMA.TABLE_NAME + ADD + DbContract.WCDMA.COLUMN_NAME_ECNO +
                    INT_TYPE + DEFAULT + BaseProperty.UNAVAILABLE;

    private static final String SQL_ALTER_CREATE_SOC_MANUFACTURER_COLUMN_DEVICE_INFO =
            ALTER_TABLE + DbContract.DEVICE_INFO.TABLE_NAME + ADD +
                    DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MANUFACTURER + TEXT_TYPE + DEFAULT + "\"\"";

    private static final String SQL_ALTER_CREATE_SOC_MODEL_COLUMN_DEVICE_INFO =
            ALTER_TABLE + DbContract.DEVICE_INFO.TABLE_NAME + ADD +
                    DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MODEL + TEXT_TYPE + DEFAULT + "\"\"";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //----------------------- CREATE DB ENTRIES (BEGIN) --------------------------------------
    public static ContentValues createWifiContentValues(BaseProperty es) {
        ContentValues values = new ContentValues();
        values.put(DbContract.WIFI.COLUMN_NAME_SSID, es.ssid);
        values.put(DbContract.WIFI.COLUMN_NAME_BSSID, Tools.MAC2Long(es.bssid));
        values.put(DbContract.WIFI.COLUMN_NAME_OPERATOR_FRIENDLY_NAME, es.operator_friendly_name);
        values.put(DbContract.WIFI.COLUMN_NAME_VENUE_NAME, es.venue_name);
        values.put(DbContract.WIFI.COLUMN_NAME_IS_PASSPOINT_NETWORK, es.is_passpoint_network);
        values.put(DbContract.WIFI.COLUMN_NAME_FREQ, es.freq);
        values.put(DbContract.WIFI.COLUMN_NAME_CENTERFREQ0, es.center_freq0);
        values.put(DbContract.WIFI.COLUMN_NAME_CENTERFREQ1, es.center_freq1);
        values.put(DbContract.WIFI.COLUMN_NAME_CHANNELWIDTH, es.channel_width);
        values.put(DbContract.WIFI.COLUMN_NAME_CAPABILITIES, es.capabilities);
        values.put(DbContract.WIFI.COLUMN_NAME_WIFI_STANDARD, es.wifiStandard);
        values.put(DbContract.WIFI.COLUMN_NAME_DBM, es.dbm);
        values.put(DbContract.WIFI.COLUMN_NAME_CONNECTED, es.connected ? 1 : 0);
        values.put(DbContract.WIFI.COLUMN_NAME_LATITUDE, es.latitude);
        values.put(DbContract.WIFI.COLUMN_NAME_LONGITUDE, es.longitude);
        values.put(DbContract.WIFI.COLUMN_NAME_CREATED, es.measured_time);
        return values;
    }

    public static ContentValues createBluetoothContentValues(BaseProperty es) {
        ContentValues values = new ContentValues();
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_NAME, es.bt_device_name);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS, es.bt_device_name_alias);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS, Tools.MAC2Long(es.bt_address));
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS, es.bt_device_class);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE, es.bt_device_type);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE, es.bt_bond_state);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_DBM, es.dbm);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_LATITUDE, es.latitude);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_LONGITUDE, es.longitude);
        values.put(DbContract.BLUETOOTH.COLUMN_NAME_CREATED, es.measured_time);
        return values;
    }

    public static ContentValues createGsmContentValues(BaseProperty es) {
        ContentValues values = new ContentValues();
        values.put(DbContract.GSM.COLUMN_NAME_TYPE, es.type);
        values.put(DbContract.GSM.COLUMN_NAME_MNC, es.mnc);
        values.put(DbContract.GSM.COLUMN_NAME_MCC, es.mcc);
        values.put(DbContract.GSM.COLUMN_NAME_CID, es.cid);
        values.put(DbContract.GSM.COLUMN_NAME_LAC, es.lac);
        values.put(DbContract.GSM.COLUMN_NAME_ARFCN, es.arfcn);
        values.put(DbContract.GSM.COLUMN_NAME_BSIC, es.bsic);
        values.put(DbContract.GSM.COLUMN_NAME_TIMING_ADVANCE, es.timing_advance);
        values.put(DbContract.GSM.COLUMN_NAME_DBM, es.dbm);
        values.put(DbContract.GSM.COLUMN_NAME_BER, es.ber);
        values.put(DbContract.GSM.COLUMN_NAME_CONNECTED, es.connected ? 1 : 0);
        values.put(DbContract.GSM.COLUMN_NAME_LATITUDE, es.latitude);
        values.put(DbContract.GSM.COLUMN_NAME_LONGITUDE, es.longitude);
        values.put(DbContract.GSM.COLUMN_NAME_CREATED, es.measured_time);
        return values;
    }

    public static ContentValues createWcdmaContentValues(BaseProperty es) {
        ContentValues values = new ContentValues();
        values.put(DbContract.WCDMA.COLUMN_NAME_TYPE, es.type);
        values.put(DbContract.WCDMA.COLUMN_NAME_MNC, es.mnc);
        values.put(DbContract.WCDMA.COLUMN_NAME_MCC, es.mcc);
        values.put(DbContract.WCDMA.COLUMN_NAME_UCID, es.ucid);
        values.put(DbContract.WCDMA.COLUMN_NAME_LAC, es.lac);
        values.put(DbContract.WCDMA.COLUMN_NAME_PSC, es.psc);
        values.put(DbContract.WCDMA.COLUMN_NAME_UARFCN, es.uarfcn);
        values.put(DbContract.WCDMA.COLUMN_NAME_DBM, es.dbm);
        values.put(DbContract.WCDMA.COLUMN_NAME_BER, es.ber);
        values.put(DbContract.WCDMA.COLUMN_NAME_ECNO, es.ecno);
        values.put(DbContract.WCDMA.COLUMN_NAME_CONNECTED, es.connected ? 1 : 0);
        values.put(DbContract.WCDMA.COLUMN_NAME_LATITUDE, es.latitude);
        values.put(DbContract.WCDMA.COLUMN_NAME_LONGITUDE, es.longitude);
        values.put(DbContract.WCDMA.COLUMN_NAME_CREATED, es.measured_time);
        return values;
    }

    public static ContentValues createLteContentValues(BaseProperty es) {
        ContentValues values = new ContentValues();
        values.put(DbContract.LTE.COLUMN_NAME_TYPE, es.type);
        values.put(DbContract.LTE.COLUMN_NAME_MNC, es.mnc);
        values.put(DbContract.LTE.COLUMN_NAME_MCC, es.mcc);
        values.put(DbContract.LTE.COLUMN_NAME_ECI, es.eci);
        values.put(DbContract.LTE.COLUMN_NAME_PCI, es.pci);
        values.put(DbContract.LTE.COLUMN_NAME_TAC, es.tac);
        values.put(DbContract.LTE.COLUMN_NAME_EARFCN, es.earfcn);
        values.put(DbContract.LTE.COLUMN_NAME_BANDWIDTH, es.bandwidth);
        values.put(DbContract.LTE.COLUMN_NAME_TIMING_ADVANCE, es.timing_advance);
        values.put(DbContract.LTE.COLUMN_NAME_RSRP, es.lte_rsrp);
        values.put(DbContract.LTE.COLUMN_NAME_RSSI, es.lte_rssi);
        values.put(DbContract.LTE.COLUMN_NAME_RSRQ, es.lte_rsrq);
        values.put(DbContract.LTE.COLUMN_NAME_RSSNR, es.lte_rssnr);
        values.put(DbContract.LTE.COLUMN_NAME_CQI, es.lte_cqi);
        values.put(DbContract.LTE.COLUMN_NAME_CONNECTED, es.connected ? 1 : 0);
        values.put(DbContract.LTE.COLUMN_NAME_LATITUDE, es.latitude);
        values.put(DbContract.LTE.COLUMN_NAME_LONGITUDE, es.longitude);
        values.put(DbContract.LTE.COLUMN_NAME_CREATED, es.measured_time);
        return values;
    }

    public static ContentValues createNewRadioContentValues(BaseProperty es) {
        ContentValues values = new ContentValues();
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_TYPE, es.type);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_MCC, es.mcc);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_MNC, es.mnc);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_NCI, es.nci);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN, es.nrarfcn);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_PCI, es.pci);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_TAC, es.tac);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP, es.csiRsrp);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRQ, es.csiRsrq);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_CSI_SINR, es.csiSinr);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRP, es.ssRsrp);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRQ, es.ssRsrq);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_SS_SINR, es.ssSinr);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED, es.connected ? 1 : 0);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_LATITUDE, es.latitude);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_LONGITUDE, es.longitude);
        values.put(DbContract.NEW_RADIO.COLUMN_NAME_CREATED, es.measured_time);
        return values;
    }

    public static ContentValues createCdmaContentValues(BaseProperty es) {
        ContentValues values = new ContentValues();
        values.put(DbContract.CDMA.COLUMN_NAME_TYPE, es.type);
        values.put(DbContract.CDMA.COLUMN_NAME_BASE_STATION_ID, es.base_station_id);
        values.put(DbContract.CDMA.COLUMN_NAME_NETWORK_ID, es.network_id);
        values.put(DbContract.CDMA.COLUMN_NAME_SYSTEM_ID, es.system_id);
        values.put(DbContract.CDMA.COLUMN_NAME_STATION_LATITUDE, es.cdma_latitude);
        values.put(DbContract.CDMA.COLUMN_NAME_STATION_LONGITUDE, es.cdma_longitude);
        values.put(DbContract.CDMA.COLUMN_NAME_CDMA_DBM, es.cdma_dbm);
        values.put(DbContract.CDMA.COLUMN_NAME_CDMA_ECIO, es.cdma_ecio);
        values.put(DbContract.CDMA.COLUMN_NAME_EVDO_DBM, es.evdo_dbm);
        values.put(DbContract.CDMA.COLUMN_NAME_EVDO_ECIO, es.evdo_ecio);
        values.put(DbContract.CDMA.COLUMN_NAME_EVDO_SNR, es.evdo_snr);
        values.put(DbContract.CDMA.COLUMN_NAME_CONNECTED, es.connected ? 1 : 0);
        values.put(DbContract.CDMA.COLUMN_NAME_LATITUDE, es.latitude);
        values.put(DbContract.CDMA.COLUMN_NAME_LONGITUDE, es.longitude);
        values.put(DbContract.CDMA.COLUMN_NAME_CREATED, es.measured_time);
        return values;
    }

    static ContentValues createOperatorsContentValues(int mcc, int mnc,
                                                      String operator_name) {
        ContentValues values = new ContentValues();
        values.put(DbContract.OPERATORS.COLUMN_NAME_MCC, mcc);
        values.put(DbContract.OPERATORS.COLUMN_NAME_MNC, mnc);
        values.put(DbContract.OPERATORS.COLUMN_NAME_OPERATOR_NAME, operator_name);
        return values;
    }

    public static ContentValues createOrientationContentValues(float x, float y, float z,
                                                               float angle, float accuracy,
                                                               long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.ORIENTATION.COLUMN_NAME_X, x);
        values.put(DbContract.ORIENTATION.COLUMN_NAME_Y, y);
        values.put(DbContract.ORIENTATION.COLUMN_NAME_Z, z);
        values.put(DbContract.ORIENTATION.COLUMN_NAME_ANGLE, angle);
        values.put(DbContract.ORIENTATION.COLUMN_NAME_ACCURACY, accuracy);
        values.put(DbContract.ORIENTATION.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createDeviceInfoContentValues(String brand,
                                                       String device,
                                                       String hardware,
                                                       String manufacturer,
                                                       String model,
                                                       String product,
                                                       String soc_manufacturer,
                                                       String soc_model,
                                                       long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_BRAND, brand);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_DEVICE, device);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_HARDWARE, hardware);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_MANUFACTURER, manufacturer);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_MODEL, model);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_PRODUCT, product);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MANUFACTURER, soc_manufacturer);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_SOC_MODEL, soc_model);
        values.put(DbContract.DEVICE_INFO.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createOSInfoContentValues(String release,
                                                   int sdk_int,
                                                   long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.OS_INFO.COLUMN_NAME_RELEASE, release);
        values.put(DbContract.OS_INFO.COLUMN_NAME_SDK_INT, sdk_int);
        values.put(DbContract.OS_INFO.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createSIMInfoContentValues(String sim_country_code,
                                                    String sim_operator,
                                                    String sim_operator_name,
                                                    long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.SIM_INFO.COLUMN_NAME_SIM_COUNTRY_CODE, sim_country_code);
        values.put(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR, sim_operator);
        values.put(DbContract.SIM_INFO.COLUMN_NAME_SIM_OPERATOR_NAME, sim_operator_name);
        values.put(DbContract.SIM_INFO.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createAppVersionContentValues(int version,
                                                       long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.APP_VERSION.COLUMN_NAME_VERSION, version);
        values.put(DbContract.APP_VERSION.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createDeviceLocaleContentValues(String device_locale,
                                                         long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.DEVICE_LOCALE.COLUMN_NAME_LOCALE, device_locale);
        values.put(DbContract.DEVICE_LOCALE.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createProfileIdContentValues(String id) {
        ContentValues values = new ContentValues();
        values.put(DbContract.PROFILE_ID.COLUMN_NAME_ID, id);
        return values;
    }

    static ContentValues createWhiteZoneContentValues(int signal_type,
                                                      double latitude,
                                                      double longitude,
                                                      long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.WHITE_ZONE.COLUMN_NAME_SIGNAL_TYPE, signal_type);
        values.put(DbContract.WHITE_ZONE.COLUMN_NAME_LATITUDE, latitude);
        values.put(DbContract.WHITE_ZONE.COLUMN_NAME_LONGITUDE, longitude);
        values.put(DbContract.WHITE_ZONE.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createEventContentValues(int event_type, long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.EVENT.COLUMN_NAME_EVENT_TYPE, event_type);
        values.put(DbContract.EVENT.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    static ContentValues createTop5SignalsContentValues(String top5SignalsJsonString, long timeMillis) {
        ContentValues values = new ContentValues();
        values.put(DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS, top5SignalsJsonString);
        values.put(DbContract.TOP_5_SIGNALS.COLUMN_NAME_SIGNALS_DATE, timeMillis);
        return values;
    }

    static ContentValues createDailyStatSummaryContentValues(int dbm, int numberOfNewSources, long summaryDateTimeMillis) {
        ContentValues values = new ContentValues();
        values.put(DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_DBM, dbm);
        values.put(DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_NUMBER_OF_NEW_SOURCES, numberOfNewSources);
        values.put(DbContract.DAILY_STAT_SUMMARY.COLUMN_NAME_SUMMARY_DATE, summaryDateTimeMillis);
        return values;
    }

    static ContentValues createUserProfileContentValues(String name, String email, int sex, int age,
                                                        int userSegment, long currentTime) {
        ContentValues values = new ContentValues();
        values.put(DbContract.USER_PROFILE.COLUMN_NAME_NAME, name);
        values.put(DbContract.USER_PROFILE.COLUMN_NAME_EMAIL, email);
        values.put(DbContract.USER_PROFILE.COLUMN_NAME_SEX, sex);
        values.put(DbContract.USER_PROFILE.COLUMN_NAME_AGE, age);
        values.put(DbContract.USER_PROFILE.COLUMN_NAME_USER_SEGMENT, userSegment);
        values.put(DbContract.USER_PROFILE.COLUMN_NAME_CREATED, currentTime);
        return values;
    }

    //----------------------- CREATE DB ENTRIES (END) ----------------------------------------

    public static void dropAllTables(SQLiteDatabase db) {
        db.execSQL(SQL_DROP_GSM);
        db.execSQL(SQL_DROP_WCDMA);
        db.execSQL(SQL_DROP_LTE);
        db.execSQL(SQL_DROP_NEW_RADIO);
        db.execSQL(SQL_DROP_CDMA);
        db.execSQL(SQL_DROP_WIFI);
        db.execSQL(SQL_DROP_BLUETOOTH);
        db.execSQL(SQL_DROP_OPERATORS);
        db.execSQL(SQL_DROP_ORIENTATION);
        db.execSQL(SQL_DROP_DEVICE_INFO);
        db.execSQL(SQL_DROP_OS_INFO);
        db.execSQL(SQL_DROP_SIM_INFO);
        db.execSQL(SQL_DROP_APP_VERSION);
        db.execSQL(SQL_DROP_PROFILE_ID);
        db.execSQL(SQL_DROP_WHITE_ZONE);
        db.execSQL(SQL_DROP_EVENT);
        db.execSQL(SQL_DROP_DEVICE_LOCALE);
        db.execSQL(SQL_DROP_USER_PROFILE);
        db.execSQL(SQL_DROP_DAY_TOP_5_SIGNALS);
        db.execSQL(SQL_DROP_DAILY_STAT_SUMMARY);
    }

    /**
     * This method is called the first time the DB is created. If the DB already
     * exist when we start the application, this method is no more called. Only
     * onOpen() will be called.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "in onCreate");
        db.execSQL(SQL_CREATE_GSM);
        db.execSQL(SQL_CREATE_WCDMA);
        db.execSQL(SQL_CREATE_LTE);
        db.execSQL(SQL_CREATE_NEW_RADIO);
        db.execSQL(SQL_CREATE_CDMA);
        db.execSQL(SQL_CREATE_WIFI);
        db.execSQL(SQL_CREATE_BLUETOOTH);
        db.execSQL(SQL_CREATE_OPERATORS);
        db.execSQL(SQL_CREATE_ORIENTATION);
        db.execSQL(SQL_CREATE_DEVICE_INFO);
        db.execSQL(SQL_CREATE_OS_INFO);
        db.execSQL(SQL_CREATE_SIM_INFO);
        db.execSQL(SQL_CREATE_APP_VERSION);
        db.execSQL(SQL_CREATE_PROFILE_ID);
        db.execSQL(SQL_CREATE_WHITE_ZONE);
        db.execSQL(SQL_CREATE_EVENT);
        db.execSQL(SQL_CREATE_DEVICE_LOCALE);
        db.execSQL(SQL_CREATE_USER_PROFILE);
        db.execSQL(SQL_CREATE_TOP_5_SIGNALS);
        db.execSQL(SQL_CREATE_DAILY_STAT_SUMMARY);
        Log.d(TAG, "onCreate done !");

        // we create the table containing the mapping between an mcc/mnc and an operator name
        DbRequestHandler.createMccMncToOperatorTable(db);
    }

    /**
     * This method is called each time the DB is opened.
     *
     * @param db the database that is opened
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        Log.d(TAG, "in onOpen");
    }

    /**
     * This method is called each time the DB is opened and must be upgraded.
     *
     * @param db         the DB object
     * @param oldVersion previous version of the DB
     * @param newVersion version of the DB to upgrade to
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: from oldVersion " + oldVersion + " to newVersion " + newVersion);
        // upgrade process to version 2 of the DB
        if (oldVersion == 1 && newVersion >= 2) {
            Log.d(TAG, "onUpgrade: upgrading from 1 to a version >=2");
            Log.d(TAG, "onUpgrade: creating the table SQL_CREATE_WHITE_ZONE");
            db.execSQL(SQL_CREATE_WHITE_ZONE);
            Log.d(TAG, "onUpgrade: add the connected columns to all cellular tables");
            db.execSQL(SQL_ALTER_CREATE_CONNECTED_COLUMN_GSM);
            db.execSQL(SQL_ALTER_CREATE_CONNECTED_COLUMN_WCDMA);
            db.execSQL(SQL_ALTER_CREATE_CONNECTED_COLUMN_LTE);
            db.execSQL(SQL_ALTER_CREATE_CONNECTED_COLUMN_CDMA);
            db.execSQL(SQL_ALTER_CREATE_CONNECTED_COLUMN_WIFI);
            Log.d(TAG, "onUpgrade: add the 6 new columns to the WIFI table");
            db.execSQL(SQL_ALTER_CREATE_OPERATOR_FRIENDLY_NAME_COLUMN_WIFI);
            db.execSQL(SQL_ALTER_CREATE_VENUE_NAME_COLUMN_WIFI);
            db.execSQL(SQL_ALTER_CREATE_IS_PASSPOINT_NETWORK_COLUMN_WIFI);
            db.execSQL(SQL_ALTER_CREATE_CENTERFREQ0_COLUMN_WIFI);
            db.execSQL(SQL_ALTER_CREATE_CENTERFREQ1_COLUMN_WIFI);
            db.execSQL(SQL_ALTER_CREATE_CHANNELWIDTH_COLUMN_WIFI);
            Log.d(TAG, "onUpgrade: done (part 1)!");
        }

        // upgrade process to version 3 of the DB, update of the mnc.txt asset file
        if (oldVersion <= 2 && newVersion >= 3) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 2 to a version >=3");
            Log.d(TAG, "onUpgrade: deleting all the operators rows...");
            db.execSQL(SQL_DELETE_OPERATORS);
            Log.d(TAG, "onUpgrade: done!");
            // we create the table containing the mapping between an mcc/mnc and an operator name
            DbRequestHandler.createMccMncToOperatorTable(db);
            Log.d(TAG, "onUpgrade: done (part 2)!");
        }

        // upgrade process to version 4 of the DB
        if (oldVersion <= 3 && newVersion >= 4) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 3 to a version >=4");
            Log.d(TAG, "onUpgrade: add columns to the GSM, WCDMA, and LTE tables");
            db.execSQL(SQL_ALTER_CREATE_ARFCN_COLUMN_GSM);
            db.execSQL(SQL_ALTER_CREATE_BSIC_COLUMN_GSM);
            db.execSQL(SQL_ALTER_CREATE_TIMING_ADVANCE_COLUMN_GSM);
            db.execSQL(SQL_ALTER_CREATE_UARFCN_COLUMN_WCDMA);
            db.execSQL(SQL_ALTER_CREATE_EARFCN_COLUMN_LTE);
            Log.d(TAG, "onUpgrade: done (part 3)!");
        }

        // upgrade process to version 5 of the DB, update of the mnc.txt asset file
        if (oldVersion <= 4 && newVersion >= 5) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 4 to a version >=5");
            Log.d(TAG, "onUpgrade: deleting all the operators rows...");
            db.execSQL(SQL_DELETE_OPERATORS);
            Log.d(TAG, "onUpgrade: done!");
            // we create the table containing the mapping between an mcc/mnc and an operator name
            DbRequestHandler.createMccMncToOperatorTable(db);
            Log.d(TAG, "onUpgrade: done (part 4)!");
        }

        // upgrade process to version 6 of the DB, update of the mnc.txt asset file
        if (oldVersion <= 5 && newVersion >= 6) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 5 to a version >=6");
            Log.d(TAG, "onUpgrade: deleting all the operators rows...");
            db.execSQL(SQL_DELETE_OPERATORS);
            Log.d(TAG, "onUpgrade: done!");
            // we create the table containing the mapping between an mcc/mnc and an operator name
            DbRequestHandler.createMccMncToOperatorTable(db);
            Log.d(TAG, "onUpgrade: done (part 5)!");
        }

        // upgrade process to version 7 of the DB, add event table for instrumentation and save the
        // current instrumentation parameter values
        if (oldVersion <= 6 && newVersion >= 7) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 6 to a version >=7");
            Log.d(TAG, "onUpgrade: adding new table `event`");
            db.execSQL(SQL_CREATE_EVENT);
            Log.d(TAG, "onUpgrade: done (part 6)!");
        }

        // upgrade process to version 8 of the DB, add user profile table for onboarding
        if (oldVersion <= 7 && newVersion >= 8) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 7 to a version >=8");
            Log.d(TAG, "onUpgrade: adding new table `user_profile`");
            db.execSQL(SQL_CREATE_USER_PROFILE);
            Log.d(TAG, "onUpgrade: done (part 7)!");
        }

        // upgrade process to version 9 of the DB, add device_locale table
        if (oldVersion <= 8 && newVersion >= 9) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 8 to a version >=9");
            Log.d(TAG, "onUpgrade: adding new table `device_locale`");
            db.execSQL(SQL_CREATE_DEVICE_LOCALE);
            Log.d(TAG, "onUpgrade: done (part 8)!");
        }

        // upgrade process to version 10 of the DB, add day_top_5_signals table
        if (oldVersion <= 9 && newVersion >= 10) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 9 to a version >=10");
            Log.d(TAG, "onUpgrade: adding new table `day_top_5_signals`");
            db.execSQL(SQL_CREATE_TOP_5_SIGNALS);
            Log.d(TAG, "onUpgrade: done (part 9)!");
        }

        // upgrade process to version 11 of the DB, add daily_stat_summary table
        if (oldVersion <= 10 && newVersion >= 11) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 10 to a version >=11");
            Log.d(TAG, "onUpgrade: adding new table `daily_stat_summary`");
            db.execSQL(SQL_CREATE_DAILY_STAT_SUMMARY);
            Log.d(TAG, "onUpgrade: done (part 10)!");
        }

        // upgrade process to version 12 of the DB, add newradio table and update lte to include
        // bandwidth and rssi columns
        if (oldVersion <= 11 && newVersion >= 12) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 11 to a version >=12");
            Log.d(TAG, "onUpgrade: adding new table `NewRadio`");
            db.execSQL(SQL_CREATE_NEW_RADIO);
            Log.d(TAG, "onUpgrade: adding the bandwidth columns to LTE table");
            db.execSQL(SQL_ALTER_CREATE_BANDWIDTH_COLUMN_LTE);
            Log.d(TAG, "onUpgrade: done (part 11)!");
        }

        // upgrade process to version 13 of the DB, add the wifiStandard column in the wifi table
        if (oldVersion <= 12 && newVersion >= 13) {
            Log.d(TAG, "onUpgrade: upgrading from version <= 12 to a version >=13");
            Log.d(TAG, "onUpgrade: adding wifiStandard column in the wifi table");
            db.execSQL(SQL_ALTER_CREATE_WIFI_STANDARD_COLUMN_WIFI);
            Log.d(TAG, "onUpgrade: adding ecno column in the wcdma table");
            db.execSQL(SQL_ALTER_CREATE_ECNO_COLUMN_WCDMA);
            Log.d(TAG, "onUpgrade: adding soc_manufacturer in the device_info table");
            db.execSQL(SQL_ALTER_CREATE_SOC_MANUFACTURER_COLUMN_DEVICE_INFO);
            Log.d(TAG, "onUpgrade: adding soc_model in the device_info table");
            db.execSQL(SQL_ALTER_CREATE_SOC_MODEL_COLUMN_DEVICE_INFO);
            Log.d(TAG, "onUpgrade: done (part 12)!");
        }
    }
}

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

import android.provider.BaseColumns;

/**
 * This class defines the tables name and all the tables columns.
 * We inherit from BaseColumns to automatically have the _ID field used as the primary key
 * <p>
 * <p>
 * CHECK LIST WHEN WE ADD A NEW TABLE
 * 0) increase the DB version in {@link DbHelper}
 * 1) define the table contract in DbContract
 * 2) write the SQL create table (e.g. SQL_CREATE_WHITE_ZONE) and
 * delete table (e.g. SQL_DELETE_WHITE_ZONE) strings request in DbHelper
 * 3) implement the createXXXContentValues() method in DbHelper to add rows to the DB for the given
 * new table
 * 4) add the statements defined in 2) in the dropAllTables() and onCreate() methods in DbHelper
 * 5) implement the DB upgrade to deal with the new table in onUpgrade(), and don't forget in that
 * case to increase DATABASE_VERSION in DbHelper
 * 6) add the code in deleteOldestDbEntries() in DbRequestHandler we remove from the new table old
 * entries
 * 7) implement a dumpXXX2json to dump the new table content into JSON in DbRequestHandler
 * 8) then update getJsonForSync() and getNbRowsForAllTables() accordingly
 * 9) update Tools.getBasePropertyFromJsonString()
 * 10) If we need to retrieve values from the DB in the app, implement a getXXXData()
 * in DbRequestHandler
 */
public class DbContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DbContract() {
    }

    /*
    IMPORTANT NOTE ON THE RANGE OF VALUES
    The range of the different values collected is not always clearly described in the documentation.
    However, all Android devices must pass the CTS (compatibility test suite,
    https://source.android.com/compatibility/cts/)

    In this CTS we find tests for specific value ranges, which is a good indication of the range
    of values permitted. For all cell info data, the CTS test is here
    https://android.googlesource.com/platform/cts/+/master/tests/tests/telephony/src/android/telephony/cts/CellInfoTest.java

    ETSI TS 127 007 V10.3.0 (2011-04) section 8.69 see
    http://www.etsi.org/deliver/etsi_ts/127000_127099/127007/10.03.00_60/ts_127007v100300p.pdf
    gives important information on the range of the signal indicators on the terminal (2G, 3G, 4G)
    rssi:  integer type [0, 31] + 99
           ranges from 0 (-113dBm or less) to 31 (-51dBm or greater).
           99 if not known or not detectable
           As an RSSI of 0 and 31 do not represent a single value but a range of values, we can
           expect a larger number of -113 and -51 measurements than other dBm values

    ber:   integer type; channel bit error rate (in percent)
           in [0, 7] and 99 if not known or not detectable

    rscp:  integer type, received signal code power (see 3GPP TS 25.133 [95] and 3GPP TS 25.123 [96]).
           ranges from 0 (-120 dBm or less) to 96 (-24 dBm or greater)
           255 not known or not detectable

    rsrp:  integer type, reference signal received power (see 3GPP TS 36.133 [96]).
           ranges from 0 (-140 dBm or less) to 97 (-43 dBm or greater)
           255 not known or not detectable

    rsrq:  integer type, reference signal received quality (see 3GPP TS 36.133 [96]).
           ranges from 0 (-19,5 dBm or less) to 34 (-2,5 dBm or greater), 0.5 dBm resolution,
           255 not known or not detectable

    ecno:  integer type, Ec/No (see 3GPP TS 25.133 [95]).
           ranges from 0 (-24 dBm or less) to 49 (0,5 dBm or greater), 0.5 dBm resolution,
           255 not known or not detectable
    */

    public static abstract class GSM implements BaseColumns {
        public static final String TABLE_NAME = "gsm";
        static final String COLUMN_NAME_TYPE = "type";                          // int (range [0,18]) defined in TelephonyManager.java
        static final String COLUMN_NAME_MNC = "mnc";                            // int (range [0,999] can be Integer.MAX_VALUE if not specified) defined in CellIdentityGsm.java
        static final String COLUMN_NAME_MCC = "mcc";                            // int (range [0,999] can be Integer.MAX_VALUE if not specified) defined in CellIdentityGsm.java
        static final String COLUMN_NAME_CID = "cid";                            // int (range [0,65535] can be Integer.MAX_VALUE if not specified) defined in CellIdentityGsm.java
        static final String COLUMN_NAME_LAC = "lac";                            // int (range [0,65535] can be Integer.MAX_VALUE if not specified) defined in CellIdentityGsm.java
        static final String COLUMN_NAME_ARFCN = "arfcn";                        // int (16-bit GSM Absolute RF Channel Number, Integer.MAX_VALUE if unknown) defined in CellIdentityGsm.java. ARFCN: absolute radio-frequency channel number
        static final String COLUMN_NAME_BSIC = "bsic";                          // int (6-bit Base Station Identity Code, Integer.MAX_VALUE if unknown) defined in CellIdentityGsm.java. BSIC: Base station identity code
        static final String COLUMN_NAME_TIMING_ADVANCE = "timing_advance";      // int (in [0, 219], and Integer.MAX_VALUE if not specified. Refer to 3GPP 45.010 Sec 5.8r) defined in CellSignalStrengthLte.java
        static final String COLUMN_NAME_DBM = "dbm";                            // int in dBm (range [-113, -51] and Integer.MAX_VALUE if not specified) defined in CellSignalStrengthGsm.java (standard TS 27.007 8.5)
        static final String COLUMN_NAME_BER = "ber";                            // int (range [0,7] and 99 if not specified, but initialized at Integer.MAX_VALUE) defined in CellSignalStrengthGsm.java (standard TS 27.007 8.5)
        static final String COLUMN_NAME_CONNECTED = "connected";                // boolean
        static final String COLUMN_NAME_LATITUDE = "latitude";                  // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude";                // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";                    // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    public static abstract class WCDMA implements BaseColumns {
        public static final String TABLE_NAME = "wcdma";
        static final String COLUMN_NAME_TYPE = "type";           // int (range [0,18]) defined in TelephonyManager.java
        static final String COLUMN_NAME_MNC = "mnc";             // int (range [0,999] can be Integer.MAX_VALUE if not specified) defined in CellIdentityWcdma.java
        static final String COLUMN_NAME_MCC = "mcc";             // int (range [0,999] can be Integer.MAX_VALUE if not specified) defined in CellIdentityWcdma.java
        static final String COLUMN_NAME_UCID = "cid";            // int (range [0,268435455] (2^28 − 1) can be Integer.MAX_VALUE if not specified) defined in CellIdentityWcdma.java, UTRAN Cell ID (also called LCID)
        static final String COLUMN_NAME_LAC = "lac";             // int (range [0,65535] can be Integer.MAX_VALUE if not specified) defined in CellIdentityWcdma.java
        static final String COLUMN_NAME_PSC = "psc";             // int (range [0,511] can be Integer.MAX_VALUE if not specified) defined in CellIdentityWcdma.java
        static final String COLUMN_NAME_UARFCN = "uarfcn";       // int (16-bit UMTS Absolute RF Channel Number, Integer.MAX_VALUE if unknown) defined in CellIdentityWcdma.java. UARFCN (abbreviation for UTRA Absolute Radio Frequency Channel Number, where UTRA stands for UMTS Terrestrial Radio Access)
        static final String COLUMN_NAME_DBM = "dbm";             // int (range [-113, -51] and Integer.MAX_VALUE if not specified) defined in CellSignalStrengthWcdma.java (standard TS 27.007 8.5)
        static final String COLUMN_NAME_BER = "ber";             // int (range [0,7] and 99 if not specified, but initialized at Integer.MAX_VALUE) defined in CellSignalStrengthWcdma.java (standard TS 27.007 8.5)
        static final String COLUMN_NAME_ECNO = "ecno";           // int (range [-24, 1] or UNAVAILABLE
        static final String COLUMN_NAME_CONNECTED = "connected"; // boolean
        static final String COLUMN_NAME_LATITUDE = "latitude";   // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude"; // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";     // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    public static abstract class LTE implements BaseColumns {
        public static final String TABLE_NAME = "lte";
        static final String COLUMN_NAME_TYPE = "type";                       // int (range [0,18]) defined in TelephonyManager.java
        static final String COLUMN_NAME_MNC = "mnc";                         // int (range [0,999] can be Integer.MAX_VALUE if not specified) defined in CellIdentityLte.java
        static final String COLUMN_NAME_MCC = "mcc";                         // int (range [0,999] can be Integer.MAX_VALUE if not specified) defined in CellIdentityLte.java
        static final String COLUMN_NAME_ECI = "ci";                          // int (range [0,268435455] can be Integer.MAX_VALUE if not specified) defined in CellIdentityLte.java
        static final String COLUMN_NAME_PCI = "pci";                         // int (range [0,503] can be Integer.MAX_VALUE if not specified) defined in CellIdentityLte.java
        static final String COLUMN_NAME_TAC = "tac";                         // int (range [0,65535] can be Integer.MAX_VALUE if not specified) defined in CellIdentityLte.java
        static final String COLUMN_NAME_EARFCN = "earfcn";                   // int (18-bit Absolute RF Channel Number, Integer.MAX_VALUE if unknown) defined in CellIdentityLte.java. EARFCN: EUTRA Absolute radio-frequency channel number
        static final String COLUMN_NAME_BANDWIDTH = "bandwidth";             // int (cell bandwidth in kHz, CellInfo#UNAVAILABLE if unknown)
        static final String COLUMN_NAME_TIMING_ADVANCE = "timing_advance";   // int ([0, 1282] according to CTS, can go up to Integer.MAX_VALUE) defined in CellSignalStrengthLte.java, but in ETSI TS 136 321 V11.2.0 (2013-04) section 6.1.3.5 page 39, the TA is on 6 bits
        static final String COLUMN_NAME_RSRP = "dbm";                        // int  RSRP in dBm(in [-140, -44] according to CTS), can go up to Integer.MAX_VALUE) defined in CellSignalStrengthLte.java
        static final String COLUMN_NAME_RSSI = "ss";                         // int RSSI in ASU in [0, 99] ([0, 31] and 99 when invalid). Reference: TS 27.007 8.5 Signal quality +CSQ). **IMPORTANT NOTE**: In API 29 (android Q), the SignalStrength field (in ASU) has been deprecated and replaced by the RSSI field (in dBm). For backward compatibility, we keep the ss field and record all rssi converted to ASU values for API >= 29 in that field. Before API 29 (android Q) this field was directly an ASU.
        static final String COLUMN_NAME_RSRQ = "rsrq";                       // int ([-20, -3] dB can go up to Integer.MAX_VALUE if unavailable) defined in CellSignalStrengthLte.java, the spec. ETSI TS 127 007 V10.3.0 (2011-04) section 8.69  specifies a range [-19.5, -2.5]
        static final String COLUMN_NAME_RSSNR = "rssnr";                     // int ([-200, 300] can go up to Integer.MAX_VALUE if unavailable) defined in CellSignalStrengthLte.java
        static final String COLUMN_NAME_CQI = "cqi";                         // int ([0, 15] can go up to Integer.MAX_VALUE if unavailable) defined in CellSignalStrengthLte.java, note that for LTE the CQI in on 4 bits, so it is in [0, 15]
        static final String COLUMN_NAME_CONNECTED = "connected";             // boolean
        static final String COLUMN_NAME_LATITUDE = "latitude";               // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude";             // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";                 // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    public static abstract class NEW_RADIO implements BaseColumns {
        public static final String TABLE_NAME = "newradio";
        static final String COLUMN_NAME_TYPE = "type";                       // int (range [0,18]) defined in TelephonyManager.java
        static final String COLUMN_NAME_MNC = "mnc";                         // int (Parsed and converted from String) defined in https://developer.android.com/reference/android/telephony/CellIdentityNr.html#getMncString()
        static final String COLUMN_NAME_MCC = "mcc";                         // int (Parsed and converted from String) defined in https://developer.android.com/reference/android/telephony/CellIdentityNr.html#getMccString()
        static final String COLUMN_NAME_NCI = "nci";                         // long (36-bit NR Cell Identity in range [0, 68719476735] can be {@link CellInfo#UNAVAILABLE_LONG} if not specified) defined in CellIdentityNr.java
        static final String COLUMN_NAME_NRARFCN = "nrarfcn";                 // int (NR Absolute Radio Frequency Channel Number in range [0, 3279165] or CellInfo#UNAVAILABLE if unknown) defined in CellIdentityNr.java
        static final String COLUMN_NAME_PCI = "pci";                         // int (Physical Cell Id in range [0, 1007] or CellInfo#UNAVAILABLE if unknown) defined in CellIdentityNr.java
        static final String COLUMN_NAME_TAC = "tac";                         // int (a 16 bit integer in range [0, 65535] or CellInfo#UNAVAILABLE if unknown) defined in CellIdentityNr.java
        static final String COLUMN_NAME_CSI_RSRP = "dbm";                    // int  3GPP TS 38.215. CSI-RSRP as dBm value -140..-44dBm or CellInfo#UNAVAILABLE
        static final String COLUMN_NAME_CSI_RSRQ = "csi_rsrq";               // int (3GPP TS 38.215. Range: -20 dB to -3 dB and CellInfo#UNAVAILABLE means unreported value)
        static final String COLUMN_NAME_CSI_SINR = "csi_sinr";               // int (3GPP TS 38.215 Sec 5.1.*, 3GPP TS 38.133 10.1.16.1 Range: -23 dB to 23 dB and CellInfo#UNAVAILABLE means unreported value)
        static final String COLUMN_NAME_SS_RSRP = "ss_rsrp";                 // int (3GPP TS 38.215. Range: -140 dBm to -44 dBm and CellInfo#UNAVAILABLE means unreported value)
        static final String COLUMN_NAME_SS_RSRQ = "ss_rsrq";                 // int (3GPP TS 38.215. Range: -20 dB to -3 dB and CellInfo#UNAVAILABLE means unreported value)
        static final String COLUMN_NAME_SS_SINR = "ss_sinr";                 // int (3GPP TS 38.215 Sec 5.1.*, 3GPP TS 38.133 10.1.16.1 Range: -23 dB to 40 dB and CellInfo#UNAVAILABLE means unreported value)
        static final String COLUMN_NAME_CONNECTED = "connected";             // boolean
        static final String COLUMN_NAME_LATITUDE = "latitude";               // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude";             // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";                 // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }


    public static abstract class CDMA implements BaseColumns {
        public static final String TABLE_NAME = "cdma";
        static final String COLUMN_NAME_TYPE = "type";                           // int (range [0,18]) defined in TelephonyManager.java
        static final String COLUMN_NAME_BASE_STATION_ID = "base_station_id";     // int (range [0,65535] can be Integer.MAX_VALUE if not specified) defined in CellIdentityCdma.java
        static final String COLUMN_NAME_NETWORK_ID = "network_id";               // int (range [0,65535] can be Integer.MAX_VALUE if not specified) defined in CellIdentityCdma.java
        static final String COLUMN_NAME_SYSTEM_ID = "system_id";                 // int (range [0,32767] can be Integer.MAX_VALUE if not specified) defined in CellIdentityCdma.java
        static final String COLUMN_NAME_STATION_LATITUDE = "station_latitude";   // int (range [-1296000, 1296000], Integer.MAX_VALUE if unknown) defined in CellIdentityCdma.java
        static final String COLUMN_NAME_STATION_LONGITUDE = "station_longitude"; // int (range [-2592000,2592000], Integer.MAX_VALUE if unknown) defined in CellIdentityCdma.java
        static final String COLUMN_NAME_CDMA_DBM = "cdma_dbm";                   // int RSSI value (in [-120, 0], can go up to Integer.MAX_VALUE if unavailable) defined in CellSignalStrengthCdma.java
        static final String COLUMN_NAME_CDMA_ECIO = "cdma_ecio";                 // int in dB(in [-160, 0], can go up to Integer.MAX_VALUE if unavailable) defined in CellSignalStrengthCdma.java, according to http://www.telecomhall.com/what-is-ecio-and-ebno.aspx always negative
        static final String COLUMN_NAME_EVDO_DBM = "evdo_dbm";                   // int RSSI value (in [-120, 0], can go up to Integer.MAX_VALUE if unavailable) defined in CellSignalStrengthCdma.java
        static final String COLUMN_NAME_EVDO_ECIO = "evdo_ecio";                 // int in dB (in [-160, 0], can go up to Integer.MAX_VALUE if unavailable) defined in CellSignalStrengthCdma.java
        static final String COLUMN_NAME_EVDO_SNR = "evdo_snr";                   // int (range [0,8]) defined in CellSignalStrengthCdma.java
        static final String COLUMN_NAME_CONNECTED = "connected";                 // boolean
        static final String COLUMN_NAME_LATITUDE = "latitude";                   // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude";                 // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";                     // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    public static abstract class WIFI implements BaseColumns {
        public static final String TABLE_NAME = "wifi";
        static final String COLUMN_NAME_SSID = "ssid";                                   // String, defined in ScanResult.java
        static final String COLUMN_NAME_BSSID = "bssid";                                 // long (we convert the string representation of the MAC address to a long for compactness)
        static final String COLUMN_NAME_OPERATOR_FRIENDLY_NAME = "operatorFriendlyName"; // String, defined in ScanResult.java
        static final String COLUMN_NAME_VENUE_NAME = "venueName";                        // String, defined in ScanResult.java
        static final String COLUMN_NAME_IS_PASSPOINT_NETWORK = "isPasspointNetwork";     // int in [-1, 0, 1]. -1 is unknown, 0 is false, and 1 is true
        static final String COLUMN_NAME_FREQ = "frequency";                              // int in MHz [2412,5825] for 2GHz and 5GHz bands, defined in ScanResult.java
        static final String COLUMN_NAME_CENTERFREQ0 = "centerFreq0";                     // int
        static final String COLUMN_NAME_CENTERFREQ1 = "centerFreq1";                     // int
        static final String COLUMN_NAME_CHANNELWIDTH = "channelWidth";                   // int
        static final String COLUMN_NAME_CAPABILITIES = "capabilities";                   // String, defined in ScanResult.java
        static final String COLUMN_NAME_WIFI_STANDARD = "wifiStandard";                  // int [0, 7] and Const.INVALID_WIFI_STANDARD if the value is not available
        static final String COLUMN_NAME_DBM = "dbm";                                     // int, defined in ScanResult.java
        static final String COLUMN_NAME_CONNECTED = "connected";                         // boolean
        static final String COLUMN_NAME_LATITUDE = "latitude";                           // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude";                         // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";                             // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    public static abstract class BLUETOOTH implements BaseColumns {
        public static final String TABLE_NAME = "bluetooth";
        static final String COLUMN_NAME_NAME = "bt_device_name";             // String
        static final String COLUMN_NAME_NAME_ALIAS = "bt_device_name_alias"; // String
        static final String COLUMN_NAME_DEVICE_CLASS = "bt_device_class";    // int
        static final String COLUMN_NAME_DEVICE_TYPE = "bt_device_type";      // int
        static final String COLUMN_NAME_ADDRESS = "bt_address";              // long (we convert the string representation of the MAC address to a long for compactness)
        static final String COLUMN_NAME_BOND_STATE = "bt_bond_state";        // int
        static final String COLUMN_NAME_DBM = "dbm";                         // int
        static final String COLUMN_NAME_LATITUDE = "latitude";               // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude";             // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";                 // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class OPERATORS implements BaseColumns {
        static final String TABLE_NAME = "operators";
        static final String COLUMN_NAME_MNC = "mnc";                     // int (range [0,999] can be Integer.MAX_VALUE if not specified)
        static final String COLUMN_NAME_MCC = "mcc";                     // int (range [0,999] can be Integer.MAX_VALUE if not specified)
        static final String COLUMN_NAME_OPERATOR_NAME = "operator_name"; // String
    }

    public static abstract class ORIENTATION implements BaseColumns {
        public static final String TABLE_NAME = "orientation";
        static final String COLUMN_NAME_X = "x";                 // float (range [-1.0, 1.0] empirical)
        static final String COLUMN_NAME_Y = "y";                 // float (range [-1.0, 1.0] empirical)
        static final String COLUMN_NAME_Z = "z";                 // float (range [-1.0, 1.0] empirical)
        static final String COLUMN_NAME_ANGLE = "angle";         // float (range [-1.0, 1.0] empirical)
        static final String COLUMN_NAME_ACCURACY = "accuracy";   // float (range [-1.0, 1.0] empirical)
        static final String COLUMN_NAME_CREATED = "created";     // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class DEVICE_INFO implements BaseColumns {
        static final String TABLE_NAME = "device_info";
        static final String COLUMN_NAME_BRAND = "brand";                         // String
        static final String COLUMN_NAME_DEVICE = "device";                       // String
        static final String COLUMN_NAME_HARDWARE = "hardware";                   // String
        static final String COLUMN_NAME_MANUFACTURER = "manufacturer";           // String
        static final String COLUMN_NAME_MODEL = "model";                         // String
        static final String COLUMN_NAME_PRODUCT = "product";                     // String
        static final String COLUMN_NAME_SOC_MANUFACTURER = "soc_manufacturer";   // String
        static final String COLUMN_NAME_SOC_MODEL = "soc_model";                 // String
        static final String COLUMN_NAME_CREATED = "created";                     // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class OS_INFO implements BaseColumns {
        static final String TABLE_NAME = "os_info";
        static final String COLUMN_NAME_RELEASE = "release";   // String
        static final String COLUMN_NAME_SDK_INT = "sdk_int";   // int
        static final String COLUMN_NAME_CREATED = "created";   // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class SIM_INFO implements BaseColumns {
        static final String TABLE_NAME = "sim_info";
        static final String COLUMN_NAME_SIM_COUNTRY_CODE = "sim_country_code";   // String
        static final String COLUMN_NAME_SIM_OPERATOR = "sim_operator";           // String
        static final String COLUMN_NAME_SIM_OPERATOR_NAME = "sim_operator_name"; // String
        static final String COLUMN_NAME_CREATED = "created";                     // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class APP_VERSION implements BaseColumns {
        static final String TABLE_NAME = "app_version";
        static final String COLUMN_NAME_VERSION = "version";   // int
        static final String COLUMN_NAME_CREATED = "created";   // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class PROFILE_ID implements BaseColumns {
        static final String TABLE_NAME = "profile_id";
        static final String COLUMN_NAME_ID = "id";                   // String
    }

    static abstract class USER_PROFILE implements BaseColumns {
        static final String TABLE_NAME = "user_profile";
        static final String COLUMN_NAME_NAME = "name";                 // String (max Const.MAX_LENGTH_EDIT_TEXT_NAME chars)
        static final String COLUMN_NAME_EMAIL = "email";               // String (max Const.MAX_LENGTH_EDIT_TEXT_EMAIL chars)
        static final String COLUMN_NAME_SEX = "sex";                   // int (-1: not provided, 0: Male, 1: Female)
        static final String COLUMN_NAME_AGE = "age";                   // int (max Const.MAX_LENGTH_EDIT_TEXT_AGE digits)
        static final String COLUMN_NAME_USER_SEGMENT = "user_segment"; // int (-1: not provided, 0: ElectroSensible, 1: Concerned, 2: Curious)
        static final String COLUMN_NAME_CREATED = "created";           // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class WHITE_ZONE implements BaseColumns {
        static final String TABLE_NAME = "white_zone";
        static final String COLUMN_NAME_SIGNAL_TYPE = "signal_type";     // int in [1, 2, 3], defined in Const.java
        static final String COLUMN_NAME_LATITUDE = "latitude";           // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_LONGITUDE = "longitude";         // double (range not specified (value in degrees)) defined in Location.java
        static final String COLUMN_NAME_CREATED = "created";             // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class EVENT implements BaseColumns {
        static final String TABLE_NAME = "event";
        static final String COLUMN_NAME_EVENT_TYPE = "event_type";       // int defined in Const.java
        static final String COLUMN_NAME_CREATED = "created";             // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class DEVICE_LOCALE implements BaseColumns {
        static final String TABLE_NAME = "device_locale";
        static final String COLUMN_NAME_LOCALE = "locale";               // String
        static final String COLUMN_NAME_CREATED = "created";             // long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class TOP_5_SIGNALS implements BaseColumns {
        static final String TABLE_NAME = "top_5_signals";
        static final String COLUMN_NAME_SIGNALS = "signals";             // String
        static final String COLUMN_NAME_SIGNALS_DATE = "signals_date";   // The day corresponding to the computation of the top 5 signals of the row in the table, long (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }

    static abstract class DAILY_STAT_SUMMARY implements BaseColumns {
        static final String TABLE_NAME = "daily_stat_summary";
        static final String COLUMN_NAME_DBM = "dbm";                                       // int, dbm value for the day
        static final String COLUMN_NAME_NUMBER_OF_NEW_SOURCES = "number_of_new_sources";   // int, the number of new sources detected the day corresponding to this row, int
        static final String COLUMN_NAME_SUMMARY_DATE = "summary_date";                     // long, the day for which we computed the summary in this row of the table (number of milliseconds since Jan. 1, 1970, midnight GMT)
    }
}

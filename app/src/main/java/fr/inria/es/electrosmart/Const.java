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

package fr.inria.es.electrosmart;

import fr.inria.es.electrosmart.monitors.CellularMonitor;

/**
 * This class contains all constants
 * Note that there is a values/const.xml file that contains constants for
 * XML resource files
 */
public class Const {

    //########################################################################################
    //########################### CONFIGURATION PARAMETERS ###################################
    //########################################################################################

    //############################ PREFERENCE PARAMETERS############################
    /*
    if DO_AGGRESSIVE_BT_CARD_SCAN (DO_AGGRESSIVE_WIFI_CARD_SCAN) is set to true, we enable the BT
    (WIFI) card before a scan is necessary (see the BluetoothMonitor and WifiMonitor for a
    description of the logic)

    Note: the variables are not final to suppress the lint warnings in the boolean expressions
    */
    public static boolean DO_AGGRESSIVE_BT_CARD_SCAN = false;   //in DEBUG settings
    public static boolean DO_AGGRESSIVE_WIFI_CARD_SCAN = false; //in DEBUG settings


    //############################ DEBUG parameter ############################

    // set to true in order to create a debug DB
    public static final boolean MAKE_DEBUG_DB = false;

    public static final boolean DO_DUMP_LOGCAT_ON_FILE = true;

    // We can use this constant to test the app with a release functionality even for a debug build
    public static final boolean IS_RELEASE_BUILD = BuildConfig.IS_RELEASE_BUILD;


    //############################ RECOMMENDATION PARAMETERS #################################

    // we have two threshold values
    // [RECOMMENDATION_HIGH_THRESHOLD, ...[ is high exposure
    // [RECOMMENDATION_LOW_THRESHOLD, RECOMMENDATION_HIGH_THRESHOLD[ is medium exposure
    // ]..., RECOMMENDATION_LOW_THRESHOLD[ is low exposure
    public static final int RECOMMENDATION_HIGH_THRESHOLD = -34; // dBm (source 100mW, 2.4GHz, 5 meters, 0.3V/m)
    public static final int RECOMMENDATION_LOW_THRESHOLD = -75; // dBm

    // To build the recommendation scale we use the following values
    // MIN_RECOMMENDATION_SCALE, RECOMMENDATION_LOW_THRESHOLD, RECOMMENDATION_HIGH_THRESHOLD, MAX_RECOMMENDATION_SCALE
    // In the different units it gives
    // 0,      42,      70,   100 (exposure score)
    // -140,  -75,     -34,   10 (dBm)
    // 1aW,    31.6pW, 398nW, 10mW (Watt)
    public static final int MIN_RECOMMENDATION_SCALE = -140; // dBm
    public static final int MAX_RECOMMENDATION_SCALE = 10; // dBm

    // Default thresholds for high exposure notification based on user segment
    public static final int HIGH_EXPOSURE_THRESHOLD_ELECTROSENSITIVE = RECOMMENDATION_LOW_THRESHOLD;
    public static final int HIGH_EXPOSURE_THRESHOLD_CURIOUS = RECOMMENDATION_HIGH_THRESHOLD;
    public static final int HIGH_EXPOSURE_THRESHOLD_CONCERNED = -40; // -40 dbm is a score of 66

    //############################ IN-APP REVIEW PARAMETERS ##############################

    // Thresholds used to decide to trigger the in-app review

    // the app must be installed more than this number of days to show in-app reviews
    public static final int MIN_DAYS_SINCE_APP_INSTALLATION = 1;
    // the MainActivity must have been resumed more that this number of unique days to show in-app reviews
    public static final int MIN_DAYS_MAIN_ACTIVITY_RESUMED = 3;
    // the MainActivity must have been resumed more that this number of unique hours to show in-app reviews
    public static final int MIN_HOURS_MAIN_ACTIVITY_RESUMED = 5;
    // the in-app review flow must not have been completed more than this number of times to show in-app reviews
    public static final int MAX_NB_TIME_IN_APP_REVIEW_COMPLETED = 6;

    //############################ STATISTICS PARAMETERS #################################
    public static final int STATS_HOUR_TIME_GAP = 3600; // in seconds

    //############################ TEST YOUR PROTECTION PARAMETERS ############################

    // The default duration to test a protection in seconds.
    public static final int PROTECTION_TEST_DURATION_SECONDS = 30; // seconds

    /**
     * The test we perform uses Wi-Fi signals because they are the more reliable. However, on
     * on Android P, due to a new limitation in the number of Wi-Fi scans allowed in foreground,
     * in the worst case, we might need to wait for 2 minutes before a new Wi-Fi scan can be
     * performed. For this reason, we need a longer duration of the test in P.
     */
    public static final int PROTECTION_TEST_DURATION_SECONDS_ANDROID_P = 125; //seconds

    /*
    Threshold of the reduction percentage below which we claim there is no protection.
    60% corresponds to a reduction by a factor of 2.5
    70% corresponds to a reduction by a factor of 3.33
    80% corresponds to a reduction by a factor of 5
    90% corresponds to a reduction by a factor of 10
    95% corresponds to a reduction by a factor of 20
    99% corresponds to a reduction by a factor of 100

    The formula to compute the reduction factor R with a reduction percentage P is
    R = 100/(100-P)
    */
    public static final int PROTECTION_NO_REDUCTION_THRESHOLD = 80; // in percentage
    public static final int PROTECTION_GREAT_REDUCTION_THRESHOLD = 95; // in percentage

    // The time we wait after we have sensed proximity and before starting the second scan
    public static final int PROXIMITY_DETECTION_TIME_MILLIS = 2_000; // in ms


    //############################ NOTIFICATION PARAMETERS############################

    // The maximum size of the accumulated notification results when we update
    // an old notification
    public static final int MAXIMUM_NOTIFICATION_HISTORY_SIZE = 6;

    // Represents the minimum number of days between the first date in the TOP_5_SIGNALS table
    // and yesterday (the last displayable statistic day). We must add one days (today)
    // to get the number of days between the first entry in the TOP_5_SIGNALS table and now.
    public final static int MIN_NUM_OF_DAYS_BEFORE_SHOWING_NEW_SOURCE_NOTIF = 2; // value in days

    // Represents the number of days before yesterday for which we will compute the top 5 signals
    // if the app has been updated and there is no top 5 signals computed yet.
    public final static int MAX_NUM_OF_DAYS_TO_COMPUTE_NEW_SOURCES_ON_UPDATE; //value in days

    static {
        // if we have a debug DB, we compute the new signals for the entire DB
        if (MAKE_DEBUG_DB) {
            MAX_NUM_OF_DAYS_TO_COMPUTE_NEW_SOURCES_ON_UPDATE = Integer.MAX_VALUE;
        } else {
            MAX_NUM_OF_DAYS_TO_COMPUTE_NEW_SOURCES_ON_UPDATE = 3;
        }
    }

    //############################ SERVICE PARAMETERS ############################

    //WARNING set MEASUREMENT_CYCLE_BACKGROUND back to 1200* 1000 after the tests (that is 20 minutes)
    public static final long MEASUREMENT_CYCLE_BACKGROUND = 1200 * 1000;   //in DEBUG settings
    // IMPORTANT: This MEASUREMENT_CYCLE_FOREGROUND is bound to the chart construction. It is not
    //            possible to have a MEASUREMENT_CYCLE_FOREGROUND < 1000 otherwise,
    //            see LIVE_TIME_GAP.
    public static final long MEASUREMENT_CYCLE_FOREGROUND = 5 * 1000; // in milliseconds

    /*
    Android P introduced foreground Wi-Fi throttling
    https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-throttling
    We can make in Pie at most 4 scans every 2 minutes. Our goal is to evenly spread these scans
    on the 2 minutes period instead of making 4 scans in a raw and waiting for 1m40s for the
    next update
    `MEASUREMENT_CYCLE_FOREGROUND_WIFI_ANDROID_P` determines how frequently Wi-Fi scans are to be
    performed on Android P+ devices
    */
    public static final long MEASUREMENT_CYCLE_FOREGROUND_WIFI_ANDROID_P = 30 * 1000; // in milliseconds

    // gives the minimum time between two consecutive Bluetooth scans for the bluetooth monitor in
    // FOREGROUND mode
    public static final int MIN_TIME_BETWEEN_CONSECUTIVE_BT_SCAN = 30; // in seconds
    // Duration we wait after starting the location service before running all monitors. This
    // duration is required to get an accurate location. This is only used in BACKGROUND mode
    // This duration must be long enough to get an accurate location, but short enough to do not
    // waste the battery.
    public static final long LOCATION_DISCOVERY_DURATION = 30 * 1000; // in milliseconds

    public static final long MIN_TIME_BETWEEN_LOCATION_UPDATES_FOREGROUND = 1000 * 2; // 2 seconds
    public static final long MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES_FOREGROUND = 1; // 1 meter

    public static final long MIN_TIME_BETWEEN_LOCATION_UPDATES_BACKGROUND = 1000 * 10; // 10 seconds
    public static final long MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES_BACKGROUND = 10; // 10 meters


    //############################ DISPLAY PARAMETERS ############################

    // Chart time gaps (The time between two slots) in seconds
    public static final int HISTORY_TIME_GAP = 3600; // (one hour)
    public static final int LIVE_TIME_GAP = (int) (MEASUREMENT_CYCLE_FOREGROUND / 1000);

    // dBm threshold values used to determine the signal strength
    // (low, low medium, medium, medium high, high).
    // Note: it is a legacy code that is only used in the chart today.
    public final static int LOW_DBM_THRESHOLD = -95;
    public final static int LOW_MEDIUM_DBM_THRESHOLD = -75;
    public final static int MEDIUM_DBM_THRESHOLD = -60;
    public final static int MEDIUM_HIGH_DBM_THRESHOLD = -40;

    // The maximum number of slots (each slot is 5 seconds) loaded to chart in live mode.
    // The chart is shrunk removing old data if it exceeds this size after new data is added.
    // Note: 720 corresponds to one hour in mode live
    public static final int CHART_MAX_SLOTS_LIVE = 720; // 720 * 5 seconds = 3600 s = 1 hour

    // The maximum number of slots (each slot is 1 hour) loaded to chart in history mode.
    // The chart is shrunk removing old data if it exceeds this size after new data is added.
    // Note: 720 corresponds to 30 days in history mode
    public static final int CHART_MAX_SLOTS_HISTORY = 720; // 720 * 1 hour / 24 hours = 30 days

    // Defines the maximum number of slots on a page in the chart.
    // A page is the number of slots that get requested from the database at once
    // in history mode.
    public static final int MAX_SLOTS_PER_PAGE = 24; // in hours

    // duration of the hysteresis before purging the old data from cachedRawSignals.
    // This is only used to update the user interface, it has no impact on the data stored in the DB
    public final static int BT_HYSTERESIS_DURATION = 65; // value in seconds
    public final static int WIFI_HYSTERESIS_DURATION = 60; // value in seconds
    public final static int CELLULAR_HYSTERESIS_DURATION = 30; // value in seconds


    public final static int MIN_MCC = 1; // in the ITU list of MCC/MNC there is no MCC at 0
    public final static int MAX_MCC = 999;
    public final static int MIN_MNC = 0;
    public final static int MAX_MNC = 999;

    // GSM specific
    public final static int GSM_MIN_CID = 0;
    public final static int GSM_MAX_CID = 65535; // 16 bits: 2**16 - 1
    public final static int GSM_MIN_LAC = 0;
    public final static int GSM_MAX_LAC = 65535; // 16 bits: 2**16 - 1
    public final static int GSM_MIN_ARFCN = 0;
    public final static int GSM_MAX_ARFCN = 65535; // 16 bits: 2**16 - 1
    public final static int GSM_MIN_BSIC = 0;
    public final static int GSM_MAX_BSIC = 63; // 6 bits: 2**6 - 1
    public final static int GSM_MIN_TA = 0;
    public final static int GSM_MAX_TA = 219;
    public final static int GSM_MIN_DBM = -113;
    public final static int GSM_MAX_DBM = -51;
    public final static int GSM_MIN_BER = 0;
    public final static int GSM_MAX_BER = 7; // 3 bits: 2**3 - 1

    // WCDMA specific
    public final static int WCDMA_MIN_UCID = 0;
    public final static int WCDMA_MAX_UCID = 268435455; // 28 bits: 2**28 - 1
    public final static int WCDMA_MIN_LAC = 0;
    public final static int WCDMA_MAX_LAC = 65535; // 16 bits: 2**16 - 1
    public final static int WCDMA_MIN_PSC = 0;
    public final static int WCDMA_MAX_PSC = 511;  // 9 bits: 2**9 - 1
    public final static int WCDMA_MIN_UARFCN = 0;
    public final static int WCDMA_MAX_UARFCN = 65535; // 16 bits: 2**16 - 1
    public final static int WCDMA_MIN_DBM = -120;
    public final static int WCDMA_MAX_DBM = -24;
    public final static int WCDMA_MIN_BER = 0;
    public final static int WCDMA_MAX_BER = 7; // 3 bits: 2**3 - 1
    public final static int WCDMA_MIN_ECNO = -24;
    public final static int WCDMA_MAX_ECNO = 1;

    // LTE specific
    public final static int LTE_MIN_ECI = 0;
    public final static int LTE_MAX_ECI = 268435455; // 28 bits: 2**28 - 1
    public final static int LTE_MIN_PCI = 0;
    public final static int LTE_MAX_PCI = 503;
    public final static int LTE_MIN_TAC = 0;
    public final static int LTE_MAX_TAC = 65535; // 16 bits: 2**16 - 1
    public final static int LTE_MIN_EARFCN = 0;
    public final static int LTE_MAX_EARFCN = 262143; // 18 bits: 2**18 - 1
    public final static int LTE_MAX_BANDWIDTH = 20000; // according to the private MAX_BANDWIDTH value in CellIdentityLte.java
    public final static int LTE_MIN_BANDWIDTH = 0;
    public final static int LTE_MIN_TA = 0;
    public final static int LTE_MAX_TA = 1282; // according to the CTS, but 63 according to the spec. see comment in DbContract.java

    public final static int LTE_MAX_RSSI = 31;  // this is the RSSI starting with Android Q (API 29). See DbContract for more info.
    public final static int LTE_MIN_RSSI = 0; // The value range of RSSI in ASU is [0, 31] inclusively or CellInfo#UNAVAILABLE if unavailable.
    public final static int LTE_MIN_RSRP = -140;
    public final static int LTE_MAX_RSRP = -43;
    public final static int LTE_MIN_RSRQ = -20;
    public final static int LTE_MAX_RSRQ = -3;
    public final static int LTE_MIN_RSSNR = -200;
    public final static int LTE_MAX_RSSNR = 300;
    public final static int LTE_MIN_CQI = 0;
    public final static int LTE_MAX_CQI = 15;

    // NewRadio specific
    public final static long NR_MIN_NCI = 0;
    public final static long NR_MAX_NCI = 68719476735L; // 36-bit NR Cell Identity
    public final static int NR_MIN_NRARFCN = 0;
    public final static int NR_MAX_NRARFCN = 3279165;
    public final static int NR_MIN_PCI = 0;
    public final static int NR_MAX_PCI = 1007;
    public final static int NR_MIN_TAC = 0;
    public final static int NR_MAX_TAC = 65535;

    public final static int NR_MIN_CSI_RSRP = -140;
    public final static int NR_MAX_CSI_RSRP = -44;
    public final static int NR_MIN_CSI_RSRQ = -20;
    public final static int NR_MAX_CSI_RSRQ = -3;
    public final static int NR_MIN_CSI_SINR = -23;
    public final static int NR_MAX_CSI_SINR = 23;
    public final static int NR_MIN_SS_RSRP = -140;
    public final static int NR_MAX_SS_RSRP = -44;
    public final static int NR_MIN_SS_RSRQ = -20;
    public final static int NR_MAX_SS_RSRQ = -3;
    public final static int NR_MIN_SS_SINR = -23;
    public final static int NR_MAX_SS_SINR = 40;

    // CDMA specific
    public final static int CDMA_MIN_NID = 0;
    public final static int CDMA_MAX_NID = 65535;
    public final static int CDMA_MIN_SID = 0;
    public final static int CDMA_MAX_SID = 32767;
    public final static int CDMA_MIN_BSID = 0;
    public final static int CDMA_MAX_BSID = 65535;
    public final static int CDMA_MIN_LATITUDE = -1296000;
    public final static int CDMA_MAX_LATITUDE = 1296000;
    public final static int CDMA_MIN_LONGITUDE = -2592000;
    public final static int CDMA_MAX_LONGITUDE = 2592000;
    public final static int CDMA_MIN_ECIO = -160;
    public final static int CDMA_MAX_ECIO = 0;
    public final static int CDMA_MIN_SNR = 0;
    public final static int CDMA_MAX_SNR = 8;
    public final static int CDMA_MIN_DBM = -120;
    public final static int CDMA_MAX_DBM = 0;

    /*
     Used only for display in CustomExpandableListAdapter
     The reason for these bounds are given in DbContract.java
     Outside of these bounds, we do not display the dbm/rssi/power values, but n/a instead.

     GSM_MIN_DBM
     GSM_MAX_DBM
     WCDMA_MIN_DBM
     WCDMA_MAX_DBM

     The dbm value in LTE can be the RSSI (preferred) or the RSRP, for the MIN_DISPLAYABLE_DBM and
     MAX_DISPLAYABLE_DBM values, we decided to use the RSSI. Note that it will not have much impact
     as the MIN_DISPLAYABLE_DBM and MAX_DISPLAYABLE_DBM are dominated by the Wi-Fi min and max values
     LTE_MIN_RSSI (in ASU, we must convert it to dBm)
     LTE_MAX_RSSI (in ASU, we must convert it to dBm)

     NR_MIN_CSI_RSRP
     NR_MAX_CSI_RSRP
     CDMA_MIN_DBM
     CDMA_MAX_DBM
   */
    public final static int WIFI_MIN_DBM = -150;
    public final static int WIFI_MAX_DBM = -1;
    public final static int BT_MIN_DBM = -150;
    public final static int BT_MAX_DBM = -1;

    /*
    Computes the MAX (resp. MIN) for the max (resp. min) dBm values above
    This is used by the chart to find the min and max values that can be displayed
    */
    public final static int MAX_DISPLAYABLE_DBM =
            Math.max(GSM_MAX_DBM,
                    Math.max(WCDMA_MAX_DBM,
                            Math.max(CellularMonitor.convertLteRssiAsuToDbm(LTE_MAX_RSSI),
                                    Math.max(NR_MAX_CSI_RSRP,
                                            Math.max(CDMA_MAX_DBM,
                                                    Math.max(WIFI_MAX_DBM, BT_MAX_DBM))))));

    public final static int MIN_DISPLAYABLE_DBM =
            Math.min(GSM_MIN_DBM,
                    Math.min(WCDMA_MIN_DBM,
                            Math.min(CellularMonitor.convertLteRssiAsuToDbm(LTE_MIN_RSSI),
                                    Math.min(NR_MIN_CSI_RSRP,
                                            Math.min(CDMA_MIN_DBM,
                                                    Math.min(WIFI_MIN_DBM, BT_MIN_DBM))))));

    /*
    The MIN_DBM_FOR_ROOT_SCORE is a critical parameter that must not change on future releases.
    Indeed, this MIN_DBM_FOR_ROOT_SCORE gives the dBm value below which the
    exposition score will be zero. So the exposition of users will be rooted to this value. If we
    change it, it will change the exposition scale, and can disturb users used to another scale.
    */
    public final static int MIN_DBM_FOR_ROOT_SCORE = -140;

    //############################ SERVER SYNCHRONIZATION PARAMETERS ############################
    /*
    After some experimentation, we found that with gzip in place, we can increase
    MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK to 8000 without much impact on the server and reducing
    the synchronization time approximately by a factor of 2.

    Also, with 8000, the average size of each chunk is around 11KB and is roughly equivalent to
    3 days of ideal es data (30 measurements per hour for 3 days).

    More details: https://bitbucket.org/es-inria/es-android/issues/154/
                  https://bitbucket.org/es-inria/es-android/downloads/test_sync_results_with_gzip.pdf

    Further, we tested the average time an android device takes to compress the json with
    MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK set to 8000.

    ------------------------------------------------------------------------------------------
    Device                      | Time taken for compression
    ------------------------------------------------------------------------------------------
    Nexus 6 (Android 7.1.1),    |  30 ms
    Nexus S (Android 4.1)       | 130 ms
    ------------------------------------------------------------------------------------------

    We found these values acceptable.
    */
    public static final int MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK = 8000;
    /*
    Minimum interval for the dichotomy search to find the time interval with at most
    MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK
    */
    public static final long MIN_DICHOTOMY_INTERVAL = 60000;

    //url strings for GET and POST requests to the sync server
    private static final String WEB_REQUESTS_DOMAIN = ""; // Give here the domain of the server that accept the web requests
    private static final String WEB_REQUESTS_PROTOCOL_PREFIX = "https://";
    public static final String APP_VERSION_NUMBER_URL_PARAMETER = "appVersionNumber=";
    public static final String UNIQUE_ID_URL_PARAMETER = "uniqueId=";
    public static final String SOLUTION_URL_PARAMETER = "solution=";
    public static final String UNIQUE_ID_GET_URL = WEB_REQUESTS_PROTOCOL_PREFIX + WEB_REQUESTS_DOMAIN + "/getUniqueId";
    public static final String LAST_SUCCESSFUL_UPLOAD_DATE_GET_URL = WEB_REQUESTS_PROTOCOL_PREFIX +
            WEB_REQUESTS_DOMAIN + "/getLastSuccessfulCommitDate?" + UNIQUE_ID_URL_PARAMETER;
    public static final String DATA_UPLOAD_POST_URL = WEB_REQUESTS_PROTOCOL_PREFIX +
            WEB_REQUESTS_DOMAIN + "/data?" + UNIQUE_ID_URL_PARAMETER;


    // url string to send solutions click event and handling further redirection
    // public static final String ES_SOLUTIONS_URL = WEB_REQUESTS_PROTOCOL_PREFIX + SOLUTIONS_WEB_REQUESTS_DOMAIN;
    public static final String SOLUTION_OWAYN = "owayn";
    public static final String SOLUTION_SYBOX = "sybox";

    // We must add to all URLs the following optional arguments to enable tracking in Google Analytics
    private static final String UTM_TRACKING_PART = "?utm_source=electrosmartapp&utm_medium=referral";

    public static final String SOLUTION_OWAYN_URL = "https://getlambs.com/" + UTM_TRACKING_PART;
    public static final String SOLUTION_SYBOX_URL_FR = "https://sybox.sycy.fr/" + UTM_TRACKING_PART;
    public static final String SOLUTION_SYBOX_URL_EN = "https://sybox.sycy.fr/en/" + UTM_TRACKING_PART;

    // URLs used in the AdviceFragment to redirect to our explanation on the score.
    public static final String ADVICE_READ_OUR_ARTICLE_URL_IT = "https://electrosmart.app/calcul-de-lindice-it/";
    public static final String ADVICE_READ_OUR_ARTICLE_URL_EN = "https://electrosmart.app/calcul-de-lindice-en/";
    public static final String ADVICE_READ_OUR_ARTICLE_URL_DE = "https://electrosmart.app/calcul-de-lindice-de/";
    public static final String ADVICE_READ_OUR_ARTICLE_URL_FR = "https://electrosmart.app/calcul-de-lindice-fr/";
    public static final String ADVICE_READ_OUR_ARTICLE_URL_ES = "https://electrosmart.app/calcul-de-lindice-es/";
    public static final String ADVICE_READ_OUR_ARTICLE_URL_PT = "https://electrosmart.app/calcul-de-lindice-pt/";

    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = MainApplication.getContext().getResources().getString(R.string.content_authority);
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = MainApplication.getContext().getResources().getString(R.string.account_type);

    /*
    The synchronization interval is computed the following way.

    We select a random value R uniformly in [0, SYNC_RANDOMIZATION].
    We use this R to find the next data synchronization time which is (MIN_DURATION_FOR_NEXT_SYNC + R).

    Note that on boot (when the devices just booted, we shorten the sync interval to solve issue
    #183 (Improve sync strategy for devices that may undergo frequent reboots)
     */
    // The interval of time after which we make a synchronization
    public final static long MIN_DURATION_FOR_NEXT_SYNC = 18 * 60 * 60; // 18 hours in seconds
    public final static long MIN_DURATION_FOR_NEXT_SYNC_ON_BOOT = 10 * 60; // 10 minutes in seconds

    // The randomization interval used to make the synchronization
    public final static long SYNC_RANDOMIZATION = 12 * 60 * 60; // 12 hours in seconds
    public final static long SYNC_RANDOMIZATION_ON_NEW_SYNC_ALARM = 10 * 60; // 10 minutes in seconds

    //############################ ON-BOARDING AND PROFILE ACTIVITY PARAMETERS #############

    // Definition of the profile and on-boarding constant values representing the user choice
    // Name
    public static final String PROFILE_NAME_UNKNOWN = "";
    // Segment
    public static final int PROFILE_SEGMENT_UNKNOWN = -1;
    public static final int PROFILE_SEGMENT_ELECTROSENSITIVE = 0;
    public static final int PROFILE_SEGMENT_CONCERNED = 1;
    public static final int PROFILE_SEGMENT_CURIOUS = 2;
    // Email
    public static final String PROFILE_EMAIL_UNKNOWN = "";
    // Age
    public static final int PROFILE_AGE_UNKNOWN = -1;
    // Sex
    public static final int PROFILE_SEX_UNKNOWN = -1;
    public static final int PROFILE_SEX_MALE = 0;
    public static final int PROFILE_SEX_FEMALE = 1;

    // Notification (if it is not set, it is opt-in by default)
    public static final boolean PREFERENCE_EXPOSURE_NOTIFICATION_UNKNOWN = true;
    public static final boolean PREFERENCE_NEW_SOURCE_NOTIFICATION_UNKNOWN = true;

    //############################ FEEDBACK ACTIVITY PARAMETERS ############################

    // settings for the feedback activity.
    public static final String FEEDBACK_EMAIL_BUGREPORT = "";
    public static final String FEEDBACK_EMAIL_SUPPORT = "";
    public static final String FEEDBACK_EMAIL_SUBJECT_PREFIX_BUG = "[BUG]";
    public static final String FEEDBACK_EMAIL_SUBJECT_PREFIX_FEATURE = "[FEATURE]";
    public static final String FEEDBACK_EMAIL_SUBJECT_PREFIX_FEEDBACK = "[FEEDBACK]";
    public static final String FEEDBACK_EMAIL_SUBJECT_PREFIX_SUGGEST_SOLUTION = "[SUGGEST_SOLUTION]";

    // Communication with feedback activity
    public static final String FEEDBACK_DEFAULT_TYPE_EXTRA_KEY = "FEEDBACK_DEFAULT_TYPE_EXTRA_KEY";
    public static final String FEEDBACK_DEFAULT_EXTRA_VALUE_SUGGEST_SOLUTION = "FEEDBACK_DEFAULT_EXTRA_VALUE_SUGGEST_SOLUTION";


    //########################################################################################
    //############################ DO NOT CHANGE THESE CONSTANTS #############################
    //########################################################################################

    // ############################## Shared preferences (BEGIN) #############################
    // define shared preferences entries
    // We used shared preferences to use persistence of variables that can be otherwise destroyed
    // if they were static variables of the monitors.
    public static final String SHARED_PREF_FILE = BuildConfig.SHARED_PREF_FILE;
    /*
    Contains the time in milliseconds since the epoch when the first entry in the DB was made

    This value is never updated and is used to synchronize the local DB with the server (until the
    first synchronization succeeds).
    */
    public static final String DATE_FIRST_ENTRY_IN_DB = "date_first_entry_in_db";

    /*
    Contains the time in milliseconds since the epoch of the oldest signal in the DB.

    The DATE_FIRST_ENTRY_IN_DB represents the oldest entry on the DB considering all tables,
    however, the DbRequestHandler.deleteOldestDbEntries() method removes oldest signals from all
    signals tables (but not all tables). We must therefore have a separate shared preference
    that contains the timestamp for the oldest signals (even if in other tables we might have
    even older entries).

    This is updated by DbRequestHandler.deleteOldestDbEntries() and used to display the chart
    when we scroll.
    */
    public static final String DATE_OLDEST_SIGNAL_IN_DB = "date_oldest_signal_in_db";

    /*
    We have a set of three shared preferences that control the start of the app

    IS_TERMS_OF_USE_ACCEPTED is true when the terms of use in the Welcome screen are accepted

    IS_ONBOARDING_DONE is true when IS_TERMS_OF_USE_ACCEPTED is true and the onboarding process
    is completed

    IS_AGREEMENT_FLOW_DONE is true when IS_ONBOARDING_DONE is true and the location authorization
    in foreground is accepted. The background location permission for Q+ devices is not required,
    because we use this variable to know whether we can start the measurement service. If
    background location permission is not granted, we still start measurement services in background
    because it does not harm.
    Once accepted it is never reverted back.

    This full process is described in WelcomeTermsOfUseActivity.java
     */
    public static final String IS_TERMS_OF_USE_ACCEPTED = "is_terms_of_use_accepted";
    public static final String IS_ONBOARDING_DONE = "is_onboarding_done";
    public static final String IS_AGREEMENT_FLOW_DONE = "is_agreement_flow_done";

    // shared preference to control when the error fragment when there is no permission for the
    // background location should be displayed. The strategy is to show the error fragment each
    // time we start the app (that is MainApplication is created), but not onResume().
    public static final String SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR = "should_show_app_background_location_error";

    // shared preference to never show again the error fragment when there is no permission for the
    // background location. That is, this preference disables SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR
    public static final String DONT_SHOW_APP_BACKGROUND_LOCATION_ERROR = "dont_show_app_background_location_error";

    // This shared preferences is only used to create the test database (when we have a DEBUG build)
    // on the first run
    public static final String IS_FIRST_RUN = "is_first_run";

    public static final String IS_FIRST_TIME_STATISTICS_NOTIFICATION_SHOWN = "is_first_time_statistics_notification_shown";

    /**
     * A Shared preference that determines whether the exposure scale is shown or not in the
     * HomeFragment and the StatisticsFragment.
     * This is toggled when the user taps on the exposure scale chevron tap view.
     *
     * @see Tools#getShowExposureScale()
     * @see Tools#setShowExposureScale(boolean)
     */
    public static final String SHOW_EXPOSURE_SCALE = "show_exposure_scale";

    public static final String NB_DAYS_IN_HISTORY = "nb_days_in_history";

    // used in Tools.grantLocationToApp() and set in Tools.processRequestPermissionsResult() the first
    // time the user deny a location permission. See the documentation of Tools.grantLocationToApp()
    // for a full explanation of the logic of these variables.
    public static final String USER_DENIED_LOCATION_PERMISSION_BACKGROUND = "user_denied_location_permission_background";
    public static final String USER_DENIED_FINE_LOCATION_PERMISSION_FOREGROUND = "user_denied_fine_location_permission";
    public static final String USER_DENIED_COARSE_LOCATION_PERMISSION_FOREGROUND = "user_denied_coarse_location_permission";
    /* variable used to correctly restore the WIFI and BT card to its initial state after a scan */
    public static final String WAS_WIFI_ENABLED_BEFORE_SCAN = "was_wifi_enabled_before_scan";
    public static final String WAS_BT_ENABLED_BEFORE_SCAN = "was_bt_enabled_before_scan";
    /* set to true if a scan is already on-going in the background. This variable is used only in background mode
   to handle the specific issue of a scan that does not complete before another scan that starts.
   When that happens, the initial card state is not correctly restored to its initial state.
   */
    public static final String ON_GOING_WIFI_BACKGROUND_SCAN = "on_going_wifi_background_scan";
    public static final String ON_GOING_BT_BACKGROUND_SCAN = "on_going_bt_background_scan";
    /*
    isLocation Running is true if a location measurement is currently running, that is
    LocationMonitor.startLocationMonitor() has been called
    */
    public static final String IS_LOCATION_SCAN_RUNNING = "is_location_scan_running";
    /*
    contains the time at which the last alarm has been scheduled, this is used by the
    watchdog to detect that alarms are no more running.
    */
    public static final String LAST_SCHEDULED_ALARM = "last_scheduled_alarm";

    // contains the time in ms of the last requested sync.
    public static final String LAST_REQUESTED_SYNC = "last_requested_sync";

    // Key for a shared preference that holds the timestamp when the last foreground service
    // background scan has been run. This is used to know if it is the first scan of the day.
    public static final String LAST_FOREGROUND_SCAN_SERVICE_RUN_AT = "last_foreground_scan_service_run_at";

    // This shared preference hold a counter that represents how many unique days a user really
    // interacted with the app.
    public static final String NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED = "nb_unique_days_main_activity_resumed";

    // This shared preference represents a timestamp in ms of the list time the
    // NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED shared preference was updated
    public static final String TIMESTAMP_LAST_TIME_NB_UNIQUE_DAYS_WAS_UPDATED = "timestamp_last_time_nb_unique_days_was_updated";

    // This shared preference hold a counter that represents how many unique hours a user really
    // interacted with the app.
    public static final String NB_UNIQUE_HOURS_MAIN_ACTIVITY_RESUMED = "nb_unique hours_main_activity_resumed";

    // This shared preference represents a timestamp in ms of the list time the
    // NB_UNIQUE_HOURS_MAIN_ACTIVITY_RESUMED shared preference was updated
    public static final String TIMESTAMP_LAST_TIME_NB_UNIQUE_HOURS_WAS_UPDATED = "timestamp_last_time_nb_unique_hours_was_updated";

    // This shared preference represents a timestamp in ms of the last time the
    // in-app review has been triggered (we have no guarantee it as been shown, but we attempted
    // to display it).
    public static final String TIMESTAMP_LAST_TIME_IN_APP_REVIEW_TRIGGERED = "timestamp_last_time_in_app_review_triggered";

    // This shared preference represents the number of times the in-app review has been triggered
    // (we have no guarantee it as been shown, but we attempted to display it).
    public static final String NB_TIMES_IN_APP_REVIEW_TRIGGERED = "nb_times_in_app_review_triggered";

    // ############################## Shared preferences (END) ###############################

    // used to signal in the DB a parsing error in CellularMonitor.onSignalStrengthsChanged()
    public static final int PARSING_DBM_ERROR = Integer.MAX_VALUE - 1;

    // Used to define an invalid orientation field. The 5 orientation fields should vary in [-1,1]
    // We must not use a Float.MAX_VALUE as it is very large and is a huge overhead for the
    // database to store.
    public static final float INVALID_ORIENTATION_FIELD = -99.0f;

    // Used to show that a Latitude or Longitude is invalid
    public static final double INVALID_LATITUDE = -1.0;
    public static final double INVALID_LONGITUDE = -1.0;

    // Used to show that a time (creation date of a signal) or timestamp is invalid
    public static final long INVALID_TIME = -1;

    // Used when we don't known the value because we cannot measure it (before API 23)
    public static final int INVALID_IS_PASSPOINT_NETWORK = -1;
    public static final int INVALID_CENTERFREQ0 = -1;
    public static final int INVALID_CENTERFREQ1 = -1;
    public static final int INVALID_CHANNELWIDTH = -1;
    public static final int INVALID_WIFI_STANDARD = -1;

    // REQUEST_CODE used by startActivityForResult, we must use unique values. I start at 99 to avoid
    // risks of collision with other codes
    public static final int REQUEST_CODE_WIFI_SCAN_ALWAYS = 99;
    public static final int REQUEST_CODE_SEND_EMAIL = 100;
    public static final int REQUEST_CODE_SETTING_ACTIVITY = 101;
    public static final int RESULT_CODE_COLOR_CHANGED = 102;
    public static final int RESULT_CODE_FINISH_ES = 104;  // used when the app must be closed
    public static final int REQUEST_SYNC_ALARM = 105; // used to periodically push data to the server

    // Used when we go through the onboarding for the first time after the app is installed
    public static final int REQUEST_CODE_WELCOME_TERMS_OF_USE = 107;

    public static final int REQUEST_CODE_WELCOME_ACTIVITY = 108;
    public static final int REQUEST_CODE_ONBOARDING_ACTIVITY = 109;
    public static final int RESULT_CODE_ONBOARDING_DONE = 110;

    // Request code for creating a csv export zip file.
    public static final int RESULT_CODE_EXPORT_CSV_FILES = 111;

    // Constants used in the ErrorFragment
    public static final int ERROR_TYPE_NO_ERROR = 0;
    public static final int ERROR_TYPE_AIRPLANE_MODE = 1;
    public static final int ERROR_TYPE_NO_LOCATION_PERMISSION_DEVICE = 2;
    public static final int ERROR_TYPE_NO_LOCATION_PERMISSION_APP = 3;
    public static final int ERROR_TYPE_NO_LOCATION_PERMISSION_APP_BACKGROUND = 4;

    // MainActivity Measurement Fragment communication
    public static final String MAIN_ACTIVITY_LOCALE_ARG_KEY = "MAIN_ACTIVITY_LOCALE_ARG_KEY";

    // MainActivity Notification communication
    public static final String NOTIFICATION_ARG_KEY = "NOTIFICATION_ARG_KEY";

    // Key to identify the date when the notification is/was generated
    // This is used to take the user to the StatisticsFragment of the corresponding date
    public static final String NOTIFICATION_DATE_TIMEMILLIS_ARG_KEY = "NOTIFICATION_DATE_TIMEMILLIS_ARG_KEY";

    // here we define a code for a specific notification type
    public static final int NOTIFICATION_HIGH_EXPOSURE = 1;
    public static final int NOTIFICATION_FIRST_TIME_STATISTICS = 2;
    public static final int NOTIFICATION_NEW_SOURCE_DETECTED = 3;

    // Unique request codes for notification clicked pending intents
    public static final int NOTIFICATION_CLICKED_PENDING_INTENT_REQUEST_CODE_HIGH_EXPOSURE = 1;
    public static final int NOTIFICATION_CLICKED_PENDING_INTENT_REQUEST_CODE_FIRST_TIME_STAT = 2;
    public static final int NOTIFICATION_CLICKED_PENDING_INTENT_REQUEST_CODE_NEW_SOURCE_DETECTED = 3;

    // Unique request codes for notification deleted pending intents
    public static final int NOTIFICATION_DELETED_PENDING_INTENT_REQUEST_CODE_HIGH_EXPOSURE = 1;
    public static final int NOTIFICATION_DELETED_PENDING_INTENT_REQUEST_CODE_FIRST_TIME_STAT = 2;
    public static final int NOTIFICATION_DELETED_PENDING_INTENT_REQUEST_CODE_NEW_SOURCE_DETECTED = 3;

    // MainActivity HomeFragment/ErrorFragment communication
    public static final String MAIN_ACTIVITY_ERROR_TYPE_ARG_KEY = "MAIN_ACTIVITY_ERROR_TYPE_ARG_KEY";

    // MainActivity/HomeFragment -> ExposureDetailsFragment
    public static final String MAIN_ACTIVITY_EXPOSURE_DETAILS_ANTENNA_DISPLAY_ARG_KEY = "MAIN_ACTIVITY_EXPOSURE_DETAILS_ANTENNA_DISPLAY_ARG_KEY";
    public static final String MAIN_ACTIVITY_EXPOSURE_DETAILS_STAT_DAY_ARG_KEY = "MAIN_ACTIVITY_EXPOSURE_DETAILS_STAT_DAY_ARG_KEY";
    public static final String MAIN_ACTIVITY_EXPOSURE_DETAILS_SIGNAL_INDEX_ARG_KEY = "MAIN_ACTIVITY_EXPOSURE_DETAILS_SIGNAL_INDEX_ARG_KEY";

    // DailyStatSummaryFragment -> StatisticsFragment
    public static final String DAILY_STAT_SUMMARY_TO_STATISTICS_STAT_DAY_ARG_KEY = "DAILY_STAT_SUMMARY_TO_STATISTICS_STAT_DAY_ARG_KEY";
    public static final String DAILY_STAT_SUMMARY_TO_STATISTICS_IS_STAT_AVAILABLE_ARG_KEY = "DAILY_STAT_SUMMARY_TO_STATISTICS_IS_STAT_AVAILABLE_ARG_KEY";

    // MainActivity/AdviceFragment -> ArticleWebView Fragment
    public static final String MAIN_ACTIVITY_ARTICLE_WEBVIEW_FRAGMENT_URL_ARG_KEY = "MAIN_ACTIVITY_ARTICLE_WEBVIEW_FRAGMENT_URL_ARG_KEY";

    // Main activity on live timeline update broadcast action name.
    public static final String MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION =
            "fr.inria.es.electrosmart.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION";

    public static final String MAIN_ACTIVITY_LIVE_TIMELINE_UPDATE_NB_SLOTS_EXTRA_KEY =
            "MAIN_ACTIVITY_LIVE_TIMELINE_UPDATE_NB_SLOTS_EXTRA_KEY";


    /*
    Signal types used to define white zones

    Note that we do not define white zones for bluetooth. Indeed, BT devices are likely to move, and
    users can move with BT devices (such as in a car with a BT system). So BT white zones will be
    highly dynamic with time, which will be impossible to analyze.
    */
    public static final int WHITE_ZONE_CELLULAR = 1;
    public static final int WHITE_ZONE_WIFI = 2;
    //public static final int WHITE_ZONE_BLUETOOTH = 3;

    public static final int FOREGROUND_SERVICE_ID = 201;

    // Notification ID for a high exposure
    public static final int EXPOSURE_NOTIFICATION_ID = 202;

    // Notification ID for statistics first notification reminder
    public static final int STATISTICS_FIRST_NOTIFICATION_ID = 203;

    // Notification ID for statistics new source detected
    public static final int STATISTICS_NEW_SOURCE_DETECTED_NOTIFICATION_ID = 204;

    // Notification channels
    public static final String FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID = "es_channel";
    public static final String NOTIFICATION_EXPOSURE_CHANNEL_ID = "NOTIFICATION_EXPOSURE_CHANNEL_ID";
    public static final String NOTIFICATION_FEATURE_DISCOVERY_CHANNEL_ID = "NOTIFICATION_FEATURE_DISCOVERY_CHANNEL_ID";
    public static final String NOTIFICATION_NEW_SOURCE_DETECTED_CHANNEL_ID = "NOTIFICATION_NEW_SOURCE_DETECTED_CHANNEL_ID";

    // Main activity on window focus changed broadcast action name.
    public static final String MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION = "fr.inria.es.electrosmart.MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION";

    /*
    Constants used for the instrumentation of the app

    Settings events are coded as 1****
        Color settings is coded as 1001*
        Unit settings is coded as 1002*
        Binary settings are coded in 150**

    Activity events are coded as 5****
        Measurement fragment is coded as 501**
        Settings activity is coded as 502**
        Feedback activity is coded as 503**
        Help activity events 504**
        Metrics activity events 505**
        Tutorial activity events 506**
        Terms of use activity events 507**
        About activity events 508**
        Diagnosis fragment events 509**
        Test Owayn fragment events 510**
        Profile activity events 511**
        Home fragment events 512**
        Exposure details fragment events 513**
        Solutions fragment events 514**
        ArticleWebView fragment 515**
        Advice fragment 516**
        Statistics fragment 517**
        DailyStatSummaryFragment 518**
        OnBoardingActivity

     Measurement scheduler is coded as 599**
        Foreground is coded as 59900
        Background is coded as 59901

     Misc. events are coded as 6****
     */
    // settings events 1****


    public static final int EVENT_SETTINGS_UNIT_ESCORE = 10020;
    public static final int EVENT_SETTINGS_UNIT_DBM = 10021;
    public static final int EVENT_SETTINGS_UNIT_WATT = 10022;
    public static final int EVENT_SETTINGS_ADVANCED_MODE_ON = 15000;
    public static final int EVENT_SETTINGS_ADVANCED_MODE_OFF = 15001;
    public static final int EVENT_SETTINGS_NOTIFICATION_EXPOSURE_ON = 15002;
    public static final int EVENT_SETTINGS_NOTIFICATION_EXPOSURE_OFF = 15003;
    public static final int EVENT_SETTINGS_SHOW_INSTRUMENT_ON = 15004;
    public static final int EVENT_SETTINGS_SHOW_INSTRUMENT_OFF = 15005;
    public static final int EVENT_SETTINGS_NOTIFICATION_NEW_SOURCE_ON = 15006;
    public static final int EVENT_SETTINGS_NOTIFICATION_NEW_SOURCE_OFF = 15007;
    public static final int EVENT_SETTINGS_ENABLE_BT_AUTOMATICALLY_ON = 15008;
    public static final int EVENT_SETTINGS_ENABLE_BT_AUTOMATICALLY_OFF = 15009;

    // Measurement fragment event 501**
    public static final int EVENT_MEASUREMENT_FRAGMENT_ON_RESUME = 50100;
    public static final int EVENT_MEASUREMENT_FRAGMENT_ON_PAUSE = 50101;
    public static final int EVENT_MEASUREMENT_FRAGMENT_ON_SELECTED_METRIC_DIALOG_START = 50152;
    public static final int EVENT_MEASUREMENT_FRAGMENT_ON_SELECTED_METRIC_DIALOG_STOP = 50153;
    public static final int EVENT_MEASUREMENT_FRAGMENT_ON_QUICK_JUMP_DIALOG_START = 50154;
    public static final int EVENT_MEASUREMENT_FRAGMENT_ON_QUICK_JUMP_DIALOG_STOP = 50155;

    // Setting activity events 502**
    public static final int EVENT_SETTINGS_ACTIVITY_ON_RESUME = 50200;
    public static final int EVENT_SETTINGS_ACTIVITY_ON_PAUSE = 50201;

    // Feedback activity events 503**
    public static final int EVENT_FEEDBACK_ACTIVITY_ON_RESUME = 50300;
    public static final int EVENT_FEEDBACK_ACTIVITY_ON_PAUSE = 50301;

    // Help activity events 504**
    public static final int EVENT_HELP_ACTIVITY_ON_RESUME = 50400;
    public static final int EVENT_HELP_ACTIVITY_ON_PAUSE = 50401;

    // Metrics activity events 505**
    public static final int EVENT_METRICS_ACTIVITY_ON_RESUME = 50500;
    public static final int EVENT_METRICS_ACTIVITY_ON_PAUSE = 50501;

    // Terms of use activity events 507**
    public static final int EVENT_TERMS_OF_USE_ACTIVITY_ON_RESUME = 50700;
    public static final int EVENT_TERMS_OF_USE_ACTIVITY_ON_PAUSE = 50701;

    // About activity events 508**
    public static final int EVENT_ABOUT_ACTIVITY_ON_RESUME = 50800;
    public static final int EVENT_ABOUT_ACTIVITY_ON_PAUSE = 50801;

    // Test Owayn fragment events 510**
    public static final int EVENT_OWAYN_FRAGMENT_ON_RESUME = 51000;
    public static final int EVENT_OWAYN_FRAGMENT_ON_PAUSE = 51001;

    // Profile activity events 511**
    public static final int EVENT_PROFILE_ACTIVITY_ON_RESUME = 51100;
    public static final int EVENT_PROFILE_ACTIVITY_ON_PAUSE = 51101;

    // Home fragment events 512**
    public static final int EVENT_HOME_FRAGMENT_ON_RESUME = 51200;
    public static final int EVENT_HOME_FRAGMENT_ON_PAUSE = 51201;

    // Exposure details fragment events 513**
    public static final int EVENT_EXPOSURE_DETAILS_FRAGMENT_ON_RESUME = 51300;
    public static final int EVENT_EXPOSURE_DETAILS_FRAGMENT_ON_PAUSE = 51301;

    // Solutions fragment events 514**
    public static final int EVENT_SOLUTIONS_FRAGMENT_ON_RESUME = 51400;
    public static final int EVENT_SOLUTIONS_FRAGMENT_ON_PAUSE = 51401;

    // ArticleWebView fragment events 515**
    public static final int EVENT_ARTICLE_WEBVIEW_FRAGMENT_ON_RESUME = 51500;
    public static final int EVENT_ARTICLE_WEBVIEW_FRAGMENT_ON_PAUSE = 51501;

    // Advice fragment events 516**
    public static final int EVENT_ADVICE_FRAGMENT_ON_RESUME = 51600;
    public static final int EVENT_ADVICE_FRAGMENT_ON_PAUSE = 51601;

    // Statistics fragment events 517**
    public static final int EVENT_STATISTICS_FRAGMENT_ON_RESUME = 51700;
    public static final int EVENT_STATISTICS_FRAGMENT_ON_PAUSE = 51701;

    // DailyStatSummary fragment events 518**
    public static final int EVENT_DAILY_STAT_SUMMARY_FRAGMENT_ON_RESUME = 51800;
    public static final int EVENT_DAILY_STAT_SUMMARY_FRAGMENT_ON_PAUSE = 51801;


    // Measurement scheduler events 599**
    public static final int EVENT_SCHEDULER_FOREGROUND = 59900;
    public static final int EVENT_SCHEDULER_BACKGROUND = 59901;

    // Misc. events 6****
    public static final int EVENT_DEVICE_BOOTED = 60000;
    public static final int EVENT_APP_STARTED = 60001;
    public static final int EVENT_USER_TAPPED_TELL_YOUR_FRIENDS = 60100;
    public static final int EVENT_USER_TAPPED_ENCOURAGE_US = 60101;


    // Notification events 602**
    public static final int EVENT_NOTIFICATION_HIGH_EXPOSURE_DELETE = 60200;
    public static final int EVENT_NOTIFICATION_HIGH_EXPOSURE_CLICK = 60201;
    public static final int EVENT_NOTIFICATION_FIRST_TIME_STATISTICS_CLICK = 60202;
    public static final int EVENT_NOTIFICATION_FIRST_TIME_STATISTICS_DELETE = 60203;
    public static final int EVENT_NOTIFICATION_NEW_SOURCE_DETECTED_CLICK = 60204;
    public static final int EVENT_NOTIFICATION_NEW_SOURCE_DETECTED_DELETE = 60205;


    // Solutions events 603**
    public static final int EVENT_SOLUTIONS_OWAYN_LEARN_MORE_CLICKED = 60300;
    public static final int EVENT_SOLUTIONS_OWAYN_TEST_CLICKED = 60301;
    public static final int EVENT_SOLUTIONS_SYBOX_LEARN_MORE_CLICKED = 60310;
    public static final int EVENT_SOLUTIONS_SYBOX_TEST_CLICKED = 60311;
    public static final int EVENT_SOLUTIONS_SUGGEST_CLICKED = 60350;


}

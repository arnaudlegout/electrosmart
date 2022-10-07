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


import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.monitors.CellularMonitor;
import fr.inria.es.electrosmart.serversync.DataUploader;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.BluetoothProperty;
import fr.inria.es.electrosmart.signalproperties.CdmaProperty;
import fr.inria.es.electrosmart.signalproperties.GsmProperty;
import fr.inria.es.electrosmart.signalproperties.LteProperty;
import fr.inria.es.electrosmart.signalproperties.NewRadioProperty;
import fr.inria.es.electrosmart.signalproperties.WcdmaProperty;
import fr.inria.es.electrosmart.signalproperties.WifiGroupProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;

/**
 * This class contains various static methods used as tools in the code.
 */
public class Tools {

    public static final String COLOR_HIGH = "color_high";
    public static final String COLOR_MEDIUM = "color_medium";
    public static final String COLOR_LOW = "color_low";
    public static final String COLOR_NO_SIGNAL = "color_no_signal";
    public static final String COLOR_STATUS_BAR = "color_status_bar";
    public static final String COLOR_TITLE_BAR = "color_title_bar";
    public static final String COLOR_TILE = "color_tile";
    private static final String TAG = "Tools";

    /**
     * Converts dbm values to percentage considering that the dbm values are between
     * [Const.MIN_DISPLAYABLE_DBM, Const.MAX_DISPLAYABLE_DBM].
     *
     * @param dbm the dBm value to convert
     * @return a value between 0 and 100
     */
    public static int dbmToPercentage(int dbm) {
        if (dbm < Const.MIN_DISPLAYABLE_DBM) {
            return 0;
        } else if (Const.MAX_DISPLAYABLE_DBM < dbm) {
            return 100;
        } else {
            return Math.round(100 * ((float) dbm - Const.MIN_DISPLAYABLE_DBM) /
                    (Const.MAX_DISPLAYABLE_DBM - Const.MIN_DISPLAYABLE_DBM));
        }
    }

    /**
     * sort in place the custom array list based on dbm.
     * <p>
     * Arguments are not supposed to be null. In such a case we expect a NullPointerException. So
     * we MUST NOT handle the case of null arguments within the compare method.
     *
     * @param array   : arraylist to be sorted
     * @param reverse : true :- decreasing (-53 dbm to -100 dbm) , false :- increasing (-100 dbm to -53 dbm)
     */
    public static void arraySortDbm(List<BaseProperty> array, final boolean reverse) {
        Collections.sort(array, (first, second) -> {
            int x, y;
            if (reverse) {
                x = second.dbm;
                y = first.dbm;
            } else {
                x = first.dbm;
                y = second.dbm;
            }
            // inspired from Integer.compare that is only available for API 19 and higher.
            //
            // Note that using (first.dbm - second.dbm) is buggy as there is the risk of int
            // overflow. In this case, this will silently return a wrong result, as overflow
            // on primitive types does not produce an exception, but wrap numbers.
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        });
    }

    /**
     * This method is used to theme the status bar in light mode for API 21 and 22.
     * <p>
     * We consider this specific case because the possibility to have a white background in the
     * status bar was introduced in API 21, but the possibility to change the text in the status bar
     * in order to support a white background has been introduced in API 23. Therefore,
     * for API 21 and 22, if was possible to set a light background in the status bar, but not
     * to set a dark text. So the status bar is unreadable
     * <p>
     * In that case, we force a dark background for API 21 and 22 in light mode
     *
     * @param activity The activity on which we set the status bar
     */
    public static void setStatusBarColor(Activity activity) {
        Log.d(TAG, "in setStatusBarColor()");

        // if 21 <= SDK_INT < 23 AND we are in light mode
        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                if (!SettingsPreferenceFragment.get_PREF_KEY_DARK_MODE()) {
                    Log.d(TAG, "setStatusBarColor: Build.VERSION.SDK_INT: " + Build.VERSION.SDK_INT);
                    activity.getWindow().setStatusBarColor(
                            ContextCompat.getColor(activity, R.color.dark_gray));
                }
            }
        }
    }

    /**
     * Method to find the number of seconds between starting date and end date. No order between
     * the dates is assumed. The method returns the absolute number of seconds between dates.
     *
     * @param first  : starting date
     * @param second : end date
     * @return : absolute number of seconds between dates
     */
    public static Long secondsBetween(Date first, Date second) {
        return Math.abs(second.getTime() - first.getTime()) / 1000;
    }

    /**
     * Return for a dbm value the corresponding level on a scale from SignalLevel.LOW
     * to SignalLevel.HIGH. This level is used when we need a represent a given level on the user
     * interface (such as the exposition color).
     *
     * @param dbm The dbm value for which we evaluate the dBm level
     * @return The level from SignalLevel.LOW to SignalLevel.HIGH.
     */
    public static SignalLevel dbmToLevel(int dbm) {
        if (dbm < Const.RECOMMENDATION_LOW_THRESHOLD) {
            return SignalLevel.LOW;
        } else if (dbm < Const.RECOMMENDATION_HIGH_THRESHOLD) {
            return SignalLevel.MODERATE;
        } else {
            return SignalLevel.HIGH;
        }
    }

    /**
     * Returns for a given Wifi frequency in MHz the corresponding channel.
     * This method works for both 2.4GHz and 5GHz
     *
     * @param freq The frequency in MHz (from 2412 to 5825)
     * @return the channel or 0 is the frequency is out of the range from 2412 to 5825
     */
    public static int convertWifiFrequencyToChannel(int freq) {
        switch (freq) {
            case 0:
                return 0;
            // Wifi 2.4GHz
            case 2412:
                return 1;
            case 2417:
                return 2;
            case 2422:
                return 3;
            case 2427:
                return 4;
            case 2432:
                return 5;
            case 2437:
                return 6;
            case 2442:
                return 7;
            case 2447:
                return 8;
            case 2452:
                return 9;
            case 2457:
                return 10;
            case 2462:
                return 11;
            case 2467:
                return 12;
            case 2472:
                return 13;
            case 2484:
                return 14;
            //Wifi 5GHz
            case 5180:
                return 36;
            case 5200:
                return 40;
            case 5220:
                return 44;
            case 5240:
                return 48;
            case 5260:
                return 52;
            case 5280:
                return 56;
            case 5300:
                return 60;
            case 5320:
                return 64;
            case 5500:
                return 100;
            case 5520:
                return 104;
            case 5540:
                return 108;
            case 5560:
                return 112;
            case 5580:
                return 116;
            case 5600:
                return 120;
            case 5620:
                return 124;
            case 5640:
                return 128;
            case 5660:
                return 132;
            case 5680:
                return 136;
            case 5700:
                return 140;
            case 5720:
                return 144;
            case 5745:
                return 149;
            case 5765:
                return 153;
            case 5785:
                return 157;
            case 5805:
                return 161;
            case 5825:
                return 165;
            default:
                return 0;
        }
    }

    /**
     * Returns a string representation of the network type
     *
     * @param networkType for which network type is returned
     * @return the name of the network type
     */
    public static String getNetworkTypeName(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return MainApplication.getContext().getResources().getString(R.string.cdma);
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return MainApplication.getContext().getResources().getString(R.string.iden);
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return MainApplication.getContext().getResources().getString(R.string.gprs);
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return MainApplication.getContext().getResources().getString(R.string.edge);
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return MainApplication.getContext().getResources().getString(R.string.onexrtt);
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return MainApplication.getContext().getResources().getString(R.string.evdo_o);
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return MainApplication.getContext().getResources().getString(R.string.evdo_a);
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return MainApplication.getContext().getResources().getString(R.string.evdo_b);
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return MainApplication.getContext().getResources().getString(R.string.umts);
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return MainApplication.getContext().getResources().getString(R.string.hspa);
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return MainApplication.getContext().getResources().getString(R.string.hsdpa);
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return MainApplication.getContext().getResources().getString(R.string.hsupa);
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return MainApplication.getContext().getResources().getString(R.string.hspap);
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return MainApplication.getContext().getResources().getString(R.string.ehrpd);
            case TelephonyManager.NETWORK_TYPE_LTE:
                return MainApplication.getContext().getResources().getString(R.string.lte);
            case TelephonyManager.NETWORK_TYPE_GSM:
                return MainApplication.getContext().getResources().getString(R.string.gsm);
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return MainApplication.getContext().getResources().getString(R.string.td_scdma);
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return MainApplication.getContext().getResources().getString(R.string.iwlan);
            case TelephonyManager.NETWORK_TYPE_NR:
                return MainApplication.getContext().getResources().getString(R.string.nr);
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                // MainApplication.getContext().getResources().getString(R.string.unknown);
                return "";
            default:
                return "";
        }
    }

    /**
     * We return the highest value among cdma_dbm and evdo_dbm that is valid
     * See CdmaProperty.java for an explanation of the dbm value in the CdmaProperty
     *
     * @param cdma_dbm the cdma_dbm value
     * @param evdo_dbm the evdo_dbm value
     * @return the maximum of the passed value that is valid
     */
    public static int getDbmForCdma(int cdma_dbm, int evdo_dbm) {
        if (!CellularMonitor.isValidDBMValueForCellularCdma(cdma_dbm) &&
                !CellularMonitor.isValidDBMValueForCellularCdma(evdo_dbm)) {
            return BaseProperty.UNAVAILABLE;
        } else if (!CellularMonitor.isValidDBMValueForCellularCdma(cdma_dbm)) {
            return evdo_dbm;
        } else if (!CellularMonitor.isValidDBMValueForCellularCdma(evdo_dbm)) {
            return cdma_dbm;
        } else {
            return Math.max(cdma_dbm, evdo_dbm);
        }
    }

    /**
     * Gets the app version number via package manager.
     *
     * @return the versionCode of the application defined in the gradle app module file,
     * or -1 in case of an exception.
     */
    public static int getAppVersionNumber() {
        int versionCode = -1;
        try {
            versionCode = MainApplication.getContext().getPackageManager().getPackageInfo(MainApplication.getContext().getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "PackageManager.NameNotFoundException");
        }
        return versionCode;
    }

    /**
     * Gets the app version name via package manager.
     *
     * @return the version name of the application defined in the gradle app module file,
     * or an empty string in case of an exception.
     */
    public static String getAppVersionName() {
        String versionName = "";
        try {
            versionName = MainApplication.getContext().getPackageManager().getPackageInfo(MainApplication.getContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "PackageManager.NameNotFoundException");
        }
        return versionName;
    }

    /**
     * return a new arraylist of BaseProperty with removed BaseProperty object with the isValidSignal
     * field set to false
     *
     * @param rawEsProperties : arraylist that needs to be filtered
     * @return filtered arraylist
     */
    public static List<BaseProperty> removeEsPropertiesWithInvalidSignals(List<BaseProperty> rawEsProperties) {
        List<BaseProperty> filteredEsProperties = new ArrayList<>();
        if (rawEsProperties != null) {
            for (BaseProperty es : rawEsProperties) {
                if (es.isValidSignal) {
                    filteredEsProperties.add(es);
                }
            }
        }
        return filteredEsProperties;
    }

    /**
     * returns a boolean set to True if location coarse is granted and False otherwise. The value
     * returned uses the ContextCompat, so it works for all version of Android down to API 4.
     *
     * @param context the package context
     * @return a boolean set to True if location coarse is granted and False otherwise
     */
    public static boolean isAccessCoarseLocationGranted(@NonNull Context context) {
        int permissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * returns a boolean set to True if location fine is granted and False otherwise. The value
     * returned uses the ContextCompat, so it works for all version of Android down to API 4.
     *
     * @param context the package context
     * @return a boolean set to True if location fine is granted and False otherwise
     */
    public static boolean isAccessFineLocationGranted(@NonNull Context context) {
        int permissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns true if background location permission has been granted and false otherwise.
     * NOTE: for versions lower than Android 10, returns always true
     *
     * @param context the calling activity context
     * @return true if background location permission has been granted and false otherwise
     */
    public static boolean isAccessBackgroundLocationGranted(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int permissionCheck = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            return permissionCheck == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    /**
     * Returns the exposition as a string with its unit. The metric is selected by the user in
     * the preferences
     *
     * @param dbm        the dBm value used to compute the correct metric
     * @param appendUnit whether to add the unit or not
     * @return a string representation of the exposition
     */
    public static String getExpositionInCurrentMetric(int dbm, boolean appendUnit) {
        String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
        String exposure = "";
        if (pref.equals(MainApplication.getContext().getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
            exposure = Integer.toString(getExposureScore(dbm));
            if (appendUnit) {
                exposure = MainApplication.getContext().getString(R.string.exposure_metric) + " "
                        + exposure + MainApplication.getContext().getString(R.string.exposure_score_scale);
            }
        } else if (pref.equals(MainApplication.getContext().getString(R.string.PREF_VALUE_DBM_METRIC))) {
            exposure = Integer.toString(dbm);
            if (appendUnit) {
                exposure += " " + MainApplication.getContext().getString(R.string.dbm_metric);
            }
        } else if (pref.equals(MainApplication.getContext().getString(R.string.PREF_VALUE_POWER_METRIC))) {
            exposure = dBmToWattWithSIPrefix(dbm, true);
        }
        return exposure;
    }

    /**
     * Removes any additional seconds to return a timestamp that is modulo
     * {@code Const.HISTORY_TIME_GAP} seconds to be used for history mode.
     *
     * @param timestamp any timestamp in seconds
     * @return a modulo {@code Const.HISTORY_TIME_GAP} seconds timestamp
     */
    public static long normalizeHistoryTimestamp(long timestamp) {
        return timestamp - timestamp % Const.HISTORY_TIME_GAP;
    }

    /**
     * Normalizes the current timestamp to generate a history mode timestamp.
     * {@see #normalizeHistoryTimestamp()}
     *
     * @return the current timestamp in seconds normalized.
     */
    public static long getHistoryCurrentTimestamp() {
        return normalizeHistoryTimestamp(System.currentTimeMillis() / 1000);
    }

    /**
     * Return a timestamp that is modulo {@code Const.LIVE_TIME_GAP} seconds to be used
     * for live mode.
     *
     * @param timestamp any timestamp in seconds
     * @return a modulo {@code Const.LIVE_TIME_GAP} seconds timestamp
     */
    private static long normalizeLiveTimestamp(long timestamp) {
        return timestamp - timestamp % Const.LIVE_TIME_GAP;
    }

    /**
     * Normalizes the current timestamp to generate a live mode timestamp that is
     * modulo {@code Const.LIVE_TIME_GAP}
     * <p>
     * {@see #normalizeLiveTimestamp()}
     *
     * @return the current timestamp in seconds normalized.
     */
    public static long getLiveCurrentTimestamp() {
        return normalizeLiveTimestamp(System.currentTimeMillis() / 1000);
    }

    public static long oldestTimestampInDb() {
        return normalizeHistoryTimestamp(oldestSignalInDb() / 1000);
    }

    /**
     * returns the timestamp of the oldest entry in the DB and 0 is there is no entry yet.
     *
     * @return timestamp of the oldest entry in the DB (unix EPOCH in milliseconds) or 0
     */
    public static long oldestSignalInDb() {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return Long.parseLong(
                sharedPreferences.getString(Const.DATE_OLDEST_SIGNAL_IN_DB, "0")
        );
    }

    /**
     * returns the timestamp of the first entry in the DB and 0 is there is no entry yet.
     * <p>
     * Note that Const.DATE_FIRST_ENTRY_IN_DB is never updated once the DB is created,
     * so Const.DATE_OLDEST_SIGNAL_IN_DB can be much more recent than Const.DATE_FIRST_ENTRY_IN_DB
     * that corresponds basically to the app installation date.
     *
     * @return timestamp of the first entry in the DB (unix EPOCH in milliseconds) or 0
     */
    public static long firstEntryInDb() {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return Long.parseLong(
                sharedPreferences.getString(Const.DATE_FIRST_ENTRY_IN_DB, "0")
        );
    }

    /**
     * @return Returns the {@link Const#LAST_FOREGROUND_SCAN_SERVICE_RUN_AT} shared preference
     * value. Returns 0 if not yet set.
     */
    public static long getLastForegroundScanServiceRunAtTimeStamp() {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return sharedPreferences.getLong(Const.LAST_FOREGROUND_SCAN_SERVICE_RUN_AT, 0L);
    }

    /**
     * Checks for the Const.IS_FIRST_TIME_STATISTICS_NOTIFICATION_SHOWN shared preference
     * and returns its boolean status. Returns false if not yet set.
     *
     * @return false if IS_FIRST_TIME_STATISTICS_NOTIFICATION_SHOWN shared preference is not set
     * or set to false, returns true otherwise.
     */
    public static boolean isFirstTimeStatisticsNotificationShown() {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Const.IS_FIRST_TIME_STATISTICS_NOTIFICATION_SHOWN, false);
    }

    /**
     * Retrieves the value of the shared preference {@link Const#SHOW_EXPOSURE_SCALE} and returns it.
     * By default (if the shared preference is not yet set) we show the exposure scale.
     *
     * @return The boolean value of the shared preference {@link Const#SHOW_EXPOSURE_SCALE}
     */
    public static boolean getShowExposureScale() {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Const.SHOW_EXPOSURE_SCALE, true);
    }

    /**
     * Set the {@link Const#SHOW_EXPOSURE_SCALE} shared preference as per the passed boolean value
     *
     * @param showExposureScale Whether to show the exposure scale or not
     */
    public static void setShowExposureScale(boolean showExposureScale) {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(Const.SHOW_EXPOSURE_SCALE, showExposureScale).apply();
    }

    /**
     * Returns the maximum number of days that can be stored in the DB (in day count)
     * <p>
     * Retrieves the value of the shared preference {@link Const#NB_DAYS_IN_HISTORY} and returns it.
     * By default (if the shared preference is not yet set) we set this value to the value contained
     * in R.string.DEFAULT_NB_DAYS_IN_HISTORY.
     *
     * @return The number of days to keep in the history
     */
    public static int getNumberOfDaysInHistory() {
        int defaultNumberOfDaysInHistory = Integer.parseInt(MainApplication.getContext().getString(
                R.string.DEFAULT_NB_DAYS_IN_HISTORY));
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(Const.NB_DAYS_IN_HISTORY, defaultNumberOfDaysInHistory);
    }

    /**
     * Set the {@link Const#NB_DAYS_IN_HISTORY} shared preference with the passed number of days
     *
     * @param numOfDays Number of days
     */
    public static void setNumberOfDaysInHistory(int numOfDays) {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(Const.NB_DAYS_IN_HISTORY, numOfDays).apply();
    }

    /**
     * Returns the maximum number of days that can be stored in the DB in milliseconds
     *
     * @return number of days in milliseconds
     */
    public static long getNumberOfDaysInHistoryInMs() {
        //     |      one day       | nb days |
        return 1000L * 60 * 60 * 24 * getNumberOfDaysInHistory();  // in milliseconds
    }

    /**
     * Returns the number of days in milliseconds of top5signals history data to be retained
     * in the DB
     *
     * @return number of days in milliseconds
     */
    public static long getMaxNumOfDaysOfTop5Signals() {
        //     |      one day       | nb days |
        return 1000L * 60 * 60 * 24 * getNumberOfDaysInHistory();  // in milliseconds
    }

    /**
     * Take a MAC address in the form of a String and convert it to a Long object. We can revert
     * back to the String representation by calling Long2MAC on this Long representation.
     *
     * @param mac The String MAC address to convert in the form AA:AA:AA:AA:AA:AA (6 octets 48 bits)
     * @return The Long representation of the MAC address
     */
    public static Long MAC2Long(String mac) {
        return Long.parseLong(mac.replace(":", ""), 16);
    }

    /**
     * Take a Long representation of a MAC address (as returned by MAC2Long) and convert it back
     * to the String representation in the form AA:AA:AA:AA:AA:AA (6 octets 48 bits)
     *
     * @param longMac The Long representation of the MAC address
     * @return The String representation in the form AA:AA:AA:AA:AA:AA
     */
    public static String Long2MAC(Long longMac) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 48; i += 8) {
            String string = Long.toHexString((longMac >> (40 - i)) & 0xff);
            if (string.length() < 2) {
                builder.append("0");
            }
            builder.append(string);
            if (i != 40) {
                builder.append(":");
            }
        }
        return builder.toString();
    }

    /**
     * The method returns true is the airplane mode is activated, false otherwise.
     * <p/>
     * This method supports the API changes that happened in API 17, so this method works from API 1
     * to the latest one.
     *
     * @param context the context
     * @return true is the airplane mode is activated, false otherwise.
     */
    public static boolean isAirplaneModeActivated(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }

    /**
     * return the resource color (R.color.<color>) for a color level
     *
     * @param color can be in (COLOR_HIGH, COLOR_MEDIUM, COLOR_LOW, COLOR_NO_SIGNAL)
     * @return an int corresponding to a color resource R.color
     */
    public static int themedColor(String color) {
        if (color.equals(COLOR_HIGH)) {
            return R.color.default_blue;
        } else if (color.equals(COLOR_MEDIUM)) {
            return R.color.default_blue;
        } else if (color.equals(COLOR_LOW)) {
            return R.color.default_blue;
        }
        // if nothing matches, we return the default_background
        return R.color.default_blue;
    }

    /**
     * Force the application to use the dark mode, or default to the system preferences
     *
     * @param is_dark_mode set to true if the dark mode must be forced, false if we respect
     *                     the system preferences
     */
    public static void setDarkMode(boolean is_dark_mode) {
        Log.d(TAG, "in setDarkMode()");
        if (is_dark_mode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Set the theme of the given progress bar instance
     *
     * @param context     The context of the activity for which we theme the progress bar
     * @param progressBar The progress bar to be themed
     */
    public static void setProgressHorizontalBarTheme(Context context, ProgressBar progressBar) {
        progressBar.setProgressDrawable(ContextCompat.getDrawable(context, R.drawable.progress_horizontal_blue_rounded));
    }

    /**
     * convert a dBm value in int to its W string representation (including the unit mW, uW, nW,
     * pW, fW, aW)
     * <p>
     * The range of acceptable values for this method is [-210, 29]dBm which corresponds to
     * [1.0yW, 794.3mW]. Outside of this range the method returns a string na.
     * <p>
     * IMPORTANT: this method must return the value separated from the unit by a space, e.g.,
     * 3.0 mW, or n/a if the value is out of bounds.
     * MeasurementFragment.updateTitleBar() relies on this convention to make the parsing of the string.
     *
     * @param dbm          the dBm value to be converted in int in [-210, 29]
     * @param isValueFloat if set to true, the return value will contains a value with .1f decimal
     *                     otherwise, it will contains a .0f decimal (that is a int)
     * @return the String representation of the conversion in W including the unit
     */
    public static String dBmToWattWithSIPrefix(int dbm, boolean isValueFloat) {
        // convert the dBm to mW according to https://en.wikipedia.org/wiki/DBm
        double mw = Math.pow(10.0, dbm / 10.0);

        String defaultValue = MainApplication.getContext().getResources().getString(R.string.na);

        if (mw == 0) {
            return defaultValue;
        }
        char[] prefix = {'m', '\u03bc', 'n', 'p', 'f', 'a', 'z', 'y'};

        int degree = (int) Math.floor(Math.log10(mw) / 3);
        double scaled = mw * Math.pow(1000, -degree);
        if (degree <= 0 && Math.abs(degree) < prefix.length) {
            String format;
            if (isValueFloat) {
                format = "%.1f %s%s";
            } else {
                format = "%.0f %s%s";
            }
            return String.format(Locale.getDefault(), format, scaled, prefix[-degree], 'W');
        } else {
            return defaultValue;
        }
    }

    /**
     * Return the result from dBmToWattWithSIPrefix in the form of an array of String,
     * with the value in the first element and the unit in the second element. If ever, the value
     * cannot be converted, the method will return an empty array. This way, we can safely assume
     * that if the array is not empty we have a valid result.
     *
     * @param dbm          the dBm value to be converted in int in [-210, 29]
     * @param isValueFloat if set to true, the return value will contains a value with .1f decimal
     *                     otherwise, it will contains a .0f decimal (that is a int)
     * @return a String array
     */
    public static String[] dBmToWattWithSIPrefixArray(int dbm, boolean isValueFloat) {
        String[] watt = dBmToWattWithSIPrefix(dbm, isValueFloat).split(" ");
        if (watt.length == 1) {
            return new String[0];
        } else {
            return watt;
        }
    }

    /**
     * convert a dBm value to mWatts
     *
     * @param dbm the dBm value to be converted in milliWatt
     * @return the double representing the value in milliWatt
     */
    public static double dbmToMilliWatt(double dbm) {
        // convert the dBm to mW according to https://en.wikipedia.org/wiki/DBm
        return Math.pow(10.0, dbm / 10.0);

    }

    /**
     * convert a milliWatt value to dbm
     *
     * @param mw the milliWatt value to be converted in dbm
     * @return the double representing the value in dbm, and Const.MIN_DBM_FOR_ROOT_SCORE -1
     * if mw is 0.
     */
    public static int milliWattToDbm(double mw) {
        int result;
        if (mw > 0) {
            // convert the dBm to mW according to https://en.wikipedia.org/wiki/DBm
            result = (int) Math.round(10.0 * Math.log10(mw));
        } else {
            // we cannot compute the log of 0 or a negative value so we return
            // Const.MIN_DBM_FOR_ROOT_SCORE - 1 that is an invalid value
            result = Const.MIN_DBM_FOR_ROOT_SCORE - 1;
        }
        return result;
    }

    /**
     * Return an exposition score based on a dBm value.
     * <p>
     * The min exposition score is the minimum the app can measure for all signals. Two points in
     * the exposition score corresponds to an exposition that is roughly doubled,
     * that is, 3dBm, 10**(3/10)=1.99526. Conversely, reducing the score by two means dividing
     * by roughly two the exposition (that is, -3dBm, 10**(-3/10)=0.50119).
     * <p>
     * This score can go from 0 to 92 (assuming the maximum measured dBm is -1). However, we
     * represent this score on a scale of 100. The rational is that a scale of 100 is a well
     * understood scale (e.g., the Yuka score). In addition, the fact that we cannot reach 100
     * is a way to reduce the anxiety linked to a high score. Note, however, that this is an
     * hypothesis that has not been validated.
     * <p>
     * For the cumulative dbm value, the score is a sum of valid dbm values. Therefore, it can
     * exceeds 92. In case the score is larger than 100 (very unlikely), we cap at 100. We do
     * not apply a restriction on dbm and mW values are those ones are not naturally capped.
     *
     * @param dbm The dbm value to be converted to an exposition score
     * @return the exposition score
     */
    public static int getExposureScore(double dbm) {
        int min_score = Const.MIN_DBM_FOR_ROOT_SCORE;
        if (dbm < min_score) {
            return 0;
        } else {
            int score = (int) Math.floor(2 * (dbm - min_score) / 3);
            return Math.min(score, 100);
        }
    }

    /**
     * A utility function to generate an alert dialog for a given activity with the given title and
     * text. This alert dialog contains a single OK button that simply closes the dialog.
     * It is basically an informational alert dialog.
     *
     * @param context The context object
     * @param title   The title to be used in the alert dialog
     * @param text    The text to be shown in the alert dialog
     */

    public static void createInformationalDialog(Context context, String title, String text) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialogStyle));

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(text).setTitle(title);

        builder.setPositiveButton(R.string.ok_button, (dialog, id) -> {
            // User clicked OK button
            dialog.cancel();
        });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * A utility function to create a Yes/No dialog which obliges the user to respond to the
     * question asked. The function sets the cancelable property of the dialog to false.
     *
     * @param activity                  The given activity
     * @param title                     The title to be used in the dialog
     * @param text                      The text to be shown in the dialog
     * @param okButtonText              The text to be shown on the OK button
     * @param okButtonClickListener     An instance of DialogInterface.OnClickListener that tells what
     *                                  to do when OK button is clicked
     * @param cancelButtonText          The text to be shown on the Cancel button
     * @param cancelButtonClickListener An instance of DialogInterface.OnClickListener that tells what
     *                                  to do when Cancel button is clicked
     * @return The appcompat AlertDialog instance that was created
     */
    public static AlertDialog createYesNoDialog(Activity activity, String title, String text, String okButtonText,
                                                DialogInterface.OnClickListener okButtonClickListener,
                                                String cancelButtonText, DialogInterface.OnClickListener cancelButtonClickListener) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.AlertDialogStyle));

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(text).setTitle(title);

        // 3. Set positive and negative buttons text and click listeners
        builder.setPositiveButton(okButtonText, okButtonClickListener);
        builder.setNegativeButton(cancelButtonText, cancelButtonClickListener);

        // 4. Set cancelable false for the dialog by default
        builder.setCancelable(false);

        // 5. Show the alert dialog
        return builder.show();
    }

    /**
     * This method converts the string representation of the preferences
     * of {@link SettingsPreferenceFragment} to the corresponding integer so that it can be
     * stored by the app instrumentation in the DB.
     *
     * @param str The string representing the preference to be looked up
     * @return int An integer constant corresponding to supplied EventType string
     */
    public static int getEventTypeValueFromString(String str) {
        if (str.equals(MainApplication.getContext().getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
            return Const.EVENT_SETTINGS_UNIT_ESCORE;
        } else if (str.equals(MainApplication.getContext().getString(R.string.PREF_VALUE_DBM_METRIC))) {
            return Const.EVENT_SETTINGS_UNIT_DBM;
        } else if (str.equals(MainApplication.getContext().getString(R.string.PREF_VALUE_POWER_METRIC))) {
            return Const.EVENT_SETTINGS_UNIT_WATT;
        }
        return 0;
    }

    public static void setGroupIconIndicatorToRight(MainActivity activity,
                                                    ExpandableListView list) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            list.setIndicatorBounds(width - GetPixelFromDips(activity, 35),
                    width - GetPixelFromDips(activity, 5));
        } else {
            if (list != null)
                list.setIndicatorBoundsRelative(width - GetPixelFromDips(activity, 35),
                        width - GetPixelFromDips(activity, 5));
        }
    }

    private static int GetPixelFromDips(Context context, float pixels) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pixels * scale + 0.5f);
    }

    /**
     * Returns the string representation of a value if it is in [min, max] and R.string.na
     * if the value is not in that range.
     * <p>
     *
     * @param context the context used to retrieve string resources
     * @param value   the value to evaluate within the range [min, max]
     * @param min     the lower bound of the interval
     * @param max     the upper bound of the interval
     * @return returns either the string representation of the integer or the string representation
     * of R.string.na if value is not in [min, max]
     */
    public static String getStringForValueInRange(Context context, int value, int min, int max) {
        if (min <= value && value <= max) {
            return String.valueOf(value);
        } else {
            return context.getResources().getString(R.string.na);
        }
    }

    /**
     * Same as above just with long parameters
     *
     * @param context
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static String getStringForValueInRange(Context context, long value, long min, long max) {
        if (min <= value && value <= max) {
            return String.valueOf(value);
        } else {
            return context.getResources().getString(R.string.na);
        }
    }

    /**
     * return the eNB based on the ECI for LTE
     * <p>
     * The ECI (28bits) = eNB(20bits) + CID(8bits)
     *
     * @param eci the ECI in int format
     * @return the string representation of the eNB
     */
    public static String getEnbFromEci(int eci) {
        return String.valueOf((eci & 0x0FFFFF00) >>> 8);

    }

    /**
     * return the CID based on the ECI for LTE
     * <p>
     * The ECI (28bits) = eNB(20bits) + CID(8bits)
     *
     * @param eci the ECI in int format
     * @return the string representation of the CID
     */
    public static String getCidFromEci(int eci) {
        return String.valueOf(eci & 0x000000FF);
    }

    /**
     * return the CID based on the UCID for 3G
     * <p>
     * The UCID (28bits) = RNC(12bits) + CID(16bits)
     *
     * @param ucid the UCID in int format
     * @return the string representation of the CID
     */
    public static String getCidFromUcid(int ucid) {
        return String.valueOf(ucid & 0x0000FFFF);
    }

    /**
     * return the RNC based on the UCID for 3G
     * <p>
     * The UCID (28bits) = RNC(12bits) + CID(16bits)
     *
     * @param ucid the UCID in int format
     * @return the string representation of the RNC
     */
    public static String getRncFromUcid(int ucid) {
        return String.valueOf((ucid & 0x0FFF0000) >>> 16);
    }

    /**
     * A utility function that returns the status of the device's WiFi background scan
     * setting.
     *
     * @return true if the device's WiFi background scan setting is ON and false otherwise
     */

    public static boolean isWiFiBackgroundScanON() {
        return (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                ((WifiManager) (MainApplication.getContext().getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE))).isScanAlwaysAvailable());
    }

    public static void easterEggDumbledore(Context context, int age) {
        if (age == 99) {
            Toast.makeText(context, context.getString(R.string.onboarding_age_99_easter_egg),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Returns true if the location service is disabled in the device, false otherwise
     *
     * @param context The app context
     * @return true if the location service is disabled in the device, false otherwise
     */
    public static boolean isLocationDisabled(Context context) {
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !((LocationManager) context.getSystemService(Context.LOCATION_SERVICE))
                        .isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !((LocationManager) context.getSystemService(Context.LOCATION_SERVICE))
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * A refactored method to show the application specific settings
     * (ACTION_APPLICATION_DETAILS_SETTINGS) activity.
     *
     * @param context the app's context
     */
    public static void showAppSettingsActivity(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    /**
     * Call this method to ask for the location app permission
     * <p>
     * Starting with Android Q, the ACCESS_FINE_LOCATION corresponds to the app location permission,
     * while the app is in use. We need to ask for the ACCESS_BACKGROUND_LOCATION to allow
     * app location permission all the time.
     * <p>
     * Starting with Android R, we must ask separately for fine and background location.
     * <p>
     * We apply the recommendations described here
     * http://developer.android.com/training/permissions/requesting.html
     * <p>
     * For a detailed explanation of our agreement workflow and design choice, refer to the
     * explanation provided in WelcomeTermsOfUseActivity.java and to
     * OnBoardingActivity.checkPermissionAndSDKAndLoadFragment()
     * <p>
     * Here is a detailed explanation of the logic we use for the location permissions
     * -------------------------------------------------------------------------------
     * <p>
     * We have three different states
     * 1) the user never granted a location permission
     * 2) the user denied the location permission
     * 3) the user denied the location permission and requested to never be asked again
     * <p>
     * The method ActivityCompat.requestPermissions() will only work for cases 1 and 2, but not
     * for 3. So we must be able to detect case 3 and in that case direct the user to the system
     * settings (this is the best we can do as there is no possibility to trigger a pop-up
     * for location permission if the user requested to never be asked again).
     * <p>
     * To detect case 3 we use the method ActivityCompat.shouldShowRequestPermissionRationale(), but
     * this method has a strange behavior that is not fully documented. Note that this method
     * maintains a state specific per permission.
     * We have three behaviors
     * a) In case the permission system pop-up has never been shown for this app (in the full app
     * history), or the user never selected a choice (whatever the choice) in this pop-up,
     * this method returns false.
     * b) In case the permission system pop-up has been shown for this app and the user
     * selected a choice (grant or deny) that does not contain "never ask again", this
     * method will always returns true
     * c) In case the permission system pop-up has been shown for this app and the user
     * denied the permission and selected "never ask again", this method will always returns false
     * <p>
     * So we see here that to know whether a user denied a permission with "never ask again",
     * so to detect case 3, we must be able to make a distinction between a and c.
     * For this reason we keep two shared variables Const.USER_DENIED_LOCATION_PERMISSION_FOREGROUND
     * and Const.USER_DENIED_LOCATION_PERMISSION_BACKGROUND that will be set to true if the user
     * deny a permission. As we only need to make a distinction between a and c, and "a" only
     * happens on the first permission pop-up, we never reset the shared variables to false.
     * <p>
     * IMPORTANT NOTE: when a permission is downgraded from the settings, the app process is
     * restarted, but when a permission is upgraded, the app process is simply resumed. This is
     * important when considering non persisted states to track permissions.
     * See https://developer.android.com/about/versions/12/approximate-location
     * "If the user downgrades your app's location access from precise to approximate, either from
     * the permission dialog or in system settings, the system restarts your app's process."
     * Later "Because this permission change is an upgrade, the system doesn't restart your app."
     *
     * @param context              fragment context
     * @param activity             current activity
     * @param requestForegroundLoc used only with SDK>=30. If true, we request a foreground location,
     *                             otherwise, a background location.
     */
    public static void grantLocationToApp(Context context, AppCompatActivity activity, boolean requestForegroundLoc) {
        Log.d(TAG, "in grantLocationToApp()");
        // Starting with Android Q, the access fine location permission corresponds location in
        // foreground only. We need an extra permission to allow app location in background.
        boolean permissionAccessFineLocationApproved = Tools.isAccessFineLocationGranted(context);

        SharedPreferences settings = context.getSharedPreferences(Const.SHARED_PREF_FILE,
                Context.MODE_PRIVATE);
        boolean hasUserDeniedFineForegroundLocationPermission = settings.getBoolean(
                Const.USER_DENIED_FINE_LOCATION_PERMISSION_FOREGROUND, false);

        boolean hasUserDeniedBackgroundLocationPermission = settings.getBoolean(
                Const.USER_DENIED_LOCATION_PERMISSION_BACKGROUND, false);

        boolean hasUserDeniedCoarseForegroundLocationPermission = settings.getBoolean(
                Const.USER_DENIED_COARSE_LOCATION_PERMISSION_FOREGROUND, false);

        Log.d(TAG, "grantLocationToApp: hasUserDeniedFineForegroundLocationPermission: " +
                hasUserDeniedFineForegroundLocationPermission + " hasUserDeniedCoarseForegroundLocationPermission: " +
                hasUserDeniedCoarseForegroundLocationPermission + " hasUserDeniedBackgroundLocationPermission: " +
                hasUserDeniedBackgroundLocationPermission);

        Log.d(TAG, "grantLocationToApp: ActivityCompat.shouldShowRequestPermissionRationale(activity, " +
                "Manifest.permission.ACCESS_FINE_LOCATION): " +
                ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION));
        Log.d(TAG, "grantLocationToApp: ActivityCompat.shouldShowRequestPermissionRationale(activity, " +
                "Manifest.permission.ACCESS_COARSE_LOCATION)" +
                ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_COARSE_LOCATION));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "grantLocationToApp: SDK >= S");

            if (requestForegroundLoc) {
                Log.d(TAG, "grantLocationToApp: We are running Android S+");
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) &&
                        hasUserDeniedFineForegroundLocationPermission &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity,
                                Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Log.d(TAG, "grantLocationToApp: fall back to Application Settings.");
                    Tools.showAppSettingsActivity(context);
                } else {
                    Log.d(TAG, "grantLocationToApp:  ask for foreground location permission");
                    // starting with Android S (API 31) we must ask both ACCESS_FINE_LOCATION and
                    // ACCESS_COARSE_LOCATION in the same request to request ACCESS_FINE_LOCATION
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION},
                            MainActivity.REQUEST_ACCESS_FINE_LOCATION);
                }
            } else {
                Log.d(TAG, "grantLocationToApp: foreground location permission already granted");
                if (Tools.isAccessBackgroundLocationGranted(context)) {
                    // App can access location both in the foreground and in the background.
                    Log.d(TAG, "grantLocationToApp: App can access both foreground and " +
                            "background location");
                } else {
                    // Check if user denied and chose never ask again. In such case, take the user
                    // to device's app settings to change permissions otherwise request the
                    // background location permission
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                            hasUserDeniedBackgroundLocationPermission) {
                        Log.d(TAG, "grantLocationToApp: fall back to Application Settings.");
                        // user denied flagging NEVER ASK AGAIN
                        // We fallback to using the application details settings and let the user
                        // enable the permission.
                        Tools.showAppSettingsActivity(context);
                    } else {
                        Log.d(TAG, "grantLocationToApp: ask for the background location permission");
                        ActivityCompat.requestPermissions(activity, new String[]{
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                MainActivity.REQUEST_ACCESS_BACKGROUND_LOCATION);
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            // Starting with R, we must ask separately Foreground and Background location permission.
            Log.d(TAG, "grantLocationToApp: SDK == R");

            if (requestForegroundLoc) {
                Log.d(TAG, "grantLocationToApp: We are running Android R");
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) &&
                        hasUserDeniedFineForegroundLocationPermission) {
                    Log.d(TAG, "grantLocationToApp: fall back to Application Settings.");
                    Tools.showAppSettingsActivity(context);
                } else {
                    Log.d(TAG, "grantLocationToApp:  ask for foreground location permission");
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MainActivity.REQUEST_ACCESS_FINE_LOCATION);
                }
            } else {
                Log.d(TAG, "grantLocationToApp: foreground location permission already granted");
                if (Tools.isAccessBackgroundLocationGranted(context)) {
                    // App can access location both in the foreground and in the background.
                    Log.d(TAG, "grantLocationToApp: App can access both foreground and " +
                            "background location");
                } else {
                    // Check if user denied and chose never ask again. In such case, take the user
                    // to device's app settings to change permissions otherwise request the
                    // background location permission
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                            hasUserDeniedBackgroundLocationPermission) {
                        Log.d(TAG, "grantLocationToApp: fall back to Application Settings.");
                        // user denied flagging NEVER ASK AGAIN
                        // We fallback to using the application details settings and let the user
                        // enable the permission.
                        Tools.showAppSettingsActivity(context);
                    } else {
                        Log.d(TAG, "grantLocationToApp: ask for the background location permission");
                        ActivityCompat.requestPermissions(activity, new String[]{
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                MainActivity.REQUEST_ACCESS_BACKGROUND_LOCATION);
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Starting with Android Q, we must manage the app location permission "all the time" and
            // "while in use"
            Log.d(TAG, "grantLocationToApp: SDK =  Q");
            if (permissionAccessFineLocationApproved) {
                Log.d(TAG, "grantLocationToApp: foreground location permission already granted");
                if (Tools.isAccessBackgroundLocationGranted(context)) {
                    // App can access location both in the foreground and in the background.
                    Log.d(TAG, "grantLocationToApp: App can access both foreground and " +
                            "background location");
                } else {
                    // Check if user denied and chose never ask again. In such case, take the user
                    // to device's app settings to change permissions otherwise request the
                    // background location permission
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION) &&
                            hasUserDeniedBackgroundLocationPermission) {
                        Log.d(TAG, "grantLocationToApp: fall back to Application Settings.");
                        // user denied flagging NEVER ASK AGAIN
                        // We fallback to using the application details settings and let the user
                        // enable the permission.
                        Tools.showAppSettingsActivity(context);
                    } else {
                        Log.d(TAG, "grantLocationToApp: ask for the background location permission");
                        ActivityCompat.requestPermissions(activity, new String[]{
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                MainActivity.REQUEST_ACCESS_BACKGROUND_LOCATION);
                    }
                }
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) &&
                        hasUserDeniedFineForegroundLocationPermission) {
                    Log.d(TAG, "grantLocationToApp: fall back to Application Settings.");
                    Tools.showAppSettingsActivity(context);
                } else {
                    Log.d(TAG, "grantLocationToApp:  ask for both foreground the background " +
                            "location permission");
                    ActivityCompat.requestPermissions(activity, new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            MainActivity.REQUEST_ACCESS_FINE_AND_BACKGROUND_LOCATION);
                }
            }
        } else {
            // we are before Android Q
            Log.d(TAG, "grantLocationToApp: M <= SDK < Q");
            if (!permissionAccessFineLocationApproved) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION) &&
                        hasUserDeniedFineForegroundLocationPermission) {
                    Log.d(TAG, "grantLocationToApp: fall back to Application Settings.");
                    Tools.showAppSettingsActivity(context);
                } else {
                    Log.d(TAG, "grantLocationToApp: requesting access fine location");
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MainActivity.REQUEST_ACCESS_FINE_LOCATION);
                }
            }
        }
    }

    /**
     * Method to call when a permission result arrives with the callback
     * ActivityCompat.onRequestPermissionsResult(). This method is used to set the three shared
     * preferences Const.USER_DENIED_FINE_LOCATION_PERMISSION_FOREGROUND,
     * Const.USER_DENIED_COARSE_LOCATION_PERMISSION_FOREGROUND, and
     * Const.USER_DENIED_LOCATION_PERMISSION_BACKGROUND
     * to keep track of when a user denies a location permission and ask to do not be asked
     * again.
     * <p>
     *
     * @param requestCode  the request code
     * @param permissions  the returned permissions array
     * @param grantResults the returned grant result array
     */
    public static void processRequestPermissionsResult(int requestCode,
                                                       @NonNull String[] permissions,
                                                       @NonNull int[] grantResults) {
        Log.d(TAG, "in processRequestPermissionsResult()");
        SharedPreferences settings = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();
        Log.d(TAG, "processRequestPermissionsResult: requestCode: " + requestCode +
                " permissions: " + permissions + " grantResults: " + grantResults);
        // If request is cancelled, the result arrays are empty.
        if ((requestCode == MainActivity.REQUEST_ACCESS_FINE_LOCATION) ||
                (requestCode == MainActivity.REQUEST_ACCESS_BACKGROUND_LOCATION) ||
                (requestCode == MainActivity.REQUEST_ACCESS_FINE_AND_BACKGROUND_LOCATION)) {
            Log.d(TAG, "processRequestPermissionsResult: we enter the if condition");
            boolean fineLocation = false;
            boolean coarseLocation = false;
            boolean backgroundLocation = false;
            for (int i = 0; i < grantResults.length; i++) {
                Log.d(TAG, "processRequestPermissionsResult: permissions[" + i + "]=" + permissions[i]);
                Log.d(TAG, "processRequestPermissionsResult: grantResults[" + i + "]=" + grantResults[i]);
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    fineLocation = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                    Log.d(TAG, "processRequestPermissionsResult: ACCESS_FINE_LOCATION permission = " + fineLocation);
                    if (fineLocation) {
                        Log.d(TAG, "processRequestPermissionsResult Permission ACCESS_FINE_LOCATION granted!");
                    } else {
                        Log.d(TAG, "processRequestPermissionsResult Permission ACCESS_FINE_LOCATION denied!");
                        edit.putBoolean(Const.USER_DENIED_FINE_LOCATION_PERMISSION_FOREGROUND, true).apply();
                    }
                } else if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    coarseLocation = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                    if (coarseLocation) {
                        Log.d(TAG, "processRequestPermissionsResult Permission ACCESS_COARSE_LOCATION granted!");
                    } else {
                        Log.d(TAG, "processRequestPermissionsResult Permission ACCESS_COARSE_LOCATION denied!");
                        edit.putBoolean(Const.USER_DENIED_COARSE_LOCATION_PERMISSION_FOREGROUND, true).apply();
                    }
                } else if (permissions[i].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    backgroundLocation = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                    if (backgroundLocation) {
                        Log.d(TAG, "processRequestPermissionsResult Permission ACCESS_BACKGROUND_LOCATION granted!");
                    } else {
                        Log.d(TAG, "processRequestPermissionsResult Permission ACCESS_BACKGROUND_LOCATION denied!");
                        edit.putBoolean(Const.USER_DENIED_LOCATION_PERMISSION_BACKGROUND, true).apply();
                    }

                }
            }
        }
    }

    /**
     * Returns the profileId of the user.
     * <p>
     * If it already exists in the DB, it returns the value stored in the DB. Otherwise,
     * it makes a request to the server to obtain a profileId. In this case:
     * 1. If the request to the server fails, it returns an empty string.
     * 2. If the request succeeds, it returns the profileId string and also sets the profileId of
     * the user in the DB.
     * <p>
     * WARNING: In case of network connectivity issue, this method might hang until there is a
     * timeout. Therefore, it must always be called from an AsyncTask to avoid blocking the UI.
     *
     * @return profileId of the user or empty string if it does not exist or there is a failure to
     * obtain it from the server
     */
    public static String getProfileIdFromDBandServer() {
        Log.d(TAG, "In getProfileIdFromDBandServer()");
        String profileId = DbRequestHandler.getProfileIdFromDB();
        if (profileId.isEmpty()) {
            // In case we don't have yet a profileId in the DB, we attempt to get one from the server
            // getUniqueID() can return null
            profileId = DataUploader.getUniqueID(Const.UNIQUE_ID_GET_URL);
            if (profileId != null && !profileId.isEmpty()) { //if the profileId was successfully retrieved
                profileId = profileId.trim();
                DbRequestHandler.setProfileId(profileId);
                Log.d(TAG, "getProfileIdFromDBandServer: Obtained profileId=" + profileId + " from the server");
            } else {
                Log.d(TAG, "getProfileIdFromDBandServer: Failed to obtain the profileId from the server");
            }
        }

        // as profileID can be null, we set it to an empty string so that this method
        // never returns null
        if (profileId == null) {
            profileId = "";
        }
        return profileId;
    }

    /**
     * Set the color of the dot view with the given color resource id
     *
     * @param context           the context of the calling fragment
     * @param recommendationDot the dot that needs to color
     * @param colorResourceId   the resource id of the color
     */
    public static void setDotColor(Context context, View recommendationDot, int colorResourceId) {
        GradientDrawable drawable = (GradientDrawable) recommendationDot.getBackground();
        int color = context.getResources().getColor(colorResourceId);

        // set stroke width and stroke color
        recommendationDot.setVisibility(View.VISIBLE);
        drawable.setStroke(2, color);
        drawable.setColor(color);
    }

    /**
     * Returns the resource id of the color corresponding to a given dbm. This is later used to set
     * the color on various views (dots, cursor)
     *
     * @param dbm the dbm value
     * @return the resource id of the color
     */
    public static int getRecommendationDotColorResourceIdFromDbm(int dbm) {
        int colorResourceId;
        SignalLevel level = dbmToLevel(dbm);
        if (level == SignalLevel.HIGH) {
            colorResourceId = R.color.recommendation_dot_red;
        } else if (level == SignalLevel.MODERATE) {
            colorResourceId = R.color.recommendation_dot_orange;
        } else {
            colorResourceId = R.color.recommendation_dot_green;
        }
        return colorResourceId;
    }

    /**
     * Sets the recommendation dot color based on a given dbm
     *
     * @param context           the context of the calling fragment
     * @param dbm               the dbm in integer based on which the dot color is set
     * @param recommendationDot the dot view that needs to be colored according to the dbm value
     */
    public static void setRecommendationDotBasedOnDbm(Context context, int dbm,
                                                      View recommendationDot) {
        int colorResourceId = getRecommendationDotColorResourceIdFromDbm(dbm);
        setDotColor(context, recommendationDot, colorResourceId);
    }

    /**
     * Returns the recommendation text string based on a given dbm
     *
     * @param context the context of the calling fragment
     * @param dbm     the dbm in integer based on which recommendation string needs
     *                to be constructed
     */
    public static String getRecommendationTextBasedOnDbm(Context context, int dbm) {
        String recommendationText = "";
        SignalLevel level = dbmToLevel(dbm);
        if (level == SignalLevel.HIGH) {
            recommendationText = context.getString(R.string.recommendation_high);
        } else if (level == SignalLevel.MODERATE) {
            recommendationText = context.getString(R.string.recommendation_moderate);
        } else {
            recommendationText = context.getString(R.string.recommendation_low);
        }
        return recommendationText;
    }

    /**
     * Take a list of Wi-Fi signals (belonging to a same antenna) and returns a BaseProperty
     * corresponding to the connected Wi-Fi signal. Returns null otherwise
     *
     * @param wifiSignals list of Wi-Fi signals (belonging to a same antenna)
     * @return the BaseProperty corresponding to the connected Wi-Fi signal, null otherwise
     */
    public static BaseProperty getConnectedWifiSignal(List<BaseProperty> wifiSignals) {
        // We go through all signals for the given antenna and retrieve
        // the connected signal if there is one. The goal is to always
        // display the name of the connected signal if there is one.
        if (wifiSignals != null) {
            for (BaseProperty wifiSignal : wifiSignals) {
                if (wifiSignal.connected) {
                    return wifiSignal;
                }
            }
        }
        return null;
    }

    /**
     * Take a childViewSignals data structure and returns a BaseProperty corresponding to the
     * serving cellular cell. Returns null if there is no serving cell.
     *
     * @param childViewSignals data structure
     * @return the BaseProperty corresponding to the serving cell, null otherwise.
     */
    public static BaseProperty getServingCellularSignal(
            ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> childViewSignals) {

        if (childViewSignals != null &&
                childViewSignals.containsKey(MeasurementFragment.AntennaDisplay.CELLULAR)) {
            List<BaseProperty> allChildViewCellularSignals = childViewSignals.get(
                    MeasurementFragment.AntennaDisplay.CELLULAR);
            for (BaseProperty cellularSignal : allChildViewCellularSignals) {
                if (cellularSignal.isValidSignal) {
                    // we retrieve the connected antenna
                    if (cellularSignal.connected) {
                        cellularSignal.prepareOperatorName();
                        return cellularSignal;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Take a childViewSignals data structure and returns the number of valid cellular signals
     *
     * @param childViewSignals data structure
     * @return the number of valid cellular signals
     */
    public static int getNumberOfValidCellularSignals(
            ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> childViewSignals) {
        // we count the number of cellular sources and we retrieve the serving
        // cell (to display the correct operator name).
        int allValidCellularSignalCount = 0;
        if (childViewSignals != null &&
                childViewSignals.containsKey(MeasurementFragment.AntennaDisplay.CELLULAR)) {
            List<BaseProperty> allChildViewCellularSignals = childViewSignals.get(
                    MeasurementFragment.AntennaDisplay.CELLULAR);
            for (BaseProperty cellularSignal : allChildViewCellularSignals) {
                if (cellularSignal.isValidSignal) {
                    // we count the number of valid cellular antennas
                    allValidCellularSignalCount++;
                }
            }
        }
        return allValidCellularSignalCount;
    }

    /**
     * Checks if the user is in the passed country. To do so, we check the SIM ISO country code.
     * If there is no SIM card, we fall back to the ISO country code of the current registered
     * operator. This is an easy and fast solution (compared to any location based methods)
     * <p>
     * See https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes for the list of available
     * country code (alpha-2 codes, that is codes on 2 letters)
     * <p>
     * To test and DEBUG, we can change the SIM ISO country code using adb in the emulator
     * adb shell su 0 setprop gsm.sim.operator.iso-country fr
     *
     * @param country a two letter SIM ISO country code
     * @return true if the user's sim country code is equal to country (case insensitive)
     * and false otherwise
     */
    public static boolean checkIfUserIsInCountry(String country) {
        TelephonyManager telephonyManager = (TelephonyManager) MainApplication.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = telephonyManager.getSimCountryIso();
        if (countryCode.isEmpty()) {
            Log.d(TAG, "checkIfUserIsInCountry: getSimCountryIso() returned an empty string, " +
                    "we fall back to getNetworkCountryIso()");
            countryCode = telephonyManager.getNetworkCountryIso();
        }
        Log.d(TAG, "checkIfUserIsInCountry: " + countryCode);

        return countryCode.equalsIgnoreCase(country);
    }

    /**
     * A utility method to render html code as text of a textview
     *
     * @param textView the text view instance whose text is to be set
     * @param text     the html string that needs to be set
     */
    public static void setHtmlTextToTextView(TextView textView, String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        } else {
            textView.setText(Html.fromHtml(text));
        }
    }

    /**
     * Returns the device's default locale if it is in {FR, DE, IT, ES, PT},
     * otherwise falls back to EN
     *
     * @return An instance of java.util.Locale representing the locale of the device
     */
    public static Locale getDefaultDeviceLocale() {
        Locale locale;
        if (Locale.getDefault().getLanguage().equals(Locale.FRENCH.getLanguage())) {
            locale = Locale.FRENCH;
        } else if (Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage())) {
            locale = Locale.GERMAN;
        } else if (Locale.getDefault().getLanguage().equals(Locale.ITALIAN.getLanguage())) {
            locale = Locale.ITALIAN;
        } else if (Locale.getDefault().getLanguage().equals(new Locale("es").getLanguage())) {
            locale = new Locale("es");
        } else if (Locale.getDefault().getLanguage().equals(new Locale("pt").getLanguage())) {
            locale = new Locale("pt");
        } else {
            locale = Locale.ENGLISH;
        }
        return locale;
    }

    /**
     * Returns the number of days between the two timestamps in ms. millis1 must be lower (or equal)
     * to millis2.
     * <p>
     * We use Calendar objects to take into account daylight savings and timezones. The computation
     * is performed in the current timezone.
     *
     * @param millis1 The first timestamp in long (number of milliseconds since the epoch)
     * @param millis2 The second timestamp in long (number of milliseconds since the epoch)
     * @return The number of days between the two timestamps. If the return value is 0, both
     * timestamps belong to the same day
     */
    public static int getNumberOfDaysBetweenTimestamps(long millis1, long millis2) {
        Log.d(TAG, "in getNumberOfDaysBetweenTimestamps()");

        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(millis1);
        roundCalendarAtMidnight(startDate);

        Calendar endDate = Calendar.getInstance();
        endDate.setTimeInMillis(millis2);
        roundCalendarAtMidnight(endDate);

        int daysBetween = 0;
        while (startDate.before(endDate)) {
            startDate.add(Calendar.DAY_OF_MONTH, 1);
            daysBetween++;
        }
        Log.d(TAG, "getNumberOfDaysBetweenTimestamps: " + daysBetween);
        return daysBetween;
    }

    /**
     * Construct BaseProperty instance from a given jsonString
     *
     * @param jsonString a JSON string representation of a BaseProperty
     * @return BaseProperty instance or null if the instance cannot be created
     */
    public static BaseProperty getBasePropertyFromJsonString(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String basePropertyType = jsonObject.optString(BaseProperty.BASE_PROPERTY_TYPE, "");
            String bt_device_name, bt_device_name_alias, bt_address;
            boolean connected, isValidSignal;
            double latitude, longitude;
            long measured_time;
            int dbm, bt_device_class, bt_bond_state, bt_device_type, type;
            int mcc, mnc, cid;
            int lac, arfcn, bsic, timing_advance, ber, bandwidth;
            int ucid, psc, uarfcn;
            int eci, pci, tac, earfcn, rssi, rsrp, rsrq, rssnr, cqi, ecno;
            long nci;
            int nrarfcn, csiRsrp, csiRsrq, csiSinr, ssRsrp, ssRsrq, ssSinr;
            int network_id, system_id, base_station_id, cdma_latitude, cdma_longitude, cdma_dbm, cdma_ecio, evdo_dbm, evdo_ecio, evdo_snr;
            BaseProperty es;
            switch (basePropertyType) {
                case BaseProperty.WIFI_PROPERTY:
                    String ssid = jsonObject.optString(WifiProperty.SSID, "");
                    String bssid = jsonObject.optString(WifiProperty.BSSID, "");
                    String starredBssid = jsonObject.optString(WifiProperty.STARRED_BSSID, "");
                    String operator_friendly_name = jsonObject.optString(WifiProperty.OPERATOR_FRIENDLY_NAME, "");
                    String venue_name = jsonObject.optString(WifiProperty.VENUE_NAME, "");
                    int is_passpoint_network = jsonObject.optInt(WifiProperty.IS_PASSPOINT_NETWORK, Const.INVALID_IS_PASSPOINT_NETWORK);
                    int freq = jsonObject.optInt(WifiProperty.FREQ, Integer.MAX_VALUE);
                    int center_freq0 = jsonObject.optInt(WifiProperty.CENTER_FREQ0, Const.INVALID_CENTERFREQ0);
                    int center_freq1 = jsonObject.optInt(WifiProperty.CENTER_FREQ1, Const.INVALID_CENTERFREQ1);
                    int channel_width = jsonObject.optInt(WifiProperty.CHANNEL_WIDTH, Const.INVALID_CHANNELWIDTH);
                    String capabilities = jsonObject.optString(WifiProperty.CAPABILITIES, null);
                    int wifiStandard = jsonObject.optInt(WifiProperty.WIFI_STANDARD, Const.INVALID_WIFI_STANDARD);
                    dbm = jsonObject.optInt(WifiProperty.DBM, Integer.MAX_VALUE);
                    connected = jsonObject.optBoolean(WifiProperty.CONNECTED, false);
                    latitude = jsonObject.optDouble(WifiProperty.LATITUDE, Const.INVALID_LATITUDE);
                    longitude = jsonObject.optDouble(WifiProperty.LONGITUDE, Const.INVALID_LATITUDE);
                    isValidSignal = jsonObject.optBoolean(WifiProperty.IS_VALID_SIGNAL, false);
                    measured_time = jsonObject.optLong(WifiProperty.MEASURED_TIME, Const.INVALID_TIME);
                    es = new WifiGroupProperty(new WifiProperty(ssid, bssid, starredBssid, operator_friendly_name,
                            venue_name, is_passpoint_network, freq,
                            center_freq0, center_freq1, channel_width,
                            capabilities, wifiStandard, dbm, connected, latitude,
                            longitude, isValidSignal, measured_time));
                    es.normalizeSignalWithInvalidDbm();
                    return es;
                case BaseProperty.BLUETOOTH_PROPERTY:
                    bt_device_name = jsonObject.optString(BluetoothProperty.BT_DEVICE_NAME, "");
                    bt_device_name_alias = jsonObject.optString(BluetoothProperty.BT_DEVICE_NAME_ALIAS, "");
                    bt_address = jsonObject.optString(BluetoothProperty.BT_ADDRESS, "");
                    bt_device_class = jsonObject.optInt(BluetoothProperty.BT_DEVICE_CLASS, Integer.MAX_VALUE);
                    bt_bond_state = jsonObject.optInt(BluetoothProperty.BT_BOND_STATE, Integer.MAX_VALUE);
                    bt_device_type = jsonObject.optInt(BluetoothProperty.BT_DEVICE_TYPE, Integer.MAX_VALUE);
                    dbm = jsonObject.optInt(BluetoothProperty.DBM, Integer.MAX_VALUE);
                    latitude = jsonObject.optDouble(BluetoothProperty.LATITUDE, Const.INVALID_LATITUDE);
                    longitude = jsonObject.optDouble(BluetoothProperty.LONGITUDE, Const.INVALID_LONGITUDE);
                    isValidSignal = jsonObject.optBoolean(BluetoothProperty.IS_VALID_SIGNAL, false);
                    measured_time = jsonObject.optLong(BluetoothProperty.MEASURED_TIME, Const.INVALID_TIME);
                    es = new BluetoothProperty(bt_device_name, bt_device_name_alias,
                            bt_address, bt_device_class, bt_device_type, bt_bond_state, dbm,
                            latitude, longitude, isValidSignal, measured_time);
                    es.normalizeSignalWithInvalidDbm();
                    return es;
                case BaseProperty.GSM_PROPERTY:
                    type = jsonObject.optInt(GsmProperty.TYPE, TelephonyManager.NETWORK_TYPE_UNKNOWN);
                    mcc = jsonObject.optInt(GsmProperty.MCC, BaseProperty.UNAVAILABLE);
                    mnc = jsonObject.optInt(GsmProperty.MNC, BaseProperty.UNAVAILABLE);
                    cid = jsonObject.optInt(GsmProperty.CID, BaseProperty.UNAVAILABLE);
                    lac = jsonObject.optInt(GsmProperty.LAC, BaseProperty.UNAVAILABLE);
                    arfcn = jsonObject.optInt(GsmProperty.ARFCN, BaseProperty.UNAVAILABLE);
                    bsic = jsonObject.optInt(GsmProperty.BSIC, BaseProperty.UNAVAILABLE);
                    timing_advance = jsonObject.optInt(GsmProperty.TIMING_ADVANCE, BaseProperty.UNAVAILABLE);
                    dbm = jsonObject.optInt(GsmProperty.DBM, BaseProperty.UNAVAILABLE);
                    ber = jsonObject.optInt(GsmProperty.BER, BaseProperty.UNAVAILABLE);
                    connected = jsonObject.optBoolean(GsmProperty.CONNECTED, false);
                    latitude = jsonObject.optDouble(GsmProperty.LATITUDE, Const.INVALID_LATITUDE);
                    longitude = jsonObject.optDouble(GsmProperty.LONGITUDE, Const.INVALID_LONGITUDE);
                    isValidSignal = jsonObject.optBoolean(GsmProperty.IS_VALID_SIGNAL, false);
                    measured_time = jsonObject.optLong(GsmProperty.MEASURED_TIME, Const.INVALID_TIME);
                    es = new GsmProperty(type, mcc, mnc, cid, lac, arfcn, bsic, timing_advance,
                            dbm, ber, connected, latitude, longitude, isValidSignal, measured_time);
                    es.normalizeSignalWithInvalidDbm();
                    return es;
                case BaseProperty.WCDMA_PROPERTY:
                    type = jsonObject.optInt(WcdmaProperty.TYPE, TelephonyManager.NETWORK_TYPE_UNKNOWN);
                    mcc = jsonObject.optInt(WcdmaProperty.MCC, BaseProperty.UNAVAILABLE);
                    mnc = jsonObject.optInt(WcdmaProperty.MNC, BaseProperty.UNAVAILABLE);
                    ucid = jsonObject.optInt(WcdmaProperty.UCID, BaseProperty.UNAVAILABLE);
                    lac = jsonObject.optInt(WcdmaProperty.LAC, BaseProperty.UNAVAILABLE);
                    psc = jsonObject.optInt(WcdmaProperty.PSC, BaseProperty.UNAVAILABLE);
                    uarfcn = jsonObject.optInt(WcdmaProperty.UARFCN, BaseProperty.UNAVAILABLE);
                    dbm = jsonObject.optInt(WcdmaProperty.DBM, BaseProperty.UNAVAILABLE);
                    ber = jsonObject.optInt(WcdmaProperty.BER, BaseProperty.UNAVAILABLE);
                    ecno = jsonObject.optInt(WcdmaProperty.ECNO, BaseProperty.UNAVAILABLE);
                    connected = jsonObject.optBoolean(WcdmaProperty.CONNECTED, false);
                    latitude = jsonObject.optDouble(WcdmaProperty.LATITUDE, Const.INVALID_LATITUDE);
                    longitude = jsonObject.optDouble(WcdmaProperty.LONGITUDE, Const.INVALID_LONGITUDE);
                    isValidSignal = jsonObject.optBoolean(WcdmaProperty.IS_VALID_SIGNAL, false);
                    measured_time = jsonObject.optLong(WcdmaProperty.MEASURED_TIME, Const.INVALID_TIME);
                    es = new WcdmaProperty(type, mcc, mnc, ucid, lac, psc, uarfcn, dbm,
                            ber, ecno, connected, latitude, longitude, isValidSignal, measured_time);
                    es.normalizeSignalWithInvalidDbm();
                    return es;
                case BaseProperty.LTE_PROPERTY:
                    type = jsonObject.optInt(LteProperty.TYPE, TelephonyManager.NETWORK_TYPE_UNKNOWN);
                    mcc = jsonObject.optInt(LteProperty.MCC, BaseProperty.UNAVAILABLE);
                    mnc = jsonObject.optInt(LteProperty.MNC, BaseProperty.UNAVAILABLE);
                    eci = jsonObject.optInt(LteProperty.ECI, BaseProperty.UNAVAILABLE);
                    pci = jsonObject.optInt(LteProperty.PCI, BaseProperty.UNAVAILABLE);
                    tac = jsonObject.optInt(LteProperty.TAC, BaseProperty.UNAVAILABLE);
                    earfcn = jsonObject.optInt(LteProperty.EARFCN, BaseProperty.UNAVAILABLE);
                    bandwidth = jsonObject.optInt(LteProperty.BANDWIDTH, BaseProperty.UNAVAILABLE);
                    timing_advance = jsonObject.optInt(LteProperty.TIMING_ADVANCE, BaseProperty.UNAVAILABLE);
                    rsrp = jsonObject.optInt(LteProperty.RSRP, BaseProperty.UNAVAILABLE);
                    rssi = jsonObject.optInt(LteProperty.RSSI, BaseProperty.UNAVAILABLE);
                    rsrq = jsonObject.optInt(LteProperty.RSRQ, BaseProperty.UNAVAILABLE);
                    rssnr = jsonObject.optInt(LteProperty.RSSNR, BaseProperty.UNAVAILABLE);
                    cqi = jsonObject.optInt(LteProperty.CQI, BaseProperty.UNAVAILABLE);
                    connected = jsonObject.optBoolean(LteProperty.CONNECTED, false);
                    latitude = jsonObject.optDouble(LteProperty.LATITUDE, Const.INVALID_LATITUDE);
                    longitude = jsonObject.optDouble(LteProperty.LONGITUDE, Const.INVALID_LONGITUDE);
                    isValidSignal = jsonObject.optBoolean(LteProperty.IS_VALID_SIGNAL, false);
                    measured_time = jsonObject.optLong(LteProperty.MEASURED_TIME, Const.INVALID_TIME);
                    es = new LteProperty(type, mcc, mnc, eci, pci, tac, earfcn,
                            bandwidth, timing_advance, rsrp, rssi, rsrq, rssnr, cqi,
                            connected, latitude, longitude, isValidSignal, measured_time);
                    es.normalizeSignalWithInvalidDbm();
                    return es;
                case BaseProperty.NEW_RADIO_PROPERTY:
                    type = jsonObject.optInt(NewRadioProperty.TYPE, TelephonyManager.NETWORK_TYPE_UNKNOWN);
                    mcc = jsonObject.optInt(NewRadioProperty.MCC, BaseProperty.UNAVAILABLE);
                    mnc = jsonObject.optInt(NewRadioProperty.MNC, BaseProperty.UNAVAILABLE);
                    nci = jsonObject.optLong(NewRadioProperty.NCI, Long.MAX_VALUE);
                    nrarfcn = jsonObject.optInt(NewRadioProperty.NRARFCN, BaseProperty.UNAVAILABLE);
                    pci = jsonObject.optInt(NewRadioProperty.PCI, BaseProperty.UNAVAILABLE);
                    tac = jsonObject.optInt(NewRadioProperty.TAC, BaseProperty.UNAVAILABLE);
                    csiRsrp = jsonObject.optInt(NewRadioProperty.CSI_RSRP, BaseProperty.UNAVAILABLE);
                    csiRsrq = jsonObject.optInt(NewRadioProperty.CSI_RSRQ, BaseProperty.UNAVAILABLE);
                    csiSinr = jsonObject.optInt(NewRadioProperty.CSI_SINR, BaseProperty.UNAVAILABLE);
                    ssRsrp = jsonObject.optInt(NewRadioProperty.SS_RSRP, BaseProperty.UNAVAILABLE);
                    ssRsrq = jsonObject.optInt(NewRadioProperty.SS_RSRQ, BaseProperty.UNAVAILABLE);
                    ssSinr = jsonObject.optInt(NewRadioProperty.SS_SINR, BaseProperty.UNAVAILABLE);
                    connected = jsonObject.optBoolean(NewRadioProperty.CONNECTED, false);
                    latitude = jsonObject.optDouble(NewRadioProperty.LATITUDE, Const.INVALID_LATITUDE);
                    longitude = jsonObject.optDouble(NewRadioProperty.LONGITUDE, Const.INVALID_LONGITUDE);
                    isValidSignal = jsonObject.optBoolean(NewRadioProperty.IS_VALID_SIGNAL, false);
                    measured_time = jsonObject.optLong(NewRadioProperty.MEASURED_TIME, Const.INVALID_TIME);

                    es = new NewRadioProperty(
                            type,
                            mcc, mnc, nci, nrarfcn,
                            pci, tac,
                            csiRsrp, csiRsrq, csiSinr,
                            ssRsrp, ssRsrq, ssSinr,
                            connected, latitude, longitude, isValidSignal,
                            measured_time
                    );
                    es.normalizeSignalWithInvalidDbm();
                    return es;
                case BaseProperty.CDMA_PROPERTY:
                    type = jsonObject.optInt(CdmaProperty.TYPE, TelephonyManager.NETWORK_TYPE_UNKNOWN);
                    network_id = jsonObject.optInt(CdmaProperty.NETWORK_ID, BaseProperty.UNAVAILABLE);
                    system_id = jsonObject.optInt(CdmaProperty.SYSTEM_ID, BaseProperty.UNAVAILABLE);
                    base_station_id = jsonObject.optInt(CdmaProperty.BASE_STATION_ID, BaseProperty.UNAVAILABLE);
                    cdma_latitude = jsonObject.optInt(CdmaProperty.LATITUDE, BaseProperty.UNAVAILABLE);
                    cdma_longitude = jsonObject.optInt(CdmaProperty.CDMA_LONGITUDE, BaseProperty.UNAVAILABLE);
                    cdma_dbm = jsonObject.optInt(CdmaProperty.CDMA_DBM, BaseProperty.UNAVAILABLE);
                    cdma_ecio = jsonObject.optInt(CdmaProperty.CDMA_ECIO, BaseProperty.UNAVAILABLE);
                    evdo_dbm = jsonObject.optInt(CdmaProperty.EVDO_DBM, BaseProperty.UNAVAILABLE);
                    evdo_ecio = jsonObject.optInt(CdmaProperty.EVDO_ECIO, BaseProperty.UNAVAILABLE);
                    evdo_snr = jsonObject.optInt(CdmaProperty.EVDO_SNR, BaseProperty.UNAVAILABLE);
                    dbm = jsonObject.optInt(CdmaProperty.DBM, BaseProperty.UNAVAILABLE);
                    connected = jsonObject.optBoolean(CdmaProperty.CONNECTED, false);
                    latitude = jsonObject.optDouble(CdmaProperty.LATITUDE, Const.INVALID_LATITUDE);
                    longitude = jsonObject.optDouble(CdmaProperty.LONGITUDE, Const.INVALID_LONGITUDE);
                    isValidSignal = jsonObject.optBoolean(CdmaProperty.IS_VALID_SIGNAL, false);
                    measured_time = jsonObject.optLong(CdmaProperty.MEASURED_TIME, Const.INVALID_TIME);
                    es = new CdmaProperty(type, network_id, system_id, base_station_id,
                            cdma_latitude, cdma_longitude, cdma_dbm, cdma_ecio, evdo_dbm,
                            evdo_ecio, evdo_snr, dbm, connected, latitude,
                            longitude, isValidSignal, measured_time);
                    es.normalizeSignalWithInvalidDbm();
                    return es;
                case "":
                    return null;
            }
        } catch (JSONException jsonException) {
            Log.e("WifiProperty", "getBasePropertyFromJsonString: ");
            jsonException.printStackTrace();
        }
        return null;
    }

    /**
     * Capitalize a string, that is change its first letter to an upper case letter.
     *
     * @param str The string to capitalize
     * @return the new string capitalized
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Round a calendar at midnight, that is set the hours, minutes, seconds, and milliseconds to 0.
     * <p>
     * This method modified the calendar object in place.
     *
     * @param cal The calendar to round
     */
    public static void roundCalendarAtMidnight(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Parse mnc, mcc string as returned by getMccString() and getMncString() methods introduced
     * since Android 28, and return the parsed integer if possible. In case of a number format
     * exception, return CellInfo.UNAVAILABLE.
     *
     * @param mccString The string value representing either mcc or mnc
     * @return the MCC or MNC value as an int
     */
    public static int parseMccMncString(String mccString) {
        try {
            return Integer.parseInt(mccString);
        } catch (NumberFormatException nfe) {
            return BaseProperty.UNAVAILABLE;
        }
    }

    /**
     * This method updates two counters (stored in shared preferences) representing the
     * MainActivity stats.
     * <p>
     * NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED: this counter is incremented only once a day if the
     * MainActivity is resumed at least once during that day. This counter represents how many
     * unique days a user really interacted with the app.
     * <p>
     * NB_UNIQUE_HOURS_MAIN_ACTIVITY_RESUMED: this counter is incremented only once each 60 minutes
     * if the MainActivity is resumed at least once during the last hour. This counter represents
     * how many unique periods of 60 minutes a user really interacted with the app. It is finer
     * grained than NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED. We take a minimum of 60 minutes to avoid
     * incrementing the counter for people interacting very fast we the app (so making many
     * onResume for the MainActivity) during a short period of time
     */
    public static void updateMainActivityStats() {
        Log.d(TAG, "in updateMainActivityStats()");

        long now = System.currentTimeMillis();
        final long ONE_HOUR_IN_MS = 60 * 60 * 1000;

        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editSharedPreferences = sharedPreferences.edit();

        long timestampLastUpdateDays = sharedPreferences.getLong(
                Const.TIMESTAMP_LAST_TIME_NB_UNIQUE_DAYS_WAS_UPDATED, 0);

        long timestampLastUpdateHours = sharedPreferences.getLong(
                Const.TIMESTAMP_LAST_TIME_NB_UNIQUE_HOURS_WAS_UPDATED, 0);

        // if the last time we updated the counter NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED is more
        // than one day ago, we increment it by +1
        long nbUniqueDays = sharedPreferences.getLong(Const.NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED, 0);
        if (!DateUtils.isToday(timestampLastUpdateDays)) {
            editSharedPreferences.putLong(Const.TIMESTAMP_LAST_TIME_NB_UNIQUE_DAYS_WAS_UPDATED, now).apply();
            nbUniqueDays = nbUniqueDays + 1;
            editSharedPreferences.putLong(Const.NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED, nbUniqueDays).apply();
        }

        // if the last time we updated the counter NB_UNIQUE_HOURS_MAIN_ACTIVITY_RESUMED is more
        // than one hour ago, we increment it by +1
        long nbUniqueHours = sharedPreferences.getLong(Const.NB_UNIQUE_HOURS_MAIN_ACTIVITY_RESUMED, 0);
        Log.d(TAG, "updateMainActivityStats (askInAppReview): timestampLastUpdateHours=" +
                new Date(timestampLastUpdateHours) + " now=" + new Date(now));
        if ((now - timestampLastUpdateHours) >= ONE_HOUR_IN_MS) {
            editSharedPreferences.putLong(Const.TIMESTAMP_LAST_TIME_NB_UNIQUE_HOURS_WAS_UPDATED, now).apply();
            nbUniqueHours = nbUniqueHours + 1;
            editSharedPreferences.putLong(Const.NB_UNIQUE_HOURS_MAIN_ACTIVITY_RESUMED, nbUniqueHours).apply();
        }
        Log.d(TAG, "updateMainActivityStats (askInAppReview): nbUniqueDays=" + nbUniqueDays +
                " nbUniqueHours=" + nbUniqueHours);
    }

    /**
     * Ask for an in-app review. We use a strategy to trigger the in-app review for the lovers, and
     * to never bother them with too many requests to review the app. Details are given in comments
     * in this method.
     * <p>
     * The system in-app review is never guaranteed to the displayed and is quota limited.
     * <p>
     * It is documented here https://developer.android.com/guide/playcore/in-app-review
     *
     * @param context  a reference to the app context
     * @param activity the activity in which we ask for the in-all review review
     */
    public static void askInAppReview(Context context, final Activity activity) {
        Log.d(TAG, "in askInAppReview()");

        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editSharedPreferences = sharedPreferences.edit();

        final long now = System.currentTimeMillis();

        // Collection of the main metrics to decide to show the in-app review

        // timestamp of the last in-app review complete flow
        long timestampLastInAppReview = sharedPreferences.getLong(Const.TIMESTAMP_LAST_TIME_IN_APP_REVIEW_TRIGGERED, 0);
        // number of times an in-app review complete flow was performed
        final int nbTimesInAppReviews = sharedPreferences.getInt(Const.NB_TIMES_IN_APP_REVIEW_TRIGGERED, 0);
        // number of unique days the MainActivity was resumed
        long nbUniqueDaysMainActivityResumes = sharedPreferences.getLong(Const.NB_UNIQUE_DAYS_MAIN_ACTIVITY_RESUMED, 0);
        // number of unique hours the MainActivity was resumed
        long nbUniqueHoursMainActivityResumed = sharedPreferences.getLong(Const.NB_UNIQUE_HOURS_MAIN_ACTIVITY_RESUMED, 0);
        // number of days the app has been installed
        int daysSinceAppInstallation = 0;
        long timestampFirstEntryInDb = firstEntryInDb();
        if (timestampFirstEntryInDb > 0) {
            daysSinceAppInstallation = Tools.getNumberOfDaysBetweenTimestamps(timestampFirstEntryInDb, now);
        }

        Log.d(TAG, "askInAppReview: timestampFirstEntryInDb=" + new Date(timestampFirstEntryInDb) +
                " now=" + new Date(now));
        Log.d(TAG, "askInAppReview: daysSinceAppInstallation=" + daysSinceAppInstallation +
                " nbUniqueDaysMainActivityResumes=" + nbUniqueDaysMainActivityResumes +
                " nbUniqueHoursMainActivityResumed=" + nbUniqueHoursMainActivityResumed +
                " timestampLastInAppReview=" + timestampLastInAppReview +
                " nbTimesInAppReviews=" + nbTimesInAppReviews);

        /*
        Rules to show the in-app review are
        - We let the user get some experience with the app:
          the app must have been installed more than (>=) MIN_DAYS_SINCE_APP_INSTALLATION days ago
        - The user must have frequently interacted with the app (a lover):
          the app must have been actively used more than (>=) MIN_DAYS_MAIN_ACTIVITY_RESUMED unique days
          or more than (>=) MIN_HOURS_MAIN_ACTIVITY_RESUMED unique hours
        - We trigger at most a single in-app review per day
        - We do not trigger more than (<=) MAX_NB_TIME_IN_APP_REVIEW_COMPLETED in-app reviews
         */
        if ((daysSinceAppInstallation >= Const.MIN_DAYS_SINCE_APP_INSTALLATION) &&
                ((nbUniqueDaysMainActivityResumes >= Const.MIN_DAYS_MAIN_ACTIVITY_RESUMED) ||
                        (nbUniqueHoursMainActivityResumed >= Const.MIN_HOURS_MAIN_ACTIVITY_RESUMED)) &&
                !DateUtils.isToday(timestampLastInAppReview) &&
                nbTimesInAppReviews <= Const.MAX_NB_TIME_IN_APP_REVIEW_COMPLETED) {
            // used for in-app reviews
            Log.d(TAG, "askInAppReview: in-app review start the code");
            final ReviewManager manager = ReviewManagerFactory.create(context);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "askInAppReview.request.onComplete: in-app review task is successful");
                    // We can get the ReviewInfo object
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(activity, reviewInfo);
                    Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                    flow.addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Log.d(TAG, "askInAppReview.flow.onComplete: in-app review flow is complete");
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                            editSharedPreferences.putInt(Const.NB_TIMES_IN_APP_REVIEW_TRIGGERED, nbTimesInAppReviews + 1).apply();
                            editSharedPreferences.putLong(Const.TIMESTAMP_LAST_TIME_IN_APP_REVIEW_TRIGGERED, now).apply();
                        }
                    });
                }
            });
        }
    }

    /**
     * Starting with Android 31 (S), pending intent must have either the flag
     * PendingIntent.FLAG_IMMUTABLE or the flag FLAG_MUTABLE. It is recommended to always
     * set the flag to PendingIntent.FLAG_IMMUTABLE. As this flag only exists starting with
     * API 23, and this flag is only required starting with API 31, we create a method that returns
     * the PendingIntent.FLAG_IMMUTABLE if API >= 31 (S) or 0 otherwise
     *
     * @return PendingIntent.FLAG_IMMUTABLE if API >=31, 0 otherwise
     */
    public static int flag_immutable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PendingIntent.FLAG_IMMUTABLE;
        } else {
            return 0;
        }
    }


    public enum SignalLevel {
        LOW,
        MODERATE,
        HIGH
    }

    /**
     * An async task that gets the profileId of the user from the server and persists it in the
     * app database.
     */
    public static class GetProfileIdAsyncTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... obj) {
            Log.d(TAG, "in Tools.GetProfileIdAsyncTask.doInBackground() ");
            return Tools.getProfileIdFromDBandServer();
        }
    }

    /**
     * return the localized string corresponding to the passed wifiStandard
     *
     * @param context      the application context
     * @param wifiStandard the wifi standard
     * @return a localized string representing the passed wifi standard or an empty string
     * if the standard is unknown of unavailable
     */
    public static String getWifiStandardString(Context context, int wifiStandard) {
        String standard = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (wifiStandard == ScanResult.WIFI_STANDARD_UNKNOWN) {
                standard = "";
            } else if (wifiStandard == ScanResult.WIFI_STANDARD_LEGACY) {
                standard = context.getString(R.string.wifi_legacy);
            } else if (wifiStandard == ScanResult.WIFI_STANDARD_11N) {
                standard = context.getString(R.string.wifi_n);
            } else if (wifiStandard == ScanResult.WIFI_STANDARD_11AC) {
                standard = context.getString(R.string.wifi_ac);
            } else if (wifiStandard == ScanResult.WIFI_STANDARD_11AX) {
                standard = context.getString(R.string.wifi_ax);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (wifiStandard == ScanResult.WIFI_STANDARD_11AD) {
                standard = context.getString(R.string.wifi_ad);
            }
        }

        // if there is no standard available we return an empty string, otherwise we return
        // a string representing the wifi format
        if (standard.isEmpty()) {
            return "";
        } else {
            return " - " + standard;
        }
    }
//    /**
//     * An async task to notify our server that a user is visiting the solution Web site.
//     * In this process, the server side will record that the user with mProfileId visited
//     * the web site for the solution mSolution. This is our way to get statistics on potential
//     * purchases.
//     */
//    public static class RecordUserIsVisitingSolutionWebSiteAsyncTask extends AsyncTask<Void, Void, Void> {
//
//        String mProfileId, mSolution;
//
//        public RecordUserIsVisitingSolutionWebSiteAsyncTask(String solution, String profileId) {
//            mSolution = solution;
//            mProfileId = profileId;
//        }
//
//        @Override
//        protected Void doInBackground(Void... obj) {
//            // we request for the profile ID in case it is not yet received from the server. As this
//            // is part of an AsyncTask, it will not block the UI. However, it will block the call to
//            // recordUserIsVisitingSolutionWebSite(), which is what we want here. We want to
//            // associate each visit to a specific user.
//            if (mProfileId.isEmpty()) {
//                mProfileId = getProfileIdFromDBandServer();
//            }
//            DataUploader.recordUserIsVisitingSolutionWebSite(mSolution, mProfileId);
//            return null;
//        }
//    }
}

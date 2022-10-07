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

package fr.inria.es.electrosmart.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Arrays;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.signalhandler.RawSignalHandler;
import fr.inria.es.electrosmart.ui.PreferenceSettingHighExposure;



/*
            ############ how to create a  new preference ################

    1) add in xml/preferences.xml the preference and updated accordingly the strings.xml

    2) in SettingsPreferenceFragment.java add a get_PREF_KEY_<MY_PREF> function and if the setting
       is for the debug mode a get_<MY_PREF> method

    3) initialize the settings in SettingsPreferenceFragment.onCreate()

    4) then use get_PREF_KEY_<MY_PREF> (for release preferences) or get_<MY_PREF> (for debug
       preferences) in the code where you need to take action based on this preference.

 */

/**
 * The SettingsPreferenceFragment is responsible for generating the layout for the settings screen.
 * WiFi scan in background.
 * ...........Starting Jelly Bean (version code 18, Android 4.3), Android permits access of WiFi signal
 * ...........measures, even if it is turned off manually. This setting however, has been moved to
 * ...........different places in the settings over the time.
 * <p/>
 * ...........For versions of Android below M, this setting can be found under Settings > Wi-Fi > (Menu)
 * ...........Advanced Wi-Fi > Useful settings > Allow Wi-Fi scanning.
 * <p/>
 * ...........For Android M and N, this setting is under Settings > Location > Scanning (Menu) > Wi-Fi scanning
 */

public class SettingsPreferenceFragment extends PreferenceFragmentCompat implements PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {
    private static final String TAG = "SettingsPrefFragment";
    // Versions of Android are not uniform in the way they handle the left space of a setting when
    // there is no icon (see PreferenceSettingHighExposure.setTextViewsPadding()). This variable
    // is set to true if left padding should be added (that is, left icon space is reserved)
    public static boolean sIsIconSpaceReserved = false;
    // A CheckBoxPreference variable to set the wifi scan in background setting
    private SwitchPreferenceCompat backgroundWiFiScan;
    // A list preference variable to set the notification importance for exposure
    private ListPreference notificationExposureImportance;
    // A list preference variable to set the notification importance for new source
    private ListPreference notificationNewSourceImportance;
    // Custom preference for high exposure threshold
    private PreferenceSettingHighExposure notificationExposureThreshold;
    // A list preference variable to store the number of days in history
    private ListPreference numberOfDaysInHistory;

    //############################# HELPER FUNCTIONS TO ACCESS THE PREF KEYS ######################

    /**
     * Return the corresponding high exposure threshold value based on the user's segment
     *
     * @return an integer dbm value
     */
    private static int getHighThresholdAlertBasedOnUserSegment() {
        Log.d(TAG, "in getHighThresholdAlertBasedOnUserSegment()");
        int segment = Const.PROFILE_SEGMENT_UNKNOWN;

        // The MainActivity.sUserProfile can be null if this method is called from the
        // MeasurementScheduler and the MainActivity is not created
        if (MainActivity.sUserProfile == null) {
            Log.d(TAG, "getHighThresholdAlertBasedOnUserSegment: sUserProfile is null getting it for the DB");
            MainActivity.sUserProfile = DbRequestHandler.getUserProfile();
            Log.d(TAG, "getHighThresholdAlertBasedOnUserSegment: sUserProfile: " + MainActivity.sUserProfile);

            /*
             If the table does not contain a profile, the method DbRequestHandler.getUserProfile()
             will return null. This is not possible on new versions of ElectroSmart (with the new
             onboarding), but I am not 100% sure it cannot happen when upgrading from an old
             version. In any case, even if this test is useless, it does not hurt. If the profile
             is null, the segment will be correctly returned as Const.PROFILE_SEGMENT_UNKNOWN
            */
            if (MainActivity.sUserProfile != null) {
                Log.d(TAG, "getHighThresholdAlertBasedOnUserSegment: sUserProfile retrieved from the DB is not null");
                segment = MainActivity.sUserProfile.getSegment();
            }
        } else {
            Log.d(TAG, "getHighThresholdAlertBasedOnUserSegment: sUserProfile is not null getting the segment");
            segment = MainActivity.sUserProfile.getSegment();
        }

        Log.d(TAG, "getHighThresholdAlertBasedOnUserSegment: segment=" + segment);

        if (segment == Const.PROFILE_SEGMENT_ELECTROSENSITIVE) {
            return Const.HIGH_EXPOSURE_THRESHOLD_ELECTROSENSITIVE;
        } else if (segment == Const.PROFILE_SEGMENT_CONCERNED) {
            return Const.HIGH_EXPOSURE_THRESHOLD_CONCERNED;
        } else {
            return Const.HIGH_EXPOSURE_THRESHOLD_CURIOUS;
        }
    }

    /**
     * returns the stored value for PREF_KEY_NOTIFICATION_EXPOSURE, or if it does not
     * exist yet, the default value that is Const.PREFERENCE_EXPOSURE_NOTIFICATION_UNKNOWN.
     * <p>
     * This setting is relevant only for SDK versions before O, for O+ this method must always
     * return true, the real notification will be managed by the channel itself
     */
    public static boolean get_PREF_KEY_NOTIFICATION_EXPOSURE() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                    .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE),
                            Const.PREFERENCE_EXPOSURE_NOTIFICATION_UNKNOWN);
        } else {
            return true;
        }
    }

    /**
     * returns the stored value for PREF_KEY_NOTIFICATION_IMPORTANCE, or if it does not
     * exist yet, the default value that is {@link R.string#PREF_VALUE_NOTIFICATION_IMPORTANCE_NONE}
     */
    public static String get_PREF_KEY_NOTIFICATION_EXPOSURE_IMPORTANCE() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getString(MainApplication.getContext().
                                getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE_IMPORTANCE),
                        MainApplication.getContext().
                                getString(R.string.PREF_VALUE_NOTIFICATION_IMPORTANCE_NONE));
    }

    /**
     * returns the stored value for PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD, or if it does not
     * exist yet, the default value that is {@code Const.RECOMMENDATION_HIGH_THRESHOLD}.
     */
    public static int get_PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD() {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);

        return sharedPreferences.getInt(MainApplication.getContext().
                        getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD),
                getHighThresholdAlertBasedOnUserSegment());
    }

    /**
     * Stores the given integer value in the shared preference
     * R.string.PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD
     *
     * @param newValue integer value in the exposure scale
     */
    public static void set_PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD(int newValue) {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(MainApplication.getContext().
                getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD), newValue).apply();
    }

    /**
     * returns the stored value for PREF_KEY_NOTIFICATION_NEW_SOURCE, or if it does not
     * exist yet, the default value that is Const.PREFERENCE_NEW_SOURCE_NOTIFICATION_UNKNOWN.
     * <p>
     * This setting is relevant only for SDK versions before O, for O+ this method must always
     * return true, the real notification will be managed by the channel itself
     */
    public static boolean get_PREF_KEY_NOTIFICATION_NEW_SOURCE() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                    .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_NOTIFICATION_NEW_SOURCE),
                            Const.PREFERENCE_NEW_SOURCE_NOTIFICATION_UNKNOWN);
        } else {
            return true;
        }
    }

    /**
     * returns the stored value for PREF_KEY_NOTIFICATION_NEW_SOURCE_IMPORTANCE, or if it does not
     * exist yet, the default value that is {@link R.string#PREF_VALUE_NOTIFICATION_IMPORTANCE_NONE}
     */
    public static String get_PREF_KEY_NOTIFICATION_NEW_SOURCE_IMPORTANCE() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getString(MainApplication.getContext().
                                getString(R.string.PREF_KEY_NOTIFICATION_NEW_SOURCE_IMPORTANCE),
                        MainApplication.getContext().
                                getString(R.string.PREF_VALUE_NOTIFICATION_IMPORTANCE_NONE));
    }


    /**
     * returns the stored value for PREF_KEY_ADVANCED_MODE, or if it does not exist yet, the
     * default value that is false.
     */
    public static boolean get_PREF_KEY_ADVANCED_MODE() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_ADVANCED_MODE),
                        false);
    }

    /**
     * returns the stored value for PREF_KEY_DARK_MODE, or if it does not exist yet, the
     * default value that is false
     */
    public static boolean get_PREF_KEY_DARK_MODE() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_DARK_MODE),
                        false);
    }

    /**
     * returns the stored value for PREF_KEY_SHOW_INSTRUMENT, or if it does not exist yet, the
     * default value that is false.
     */
    public static boolean get_PREF_KEY_SHOW_INSTRUMENT() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_SHOW_INSTRUMENT),
                        false);
    }

    /**
     * returns the stored value for PREF_KEY_ENABLE_BT_AUTOMATICALLY, or if it does not exist yet,
     * the default value that is true.
     */
    public static boolean get_PREF_KEY_ENABLE_BT_AUTOMATICALLY() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_ENABLE_BT_AUTOMATICALLY),
                        true);
    }

    /**
     * Stores the given boolean value in the shared preference
     * R.string.PREF_KEY_ENABLE_BT_AUTOMATICALLY
     *
     * @param newValue boolean value set to false if BT must not be automatically switched on in
     *                 foreground, true otherwise (default behavior)
     */
    public static void set_PREF_KEY_ENABLE_BT_AUTOMATICALLY(boolean newValue) {
        SharedPreferences sharedPreferences = MainApplication.getContext().
                getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(MainApplication.getContext().
                getString(R.string.PREF_KEY_ENABLE_BT_AUTOMATICALLY), newValue).apply();

        // logging for the event
        if (newValue) {
            DbRequestHandler.dumpEventToDatabase(Const.EVENT_SETTINGS_ENABLE_BT_AUTOMATICALLY_ON);
        } else {
            DbRequestHandler.dumpEventToDatabase(Const.EVENT_SETTINGS_ENABLE_BT_AUTOMATICALLY_OFF);
        }
    }


    /**
     * returns the stored value for PREF_KEY_AGGRESSIVE_WIFI_SCAN, or if it does not exist yet, the
     * default value that is Const.DO_AGGRESSIVE_WIFI_CARD_SCAN
     */
    public static boolean get_PREF_KEY_AGGRESSIVE_WIFI_SCAN() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_AGGRESSIVE_WIFI_SCAN),
                        Const.DO_AGGRESSIVE_WIFI_CARD_SCAN);
    }

    /**
     * returns the stored value for PREF_KEY_AGGRESSIVE_BT_SCAN, or if it does not exist yet, the
     * default value that is Const.DO_AGGRESSIVE_BT_CARD_SCAN
     */
    public static boolean get_PREF_KEY_AGGRESSIVE_BT_SCAN() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_AGGRESSIVE_BT_SCAN),
                        Const.DO_AGGRESSIVE_BT_CARD_SCAN);
    }

    /**
     * returns the stored value for PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND, or if it does not exist yet, the
     * default value that is Const.MEASUREMENT_CYCLE_BACKGROUND
     */
    public static String get_PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getString(MainApplication.getContext().getString(R.string.PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND),
                        Long.toString(Const.MEASUREMENT_CYCLE_BACKGROUND));
    }

    /**
     * returns the stored value for get_PREF_KEY_WARDRIVE_MODE, or if it does not exist yet, the
     * default value that is false
     */
    public static boolean get_PREF_KEY_WARDRIVE_MODE() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_WARDRIVE_MODE),
                        false);
    }

    /**
     * returns the stored value for get_PREF_KEY_SYNC_CELLULAR, or if it does not exist yet, the
     * default value that is false
     */
    public static boolean get_PREF_KEY_SYNC_CELLULAR() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getBoolean(MainApplication.getContext().getString(R.string.PREF_KEY_SYNC_CELLULAR),
                        false);
    }

    /**
     * returns the stored value for PREF_KEY_EXPOSURE_METRIC, or if it does not exist yet, the
     * default value that is @string/PREF_VALUE_EXPOSURE_SCORE_METRIC
     */
    public static String get_PREF_KEY_EXPOSURE_METRIC() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getString(MainApplication.getContext().getString(R.string.PREF_KEY_EXPOSURE_METRIC),
                        MainApplication.getContext().getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC));
    }

    /**
     * returns the stored value for PREF_KEY_PROTECTION_TEST_DURATION, or if it does not exist yet, the
     * default value that is returned by {@link SettingsPreferenceFragment#getDefaultProtectionTestDuration }
     */
    public static String get_PREF_KEY_PROTECTION_TEST_DURATION() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
                .getString(MainApplication.getContext().getString(R.string.PREF_KEY_PROTECTION_TEST_DURATION),
                        Integer.toString(getDefaultProtectionTestDuration()));
    }
    //#############################################################################################


    //###################### HELPER FUNCTIONS TO GET PREFERENCES FOR DEBUG SETTINGS #############

    /**
     * For a release build, returns the value Const.DO_AGGRESSIVE_WIFI_CARD_SCAN, and for a
     * debug build returns the value set in the debug settings
     */
    public static boolean get_DO_AGGRESSIVE_WIFI_CARD_SCAN() {
        if (Const.IS_RELEASE_BUILD) {
            return Const.DO_AGGRESSIVE_WIFI_CARD_SCAN;
        } else {
            return get_PREF_KEY_AGGRESSIVE_WIFI_SCAN();
        }
    }

    /**
     * For a release build, returns the value Const.DO_AGGRESSIVE_BT_CARD_SCAN, and for a
     * debug build returns the value set in the debug settings
     */
    public static boolean get_DO_AGGRESSIVE_BT_CARD_SCAN() {
        if (Const.IS_RELEASE_BUILD) {
            return Const.DO_AGGRESSIVE_BT_CARD_SCAN;
        } else {
            return get_PREF_KEY_AGGRESSIVE_BT_SCAN();
        }
    }

    /**
     * For a release build, returns the value Const.MEASUREMENT_CYCLE_BACKGROUND, and for a
     * debug build returns the value set in the debug settings
     */
    public static long get_MEASUREMENT_CYCLE_BACKGROUND() {
        if (Const.IS_RELEASE_BUILD) {
            return Const.MEASUREMENT_CYCLE_BACKGROUND;
        } else {
            try {
                return Long.parseLong(get_PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND());
            } catch (NumberFormatException ex) {
                Log.e(TAG, "get_MEASUREMENT_CYCLE_BACKGROUND: cannot be parsed: " +
                        "get_PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND()=" +
                        get_PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND());
                return Const.MEASUREMENT_CYCLE_BACKGROUND;
            }
        }
    }

    /**
     * Returns true if the wardrive mode is enabled, false otherwise.
     * <p>
     * The wardrive mode can be enabled in the debug settings or when the method
     * RawSignalHandler.setTempWardrive(true) is called.
     */
    public static boolean get_WARDRIVE_MODE() {
        if (Const.IS_RELEASE_BUILD) {
            return RawSignalHandler.isTempWardrive();
        } else {
            return get_PREF_KEY_WARDRIVE_MODE() || RawSignalHandler.isTempWardrive();
        }
    }

    /**
     * For a release build, return the default test scan duration and for a debug release return what
     * is set in the preference. If not set, return the default test scan duration.
     *
     * @return An integer that represents time in seconds
     */

    public static int getProtectionTestDuration() {
        if (Const.IS_RELEASE_BUILD) {
            return getDefaultProtectionTestDuration();
        } else {
            try {
                return Integer.parseInt(get_PREF_KEY_PROTECTION_TEST_DURATION());
            } catch (NumberFormatException ex) {
                Log.e(TAG, "get_PREF_KEY_PROTECTION_TEST_DURATION: cannot be parsed: " +
                        "get_PREF_KEY_PROTECTION_TEST_DURATION()=" +
                        get_PREF_KEY_PROTECTION_TEST_DURATION());
                return getDefaultProtectionTestDuration();
            }
        }
    }

    /**
     * A utility function that returns {@link Const#PROTECTION_TEST_DURATION_SECONDS_ANDROID_P} for andorid P+
     * devices else returns {@link Const#PROTECTION_TEST_DURATION_SECONDS}
     *
     * @return The default protection test duration in seconds
     */
    public static int getDefaultProtectionTestDuration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Const.PROTECTION_TEST_DURATION_SECONDS_ANDROID_P;
        } else {
            return Const.PROTECTION_TEST_DURATION_SECONDS;
        }
    }
    //#############################################################################################

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        backgroundWiFiScan = (SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_BACKGROUND_WIFI_SCAN));
        backgroundWiFiScan.setOnPreferenceChangeListener((preference, o) -> {
            Log.d(TAG, "backgroundWiFi " + o.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

                WifiManager wm = (WifiManager) getActivity().getApplicationContext().
                        getSystemService(Context.WIFI_SERVICE);
                if (Tools.isAirplaneModeActivated(getContext())) {
                    Tools.createInformationalDialog(
                            getActivity(),
                            getString(R.string.settings_preference_airplane_mode_dialog_title),
                            getString(R.string.settings_preference_airplane_mode_dialog_text)
                    );

                    // Set the setting false by default as we have no way of tracking whether
                    // the Wi-Fi Scan in background was enabled or not. This is because Android
                    // returns false when calling isScanAlwaysAvailable() in this case.
                    return false;
                }
                if (wm.isScanAlwaysAvailable()) {

                    // Starting Marshmallow, the scan WiFi in background is moved to Location
                    // settings. Hence, we start the corresponding activity.
                    // Tested on Android N, M and KitKat

                    Intent intent;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        intent = new Intent(Settings
                                .ACTION_LOCATION_SOURCE_SETTINGS);
                        /*
                        FLAG_ACTIVITY_NEW_TASK creates a new task (a set of activity with their
                        own back stack)
                        FLAG_ACTIVITY_CLEAR_TOP guarantee that wherever we went in the launch
                        task we will always start again at the first activity on the task
                        FLAG_ACTIVITY_NO_HISTORY makes the first activity not present in the
                        history. In the regular case, it makes faster with the back button to go
                        back to ES. If ever the user goes more than 2 activities deep into this
                        task, then we will only save one back button touch.
                         */
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    } else {
                        intent = new Intent(Settings
                                .ACTION_WIFI_IP_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    }
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException exc) {
                        Tools.createInformationalDialog(
                                getActivity(),
                                getString(R.string.activity_does_not_exist_error_title),
                                getString(R.string.activity_does_not_exist_error_text)
                        );

                        return false;
                    }
                    return true;
                } else {
                    /*
                    When we start the intent for ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE using
                    startActivity(), the popup doesn't seem to receive the name of the app
                    (ElectroSmart) and shows null instead.

                    When we start this intent using startActivityForResult(), we see the app name.
                    Though we do not use the ActivityResult for any purpose, we need to use this.
                    Also this seems to be the correct way of starting this intent.
                    See doc:
                    https://developer.android.com/reference/android/net/wifi/WifiManager.html#ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE
                    */
                    Intent intent = new Intent(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE);
                    startActivityForResult(intent, Const.REQUEST_CODE_WIFI_SCAN_ALWAYS);
                    return false;
                }
            } else {
                Log.d(TAG, "ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE is not available for your OS.");
                return true;
            }
        });

        // we don't show the backgroundWiFiScan setting for API < 18
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Preference pref = findPreference(getString(R.string.PREF_KEY_BACKGROUND_WIFI_SCAN));
            getPreferenceScreen().removePreference(pref);
        }

        // we don't show the debug setting for release versions
        if (Const.IS_RELEASE_BUILD) {
            Preference pref = findPreference(getString(R.string.PREF_KEY_DEBUG_SETTINGS));
            getPreferenceScreen().removePreference(pref);
        } else {
            // We set the default values for the debug settings if they don't already exist, and their
            // current values otherwise
            ((CheckBoxPreference) findPreference(getString(R.string.PREF_KEY_AGGRESSIVE_WIFI_SCAN)))
                    .setChecked(get_PREF_KEY_AGGRESSIVE_WIFI_SCAN());
            ((CheckBoxPreference) findPreference(getString(R.string.PREF_KEY_AGGRESSIVE_BT_SCAN)))
                    .setChecked(get_PREF_KEY_AGGRESSIVE_BT_SCAN());
            ((ListPreference) findPreference(getString(R.string.PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND)))
                    .setValue(get_PREF_KEY_MEASUREMENT_CYCLE_BACKGROUND());
            ((ListPreference) findPreference(getString(R.string.PREF_KEY_PROTECTION_TEST_DURATION)))
                    .setValue(get_PREF_KEY_PROTECTION_TEST_DURATION());
            ((CheckBoxPreference) findPreference(getString(R.string.PREF_KEY_WARDRIVE_MODE)))
                    .setChecked(get_PREF_KEY_WARDRIVE_MODE());
        }

        // set the default values if they don't already exist, and their current values otherwise
        SwitchPreferenceCompat preferenceDarkMode =
                ((SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_DARK_MODE)));
        preferenceDarkMode.setChecked(get_PREF_KEY_DARK_MODE());
        preferenceDarkMode.setOnPreferenceChangeListener((preference, newValue) -> {
            if (((boolean) newValue)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            return true;
        });

        SwitchPreferenceCompat preferenceSyncCellular =
                ((SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_SYNC_CELLULAR)));
        preferenceSyncCellular.setChecked(get_PREF_KEY_SYNC_CELLULAR());
        preferenceSyncCellular.setOnPreferenceChangeListener((preference, newValue) -> true);

        SwitchPreferenceCompat preferenceAdvancedMode =
                (SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_ADVANCED_MODE));
        preferenceAdvancedMode.setChecked(get_PREF_KEY_ADVANCED_MODE());
        preferenceAdvancedMode.setOnPreferenceChangeListener((preference, newValue) -> {
            DbRequestHandler.dumpEventToDatabase(
                    (boolean) newValue ? Const.EVENT_SETTINGS_ADVANCED_MODE_ON : Const.EVENT_SETTINGS_ADVANCED_MODE_OFF
            );
            return true;
        });

        // handle show instrument preference click
        SwitchPreferenceCompat preferenceShowInstrument =
                (SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_SHOW_INSTRUMENT));
        preferenceShowInstrument.setChecked(get_PREF_KEY_SHOW_INSTRUMENT());
        preferenceShowInstrument.setOnPreferenceChangeListener((preference, newValue) -> {
            DbRequestHandler.dumpEventToDatabase(
                    (boolean) newValue ? Const.EVENT_SETTINGS_SHOW_INSTRUMENT_ON : Const.EVENT_SETTINGS_SHOW_INSTRUMENT_OFF
            );
            return true;
        });


        // we create the exposure notification setting
        SwitchPreferenceCompat preferenceNotificationExposure =
                (SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE));

        // we create the exposure notification threshold setting
        notificationExposureThreshold = (PreferenceSettingHighExposure)
                findPreference(getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD));

        // we create the exposure metric setting
        ListPreference exposureMetric = (ListPreference) findPreference(getString(R.string.PREF_KEY_EXPOSURE_METRIC));
        exposureMetric.setValue(get_PREF_KEY_EXPOSURE_METRIC());
        exposureMetric.setSummary(String.format(getString(R.string.settings_preference_exposure_metric_summary), exposureMetric.getEntry()));
        exposureMetric.setOnPreferenceChangeListener((preference, newValue) -> {
            Log.d(TAG, "exposureMetric onPreferenceChange newValue = " + newValue.toString());
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setValue(newValue.toString());
            preference.setSummary(String.format(getString(R.string.settings_preference_exposure_metric_summary),
                    listPreference.getEntry()));
            // Initialize the threshold value as set in the shared preference
            notificationExposureThreshold.initializeValues();
            DbRequestHandler.dumpEventToDatabase(get_PREF_KEY_EXPOSURE_METRIC());

            return true;
        });
        refreshCustomStatus();

        numberOfDaysInHistory = (ListPreference) findPreference(getString(R.string.PREF_KEY_NUMBER_OF_DAYS_IN_HISTORY));
        Log.d(TAG, " onCreate: getNumberOfDaysInHistory() = " + Tools.getNumberOfDaysInHistory());

        numberOfDaysInHistory.setOnPreferenceChangeListener((preference, newValue) -> {
            Log.d(TAG, "onPreferenceChange: newValue " + newValue.toString());

            numberOfDaysInHistory.setValue(newValue.toString());
            numberOfDaysInHistory.setSummary(String.format(getString(R.string.settings_preference_history_nb_days_summary), numberOfDaysInHistory.getEntry()));

            Log.d(TAG, "onPreferenceChange: Old value for getNumberOfDaysInHistory = " +
                    Tools.getNumberOfDaysInHistory());
            Tools.setNumberOfDaysInHistory(Integer.parseInt(newValue.toString()));

            Log.d(TAG, "onPreferenceChange: New values for getNumberOfDaysInHistory = " +
                    Tools.getNumberOfDaysInHistory());
            return false;
        });

        Log.d(TAG, "onCreate: " + Arrays.toString(numberOfDaysInHistory.getEntryValues()));
        numberOfDaysInHistory.setValue(String.valueOf(Tools.getNumberOfDaysInHistory()));
        numberOfDaysInHistory.setSummary(String.format(
                getString(R.string.settings_preference_history_nb_days_summary),
                numberOfDaysInHistory.getEntry()));

        // we create the exposure notification importance setting
        notificationExposureImportance = (ListPreference) findPreference(getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE_IMPORTANCE));

        // we create the new source setting
        SwitchPreferenceCompat preferenceNotificationNewSource =
                (SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_NOTIFICATION_NEW_SOURCE));

        // we create the new source notification importance setting
        notificationNewSourceImportance = (ListPreference) findPreference(getString(R.string.PREF_KEY_NOTIFICATION_NEW_SOURCE_IMPORTANCE));

        // preference to control whether we enable BT automatically on foreground
        final SwitchPreferenceCompat preferenceEnableBTAutomatically =
                (SwitchPreferenceCompat) findPreference(getString(R.string.PREF_KEY_ENABLE_BT_AUTOMATICALLY));
        preferenceEnableBTAutomatically.setChecked(get_PREF_KEY_ENABLE_BT_AUTOMATICALLY());
        preferenceEnableBTAutomatically.setOnPreferenceChangeListener((preference, newValue) -> {
            final boolean changedValue = (boolean) newValue;
            if (!changedValue) {
                // User has decided to disable bluetooth
                // Warn him/her about the fact that bluetooth measurements will be disabled
                Tools.createYesNoDialog(
                        getActivity(),
                        getString(R.string.enable_bt_automatically_settings_dialog_title),          // dialog title
                        getString(R.string.enable_bt_automatically_settings_dialog_description),    // dialog text
                        getString(R.string.yes),                    // ok button text
                        new DialogInterface.OnClickListener() {                           // ok button click listener
                            public void onClick(DialogInterface dialog, int id) {
                                set_PREF_KEY_ENABLE_BT_AUTOMATICALLY(false);
                            }
                        },
                        getString(R.string.no),        // cancel button text
                        new DialogInterface.OnClickListener() {                           // cancel button click listener
                            public void onClick(DialogInterface dialog, int id) {
                                // cancel and do nothing
                                dialog.cancel();
                                preferenceEnableBTAutomatically.setChecked(true);
                            }
                        }
                );
            } else {
                set_PREF_KEY_ENABLE_BT_AUTOMATICALLY(true);
            }
            return true;
        });

        // we need a native (not a custom) preference object to get to correct
        // state of isIconSpaceReserved()
        sIsIconSpaceReserved = preferenceNotificationExposure.isIconSpaceReserved();

        /*
         On Android O+ devices, we have dedicated channel setting for notifications. Hence, we
         use the channel system setting like in Slack (instead of managing ourselves the various
         notification options).

         On O+ devices, we hide "Enable the notification" SwitchPreference and reuse the
         importance setting (by changing its title and summary) to have a direct access to
         the channel configuration
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            preferenceNotificationExposure.setVisible(false);
            preferenceNotificationNewSource.setVisible(false);
            notificationExposureImportance.setTitle(getString(R.string.settings_preference_notification_system_setting));
            notificationNewSourceImportance.setTitle(getString(R.string.settings_preference_notification_system_setting));
            notificationExposureImportance.setSummary(R.string.settings_preference_notification_system_setting_summary);
            notificationNewSourceImportance.setSummary(R.string.settings_preference_notification_system_setting_summary);

            notificationExposureImportance.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, Const.NOTIFICATION_EXPOSURE_CHANNEL_ID);
                startActivity(intent);
                return true;
            });

            // On android O+ devices we want to take the user to the notification channel setting
            notificationNewSourceImportance.setOnPreferenceClickListener(preference -> {
                Log.d(TAG, "onPreferenceClick: notificationNewSourceImportance");
                Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, Const.NOTIFICATION_NEW_SOURCE_DETECTED_CHANNEL_ID);
                startActivity(intent);
                return true;
            });
        } else {
            // we enable the exposure importance and threshold settings based in the state of the
            // exposure notification setting.
            notificationExposureImportance.setEnabled(get_PREF_KEY_NOTIFICATION_EXPOSURE());
            notificationExposureThreshold.setEnabled(get_PREF_KEY_NOTIFICATION_EXPOSURE());

            /*
             * We enable/disable the exposure notification importance and threshold settings based on
             * whether the exposure notification is turned on or not
             */
            preferenceNotificationExposure.setOnPreferenceChangeListener((preference, newValue) -> {
                DbRequestHandler.dumpEventToDatabase(
                        (boolean) newValue ? Const.EVENT_SETTINGS_NOTIFICATION_EXPOSURE_ON :
                                Const.EVENT_SETTINGS_NOTIFICATION_EXPOSURE_OFF
                );
                notificationExposureImportance.setEnabled((boolean) newValue);
                notificationExposureThreshold.setEnabled((boolean) newValue);
                return true;
            });

            preferenceNotificationExposure.setChecked(get_PREF_KEY_NOTIFICATION_EXPOSURE());
            preferenceNotificationNewSource.setChecked(get_PREF_KEY_NOTIFICATION_NEW_SOURCE());

            /*
             * We enable/disable the new source notification importance setting based on whether
             * the new source notification is turned on or not
             */
            notificationNewSourceImportance.setEnabled(get_PREF_KEY_NOTIFICATION_NEW_SOURCE());
            preferenceNotificationNewSource.setOnPreferenceChangeListener((preference, newValue) -> {
                DbRequestHandler.dumpEventToDatabase(
                        (boolean) newValue ? Const.EVENT_SETTINGS_NOTIFICATION_NEW_SOURCE_ON :
                                Const.EVENT_SETTINGS_NOTIFICATION_NEW_SOURCE_OFF
                );
                notificationNewSourceImportance.setEnabled((boolean) newValue);
                return true;
            });
        }
    }

    /**
     * We use the onWindowFocusChanged() of the SettingsActivity to update the status of various
     * settings.
     */
    public void onWindowFocusChanged() {
        // We use the window focus change event to refresh the status of the various Preferences.
        refreshCustomStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * A method that checks the statuses of various settings and updates them.
     */
    public void refreshCustomStatus() {
        // NOTE: We need to check !null condition for each of them because we may arrive here before
        // they are initialized.
        Log.d(TAG, "In refreshCustomStatus.");
        if (backgroundWiFiScan != null) {
            boolean wiFiBackgroundScanON = Tools.isWiFiBackgroundScanON();
            backgroundWiFiScan.setChecked(wiFiBackgroundScanON);
            Log.d(TAG, "Set backgroundWiFiScan to " + wiFiBackgroundScanON);
        }
    }

    /**
     * If we do not implement this method,
     * {@link #onPreferenceDisplayDialog(PreferenceFragmentCompat, Preference)} does not work
     * properly. This is because the default implementation returns {@code null}
     *
     * @return The current preference fragment object
     */
    @Override
    public Fragment getCallbackFragment() {
        return this;
    }

    /**
     * This method is called before a preference dialog is displayed. If this method returns
     * false the dialog gets displayed. The dialog is not shown if it returns true.
     *
     * @param preferenceFragmentCompat the parent preference fragment
     * @param preference               the preference on result of witch the dialog tries to be displayed
     * @return false to display the dialog, and true to not show it.
     */
    @Override
    public boolean onPreferenceDisplayDialog(
            @NonNull PreferenceFragmentCompat preferenceFragmentCompat,
            Preference preference) {
        // For API >= 26 we want to prevent the notification importance dialog from being displayed
        // Prevent the notification importance to show for API >= 26
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && (getString(R.string.PREF_KEY_NOTIFICATION_EXPOSURE_IMPORTANCE).equals(preference.getKey()) ||
                getString(R.string.PREF_KEY_NOTIFICATION_NEW_SOURCE_IMPORTANCE).equals(preference.getKey()));
    }
}

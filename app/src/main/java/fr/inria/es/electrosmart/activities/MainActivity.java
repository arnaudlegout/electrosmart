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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.UserProfile;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.fragments.AdviceFragment;
import fr.inria.es.electrosmart.fragments.ArticleWebViewFragment;
import fr.inria.es.electrosmart.fragments.DailyStatSummaryFragment;
import fr.inria.es.electrosmart.fragments.ErrorFragment;
import fr.inria.es.electrosmart.fragments.ExposureDetailsFragment;
import fr.inria.es.electrosmart.fragments.HomeFragment;
import fr.inria.es.electrosmart.fragments.MainActivityFragment;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.fragments.OwaynFragment;
import fr.inria.es.electrosmart.fragments.SolutionsFragment;
import fr.inria.es.electrosmart.fragments.StatisticsFragment;
import fr.inria.es.electrosmart.fragmentstates.StatisticsFragmentState;
import fr.inria.es.electrosmart.monitors.BluetoothMonitor;
import fr.inria.es.electrosmart.monitors.CellularMonitor;
import fr.inria.es.electrosmart.monitors.DeviceInfoMonitor;
import fr.inria.es.electrosmart.monitors.LocationMonitor;
import fr.inria.es.electrosmart.monitors.WifiMonitor;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;
import fr.inria.es.electrosmart.util.VisibilityGroup;
import fr.inria.es.electrosmart.util.VisibilityGroupImpl;
import fr.inria.es.electrosmart.util.VisibilityView;

public class MainActivity extends AppCompatActivity implements
        SolutionsFragment.OnTestOwaynListener,
        MainActivityFragment.OnNavigateToFragmentListener {

    // result code used by the ResultReceiver
    public static final int RECEIVED_RAW_SIGNALS = 100;
    // used to identify the permission request in the callback processRequestPermissionsResult()
    public static final int REQUEST_ACCESS_FINE_LOCATION = 1;
    // used to identify the fine and background location permission request in processRequestPermissionsResult
    public static final int REQUEST_ACCESS_FINE_AND_BACKGROUND_LOCATION = 2;
    // used to identify the background location permission request in processRequestPermissionsResult
    public static final int REQUEST_ACCESS_BACKGROUND_LOCATION = 3;

    private static final String TAG = "MainActivity";
    // A reference to the user profile
    public static UserProfile sUserProfile;
    // contains the signal slot the AdviceFragment (and other related fragments) must use
    public static SignalsSlot stateAdviceFragmentSignalSlot;
    // stores the state of the StatisticsFragment that enables quick recreation of the fragment
    // from a previously queried result
    public static StatisticsFragmentState sStatisticsFragmentState;
    public static HashSet<BaseProperty> sAllTop5Signals;
    // the receiver used by the RawSignalHandler to set the received signals to the activity
    public static SignalDataReceiver signalDataReceiver;
    // represents the list of SignalsSlot, one object per time slot.
    public Timeline mLiveTimeline = new Timeline(Const.CHART_MAX_SLOTS_LIVE, Const.LIVE_TIME_GAP);
    // The toolbar view
    public Toolbar mToolbar;
    // The drawer toggle
    ActionBarDrawerToggle mDrawerToggle;
    // The navigation drawer, that is the container of all main activity's
    // views
    private DrawerLayout mDrawer;
    // A boolean to hold whether back arrow click listener was registered or not
    private boolean mToolBarBackButtonListenerIsRegistered = false;
    // The bundle object that is used to share parameters between this activity
    // and its different fragments
    private Bundle mFragmentsArguments;
    // A reference to hold the currently shown fragment object
    private MainActivityFragment mFragment;
    // An object to collectively control measurement header elements visibility
    private VisibilityGroup mMeasurementHeaderElements;
    // The measurement header elements container view (The ConstaintLayout containing the
    // progress bar and the e-score text views)
    private View mHeaderMeasurement;
    // The text view at the top (in the header) of a navigation drawer
    private TextView navigationHeaderTextView;
    // we get a hook to the shared preferences, the initialization is made in onCreate
    private SharedPreferences settings;
    private NavigationView mNavigationView;
    private BottomNavigationView mBottomNavigationView;
    private FrameLayout mFragmentContainer;
    private NavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnBottomNavigationItemSelectedListener;

    // keep track of the previously loaded fragment. This is used to do not reload an already
    // loaded fragment.
    private int mPreviousBottomNavId = Integer.MIN_VALUE;

    /**
     * The onCreate method is the starting point of ElectroSmart and all the initial settings are
     * handled in it.
     *
     * @param savedInstanceState The saved instance used by Android to restore previous state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        Tools.setStatusBarColor(this);
        super.onCreate(savedInstanceState);

        // we get the SharedPreferences
        if (settings == null) {
            settings = getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        }

        // if this is the first time we run the application,
        // we create the test databased if Const.MAKE_DEBUG_DB is true
        if ((settings.getBoolean(Const.IS_FIRST_RUN, true))) {
            Log.d(TAG, "onCreate: First run of the application after a fresh install " +
                    "(create the test database if in DEBUG)");
            // We test is the application is a debug/test version. If true, we populate the local
            // database with fake data for test, otherwise, the database starts with the legacy data
            // if it is an update or with no data if it is a new install
            if (Const.MAKE_DEBUG_DB) {
                DbRequestHandler.createDebugDatabase();
            }
            settings.edit().putBoolean(Const.IS_FIRST_RUN, false).apply();
        }

        // we create a new SignalDataReceiver
        signalDataReceiver = new SignalDataReceiver(new Handler());

        mMeasurementHeaderElements = new VisibilityGroupImpl();
        mMeasurementHeaderElements.addItem(new VisibilityView(findViewById(R.id.tabs)));
        mMeasurementHeaderElements.addItem(new VisibilityView(findViewById(R.id.exposure_summary_progress_bar)));
        mMeasurementHeaderElements.addItem(new VisibilityView(findViewById(R.id.exposure_summary_value)));
        mMeasurementHeaderElements.addItem(new VisibilityView(findViewById(R.id.exposure_summary_metric)));
        mMeasurementHeaderElements.addItem(new VisibilityView(findViewById(R.id.exposure_summary_scale)));

        mFragmentsArguments = new Bundle();

        // Other than english, we only support french, german and italian.
        // If none of the these is the language used in this device, we fall back to english.
        Locale locale = Tools.getDefaultDeviceLocale();
        mFragmentsArguments.putSerializable(Const.MAIN_ACTIVITY_LOCALE_ARG_KEY, locale);

        mHeaderMeasurement = findViewById(R.id.header_measurement);


        mToolbar = findViewById(R.id.main_activity_toolbar);
        mToolbar.setTitleTextColor(getResources().getColor(R.color.primary_text_color));

        // We set this because later on we use getSupportActionBar() and add the arrow button in the
        // ExposureDetailsFragment
        setSupportActionBar(mToolbar);
        mHeaderMeasurement.setBackgroundColor(getResources().getColor(R.color.regular_background_color));

        // Find our drawer view
        mDrawer = findViewById(R.id.drawer_layout);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar,
                R.string.drawer_open, R.string.drawer_close);
        mDrawerToggle.syncState();
        mDrawer.addDrawerListener(mDrawerToggle);
        mNavigationView = findViewById(R.id.navigation_view);
        MenuItem exportMenuItem = mNavigationView.getMenu().findItem(R.id.nav_export);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // hide the CSV export feature for devices older than KitKat
            exportMenuItem.setVisible(false);
        }
        mBottomNavigationView = findViewById(R.id.bottom_navigation_view);
        mFragmentContainer = findViewById(R.id.fragment_container);

        // bottom navigation bar listener
        mOnBottomNavigationItemSelectedListener = menuItem -> {
            int id = menuItem.getItemId();

            Log.d(TAG, "onNavigationItemSelected: id=" + id);
            // action when we select an item in the bottom navigation bar
            if (mPreviousBottomNavId != id) {
                if (id == R.id.bottom_nav_home) {
                    mPreviousBottomNavId = R.id.bottom_nav_home;
                    showHomeFragment();
                } else if (id == R.id.bottom_nav_statistics) {
                    mPreviousBottomNavId = R.id.bottom_nav_statistics;
                    showDailyStatSummaryFragment();
                } else if (id == R.id.bottom_nav_solutions) {
                    mPreviousBottomNavId = R.id.bottom_nav_solutions;
                    showSolutionFragment();
                }
            }
            return true;
        };

        // drawer navigation listener
        mOnNavigationItemSelectedListener =
                menuItem -> {
                    int id = menuItem.getItemId();

                    // We don't inflate the fragment again if we do not change the fragment,
                    // that is, we select from the drawer the fragment already inflated
                    if (id == R.id.nav_home) {
                        showHomeFragment();
                    } else if (id == R.id.nav_instrument) {
                        setupMeasurementHeader();
                        mFragment = MeasurementFragment.newInstance(mFragmentsArguments);
                        mFragmentContainer.setBackgroundColor(getResources().getColor(R.color.lowest_level_background_color));
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, mFragment)
                                .commitAllowingStateLoss();
                    } else if (id == R.id.nav_tell_your_friends) {
                        Intent shareIntent = ShareCompat.IntentBuilder.from(MainActivity.this)
                                .setType("text/plain")
                                .setText(getString(R.string.nav_invite_text))
                                .getIntent();
                        if (shareIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(Intent.createChooser(
                                    shareIntent, getResources().getText(R.string.nav_send_to)
                            ));
                        }
                        DbRequestHandler.dumpEventToDatabase(
                                Const.EVENT_USER_TAPPED_TELL_YOUR_FRIENDS
                        );
                    } else if (id == R.id.nav_encourage_us) {
                        Log.d(TAG, "selectDrawerItem: encourage us");
                        String packageName = getPackageName();
                        if (packageName.endsWith(".debug")) {
                            packageName = packageName.replace(".debug", "");
                        }

                        Uri uri = Uri.parse("market://details?id=" + packageName);
                        Log.d(TAG, "selectDrawerItem: uri = " + uri.toString());
                        Intent goToMarketIntent = new Intent(Intent.ACTION_VIEW, uri);
                        try {
                            startActivity(goToMarketIntent);
                        } catch (ActivityNotFoundException anfe) {
                            Log.e(TAG, "selectDrawerItem: No suitable activity found to open the " +
                                    "gotoMarketIntent. Attempting to open google play web page");
                            uri = Uri.parse("http://play.google.com/store/apps/details?id=" + packageName);
                            goToMarketIntent = new Intent(Intent.ACTION_VIEW, uri);
                            try {
                                startActivity(goToMarketIntent);
                            } catch (ActivityNotFoundException ex) {
                                Log.e(TAG, "selectDrawerItem: No suitable activity found to open the " +
                                        "gotoMarketIntent web. Showing a dialog");
                                Tools.createInformationalDialog(
                                        MainActivity.this,
                                        getString(R.string.nav_encourage_us_err_title),
                                        getString(R.string.nav_encourage_us_err_text)
                                );
                            }

                        }
                        DbRequestHandler.dumpEventToDatabase(
                                Const.EVENT_USER_TAPPED_ENCOURAGE_US
                        );
                    } else if (id == R.id.nav_export) {
                        // Note that this menu item is not displayed for SDKs lower than
                        // 19 (KITKAT). We add a redundant test here to increase reliability
                        // and remove a lint warning
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            String now = new SimpleDateFormat("yyyy-MM-dd-HH'h'mm'm'ss's'",
                                    Locale.US).format(new Date());
                            String outputZipFileName = "electrosmart-csv-" + now + ".zip";

                            Log.d(TAG, "outputZipFileName: " + outputZipFileName);
                            openFilePickerStorageAccessFramework(outputZipFileName);
                        }
                    } else if (id == R.id.nav_feedback) {
                        startActivity(new Intent(MainActivity.this, FeedbackActivity.class));
                    } else if (id == R.id.nav_settings) {
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivityForResult(settingsIntent, Const.REQUEST_CODE_SETTING_ACTIVITY);
                    } else if (id == R.id.nav_help) {
                        Intent helpIntent = new Intent(MainActivity.this, HelpActivity.class);
                        startActivity(helpIntent);
                    }

                    // Highlight the selected item has been done by NavigationView
                    //menuItem.setChecked(true);
                    // Set action bar title
                    setTitle(menuItem.getTitle());
                    // Close the navigation drawer
                    mDrawer.closeDrawers();
                    return true;
                };

        // Here we select in the drawer and the bottom navigation bar the home landing page when
        // we create the activity
        mNavigationView.setCheckedItem(R.id.nav_home);
        mBottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);
        mOnNavigationItemSelectedListener.onNavigationItemSelected(mNavigationView.getCheckedItem());
        mBottomNavigationView.setOnNavigationItemSelectedListener(mOnBottomNavigationItemSelectedListener);

        mNavigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    if (mNavigationView.getCheckedItem() != menuItem ||
                            !mNavigationView.getCheckedItem().isChecked()) {
                        mNavigationView.getCheckedItem().setChecked(true);
                        return mOnNavigationItemSelectedListener.onNavigationItemSelected(menuItem);
                    } else {
                        mDrawer.closeDrawers();
                        return false;
                    }
                }
        );

        // get the header of the navigation drawer
        View headerView = mNavigationView.getHeaderView(0);
        // get the text view in the header
        navigationHeaderTextView = headerView.findViewById(R.id.nav_header_text_view);
        sUserProfile = DbRequestHandler.getUserProfile();

        // We attach the click listener to the full header of the drawer
        headerView.findViewById(R.id.nav_header).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        mLiveTimeline.setTimeOrigin(Tools.getLiveCurrentTimestamp());

        /*
        There are extras only in case the app is opened with a notification.
        The code below is used to define the behavior of the app depending on the kind of
        notifications that triggered the app.
        */
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Log.d(TAG, "onCreate: MainActivity -> extras is not null -> We came from notification tap");

            // If we open this activity as a result of a notification click
            int notificationType = extras.getInt(Const.NOTIFICATION_ARG_KEY, -1);

            if (notificationType == Const.NOTIFICATION_HIGH_EXPOSURE) {
                Log.d(TAG, "onCreate: Const.NOTIFICATION_HIGH_EXPOSURE");

                // Clear notifications history if we respond to a notification click
                MeasurementScheduler.sHighExposureNotificationHistory.clear();

            } else if (notificationType == Const.NOTIFICATION_FIRST_TIME_STATISTICS ||
                    notificationType == Const.NOTIFICATION_NEW_SOURCE_DETECTED) {
                Log.d(TAG, "onCreate: notificationType = " + notificationType);

                // Set the statistics fragment state to null so that we open yesterday's exposure
                // stats by default
                MainActivity.sStatisticsFragmentState = null;

                // By default if NOTIFICATION_DATE_TIMEMILLIS_ARG_KEY is absent we fallback to
                // yesterday
                Calendar yesterdayCal = Calendar.getInstance();
                yesterdayCal.add(Calendar.DAY_OF_MONTH, -1);
                long notificationTimeMillis = extras.getLong(Const.NOTIFICATION_DATE_TIMEMILLIS_ARG_KEY,
                        yesterdayCal.getTimeInMillis());

                // Show the statistics fragment of the given statDay
                Calendar statDay = Calendar.getInstance();
                statDay.setTimeInMillis(notificationTimeMillis);
                Tools.roundCalendarAtMidnight(statDay);
                showStatisticsFragment(statDay, true);
            }
        }

        // by default (when we reach here), the HomeFragment is opened.
    }

    /**
     * Open a file picker from the storage access framework that allows to select a file to write
     * on a shared storage without any specific permission.
     * https://developer.android.com/training/data-storage/shared/documents-files
     * <p>
     * It is only available for API 19 (KITKAT) and higher
     *
     * @param outputFileName the name of the output file as string
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void openFilePickerStorageAccessFramework(String outputFileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/csv");
        intent.putExtra(Intent.EXTRA_TITLE, outputFileName);
        startActivityForResult(intent, Const.RESULT_CODE_EXPORT_CSV_FILES);
    }

    public void showSolutionFragment() {
        setupSolutionHeader();
        mFragment = SolutionsFragment.newInstance(mFragmentsArguments);
        mFragmentContainer.setBackgroundColor(getResources().getColor(R.color.regular_background_color));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
    }

    public void showHomeFragment() {
        setupHomeHeader();
        mFragment = new HomeFragment();
        mFragmentContainer.setBackgroundColor(getResources().getColor(R.color.regular_background_color));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
    }

    /**
     * After 1.19, we decided to have a list view of daily summary stats in the landing
     * statistics page. The new fragment is called DailyStatSummaryFragment. It contains a
     * list of dates with corresponding exposures. On tapping on one of the dates, the user
     * is taken to the StatisticsFragment.
     */
    public void showDailyStatSummaryFragment() {
        setupDailyStatSummaryHeader();
        mFragment = new DailyStatSummaryFragment();
        mFragmentContainer.setBackgroundColor(getResources().getColor(R.color.regular_background_color));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
    }


    /**
     * Loads the statistics fragment.
     *
     * @param statDay         The day on which we want to load statistics
     * @param isStatAvailable if set to false, we do not load statistics and show a default
     *                        "missing stats" message. Note that if set to true and there is no
     *                        stats, it will not make an error, but search in the DB for signals
     *                        and eventually show the "missing stat" message.
     */
    public void showStatisticsFragment(Calendar statDay, boolean isStatAvailable) {
        setupStatisticsHeader();
        mFragment = new StatisticsFragment();
        mFragmentContainer.setBackgroundColor(getResources().getColor(R.color.regular_background_color));
        mFragmentsArguments.putSerializable(
                Const.DAILY_STAT_SUMMARY_TO_STATISTICS_STAT_DAY_ARG_KEY, statDay);
        mFragmentsArguments.putSerializable(
                Const.DAILY_STAT_SUMMARY_TO_STATISTICS_IS_STAT_AVAILABLE_ARG_KEY, isStatAvailable);
        mFragment = StatisticsFragment.newInstance(mFragmentsArguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
    }

    public void showErrorFragment(int errorType) {
        setupHomeHeader();
        Bundle argBundle = new Bundle();
        argBundle.putInt(Const.MAIN_ACTIVITY_ERROR_TYPE_ARG_KEY, errorType);
        mBottomNavigationView.setVisibility(View.GONE);
        ErrorFragment errorFragment = ErrorFragment.newInstance(argBundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, errorFragment)
                .commitAllowingStateLoss();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "in onResume()");
        super.onResume();

        if (settings.getBoolean(Const.IS_TERMS_OF_USE_ACCEPTED, false)) {
            Log.d(TAG, "in onResume(): Terms of use already accepted.");
        } else {
            showWelcome();
            return;
        }

        if (!settings.getBoolean(Const.IS_ONBOARDING_DONE, false)) {
            Log.d(TAG, "Onboarding is not yet completed. So, go through onboarding");
            showOnboarding();
            return;
        }


        if (settings.getBoolean(Const.IS_AGREEMENT_FLOW_DONE, false)) {
            Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is true, we start the foreground service");
            // we create the alarm for the first time statistics notification to be shown
            if (!Tools.isFirstTimeStatisticsNotificationShown()) {
                MeasurementScheduler.createFirstTimeStatisticsNotificationAlarm();
            }

            // we must recover from pause before starting the foreground service
            recoverFromPause();

            // onResume we start all antennas, start the PhoneStateListener, and start the
            // foreground service
            startAllServices();

            // we update resume statistics for the MainActivity
            Tools.updateMainActivityStats();

            // trigger the in-app review
            Tools.askInAppReview(this, this);
        } else {
            Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is false, we DO NOT start the " +
                    "foreground service.");
        }

        // We update the text in the header of the navigation drawer
        if (sUserProfile != null && navigationHeaderTextView != null) {
            String profileName;
            if (sUserProfile.getName() == null || sUserProfile.getName().isEmpty()) {
                profileName = getString(R.string.edit_your_profile);
            } else {
                profileName = sUserProfile.getName();
            }
            navigationHeaderTextView.setText(profileName);
        }
        Menu menu = mNavigationView.getMenu();
        if (SettingsPreferenceFragment.get_PREF_KEY_SHOW_INSTRUMENT()) {
            // show Instrument item in the left navigation bar
            menu.findItem(R.id.nav_instrument).setVisible(true);
        } else {
            // hide Instrument item in the left navigation bar
            menu.findItem(R.id.nav_instrument).setVisible(false);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "in onPause()");
        super.onPause();
        // We start startBackgroundMeasurementScheduler if we onPause the MainActivity, and the
        // Const.IS_AGREEMENT_FLOW_DONE is true.
        MeasurementScheduler.restoreInitialCardStateAndStopBroadcastReceivers(true);
        if (settings.getBoolean(Const.IS_AGREEMENT_FLOW_DONE, false)) {
            Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is true, we start the background service");
            startBackgroundMeasurementScheduler();
        } else {
            Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is false, we DO NOT start the " +
                    "background service.");
        }
    }

    /**
     * A refactored method that enables all cards and starts measurements
     */
    public static void startAllServices() {
        Log.d(TAG, "in startAllServices()");
        DeviceInfoMonitor.run(true);
        WifiMonitor.enableWifi(true);
        if (SettingsPreferenceFragment.get_PREF_KEY_ENABLE_BT_AUTOMATICALLY()) {
            BluetoothMonitor.enableBluetooth(true);
        }
        LocationMonitor.startLocationMonitor(true);
        CellularMonitor.registerMyPhoneStateListener();
        startForegroundMeasurementScheduler();
    }

    /**
     * Adds empty data slots that represent missed slots while the activity is in the
     * paused state
     */
    private void recoverFromPause() {
        Log.d(TAG, "In recoverFromPause()");
        // Add empty signal slots to the timeline to represent
        // missed measurements while the activity was in pause state
        final long currentTimestamp = Tools.getLiveCurrentTimestamp();
        final int lastIndex = mLiveTimeline.size() == 0 ? 0 : mLiveTimeline.size() - 1;
        final long lastTimestamp = mLiveTimeline.getTimeOrigin() + lastIndex * Const.LIVE_TIME_GAP;
        final int missedSlots = (int) ((currentTimestamp - lastTimestamp) / Const.LIVE_TIME_GAP);

        if (missedSlots > Const.CHART_MAX_SLOTS_LIVE) {
            Log.d(TAG, "recoverFromPause: we reset the liveTimeline because missedSlots (" +
                    missedSlots + ") is > to Const.CHART_MAX_SLOTS_LIVE " + Const.CHART_MAX_SLOTS_LIVE);
            resetLiveTimeline();
        } else if (missedSlots > 0) {
            Log.d(TAG, "recoverFromPause: we add " + missedSlots + " missed slots to mLiveTimeline");
            for (int i = 0; i < missedSlots; i++) {
                mLiveTimeline.add(new SignalsSlot());
            }
            mLiveTimeline.shrinkLeft(Const.CHART_MAX_SLOTS_LIVE);
            sendLiveTimelineUpdateToFragments(missedSlots);
        }
    }

    /**
     * launch the services for current mode
     */
    public static void startForegroundMeasurementScheduler() {
        Log.d(TAG, "in startForegroundMeasurementScheduler()");
        Log.d(TAG, "stop MeasurementScheduler...");
        MeasurementScheduler.stopMeasurementScheduler();
        Log.d(TAG, "stop MeasurementScheduler... DONE");
        Log.d(TAG, "start MeasurementScheduler...");
        MeasurementScheduler.startMeasurementScheduler(MeasurementScheduler.SchedulerMode.FOREGROUND,
                signalDataReceiver);
        Log.d(TAG, "start MeasurementScheduler... DONE");
    }


    /**
     * launch the services for background mode
     */
    public void startBackgroundMeasurementScheduler() {
        Log.d(TAG, "in startBackgroundMeasurementScheduler()");
        Log.d(TAG, "stop MeasurementScheduler...");
        MeasurementScheduler.stopMeasurementScheduler();
        Log.d(TAG, "stop MeasurementScheduler... DONE");
        Log.d(TAG, "start MeasurementScheduler...");
        MeasurementScheduler.startMeasurementScheduler(MeasurementScheduler.SchedulerMode.BACKGROUND,
                null);
        Log.d(TAG, "start MeasurementScheduler... DONE");
    }


    /**
     * Prepares the app header for the protection fragment. Should be called before the
     * protection fragment is inflated.
     */
    private void setupSolutionHeader() {
        setBackButtonOnFragment(false);
        mBottomNavigationView.setVisibility(View.VISIBLE);
        mMeasurementHeaderElements.setVisibility(View.GONE);
        mHeaderMeasurement.getLayoutParams().height =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        // The measurement fragment has no title (We use the e-score indicator instead)
        // Here we set a title
        setupToolbar(R.string.solutions);
    }


    /**
     * Prepares the app header for the owayn fragment. Should be called before the
     * owayn fragment is inflated.
     */
    private void setupOwaynHeader() {
        // display the back arrow in the action bar
        setBackButtonOnFragment(true);
        // hide the bottom navigation bar
        mBottomNavigationView.setVisibility(View.GONE);
        mMeasurementHeaderElements.setVisibility(View.GONE);
        mHeaderMeasurement.getLayoutParams().height =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        // The measurement fragment has no title (We use the e-score indicator instead)
        // Here we set a title
        setupToolbar(R.string.owayn);
    }

    /**
     * Prepares the app header for statistics fragment. Should be called before the
     * statistics fragment is inflated.
     */
    private void setupStatisticsHeader() {
        setBackButtonOnFragment(true);
        mBottomNavigationView.setVisibility(View.GONE);
        mMeasurementHeaderElements.setVisibility(View.GONE);
        mHeaderMeasurement.getLayoutParams().height =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        // set the color to the action bar
        mHeaderMeasurement.setBackgroundColor(
                ContextCompat.getColor(this, R.color.regular_background_color)
        );
    }

    /**
     * Identical to {@link #setupStatisticsHeader()} except for that there is a hamburger icon
     * instead of the back button, and the bottom navigation is visible.
     */
    private void setupDailyStatSummaryHeader() {
        setBackButtonOnFragment(false);
        mBottomNavigationView.setVisibility(View.VISIBLE);
        mMeasurementHeaderElements.setVisibility(View.GONE);
        mHeaderMeasurement.getLayoutParams().height =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        // set the color to the action bar
        mHeaderMeasurement.setBackgroundColor(
                ContextCompat.getColor(this, R.color.regular_background_color)
        );
        setupToolbar(R.string.statistics_title);
    }

    /**
     * Setup the toolbar of the MainActivity. This method sets the title and hide the calendar
     * image view.
     * <p>
     * Note that we customized the layout of the toolbar to include an image view to the right
     * (for the calendar in the statistics fragment). By default, this image is always invisible.
     *
     * @param title A string value for the title of the toolbar
     */
    public void setupToolbar(String title) {
        mToolbar.setTitle(title);
    }

    /**
     * Setup the toolbar of the MainActivity.
     *
     * @param titleResourceId An integer representing a string resource id
     */

    public void setupToolbar(int titleResourceId) {
        setupToolbar(getString(titleResourceId));
    }


    private void setupHomeHeader() {
        setBackButtonOnFragment(false);
        mBottomNavigationView.setVisibility(View.VISIBLE);
        mMeasurementHeaderElements.setVisibility(View.GONE);
        mHeaderMeasurement.getLayoutParams().height =
                ViewGroup.LayoutParams.WRAP_CONTENT;

        // set the color to the action bar
        mHeaderMeasurement.setBackgroundColor(
                ContextCompat.getColor(this, R.color.regular_background_color)
        );

        setupToolbar(R.string.home_title);
    }

    /**
     * Prepares the app header for the measurement fragment. Should be called before the
     * measurement fragment is inflated.
     */
    private void setupMeasurementHeader() {
        setBackButtonOnFragment(false);
        mBottomNavigationView.setVisibility(View.GONE);
        mMeasurementHeaderElements.setVisibility(View.VISIBLE);
        mHeaderMeasurement.getLayoutParams().height =
                (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        100,
                        getResources().getDisplayMetrics()
                );

        setupToolbar("");
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(Gravity.LEFT)) {
            mDrawer.closeDrawer(Gravity.LEFT);
        } else {
            boolean isBackPressedHandled = false;
            /*
            We send the backpress to the current fragment. If it is handled by the fragment, it
            returns true, otherwise we propagate the back press to the system.

            We must test mFragment isAdded() because the backpress cannot be handled by
            the fragment as long as it is not attached to the activity.
            */
            if (mFragment.isAdded()) {
                isBackPressedHandled = mFragment.onBackPressed();
            }
            if (!isBackPressedHandled) {
                super.onBackPressed();
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The call below is necessary in order to propagate the event to the underlying fragments
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "in onActivityResult(): requestCode " + requestCode + " resultCode " + resultCode);

        /*
        The logic is the following:
        - When we change the colors we must restart the activities so that the new color takes effect.
        - When the color is changed we are in the setting activity, we call setResult() with
          Const.RESULT_CODE_COLOR_CHANGED, and finish the setting activity. The result code is
          captured below. Then we restart the main activity (to apply all color changes) in current
          mode so that the user can immediately see the effect of the color change.
         */
        if (requestCode == Const.REQUEST_CODE_SETTING_ACTIVITY) {
            if (resultCode == Const.RESULT_CODE_COLOR_CHANGED) {
                /*
                 We restart the activity if the color theme has been changed in order to apply it.

                 Note: an activity can be recreated with recreate(). However, the behavior is
                       different from finish();startActivity(intent). recreate() keeps some states,
                       one advantage is that we can, for instance, restart the activity in the mode
                       it was before, but one drawback is that some variables might not be
                       correctly initialized.
                       https://developer.android.com/reference/android/app/Activity.html#recreate()

                       As I don't fully understand how the state is handled
                       during the transition, and as in case of color changes it is safer to fully
                       restart the activity, I preferred the option with finish();startActivity().
                  */
                Intent intent = getIntent();
                Log.d(TAG, "onActivityResult: intent:" + intent);
                finish();
                startActivity(intent);
                // After restarting the activity we start again the setting activity
/*                Intent settingsIntent = new Intent(MeasurementFragment.this, SettingsActivity.class);
                startActivityForResult(settingsIntent, Const.REQUEST_CODE_SETTING_ACTIVITY);*/
            }
        } else if (requestCode == Const.REQUEST_CODE_WELCOME_ACTIVITY ||
                requestCode == Const.REQUEST_CODE_WELCOME_TERMS_OF_USE) {
            if (resultCode == Const.RESULT_CODE_FINISH_ES) {
                Log.d(TAG, "in onActivityResult(): User either back pressed or refused the terms of " +
                        "use in the WelcomeTermsOfUseActivity. Hence, we close the application.");
                finish();
            } else if (resultCode == Const.RESULT_CODE_ONBOARDING_DONE) {
                Log.d(TAG, "in onActivityResult(): Onboarding done");
            }
        } else if (requestCode == Const.RESULT_CODE_EXPORT_CSV_FILES && resultCode == Activity.RESULT_OK) {
            // The result of this activity is a URI of the file in which we have the authorization
            // to write, as chosen by the user (thanks to the storage access framework)
            // We pass this URI to the async task that uses this to store the generated zip file
            if (data != null) {
                Uri uri = data.getData();
                new DbRequestHandler.exportCsvAsyncTask(this).execute(uri);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Intent intent = new Intent();
        intent.setAction(Const.MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcastSync(intent);
    }

    private void showWelcome() {
        Log.d(TAG, "in showWelcome()");
        Intent welcometIntent = new Intent(this, WelcomeActivity.class);
        startActivityForResult(welcometIntent, Const.REQUEST_CODE_WELCOME_ACTIVITY);
    }

    /**
     * Send an intent to all fragments to notify that the LiveTimeline has been updated by
     * nbNewSlots slots
     *
     * @param nbNewSlots The number of new slots in the LiveTimeline
     */
    private void sendLiveTimelineUpdateToFragments(int nbNewSlots) {
        Intent intent = new Intent();
        intent.setAction(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION);
        intent.putExtra(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATE_NB_SLOTS_EXTRA_KEY, nbNewSlots);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MainActivity.this);
        localBroadcastManager.sendBroadcastSync(intent);
    }

    /**
     * Reset the LiveTimeline data structure.
     */
    private void resetLiveTimeline() {
        Log.d(TAG, "in resetLiveTimeline()");
        mLiveTimeline.reset(Tools.getLiveCurrentTimestamp());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "in onRequestPermissionsResult()");
        Tools.processRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * This method must only be the first time.
     * <p>
     * The startActivityForResult() is used to send a result to the MeasurementFragment when we
     * accept the terms of use and click on "Let's start" for the first time in order to reset the
     * chart to be sure there is no chart the first time we start the app.
     */
    private void showOnboarding() {
        Log.d(TAG, "in showOnboarding()");
        Intent intent = new Intent(this, OnBoardingActivity.class);
        startActivityForResult(intent, Const.REQUEST_CODE_WELCOME_TERMS_OF_USE);
    }


    @Override
    public void onTestOwayn() {
        setupOwaynHeader();
        mNavigationView.getCheckedItem().setChecked(false);
        mFragment = OwaynFragment.newInstance(mFragmentsArguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
    }

    /**
     * Called when we navigate from the bottom navigation bar
     *
     * @param itemId item ID to navigate to
     */
    @Override
    public void onBottomNavigateTo(int itemId) {
        MenuItem menuItem = selectAndGetBottomNavItem(itemId);
        if (menuItem != null) {
            mOnBottomNavigationItemSelectedListener.onNavigationItemSelected(menuItem);
        }
    }

    /**
     * called when we want to show the exposition details fragment
     *
     * @param antennaDetailsKey The antenna display to expand on
     * @param signalsSlot       the signal slot to display. If it is an invalid SignalSlot, that
     *                          is SignalsSlot.containsValidSignals() return false
     *                          the {@link ExposureDetailsFragment} is updated continuously,
     *                          otherwise it is locked to the passed SignalSlot.
     * @param signalIndex       The index in the childViewSignals for bluetooth/cellular and
     *                          sortedWifiGroupSignals lists where we can find the signal to
     *                          highlight. If signalsSlot is null, this parameter is not taken
     *                          into account.
     * @param statDay           Represents the day for which we show the statistics.
     *                          This parameter is passed when we should go back from the
     *                          {@link ExposureDetailsFragment} to the {@link StatisticsFragment}.
     *                          This parameter must be set to null if we should not go back
     *                          to the statistics fragment.
     * @param fragmentTitle     A string value which will be set as the title of the Statistics
     *                          fragment; When set to null the title reverts back to the classic
     */

    public void showExposureDetails(MeasurementFragment.AntennaDisplay antennaDetailsKey,
                                    SignalsSlot signalsSlot, int signalIndex, Calendar statDay,
                                    String fragmentTitle) {
        Log.d(TAG, "showExposureDetails: Show ExposureDetailsFragment");
        setBackButtonOnFragment(true);
        stateAdviceFragmentSignalSlot = signalsSlot;

        mFragmentsArguments.putSerializable(
                Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_STAT_DAY_ARG_KEY, statDay);
        mFragmentsArguments.putSerializable(
                Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_ANTENNA_DISPLAY_ARG_KEY, antennaDetailsKey);
        mFragmentsArguments.putSerializable(
                Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_SIGNAL_INDEX_ARG_KEY, signalIndex);
        mFragment = ExposureDetailsFragment.newInstance(mFragmentsArguments);

        if (fragmentTitle != null) {
            setupToolbar(fragmentTitle);
        } else {
            // we set the fragment title according to the signal type
            if (antennaDetailsKey == MeasurementFragment.AntennaDisplay.WIFI) {
                setupToolbar(R.string.exposition_details_wifi);
            } else if (antennaDetailsKey == MeasurementFragment.AntennaDisplay.CELLULAR) {
                setupToolbar(R.string.exposition_details_cellular);
            } else {
                setupToolbar(R.string.exposition_details_bluetooth);
            }
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
        mBottomNavigationView.setVisibility(View.GONE);
    }

    /**
     * called when we want to show the advice details fragment
     *
     * @param signalsSlot the signal slot the advice will be based on
     */
    public void showAdviceFragment(SignalsSlot signalsSlot) {
        Log.d(TAG, "showAdviceFragment: Show showAdviceFragment");
        setBackButtonOnFragment(true);
        stateAdviceFragmentSignalSlot = signalsSlot;

        mFragment = AdviceFragment.newInstance(mFragmentsArguments);
        setupToolbar(
                String.format(
                        getString(R.string.advice_fragment_title),
                        Tools.getRecommendationTextBasedOnDbm(this,
                                signalsSlot.getSlotCumulativeTotalDbmValue()).toLowerCase()
                )
        );
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
        mBottomNavigationView.setVisibility(View.GONE);
    }

    /**
     * called when we want to show the StatisticsFragment fragment
     *
     * @param signalsSlot the signal slot the advice will be based on
     * @param firstDay    the first day to start the request in getSignalsSlotFromDb in the StatisticsFragment.
     *                    Setting this to null will return to StatisticsFragment with yesterday's exposition
     */
    public void showStatisticsFragment(SignalsSlot signalsSlot, Calendar firstDay) {
        Log.d(TAG, "showStatisticsFragment: Show showStatisticsFragment");
        setupStatisticsHeader();
        stateAdviceFragmentSignalSlot = signalsSlot;
        mFragmentsArguments.putSerializable(
                Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_STAT_DAY_ARG_KEY, firstDay);
        mFragment = StatisticsFragment.newInstance(mFragmentsArguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
    }

    /**
     * Returns the localized URL for the article on the exposure score
     *
     * @return The URL for the correct language
     */
    private String getAdviceReadOurArticleUrl() {
        String returnValue = "";
        if (Tools.getDefaultDeviceLocale().equals(Locale.FRENCH)) {
            returnValue = Const.ADVICE_READ_OUR_ARTICLE_URL_FR;
        } else if (Tools.getDefaultDeviceLocale().equals(Locale.GERMAN)) {
            returnValue = Const.ADVICE_READ_OUR_ARTICLE_URL_DE;
        } else if (Tools.getDefaultDeviceLocale().equals(Locale.ITALIAN)) {
            returnValue = Const.ADVICE_READ_OUR_ARTICLE_URL_IT;
        } else if (Tools.getDefaultDeviceLocale().equals(new Locale("es"))) {
            returnValue = Const.ADVICE_READ_OUR_ARTICLE_URL_ES;
        } else if (Tools.getDefaultDeviceLocale().equals(new Locale("pt"))) {
            returnValue = Const.ADVICE_READ_OUR_ARTICLE_URL_PT;
        } else {
            returnValue = Const.ADVICE_READ_OUR_ARTICLE_URL_EN;
        }
        // Check if the user has set the dark mode in the app OR if the user has enabled system-wide
        // dark mode and if set, return the themed version of the advice article
        // Notes:
        // 1. All the dark-themed articles have similar urls as the non-themed version
        //    except that they end with "/dm". (dm stands for the "dark mode")
        //    E.g. https://electrosmart.app/calcul-de-lindice-en/     Non-themed url
        //         https://electrosmart.app/calcul-de-lindice-en/dm   Dark-themed url
        //
        // 2. It may be possible to do this completely using CSS (prefers-color-scheme: dark),
        //    however I found compatibility issues on different versions of android and different
        //    versions of browser. Hence, I chose to go with solution 1)
        //    Some refs:
        //    - https://joebirch.co/android/enabling-dark-theme-in-android-webviews/
        //    - https://nandovieira.com/supporting-dark-mode-in-web-content
        //

        if (SettingsPreferenceFragment.get_PREF_KEY_DARK_MODE() ||
                (getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            returnValue = returnValue + "dm";
        }
        Log.d(TAG, "getAdviceReadOurArticleUrl: + " + returnValue);
        return returnValue;
    }

    /**
     * Shows the {@link ArticleWebViewFragment} fragment
     *
     * @param signalsSlot the signal slot from which {@link ArticleWebViewFragment}
     *                    has been called. This is used for the back action to come back
     *                    to the same slot.
     */
    public void showArticleWebViewFragment(SignalsSlot signalsSlot) {
        setBackButtonOnFragment(true);
        stateAdviceFragmentSignalSlot = signalsSlot;

        mFragmentsArguments.putSerializable(
                Const.MAIN_ACTIVITY_ARTICLE_WEBVIEW_FRAGMENT_URL_ARG_KEY,
                getAdviceReadOurArticleUrl()
        );
        mFragment = ArticleWebViewFragment.newInstance(mFragmentsArguments);
        setupToolbar(R.string.article_webview_electrosmart_index_title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mFragment)
                .commitAllowingStateLoss();
        mBottomNavigationView.setVisibility(View.GONE);
    }


    /**
     * Select in the drawer (NavigationView) the item passed as argument.
     *
     * @param itemId the item resource ID to select in the navigation drawer
     */
    public void selectAndGetNavItem(final int itemId) {
        final MenuItem itemToBeChecked = mNavigationView.getMenu().findItem(itemId);
        if (mNavigationView.getCheckedItem() != itemToBeChecked || !mNavigationView.getCheckedItem().isChecked()) {
            itemToBeChecked.setChecked(true);
        }
    }

    /**
     * Select in the bottom navigation bar the item passed as argument.
     *
     * @param itemId the item resource ID to select in the navigation drawer
     * @return instance of the just checked menu item
     */
    public MenuItem selectAndGetBottomNavItem(int itemId) {
        MenuItem itemToBeSelected = mBottomNavigationView.getMenu().findItem(itemId);
        if (mBottomNavigationView.getSelectedItemId() != itemId) {
            itemToBeSelected.setChecked(true);
        }
        return itemToBeSelected;
    }

    /**
     * Used to enable or disable the back arrow in the action bar. When disabled, the
     * hamburger icons is shown.
     *
     * @param enable When true replaces the hamburger icon with the back button and vice versa
     */
    private void setBackButtonOnFragment(boolean enable) {
        if (enable) {
            // Whenever we have a back arrow, we force reload of the home fragment by setting
            // mPreviousBottomNavId to Integer.MIN_VALUE
            mPreviousBottomNavId = Integer.MIN_VALUE;

            // We disable the possibility to open the drawer (e.g., by swiping from the left)
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            // Remove the hamburger icon
            mDrawerToggle.setDrawerIndicatorEnabled(false);

            // Show the back button
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // with mDrawerToggle.setDrawerIndicatorEnabled(false) we removed the hamburger icon,
            // but also disabled all clicks on that area. As we replace the hamburger icon with a
            // back arrow, we need to handle clicks. Therefore, we add a listener so that
            // mDrawerToggle can forward click events to this listener.
            if (!mToolBarBackButtonListenerIsRegistered) {
                mDrawerToggle.setToolbarNavigationClickListener(v -> {
                    Log.d(TAG, "onClick: Back button pressed");
                    onBackPressed();
                });
                mToolBarBackButtonListenerIsRegistered = true;
            }

        } else {
            // We enable the possibility to open the drawer.
            mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

            // We remove the back arrow button
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }

            // We show the hamburger icon
            mDrawerToggle.setDrawerIndicatorEnabled(true);

            // We remove the drawer toggle listener
            mDrawerToggle.setToolbarNavigationClickListener(null);
            mToolBarBackButtonListenerIsRegistered = false;
        }
    }

    /**
     * inner class broadcast receiver to handle communication between background services and ES main activity (in now mode)
     */
    @SuppressLint("ParcelCreator")
    public class SignalDataReceiver extends ResultReceiver {
        SignalDataReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Log.d(TAG, "in onReceiveResult(): " + resultCode);
            if (resultCode == MainActivity.RECEIVED_RAW_SIGNALS) {
                ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> rawSignals;
                rawSignals = (ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>>) resultData.getSerializable("signals");
                SignalsSlot signalsSlot = new SignalsSlot(rawSignals);

                // if the services take more than Const.MEASUREMENT_CYCLE_FOREGROUND to return measurements,
                // we must fill the chart with last measurement values up to the current time, so that
                // the chart does not become delayed in the past.
                //
                // drift represent the delay between the actual measurement time and the planned measurement
                // time. A drift of 1 means no delay, a drift of 2 means that the measurement is one
                // cycle late (so the same measurement must be displayed for 2 cycles), etc.

                final long currentTimestamp = Tools.getLiveCurrentTimestamp();
                final int lastIndex = mLiveTimeline.size() == 0 ? 0 : mLiveTimeline.size() - 1;
                final long lastTimestamp = mLiveTimeline.getTimeOrigin() + Const.LIVE_TIME_GAP * lastIndex;
                final int drift = (int) ((currentTimestamp - lastTimestamp) / Const.LIVE_TIME_GAP + 1);

                if (drift > 1) {
                    Log.w(TAG, "WARNING: time has drift, correcting the curve by " + drift + " cycles");
                }

                for (int i = 0; i < drift; i++) {
                    mLiveTimeline.add(signalsSlot);
                }

                mLiveTimeline.shrinkLeft(Const.CHART_MAX_SLOTS_LIVE);
                sendLiveTimelineUpdateToFragments(drift);

            } else {
                Log.w(TAG, "Received an unknown resultCode in the SignalDataReceiver: " + resultCode);
            }
        }
    }
}

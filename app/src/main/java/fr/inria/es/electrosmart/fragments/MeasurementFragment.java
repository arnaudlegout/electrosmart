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

// TODO (medium) implement notifications

// TODO(medium): explore why when BT and Wifi scans are made in parallel, we loose some wifi signals (see below)
// note that increasing the BT scan period seems to partially resolve the issue because there are
// fewer BT and wifi scans in parallel. The problem might be due also to an overlap in broadcast
// receivers.

// TODO (low) : implement low BT energy http://developer.android.com/guide/topics/connectivity/bluetooth-le.html
/*
Note: You can only scan for Bluetooth LE devices or scan for Classic Bluetooth devices, as described
in Bluetooth. You cannot scan for both Bluetooth LE and classic devices at the same time.
*/

// TODO (low) Check that the activity has no outside referenced static variables


package fr.inria.es.electrosmart.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.QuickJumpDialog;
import fr.inria.es.electrosmart.activities.SelectedMetricExplanationDialog;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.chart.Chart;
import fr.inria.es.electrosmart.database.DbHistoryAsyncTaskRequest;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;
import fr.inria.es.electrosmart.ui.CustomExpandableListAdapter;
import fr.inria.es.electrosmart.ui.HeaderExpandableListAdapter;
import fr.inria.es.electrosmart.ui.NumberTextViewAnimation;
import fr.inria.es.electrosmart.ui.ProgressBarAnimation;
import fr.inria.es.electrosmart.util.CachedTagGenerator;
import fr.inria.es.electrosmart.util.HistoryTagGenerator;
import fr.inria.es.electrosmart.util.LiveTagGenerator;
import fr.inria.es.electrosmart.util.TagGenerator;

public class MeasurementFragment extends MainActivityFragment {

    private static final String TAG = "MeasurementFragment";
    // when isGroupViewExpanded is true, a child view is displayed to expand a tile
    private static boolean isGroupViewExpanded;
    // Listener to attach to tabLayout in order to respond to tab selection events
    TabLayout.OnTabSelectedListener mOnTabSelectedListener;
    // mode of the application: LIVE, HISTORY
    private MeasurementMode mMeasurementMode;
    // give the number (starting at zero for the group view) of the first visible group view at the
    // top of the screen
    private int firstVisibleGroupView = 0;
    // The context that is used to get retrieve resources
    private Context mContext;
    // The parent activity
    private MainActivity mActivity;
    private AntennaDisplay lastExpandedGroupViewAntennaDisplay;
    // The tabs used to select the mode (live, history)
    private TabLayout tabLayout;
    // The view pager that is populated with the two modes (live, history) pages
    private ViewPager viewPager;
    // The progress bar that is used to represent the loading states
    private ProgressBar pb;
    // The view used for showing the errors pertaining to mandatory settings
    private View errorView;
    // singleton OnClickListeners that handle what to do when no location is of device is off
    private View.OnClickListener noLocationToDeviceOnClickListener;
    // singleton OnClickListeners that handle what to do when no location to the app is denied
    private View.OnClickListener noLocationToAppOnClickListener;
    // The list view that shows signal details
    private ExpandableListView list;
    private final BroadcastReceiver mWindowFocusChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Tools.setGroupIconIndicatorToRight(mActivity, list);
            passTheErrorViewCheck();
        }
    };
    // The text view that shows the no data message
    private TextView no_data_message;
    // we get a hook to the shared preferences, the initialization is made in onCreate
    private SharedPreferences settings;
    // Holds a reference on the chart
    private Chart mChart;
    // Holds a reference of the linear layout around the mSlotTagTextView.
    // It's used to set an on click listener to show quick jump dialog
    private ViewGroup mSlotTagWrapper;
    // The progress bar that represents the exposure level
    private ProgressBar mExposureSummaryProgressBar;
    // The AppBarLayout component of the activity that contains the exposure summary
    private View mHeader;
    // animation of the progress bar of the summary exposure
    private ProgressBarAnimation mExposureSummaryProgressBarAnimation;
    // animation of the value of the summary exposure
    private NumberTextViewAnimation mExposureSummaryValueTextViewAnimation;
    // metric of the value of the summary exposure
    private TextView mExposureSummaryMetricTextView;
    // The text view that show /100 for Your Exposure
    private TextView mExposureScaleTextView;
    // The tag generator that is used to generate text from timestamps to update
    // `mSlotTagTextView`
    private TagGenerator mTagGenerator;

    // The locale object to be used to format text
    private Locale mLocale;
    // Text view that shows the selected slot date
    private TextView mSlotTagTextView;

    // Holds a reference on either the history or live time line object that is retrieved from
    // the parent activity when the mode changes
    private Timeline mTimeline;


    // The broadcast receiver that should be executed whenever the live timeline is updated and
    // we are in live mode. It is its responsibility is to update the user interface accordingly.
    private final BroadcastReceiver mLiveTimelineUpdatedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mMeasurementMode == MeasurementMode.LIVE) {
                Log.d(TAG, "in mLiveTimelineUpdatedBroadcastReceiver.onReceive() ");
                int nbNewSlots = intent.getIntExtra(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATE_NB_SLOTS_EXTRA_KEY,
                        1);
                updateChartLive(mTimeline, nbNewSlots);
                if ((!mTimeline.isEmpty() && mTimeline.get(mTimeline.size() - 1).containsValidSignals())
                        || Tools.isAirplaneModeActivated(mContext)) {
                    hideProgressBar();
                }
            }
        }
    };

    public static String AntennaDisplayToString(Context context, AntennaDisplay antenna) {
        if (antenna == AntennaDisplay.WIFI) {
            return context.getResources().getString(R.string.wifi);
        } else if (antenna == AntennaDisplay.BLUETOOTH) {
            return context.getResources().getString(R.string.bt);
        } else if (antenna == AntennaDisplay.CELLULAR) {
            return context.getResources().getString(R.string.cellular);
        } else {
            return "";
        }
    }

    /**
     * Creates a new instance of the fragment for which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static MainActivityFragment newInstance(Bundle arguments) {
        MainActivityFragment fragment = new MeasurementFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * set the selected mode such as LIVE or HISTORY which is defined as ENUM
     *
     * @param mode ENUM mode to set, that we need to provide at the start of the mode
     */
    private void setMeasurementFragmentMode(MeasurementMode mode) {
        mMeasurementMode = mode;
        passTheErrorViewCheck();

        updateTopPanel(null);
        setChartMode(mode);
        resetTopPanel();

        if (settings == null) {
            settings = mContext.getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        }

        if (mode == MeasurementMode.HISTORY) {

            // listener for the quick jump dialog that will trigger the jump in the chart when
            // we select a jump date
            final QuickJumpDialog.OnResultListener listener = result -> {

                final long selectedTimestamp = result.getSelectedTimestamp();

                try {
                    // If the selected timestamp is already loaded
                    // we just scroll to it.
                    mChart.scrollTo(mTimeline.indexOfTimestamp(selectedTimestamp));
                } catch (Timeline.TimestampOutOfBoundsException exception) {
                    // Otherwise we clear the loaded data and we load a new page centred
                    // around the selected timestamp.
                    loadHistory(selectedTimestamp);
                }
            };

            // create the tag (i.e., the date component of the chart)
            final TagGenerator generator = new CachedTagGenerator(
                    new HistoryTagGenerator(mContext, mLocale, 0)
            );

            // listener for the chart that trigger the pop-up of the quick jump dialog with we touch
            // the chart
            final Chart.OnClickListener onClickListener = () -> {
                if (DbHistoryAsyncTaskRequest.backgroundTask != null &&
                        DbHistoryAsyncTaskRequest.backgroundTask.isRunning()) {
                    // If a data request is already running we don't show
                    // the quick jump dialog
                    return;
                }
                mChart.stopScroll();
                final long oldestTimestamp = Tools.oldestTimestampInDb();
                final long latestTimestamp = Tools.getHistoryCurrentTimestamp();
                // if there is a single displayed slot, we don't show the quick jump dialog
                if ((latestTimestamp - oldestTimestamp) / Const.HISTORY_TIME_GAP < 1) {
                    return;
                }
                long initTimestamp;
                if (mChart.size() == 0) {
                    initTimestamp = mTimeline.getTimeOrigin();
                } else {
                    initTimestamp = mTimeline.getTimestamp(mChart.position());
                }
                generator.setCurrent(Tools.getHistoryCurrentTimestamp());
                QuickJumpDialog datetimeDialog =
                        new QuickJumpDialog(mContext,
                                initTimestamp,
                                Tools.oldestTimestampInDb(),
                                Tools.getHistoryCurrentTimestamp(), listener,
                                generator, Const.HISTORY_TIME_GAP, false
                        );
                datetimeDialog.show();
            };

            mChart.setOnClickListener(onClickListener);

            // we can also touch the tag to open the quick jump dialog
            mSlotTagWrapper.setOnClickListener(v -> onClickListener.onClick());

        } else {
            DbHistoryAsyncTaskRequest.cancelCurrentAsyncTask();
            hideProgressBar();

            // if we reach here, we are in LIVE mode
            if (settings.getBoolean(Const.IS_AGREEMENT_FLOW_DONE, false) && mTimeline.isEmpty()) {
                Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is true, we show the progress bar until " +
                        "we receive new signals");
                showProgressBar();
            } else {
                Log.d(TAG, "IS_AGREEMENT_FLOW_DONE is false, we DO NOT show the progree bar");
            }

            // listener for the quick jump dialog that will trigger the jump in the chart when
            // we select a jump date
            final QuickJumpDialog.OnResultListener listener = result -> {
                if (result.getSelectedAction() == QuickJumpDialog.Result.ACTION_JUMP_TO_END) {
                    mChart.scrollTo(mChart.slotCount() - 1);
                } else if (result.getSelectedTimestamp() < mTimeline.getTimeOrigin()) {
                    // The timestamp selected in the quick jump dialog may be removed by
                    // the chart shrinking. In this case we only position the chart
                    // at its first slot.
                    mChart.scrollTo(0);
                } else {
                    mChart.scrollTo(mTimeline.indexOfTimestamp(result.getSelectedTimestamp()));
                }
            };

            // create the tag (i.e., the date component of the chart)
            final TagGenerator generator = new CachedTagGenerator(
                    new LiveTagGenerator(mContext, mLocale, 0)
            );

            // listener for the chart that trigger the pop-up of the quick jump dialog with we touch
            // the chart
            final Chart.OnClickListener onClickListener = () -> {
                mChart.stopScroll();
                if (mChart.slotCount() <= 1) {
                    return;
                }
                generator.setCurrent(mTimeline.getTimestamp(mChart.size() - 1));
                QuickJumpDialog datetimeDialog =
                        new QuickJumpDialog(mContext,
                                mTimeline.getTimestamp(mChart.position()),
                                mTimeline.getTimeOrigin(),
                                mTimeline.getTimestamp(mChart.size() - 1), listener,
                                generator, Const.LIVE_TIME_GAP, true
                        );
                datetimeDialog.show();
            };

            mChart.setOnClickListener(onClickListener);

            // we can also touch the tag to open the quick jump dialog
            mSlotTagWrapper.setOnClickListener(v -> onClickListener.onClick());
        }
    }

    /**
     * Loads at most one page (see {@link Const#MAX_SLOTS_PER_PAGE}) of data for history mode
     * as an initialization. It is used when the user switched from the live to history mode.
     * Note that before calling this method the activity should already be configured for history
     * mode. That is the foreground services should be stopped and the chart configured for
     * one hour timestamp gaps.
     */
    private void initializeHistory() {
        // Loading the first history mode page
        final long oldestTimestamp = Tools.oldestTimestampInDb();
        final long latestTimestamp = Tools.getHistoryCurrentTimestamp();

        // Calculate the number of slots to load
        int nbSlots = Math.min((int) (
                        (latestTimestamp - oldestTimestamp) / Const.HISTORY_TIME_GAP) + 1,
                Const.MAX_SLOTS_PER_PAGE);

        // Load at least one slot even if the database is not one hour old
        if (nbSlots <= 0) {
            nbSlots = 1;
        }

        final List<Pair<Date, Date>> timeSlots = DbHistoryAsyncTaskRequest.timeSlots(
                latestTimestamp * 1000, nbSlots, Const.HISTORY_TIME_GAP, true);

        DbHistoryAsyncTaskRequest.callBackground(timeSlots, new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
            @Override
            public void onPreExecute() {
                showProgressBar();
            }

            @Override
            public void onPostExecute(List<SignalsSlot> result) {
                hideProgressBar();
                mTimeline.addAll(0, result);
                updateChartHistory(mTimeline, mTimeline.size());
                mTimeline.setTimeOrigin(timeSlots.get(0).first.getTime() / 1000);
                mChart.scrollTo(mChart.slotCount() - 1);
            }
        });
    }

    /**
     * Load at most one page of history data centred around the given timestamp. Once the data is
     * loaded the chart is scrolled to position itself at the timestamp. Like
     * {@link #initializeHistory()} the activity should be already configured for history mode.
     *
     * @param timestamp the timestamp around which the page is loaded.
     */
    private void loadHistory(final long timestamp) {

        resetChartHistory();
        updateTopPanel(null);

        // The time interval available in the database
        final long availableLow = Tools.oldestTimestampInDb();
        final long availableHigh = Tools.getHistoryCurrentTimestamp();

        // The 24 hour around `timestamp`
        long targetLow = timestamp -
                Const.HISTORY_TIME_GAP * Const.MAX_SLOTS_PER_PAGE / 2;
        long targetHigh = timestamp +
                Const.HISTORY_TIME_GAP * Const.MAX_SLOTS_PER_PAGE / 2;

        // Calculate the intersection
        long interLow = Math.max(availableLow, targetLow);
        long interHigh = Math.min(availableHigh, targetHigh);

        final List<Pair<Date, Date>> timeSlots;
        if (interLow >= interHigh) {
            // If the intersection is empty
            timeSlots = DbHistoryAsyncTaskRequest.timeSlots(
                    timestamp * 1000, 1, Const.HISTORY_TIME_GAP, false);

            DbHistoryAsyncTaskRequest.callBackground(timeSlots, new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
                @Override
                public void onPreExecute() {
                    showProgressBar();
                }

                @Override
                public void onPostExecute(List<SignalsSlot> result) {
                    hideProgressBar();
                    mTimeline.addAll(0, result);
                    mTimeline.setTimeOrigin(timeSlots.get(0).first.getTime() / 1000);
                    updateChartHistory(mTimeline, mTimeline.size());
                    mChart.scrollTo(mTimeline.indexOfTimestamp(timestamp));
                }
            });
        } else if (interLow <= timestamp && timestamp <= interHigh) {
            // If the intersection is not empty and `timestamp` belongs to the intersection
            // (The normal case)
            int nbSlots = (int) ((interHigh - interLow) / Const.HISTORY_TIME_GAP) + 1;

            timeSlots = DbHistoryAsyncTaskRequest.timeSlots(
                    interLow * 1000, nbSlots, Const.HISTORY_TIME_GAP, false);

            DbHistoryAsyncTaskRequest.callBackground(timeSlots, new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
                @Override
                public void onPreExecute() {
                    showProgressBar();
                }

                @Override
                public void onPostExecute(List<SignalsSlot> result) {
                    hideProgressBar();
                    mTimeline.addAll(0, result);
                    mTimeline.setTimeOrigin(timeSlots.get(0).first.getTime() / 1000);
                    updateChartHistory(mTimeline, mTimeline.size());
                    mChart.scrollTo(mTimeline.indexOfTimestamp(timestamp));
                }
            });
        } else if (timestamp < interLow) {
            // If `timestamp` is less than the lower bound of the intersection
            // (At the left of the intersection)
            int nbSlots = (int) ((timestamp - interLow) / Const.HISTORY_TIME_GAP) + 1;

            timeSlots = DbHistoryAsyncTaskRequest.timeSlots(
                    timestamp * 1000, nbSlots, Const.HISTORY_TIME_GAP, false);

            DbHistoryAsyncTaskRequest.callBackground(timeSlots, new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
                @Override
                public void onPreExecute() {
                    showProgressBar();
                }

                @Override
                public void onPostExecute(List<SignalsSlot> result) {
                    hideProgressBar();
                    mTimeline.addAll(0, result);
                    mTimeline.setTimeOrigin(timeSlots.get(0).first.getTime() / 1000);
                    updateChartHistory(mTimeline, mTimeline.size());
                    mChart.scrollTo(mTimeline.indexOfTimestamp(timestamp));
                }
            });
        } else if (timestamp > interHigh) {
            // If `timestamp` is greater than the upper bound of the intersection
            // (At the right of the intersection). Which is the only remaining case
            // if all the previous ones are false.
            timeSlots = DbHistoryAsyncTaskRequest.timeSlots(
                    timestamp * 1000, 1, Const.HISTORY_TIME_GAP, false);
            DbHistoryAsyncTaskRequest.callBackground(timeSlots, new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
                @Override
                public void onPreExecute() {
                    showProgressBar();
                }

                @Override
                public void onPostExecute(List<SignalsSlot> result) {
                    hideProgressBar();
                    mTimeline.addAll(0, result);
                    mTimeline.setTimeOrigin(timeSlots.get(0).first.getTime() / 1000);
                    updateChartHistory(mTimeline, mTimeline.size());
                    mChart.scrollTo(mTimeline.indexOfTimestamp(timestamp));
                }
            });
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measurement, container, false);

        // Retrieve the `Locale` object passed by the calling activity, otherwise
        // fallback to english
        Serializable locale = getArguments().getSerializable(Const.MAIN_ACTIVITY_LOCALE_ARG_KEY);
        if (locale instanceof Locale) {
            mLocale = (Locale) locale;
        } else {
            mLocale = Locale.ENGLISH;
        }

        settings = mContext.getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);

        ESPagerAdapter pagerAdapter = new ESPagerAdapter();
        viewPager = view.findViewById(R.id.viewpager_tab);

        // We obtained viewPager after setContentView, and hence we assume that the findViewById will
        // always return a non null value as, R.id.viewpager always exists in app_main_layout, and the
        // id has not been used elsewhere.
        viewPager.setAdapter(pagerAdapter);
        viewPager.beginFakeDrag();

        viewPager.setOffscreenPageLimit(3);

        // Create an initial view to display.
        final ViewGroup measurementLayout = view.findViewById(R.id.layout_measurement);

        // Create views for the three tabs and add them to the layout
        LinearLayout dummyLayout = (LinearLayout) inflater.inflate(R.layout.dummy_tab_layout,
                measurementLayout, false);
        pagerAdapter.addView(dummyLayout, mContext.getString(R.string.live), 0);
        pagerAdapter.addView(dummyLayout, "", 1);
        pagerAdapter.addView(dummyLayout, mContext.getString(R.string.history), 2);
        pagerAdapter.notifyDataSetChanged();

        tabLayout = mActivity.findViewById(R.id.tabs);

        // We obtained tabLayout after setContentView, and hence we assume that the findViewById will
        // always return a non null value, as R.id.tabs always exists in main_content, and the
        // id has not been used elsewhere.
        tabLayout.setupWithViewPager(viewPager);

        // Disable the center tab (it is used as a placeholder)
        ((ViewGroup) tabLayout.getChildAt(0)).getChildAt(1).setEnabled(false);

        tabLayout.setTabTextColors(
                ContextCompat.getColor(mContext, R.color.secondary_text_color),
                ContextCompat.getColor(mContext, R.color.primary_text_color));

        /*
         * Connection with xml components such as buttons, layout etc
         */
        no_data_message = view.findViewById(R.id.txt_msg);
        errorView = view.findViewById(R.id.error_view);
        list = view.findViewById(R.id.list);
        pb = view.findViewById(R.id.progressBar);
        mChart = view.findViewById(R.id.chart);

        mSlotTagWrapper = view.findViewById(R.id.chart_slot_tag_wrapper);

        // Set curves color gradient to the chart
        mChart.setCurvesColorGradient(
                ContextCompat.getColor(mContext, Tools.themedColor(Tools.COLOR_LOW)),
                ContextCompat.getColor(mContext, Tools.themedColor(Tools.COLOR_MEDIUM)),
                ContextCompat.getColor(mContext, Tools.themedColor(Tools.COLOR_HIGH)),
                new float[]{
                        ((float) Tools.dbmToPercentage(Const.LOW_DBM_THRESHOLD)) / 100f,
                        ((float) Tools.dbmToPercentage(Const.LOW_MEDIUM_DBM_THRESHOLD)) / 100f,
                        ((float) Tools.dbmToPercentage(Const.MEDIUM_DBM_THRESHOLD)) / 100f,
                        ((float) Tools.dbmToPercentage(Const.MEDIUM_HIGH_DBM_THRESHOLD)) / 100f
                }
        );

        mSlotTagTextView = view.findViewById(R.id.chart_slot_tag_text_view);

        mExposureSummaryProgressBar = mActivity.findViewById(R.id.exposure_summary_progress_bar);
        mExposureSummaryProgressBarAnimation = new ProgressBarAnimation(
                mExposureSummaryProgressBar);
        mExposureSummaryProgressBarAnimation.setDuration(250);
        mExposureSummaryValueTextViewAnimation =
                new NumberTextViewAnimation(
                        (TextView) mActivity.findViewById(R.id.exposure_summary_value));
        mExposureSummaryValueTextViewAnimation.setDuration(250);
        mExposureSummaryMetricTextView = mActivity.findViewById(R.id.exposure_summary_metric);
        mExposureScaleTextView = mActivity.findViewById(R.id.exposure_summary_scale);
        mHeader = mActivity.findViewById(R.id.header);

        // By default when we start the UI, we begin with the default exposition color
        mHeader.setBackgroundColor(
                ContextCompat.getColor(mContext, R.color.regular_background_color)
        );

        // Make sure we readjust the icon indicators position if the screen changes it's size.
        // This can happen when we use the split-screen feature.
        measurementLayout.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        Tools.setGroupIconIndicatorToRight(mActivity, list));

        initializeListeners();

        setMeasurementFragmentMode(MeasurementMode.LIVE);

        return view;
    }

    /**
     * The onCreate method is the starting point of ElectroSmart and all the initial settings are
     * handled in it.
     *
     * @param savedInstanceState The saved instance used by Android to restore previous state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "in onCreate()");
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach: ");
        mContext = context;
        mActivity = (MainActivity) getActivity();
    }

    /**
     * A function that initializes event listeners on the various user interface components.
     */
    private void initializeListeners() {
        /*
         * When location permission is denied to ElectroSmart, which is only the case in Android M
         * and higher devices, we show the rationale dialog to the user if needed, and then ask user
         * for location permission. If user flagged NEVER ASK AGAIN, we fall back to app settings
         * activity to enable the location permission to ElectroSmart.
         */
        noLocationToAppOnClickListener = v -> Tools.grantLocationToApp(mContext, mActivity, false);

        /*
         * When location permission of the device is turned off, we take the user to the location
         * settings screen so that the user can turn it on.
         */
        noLocationToDeviceOnClickListener = v -> {
            Intent viewIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(viewIntent);
        };

        /*
         *   called when the group views are scrolled, the sequence of event is
         *   start scrolling -> call setOnScrollListener -> call onScroll (as long as the scroll event
         *   continues)
         *   stop scrolling -> call setOnScrollListener
         */
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            // called when we start and stop scrolling
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            // called when we scroll
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (visibleItemCount > 0) {
                    firstVisibleGroupView = firstVisibleItem;
                    Log.d(TAG, "onScroll: firstVisibleGroupView: " + firstVisibleGroupView);
                }
            }
        });

        /*
          called each time a group view is touched. It manages the updates of the group views.
         */
        list.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            Log.d(TAG, "onCreate: setOnGroupClickListener");

            // we store in the group_view_tile the AntennaDisplay type for this group view
            AntennaDisplay type = (AntennaDisplay) v.findViewById(R.id.group_view_tile).getTag();
            if (isGroupViewExpanded) {
                isGroupViewExpanded = false;
                lastExpandedGroupViewAntennaDisplay = null;
            } else {
                isGroupViewExpanded = true;
                lastExpandedGroupViewAntennaDisplay = type;
            }
            // as we are overriding the default click behaviour of the group, it is our
            // responsibility to play sound for haptic feedback
            v.playSoundEffect(SoundEffectConstants.CLICK);
            updateTopPanelFromChartInThePast(mTimeline, mChart.position());
            return true;
        });

        mChart.setOnPositionChangedListener((oldPosition, newPosition) ->
                mSlotTagTextView.setText(mTagGenerator.get(mTimeline.getTimestamp(newPosition))));


        mChart.setOnScrollEndListener((position, fromUser) -> {

            if (mChart.size() == 0) {
                // If the chart holds no data we do nothing
                return;
            }

            updateTopPanelFromChartInThePast(mTimeline, position);

            if (mMeasurementMode == MeasurementMode.HISTORY) {

                if (mChart.slotCount() == 1) {
                    // This is a spacial case where the chart
                    // is positioned at the start and at the end
                    // at the same time.
                    return;
                }

                if (DbHistoryAsyncTaskRequest.backgroundTask != null &&
                        DbHistoryAsyncTaskRequest.backgroundTask.isRunning()) {
                    // We don't load any new data if we are already loading
                    return;
                }

                final long oldestTimestamp = Tools.oldestTimestampInDb();
                final long latestTimestamp = Tools.getHistoryCurrentTimestamp();
                final long nbSlotsInDB = (latestTimestamp - oldestTimestamp)
                        / Const.HISTORY_TIME_GAP;

                // Do nothing if the database is smaller than the loaded data
                if (mChart.slotCount() >= nbSlotsInDB) {
                    return;
                }

                // Load data before if the chart is scrolled to its start
                if (mChart.position() == 0) {

                    // Do nothing we reached the first date in the database
                    if (mTimeline.getTimeOrigin() <= oldestTimestamp) {
                        return;
                    }

                    int nbSlots = Math.min((int) ((mTimeline.getTimeOrigin() - oldestTimestamp)
                                    / Const.HISTORY_TIME_GAP),
                            Const.MAX_SLOTS_PER_PAGE);

                    final List<Pair<Date, Date>> timeSlots =
                            DbHistoryAsyncTaskRequest.timeSlots((mTimeline.getTimeOrigin()
                                            - Const.HISTORY_TIME_GAP) * 1000,
                                    nbSlots, Const.HISTORY_TIME_GAP, true);

                    DbHistoryAsyncTaskRequest.callBackground(timeSlots, new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
                        @Override
                        public void onPreExecute() {
                            showProgressBar();
                        }

                        @Override
                        public void onPostExecute(List<SignalsSlot> result) {
                            hideProgressBar();

                            mTimeline.addAll(0, result);
                            long oldTimestamp = mTimeline.getTimestamp(mChart.position());
                            // We cannot add values to left of the chart. Therefore we
                            // clear it and reassign the values from the beginning
                            mChart.reset();
                            mTimeline.setTimeOrigin(timeSlots.get(0).first.getTime() / 1000);

                            mTimeline.shrinkRight(Const.CHART_MAX_SLOTS_HISTORY);

                            updateChartHistory(mTimeline, mTimeline.size());

                            if (oldTimestamp < mTimeline.getTimestamp(mChart.slotCount() - 1)) {
                                mChart.scrollTo(mTimeline.indexOfTimestamp(oldTimestamp));
                            } else {
                                mChart.scrollTo(mChart.slotCount() - 1);
                            }
                        }
                    });
                }

                // Load data before if the chart is scrolled to its end
                if (mChart.position() == mChart.slotCount() - 1) {

                    // Do nothing we reached the last date in the database
                    if (mTimeline.getTimestamp(mChart.slotCount() - 1) +
                            Const.HISTORY_TIME_GAP >= latestTimestamp) {
                        return;
                    }

                    final int nbSlots = Math.min((int) ((latestTimestamp -
                                    mTimeline.getTimestamp(mChart.slotCount() - 1))
                                    / Const.HISTORY_TIME_GAP),
                            Const.MAX_SLOTS_PER_PAGE);

                    final List<Pair<Date, Date>> timeSlots =
                            DbHistoryAsyncTaskRequest.timeSlots((
                                            mTimeline.getTimestamp(mChart.slotCount() - 1)
                                                    + Const.HISTORY_TIME_GAP) * 1000,
                                    nbSlots, Const.HISTORY_TIME_GAP, false);

                    DbHistoryAsyncTaskRequest.callBackground(timeSlots, new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
                        @Override
                        public void onPreExecute() {
                            showProgressBar();
                        }

                        @Override
                        public void onPostExecute(List<SignalsSlot> result) {
                            hideProgressBar();

                            mTimeline.addAll(mTimeline.size(), result);
                            long oldTimestamp = mTimeline.getTimestamp(mChart.position());
                            updateChartHistory(mTimeline, result.size());

                            if (mChart.slotCount() > Const.CHART_MAX_SLOTS_HISTORY) {
                                mChart.shrinkLeft(Const.CHART_MAX_SLOTS_HISTORY);
                                mTimeline.shrinkLeft(Const.CHART_MAX_SLOTS_HISTORY);
                            }

                            if (oldTimestamp > mTimeline.getTimeOrigin()) {
                                mChart.scrollTo(mTimeline.indexOfTimestamp(oldTimestamp));
                            } else {
                                mChart.scrollTo(0);
                            }
                        }
                    });
                }
            }
        });

        // Change the contents of the layout on selection of different tabs (Live and History)
        mOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                viewPager.setCurrentItem(position);
                if (position == 0) {
                    setMeasurementFragmentMode(MeasurementMode.LIVE);
                } else if (position == 2) {
                    setMeasurementFragmentMode(MeasurementMode.HISTORY);
                    resetChartHistory();
                    initializeHistory();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };
        tabLayout.clearOnTabSelectedListeners();
        tabLayout.addOnTabSelectedListener(mOnTabSelectedListener);

        mExposureSummaryProgressBar.setOnClickListener(
                view -> new SelectedMetricExplanationDialog(mContext).show()
        );
    }

    /**
     * This method displays the error group view if it is needed
     * <p>
     * A function that checks whether the mandatory and optional conditions for the proper functioning
     * of the app are met or not. If they are not, the error_view is made visible there by alerting
     * the user to check the permissions in the settings activity.
     */
    private void passTheErrorViewCheck() {
        //check for optimality options
        if (Tools.isAirplaneModeActivated(mContext) &&
                mMeasurementMode == MeasurementMode.LIVE) {
            TextView title_text_view = errorView.findViewById(R.id.title_text_view);
            TextView explanation_text_view = errorView.findViewById(R.id.explanation_text_view);
            title_text_view.setText(R.string.error_airplane_mode);
            explanation_text_view.setText(R.string.error_airplane_mode_explanation);
            errorView.setVisibility(View.VISIBLE);
            errorView.setOnClickListener(null);
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Tools.isAccessFineLocationGranted(mContext) &&
                mMeasurementMode == MeasurementMode.LIVE) {
            TextView title_text_view = errorView.findViewById(R.id.title_text_view);
            TextView explanation_text_view = errorView.findViewById(R.id.explanation_text_view);
            title_text_view.setText(R.string.error_no_location_to_application);
            explanation_text_view.setText(R.string.error_no_location_to_application_explanation);
            errorView.setVisibility(View.VISIBLE);
            errorView.setOnClickListener(noLocationToAppOnClickListener);
        } else if (Tools.isLocationDisabled(mContext) && mMeasurementMode == MeasurementMode.LIVE) {
            TextView title_text_view = errorView.findViewById(R.id.title_text_view);
            TextView explanation_text_view = errorView.findViewById(R.id.explanation_text_view);
            title_text_view.setText(R.string.error_no_device_location);
            explanation_text_view.setText(R.string.error_no_device_location_explanation);
            errorView.setVisibility(View.VISIBLE);
            errorView.setOnClickListener(noLocationToDeviceOnClickListener);
        } else {
            errorView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, "in onResume()");
        super.onResume();
        // if we are coming from Settings Activity where a user may have turned off the
        // show instrument setting, we should take the user to HomeFragment, as we hide the
        // instrument item in the menu
        if (!SettingsPreferenceFragment.get_PREF_KEY_SHOW_INSTRUMENT()) {
            mActivity.showHomeFragment();

            // if the user turned off the show instrument, we must exit onResume here
            return;
        }

        DbRequestHandler.dumpEventToDatabase(Const.EVENT_MEASUREMENT_FRAGMENT_ON_RESUME);

        // in case the wardrive mode is enabled, we keep the screen always on.
        if (SettingsPreferenceFragment.get_WARDRIVE_MODE()) {
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Log.d(TAG, "onResume: wardrive mode enabled");
        }

        passTheErrorViewCheck();

        // we collapse the group views when onResume()
        resetTopPanel();

        if (mMeasurementMode == MeasurementMode.LIVE) {
            mChart.reset();
            /*
            When we resume, we put the chart to the right most value. This is the simplest
            strategy. It is implemented in updateChartLive. We could imagine a more sophisticated
            strategy in which we keep the position of the chart before the onPause and center the
            new chart on that position in the onResume. But it is slightly more complex because
            we need to handle the case when the past position does not exist anymore in the
            chart (because the chart has been shrunk). We decided to go for the simplest
            strategy, but it can easily be changed later if needed in that place.
            */
            updateChartLive(mTimeline, mTimeline.size());
        }

        updateTopPanelFromChartInThePast(mTimeline, mChart.position());

        // Tell main activity what to do in case the window focus changed.

        // This listener is called when the window focus on the app changes. The old code contained
        // the logic to move the group icon indicator to the right, here.
        // This has been refactored to the function setGroupIconIndicatorToRight(), which is called
        // on selection of new tab.
        //
        // See http://stackoverflow.com/questions/5800426/expandable-list-view-move-group-icon-indicator-to-right
        //
        // Also, the app needs to check if the conditions for showing the error view are
        // met. Hence the passTheErrorViewCheck()
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(mLiveTimelineUpdatedBroadcastReceiver,
                new IntentFilter(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION));
        localBroadcastManager.registerReceiver(mWindowFocusChangedBroadcastReceiver,
                new IntentFilter(Const.MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        Log.d(TAG, "in onPause()");
        super.onPause();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_MEASUREMENT_FRAGMENT_ON_PAUSE);

        // onPause we always release the flag FLAG_KEEP_SCREEN_ON
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.unregisterReceiver(mLiveTimelineUpdatedBroadcastReceiver);
        localBroadcastManager.unregisterReceiver(mWindowFocusChangedBroadcastReceiver);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "in onDestroy()");
        super.onDestroy();
        DbHistoryAsyncTaskRequest.cancelCurrentAsyncTask();
        hideProgressBar();
    }

    /**
     * Take the user back to HomeFragment when he/she presses the back button
     *
     * @return true because we've handled the back pressed
     */
    @Override
    public boolean onBackPressed() {
        mActivity.onBottomNavigateTo(R.id.bottom_nav_home);
        return true;
    }

    /**
     * Adapts the chart to the measurement mode mode.
     *
     * @param mode the mode to switch to
     */
    private void setChartMode(MeasurementMode mode) {
        mChart.reset();
        if (mode == MeasurementMode.LIVE) {
            mTimeline = mActivity.mLiveTimeline;
            mTagGenerator = new CachedTagGenerator(new LiveTagGenerator(mContext,
                    mLocale, mTimeline.getTimeOrigin()));
            updateChartLive(mTimeline, mTimeline.size());
            if (mChart.size() != 0) {
                mChart.scrollTo(mChart.slotCount() - 1);
            }
        } else {
            long initTimestamp = Tools.getHistoryCurrentTimestamp();
            // The history timeline is not kept when we switch to live mode.
            // Hence, we create a new one every time.
            mTimeline = new Timeline(Const.CHART_MAX_SLOTS_HISTORY, Const.HISTORY_TIME_GAP);
            mTimeline.setTimeOrigin(initTimestamp);
            mTagGenerator = new CachedTagGenerator(new HistoryTagGenerator(mContext,
                    mLocale, initTimestamp));
            mSlotTagTextView.setText(mTagGenerator.get(initTimestamp));
        }
    }

    /**
     * Resets the chart for history mode.
     */
    private void resetChartHistory() {
        mChart.reset();
        long initTimestamp = Tools.getHistoryCurrentTimestamp();
        // The history timeline is not kept when we switch to live mode.
        // Hence, we create a new one every time.
        mTimeline = new Timeline(Const.CHART_MAX_SLOTS_HISTORY, Const.HISTORY_TIME_GAP);
        mTimeline.setTimeOrigin(initTimestamp);
        mTagGenerator = new CachedTagGenerator(new HistoryTagGenerator(mContext,
                mLocale, initTimestamp));
        mSlotTagTextView.setText(mTagGenerator.get(initTimestamp));
    }

    /**
     * updates the group view when moving the selector bar on the chartview
     *
     * @param timeline          represents the timeline containing the SignalsSlot
     * @param selectedDateIndex index of the selected date in the chart, which is the same selected
     *                          date in the data structures
     */
    private void updateTopPanelFromChartInThePast(List<SignalsSlot> timeline, int selectedDateIndex) {
        Log.d(TAG, "in updateTopPanelFromChartInThePast()");

        if ((0 <= selectedDateIndex) && (selectedDateIndex < timeline.size())) {
            updateTopPanel(timeline.get(selectedDateIndex));
        }
    }

    /**
     * Update the group view according to the passed signalsSlot. If signalsSlot is
     * null, then the top panel is cleared and "no data" is displayed
     *
     * @param signalsSlot represents the SignalsSlot object used for updating the top panel
     */
    private void updateTopPanel(SignalsSlot signalsSlot) {
        CustomExpandableListAdapter listAdapter;
        Log.d(TAG, "in updateTopPanel()");

        if (signalsSlot != null && signalsSlot.containsValidSignals()) {
            no_data_message.setVisibility(View.INVISIBLE);

            if (isGroupViewExpanded) {
                listAdapter = new HeaderExpandableListAdapter(mContext,
                        lastExpandedGroupViewAntennaDisplay, signalsSlot, false, 0);
            } else {
                listAdapter = new HeaderExpandableListAdapter(mContext,
                        null, signalsSlot, false, 0);
            }
            pushUpdateTopPanel(signalsSlot, listAdapter);

        } else {
            // if topPanelEsProperties is null or empty we remove all group views and display a
            // "no data" message
            listAdapter = new HeaderExpandableListAdapter(mContext, null,
                    null, false, 0);
            list.setAdapter(listAdapter);
            listAdapter.notifyDataSetChanged();
            if (Tools.isAccessFineLocationGranted(mContext) || mMeasurementMode == MeasurementMode.HISTORY) {
                no_data_message.setVisibility(View.VISIBLE);
            } else {
                no_data_message.setVisibility(View.INVISIBLE);
            }

            pushUpdateTopPanel(null, listAdapter);
        }
    }

    /**
     * Update the title and group views
     *
     * @param signalsSlot represents the SignalsSlot object used for updating the top panel
     * @param listAdapter the expandable list adapter that will update the listview
     */
    private void pushUpdateTopPanel(SignalsSlot signalsSlot, CustomExpandableListAdapter listAdapter) {
        //we update the title status and title bar.
        updateTitleAndStatusBars(signalsSlot);

        // we update the group views
        list.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
    }

    /**
     * Update the title bar with the passed background_color and exposure_text_id
     *
     * @param dbm             The dbm value of the max signal
     * @param isValidExposure if set to false, displays a n/a exposure (does not take the dbm),
     *                        displays the real exposure otherwise
     */
    private void updateTitleBar(int dbm, boolean isValidExposure) {

        if (!isValidExposure) {
            mExposureSummaryProgressBarAnimation.setProgress(0);
            mExposureSummaryValueTextViewAnimation.setNA();
            mExposureSummaryMetricTextView.setText(R.string.na_metric);
            mExposureScaleTextView.setVisibility(View.GONE);
        } else {
            String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();

            if (pref.equals(mContext.getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
                mExposureScaleTextView.setVisibility(View.VISIBLE);
                mExposureSummaryValueTextViewAnimation.setValue(Tools.getExposureScore(dbm));
                mExposureSummaryMetricTextView.setText(R.string.your_exposure);
            } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_DBM_METRIC))) {
                mExposureScaleTextView.setVisibility(View.GONE);
                mExposureSummaryValueTextViewAnimation.setValue(dbm);
                mExposureSummaryMetricTextView.setText(R.string.dbm_metric);
            } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_POWER_METRIC))) {
                mExposureScaleTextView.setVisibility(View.GONE);
                // The RawSignalHandler normalize all signals before passing them to the UI.
                // We have the guarantee that the dbm value can be converted in watt.
                String[] watt = Tools.dBmToWattWithSIPrefixArray(dbm, false);
                mExposureSummaryValueTextViewAnimation.setValue(Integer.parseInt(watt[0]));
                mExposureSummaryMetricTextView.setText(watt[1]);
            }
            mExposureSummaryProgressBarAnimation.setProgress(Tools.getExposureScore(dbm));
        }
    }

    private void updateTitleAndStatusBars(SignalsSlot signalsSlot) {
        Log.d(TAG, "in updateTitleAndStatusBars()");

        BaseProperty topSignal = null;
        if (signalsSlot != null && signalsSlot.containsValidSignals()) {
            topSignal = signalsSlot.getTopSignal();
        }

        if (topSignal != null) {
            Log.d(TAG, "updateTitleAndStatusBars: topPanelEsProperties" + topSignal);
            int cumulativeDbmValue = signalsSlot.getSlotCumulativeTotalDbmValue();
            Tools.SignalLevel level = Tools.dbmToLevel(cumulativeDbmValue);
            if (level == Tools.SignalLevel.LOW) {
                int titleBarColor = Tools.themedColor(Tools.COLOR_LOW);
                updateTitleBar(cumulativeDbmValue, true);
                themeProgressBar(titleBarColor);
            } else if (level == Tools.SignalLevel.MODERATE) {
                int titleBarColor = Tools.themedColor(Tools.COLOR_MEDIUM);
                updateTitleBar(cumulativeDbmValue, true);
                themeProgressBar(titleBarColor);
            } else if (level == Tools.SignalLevel.HIGH) {
                int titleBarColor = Tools.themedColor(Tools.COLOR_HIGH);
                updateTitleBar(cumulativeDbmValue, true);
                themeProgressBar(titleBarColor);
            }
        } else {
            Log.d(TAG, "updateTitleAndStatusBars: topPanelEsPropeties = 0");
            updateTitleBar(0, false);
        }
    }

    /**
     * A utility function to set the theme of the progress bar instance of the measurement fragment
     *
     * @param color The color with which the progress bar is to be themed
     */
    private void themeProgressBar(int color) {
        LayerDrawable layerDrawable = (LayerDrawable) mExposureSummaryProgressBar.getProgressDrawable();
        // index 0 represents the background drawable and index 1 the progress
        Drawable progressDrawable = layerDrawable.getDrawable(1);
        progressDrawable.setColorFilter(ContextCompat.getColor(mContext, color), PorterDuff.Mode.SRC_IN);
    }

    /**
     * Updates the chart for live mode with the last {@code nbSlots} new values from {@code timeline}.
     * And handles the chart shrinking if it reached its maximum size.
     *
     * @param timeline The structure containing all SignalsSlot object
     * @param nbSlots  The number of slots to update
     */
    private void updateChartLive(List<SignalsSlot> timeline, int nbSlots) {
        boolean scrollToEnd = false;

        /*
        Check if we have to scroll to the end of the chart after we update.

        (mChart.slotCount() - 1 == mChart.position() && firstVisibleGroupView == 0) means we
        are at the last position in the chart and the first tile is visible

        mChart.size() == 0 means that the chart was empty (before the update)

        In both cases, when we update the chart, we must scroll to the end
        */
        if ((mChart.slotCount() - 1 == mChart.position() && firstVisibleGroupView == 0) ||
                mChart.size() == 0) {
            scrollToEnd = true;
        }

        updateChart(timeline, nbSlots);

        if (mChart.size() == 1) {
            // If there is only one slot in the chart update the ui and quit
            updateTopPanelFromChartInThePast(timeline, 0);
            return;
        }

        // Shrink the chart if it reached the maximum size allowed
        if (mChart.slotCount() > Const.CHART_MAX_SLOTS_LIVE) {
            mChart.shrinkLeft(Const.CHART_MAX_SLOTS_LIVE);
        }

        // We must set the TagGenerator before scrolling the chart, because the scroll action
        // triggers a callback that uses the current value of the TagGenerator
        mTagGenerator.setCurrent(Tools.getLiveCurrentTimestamp());

        // Scroll to end
        if (scrollToEnd && mChart.size() > 0) {
            mChart.scrollTo(mChart.size() - 1);
        }
    }

    /**
     * Update the chart for history mode with the last {@code nbSlots} new values from {@code timeline}.
     *
     * @param timeline The structure containing all SignalsSlot object
     * @param nbSlots  The number of slots to update
     */
    private void updateChartHistory(List<SignalsSlot> timeline, int nbSlots) {
        updateChart(timeline, nbSlots);
        mTagGenerator.setCurrent(Tools.getHistoryCurrentTimestamp());
    }

    /**
     * Update the chart with the last {@code nbSlots} new values from {@code timeline}.
     *
     * @param timeline The structure containing all SignalsSlot object
     * @param nbSlots  The number of slots to update
     */
    private void updateChart(List<SignalsSlot> timeline, int nbSlots) {
        // index where to start adding new slots to the chart
        int chartStart = mChart.size();

        // index of the last element of the chart when the new slots will be added
        int chartEnd = chartStart + nbSlots;

        /*
        index of the timeline where to start getting slots to add to the chart.
        The Chart and the timeline are supposed to be synchronized, so chartStart should be equal
        to timelineStart. However, when the timeline exceeds its maximum size, it is shrunk left.
        In that specific case, when we enter this method, the timeline and the chart are not
        synchronized. So timelineStart and chartStart are different. The chart will be in turn
        shrunk left in updateChartLive() after the call to this method.
        */
        int timelineStart = timeline.size() - nbSlots;

        for (int chartIndex = chartStart, timelineIndex = timelineStart; chartIndex < chartEnd;
             chartIndex++, timelineIndex++) {
            SignalsSlot signalsSlot = timeline.get(timelineIndex);
            Map<AntennaDisplay, BaseProperty> groupViewSignals = signalsSlot.getGroupViewSignals();
            if (groupViewSignals != null) {
                for (Map.Entry<AntennaDisplay, BaseProperty> entry : groupViewSignals.entrySet()) {
                    if (entry.getValue().isValidSignal) {
                        if (entry.getValue().isDbmValueInRange()) {
                            // The dbm value is correct, we convert it to a percentage
                            // and add it to the chart.
                            mChart.put(chartIndex, AntennaDisplayToString(mContext, entry.getKey()),
                                    Tools.dbmToPercentage(entry.getValue().dbm));
                        } else {
                            // The signal is valid, but the dbm value is not, which corresponds
                            // to an n/a in the signals details. In the chart we set a value
                            // of 0.
                            mChart.put(chartIndex, AntennaDisplayToString(mContext, entry.getKey()), 0);
                        }
                    } else {
                        // The signal is not valid (just a placeholder object). We just expand
                        // the size of the chart and we do not set any value (the index is kept
                        // empty).
                        mChart.put(chartIndex, AntennaDisplayToString(mContext, entry.getKey()), -1);
                    }
                }
            } else {
                // The signals slot is not valid (just a placeholder object). We just expand
                // the size of the chart and we do not set any value (the index is kept
                // empty). This is the case when we recoverFromPause.
                mChart.put(chartIndex, "", -1);
            }
        }
    }


    /**
     * This method collapse all group views on the top panel.
     */
    private void resetTopPanel() {
        Log.d(TAG, "in resetTopPanel()");
        isGroupViewExpanded = false;
        lastExpandedGroupViewAntennaDisplay = null;
        firstVisibleGroupView = 0;
    }

    private void showProgressBar() {
        Log.d(TAG, "in showProgressBar()");
        pb.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        Log.d(TAG, "in hideProgressBar()");
        pb.setVisibility(View.GONE);
    }

    // define the different types of antenna that can be displayed in the UI by ElectroSmart
    public enum AntennaDisplay {
        WIFI,
        BLUETOOTH,
        CELLULAR
    }

    public enum MeasurementMode {LIVE, HISTORY}

    /**
     * A custom PagerAdapter class to populate the pages (tabs) of the ViewPager of the application.
     * A part of the code of this class was taken from [http://stackoverflow.com/a/13671777]
     */
    public class ESPagerAdapter extends PagerAdapter {
        // This holds all the currently displayable views, in order from left to right.
        private List<View> views = new ArrayList<>();

        // This holds the titles for the views. Eg. Live, History
        private List<String> viewTitles = new ArrayList<>();

        // Used by ViewPager.  "Object" represents the page; tell the ViewPager where the
        // page should be displayed, from left-to-right.  If the page no longer exists,
        // return POSITION_NONE.
        @Override
        public int getItemPosition(@NonNull Object object) {
            View itemView = (View) object;
            int index = views.indexOf(itemView);
            if (index == -1)
                return POSITION_NONE;
            else
                return index;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return viewTitles.get(position) == null ? "View " + position : viewTitles.get(position);
        }

        // Used by ViewPager.  Called when ViewPager needs a page to display; it is our job
        // to add the page to the container, which is normally the ViewPager itself.  Since
        // all our pages are persistent, we simply retrieve it from our "views" ArrayList.
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View v = views.get(position);
            container.addView(v);
            return v;
        }

        // Used by ViewPager.  Called when ViewPager no longer needs a page to display; it
        // is our job to remove the page from the container, which is normally the
        // ViewPager itself.  Since all our pages are persistent, we do nothing to the
        // contents of our "views" ArrayList.
        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView(views.get(position));
        }

        // Used by ViewPager; can be used by app as well.
        // Returns the total number of pages that the ViewPage can display.  This must
        // never be 0.
        @Override
        public int getCount() {
            return views.size();
        }

        // Used by ViewPager.
        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        // Add "view" at "position" to "views".
        // Returns position of new view.
        // The app should call this to add pages; not used by ViewPager.
        int addView(View v, String title, int position) {
            views.add(position, v);
            viewTitles.add(position, title);
            return position;
        }

        // finishUpdate - called by the ViewPager - we don't care about what pages the
        // pager is displaying so we don't use this method.
    }
}

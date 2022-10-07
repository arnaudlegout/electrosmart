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

package fr.inria.es.electrosmart.fragments;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.transition.TransitionManager;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DailyStatSummary;
import fr.inria.es.electrosmart.database.DbHistoryAsyncTaskRequest;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.fragmentstates.StatisticsFragmentState;
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


/*

TEST CASES for the StatisticsFragment

# no stats, test the reload on the next day after onResume() from StatisticsFragment
1.1) start with no db
  a) open the app, go on statistics
      => message "come back tomorrow"
  b) stay on the *StatisticsFragment*, go onPause (Home button of Android)
     advance the date of the system by 24h
     start again the app (onResume)
     => the statistics load yesterday
  c) open the date picker
     => the selected date is yesterday (with respect to the new system date)

# no stats, test the reload on the next day after onResume() from HomeFragment
1.2) start with no db
  a) open the app, go on statistics
      => message "come back tomorrow"
  b) go on the *HomeFragment*, go onPause (Home button of Android)
     advance the date of the system by 24h
     start again the app (onResume)
     go on the StatisticsFragment
     => the statistics load yesterday
  c) open the date picker
     => the selected date is yesterday (with respect to the new system date)

# no stats, test the notification
1.3) start with no db
  a) open the app, go on statistics
      => message "come back tomorrow"
  b) go on the *HomeFragment*, go onPause (Home button of Android)
     advance the date of the system by 24 hours
     start again the app (onResume)
     go on the StatisticsFragment
     => the statistics load yesterday (with respect to the new system date)
  c) open the date picker
     => the selected date is yesterday (with respect to the new system date)

# stats, test the cache on yesterday, reset the system time to the default value
2.1) start with the test db
  a) open the app, go on statistics
     => statistics of yesterday load
  b) touch on a top 5 signal
     => the date is still yesterday
  c) go back to statistics
     => there is no reload, we still have yesterday statistics
  d) go to the HomeFragment, then go back to the StatisticsFragment
     => there is no reload, we still have yesterday statistics
  e) touch on a top 5 signal
     => the date is still yesterday
  f) go back to statistics
     => there is no reload, we still have yesterday statistics

# stats, test the cache on another date (not yesterday)
3.1) start with the test db
  a) open the app, go on statistics
     => statistics of yesterday load
  b) go to the data picker and select two days before (D2)
     => On android O (API26) and further, the StatisticsFragment loads immediatly on date selection,
        on older version of Android, we must touch the "OK" button
     => the StatisticsFragment loads the correct date, the title is updated accordingly
  c) open the date picker
     => date D2 is selected
  d) touch date D2 (and Android below O, touch OK to select it)
     => there is no reload, the StatisticsFragment display D2
  e) touch on a top 5 signal
     => the date is D2 is in the title
  f) go back to statistics
     => there is no reload, we still have D2 statistics
  g) go to the HomeFragment, then go back to the StatisticsFragment
     => there is no reload, we still have D2 statistics
  h) touch on a top 5 signal
     => the date is still D2
  i) go back to statistics
     => there is no reload, we still have D2 statistics

# stats, test the cache is outdated on yesterday
4.1) start with the test db
  a) open the app, go on statistics
     => statistics of yesterday load
  b) go onPause(), change the system date to tomorrow, open the app (onResume())
     => the statistics of yesterday (with respect to the new system date) reload
     => the date picker max date is yesterday (with respect to the new system date)
     => the selected date is yesterday (with respect to the new system date)
  c) change the system date to today, open the app, go on statistics
     => statistics of yesterday reload
  d) touch on a top 5 signals
     => the date is still yesterday
  e) go onPause(), change the system date to tomorrow, open the app (onResume())
     => the title of the ExposureDetailsFragment is still yesterday,
  f) go back to statistics
     => the statistics of yesterday (with respect to the new system date) reload
     => the date picker max date is yesterday (with respect to the new system date)
     => the selected date is yesterday (with respect to the new system date)
  g) change the system date to today, open the app, go on statistics
     => statistics of yesterday reload
  h) go to the HomeFragment, go onPause(), change the system date to tomorrow,
     open the app (onResume()), go to the StatisticsFragment
     => statistics of yesterday reload (with respect to the new system date)

# stats, test the cache is outdated on another date (not yesterday)
4.1) start with the test db
  a) open the app, go on statistics
     => statistics of yesterday load
  b) go to the data picker and select two days before (D2)
     => On android O (API26) and further, the StatisticsFragment loads immediatly on date selection,
        on older version of Android, we must touch the "OK" button
     => the StatisticsFragment loads the correct date, the title is updated accordingly
  c) go onPause(), change the system date to tomorrow, open the app (onResume())
     => the statistics of yesterday (with respect to the new system date) reload
     => the date picker max date is yesterday (with respect to the new system date)
     => the selected date is yesterday (with respect to the new system date)
  d) change the system date to today, swipe out the app, open the app, go on statistics
     => statistics of yesterday reload
  e) go to the data picker and select two days before (D2)
     => the StatisticsFragment loads the correct date D2, the title is updated accordingly
  f) touch on a top 5 signals
     => the title date is D2
  g) go onPause(), change the system date to tomorrow, open the app (onResume())
     => the title of the ExposureDetailsFragment is still yesterday,
  h) go back to statistics
     => the statistics of yesterday (with respect to the new system date) reload
     => the date picker max date is yesterday (with respect to the new system date)
     => the selected date is yesterday (with respect to the new system date)
  i) change the system date to today, open the app, go on statistics
     => statistics of yesterday reload
  j) go to the data picker and select two days before (D2)
     => the StatisticsFragment loads the correct date D2, the title is updated accordingly
  k) go to the HomeFragment, go onPause(), change the system date to tomorrow,
     open the app (onResume()), go to the StatisticsFragment
     => statistics of yesterday reload (with respect to the new system date)
     => the date picker max date is yesterday (with respect to the new system date)
     => the selected date is yesterday (with respect to the new system date)
*/
public class StatisticsFragment extends MainActivityFragment {
    private static final String TAG = "StatisticsFragment";
    public static final int ONE_DAY_WINDOW = 24; // in hours

    private Context mContext;
    private MainActivity mActivity;

    // ######## UI components of the fragment #########

    private Timeline mTimeline;
    private ViewTreeObserver.OnGlobalLayoutListener mMyOnGlobalLayoutListener;
    // the root view for all other components in the fragment
    private View mFragmentStatisticsView;
    // the group widget that references all the ui components that need to be set visible/gone ensemble
    private Group mGroupAllUIComponentsExceptPb;
    // Message displayed when there is no data, so we cannot display statistics
    private AppCompatTextView mNoDataMessage;
    // the progressbar spinner that is shown when the data is still loading in the fragment
    private ProgressBar mProgressBar;

    // ## The First/top tile components ##

    private ConstraintLayout mConstraintLayoutSourcesTypeDetails, mConstraintLayoutTop5Sources;

    // title of the first tile
    private AppCompatTextView mTextViewYourExposure;
    private AppCompatTextView mTextViewExposureValue, mTextViewExposureUnit;

    // the text and image views for exposure diff between yesterday and the day before
    private AppCompatImageView mImageViewExposureDiff;
    private AppCompatTextView mTextViewExposureDiff;
    private AppCompatTextView mRecommendationTextView;
    private View mRecommendationDot;

    // The chevron image next to the recommendation text, that opens the exposure scale
    private AppCompatImageView mChevronExposureScaleImage;

    // the layout representing the exposure scale
    private ConstraintLayout mExposureScaleLayout;

    // The text views that show the values for low, medium and high limits
    private AppCompatTextView mScaleLowStart, mScaleMediumStart, mScaleMediumEnd, mScaleHighEnd;

    // the moving component on top of the exposure scale
    private AppCompatImageView mCursorImage;

    // Group that contains all scale components
    private Group mScaleGroup;
    private int mExposureScaleWidth;

    // the new constraint set that needs to be set on the exposure scale layout
    private ConstraintSet mConstraintSetScaleNew;

    // The horizontal scroll view for the bar chart component
    private HorizontalScrollView mHorizontalScrollView;
    private LinearLayout mLayoutBars, mLayoutBarScale;

    // ## Second tile (top 5 sources) components ##

    private AppCompatImageView mImageViewScrollBack, mImageViewScrollForward;

    // ## Third tile (nb of sources) components ##

    private AppCompatTextView mTextViewNbSourceBluetooth;
    private AppCompatTextView mTextViewNbSourceCellular;

    // ## Fourth tile (kind of sources) components ##

    private AppCompatTextView mTextViewTop5Title;
    private AppCompatTextView mTextViewKindOfSourcesTitle;
    private AppCompatTextView mTextViewNbSourcesTitle;
    private AppCompatTextView mTextViewNbSourceWifi;


    // ######## logic components of the fragment #########

    // Variables computed each time the fragment is loaded; initialized onPreExecute of the
    // background task
    //
    // contains the average for a day of all the slotCumulativeTotalMwValue for all slots of the day
    private double dayAverageMwValue = 0;
    // contains the average dBm value for the previous day
    private int previousDayAverageDbmValue = 0;
    // contains the cumulative sum of the Wi-Fi, Bluetooth, or Cellular exposure for a full day in Mw
    private double cumulativeDayWiFiMwValue = 0;
    private double cumulativeDayBluetoothMwValue = 0;
    private double cumulativeDayCellularMwValue = 0;
    private double cumulativeDayTotalMwValue = 0;

    private int nbSourcesWifiDay = 0;
    private int nbSourcesBluetoothDay = 0;
    private int nbSourcesCellularDay = 0;

    private Map<MeasurementFragment.AntennaDisplay, HashSet<BaseProperty>> allUniqueSourcesDayPerAntenna;

    // The current day loaded in the statistics fragment
    private Calendar mStatDay;

    // contains the top 5 signals of the day in a ArrayList to preserve the order, most powerful
    // signal first
    private List<BaseProperty> topFiveSignalsDay = new ArrayList<>();

    // We show a list of top 5 sources in the day in the statistics fragment.
    // When we tap on each of the sources, we open the exposure details fragment. For this, we need
    // the signal (BaseProperty) and the SignalsSlot to which it belongs.
    // This Hashtable serves that purpose.
    private Hashtable<BaseProperty, SignalsSlot> signalToSignalsSlotTable;

    // A table to store the time a SignalsSlot corresponds to
    // We use this to retrieve and show the time of the signals in the exposure details of a
    // SignalsSlot
    private Hashtable<SignalsSlot, Long> signalsSlotToTimeTable;

    // a variable to distinguish the first call, so that we don't have to redraw the scale of
    // horizontal scrollview
    private boolean isFirstCall = true;

    /**
     * Creates a new instance of the fragment to which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static MainActivityFragment newInstance(Bundle arguments) {
        MainActivityFragment fragment = new StatisticsFragment();
        fragment.setArguments(arguments);
        return fragment;
    }


    /**
     * This method is the entry point each time we update the date to display in the
     * {@link StatisticsFragment}
     * <p>
     * Loads the fragment from state if the state's date is equal to the passed statDay, else falls
     * back to a database request to get the SignalsSlot and load the fragment. The UI is then
     * automatically updated.
     *
     * @param statDay a {@link java.util.Calendar} instance that represents the day of the
     *                statistics to be displayed
     */
    private void loadStatisticsAndBuildUI(Calendar statDay) {
        // Reload sAllTop5Signals so that it contains all top 5 signals strictly before yesterday.
        // As getAllTop5SignalsBeforeTheDay considers midnight of the passed day, we must pass it
        // statDay, so that getAllTop5SignalsBeforeTheDay returns all top 5 signals up to
        // and excluding yesterday midnight.
        MainActivity.sAllTop5Signals = DbRequestHandler.getAllTop5SignalsBeforeTheDay(statDay);

        Log.d(TAG, "in loadStatisticsAndBuildUI()");
        // show the progressbar and hide the contents until the data has been loaded
        mGroupAllUIComponentsExceptPb.setVisibility(View.INVISIBLE);
        // Remove previously added bars
        mLayoutBars.removeAllViews();
        mProgressBar.setVisibility(View.VISIBLE);

        if (MainActivity.sStatisticsFragmentState != null &&
                statDay.equals(MainActivity.sStatisticsFragmentState.getStatDay())) {
            Log.d(TAG, "loadStatisticsAndBuildUI: Loading from FragmentState");
            loadsStatisticsFromState();
        } else {
            Log.d(TAG, "loadStatisticsAndBuildUI: Loading from DB");
            loadsStatisticsFromDb(statDay);
        }
    }


    /**
     * Load the variables needed for the fragment from the saved state and then load the UI.
     * Make sure when we call this method that MainActivity.sStatisticsFragmentState != null
     */
    private void loadsStatisticsFromState() {
        this.dayAverageMwValue = MainActivity.sStatisticsFragmentState.getDayAverageMwValue();
        this.previousDayAverageDbmValue = MainActivity.sStatisticsFragmentState.getPreviousDayAverageDbmValue();

        this.cumulativeDayWiFiMwValue = MainActivity.sStatisticsFragmentState.getCumulativeDayWiFiMwValue();
        this.cumulativeDayBluetoothMwValue = MainActivity.sStatisticsFragmentState.getCumulativeDayBluetoothMwValue();
        this.cumulativeDayCellularMwValue = MainActivity.sStatisticsFragmentState.getCumulativeDayCellularMwValue();
        this.cumulativeDayTotalMwValue = MainActivity.sStatisticsFragmentState.getCumulativeDayTotalMwValue();

        // Note, we do not need to keep allUniqueSourcesDayPerAntenna as it is used only
        // to compute nbSourcesWifiDay, nbSourcesBluetoothDay, nbSourcesCellularDay
        this.nbSourcesWifiDay = MainActivity.sStatisticsFragmentState.getNbSourcesWifiDay();
        this.nbSourcesBluetoothDay = MainActivity.sStatisticsFragmentState.getNbSourcesBluetoothDay();
        this.nbSourcesCellularDay = MainActivity.sStatisticsFragmentState.getNbSourcesCellularDay();

        this.mStatDay = MainActivity.sStatisticsFragmentState.getStatDay();
        this.topFiveSignalsDay = MainActivity.sStatisticsFragmentState.getTopFiveSignalsDay();
        this.signalToSignalsSlotTable = MainActivity.sStatisticsFragmentState.getSignalToSignalsSlotTable();
        this.signalsSlotToTimeTable = MainActivity.sStatisticsFragmentState.getSignalsSlotToTimeTable();
        this.mTimeline = MainActivity.sStatisticsFragmentState.getTimeline();

        loadUI(this.mStatDay);
    }


    /**
     * This method request the DB for all the signals during the day statDay.
     * Signals are aggregated in SignalsSlot representing a time gap of Const.STATS_HOUR_TIME_GAP
     * <p>
     * This method will create a TimeLine of 24 slots, and each SignalsSlot will correspond to
     * all the signals for the given hour slot.
     * <p>
     * <p>
     * This method prepares several data structures used by the StatisticsFragment. It resets and
     * update the global mTimeline variable (by side effect)
     *
     * @param statDay The stat day to start the request
     */
    private void loadsStatisticsFromDb(final Calendar statDay) {

        Log.d(TAG, "loadsStatisticsFromDb: statDay: " + statDay);
        Log.d(TAG, "loadsStatisticsFromDb: statDay ms: " + statDay.getTimeInMillis());

        // we build the list of time slots
        final List<Pair<Date, Date>> timeSlots = DbHistoryAsyncTaskRequest.timeSlots(
                statDay.getTime().getTime(),
                ONE_DAY_WINDOW,
                Const.STATS_HOUR_TIME_GAP,
                false);

        //Log.d(TAG, "loadsStatisticsFromDb: timeSlots: " + timeSlots);

        // we call the AsyncTask and define the callback to update the mTimeLine for the AsyncTask
        // result
        DbHistoryAsyncTaskRequest.callBackground(timeSlots,
                new DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks() {
                    @Override
                    public void onPreExecute() {
                        Log.d(TAG, "in onPreExecute()");
                        // initialize all variables that are computed by the background task
                        dayAverageMwValue = 0;
                        previousDayAverageDbmValue = 0;

                        cumulativeDayWiFiMwValue = 0;
                        cumulativeDayBluetoothMwValue = 0;
                        cumulativeDayCellularMwValue = 0;
                        cumulativeDayTotalMwValue = 0;

                        nbSourcesWifiDay = 0;
                        nbSourcesBluetoothDay = 0;
                        nbSourcesCellularDay = 0;

                        // we initialize the data structure that keeps the set of unique signals
                        // per antenna type for a day
                        allUniqueSourcesDayPerAntenna = new HashMap<>();
                        allUniqueSourcesDayPerAntenna.put(MeasurementFragment.AntennaDisplay.WIFI, new HashSet<>());
                        allUniqueSourcesDayPerAntenna.put(MeasurementFragment.AntennaDisplay.BLUETOOTH, new HashSet<>());
                        allUniqueSourcesDayPerAntenna.put(MeasurementFragment.AntennaDisplay.CELLULAR, new HashSet<>());

                        topFiveSignalsDay = new ArrayList<>();
                        signalToSignalsSlotTable = new Hashtable<>();
                        signalsSlotToTimeTable = new Hashtable<>();

                        hideExposureScale();
                    }

                    @Override
                    public void onPostExecute(List<SignalsSlot> result) {
                        Log.d(TAG, "in onPostExecute()");
                        mTimeline.reset(timeSlots.get(0).first.getTime() / 1000);
                        mTimeline.addAll(0, result);


                        Calendar previousDay = (Calendar) statDay.clone();
                        previousDay.add(Calendar.DAY_OF_MONTH, -1);
                        DailyStatSummary previousDayDailyStatSummary =
                                DbRequestHandler.getDailyStatSummaryOfDay(previousDay);
                        previousDayAverageDbmValue = previousDayDailyStatSummary.getDbm();

                        // Temporary variables used for the computations
                        double sum = 0;
                        int nbValidHours = 0;
                        List<BaseProperty> tempTopFiveSignalsDay = new ArrayList<>();

                        // 1) We iterate through all the hours of the next (last) day
                        //Log.d(TAG, "onPostExecute: next day iteration");

                        for (int i = 0; i < mTimeline.size(); i++) {
                            //Log.d(TAG, "onPostExecute: i: " + i + " sum: " + sum);
                            SignalsSlot signalsSlot = mTimeline.get(i);

                            // we build a set of unique signals per antenna for the day
                            if (signalsSlot.containsValidSignals()) {
                                for (MeasurementFragment.AntennaDisplay antenna : MeasurementFragment.AntennaDisplay.values()) {
                                    if (antenna == MeasurementFragment.AntennaDisplay.WIFI) {
                                        allUniqueSourcesDayPerAntenna
                                                .get(antenna)
                                                .addAll(signalsSlot.getSortedWifiGroupSignals());
                                    } else {
                                        allUniqueSourcesDayPerAntenna
                                                .get(antenna)
                                                .addAll(signalsSlot.getChildViewSignals().get(antenna));
                                    }
                                }
                                nbValidHours = nbValidHours + 1;
                            }

                            // `i`  is the number of hours since the commencement of the stat day
                            // We convert it to milliseconds and add it to the statDay
                            signalsSlotToTimeTable.put(signalsSlot, statDay.getTimeInMillis() + i * 60 * 60 * 1000);

                            sum = sum + signalsSlot.getSlotCumulativeTotalMwValue();

                            List<BaseProperty> topFiveSignals = signalsSlot.getTopFiveSignals();

                            /*
                            Build the signalToSignalsSlotTable for each of the topFiveSignals.
                            We also build the tempTopFiveSignalsDay and keep only unique signals
                            in tempTopFiveSignalsDay.

                            In case of equality (in both data structure), we keep the highest
                            power signal.
                            */
                            if (topFiveSignals != null) {
                                for (BaseProperty signal : topFiveSignals) {
                                    if (signal.isValidSignal) {
                                        if (tempTopFiveSignalsDay.contains(signal)) {
                                            int indexOldSignal = tempTopFiveSignalsDay.indexOf(signal);
                                            BaseProperty oldSignal = tempTopFiveSignalsDay.get(indexOldSignal);
                                            if (oldSignal.dbm < signal.dbm) {
                                                tempTopFiveSignalsDay.remove(indexOldSignal);
                                                tempTopFiveSignalsDay.add(signal);
                                                signalToSignalsSlotTable.remove(oldSignal);
                                                signalToSignalsSlotTable.put(signal, signalsSlot);
                                                //Log.d(TAG, "onPostExecute: oldsignal : " + oldSignal.dbm + " newsignal: " + signal.dbm);
                                            }
                                        } else {
                                            tempTopFiveSignalsDay.add(signal);
                                            signalToSignalsSlotTable.put(signal, signalsSlot);
                                        }
                                    }
                                }
                            }

                            cumulativeDayWiFiMwValue = cumulativeDayWiFiMwValue +
                                    signalsSlot.getSlotCumulativeWifiMwValue();

                            cumulativeDayBluetoothMwValue = cumulativeDayBluetoothMwValue +
                                    signalsSlot.getSlotCumulativeBluetoothMwValue();

                            cumulativeDayCellularMwValue = cumulativeDayCellularMwValue +
                                    signalsSlot.getSlotCumulativeCellularMwValue();

//                            Log.d(TAG, "onPostExecute: SignalsSlot: " + signalsSlot);
//                            Log.d(TAG, "onPostExecute: in Last Day. " +
//                                    "cumulativeDayWiFiMwValue: " + cumulativeDayWiFiMwValue +
//                                    " cumulativeDayBluetoothMwValue: " + cumulativeDayBluetoothMwValue +
//                                    " cumulativeDayCellularMwValue: " + cumulativeDayCellularMwValue
//                            );
                        }

                        // we compute the number of sources for the stat day for each antenna type.
                        HashSet<BaseProperty> uniqueSignals = allUniqueSourcesDayPerAntenna.get(MeasurementFragment.AntennaDisplay.WIFI);
                        nbSourcesWifiDay = (uniqueSignals == null) ? 0 : getValidSignalsCount(uniqueSignals);

                        uniqueSignals = allUniqueSourcesDayPerAntenna.get(MeasurementFragment.AntennaDisplay.BLUETOOTH);
                        nbSourcesBluetoothDay = (uniqueSignals == null) ? 0 : getValidSignalsCount(uniqueSignals);

                        uniqueSignals = allUniqueSourcesDayPerAntenna.get(MeasurementFragment.AntennaDisplay.CELLULAR);
                        nbSourcesCellularDay = (uniqueSignals == null) ? 0 : getValidSignalsCount(uniqueSignals);

//                        Log.d(TAG, "Total number of sources: nb wifi: " + nbSourcesWifiDay +
//                                " nb BT: " + nbSourcesBluetoothDay +
//                                " nb Cellular: " + nbSourcesCellularDay);

                        // Set the stat day remaining variables
                        dayAverageMwValue = sum / nbValidHours;

                        Tools.arraySortDbm(tempTopFiveSignalsDay, true);
                        if (tempTopFiveSignalsDay.size() > 5) {
                            topFiveSignalsDay.addAll(new ArrayList<>(tempTopFiveSignalsDay.subList(0, 5)));
                        } else {
                            topFiveSignalsDay.addAll(new ArrayList<>(tempTopFiveSignalsDay));
                        }

                        cumulativeDayTotalMwValue = cumulativeDayWiFiMwValue +
                                cumulativeDayBluetoothMwValue +
                                cumulativeDayCellularMwValue;

                        // debug logs
//                        Log.d(TAG, "onPostExecute: sum: " + sum +
//                                " previousDayAverageDbmValue: " + previousDayAverageDbmValue +
//                                " dayAverageMwValue: " + dayAverageMwValue);
//                        Log.d(TAG, "onPostExecute: wifi% = " + (cumulativeDayTotalMwValue != 0 ?
//                                cumulativeDayWiFiMwValue / cumulativeDayTotalMwValue : 0));
//                        Log.d(TAG, "onPostExecute: BT% = " + (cumulativeDayTotalMwValue != 0 ?
//                                cumulativeDayBluetoothMwValue / cumulativeDayTotalMwValue : 0));
//                        Log.d(TAG, "onPostExecute: Cellular% = " + (cumulativeDayTotalMwValue != 0 ?
//                                cumulativeDayCellularMwValue / cumulativeDayTotalMwValue : 0));
//                        Log.d(TAG, "onPostExecute: topFiveSignalsDay: " +
//                                topFiveSignalsDay);

                        /*
                        We cache all the computed variables if no variable is yet cached or if
                        the computed day (identified by statDay, which is one day before the
                        the day that is displayed) is not the one currently cached.

                        Note: it means that we always cache a single day at a time. We do not
                              want to cache more as to cache multiple days, the best strategy
                              would be to persist the variables in the DB.
                        */
                        if (MainActivity.sStatisticsFragmentState == null ||
                                !MainActivity.sStatisticsFragmentState.getStatDay().equals(statDay)) {
                            MainActivity.sStatisticsFragmentState = new StatisticsFragmentState(
                                    cumulativeDayBluetoothMwValue, cumulativeDayCellularMwValue,
                                    cumulativeDayTotalMwValue, cumulativeDayWiFiMwValue,
                                    previousDayAverageDbmValue, dayAverageMwValue, nbSourcesWifiDay,
                                    nbSourcesBluetoothDay, nbSourcesCellularDay, topFiveSignalsDay,
                                    signalToSignalsSlotTable, signalsSlotToTimeTable, statDay,
                                    mTimeline
                            );
                        }

                        loadUI(statDay);
                    }
                });
    }


    /**
     * Create all the elements to display in the fragment.
     *
     * @param statDay the day on which we want to load statistics. Used to go back to the correct
     *                date in the {@link StatisticsFragment} from the {@link ExposureDetailsFragment}
     */
    private void loadUI(final Calendar statDay) {
        Log.d(TAG, "in loadUI()");

        Calendar day = (Calendar) statDay.clone();
        day.add(Calendar.DAY_OF_MONTH, 1);

        // #### 0) we set the titles of the tiles that are date dependant
        if (DateUtils.isToday(day.getTimeInMillis())) {
            // statDay is yesterday
            mTextViewTop5Title.setText(R.string.statistics_top_5_sources_yesterday_title);
            mTextViewKindOfSourcesTitle.setText(R.string.statistics_kind_of_sources_yesterday_title);
            mTextViewNbSourcesTitle.setText(R.string.statistics_nb_sources_yesterday_title);
            mActivity.setupToolbar(R.string.statistics_yesterday_title);
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                    getString(R.string.statistics_title_date_format), Tools.getDefaultDeviceLocale());
            mTextViewTop5Title.setText(R.string.statistics_top_5_sources_otherday_title);
            mTextViewKindOfSourcesTitle.setText(R.string.statistics_kind_of_sources_otherday_title);
            mTextViewNbSourcesTitle.setText(R.string.statistics_nb_sources_otherday_title);
            mActivity.setupToolbar(Tools.capitalize(String.format(
                    getString(R.string.statistics_on_date_title), simpleDateFormat.format(statDay.getTime()))));
        }

        mTextViewYourExposure.setText(R.string.statistics_my_exposure);

        // The maximum and minimum heights of the bars in pixels
        int heightMax = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_bar_height_max);
        int heightMin = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_bar_height_min);

        // A small piece of horizontal line (part of the scale) which is shown at the beginning
        // of the scale
        int preLineSegmentLength = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_bar_margin_right_left) +
                (mContext.getResources().getDimensionPixelSize(R.dimen.statistics_bar_width) / 2);


        // #### 1) we build the bar chart

        for (int i = 0; i < mTimeline.size(); i++) {
            SignalsSlot signalsSlot = mTimeline.get(i);
            // create bars from timeline data
            int signalsSlotExposureScore = 0;
            if (signalsSlot.containsValidSignals()) {
                signalsSlotExposureScore = Tools.getExposureScore(signalsSlot.getSlotCumulativeTotalDbmValue());
            }

            //Log.d(TAG, "onPostExecute: " + i + " signalsSlotExposureScore = " + signalsSlotExposureScore);

            // Each bar is a TableRow
            TableRow tr = new TableRow(mContext);

            // Define and set the parameters of the layout
            TableRow.LayoutParams lp = new TableRow.LayoutParams();
            lp.width = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_bar_width);
            lp.height = (int) ((signalsSlotExposureScore * 0.01) * (heightMax - heightMin));
            lp.gravity = Gravity.BOTTOM;
            lp.leftMargin = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_bar_margin_right_left);
            lp.rightMargin = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_bar_margin_right_left);
            tr.setLayoutParams(lp);

            // set the color to the bar based on the cumulative dbm value
            int colorResourceId = Tools.getRecommendationDotColorResourceIdFromDbm(signalsSlot.getSlotCumulativeTotalDbmValue());
            tr.setBackgroundColor(ContextCompat.getColor(mContext, colorResourceId));

            // Add tablerow instance to the layout
            mLayoutBars.addView(tr);

            // horizontal scroll view scale related components; to be added for the
            // first call only
            // note: the hours labels are added in the xml
            if (isFirstCall) {

                // Dynamic generation of scale below the bars
                // we make a tick every 3 hours, for hour 0 and for hours 2
                if ((i + 4) % 3 == 0 || (i % ONE_DAY_WINDOW == 0) || (i == ONE_DAY_WINDOW + 2)) {
                    // left_right_margin
                    //
                    //      |                                   |                           |                        |
                    //      |               BAR  1              |                           |          BAR 2         |
                    //<---->|                                   |                           |                        |
                    //      |-----------------------------------|                           |------------------------|
                    //
                    //                      vertical
                    //                        line
                    //                         |                                                        |
                    // ------------------------|--------------------------------------------------------|-----------------------------------
                    //                        2h                                                       5h
                    // <--------------------->  <--------------> <-------------------->
                    //  pre line segment        post line segment  normal line segment

                    // pre line segment
                    TableRow trScale = new TableRow(mContext);
                    TableRow.LayoutParams lpScale = new TableRow.LayoutParams();
                    lpScale.width = preLineSegmentLength;
                    lpScale.height = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_height);
                    lpScale.gravity = Gravity.BOTTOM;
                    lpScale.bottomMargin = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_margin_bottom);
                    trScale.setLayoutParams(lpScale);
                    trScale.setBackgroundColor(ContextCompat.getColor(mContext, R.color.secondary_text_color));
                    mLayoutBarScale.addView(trScale);

                    // vertical line
                    trScale = new TableRow(mContext);
                    lpScale = new TableRow.LayoutParams();
                    lpScale.width = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_vertical_line_width);
                    lpScale.height = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_vertical_line_height);
                    lpScale.gravity = Gravity.BOTTOM;
                    trScale.setLayoutParams(lpScale);
                    trScale.setBackgroundColor(ContextCompat.getColor(mContext, R.color.secondary_text_color));
                    mLayoutBarScale.addView(trScale);

                    // post line segment
                    trScale = new TableRow(mContext);
                    lpScale = new TableRow.LayoutParams();
                    lpScale.width = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_width_when_tick) - preLineSegmentLength;
                    lpScale.height = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_height);
                    lpScale.gravity = Gravity.BOTTOM;
                    lpScale.bottomMargin = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_margin_bottom);
                    trScale.setLayoutParams(lpScale);
                    trScale.setBackgroundColor(ContextCompat.getColor(mContext, R.color.secondary_text_color));
                    mLayoutBarScale.addView(trScale);
                } else {
                    // add normal line
                    TableRow trScale = new TableRow(mContext);
                    TableRow.LayoutParams lpScale = new TableRow.LayoutParams();
                    lpScale.width = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_width);
                    lpScale.height = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_height);
                    lpScale.gravity = Gravity.BOTTOM;
                    lpScale.bottomMargin = mContext.getResources().getDimensionPixelSize(R.dimen.statistics_scale_margin_bottom);
                    trScale.setLayoutParams(lpScale);
                    trScale.setBackgroundColor(ContextCompat.getColor(mContext, R.color.secondary_text_color));
                    mLayoutBarScale.addView(trScale);
                }
            }
        }


        // Change the scroll position of the bar chart to the right after the UI has been laid out
        mHorizontalScrollView.post(() -> mHorizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT));


        // #### 2) Set the exposure value, exposure scale, and recommendation

        int dayAverageDbmValue = Tools.milliWattToDbm(dayAverageMwValue);
        mRecommendationTextView.setText(Tools.getRecommendationTextBasedOnDbm(mContext, dayAverageDbmValue));
        Tools.setRecommendationDotBasedOnDbm(mContext, dayAverageDbmValue, mRecommendationDot);

        // We display the exposure value and the exposure scale
        String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();

        // Adapt the exposure value to the defined exposure metric
        if (pref.equals(mContext.getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
            mImageViewExposureDiff.setVisibility(View.VISIBLE);
            mTextViewExposureUnit.setText(R.string.exposure_score_scale);
            mTextViewExposureValue.setText(String.valueOf(Tools.getExposureScore(dayAverageDbmValue)));
            mScaleLowStart.setText(String.valueOf(Tools.getExposureScore(Const.MIN_RECOMMENDATION_SCALE)));
            mScaleMediumStart.setText(String.valueOf(Tools.getExposureScore(Const.RECOMMENDATION_LOW_THRESHOLD)));
            mScaleMediumEnd.setText(String.valueOf(Tools.getExposureScore(Const.RECOMMENDATION_HIGH_THRESHOLD)));
            mScaleHighEnd.setText(String.valueOf(Tools.getExposureScore(Const.MAX_RECOMMENDATION_SCALE)));
            int dayDiffScoreValue = Tools.getExposureScore(dayAverageDbmValue)
                    - Tools.getExposureScore(previousDayAverageDbmValue);

            // set the diff text and image views
            if (dayDiffScoreValue > 0) {
                mTextViewExposureDiff.setText(String.format(getString(R.string.statistics_positive_diff_format),
                        String.valueOf(dayDiffScoreValue)));
                mImageViewExposureDiff.setImageResource(R.drawable.ic_arrow_upward_black_24dp);
            } else if (dayDiffScoreValue == 0) {
                mTextViewExposureDiff.setText(String.valueOf(dayDiffScoreValue));
                mImageViewExposureDiff.setImageResource(R.drawable.ic_arrow_forward_black_24dp);
            } else {
                mTextViewExposureDiff.setText(String.valueOf(dayDiffScoreValue));
                mImageViewExposureDiff.setImageResource(R.drawable.ic_arrow_downward_black_24dp);
            }
        } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_DBM_METRIC))) {
            mTextViewExposureUnit.setText(R.string.dbm_metric);
            mTextViewExposureValue.setText(String.valueOf(dayAverageDbmValue));
            mScaleLowStart.setText(String.valueOf(Const.MIN_RECOMMENDATION_SCALE));
            mScaleMediumStart.setText(String.valueOf(Const.RECOMMENDATION_LOW_THRESHOLD));
            mScaleMediumEnd.setText(String.valueOf(Const.RECOMMENDATION_HIGH_THRESHOLD));
            mScaleHighEnd.setText(String.valueOf(Const.MAX_RECOMMENDATION_SCALE));

            // don't show diff when the unit is in dbm
            mImageViewExposureDiff.setImageResource(0);
            mTextViewExposureDiff.setText("");
        } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_POWER_METRIC))) {
            // The RawSignalHandler normalize all signals before passing them to the UI.
            // We have the guarantee that the dbm value can be converted in watt.
            String[] watt = Tools.dBmToWattWithSIPrefixArray(dayAverageDbmValue, false);
            mTextViewExposureValue.setText(String.valueOf(Integer.parseInt(watt[0])));
            mTextViewExposureUnit.setText(watt[1]);
            mScaleLowStart.setText(Tools.dBmToWattWithSIPrefix(Const.MIN_RECOMMENDATION_SCALE, false));
            mScaleMediumStart.setText(Tools.dBmToWattWithSIPrefix(Const.RECOMMENDATION_LOW_THRESHOLD, false));
            mScaleMediumEnd.setText(Tools.dBmToWattWithSIPrefix(Const.RECOMMENDATION_HIGH_THRESHOLD, false));
            mScaleHighEnd.setText(Tools.dBmToWattWithSIPrefix(Const.MAX_RECOMMENDATION_SCALE, false));

            // don't show diff when the unit is in mw
            mImageViewExposureDiff.setImageResource(0);
            mTextViewExposureDiff.setText("");
        }

        // In case there is no signal in the previous day, we do not show the diff
        if (previousDayAverageDbmValue < Const.MIN_DBM_FOR_ROOT_SCORE) {
            // .setVisibility(View.INVISIBLE) does not work. It is not clear why,
            // might be a appCompat bug. .setImageResource(0) works so we use this
            // solution
            mImageViewExposureDiff.setImageResource(0);
            mTextViewExposureDiff.setText("");
        }

        // Reset the chevron and the exposure scale to the position/visibility they
        // were in before
        if (Tools.getShowExposureScale()) {
            // rotate the chevron when on create if Tools.getShowExposureScale() returns
            // true, because, in that case, the exposure scale is expanded.
            HomeFragment.rotateImageWithAnimation(mChevronExposureScaleImage, 0, 90, false);
            HomeFragment.animateCursorToDbm(dayAverageDbmValue, true, mCursorImage,
                    mContext, mExposureScaleWidth, this, mFragmentStatisticsView);
            showExposureScale();
        } else {
            HomeFragment.rotateImageWithAnimation(mChevronExposureScaleImage, 90, 0, false);
            hideExposureScale();
        }

        // #### 3) we display the top 5 sources

        // remove previously added views because we will create and add them below again
        mConstraintLayoutTop5Sources.removeAllViews();

        // An array of views representing the top five sources
        View[] top5Sources = new View[topFiveSignalsDay.size()];

        // We instantiate each of the views in the loop below
        int position = 0;
        for (final BaseProperty signal : topFiveSignalsDay) {
            if (signal != null) {
                top5Sources[position] = getLayoutInflater()
                        .inflate(R.layout._statistics_top5_sources_row, mConstraintLayoutTop5Sources,
                                false);
                top5Sources[position].setId(ViewCompat.generateViewId());

                // we handle the behavior when we touch one of the top 5 sources
                top5Sources[position].setOnClickListener(v -> {
                    SignalsSlot signalsSlot = signalToSignalsSlotTable.get(signal);
                    if (signalsSlot != null) {
                        MeasurementFragment.AntennaDisplay signalAntennaDisplay = signal.getAntennaDisplay();
                        List<BaseProperty> sortedGroupViewSignals = null;

                        // a) We start by retrieving the index of the signal in the list of
                        //    signals. This is used by the ExposureDetailsFragment to highlight
                        //    the correct tile
                        int indexOfSignal = -1;
                        if (signalAntennaDisplay == MeasurementFragment.AntennaDisplay.WIFI) {
                            sortedGroupViewSignals = signalsSlot.getSortedWifiGroupSignals();
                            if (sortedGroupViewSignals != null) {
                                for (BaseProperty childSignal : sortedGroupViewSignals) {
                                    if (childSignal.equals(signal)) {
                                        indexOfSignal = sortedGroupViewSignals.indexOf(childSignal);
                                        break;
                                    }
                                }
                            }
                        } else {
                            if (signalsSlot.getChildViewSignals() != null) {
                                sortedGroupViewSignals = signalsSlot.getChildViewSignals().get(signalAntennaDisplay);
                            }
                            if (sortedGroupViewSignals != null && sortedGroupViewSignals.contains(signal)) {
                                indexOfSignal = sortedGroupViewSignals.indexOf(signal);
                            }
                        }

                        // b) Then we retrieve the corresponding timestamp for this signal
                        if (indexOfSignal >= 0) {
                            String fragmentTitle = null;
                            long timestamp = -1;
                            if (signalsSlotToTimeTable.get(signalsSlot) != null) {
                                timestamp = signalsSlotToTimeTable.get(signalsSlot);
                            }
                            if (timestamp != -1) {
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(timestamp);
                                Date d = cal.getTime();

                                // We add one day to the calendar and check if it is today
                                // if today => the timestamp was yesterday otherwise we show a
                                // different date format
                                cal.add(Calendar.DAY_OF_MONTH, 1);
                                if (DateUtils.isToday(cal.getTimeInMillis())) {
                                    // timestamp is yesterday
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                            getString(R.string.exposure_details_title_datetime_format_yesterday),
                                            Tools.getDefaultDeviceLocale());
                                    fragmentTitle = simpleDateFormat.format(d);
                                } else {
                                    // timestamp is older than yesterday
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                            getString(R.string.exposure_details_title_datetime_format_custom),
                                            Tools.getDefaultDeviceLocale());
                                    fragmentTitle = simpleDateFormat.format(d);
                                }
                            }

                            // c) we call the ExposureDetailsFragment with the signal index
                            //    and timestamp
                            mActivity.showExposureDetails(signal.getAntennaDisplay(),
                                    signalsSlot, indexOfSignal, statDay, Tools.capitalize(fragmentTitle));
                        }
                    }
                });

                // Set the image/texts for the row
                AppCompatImageView protocolImage = top5Sources[position].findViewById(R.id.protocol_image);
                AppCompatTextView sourceName = top5Sources[position].findViewById(R.id.text_view_source_name);
                AppCompatImageView connectedImage = top5Sources[position].findViewById(R.id.image_view_connected);
                AppCompatImageView isNewSourceImage = top5Sources[position].findViewById(R.id.new_source);
                View expositionDot = top5Sources[position].findViewById(R.id.exposition_dot);

                // Set the protocol image/connected image/sourceName
                if (signal instanceof LteProperty || signal instanceof GsmProperty ||
                        signal instanceof WcdmaProperty || signal instanceof CdmaProperty ||
                        signal instanceof NewRadioProperty) {
                    protocolImage.setImageResource(R.drawable.baseline_signal_cellular_4_bar_24);
                    // we display the connected image if this is the serving cell
                    if (signal.connected) {
                        connectedImage.setVisibility(View.VISIBLE);
                    } else {
                        connectedImage.setVisibility(View.INVISIBLE);
                    }
                    // we set the name of the operator
                    signal.prepareOperatorName(); // we call it again in case there is no serving cell
                    if (signal instanceof CdmaProperty) {
                        sourceName.setText(
                                String.format(getString(R.string.statistics_cdma_source_name_format),
                                        Tools.getStringForValueInRange(mContext, signal.network_id, Const.CDMA_MIN_NID, Const.CDMA_MAX_NID))
                        );
                    } else {
                        sourceName.setText(signal.mOperatorName);
                    }
                } else if (signal instanceof WifiProperty) {
                    protocolImage.setImageResource(R.drawable.baseline_wifi_24);

                    // we display the connected image if we are connected to the Wi-Fi antenna
                    if (signal.connected) {
                        connectedImage.setVisibility(View.VISIBLE);
                    } else {
                        connectedImage.setVisibility(View.INVISIBLE);
                    }

                    if (signal.ssid != null && !signal.ssid.isEmpty()) {
                        sourceName.setText(signal.friendlySourceName(mContext));
                    } else {
                        sourceName.setText(R.string.hidden_ssid);
                    }

                } else if (signal instanceof BluetoothProperty) {
                    protocolImage.setImageResource(R.drawable.baseline_bluetooth_24);

                    // we display the connected image if we are connected to the BT device
                    if (signal.bt_bond_state == BluetoothDevice.BOND_BONDED) {
                        connectedImage.setVisibility(View.VISIBLE);
                    } else {
                        connectedImage.setVisibility(View.INVISIBLE);
                    }

                    // we set the name of the highest BT signal
                    if (signal.bt_device_name != null && !signal.bt_device_name.isEmpty()) {
                        // We display bonded devices in bold
                        sourceName.setText(signal.bt_device_name);
                    } else {
                        sourceName.setText(R.string.not_available);
                    }
                }

                // We show the NEW label for new sources
                long oldestTop5SignalsTimestampInDB = DbRequestHandler.getOldestTop5SignalsTimeStampDB();
                /*
                 We show the NEW label only if we have at least three days of computed top5Signals
                 in the DB. Therefore, we display the NEW label if
                 Tools.getNumberOfDaysBetweenTimestamps(oldestTop5SignalsTimestampInDB,
                 statDay.getTimeInMillis()) >= Const.MIN_NUM_OF_DAYS_BEFORE_SHOWING_NEW_SOURCE_NOTIF
                */
                if (oldestTop5SignalsTimestampInDB != 0 &&
                        (Tools.getNumberOfDaysBetweenTimestamps(oldestTop5SignalsTimestampInDB,
                                statDay.getTimeInMillis()) >=
                                Const.MIN_NUM_OF_DAYS_BEFORE_SHOWING_NEW_SOURCE_NOTIF)) {
                    if (MainActivity.sAllTop5Signals.contains(signal)) {
                        isNewSourceImage.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "loadUI: signal exists in sAllTop5Signals " + signal.toString());
                    } else {
                        if ((signal instanceof WifiProperty) || (signal instanceof BluetoothProperty)) {
                            isNewSourceImage.setVisibility(View.VISIBLE);
                            Log.d(TAG, "loadUI: signal does not exist in sAllTop5Signals " + signal.toString());
                        } else {
                            isNewSourceImage.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "loadUI: signal is new, but it is a cellular, we do not show as a new source");
                        }
                    }
                } else {
                    isNewSourceImage.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "loadUI: new source notification not yet shown");
                }

                // Set the exposition dot color based on cumul_dbm
                Tools.setRecommendationDotBasedOnDbm(mContext, signal.dbm, expositionDot);

                // Add the view to the layout
                mConstraintLayoutTop5Sources.addView(top5Sources[position]);
                position++;
            }
        }

        Log.d(TAG, "loadUI: sAllTop5Signals = " + MainActivity.sAllTop5Signals);
        Log.d(TAG, "loadUI: sAllTop5Signals.size() = " + MainActivity.sAllTop5Signals.size());

        // set the last divider invisible
        if (topFiveSignalsDay.size() > 0) {
            TableRow lastDivider = top5Sources[topFiveSignalsDay.size() - 1].findViewById(R.id.source_divider);
            lastDivider.setVisibility(View.INVISIBLE);
        }

        // Create constraintSet and connect each of the top five signal views
        // created above
        ConstraintSet constraintSetTop5Sources = new ConstraintSet();
        constraintSetTop5Sources.clone(mConstraintLayoutTop5Sources);

        // index representing the one of the top five signals view and used for
        // accessing them in the loop sourcePosition=0 refers to the topmost signal
        int sourcePosition = 0;

        for (View sourceView : top5Sources) {
            // constrain horizontally to the start of the parent
            constraintSetTop5Sources.connect(
                    sourceView.getId(), ConstraintSet.START,
                    mConstraintLayoutTop5Sources.getId(), ConstraintSet.START, 0);

            // We connect the topmost signal to the top of the layout, and the
            // following signals to ones above them
            if (sourcePosition == 0) {
                constraintSetTop5Sources.connect(
                        sourceView.getId(), ConstraintSet.TOP,
                        mConstraintLayoutTop5Sources.getId(), ConstraintSet.TOP, 0);
            } else {
                constraintSetTop5Sources.connect(
                        sourceView.getId(), ConstraintSet.TOP,
                        top5Sources[sourcePosition - 1].getId(), ConstraintSet.BOTTOM, 0);
            }
            sourcePosition++;
        }

        TransitionManager.beginDelayedTransition(mConstraintLayoutTop5Sources);
        constraintSetTop5Sources.applyTo(mConstraintLayoutTop5Sources); // set new constraints

        // #### 4) We display the number of sources

        // we set the text views
        mTextViewNbSourceWifi.setText(String.valueOf(nbSourcesWifiDay));
        mTextViewNbSourceBluetooth.setText(String.valueOf(nbSourcesBluetoothDay));
        mTextViewNbSourceCellular.setText(String.valueOf(nbSourcesCellularDay));

        // ### 5) We display the types of sources that exposed me the most yesterday

        Hashtable<MeasurementFragment.AntennaDisplay, Double> signalValuesToCompare = new Hashtable<>();
        if (cumulativeDayWiFiMwValue > 0) {
            signalValuesToCompare.put(MeasurementFragment.AntennaDisplay.WIFI,
                    cumulativeDayWiFiMwValue);
        }
        if (cumulativeDayCellularMwValue > 0) {
            signalValuesToCompare.put(MeasurementFragment.AntennaDisplay.CELLULAR,
                    cumulativeDayCellularMwValue);
        }
        if (cumulativeDayBluetoothMwValue > 0) {
            signalValuesToCompare.put(MeasurementFragment.AntennaDisplay.BLUETOOTH,
                    cumulativeDayBluetoothMwValue);
        }

        // Types of sources that expose me the most today
        Double maxMwValue;
        MeasurementFragment.AntennaDisplay topAntenna;
        mConstraintLayoutSourcesTypeDetails.removeAllViews();
        View[] sourceTypes = new View[signalValuesToCompare.size()];
        position = 0;

        while (signalValuesToCompare.size() > 0) {
            topAntenna = null;
            maxMwValue = Double.MIN_VALUE;
            for (MeasurementFragment.AntennaDisplay key : signalValuesToCompare.keySet()) {
                Double tmp = signalValuesToCompare.get(key);
                if (tmp != null && tmp.compareTo(maxMwValue) >= 0) {
                    maxMwValue = tmp;
                    topAntenna = key;
                }
            }

            if (topAntenna != null) {
                sourceTypes[position] = getLayoutInflater()
                        .inflate(R.layout._statistics_exposing_sources_type, mConstraintLayoutSourcesTypeDetails, false);
                sourceTypes[position].setId(ViewCompat.generateViewId());

                AppCompatImageView protocolImage = sourceTypes[position].findViewById(R.id.protocol_image);
                AppCompatTextView sourceType = sourceTypes[position].findViewById(R.id.text_view_source_type);
                ProgressBar progressBarSourceType = sourceTypes[position].findViewById(R.id.progress_bar_source_type);
                AppCompatTextView exposurePercent = sourceTypes[position].findViewById(R.id.textViewExposurePercentage);
                double percent = -1;
                // set the icon accordingly
                if (topAntenna == MeasurementFragment.AntennaDisplay.WIFI) {
                    protocolImage.setImageResource(R.drawable.baseline_wifi_24);
                    sourceType.setText(R.string.wifi);
                    percent = ((cumulativeDayWiFiMwValue / cumulativeDayTotalMwValue) * 100);
                } else if (topAntenna == MeasurementFragment.AntennaDisplay.CELLULAR) {
                    protocolImage.setImageResource(R.drawable.baseline_signal_cellular_4_bar_24);
                    sourceType.setText(R.string.cellular);
                    percent = ((cumulativeDayCellularMwValue / cumulativeDayTotalMwValue) * 100);
                } else if (topAntenna == MeasurementFragment.AntennaDisplay.BLUETOOTH) {
                    protocolImage.setImageResource(R.drawable.baseline_bluetooth_24);
                    sourceType.setText(R.string.bt);
                    percent = ((cumulativeDayBluetoothMwValue / cumulativeDayTotalMwValue) * 100);
                }
                //Log.d(TAG, "onPostExecute: PERCENT " + percent + Math.round(percent));

                /*
                When the percentage is larger than 99%, but lower than 100%, we display 99%
                When the percentage is larger than 0% but lower than 1% we display "< 1%"
                 */
                if (percent != -1) {
                    int roundedPercent = (int) Math.round(percent);
                    String percentString = roundedPercent + getString(R.string.statistics_type_source_percentage_unit);

                    if (percent == 100) {
                        percentString = getString(R.string.statistics_type_source_100_percent);
                    }

                    if (roundedPercent == 100 && percent < 100) {
                        // Show 99%
                        percentString = getString(R.string.statistics_type_source_99_percent);
                    }

                    if (roundedPercent == 0 && percent > 0) {
                        // Show < 1%
                        percentString = getString(R.string.statistics_type_source_lower_than_1_percent);
                    }
                    progressBarSourceType.setProgress(roundedPercent);
                    exposurePercent.setText(percentString);
                }
                mConstraintLayoutSourcesTypeDetails.addView(sourceTypes[position], position);
                position++;
                signalValuesToCompare.remove(topAntenna);
            }
        }

        // set the last divider invisible
        if (sourceTypes.length > 0) {
            TableRow lastDivider = sourceTypes[sourceTypes.length - 1].findViewById(R.id.source_divider);
            lastDivider.setVisibility(View.INVISIBLE);
        }

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mConstraintLayoutSourcesTypeDetails);

        sourcePosition = 0;
        for (View sourceType : sourceTypes) {
            constraintSet.connect(sourceType.getId(), ConstraintSet.START,
                    mConstraintLayoutSourcesTypeDetails.getId(), ConstraintSet.START, 0);
            if (sourcePosition == 0) {
                constraintSet.connect(sourceType.getId(), ConstraintSet.TOP,
                        mConstraintLayoutSourcesTypeDetails.getId(), ConstraintSet.TOP, 0);
            } else {
                constraintSet.connect(sourceType.getId(), ConstraintSet.TOP,
                        sourceTypes[sourcePosition - 1].getId(), ConstraintSet.BOTTOM, 0);
            }
            sourcePosition++;
        }

        TransitionManager.beginDelayedTransition(mConstraintLayoutSourcesTypeDetails);
        constraintSet.applyTo(mConstraintLayoutSourcesTypeDetails); // set new constraints
        mGroupAllUIComponentsExceptPb.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        isFirstCall = false;

        // #### 6) Handle the no data case
        if (topFiveSignalsDay.isEmpty()) {
            mGroupAllUIComponentsExceptPb.setVisibility(View.INVISIBLE);
            hideExposureScale();
            mNoDataMessage.setVisibility(View.VISIBLE);
            if (DateUtils.isToday(Tools.oldestSignalInDb())) {
                mNoDataMessage.setText(R.string.statistics_no_data_first_day);
            } else {
                noDataMessage(statDay);
            }
        } else {
            mGroupAllUIComponentsExceptPb.setVisibility(View.VISIBLE);
            mNoDataMessage.setVisibility(View.GONE);
        }
    }


    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "in onAttach()");
        super.onAttach(context);
        mContext = context;
        mActivity = (MainActivity) getActivity();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "in onCreateView()");

        // root view of all other components in this fragment
        mFragmentStatisticsView = inflater.inflate(R.layout.fragment_statistics, container, false);

        // group to show/hide all components in one shot.
        mGroupAllUIComponentsExceptPb = mFragmentStatisticsView.findViewById(R.id.all_ui_components_except_pb);

        // progress bar to display when the data is loading.
        mProgressBar = mFragmentStatisticsView.findViewById(R.id.progressbar_loading);

        // we start the fragment with the progress bar spinning.
        mGroupAllUIComponentsExceptPb.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);

        // We init the TimeLine object to contains ONE_DAY_WINDOW of data
        // with a STATS_HOUR_TIME_GAP time gap.
        mTimeline = new Timeline(ONE_DAY_WINDOW, Const.STATS_HOUR_TIME_GAP);

        mLayoutBars = mFragmentStatisticsView.findViewById(R.id.layout_bars);
        mLayoutBarScale = mFragmentStatisticsView.findViewById(R.id.layout_bar_scale);

        mHorizontalScrollView = mFragmentStatisticsView.findViewById(R.id.horizontal_scroll_view);
        // Back and Forward ImageViews
        mImageViewScrollBack = mFragmentStatisticsView.findViewById(R.id.scroll_view_backward);
        mImageViewScrollForward = mFragmentStatisticsView.findViewById(R.id.scroll_view_forward);
        // listener to hide/show back and forward arrows in the bar chart scroll view
        mHorizontalScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            // we disable the onScrollChanged in case there is no signal, otherwise, the image
            // views can be set visible. Indeed, in loadUI(), we scroll right
            // (even if there is no signal), which call this method.
            if (!topFiveSignalsDay.isEmpty()) {
                if (!mHorizontalScrollView.canScrollHorizontally(1)) {
                    // already at the rightmost position
                    // hide forward arrow
                    mImageViewScrollForward.setVisibility(View.INVISIBLE);
                } else {
                    mImageViewScrollForward.setVisibility(View.VISIBLE);
                }

                if (!mHorizontalScrollView.canScrollHorizontally(-1)) {
                    // already at the leftmost position
                    // hide the back arrow
                    mImageViewScrollBack.setVisibility(View.INVISIBLE);
                } else {
                    mImageViewScrollBack.setVisibility(View.VISIBLE);
                }
            }
        });

        mTextViewYourExposure = mFragmentStatisticsView.findViewById(R.id.textViewYourExposure);
        mTextViewExposureValue = mFragmentStatisticsView.findViewById(R.id.textViewExposureValue);
        mTextViewExposureUnit = mFragmentStatisticsView.findViewById(R.id.textViewExposureUnit);
        mScaleLowStart = mFragmentStatisticsView.findViewById(R.id.scale_low_start);
        mScaleMediumStart = mFragmentStatisticsView.findViewById(R.id.scale_medium_start);
        mScaleMediumEnd = mFragmentStatisticsView.findViewById(R.id.scale_medium_end);
        mScaleHighEnd = mFragmentStatisticsView.findViewById(R.id.scale_high_end);
        mRecommendationTextView = mFragmentStatisticsView.findViewById(R.id.textViewRecommendation);
        mRecommendationDot = mFragmentStatisticsView.findViewById(R.id.recommendationDot);

        // exposure scale related code
        mChevronExposureScaleImage = mFragmentStatisticsView.findViewById(R.id.scale_chevron);
        mExposureScaleLayout = mFragmentStatisticsView.findViewById(R.id.layout_scale);

        mCursorImage = mFragmentStatisticsView.findViewById(R.id.scale_cursor);
        View exposureScaleChevronTapView = mFragmentStatisticsView.findViewById(R.id.scale_chevron_tap_view);
        mScaleGroup = mFragmentStatisticsView.findViewById(R.id.scale_group);

        mConstraintSetScaleNew = new ConstraintSet();
        mConstraintSetScaleNew.clone(mExposureScaleLayout);

        // manage the action when we touch the chevron to show/hide the exposure scale
        exposureScaleChevronTapView.setOnClickListener(v -> {
            // toggle based on Tools.getShowExposureScale()
            if (Tools.getShowExposureScale()) {
                Tools.setShowExposureScale(false);
                hideExposureScale();
                // rotate the chevron back to 0 degrees
                HomeFragment.rotateImageWithAnimation(mChevronExposureScaleImage, 90, 0, true);
                Log.d(TAG, "onClick: Tools.getShowExposureScale() -> false");
            } else {
                Tools.setShowExposureScale(true);
                showExposureScale();
                // animate the cursor from the beginning of the layout
                HomeFragment.animateCursorToDbm(Tools.milliWattToDbm(dayAverageMwValue),
                        false, mCursorImage, mContext, mExposureScaleWidth,
                        StatisticsFragment.this, mFragmentStatisticsView);

                // animate rotation of the chevron by 90 degrees
                HomeFragment.rotateImageWithAnimation(mChevronExposureScaleImage, 0, 90, true);
                Log.d(TAG, "onClick: Tools.getShowExposureScale() -> true");
            }
            mConstraintSetScaleNew.applyTo(mExposureScaleLayout); // set new constraints
        });

        mTextViewTop5Title = mFragmentStatisticsView.findViewById(R.id.layout_top_5_sources_title);

        mTextViewNbSourcesTitle = mFragmentStatisticsView.findViewById(R.id.layout_nb_sources_title);
        mTextViewNbSourceWifi = mFragmentStatisticsView.findViewById(R.id.layout_nb_sources_wifi);
        mTextViewNbSourceBluetooth = mFragmentStatisticsView.findViewById(R.id.layout_nb_sources_bluetooth);
        mTextViewNbSourceCellular = mFragmentStatisticsView.findViewById(R.id.layout_nb_sources_cellular);

        mTextViewKindOfSourcesTitle = mFragmentStatisticsView.findViewById(R.id.layout_sources_type_title);

        mNoDataMessage = mFragmentStatisticsView.findViewById(R.id.statistics_no_data);

        mMyOnGlobalLayoutListener = new MyOnGlobalLayoutListener();
        mFragmentStatisticsView.getViewTreeObserver().addOnGlobalLayoutListener(mMyOnGlobalLayoutListener);

        mImageViewExposureDiff = mFragmentStatisticsView.findViewById(R.id.imageViewExposureDiff);
        mTextViewExposureDiff = mFragmentStatisticsView.findViewById(R.id.textViewExposureValueDiff);
        mConstraintLayoutSourcesTypeDetails = mFragmentStatisticsView.findViewById(R.id.layout_sources_type_details);
        mConstraintLayoutTop5Sources = mFragmentStatisticsView.findViewById(R.id.layout_top_5_sources);

        return mFragmentStatisticsView;
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause()");

        DbRequestHandler.dumpEventToDatabase(Const.EVENT_STATISTICS_FRAGMENT_ON_PAUSE);

        if (DbHistoryAsyncTaskRequest.backgroundTask != null) {
            Log.d(TAG, "onPause: Cancelling background task");
            DbHistoryAsyncTaskRequest.backgroundTask.cancel(true);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "in onResume()");

        boolean isStatAvailable = true;
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_STATISTICS_FRAGMENT_ON_RESUME);

        // We compute the correct mStatDay. We use the following strategy.
        mStatDay = null;

        StatisticsFragmentState cachedState = MainActivity.sStatisticsFragmentState;
        Bundle args = getArguments();
        /*
        a) If we come from the ExposureDetailsFragment or the DailyStatSummaryFragment,
        the fragment bundle contains the date the StatisticsFragment must load.

        If this date is outdated (tested with !cachedState.hasBeenCachedToday()), we reset
        both the cache and the mStatDay. For the user, onBackPressed, he will be redirected
        to the latest statistic (of yesterday). This is the most relevant to show is you
        stayed on pause for a long time on the ExposureDetailsFragment.
        */
        if (args != null) {
            // Const.DAILY_STAT_SUMMARY_TO_STATISTICS_IS_STAT_AVAILABLE_ARG_KEY is not set
            // when coming back from the exposure details fragment. So we must test for nullability
            Serializable isStatAvailableSerializable = args.getSerializable(
                    Const.DAILY_STAT_SUMMARY_TO_STATISTICS_IS_STAT_AVAILABLE_ARG_KEY);

            if (isStatAvailableSerializable != null) {
                isStatAvailable = (boolean) isStatAvailableSerializable;
            }

            // If we come from the ExposureDetailsFragment, an arg will be set. It will be null
            // otherwise
            mStatDay = (Calendar) args.getSerializable(Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_STAT_DAY_ARG_KEY);
            Log.d(TAG, "onResume: mStatDay - we come from exposure details date is " + ((mStatDay != null) ? mStatDay.getTime() : "null"));
            // If the arg is null, we get the arg from the daily stat summary
            if (mStatDay == null) {
                Log.d(TAG, "onResume: MAIN_ACTIVITY_EXPOSURE_DETAILS_STAT_DAY_ARG_KEY is null, so we probably came from DailyStatSummaryFragment");
                mStatDay = (Calendar) args.getSerializable(Const.DAILY_STAT_SUMMARY_TO_STATISTICS_STAT_DAY_ARG_KEY);
                Log.d(TAG, "onResume: mStatDay - we come from the daily stat summary date is " + ((mStatDay != null) ? mStatDay.getTime() : "null"));
            }

            // the bundle has been consumed, we clear it.
            args.clear();
        }

        /*
        b) If mStatDay is still null, but there are valid cached state (that is state computed the
        current day), we retrieve the cached mStatDay and use this one.

        If there is no valid cache, we set mStatDay to its default value (that is, one day
        before now)
        */
        if (mStatDay == null) {
            if (cachedState != null && cachedState.hasBeenCachedToday()) {
                mStatDay = cachedState.getStatDay();
                Log.d(TAG, "onResume: mStatDay is null, we get the cached value cachedState.getStatDay(): " + cachedState.getStatDay().getTime() + " state.hasBeenCachedToday()" + cachedState.hasBeenCachedToday());
            } else {
                // we retrieve one day before now starting at midnight
                Calendar oneDayBeforeNow = Calendar.getInstance();
                oneDayBeforeNow.add(Calendar.DAY_OF_MONTH, -1);
                Tools.roundCalendarAtMidnight(oneDayBeforeNow);
                mStatDay = oneDayBeforeNow;
                Log.d(TAG, "onResume: mStatDay is null, no valid cache, we get the default oneDayBeforeNow value: " + mStatDay.getTime());
            }
        }

        if (isStatAvailable) {
            loadStatisticsAndBuildUI(mStatDay);
        } else {
            noDataMessage(mStatDay);
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        // In case onPause is called before the onGlobalLayout() is called, the listener will
        // not be correctly unregistered from itself. This is why we unregister it from onPause.
        // Note that unregistering the listener if is has already been unregistered is safe.
        mFragmentStatisticsView.getViewTreeObserver().removeOnGlobalLayoutListener(mMyOnGlobalLayoutListener);
    }

    private void hideExposureScale() {
        // Setting visibility to components like chevron and exposure scale
        // along with the others does not work because they contain animation.
        // As a work around, we clear animation onPreExecute and then call hide.
        mChevronExposureScaleImage.clearAnimation();
        mExposureScaleLayout.clearAnimation();

        mScaleGroup.setVisibility(View.GONE);
        // connect divider to the bottom of scale chevron
        mConstraintSetScaleNew.connect(R.id.divider_statistics_scale_top5_sources, ConstraintSet.TOP, R.id.textViewRecommendation, ConstraintSet.BOTTOM, 0);
    }

    private void showExposureScale() {
        mScaleGroup.setVisibility(View.VISIBLE);
        // connect divider to the bottom of exposure scale layout
        mConstraintSetScaleNew.connect(R.id.divider_statistics_scale_top5_sources, ConstraintSet.TOP, R.id.layout_scale, ConstraintSet.BOTTOM, 0);
    }

    /**
     * Display a no data message on day
     *
     * @param day the day which there is no statistics
     */
    private void noDataMessage(Calendar day) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                getString(R.string.statistics_title_date_format),
                Tools.getDefaultDeviceLocale());
        mNoDataMessage.setText(String.format(
                getString(R.string.statistics_no_data),
                simpleDateFormat.format(day.getTime())));
    }

    @Override
    public boolean onBackPressed() {
        mActivity.onBottomNavigateTo(R.id.bottom_nav_statistics);
        return true;
    }

    /**
     * Given a {@link java.util.HashSet} returns the number of valid signals in the set
     *
     * @param signals a HashSet of BaseProperty signals
     * @return number of valid signals in the HashSet
     */
    private int getValidSignalsCount(HashSet<BaseProperty> signals) {
        int count = 0;
        for (BaseProperty signal : signals) {
            if (signal.isValidSignal) {
                count++;
            }
        }
        return count;
    }

    class MyOnGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {

        @Override
        public void onGlobalLayout() {
            Log.d(TAG, "in onGlobalLayout()");

            Log.d(TAG, "onGlobalLayout: Listener observer removed");
            mFragmentStatisticsView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            // If onPause is called before this callback is called (it is possible if ever we
            // go onPause before the rendering is complete, then the fragment is no more
            // linked to an Activity and the method getResources() will return an
            // IllegalStateException. We can test for that with isAdded()
            if (isAdded()) {
                int marginInPixels = (int) getResources().getDimension(R.dimen._exposure_scale_marginLeftRight);
                // the total width of the exposure scale bar in pixels
                // we multiply by 2 to take into account the left and right margins
                mExposureScaleWidth = mFragmentStatisticsView.getWidth() - 2 * marginInPixels;
                // we call animateCursorToDbm as soon as we have the correct View width
                HomeFragment.animateCursorToDbm(Tools.milliWattToDbm(dayAverageMwValue),
                        false, mCursorImage, mContext, mExposureScaleWidth,
                        StatisticsFragment.this, mFragmentStatisticsView);
                Log.d(TAG, "onGlobalLayout: mExposureScaleWidth " + mExposureScaleWidth);
            }
        }
    }

}

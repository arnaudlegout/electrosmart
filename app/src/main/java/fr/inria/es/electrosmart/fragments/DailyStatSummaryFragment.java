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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DailyStatSummary;
import fr.inria.es.electrosmart.database.DbRequestHandler;


public class DailyStatSummaryFragment extends MainActivityFragment {
    private static final String TAG = "DailyStatSummaryFrag";
    private Context mContext;
    private MainActivity mActivity;
    private ConstraintLayout mLayoutDailyStatSummary;
    private AppCompatTextView mNoDataMessage;
    private ProgressBar mProgressBar;
    private ComputeTop5SignalsAndDailyStatSummaryOnYesterdayAsyncTask task;

    @Override
    public void onAttach(@NonNull Context context) {
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
        View mFragmentStatisticsView = inflater.inflate(R.layout.fragment_daily_stat_summary, container, false);
        mLayoutDailyStatSummary = mFragmentStatisticsView.findViewById(R.id.layout_daily_stat_summary);
        mNoDataMessage = mFragmentStatisticsView.findViewById(R.id.statistics_no_data);

        // progress bar to display when the data is loading.
        mProgressBar = mFragmentStatisticsView.findViewById(R.id.progressbar_loading);
        return mFragmentStatisticsView;
    }

    /**
     * create all elements to display in the fragment
     */
    private void loadUI() {

        long oldestDailyStatTimestampInDB = DbRequestHandler.getOldestDailyStatTimeStampDB();
        if (oldestDailyStatTimestampInDB == 0) {
            Log.d(TAG, "loadUI: No Data");
            mNoDataMessage.setText(R.string.statistics_no_data_first_day);
        } else {
            mNoDataMessage.setVisibility(View.GONE);
            // 1. Get the number of days between yesterday and the oldest timestamp in the top5signals table
            Calendar yesterdayCal = new GregorianCalendar();
            yesterdayCal.add(Calendar.DATE, -1);
            Log.d(TAG, "loadUI: oldestDailyStatTimestampInDB = " + new Date(oldestDailyStatTimestampInDB));
            Log.d(TAG, "loadUI: yesterdayCal = " + yesterdayCal.getTime());

            // we compute the number of days for which we have a daily stat summary.
            // It is the number of days between the oldestDailyStatTimestampInDB and yesterday +
            // 1 day, because the number of days between two dates does not count the first day.
            int numberOfDays = Tools.getNumberOfDaysBetweenTimestamps(oldestDailyStatTimestampInDB,
                    yesterdayCal.getTimeInMillis()) + 1;

            // 2. Create the list of daily stat views for each of the days
            View[] allDailyStats = new View[numberOfDays];
            Log.d(TAG, "loadUI: number of days = " + numberOfDays);
            SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.daily_stat_summary_date_format), Tools.getDefaultDeviceLocale());
            // we remove all existing views in the constraint layout to avoid an inconsistent layout
            mLayoutDailyStatSummary.removeAllViews();
            Calendar thisDayCal = new GregorianCalendar();
            thisDayCal.setTimeInMillis(oldestDailyStatTimestampInDB);
            for (int day = 0; day < numberOfDays; day++) {
                // Get the daily stats of thisDayCal date and set the corresponding text/image views
                Log.d(TAG, "loadUI: thisDayCal: " + thisDayCal.getTime());
                final DailyStatSummary dailyStatSummary = DbRequestHandler.getDailyStatSummaryOfDay(thisDayCal);
                Log.d(TAG, "loadUI: day = " + day);
                allDailyStats[day] = getLayoutInflater()
                        .inflate(R.layout._daily_stat_summary_row, mLayoutDailyStatSummary, false);
                allDailyStats[day].setId(ViewCompat.generateViewId());
                AppCompatTextView textViewDate = allDailyStats[day].findViewById(R.id.text_view_date);
                AppCompatTextView textViewDailyScore = allDailyStats[day].findViewById(R.id.text_view_daily_score);
                AppCompatImageView imageViewNewSource = allDailyStats[day].findViewById(R.id.new_source);
                View expositionDot = allDailyStats[day].findViewById(R.id.exposition_dot);
                textViewDate.setText(Tools.capitalize(sdf.format(thisDayCal.getTime())));

                AppCompatTextView textViewDailyScoreUnit = allDailyStats[day].findViewById(R.id.text_view_daily_score_unit);

                // We display the score based on users preferred units
                String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
                if (dailyStatSummary.getDbm() < Const.MIN_DBM_FOR_ROOT_SCORE) {
                    textViewDailyScore.setText(R.string.na);
                    textViewDailyScoreUnit.setVisibility(View.GONE);
                } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
                    textViewDailyScore.setText(String.valueOf(Tools.getExposureScore(dailyStatSummary.getDbm())));
                    textViewDailyScoreUnit.setVisibility(View.VISIBLE);
                } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_DBM_METRIC))) {
                    textViewDailyScore.setText(String.format("%s %s", String.valueOf(dailyStatSummary.getDbm()),
                            MainApplication.getContext().getString(R.string.dbm_metric)));
                    textViewDailyScoreUnit.setVisibility(View.GONE);
                } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_POWER_METRIC))) {
                    String[] watt = Tools.dBmToWattWithSIPrefixArray(dailyStatSummary.getDbm(), false);
                    textViewDailyScore.setText(String.format("%s %s", watt[0], watt[1]));
                    textViewDailyScoreUnit.setVisibility(View.GONE);
                }

                // Set the basic recommendation based on dbm
                if (dailyStatSummary.getDbm() < Const.MIN_DBM_FOR_ROOT_SCORE) {
                    expositionDot.setVisibility(View.INVISIBLE);
                } else {
                    expositionDot.setVisibility(View.VISIBLE);
                    Tools.setRecommendationDotBasedOnDbm(mContext, dailyStatSummary.getDbm(), expositionDot);
                }


                // Set the NEW image if we found new sources on this day
                if (dailyStatSummary.getNumberOfNewSources() > 0) {
                    Log.d(TAG, "loadUI: We found new sources - count = " + dailyStatSummary.getNumberOfNewSources());
                    imageViewNewSource.setVisibility(View.VISIBLE);
                } else {
                    imageViewNewSource.setVisibility(View.INVISIBLE);
                }

                // When each of the views is clicked, we open the StatisticsFragment of the
                // corresponding date
                allDailyStats[day].setOnClickListener(view -> {
                    Log.d(TAG, "onClick: ");
                    Calendar dateToShow = new GregorianCalendar();
                    dateToShow.setTimeInMillis(dailyStatSummary.getSummaryDateTimeMillis());
                    mActivity.showStatisticsFragment(dateToShow,
                            Const.MIN_DBM_FOR_ROOT_SCORE <= dailyStatSummary.getDbm());
                });
                mLayoutDailyStatSummary.addView(allDailyStats[day]);
                thisDayCal.add(Calendar.DAY_OF_MONTH, 1);
            }


            // 3. Set the constraints to the newly constructed views
            ConstraintSet constraintSetDailySource = new ConstraintSet();
            constraintSetDailySource.clone(mLayoutDailyStatSummary);

            int dayCount = 0;

            for (View dayStat : allDailyStats) {
                // constrain horizontally to the start of the parent
                constraintSetDailySource.connect(
                        dayStat.getId(), ConstraintSet.START,
                        mLayoutDailyStatSummary.getId(), ConstraintSet.START, 0);

                // We connect the topmost signal to the top of the layout, and the
                // following signals to ones above them
                if (dayCount == 0) {
                    constraintSetDailySource.connect(
                            dayStat.getId(), ConstraintSet.BOTTOM,
                            mLayoutDailyStatSummary.getId(), ConstraintSet.BOTTOM, 0);
                } else {
                    constraintSetDailySource.connect(
                            dayStat.getId(), ConstraintSet.BOTTOM,
                            allDailyStats[dayCount - 1].getId(), ConstraintSet.TOP, 0);
                }
                dayCount++;
            }

            constraintSetDailySource.applyTo(mLayoutDailyStatSummary); // set new constraints
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause()");
        if (task != null) {
            task.cancel(true);
        }
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_DAILY_STAT_SUMMARY_FRAGMENT_ON_PAUSE);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "in onResume()");

        task = new ComputeTop5SignalsAndDailyStatSummaryOnYesterdayAsyncTask();
        task.execute();

        DbRequestHandler.dumpEventToDatabase(Const.EVENT_DAILY_STAT_SUMMARY_FRAGMENT_ON_RESUME);
    }

    @Override
    public boolean onBackPressed() {
        mActivity.onBottomNavigateTo(R.id.bottom_nav_home);
        return true;
    }

    /**
     * This AsyncTask is used to compute the top 5 signals and daily summary in the background
     * without bloking the UI.
     * <p>
     * Note: Lint warns for a possible memory leak. There is no leak in practice as this AsyncTask
     * is short lived and will release fast the refence to this fragment.
     */
    private class ComputeTop5SignalsAndDailyStatSummaryOnYesterdayAsyncTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... obj) {
            DbRequestHandler.computeTop5SignalsAndDailyStatSummaryOnYesterday();
            return null;
        }

        protected void onPreExecute() {
            Log.d(TAG, "ComputeTop5SignalsAndDailyStatSummaryOnYesterdayAsyncTask onPreExecute: ");
            mProgressBar.setVisibility(View.VISIBLE);
        }

        protected void onPostExecute(Void obj) {
            Log.d(TAG, "ComputeTop5SignalsAndDailyStatSummaryOnYesterdayAsyncTask onPostExecute: ");
            mProgressBar.setVisibility(View.INVISIBLE);
            loadUI();
        }

        protected void onCancelled(Void obj) {
            // This method is called instead of onPostExecute() when we call .cancel() on
            // the AsyncTask. Therefore, it prevents crashes due to a call to loadUI(), whereas
            // the fragment does not exist anymore.
            Log.d(TAG, "ComputeTop5SignalsAndDailyStatSummaryOnYesterdayAsyncTask onCancelled()");
        }
    }

}

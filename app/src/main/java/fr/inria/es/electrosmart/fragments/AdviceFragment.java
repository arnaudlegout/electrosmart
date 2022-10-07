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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;


public class AdviceFragment extends MainActivityFragment {
    private static final String TAG = "AdviceFragment";
    private AppCompatTextView mAdviceDescriptionTextView, mAdviceReadArticleTextView;
    private Context mContext;
    private MainActivity mActivity;
    private MeasurementFragment.AntennaDisplay mSelectedAntennaDisplay;
    private int mDbmForAdvice;
    private String mTopExposingSource;
    private int mSourcesTotalContribution;
    private SignalsSlot mSignalsSlot;
    private View mLayoutPowerOff;
    private TextView mAdviceReduceExposure;
    private AppCompatImageView mImageAdvice;

    private View mIWantToReduceMyExposureLayout, mHowToManageThisSource;

    /**
     * Creates a new instance of the fragment to which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static MainActivityFragment newInstance(Bundle arguments) {
        MainActivityFragment fragment = new AdviceFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Instrument
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_ADVICE_FRAGMENT_ON_RESUME);

        // hide power off advice for cellular
        if (mSelectedAntennaDisplay == MeasurementFragment.AntennaDisplay.CELLULAR) {
            mLayoutPowerOff.setVisibility(View.GONE);
        } else {
            mLayoutPowerOff.setVisibility(View.VISIBLE);
        }

        Tools.SignalLevel level = Tools.dbmToLevel(mDbmForAdvice);
        if (level == Tools.SignalLevel.HIGH) {
            // first tile
            mAdviceDescriptionTextView.setText(R.string.advice_high_description);
            mAdviceReadArticleTextView.setVisibility(View.VISIBLE);
            mImageAdvice.setImageResource(R.drawable.ic_error_outline_black_24dp);

            // second tile
            mIWantToReduceMyExposureLayout.setVisibility(View.VISIBLE);

            //third tile
            mHowToManageThisSource.setVisibility(View.VISIBLE);
        } else if (level == Tools.SignalLevel.MODERATE) {
            // first tile
            mAdviceDescriptionTextView.setText(R.string.advice_moderate_description);
            mAdviceReadArticleTextView.setVisibility(View.VISIBLE);
            mImageAdvice.setImageResource(R.drawable.ic_info_outline_black_24dp);

            // second tile
            mIWantToReduceMyExposureLayout.setVisibility(View.VISIBLE);

            // third tile
            mHowToManageThisSource.setVisibility(View.VISIBLE);
        } else {
            // first tile
            mAdviceDescriptionTextView.setText(R.string.advice_low_description);
            mAdviceReadArticleTextView.setVisibility(View.VISIBLE);
            mImageAdvice.setImageResource(R.drawable.ic_info_outline_black_24dp);

            // When the exposure is low, we do not show mIWantToReduceMyExposureLayout (second tile)
            // and mHowToManageThisSource (third tile)
            // second tile
            mIWantToReduceMyExposureLayout.setVisibility(View.GONE);

            // third tile
            mHowToManageThisSource.setVisibility(View.GONE);
        }

        // we personalized the text for the advice with the current highest source
        String detail = String.format(getString(R.string.advice_reduce_exposure),
                MeasurementFragment.AntennaDisplayToString(mContext, mSelectedAntennaDisplay),
                mTopExposingSource,
                String.valueOf(mSourcesTotalContribution));
        Tools.setHtmlTextToTextView(mAdviceReduceExposure, detail);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause()");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_ADVICE_FRAGMENT_ON_PAUSE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach: ");
        mContext = context;
        mActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        mSignalsSlot = MainActivity.stateAdviceFragmentSignalSlot;

        // We retrieve the SignalSlot topSignal characteristics to populate the fragment
        if (mSignalsSlot != null && mSignalsSlot.containsValidSignals()) {
            BaseProperty topSignal = mSignalsSlot.getTopSignal();
            if (topSignal != null) {
                mDbmForAdvice = topSignal.dbm;
                mSelectedAntennaDisplay = topSignal.getAntennaDisplay();
                mTopExposingSource = topSignal.friendlySourceName(mContext).toString();
                /*
                we take the floor so that we never get 100% due to rounding error, we use
                also signalsSlot.getSlotCumulativeTotalMwValue() instead of
                signalsSlot.getSlotCumulativeTotalDbmValue() to avoid a rounding error due to the
                int dbm value
                 */
                mSourcesTotalContribution = (int) Math.floor((Tools.dbmToMilliWatt(topSignal.dbm) /
                        mSignalsSlot.getSlotCumulativeTotalMwValue()) * 100);
            } else {
                // we fall back to the Home Fragment if the top signal is null
                Log.d(TAG, "onCreateView: topSignal is null starting the HomeFragment");
                mActivity.onBottomNavigateTo(R.id.bottom_nav_home);

            }
        } else {
            // we fall back to the Home Fragment if the SignalSlot is null or invalid
            Log.d(TAG, "onCreateView: mSignalSlot is null starting the HomeFragment");
            mActivity.onBottomNavigateTo(R.id.bottom_nav_home);
        }

        View fragmentAdviceView = inflater.inflate(R.layout.fragment_advice, container, false);
        View layoutExposureRecommendation = fragmentAdviceView.findViewById(R.id.layout_exposure_recommendation);

        // first tile
        mAdviceDescriptionTextView = layoutExposureRecommendation.findViewById(R.id.text_view_advice_description);
        mAdviceReadArticleTextView = layoutExposureRecommendation.findViewById(R.id.link_advice_read_article);
        mAdviceReadArticleTextView.setOnClickListener(v -> mActivity.showArticleWebViewFragment(mSignalsSlot));
        mImageAdvice = fragmentAdviceView.findViewById(R.id.image_advice);

        // second tile
        mHowToManageThisSource = fragmentAdviceView.findViewById(R.id.how_to_manage_this_source_layout);
        mAdviceReduceExposure = fragmentAdviceView.findViewById(R.id.advice_reduce_exposure);
        View recommendationDot = fragmentAdviceView.findViewById(R.id.advice_recommendation_dot);
        Tools.setRecommendationDotBasedOnDbm(mContext, mDbmForAdvice, recommendationDot);

        mIWantToReduceMyExposureLayout = fragmentAdviceView.findViewById(R.id.advice_i_want_to_reduce_my_exposure_layout);
        mIWantToReduceMyExposureLayout.setOnClickListener(v -> {
            Log.d(TAG, "onClick: mIWantToReduceMyExposureLayout clicked");
            mActivity.showExposureDetails(mSelectedAntennaDisplay, mSignalsSlot, 0, null, null);
        });

        // third tile
        mLayoutPowerOff = fragmentAdviceView.findViewById(R.id.layout_power_off);
        AppCompatTextView textViewAdvicePowerOffDescription = mLayoutPowerOff.findViewById(R.id.text_view_advice_power_off_description);
        String power_off_text = String.format(getString(R.string.advice_poweroff_description),
                String.valueOf(mSourcesTotalContribution));
        Tools.setHtmlTextToTextView(textViewAdvicePowerOffDescription, power_off_text);

        View layoutIsolation = fragmentAdviceView.findViewById(R.id.layout_isolation);
        AppCompatTextView textViewLinkRecommendASolution = layoutIsolation.findViewById(R.id.link_recommend_me_a_solution);
        textViewLinkRecommendASolution.setOnClickListener(v -> mActivity.showSolutionFragment());

        return fragmentAdviceView;
    }

    @Override
    public boolean onBackPressed() {
        mActivity.onBottomNavigateTo(R.id.bottom_nav_home);
        return true;
    }

}

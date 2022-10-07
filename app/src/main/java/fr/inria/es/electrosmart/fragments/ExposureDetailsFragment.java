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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Calendar;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;
import fr.inria.es.electrosmart.ui.CustomExpandableListAdapter;
import fr.inria.es.electrosmart.ui.HeaderExpandableListAdapter;

public class ExposureDetailsFragment extends MainActivityFragment {
    private static final String TAG = "ExposureDetailsFrag";
    // define the number of child views that can be scrolled before we pause the updates.
    // Note that the header of the child views is considered as one view
    private static final int PAUSE_UPDATE_THRESHOLD = 1;
    // when isGroupViewExpanded is true, a child view is displayed to expand a tile
    private static boolean isGroupViewExpanded;
    // Set to true when the child view are scrolled more than PAUSE_UPDATE_THRESHOLD.
    private boolean isScrollPaused = false;
    private Toast toast;
    private ExpandableListView list;
    private MeasurementFragment.AntennaDisplay lastExpandedGroupViewAntennaDisplay;
    private Calendar firstDay;
    private SignalsSlot mSignalsSlot;
    // give the number (starting at zero for the group view) of the first visible group view at the
    // top of the screen
    private int firstVisibleGroupView = 0;
    private MainActivity mActivity;
    private Context mContext;
    private final BroadcastReceiver mWindowFocusChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Tools.setGroupIconIndicatorToRight(mActivity, list);
            // only if the ExposureDetailsFragment is updated in real time we check for the
            // permissions. Real time updates happen with mSignalsSlot does not contain valid signals.
            if (mSignalsSlot != null && !mSignalsSlot.containsValidSignals()) {
                checkAndShowErrorIfExists(mContext, mActivity);
            }
        }
    };
    private int mSignalIndex;
    // The broadcast receiver that should be executed whenever the live timeline is updated and
    // we are in live mode. It is its responsibility is to update the user interface accordingly.
    private final BroadcastReceiver mLiveTimelineUpdatedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "in mLiveTimelineUpdatedBroadcastReceiver.onReceive() ");
            // We retrieve the last SignalsSlot in the timeline and update the layout
            updateLayout(mActivity.mLiveTimeline);
        }
    };


    /**
     * Creates a new instance of the fragment to which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static MainActivityFragment newInstance(Bundle arguments) {
        MainActivityFragment fragment = new ExposureDetailsFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * Define the action when we leave the current fragment.
     */
    private void backAction() {
        if (firstDay != null) {
            // we came to Exposure Details from Statistics, so get back to Statistics
            mActivity.showStatisticsFragment(mSignalsSlot, firstDay);
        } else {
            if (mSignalsSlot != null && mSignalsSlot.containsValidSignals()) {
                // We came to Exposure details from AdviceFragment, so go back to AdviceFragment
                mActivity.showAdviceFragment(mSignalsSlot);
            } else {
                // if the SignalsSlot is null or invalid we fallback to the HomeFragment
                mActivity.onBottomNavigateTo(R.id.bottom_nav_home);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exposure_details, container, false);
        list = view.findViewById(R.id.list);

        // NOTE: this fragment is created with newInstance() that is always called with a Bundle
        //       containing arguments.

        // we expand the groupView passed by the main activity
        lastExpandedGroupViewAntennaDisplay =
                (MeasurementFragment.AntennaDisplay) getArguments().getSerializable(
                        Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_ANTENNA_DISPLAY_ARG_KEY);

        // we obtain the first day parameter which is set when exposure details is instantiated from
        // the StatisticsFragment
        firstDay = (Calendar) getArguments().getSerializable(
                Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_STAT_DAY_ARG_KEY);

        // we obtain
        mSignalIndex = (int) getArguments().getSerializable(
                Const.MAIN_ACTIVITY_EXPOSURE_DETAILS_SIGNAL_INDEX_ARG_KEY);

        mSignalsSlot = MainActivity.stateAdviceFragmentSignalSlot;


        /*
          called each time a group view is touched. It manages the updates of the group views.
         */
        list.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            Log.d(TAG, "onCreate: setOnGroupClickListener");
            // as we are overriding the default click behaviour of the group, it is our
            // responsibility to play sound for haptic feedback
            v.playSoundEffect(SoundEffectConstants.CLICK);
            backAction();
            return true;
        });

        // We do not want to have scroll listener if we came from AdviceFragment
        if (mSignalsSlot != null && !mSignalsSlot.containsValidSignals()) {
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
                        Log.d(TAG, "onScroll: isScrollPaused " + isScrollPaused);

                        if (firstVisibleGroupView > PAUSE_UPDATE_THRESHOLD) {
                            if (!isScrollPaused) {
                                if (toast != null) {
                                    toast.cancel();
                                }
                                toast = Toast.makeText(mContext, R.string.ExposureDetailsToastPaused, Toast.LENGTH_LONG);
                                toast.show();
                                // onScroll is called at each scroll event, so very often. We only
                                // display the toast once when pass the PAUSE_UPDATE_THRESHOLD
                                isScrollPaused = true;
                            }
                        } else {
                            if (isScrollPaused) {
                                if (toast != null) {
                                    toast.cancel();
                                }
                                toast = Toast.makeText(mContext, R.string.ExposureDetailsToastResumed, Toast.LENGTH_LONG);
                                toast.show();
                                isScrollPaused = false;
                            }
                        }
                    }
                }
            });
        }

        ConstraintLayout expositionDetailsFragmentLayout = view.findViewById(R.id.fragment_exposition_details_layout);

        // Make sure we readjust the icon indicators position if the screen changes it's size.
        // This can happen when we use the split-screen feature.
        expositionDetailsFragmentLayout.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                                   oldLeft, oldTop, oldRight,
                                                                   oldBottom) ->
                Tools.setGroupIconIndicatorToRight(mActivity, list));

        updateLayout(mActivity.mLiveTimeline);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Registering");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_EXPOSURE_DETAILS_FRAGMENT_ON_RESUME);

        // onResume we reset the state to collapsed group views
        isScrollPaused = false;
        firstVisibleGroupView = 0;

        isGroupViewExpanded = true;

        // only if the ExposureDetailsFragment is updated in real time we check for the
        // permissions. Real time updates happens with mSignalsSlot does not contain valid signals.
        if (mSignalsSlot != null && !mSignalsSlot.containsValidSignals()) {
            checkAndShowErrorIfExists(mContext, mActivity);
        }

        // update layout if we have data in the timeline
        updateLayout(mActivity.mLiveTimeline);

        // register the broadcast listeners
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);

        // We do not want to listen to updates if we came from the AdviceFragment
        if (mSignalsSlot != null && !mSignalsSlot.containsValidSignals()) {
            localBroadcastManager.registerReceiver(mLiveTimelineUpdatedBroadcastReceiver,
                    new IntentFilter(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION));
        }
        localBroadcastManager.registerReceiver(mWindowFocusChangedBroadcastReceiver,
                new IntentFilter(Const.MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: unregistering");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_EXPOSURE_DETAILS_FRAGMENT_ON_PAUSE);

        // unregister the broadcast listeners
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        // We do not want to have listen to updates if we came from AdviceFragment
        if (mSignalsSlot != null && !mSignalsSlot.containsValidSignals()) {
            localBroadcastManager.unregisterReceiver(mLiveTimelineUpdatedBroadcastReceiver);
        }
        localBroadcastManager.unregisterReceiver(mWindowFocusChangedBroadcastReceiver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach: ");
        mContext = context;
        mActivity = (MainActivity) getActivity();
    }

    @Override
    public boolean onBackPressed() {
        backAction();
        return true;
    }

    private void updateLayout(Timeline timeline) {
        Log.d(TAG, "in updateLayout()");
        Log.d(TAG, "isScrollPaused " + isScrollPaused);

        SignalsSlot signalsSlot = null;
        boolean highlightTopSignal;
        // if we come here from AdviceFragment (implied by a passed signalsSlotIndex), we update the
        // layout with the passed signalsSlotIndex explicitly
        if (mSignalsSlot != null && mSignalsSlot.containsValidSignals()) {
            highlightTopSignal = true;
            signalsSlot = mSignalsSlot;
        } else {
            // if the child views are scrolled down, we pause the updates
            if (firstVisibleGroupView > PAUSE_UPDATE_THRESHOLD) {
                Log.d(TAG, "updateLayout: " + firstVisibleGroupView + " we pause the updates");
                return;
            }

            // if the timeline contains some slots we get the latest one, otherwise, we just exit.
            if (!timeline.isEmpty()) {
                signalsSlot = timeline.get(timeline.size() - 1);
            }
            highlightTopSignal = false;
        }
        updateLayoutWithTheGivenSignalsSlot(signalsSlot, highlightTopSignal, mSignalIndex);
    }

    private void updateLayoutWithTheGivenSignalsSlot(SignalsSlot signalsSlot, boolean highlightTopSignal, int signalIndex) {
        CustomExpandableListAdapter listAdapter;
        // noSignal is set to true when there is no top signal to display. In that case, we
        // display the no signal value, and we remove the group views
        boolean noSignal = false;

        if (signalsSlot != null && signalsSlot.containsValidSignals()) {
            Log.d(TAG, "updateLayout: signalsSlot contains valid signals");

            if (isGroupViewExpanded) {
                Log.d(TAG, "updateLayout: isGroupViewExpanded is true");
                listAdapter = new HeaderExpandableListAdapter(mContext,
                        lastExpandedGroupViewAntennaDisplay, signalsSlot, highlightTopSignal, signalIndex);
            } else {
                Log.d(TAG, "updateLayout: isGroupViewExpanded is false");
                listAdapter = new HeaderExpandableListAdapter(mContext,
                        null, signalsSlot, false, signalIndex);
            }

            list.setAdapter(listAdapter);
            listAdapter.notifyDataSetChanged();

            // test the case when the signalSlot contains valid signals, but with all invalid dBm
            // values.
            BaseProperty topSignal = signalsSlot.getTopSignal();
            if (topSignal == null) {
                noSignal = true;
            }
        } else {
            noSignal = true;
        }

        if (noSignal) {
            // if topPanelEsProperties is null or empty we remove all group views
            Log.d(TAG, "updateLayout: signalsSlot DOES NOT contain valid signals");
            listAdapter = new HeaderExpandableListAdapter(mContext, null,
                    null, false, signalIndex);
            list.setAdapter(listAdapter);
            listAdapter.notifyDataSetChanged();
        }
    }
}

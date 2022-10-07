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

import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.transition.TransitionManager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.BluetoothProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;

public class HomeFragment extends MainActivityFragment {
    private static final String TAG = "HomeFragment";
    private ViewTreeObserver.OnGlobalLayoutListener mMyOnGlobalLayoutListener;
    private Context mContext;
    private MainActivity mActivity;
    private final BroadcastReceiver mWindowFocusChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkAndShowErrorIfExists(mContext, mActivity);
        }
    };
    private Timeline mTimeline;
    private TextView mExposureValueTextView;
    private TextView mExposureUnit;
    private TextView mBluetoothTextView;
    private TextView mBluetoothOthersTextView;
    private TextView mWiFiTextView;
    private TextView mWiFiTextViewOthers;
    private TextView mCellularTextView;
    private TextView mCellularTextViewOthers;
    private TextView mRecommendationTextView;
    private View mRecommendationDot, mBluetoothDot, mWiFiDot, mCellularDot;
    private View mFragmentHomeView;
    private Group mScaleGroup;
    private ConstraintLayout mBottomConstraintLayout;
    private ConstraintSet mConstraintSetNew;
    private View mBluetoothSourceDivider, mWiFiSourceDivider, mCellularSourceDivider;
    private AppCompatImageView mImageBluetoothConnected, mImageWiFiConnected, mImageCellularConnected;
    // the moving component on top of the exposure scale
    private AppCompatImageView mCursorImage;
    // a variable to hold the current dbm value being shown to the user
    private int mCurrentDbm;
    // the layout representing the exposure scale
    private ConstraintLayout mExposureScaleLayout;
    // the new constraint set that needs to be set on the exposure scale layout
    private ConstraintSet mConstraintSetScaleNew;
    // the chevron image next to the recommendation text, that opens the exposure scale
    private AppCompatImageView mChevronExposureScaleImage;
    // the text views that show the values for low, medium and high limits
    private AppCompatTextView mScaleLowStart, mScaleMediumStart, mScaleMediumEnd, mScaleHighEnd;
    // the view spread across the exposure value and that handles the exposure scale chevron tap
    private View mExposureScaleChevronTapView;
    private int mExposureScaleWidth;
    // the Advice layout used to make is visible or gone
    private View mLayoutAdvice;
    // The TextView advice to personalized according to the exposure
    private TextView mTextViewAdvice;
    // The image next to the TextView advice
    private AppCompatImageView mImageAdvice;

    private final BroadcastReceiver mLiveTimelineUpdatedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive");
            if (mTimeline.size() > 0) {
                SignalsSlot mSignalsSlot = mTimeline.get(mTimeline.size() - 1);
                updateLayout(mSignalsSlot);
            }
        }
    };

    /**
     * Makes the cursorImage imageview move over the exposure scale in an animated fashion, and
     * set the correct color for the cursor according to the new position
     *
     * @param dbm                the dbm value up to which the cursor needs to be moved
     * @param animate            when true animate smoothly the cursor from its previous position, otherwise
     *                           jump to the position
     * @param cursorImage        The image view to animate
     * @param context            The context in which we make the animation
     * @param exposureScaleWidth The exposure scale width
     * @param fragment           The fragment calling this method
     * @param fragmentView       The view object representing the fragment layout
     */
    static void animateCursorToDbm(int dbm, boolean animate, AppCompatImageView cursorImage,
                                   Context context, int exposureScaleWidth, Fragment fragment,
                                   View fragmentView) {
        Log.d(TAG, "in animateCursorToDbm()");
        Log.d(TAG, "animateCursorToDbm: animate :" + animate);

        // in case the exposure scale has not been set, we try to set it
        if ((exposureScaleWidth == 0) && fragment.isAdded()) {
            Log.d(TAG, "animateCursorToDbm: we compute exposureScaleWidth");
            int marginInPixels = (int) fragment.getResources().getDimension(
                    R.dimen._exposure_scale_marginLeftRight);
            // the total width of the exposure scale bar in pixels
            // we multiply by 2 to take into account the left and right margins
            exposureScaleWidth = fragmentView.getWidth() - 2 * marginInPixels;
            Log.d(TAG, "animateCursorToDbm: we compute exposureScaleWidth: " + exposureScaleWidth);
        }


        // Set the color of the cursorImage according to the dbm
        ImageViewCompat.setImageTintList(cursorImage,
                ColorStateList.valueOf(ContextCompat.getColor(context, Tools.getRecommendationDotColorResourceIdFromDbm(dbm))));

        float position = ((float) Tools.getExposureScore(dbm) / 100f) * (float) exposureScaleWidth;
        Log.d(TAG, "animateCursorToDbm: " + dbm + " -> " + position + ", exposureScaleWidth = " + exposureScaleWidth);
        ObjectAnimator animation;
        if (animate) {
            animation = ObjectAnimator.ofFloat(cursorImage, "translationX", position);
        } else {
            animation = ObjectAnimator.ofFloat(cursorImage, "translationX", position, position);
        }
        animation.setDuration(200);
        animation.start();
    }

    /**
     * Rotates and animate smoothly the rotation of the given image
     *
     * @param image       an imageview that needs to be rotated
     * @param fromDegrees the initial rotation of the image
     * @param toDegrees   the target rotation of the image
     * @param animate     when true animate smoothly the image rotation, otherwise jump to the new
     *                    rotation
     */
    public static void rotateImageWithAnimation(AppCompatImageView image,
                                                int fromDegrees, int toDegrees, boolean animate) {
        RotateAnimation rotate = new RotateAnimation(fromDegrees, toDegrees,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        if (animate) {
            rotate.setDuration(200);
        } else {
            rotate.setDuration(0);
        }
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setFillAfter(true);
        image.startAnimation(rotate);
    }

    /**
     * This method dynamically position the list of signals in the activity. The most powerful
     * signal first.
     *
     * @param index  position of the row vertically. O is the first row and 2 is the last one
     * @param viewId corresponds to textView of each signal row
     */
    private void setTextViewPosition(int index, int viewId) {
        if (index == 0) {
            mConstraintSetNew.connect(viewId, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
            mConstraintSetNew.connect(viewId, ConstraintSet.BOTTOM, R.id.guideline_horizontal_30, ConstraintSet.TOP, 0);
        } else if (index == 1) {
            mConstraintSetNew.connect(viewId, ConstraintSet.TOP, R.id.guideline_horizontal_30, ConstraintSet.TOP, 0);
            mConstraintSetNew.connect(viewId, ConstraintSet.BOTTOM, R.id.guideline_horizontal_60, ConstraintSet.TOP, 0);
        } else if (index == 2) {
            mConstraintSetNew.connect(viewId, ConstraintSet.TOP, R.id.guideline_horizontal_60, ConstraintSet.TOP, 0);
            mConstraintSetNew.connect(viewId, ConstraintSet.BOTTOM, R.id.guideline_horizontal_90, ConstraintSet.TOP, 0);
        }
    }

    /**
     * Update the UI with a given signalsSlot
     *
     * @param signalsSlot the signalSlots to use to update the UI
     */
    private void updateLayout(SignalsSlot signalsSlot) {
        Log.d(TAG, "in updateLayout()");
        // noSignal is set to true when there is no top signal to display. In that case, we
        // display a no source message and we remove the group views
        boolean noSignal = false;

        hideAllGroups();
        if (signalsSlot != null && signalsSlot.containsValidSignals()) {
            Log.d(TAG, "updateLayout: signalsSlot.toString()" + signalsSlot.toString());
            BaseProperty topSignal = signalsSlot.getTopSignal();
            List<BaseProperty> sortedGroupViewSignals = signalsSlot.getSortedGroupViewSignals();

            if (topSignal != null) {
                String mTopSignalSourceName = topSignal.friendlySourceName(mContext).toString();
                int dbm = signalsSlot.getSlotCumulativeTotalDbmValue();
                mCurrentDbm = dbm;

                // Set the advice according to the dbm levels
                Tools.SignalLevel level = Tools.dbmToLevel(mCurrentDbm);
                if (level == Tools.SignalLevel.HIGH) {
                    // high
                    String adviceHighExposure = String.format(
                            getString(R.string.advice_high_exposure_description_text),
                            MeasurementFragment.AntennaDisplayToString(mContext, topSignal.getAntennaDisplay()),
                            mTopSignalSourceName
                    );
                    Tools.setHtmlTextToTextView(mTextViewAdvice, adviceHighExposure);
                    // set the advice image to warning (with an exclamation mark)
                    mImageAdvice.setImageResource(R.drawable.ic_error_outline_black_24dp);
                    ImageViewCompat.setImageTintList(mImageAdvice,
                            ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.recommendation_dot_red)));
                } else if (level == Tools.SignalLevel.MODERATE) {
                    //moderate
                    Tools.setHtmlTextToTextView(mTextViewAdvice, getString(R.string.advice_moderate_exposure_description_text));
                    // set the advice image to info ("with an i")
                    mImageAdvice.setImageResource(R.drawable.ic_info_outline_black_24dp);
                    ImageViewCompat.setImageTintList(mImageAdvice,
                            ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.recommendation_dot_orange)));
                } else {
                    //low
                    Tools.setHtmlTextToTextView(mTextViewAdvice, getString(R.string.advice_low_exposure_description_text));
                    // set the advice image to info ("with an i")
                    mImageAdvice.setImageResource(R.drawable.ic_info_outline_black_24dp);
                    ImageViewCompat.setImageTintList(mImageAdvice,
                            ColorStateList.valueOf(ContextCompat.getColor(mContext, R.color.recommendation_dot_green)));
                }

                // We display the top score
                String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
                if (pref.equals(mContext.getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
                    mExposureUnit.setText(R.string.exposure_score_scale);
                    mExposureValueTextView.setText(String.valueOf(Tools.getExposureScore(dbm)));
                    mScaleLowStart.setText(String.valueOf(Tools.getExposureScore(Const.MIN_RECOMMENDATION_SCALE)));
                    mScaleMediumStart.setText(String.valueOf(Tools.getExposureScore(Const.RECOMMENDATION_LOW_THRESHOLD)));
                    mScaleMediumEnd.setText(String.valueOf(Tools.getExposureScore(Const.RECOMMENDATION_HIGH_THRESHOLD)));
                    mScaleHighEnd.setText(String.valueOf(Tools.getExposureScore(Const.MAX_RECOMMENDATION_SCALE)));
                } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_DBM_METRIC))) {
                    mExposureUnit.setText(R.string.dbm_metric);
                    mExposureValueTextView.setText(String.valueOf(dbm));
                    mScaleLowStart.setText(String.valueOf(Const.MIN_RECOMMENDATION_SCALE));
                    mScaleMediumStart.setText(String.valueOf(Const.RECOMMENDATION_LOW_THRESHOLD));
                    mScaleMediumEnd.setText(String.valueOf(Const.RECOMMENDATION_HIGH_THRESHOLD));
                    mScaleHighEnd.setText(String.valueOf(Const.MAX_RECOMMENDATION_SCALE));
                } else if (pref.equals(mContext.getString(R.string.PREF_VALUE_POWER_METRIC))) {
                    // The RawSignalHandler normalize all signals before passing them to the UI.
                    // We have the guarantee that the dbm value can be converted in watt.
                    String[] watt = Tools.dBmToWattWithSIPrefixArray(dbm, false);
                    mExposureValueTextView.setText(String.valueOf(Integer.parseInt(watt[0])));
                    mExposureUnit.setText(watt[1]);
                    mScaleLowStart.setText(Tools.dBmToWattWithSIPrefix(Const.MIN_RECOMMENDATION_SCALE, false));
                    mScaleMediumStart.setText(Tools.dBmToWattWithSIPrefix(Const.RECOMMENDATION_LOW_THRESHOLD, false));
                    mScaleMediumEnd.setText(Tools.dBmToWattWithSIPrefix(Const.RECOMMENDATION_HIGH_THRESHOLD, false));
                    mScaleHighEnd.setText(Tools.dBmToWattWithSIPrefix(Const.MAX_RECOMMENDATION_SCALE, false));
                }

                // Set the basic recommendation based on dbm
                mRecommendationTextView.setText(Tools.getRecommendationTextBasedOnDbm(mContext, dbm));
                Tools.setRecommendationDotBasedOnDbm(mContext, dbm, mRecommendationDot);

                // we display to "what is exposing you the most?" part
                ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> childViewSignals = signalsSlot.getChildViewSignals();
                ConcurrentHashMap<BaseProperty, List<BaseProperty>> childViewWiFiSignals = signalsSlot.getChildViewWifiSignals();

                if (sortedGroupViewSignals != null) {
                    // we go through all the sorted group view signals. There are one signal per
                    // signal type. The index in the for loop is also the position of the row when
                    // we display it. index 0 is for the most powerful signal and is displayed
                    // first.
                    for (int index = 0; index < sortedGroupViewSignals.size(); index++) {
                        BaseProperty signal = sortedGroupViewSignals.get(index);

                        // the signal is Wi-Fi
                        if (signal instanceof WifiProperty) {

                            // We set the exposition dot of the layout according to signal's dbm,
                            // and not according to getConnectedWifiSignal
                            int cumulDbm = signal.cumul_dbm;

                            BaseProperty connectedWifiSignal = Tools.getConnectedWifiSignal(
                                    childViewWiFiSignals.get(signal));

                            // if there is a connected Wi-Fi signal, we use this one for the display
                            if (connectedWifiSignal != null) {
                                signal = connectedWifiSignal;
                            }

                            // we set the name of the highest Wi-Fi signal
                            if (!signal.ssid.isEmpty()) {
                                mWiFiTextView.setText(signal.friendlySourceName(mContext));
                            } else {
                                mWiFiTextView.setText(R.string.hidden_ssid);
                            }

                            // we display the connected image if we are connected to the Wi-Fi antenna
                            if (signal.connected) {
                                mImageWiFiConnected.setVisibility(View.VISIBLE);
                                Log.d(TAG, "updateLayout: connectedWifiSignal Show wifi connected");
                            } else {
                                mImageWiFiConnected.setVisibility(View.INVISIBLE);
                                Log.d(TAG, "updateLayout: connectedWifiSignal NORMAL");
                            }

                            // we set the number of other sources
                            int wifiNbOtherSources = childViewWiFiSignals.size() - 1;
                            if (wifiNbOtherSources > 0) {
                                mWiFiTextViewOthers.setText(
                                        mContext.getResources().getQuantityString(
                                                R.plurals.home_others,
                                                wifiNbOtherSources,
                                                wifiNbOtherSources
                                        )
                                );
                                mWiFiTextViewOthers.setVisibility(View.VISIBLE);
                            } else {
                                mWiFiTextViewOthers.setText("");
                                mWiFiTextViewOthers.setVisibility(View.GONE);
                            }

                            // we set the correct row position
                            setTextViewPosition(index, R.id.layout_wifi);

                            // we set the color of the dot
                            Tools.setRecommendationDotBasedOnDbm(mContext, cumulDbm, mWiFiDot);

                            // show the wifi source divider at the bottom
                            mWiFiSourceDivider.setVisibility(View.VISIBLE);

                            // we display the row
                            mConstraintSetNew.setVisibility(R.id.layout_wifi, ConstraintSet.VISIBLE);

                            // the signal is BT
                        } else if (signal instanceof BluetoothProperty) {

                            // we set the name of the highest BT signal
                            if (signal.bt_device_name != null && !signal.bt_device_name.isEmpty()) {
                                // We display bonded devices in bold
                                mBluetoothTextView.setText(signal.bt_device_name);
                            } else {
                                mBluetoothTextView.setText(R.string.not_available);
                            }

                            // we display the connected image if we are connected to the BT device
                            if (signal.bt_bond_state == BluetoothDevice.BOND_BONDED) {
                                mImageBluetoothConnected.setVisibility(View.VISIBLE);
                            } else {
                                mImageBluetoothConnected.setVisibility(View.INVISIBLE);
                            }

                            // we set the number of other sources
                            List<BaseProperty> bluetoothSignals = childViewSignals.get(MeasurementFragment.AntennaDisplay.BLUETOOTH);
                            int bluetoothNbOtherSources = bluetoothSignals.size() - 1;
                            if (bluetoothNbOtherSources > 0) {
                                mBluetoothOthersTextView.setText(
                                        mContext.getResources().getQuantityString(
                                                R.plurals.home_others,
                                                bluetoothNbOtherSources,
                                                bluetoothNbOtherSources
                                        )
                                );
                                mBluetoothOthersTextView.setVisibility(View.VISIBLE);
                            } else {
                                mBluetoothOthersTextView.setText("");
                                mBluetoothOthersTextView.setVisibility(View.GONE);
                            }

                            // we set the correct row position
                            setTextViewPosition(index, R.id.layout_bluetooth);

                            // we set the color of the dot
                            Tools.setRecommendationDotBasedOnDbm(mContext, signal.cumul_dbm, mBluetoothDot);

                            // show the bluetooth source divider at the bottom
                            mBluetoothSourceDivider.setVisibility(View.VISIBLE);

                            // we display the row
                            mConstraintSetNew.setVisibility(R.id.layout_bluetooth, ConstraintSet.VISIBLE);

                            // the signal is cellular
                        } else {

                            // We set the exposition dot of the layout according to signal's dbm,
                            // and not according to servingCellularSignal
                            int cumulDbm = signal.cumul_dbm;

                            BaseProperty servingCellularSignal = Tools.getServingCellularSignal(childViewSignals);
                            // if there is a serving cell, we use this one for the display
                            if (servingCellularSignal != null) {
                                signal = servingCellularSignal;
                            }

                            // we set the name of the operator
                            signal.prepareOperatorName(); // we call it again in case there is no serving cell
                            mCellularTextView.setText(signal.mOperatorName);

                            // we display the connected image if this is the serving cell
                            if (signal.connected) {
                                mImageCellularConnected.setVisibility(View.VISIBLE);
                            } else {
                                mImageCellularConnected.setVisibility(View.INVISIBLE);
                            }

                            // we set the number of other sources
                            int cellularNbOtherSources = Tools.getNumberOfValidCellularSignals(childViewSignals) - 1;
                            if (cellularNbOtherSources > 0) {
                                mCellularTextViewOthers.setText(
                                        mContext.getResources().getQuantityString(
                                                R.plurals.home_others,
                                                cellularNbOtherSources,
                                                cellularNbOtherSources
                                        )
                                );
                                mCellularTextViewOthers.setVisibility(View.VISIBLE);
                            } else {
                                mCellularTextViewOthers.setText("");
                                mCellularTextViewOthers.setVisibility(View.GONE);
                            }

                            // we set the correct row position
                            setTextViewPosition(index, R.id.layout_cellular);

                            // we set the color of the dot
                            Tools.setRecommendationDotBasedOnDbm(mContext, cumulDbm, mCellularDot);

                            // show the cellular source divider at the bottom
                            mCellularSourceDivider.setVisibility(View.VISIBLE);

                            // we display the row
                            mConstraintSetNew.setVisibility(R.id.layout_cellular, ConstraintSet.VISIBLE);
                        }
                    }

                    // we hide the divider of the last GroupView to have a better display
                    if (sortedGroupViewSignals.size() > 0) {
                        // get the last signal in the sortedGroupViewSignals
                        BaseProperty lastSignal = sortedGroupViewSignals.get(sortedGroupViewSignals.size() - 1);
                        // hide the last divider
                        if (lastSignal instanceof WifiProperty) {
                            mWiFiSourceDivider.setVisibility(View.INVISIBLE);
                        } else if (lastSignal instanceof BluetoothProperty) {
                            mBluetoothSourceDivider.setVisibility(View.INVISIBLE);
                        } else {
                            mCellularSourceDivider.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            } else {
                noSignal = true;
            }
        } else {
            noSignal = true;
        }

        if (noSignal) {
            mExposureValueTextView.setText(getString(R.string.na));
            mExposureUnit.setText("");
            mRecommendationDot.setVisibility(View.INVISIBLE);
            mRecommendationTextView.setVisibility(View.INVISIBLE);

            // As we don't want to have any detailed signal to show, we disable the
            // what's exposing view so that the user can't enter exposure details
            mBottomConstraintLayout.setEnabled(false);

            // show no source message in the center row
            mConstraintSetNew.setVisibility(R.id.textViewNoSource, ConstraintSet.VISIBLE);

            // Note that we must hide the exposure scale before hiding the chevron
            hideExposureScale();
            // hide the exposure scale chevron and disable the chevron tap view
            mChevronExposureScaleImage.setVisibility(View.INVISIBLE);
            mExposureScaleChevronTapView.setEnabled(false);

            // Hide the advice section
            mLayoutAdvice.setVisibility(View.GONE);
        } else {
            mRecommendationDot.setVisibility(View.VISIBLE);
            mRecommendationTextView.setVisibility(View.VISIBLE);

            // Enable the group view so that user can click on it
            mBottomConstraintLayout.setEnabled(true);

            // set "no source" invisible
            mConstraintSetNew.setVisibility(R.id.textViewNoSource, ConstraintSet.INVISIBLE);

            // show the exposure scale chevron and enable the chevron tap view
            mChevronExposureScaleImage.setVisibility(View.VISIBLE);
            mExposureScaleChevronTapView.setEnabled(true);
            if (Tools.getShowExposureScale()) {
                showExposureScale();
                animateCursorToDbm(mCurrentDbm, true, mCursorImage, mContext,
                        mExposureScaleWidth, this, mFragmentHomeView);
            }

            // Show the advice section
            mLayoutAdvice.setVisibility(View.VISIBLE);
        }

        TransitionManager.beginDelayedTransition(mBottomConstraintLayout);
        mConstraintSetNew.applyTo(mBottomConstraintLayout); // set new constraints
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");

        DbRequestHandler.dumpEventToDatabase(Const.EVENT_HOME_FRAGMENT_ON_RESUME);

        checkAndShowErrorIfExists(mContext, mActivity);

        // update layout if we have data in the timeline
        if (mTimeline.size() > 0) {
            updateLayout(mTimeline.get(mTimeline.size() - 1));
        } else {
            // We do not have any data to show. So update the layout with no signalSlot
            // This makes the message 'no source' visible
            updateLayout(null);
        }

        // register the broadcast listeners
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(mLiveTimelineUpdatedBroadcastReceiver,
                new IntentFilter(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION));
        localBroadcastManager.registerReceiver(mWindowFocusChangedBroadcastReceiver,
                new IntentFilter(Const.MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION));

        // we update the selected item to home fragment in the drawer and the bottom navigation bar
        mActivity.selectAndGetNavItem(R.id.nav_home);
        mActivity.selectAndGetBottomNavItem(R.id.bottom_nav_home);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause()");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_HOME_FRAGMENT_ON_PAUSE);

        // unregister the broadcast listeners
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.unregisterReceiver(mLiveTimelineUpdatedBroadcastReceiver);
        localBroadcastManager.unregisterReceiver(mWindowFocusChangedBroadcastReceiver);

        // In case onPause is called before the onGlobalLayout() is called, the listener will
        // not be correctly unregistered from itself. This is why we unregister it from onPause.
        // Note that unregistering the listener if is has already been unregistered is safe.
        mFragmentHomeView.getViewTreeObserver().removeOnGlobalLayoutListener(mMyOnGlobalLayoutListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "in onCreateView()");
        mFragmentHomeView = inflater.inflate(R.layout.fragment_home, container, false);
        mBottomConstraintLayout = mFragmentHomeView.findViewById(R.id.layout_inner);
        mExposureValueTextView = mFragmentHomeView.findViewById(R.id.textViewExposureValue);
        mExposureUnit = mFragmentHomeView.findViewById(R.id.textViewExposureUnit);
        mRecommendationTextView = mFragmentHomeView.findViewById(R.id.textViewRecommendation);

        ImageView imageView = mFragmentHomeView.findViewById(R.id.es_animating_logo);
        ((AnimationDrawable) imageView.getDrawable()).start();
        mRecommendationDot = mFragmentHomeView.findViewById(R.id.recommendationDot);

        mConstraintSetNew = new ConstraintSet();
        mConstraintSetNew.clone(mBottomConstraintLayout);

        // protocolImage is a temporary variable used to set the correct protocol image for each
        // signal type
        AppCompatImageView protocolImage;

        final LinearLayout mLinearLayoutBluetooth = mFragmentHomeView.findViewById(R.id.layout_bluetooth);
        protocolImage = mLinearLayoutBluetooth.findViewById(R.id.protocol_image);
        protocolImage.setImageResource(R.drawable.baseline_bluetooth_24);
        mBluetoothTextView = mLinearLayoutBluetooth.findViewById(R.id.text_view_source_name);
        mBluetoothOthersTextView = mLinearLayoutBluetooth.findViewById(R.id.text_view_number_of_sources);
        mBluetoothDot = mLinearLayoutBluetooth.findViewById(R.id.exposition_dot);
        mBluetoothSourceDivider = mLinearLayoutBluetooth.findViewById(R.id.source_divider);
        mImageBluetoothConnected = mLinearLayoutBluetooth.findViewById(R.id.image_view_connected);
        mLinearLayoutBluetooth.setOnClickListener(v ->
                mActivity.showExposureDetails(MeasurementFragment.AntennaDisplay.BLUETOOTH,
                        new SignalsSlot(), 0, null, null));

        LinearLayout mLinearLayoutWiFi = mFragmentHomeView.findViewById(R.id.layout_wifi);
        protocolImage = mLinearLayoutWiFi.findViewById(R.id.protocol_image);
        protocolImage.setImageResource(R.drawable.baseline_wifi_24);
        mWiFiTextView = mLinearLayoutWiFi.findViewById(R.id.text_view_source_name);
        mWiFiTextViewOthers = mLinearLayoutWiFi.findViewById(R.id.text_view_number_of_sources);
        mWiFiDot = mLinearLayoutWiFi.findViewById(R.id.exposition_dot);
        mWiFiSourceDivider = mLinearLayoutWiFi.findViewById(R.id.source_divider);
        mImageWiFiConnected = mLinearLayoutWiFi.findViewById(R.id.image_view_connected);
        mLinearLayoutWiFi.setOnClickListener(v ->
                mActivity.showExposureDetails(MeasurementFragment.AntennaDisplay.WIFI,
                        new SignalsSlot(), 0, null, null));

        LinearLayout mLinearLayoutCellular = mFragmentHomeView.findViewById(R.id.layout_cellular);
        protocolImage = mLinearLayoutCellular.findViewById(R.id.protocol_image);
        protocolImage.setImageResource(R.drawable.baseline_signal_cellular_4_bar_24);
        mCellularTextView = mLinearLayoutCellular.findViewById(R.id.text_view_source_name);
        mCellularTextViewOthers = mLinearLayoutCellular.findViewById(R.id.text_view_number_of_sources);
        mCellularDot = mLinearLayoutCellular.findViewById(R.id.exposition_dot);
        mCellularSourceDivider = mLinearLayoutCellular.findViewById(R.id.source_divider);
        mImageCellularConnected = mLinearLayoutCellular.findViewById(R.id.image_view_connected);
        mLinearLayoutCellular.setOnClickListener(v ->
                mActivity.showExposureDetails(MeasurementFragment.AntennaDisplay.CELLULAR,
                        new SignalsSlot(), 0, null, null));
        hideAllGroups();

        // exposure scale related code
        mChevronExposureScaleImage = mFragmentHomeView.findViewById(R.id.scale_chevron);
        if (Tools.getShowExposureScale()) {
            // rotate the chevron when on create if Tools.getShowExposureScale() returns
            // true, because, in that case, the exposure scale is expanded.
            rotateImageWithAnimation(mChevronExposureScaleImage, 0, 90, false);
        }
        mExposureScaleLayout = mFragmentHomeView.findViewById(R.id.layout_scale);


        mCursorImage = mFragmentHomeView.findViewById(R.id.scale_cursor);
        mExposureScaleChevronTapView = mFragmentHomeView.findViewById(R.id.scale_chevron_tap_view);
        mScaleGroup = mFragmentHomeView.findViewById(R.id.scale_group);
        mExposureScaleChevronTapView.setOnClickListener(v -> {
            // toggle based on Tools.getShowExposureScale()
            if (Tools.getShowExposureScale()) {
                Tools.setShowExposureScale(false);
                hideExposureScale();
                // rotate the chevron back to 0 degrees
                rotateImageWithAnimation(mChevronExposureScaleImage, 90, 0, true);
                Log.d(TAG, "onClick: Tools.getShowExposureScale() -> false");
            } else {
                Tools.setShowExposureScale(true);
                showExposureScale();
                // animate the cursor from the beginning of the layout
                animateCursorToDbm(mCurrentDbm, false, mCursorImage, mContext,
                        mExposureScaleWidth, HomeFragment.this, mFragmentHomeView);

                // animate rotation of the chevron by 90 degrees
                rotateImageWithAnimation(mChevronExposureScaleImage, 0, 90, true);
                Log.d(TAG, "onClick: Tools.getShowExposureScale() -> true");
            }
            TransitionManager.beginDelayedTransition(mExposureScaleLayout);
            mConstraintSetScaleNew.applyTo(mExposureScaleLayout); // set new constraints
        });

        mConstraintSetScaleNew = new ConstraintSet();
        mConstraintSetScaleNew.clone(mExposureScaleLayout);

        // scale limit text views
        mScaleLowStart = mFragmentHomeView.findViewById(R.id.scale_low_start);
        mScaleMediumStart = mFragmentHomeView.findViewById(R.id.scale_medium_start);
        mScaleMediumEnd = mFragmentHomeView.findViewById(R.id.scale_medium_end);
        mScaleHighEnd = mFragmentHomeView.findViewById(R.id.scale_high_end);

        mMyOnGlobalLayoutListener = new MyOnGlobalLayoutListener();
        mFragmentHomeView.getViewTreeObserver().addOnGlobalLayoutListener(mMyOnGlobalLayoutListener);

        mLayoutAdvice = mFragmentHomeView.findViewById(R.id.layout_advice);
        mLayoutAdvice.setOnClickListener(v -> {
            if (mActivity.mLiveTimeline.size() > 0) {
                mActivity.showAdviceFragment(
                        mActivity.mLiveTimeline.get(mActivity.mLiveTimeline.size() - 1));
            }
        });

        mTextViewAdvice = mLayoutAdvice.findViewById(R.id.text_view_advice_description);
        mImageAdvice = mLayoutAdvice.findViewById(R.id.image_advice);

        return mFragmentHomeView;
    }

    private void hideExposureScale() {
        mScaleGroup.setVisibility(View.GONE);
        // connect divider to the bottom of scale chevron
        mConstraintSetScaleNew.connect(R.id.divider_advice_scale_what_to_do, ConstraintSet.TOP, R.id.textViewRecommendation, ConstraintSet.BOTTOM, 0);
    }

    private void showExposureScale() {
        mScaleGroup.setVisibility(View.VISIBLE);
        // connect divider to the bottom of exposure scale layout
        mConstraintSetScaleNew.connect(R.id.divider_advice_scale_what_to_do, ConstraintSet.TOP, R.id.layout_scale, ConstraintSet.BOTTOM, 0);
    }


    /**
     * reset the display of the groups
     */
    private void hideAllGroups() {
        mConstraintSetNew.setVisibility(R.id.layout_wifi, ConstraintSet.INVISIBLE);
        mConstraintSetNew.setVisibility(R.id.layout_cellular, ConstraintSet.INVISIBLE);
        mConstraintSetNew.setVisibility(R.id.layout_bluetooth, ConstraintSet.INVISIBLE);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Log.d(TAG, "in onAttach()");
        super.onAttach(context);
        mContext = context;
        mActivity = (MainActivity) getActivity();
        mTimeline = mActivity.mLiveTimeline;
    }

    // used to get the view width when we are sure that all elements are rendered
    class MyOnGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {

        @Override
        public void onGlobalLayout() {
            Log.d(TAG, "in onGlobalLayout()");

            Log.d(TAG, "onGlobalLayout: Listener observer removed");
            mFragmentHomeView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

            // If onPause is called before this callback is called (it is possible if ever we
            // go onPause before the rendering is complete, then the fragment is no more
            // linked to an Activity and the method getResources() will return an
            // IllegalStateException. We can test for that with isAdded()
            if (isAdded()) {
                int marginInPixels = (int) getResources().getDimension(R.dimen._exposure_scale_marginLeftRight);
                // the total width of the exposure scale bar in pixels
                // we multiply by 2 to take into account the left and right margins
                mExposureScaleWidth = mFragmentHomeView.getWidth() - 2 * marginInPixels;
                // we call animateCursorToDbm as soon as we have the correct View width
                animateCursorToDbm(mCurrentDbm, false, mCursorImage, mContext,
                        mExposureScaleWidth, HomeFragment.this, mFragmentHomeView);
                Log.d(TAG, "onGlobalLayout: mExposureScaleWidth " + mExposureScaleWidth);
            }
        }
    }

}

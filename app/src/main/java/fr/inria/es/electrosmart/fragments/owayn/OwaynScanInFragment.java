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

package fr.inria.es.electrosmart.fragments.owayn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.fragments.OwaynFragment;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;

/**
 * The fragment that is responsible of running the scan inside the boxer. It uses the proximity
 * sensor to detect that the device is put inside the boxer, waits for
 * {@link Const#PROXIMITY_DETECTION_TIME_MILLIS} milliseconds and starts listening for timeline updates.
 */
public class OwaynScanInFragment extends Fragment {
    private static final String TAG = "OwaynScanInFragment";

    // A progress bar that represents the progress of the scan
    ProgressBar mProgressBar;
    // The timeline from which we retrieve the scan results
    Timeline mTimeline;
    // The listener to notify when the in scan is done successfully.
    // When requested it goes to the next step, that is either the result fragment or the no
    // protection fragment, based on the exposure reduction value.
    // Its behavior is handled by the parent fragment.
    private OnScanInFinishedListener mOnScanInFinishedListener;
    // The listener to notify when the scan in starts and the device is detected
    // to be taken out of the boxer. When requested we jump to the out of boxer error fragment.
    // Its behavior is handled by the parent fragment.
    private OnOutOfBoxListener mOnOutOfBoxListener;
    // The current context
    private Context mContext;
    // Listener that reacts to changes from the proximity sensor
    private SensorEventListener mSensorEventListener = new SensorEventListener() {

        @Override
        public final void onSensorChanged(SensorEvent event) {
            float distance = event.values[0];

            Sensor proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            Log.d(TAG, "onSensorChanged: distance = " + distance);
            Log.d(TAG, "onSensorChanged: proximitySensor.getMaximumRange() = "
                    + proximitySensor.getMaximumRange());

            if (mProximityCountDownTimer != null) {
                mProximityCountDownTimer.cancel();
                mProximityCountDownTimer = null;
            }

            if (distance < proximitySensor.getMaximumRange()) {
                mProximityCountDownTimer = new CountDownTimer(Const.PROXIMITY_DETECTION_TIME_MILLIS,
                        1000) {

                    @Override
                    public void onTick(long millisUntilFinished) {

                    }

                    @Override
                    public void onFinish() {
                        mLastBaseProperty = new WifiProperty(false);

                        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
                        localBroadcastManager.registerReceiver(mLiveTimelineUpdatedBroadcastReceiver,
                                new IntentFilter(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION));

                        final int millisInFuture = SettingsPreferenceFragment.getProtectionTestDuration() * 1000;
                        mProgressBar.setMax(millisInFuture);
                        mProgressBar.setVisibility(View.VISIBLE);
                        mProgressBar.setProgress(0);
                        mScanCountDownTimer = new OwaynFragment.ScanCountDownTimer(mContext,
                                millisInFuture, 1) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                mProgressBar.setProgress((int) (millisInFuture - millisUntilFinished));
                            }

                            @Override
                            public void onFinish() {
                                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
                                localBroadcastManager.unregisterReceiver(mLiveTimelineUpdatedBroadcastReceiver);
                                if (mLastBaseProperty.isValidSignal) {
                                    mOnScanInFinishedListener.onScanInFinished(mLastBaseProperty.dbm);
                                } else {
                                    mOnNoWifiListener.onNoWifi();
                                }
                            }
                        };
                        mScanCountDownTimer.start();
                    }
                }.start();
            } else if (mScanCountDownTimer != null) {
                mScanCountDownTimer.cancel();
                mScanCountDownTimer = null;
                mOnOutOfBoxListener.onOutOfBox();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    // The count down timer object that is used to wait 2 seconds when the device is inside the
    // boxer before starting the scan.
    private CountDownTimer mProximityCountDownTimer;

    // The sensor manager that enables communication with the device sensors
    private SensorManager mSensorManager;

    // The count down timer object that is used for the scan
    private OwaynFragment.ScanCountDownTimer mScanCountDownTimer;

    // The listener to notify when the scan finishes with now wifi source detected.
    // When requested it goes to the no wifi error fragment.
    // Its behavior is handled by the parent fragment.
    private OwaynNoWifiErrorFragment.OnNoWifiListener mOnNoWifiListener;

    // The broadcast event used to retrieve scan results by listening to timeline updates.
    private BroadcastReceiver mLiveTimelineUpdatedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeline.size() > 0) {
                List<BaseProperty> sortedWifiSignals = mTimeline.get(mTimeline.size() - 1)
                        .getSortedWifiGroupSignals();
                if (sortedWifiSignals != null) {
                    BaseProperty baseProperty = sortedWifiSignals.get(0);
                    if (baseProperty != null && baseProperty.isValidSignal) {
                        mLastBaseProperty = baseProperty;
                    }
                }
            }
        }
    };

    // A boolean variable that keeps track of the fragment's visibility.
    private boolean mIsVisibleToUser;
    // An object that keeps track of the last valid wifi scan result.
    private BaseProperty mLastBaseProperty = new WifiProperty(false);

    private void onShow() {
        if (mContext != null) {
            mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
            Sensor proximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mSensorManager.registerListener(mSensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }

    }

    private void onHide() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
        if (mScanCountDownTimer != null) {
            mScanCountDownTimer.cancel();
            mScanCountDownTimer = null;
        }
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.unregisterReceiver(mLiveTimelineUpdatedBroadcastReceiver);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owayn_scan_in, container, false);
        ImageView imageView = view.findViewById(R.id.image_owayn_phone_inside_boxer);
        ((AnimationDrawable) imageView.getDrawable()).start();
        mProgressBar = view.findViewById(R.id.progress_bar_owayn_scan_in_progress);
        Tools.setProgressHorizontalBarTheme(mContext, mProgressBar);
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !mIsVisibleToUser) {
            onShow();
        } else if (!isVisibleToUser && mIsVisibleToUser) {
            onHide();
        }
        mIsVisibleToUser = isVisibleToUser;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof OnScanInFinishedListener) {
            mOnScanInFinishedListener = (OnScanInFinishedListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.getClass().getName()
                    + " must implement " + OnScanInFinishedListener.class.getName());
        }
        if (parentFragment instanceof OwaynNoWifiErrorFragment.OnNoWifiListener) {
            mOnNoWifiListener = (OwaynNoWifiErrorFragment.OnNoWifiListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.getClass().getName()
                    + " must implement " + OwaynNoWifiErrorFragment.OnNoWifiListener.class.getName());
        }
        if (parentFragment instanceof OnOutOfBoxListener) {
            mOnOutOfBoxListener = (OnOutOfBoxListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.getClass().getName()
                    + " must implement " + OnOutOfBoxListener.class.getName());
        }
        mTimeline = ((MainActivity) getActivity()).mLiveTimeline;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mIsVisibleToUser) {
            onHide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsVisibleToUser) {
            onShow();
        }
    }


    /**
     * A listener interface that is used to implement the action to execute when the scan inside
     * the boxer terminates successfully.
     */
    public interface OnScanInFinishedListener {
        void onScanInFinished(int dbm);
    }


    /**
     * A listener interface that is used to implement the action to execute when the scan in starts
     * and then the device is detected to be taken out of the boxer.
     */
    public interface OnOutOfBoxListener {
        void onOutOfBox();
    }

    public static class OwaynFragmentFactory implements OwaynFragment.OwaynFragmentFactory {
        @Override
        public Fragment getInstance() {
            return new OwaynScanInFragment();
        }
    }
}

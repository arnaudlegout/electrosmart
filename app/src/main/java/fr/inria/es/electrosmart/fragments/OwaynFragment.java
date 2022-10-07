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


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.HashSet;
import java.util.Set;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.fragments.owayn.OwaynAirplaneModeFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynExplanationFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynGenericErrorFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynLocationDisabledFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynLocationPermissionFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynNoProtectionFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynNoWifiErrorFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynOutOfBoxerErrorFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynResultFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynScanInFragment;
import fr.inria.es.electrosmart.fragments.owayn.OwaynScanOutFragment;
import fr.inria.es.electrosmart.signalhandler.RawSignalHandler;
import fr.inria.es.electrosmart.util.LockableViewPager;

/**
 * A container of the owayn fragments (fragments in the {@code owayn} package). It uses a
 * view pager to integrate the fragments. It acts as a callback object in order to listen
 * to events sent by the owayn fragments and acts accordingly.
 */
public class OwaynFragment extends MainActivityFragment implements
        OwaynExplanationFragment.OnStartClickListener,
        OwaynScanOutFragment.OnScanOutFinishedListener,
        OwaynScanInFragment.OnScanInFinishedListener,
        OwaynResultFragment.OnRestartListener,
        OwaynNoWifiErrorFragment.OnNoWifiListener,
        OwaynScanInFragment.OnOutOfBoxListener,
        OwaynOutOfBoxerErrorFragment.OnResumeScanInListener {

    private static final String TAG = "OwaynFragment";

    // The ViewPager that is inflated with one of the protection test steps fragments.
    private LockableViewPager mViewPager;

    // A variable used to detect that the protection test has been interrupted. It is used to show
    // the "test interrupted" error dialog.
    private boolean mWorkflowInterrupted;

    // The window object used to switch on and off the keep screen on mode
    private Window mWindow;

    // The current context
    private Context mContext;

    // The listener to notify when the measure button is clicked.
    // When requested it goes to the fragment with the specified id.
    // Its behavior is handled by the parent activity.
    private MainActivityFragment.OnNavigateToFragmentListener mOnNavigateToFragmentListener;

    // Identify each fragment with an id. Note that the smallest id should always be 0
    // and every id should be an increment by one of the id just below it. The order is not
    // important.
    private static final int EXPLANATION = 0;           // scan welcome screen
    private static final int SCAN_OUT = 1;              // scan outside the boxer
    private static final int SCAN_IN = 2;               // scan inside the boxer
    private static final int AIRPLANE_MODE = 3;         // error screen when in airplane mode
    private static final int GENERIC_ERROR = 4;         // generic error screen
    private static final int NO_WIFI_ERROR = 5;         // error screen when there is no Wi-Fi
    private static final int OUT_OF_BOXER_ERROR = 6;    // error when the scan_in is interrupted
    private static final int NO_PROTECTION = 7;         // result screen when the scan shows no protection
    private static final int RESULT = 8;                // result screen
    private static final int LOCATION_DISABLED = 9;     // error screen when there is no location service
    private static final int LOCATION_PERMISSION = 10;  // error screen when location permission is denied


    // Defines the order of the fragments to be used in the view pager
    private final int[] mPosToId;

    // Provides a way to find the position of a fragment knowing its id
    private final int[] mIdToPos;

    public OwaynFragment() {

        mPosToId = new int[]{
                EXPLANATION,
                SCAN_OUT,
                SCAN_IN,
                AIRPLANE_MODE,
                LOCATION_DISABLED,
                GENERIC_ERROR,
                NO_WIFI_ERROR,
                OUT_OF_BOXER_ERROR,
                NO_PROTECTION,
                RESULT,
                LOCATION_PERMISSION
        };

        mIdToPos = new int[mPosToId.length];

        // Build mIdToPos array and check that the ids are unique (No duplicates)
        Set<Integer> ids = new HashSet<>(mPosToId.length);

        for (int position = 0; position < mPosToId.length; position++) {
            int id = mPosToId[position];
            if (ids.contains(id)) {
                throw new RuntimeException("Fragment in position " + mIdToPos[id] + " and " +
                        position + " have the same id: " + id);
            }
            ids.add(id);
            try {
                mIdToPos[id] = position;
            } catch (ArrayIndexOutOfBoundsException ex) {
                RuntimeException exception = new RuntimeException("Constraint on ids is broken: " +
                        "The smallest id is not equal to 0 or ids are not contiguous");
                exception.setStackTrace(ex.getStackTrace());
                throw exception;
            }
        }
    }

    /**
     * The pager adapter that returns the right fragment implementation for each position
     */
    static class ProtectionPagerAdapter extends FragmentStatePagerAdapter {

        private final int[] mIdToPos;
        private final int[] mPosToId;
        private final OwaynFragmentFactory[] mFragmentFactories;

        ProtectionPagerAdapter(FragmentManager fm, int[] posToId, int[] idToPos) {
            super(fm);
            mIdToPos = idToPos;
            mPosToId = posToId;
            mFragmentFactories = new OwaynFragmentFactory[mIdToPos.length];
            mFragmentFactories[EXPLANATION] = new OwaynExplanationFragment.OwaynFragmentFactory();
            mFragmentFactories[SCAN_OUT] = new OwaynScanOutFragment.OwaynFragmentFactory();
            mFragmentFactories[SCAN_IN] = new OwaynScanInFragment.OwaynFragmentFactory();
            mFragmentFactories[AIRPLANE_MODE] = new OwaynAirplaneModeFragment.OwaynFragmentFactory();
            mFragmentFactories[GENERIC_ERROR] = new OwaynGenericErrorFragment.OwaynFragmentFactory();
            mFragmentFactories[NO_WIFI_ERROR] = new OwaynNoWifiErrorFragment.OwaynFragmentFactory();
            mFragmentFactories[OUT_OF_BOXER_ERROR] = new OwaynOutOfBoxerErrorFragment.OwaynFragmentFactory();
            mFragmentFactories[NO_PROTECTION] = new OwaynNoProtectionFragment.OwaynFragmentFactory();
            mFragmentFactories[RESULT] = new OwaynResultFragment.OwaynFragmentFactory();
            mFragmentFactories[LOCATION_DISABLED] = new OwaynLocationDisabledFragment.OwaynFragmentFactory();
            mFragmentFactories[LOCATION_PERMISSION] = new OwaynLocationPermissionFragment.OwaynFragmentFactory();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentFactories[mPosToId[position]].getInstance();
        }

        @Override
        public int getCount() {
            return mPosToId.length;
        }
    }


    private final BroadcastReceiver mWindowFocusChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkErrors();
        }
    };

    // A variable to save the result of the scan outside the boxer
    private int mResultScanOut;

    // A variable to save the result of the test
    public static int sResultReductionPercent;

    /**
     * Creates a new instance of the fragment to which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static MainActivityFragment newInstance(Bundle arguments) {
        MainActivityFragment fragment = new OwaynFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owayn, container, false);
        mViewPager = view.findViewById(R.id.viewPager);
        mViewPager.setAdapter(new ProtectionPagerAdapter(getChildFragmentManager(), mPosToId, mIdToPos));
        mViewPager.setSwappable(false);

        return view;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartClick() {
        mViewPager.setCurrentItem(mIdToPos[SCAN_OUT]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onScanOutFinished(int dbm) {
        mResultScanOut = dbm;
        mViewPager.setCurrentItem(mIdToPos[SCAN_IN]);
    }

    /**
     * Compute the exposure reduction percentage based in the value measured out of the boxer
     * and the value measured in the boxer, both values in dBm. The formula we use is
     * percentage_res = 100 - 10**((in-out)/10)*100
     * <p>
     * Explanation
     * -----------
     * Assuming in and out in Watt
     * We know that
     * out*(1 - percentage_res/100) = in
     * therefore
     * percentage_res = 100 - in/out*100
     * <p>
     * As in and out are in dBm, we can compute the value in Watt with 10**(in/10) and 10**(out/10)
     * which gives when we substitute
     * percentage_res = 100 - 10**((in-out)/10)*100
     *
     * @param out measure out of the boxer in dbm
     * @param in  measure in the boxer in dbm
     * @return the reduction percentage
     */
    private int scanResult(int out, int in) {
        return (int) (100f - Math.pow(10f, (in - out) / 10f) * 100f);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onScanInFinished(int resultScanIn) {
        sResultReductionPercent = scanResult(mResultScanOut, resultScanIn);
        // if the protection is lower than Const.PROTECTION_NO_REDUCTION_THRESHOLD we display there
        // is no protection detected.
        if (sResultReductionPercent >= Const.PROTECTION_NO_REDUCTION_THRESHOLD) {
            mViewPager.setCurrentItem(mIdToPos[RESULT]);
        } else {
            mViewPager.setCurrentItem(mIdToPos[NO_PROTECTION]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRestart() {
        mViewPager.setCurrentItem(mIdToPos[EXPLANATION]);
    }


    /**
     * Handle the back button event in the fragment.
     * <p>
     * If we are not in the explanation fragment, the back button returns to the explanation
     * fragment. If we are in the explanation fragment, the back button delegate to the main
     * activity the action (by returning false). In practice, the main activity will simply load
     * the measurement fragment.
     *
     * @return true if we must stay in the explanation fragment, false we must leave the owayn test
     */
    @Override
    public boolean onBackPressed() {
        if (mViewPager.getCurrentItem() != mIdToPos[EXPLANATION]
                && mViewPager.getCurrentItem() != mIdToPos[RESULT]
                && mViewPager.getCurrentItem() != mIdToPos[AIRPLANE_MODE]
                && mViewPager.getCurrentItem() != mIdToPos[LOCATION_DISABLED]
                && mViewPager.getCurrentItem() != mIdToPos[LOCATION_PERMISSION]
        ) {
            mViewPager.setCurrentItem(mIdToPos[EXPLANATION]);
        } else {
            mOnNavigateToFragmentListener.onBottomNavigateTo(R.id.bottom_nav_solutions);
        }
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onNoWifi() {
        mViewPager.setCurrentItem(mIdToPos[NO_WIFI_ERROR]);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onOutOfBox() {
        mViewPager.setCurrentItem(mIdToPos[OUT_OF_BOXER_ERROR]);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onResumeScanIn() {
        mViewPager.setCurrentItem(mIdToPos[SCAN_IN]);
    }

    /**
     * A wrapper around a {@link CountDownTimer} that intercepts the start and cancel queries and
     * the finish callback to enable or disable the temporary wardrive mode and plays the ticking
     * and microwave bell sounds and the vibration.
     */
    public static abstract class ScanCountDownTimer {

        // The duration of the end vibration in milliseconds
        private static final int VIBRATE_DURATION_MILLISECONDS = 500;
        // The decorated CountDownTimer
        @NonNull
        private final CountDownTimer mCountDownTimer;
        // MediaPlayer object used to play the ticking sound
        private MediaPlayer mMediaPlayerTicking;
        // MediaPlayer object used to play the microwave bell sound
        private MediaPlayer mMediaPlayerBell;
        // The vibrator object to be used to play vibration
        @NonNull
        private final Vibrator mVibrator;
        // The context object to be used to retrieve the sound resources
        @NonNull
        private final Context mContext;

        /**
         * Constructor.
         *
         * @param context the context to be used to retrieve resources
         * @see CountDownTimer
         */
        public ScanCountDownTimer(@NonNull Context context, long millisInFuture, long countDownInterval) {
            mCountDownTimer = new CountDownTimer(millisInFuture, countDownInterval) {
                @Override
                public void onTick(long millisUntilFinished) {
                    ScanCountDownTimer.this.onTick(millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    ScanCountDownTimer.this.onFinish();
                    RawSignalHandler.setTempWardrive(false);
                    if (mMediaPlayerTicking != null) {
                        mMediaPlayerTicking.stop();
                        mMediaPlayerTicking.release();
                        mMediaPlayerTicking = null;
                    }
                    mMediaPlayerBell = MediaPlayer.create(mContext, R.raw.small_bell);
                    mMediaPlayerBell.setOnCompletionListener(MediaPlayer::release);
                    mMediaPlayerBell.start();
                    mMediaPlayerBell = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mVibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_DURATION_MILLISECONDS,
                                VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        mVibrator.vibrate(VIBRATE_DURATION_MILLISECONDS);
                    }
                }
            };
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            mContext = context;
        }

        /**
         * @see CountDownTimer#onTick(long)
         */
        public abstract void onTick(long millisUntilFinished);

        /**
         * @see CountDownTimer#onFinish()
         */
        public abstract void onFinish();


        /**
         * Starts the {@code CountDownTimer} as described in {@link CountDownTimer#start()} but also enables
         * wardrive mode and prevent's the screen from turning off.
         *
         * @see CountDownTimer#start()
         */
        public CountDownTimer start() {
            RawSignalHandler.setTempWardrive(true);
            mMediaPlayerTicking = MediaPlayer.create(mContext, R.raw.ticking);
            mMediaPlayerTicking.start();
            return mCountDownTimer.start();
        }

        /**
         * Cancels the {@code CountDownTimer} as described in {@link CountDownTimer#cancel()} but also disables
         * wardrive mode and set's screen mode to normal.
         *
         * @see CountDownTimer#cancel()
         */
        public void cancel() {
            RawSignalHandler.setTempWardrive(false);
            if (mMediaPlayerTicking != null) {
                mMediaPlayerTicking.stop();
                mMediaPlayerTicking.release();
                mMediaPlayerTicking = null;
            }
            mCountDownTimer.cancel();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        Activity parentActivity = getActivity();
        if (parentActivity instanceof MainActivityFragment.OnNavigateToFragmentListener) {
            mOnNavigateToFragmentListener = (MainActivityFragment.OnNavigateToFragmentListener)
                    parentActivity;
        } else {
            throw new RuntimeException(parentActivity.getClass().getName()
                    + " must implement " + MainActivityFragment.OnNavigateToFragmentListener
                    .class.getName());
        }
        mWindow = parentActivity.getWindow();
    }

    @Override
    public void onPause() {
        super.onPause();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_OWAYN_FRAGMENT_ON_PAUSE);

        Log.d(TAG, "onPause: mViewPager.getCurrentItem(): " + mViewPager.getCurrentItem());
        mWorkflowInterrupted = mViewPager.getCurrentItem() == mIdToPos[SCAN_IN]
                || mViewPager.getCurrentItem() == mIdToPos[SCAN_OUT];
        Log.d(TAG, "onPause: mWorkflowInterrupted: " + mWorkflowInterrupted);
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.unregisterReceiver(mWindowFocusChangedBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_OWAYN_FRAGMENT_ON_RESUME);

        // Keep the screen always on when we are in the owayn test
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Attach the on windows focus changed receiver
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(mWindowFocusChangedBroadcastReceiver,
                new IntentFilter(Const.MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION));

        checkErrors();
    }

    /**
     * Checks if there is any errors and jumps to the appropriate error fragment. If we are showing
     * an error fragment and the error is no longer valid, jump to the starting fragment.
     */
    private void checkErrors() {
        if (Tools.isAirplaneModeActivated(mContext)) {
            mViewPager.setCurrentItem(mIdToPos[AIRPLANE_MODE]);
        } else if (Tools.isLocationDisabled(mContext)) {
            mViewPager.setCurrentItem(mIdToPos[LOCATION_DISABLED]);
        } else if (!Tools.isAccessFineLocationGranted(mContext)) {
            mViewPager.setCurrentItem(mIdToPos[LOCATION_PERMISSION]);
        } else if (mWorkflowInterrupted) {
            mViewPager.setCurrentItem(mIdToPos[GENERIC_ERROR]);
            mWorkflowInterrupted = false;

            // if we were on an error fragment and return to the app with the error fixed (we know
            // because we reached this else if clause), we show the explanation dialog.
        } else if (mViewPager.getCurrentItem() == mIdToPos[AIRPLANE_MODE]
                || mViewPager.getCurrentItem() == mIdToPos[LOCATION_DISABLED]
                || mViewPager.getCurrentItem() == mIdToPos[LOCATION_PERMISSION]
                || mViewPager.getCurrentItem() == mIdToPos[GENERIC_ERROR]
        ) {
            mViewPager.setCurrentItem(mIdToPos[EXPLANATION]);
        }
    }

    /**
     * Defines a uniform way to instantiate owayn fragments
     */
    public interface OwaynFragmentFactory {
        Fragment getInstance();
    }
}

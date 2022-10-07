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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.fragments.OwaynFragment;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;
import fr.inria.es.electrosmart.util.LockableViewPager;

/**
 * The fragment responsible of running the scan outside the boxer.
 */
public class OwaynScanOutFragment extends Fragment {

    private static final String TAG = "OwaynScanOutFragment";

    // A progress bar that represent the progress of the scan
    ProgressBar mProgressBar;

    // The view pager that used to change the views that are shown as the scan continues.
    private LockableViewPager mViewPager;
    private Activity mParentActivity;
    private Fragment mParentFragment;
    // The context object to be used to retrieve resources
    private Context mContext;

    // The count down timer object that is used for the scan
    private OwaynFragment.ScanCountDownTimer mCountDownTimer;
    // A boolean variable that keeps track of the fragment's visibility.
    private boolean mIsVisibleToUser;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owayn_scan_out, container, false);
        mProgressBar = view.findViewById(R.id.progress_bar_owayn_scan_out_progress);
        mViewPager = view.findViewById(R.id.view_pager_owayn_scan_out);
        mViewPager.setSwappable(false);
        Tools.setProgressHorizontalBarTheme(mContext, mProgressBar);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mParentFragment = getParentFragment();
        mParentActivity = getActivity();
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

    private void onHide() {
        mCountDownTimer.cancel();
    }

    private void onShow() {
        long millisInFuture = SettingsPreferenceFragment.getProtectionTestDuration() * 1000;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mCountDownTimer = new ScanOutCountDownTimerAPI28(mContext, mParentActivity,
                    mParentFragment, mProgressBar, mViewPager, millisInFuture);
        } else {
            mCountDownTimer = new ScanOutCountDownTimerOther(mContext, mParentActivity,
                    mParentFragment, mProgressBar, mViewPager, millisInFuture);
        }
        mCountDownTimer.start();
    }

    /**
     * An implementation of {@link ScanOutCountDownTimer} for API >= 28. In that case, as the
     * scan takes much longer than for other API (due to the Android P limitation of
     * 4 Wi-Fi scans every 2 minutes), we display more information screens.
     */
    static final class ScanOutCountDownTimerAPI28 extends ScanOutCountDownTimer {

        // The duration of the count down in milliseconds
        private final long mMillisInFuture;

        ScanOutCountDownTimerAPI28(@NonNull Context context, @NonNull Activity parentActivity,
                                   @NonNull Fragment parentFragment, @NonNull ProgressBar progressBar,
                                   @NonNull ViewPager viewPager, long millisInFuture) {
            super(context, parentActivity, parentFragment, progressBar, viewPager,
                    new ScanOutPagerAdapterAPI28(context,
                            new ScanOutPagerAdapterOther(context,
                                    new ScanOutPagerAdapter(context, new AtomicInteger(0)))),
                    millisInFuture);
            mMillisInFuture = millisInFuture;
        }

        @Override
        protected int page(long millisUntilFinished) {
            if (millisUntilFinished <= 0.25 * mMillisInFuture) {
                return 3;
            } else if (millisUntilFinished <= 0.5 * mMillisInFuture) {
                return 2;
            } else if (millisUntilFinished <= 0.75 * mMillisInFuture) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * An implementation of {@link ScanOutCountDownTimer} for API < 28.
     */
    static final class ScanOutCountDownTimerOther extends ScanOutCountDownTimer {

        // The duration of the count down in milliseconds
        private final long mMillisInFuture;

        ScanOutCountDownTimerOther(@NonNull Context context, @NonNull Activity parentActivity,
                                   @NonNull Fragment parentFragment, @NonNull ProgressBar progressBar,
                                   @NonNull ViewPager viewPager, long millisInFuture) {
            super(context, parentActivity, parentFragment, progressBar, viewPager,
                    new ScanOutPagerAdapterOther(context,
                            new ScanOutPagerAdapter(context, new AtomicInteger(0))), millisInFuture);
            mMillisInFuture = millisInFuture;
        }

        @Override
        protected int page(long millisUntilFinished) {
            if (millisUntilFinished <= 0.3 * mMillisInFuture) {
                return 2;
            } else if (millisUntilFinished <= 0.6 * mMillisInFuture) {
                return 1;
            } else {
                return 0;
            }
        }
    }


    /**
     * The common behavior between {@link ScanOutCountDownTimerOther} and
     * {@link ScanOutCountDownTimerAPI28} with an abstract method {@link #page(long)} which its
     * implementation differs between the two.
     */
    static abstract class ScanOutCountDownTimer extends OwaynFragment.ScanCountDownTimer {

        // The broadcast receiver object used to listen to timeline updates
        @NonNull
        private final BroadcastReceiver mReceiver;
        // The view pager to populate with different views as the scan continues
        @NonNull
        private final ViewPager mViewPager;
        // The progress bar that shows the progress of the scan
        @NonNull
        private final ProgressBar mProgressBar;
        // The listener to notify when the outside scan is done successfully.
        // When requested, we jump to the "scan in" fragment.
        // Its behavior is handled by the parent fragment.
        @NonNull
        private final OnScanOutFinishedListener mOnScanOutFinishedListener;
        // An object that keeps track of the maximum dbm value wifi scan result.
        @NonNull
        private BaseProperty mMaxBaseProperty;
        // The listener to notify when the scan finishes with no wifi source detected.
        // When requested it goes to the "no wifi error" fragment.
        // Its behavior is handled by the parent fragment.
        private OwaynNoWifiErrorFragment.OnNoWifiListener mOnNoWifiListener;
        @NonNull
        // The context object to be used to retrieve resources
        private final Context mContext;
        // The live timeline used to retrieve scan results
        @NonNull
        private final Timeline mTimeline;
        // The adapter round mViewPager responsible of inflating the right content
        @NonNull
        private final ScanOutPagerAdapter mPagerAdapter;
        // The number of wifi sources detected
        @NonNull
        private final AtomicInteger mSourceCount;
        // The duration of the count down in milliseconds
        private final long mMillisInFuture;

        ScanOutCountDownTimer(@NonNull Context context, @NonNull Activity parentActivity,
                              @NonNull Fragment parentFragment, @NonNull ProgressBar progressBar,
                              @NonNull ViewPager viewPager, @NonNull ScanOutPagerAdapter pagerAdapter,
                              long millisInFuture) {
            super(context, millisInFuture, 1);
            mMillisInFuture = millisInFuture;
            mContext = context;
            if (parentFragment instanceof OnScanOutFinishedListener) {
                mOnScanOutFinishedListener = (OnScanOutFinishedListener) parentFragment;
            } else {
                throw new RuntimeException(parentFragment.getClass().getName()
                        + " must implement " + OnScanOutFinishedListener.class.getName());
            }
            if (parentFragment instanceof OwaynNoWifiErrorFragment.OnNoWifiListener) {
                mOnNoWifiListener = (OwaynNoWifiErrorFragment.OnNoWifiListener) parentFragment;
            } else {
                throw new RuntimeException(parentFragment.getClass().getName()
                        + " must implement " + OwaynNoWifiErrorFragment.OnNoWifiListener.class.getName());
            }
            mTimeline = ((MainActivity) parentActivity).mLiveTimeline;
            mViewPager = viewPager;
            mProgressBar = progressBar;
            mPagerAdapter = pagerAdapter;
            mSourceCount = mPagerAdapter.getSourceCount();
            mViewPager.setAdapter(mPagerAdapter);
            mMaxBaseProperty = new WifiProperty(false);
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (mTimeline.size() > 0) {
                        List<BaseProperty> sortedWifiSignals = mTimeline.get(mTimeline.size() - 1)
                                .getSortedWifiGroupSignals();
                        if (sortedWifiSignals != null) {
                            BaseProperty baseProperty = sortedWifiSignals.get(0);
                            if (baseProperty != null && baseProperty.compareTo(mMaxBaseProperty) > 0) {
                                mMaxBaseProperty = baseProperty;
                                if (mSourceCount.get() < sortedWifiSignals.size()) {
                                    mSourceCount.set(sortedWifiSignals.size());
                                }
                            }
                            mPagerAdapter.notifyDataSetChanged();
                            Log.d(TAG, "onReceive: source count: " + mSourceCount);
                        }
                    }
                }
            };
        }

        @Override
        public CountDownTimer start() {
            mViewPager.setCurrentItem(0);
            mProgressBar.setMax((int) mMillisInFuture);
            mProgressBar.setProgress(0);
            mMaxBaseProperty = new WifiProperty(false);
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
            localBroadcastManager.registerReceiver(mReceiver,
                    new IntentFilter(Const.MAIN_ACTIVITY_LIVE_TIMELINE_UPDATED_ACTION));
            return super.start();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mViewPager.setCurrentItem(page(millisUntilFinished));
            mProgressBar.setProgress(progress(millisUntilFinished));
        }

        protected abstract int page(long millisUntilFinished);

        private int progress(long millisUntilFinished) {
            return (int) (mMillisInFuture - millisUntilFinished);
        }

        @Override
        public void cancel() {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
            localBroadcastManager.unregisterReceiver(mReceiver);
            super.cancel();
        }

        @Override
        public void onFinish() {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
            localBroadcastManager.unregisterReceiver(mReceiver);
            if (mMaxBaseProperty.isValidSignal) {
                mOnScanOutFinishedListener.onScanOutFinished(mMaxBaseProperty.dbm);
            } else {
                mOnNoWifiListener.onNoWifi();
            }
        }
    }

    /**
     * The common behavior between {@link ScanOutPagerAdapterOther} and
     * {@link ScanOutPagerAdapterAPI28}. As the scan takes much longer for API 28
     * than for other API (due to the Android P limitation of 4 Wi-Fi scans every
     * 2 minutes), we display more information screens.
     */
    static class ScanOutPagerAdapter extends PagerAdapter {

        // The context used to retrieve resources
        @NonNull
        private final Context mContext;
        // The number of detected wifi sources
        @NonNull
        private final AtomicInteger mSourceCount;

        ScanOutPagerAdapter(@NonNull Context context, @NonNull AtomicInteger sourceCount) {
            mContext = context;
            mSourceCount = sourceCount;
        }

        @NonNull
        AtomicInteger getSourceCount() {
            return mSourceCount;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public void startUpdate(@NonNull ViewGroup container) {
            TextView textViewCount = container.findViewById(R.id.text_view_source_count_number);
            TextView textViewCountMessage = container.findViewById(R.id.text_view_scan_out_source_count_message);
            if (textViewCount != null && textViewCountMessage != null) {
                textViewCount.setText(String.valueOf(mSourceCount.get()));
                if (mSourceCount.get() == 0) {
                    textViewCountMessage.setText(R.string.owayn_scan_out_source_count_zero);
                } else {
                    textViewCountMessage.setText(Html.fromHtml(mContext.getResources().getQuantityString(
                            R.plurals.owayn_scan_out_source_count, mSourceCount.get(), mSourceCount.get())));
                }
            }
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }

    /**
     * Decorates {@link ScanOutPagerAdapter} by adding the behavior to handle the content
     * of the {@link #mViewPager} that is specific for API < 28.
     */
    static final class ScanOutPagerAdapterOther extends ScanOutPagerAdapter {

        // The context used to retrieve resources
        @NonNull
        private final Context mContext;
        // The decorated object
        @NonNull
        private final ScanOutPagerAdapter mPagerAdapter;

        ScanOutPagerAdapterOther(@NonNull Context context, @NonNull ScanOutPagerAdapter pagerAdapter) {
            super(context, pagerAdapter.getSourceCount());
            mContext = context;
            mPagerAdapter = pagerAdapter;
        }

        @NonNull
        public AtomicInteger getSourceCount() {
            return mPagerAdapter.getSourceCount();
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return mPagerAdapter.isViewFromObject(view, object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            if (position < 2) {
                View viewMessageContainer = inflater.inflate(R.layout.layout_owayn_scan_out_message_container,
                        container, false);
                TextView textViewMessage = viewMessageContainer.findViewById(R.id.text_view_scan_out_message);
                if (position == 0) {
                    textViewMessage.setText(R.string.owayn_scan_out_measuring);
                } else {
                    textViewMessage.setText(R.string.owayn_scan_out_volume_on);
                }
                container.addView(viewMessageContainer);
                return viewMessageContainer;
            } else {
                View viewSourceCountContainer = inflater.inflate(R.layout.layout_owayn_scan_out_source_count,
                        container, false);
                container.addView(viewSourceCountContainer);
                return viewSourceCountContainer;
            }
        }

        @Override
        public void startUpdate(@NonNull ViewGroup container) {
            mPagerAdapter.startUpdate(container);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            mPagerAdapter.destroyItem(container, position, object);
        }

    }

    /**
     * Decorates {@link ScanOutPagerAdapter} by adding the behavior to handle the content
     * of the {@link #mViewPager} that is specific for API >= 28.
     */
    static final class ScanOutPagerAdapterAPI28 extends ScanOutPagerAdapter {

        // The context used to retrieve resources
        @NonNull
        private final Context mContext;
        // The decorated object
        @NonNull
        private final ScanOutPagerAdapter mPagerAdapter;

        ScanOutPagerAdapterAPI28(@NonNull Context context, @NonNull ScanOutPagerAdapter pagerAdapter) {
            super(context, pagerAdapter.getSourceCount());
            mContext = context;
            mPagerAdapter = pagerAdapter;
        }

        @NonNull
        public AtomicInteger getSourceCount() {
            return mPagerAdapter.getSourceCount();
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return mPagerAdapter.isViewFromObject(view, object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            if (position < 2) {
                return mPagerAdapter.instantiateItem(container, position);
            } else if (position == 2) {
                View viewMessageContainer = inflater.inflate(R.layout.layout_owayn_scan_out_message_container,
                        container, false);
                TextView textViewMessage = viewMessageContainer.findViewById(R.id.text_view_scan_out_message);
                textViewMessage.setText(R.string.owayn_scan_out_one_more_minute);
                container.addView(viewMessageContainer);
                return viewMessageContainer;
            } else {
                return mPagerAdapter.instantiateItem(container, position - 1);
            }
        }

        @Override
        public void startUpdate(@NonNull ViewGroup container) {
            mPagerAdapter.startUpdate(container);
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            mPagerAdapter.destroyItem(container, position, object);
        }
    }

    /**
     * A listener interface that is used to implement the behavior to be executed when the scan
     * {@link OwaynScanOutFragment} finishes successfully.
     */
    public interface OnScanOutFinishedListener {
        /**
         * The method that is executed when the scan is done.
         *
         * @param dbm the result of the scan.
         */
        void onScanOutFinished(int dbm);
    }

    public static class OwaynFragmentFactory implements OwaynFragment.OwaynFragmentFactory {
        @Override
        public Fragment getInstance() {
            return new OwaynScanOutFragment();
        }
    }
}

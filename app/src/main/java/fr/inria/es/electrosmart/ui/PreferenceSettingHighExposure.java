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

package fr.inria.es.electrosmart.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceViewHolder;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;

public class PreferenceSettingHighExposure extends androidx.preference.Preference {
    private final String TAG = "PreferenceHighExposure";
    private AppCompatSeekBar mHighExposureSeekbar;
    private AppCompatTextView mSubtitleText, mTitleText;
    private ConstraintLayout mIncludedExposureScale;

    public PreferenceSettingHighExposure(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Initialize seek bar progress and subtitle text of exposure threshold to what is set in the
     * shared preference
     */
    public void initializeValues() {
        if (mHighExposureSeekbar != null) {
            mHighExposureSeekbar.setProgress(
                    SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD() - Const.MIN_DBM_FOR_ROOT_SCORE);
        }
        if (mSubtitleText != null) {
            mSubtitleText.setText(Tools.getExpositionInCurrentMetric(
                    SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD(), true));
        }
    }


    /**
     * Depending on the Android version, an icon space is reserved on the left of the setting when
     * there is no icon to display. This reserved space exists for recent versions of Android (at
     * least Android 5) to have a better alignment of the settings when some have a left icon and
     * others have no left icon.
     * <p>
     * If we want a correct alignment, we need to handle this case in our custom preference setting,
     * because it has no left icon.
     * <p>
     * The method .isIconSpaceReserved() on a Preference object returns true is a space is reserved
     * and false otherwise. We get the returns value of this method in a static variable
     * {@link SettingsPreferenceFragment#sIsIconSpaceReserved}
     * <p>
     * In case the icon space is reserved, we add padding to all components of this custom view.
     */
    private void setTextViewsPadding() {

        if (SettingsPreferenceFragment.sIsIconSpaceReserved) {
            int paddingInDip = (int) (
                    getContext().getResources().getDimension(R.dimen.preference_exposure_threshold_textviews_paddingLeft) /
                            getContext().getResources().getDisplayMetrics().density
            );
            Log.d(TAG, "setTextViewsPadding: " + paddingInDip);
            int paddingInPixel = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    paddingInDip,
                    getContext().getResources().getDisplayMetrics()
            );
            Log.d(TAG, "setTextViewsPadding: paddingInPixel = " + paddingInPixel);
            mTitleText.setPadding(paddingInPixel, 0, 0, 0);
            mSubtitleText.setPadding(paddingInPixel, 0, 0, 0);
            mHighExposureSeekbar.setPadding(paddingInPixel, 0, 0, 0);
            mIncludedExposureScale.setPadding(paddingInPixel, 0, 0, 0);
        }
    }

    /**
     * This method is where we get hold of the UI components that we declared in the custom
     * preference that we defined in
     * {@link fr.inria.es.electrosmart.R.layout#_preference_high_exposure_setting}
     *
     * @param holder A {@link androidx.preference.PreferenceViewHolder} instance that contains the
     *               components of the custom layout that we created
     */
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHighExposureSeekbar = (AppCompatSeekBar) holder.findViewById(R.id.high_exposure_seekbar);
        mIncludedExposureScale = (ConstraintLayout) holder.findViewById(R.id.included_exposure_scale);
        mTitleText = (AppCompatTextView) holder.findViewById(R.id.title_text);
        mSubtitleText = (AppCompatTextView) holder.findViewById(R.id.subtitle_text);

        // set the blue color to seek bar thumb and progress
        int thumbColor = ContextCompat.getColor(getContext(), R.color.default_blue);
        mHighExposureSeekbar.getThumb().setColorFilter(thumbColor, PorterDuff.Mode.SRC_ATOP);
        mHighExposureSeekbar.getProgressDrawable().setColorFilter(ContextCompat.getColor(getContext(),
                R.color.progress_bar_background_color), PorterDuff.Mode.SRC_ATOP);

        Log.d(TAG, "onBindViewHolder: pref dbm = " +
                SettingsPreferenceFragment.get_PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD());

        // initialize the exposure threshold value as set in the shared preference
        initializeValues();

        // add padding to textviews if needed
        setTextViewsPadding();

        mHighExposureSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // this method is used to update the timestamp when we scroll the seek bar
            @Override
            public void onProgressChanged(@NonNull SeekBar seekBar, int progress, boolean fromUser) {
                Log.d(TAG, "onProgressChanged: " + progress + ", fromUser = " + fromUser);
                Log.d(TAG, "onProgressChanged: dbm =" + (progress + Const.MIN_DBM_FOR_ROOT_SCORE) + ", fromUser = " + fromUser);
                mSubtitleText.setText(Tools.getExpositionInCurrentMetric((progress + Const.MIN_DBM_FOR_ROOT_SCORE), true));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mHighExposureSeekbar.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }

            @Override
            public void onStopTrackingTouch(@NonNull SeekBar seekBar) {
                mHighExposureSeekbar.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                // save the value in shared preference
                Log.d(TAG, "onStopTrackingTouch: Saving exposure threshold to " +
                        seekBar.getProgress());
                // Store the dbm value in the shared preference
                SettingsPreferenceFragment.set_PREF_KEY_NOTIFICATION_EXPOSURE_THRESHOLD(
                        seekBar.getProgress() + Const.MIN_DBM_FOR_ROOT_SCORE);
            }
        });
    }
}
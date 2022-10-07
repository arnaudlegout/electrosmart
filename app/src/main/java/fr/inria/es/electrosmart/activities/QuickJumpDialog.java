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

package fr.inria.es.electrosmart.activities;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.content.ContextCompat;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.util.TagGenerator;

/**
 * Used to offer a quick way to jump to a timestamp that is far away from the current
 * position. This dialog is opened when we click the chart or the text view below it.
 * The dialog shows a progress bar with a handle that can be used to select any date
 * the the chart is able to scroll to.
 */
public class QuickJumpDialog extends AppCompatDialog {
    private static final String TAG = "QuickJumpDialog";

    // The callback to be executed when the quick jump timestamp is selected.
    @NonNull
    private final OnResultListener mOnResultListener;

    // The timestamp that should appear as the selected timestamp when
    // we first open the dialog.
    @IntRange(from = 0)
    private final long mInitTimestamp;
    // The minimum timestamp that can be selected using this dialog
    @IntRange(from = 0)
    private final long mMinTimestamp;
    // The maximum timestamp that can be selected using this dialog
    @IntRange(from = 0)
    private final long mMaxTimestamp;
    // The granularity (The precision) of the timestamps that can be selected
    @IntRange(from = 0)
    private final long mGranularity;
    // The tag generator used to display the timestamps in the dialog
    @NonNull
    private final TagGenerator mTagGenerator;
    // The string to be used when we choose to jump to the last timestamp
    // when the jump to end mode is enabled
    @NonNull
    final private String mNow;
    // A boolean that specifies whether or not the jump to end mode is enabled
    final private boolean mEnableJumpToEnd;

    /**
     * Constructor.
     *
     * @param context         The Context the view is running in, through which it can access the
     *                        current theme, resources, etc.
     * @param initTimestamp   The timestamp that should appear as the selected timestamp when
     *                        we first open the dialog.
     * @param minTimestamp    The minimum timestamp that can be selected using this dialog
     * @param maxTimestamp    The maximum timestamp that can be selected using this dialog
     * @param listener        The callback to be executed when the quick jump is done
     * @param generator       The tag generator used to display the timestamps in the dialog
     * @param granularity     The granularity (The precision) of the timestamps that can be
     *                        selected
     * @param enableJumpToEnd A boolean that specifies whether or not the jump to end mode is
     *                        enabled
     */
    public QuickJumpDialog(@NonNull Context context, @IntRange(from = 0) long initTimestamp,
                           @IntRange(from = 0) long minTimestamp, @IntRange(from = 0) long maxTimestamp,
                           @NonNull OnResultListener listener, @NonNull TagGenerator generator,
                           @IntRange(from = 0) long granularity, boolean enableJumpToEnd) {
        super(context);
        mInitTimestamp = initTimestamp;
        mMinTimestamp = minTimestamp;
        mMaxTimestamp = maxTimestamp;
        mOnResultListener = listener;
        mTagGenerator = generator;
        mGranularity = granularity;
        mNow = context.getString(R.string.now);
        mEnableJumpToEnd = enableJumpToEnd;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_quick_jump);

        final AppCompatSeekBar seekBar = findViewById(R.id.timeSeekBar);
        if (mEnableJumpToEnd) {
            seekBar.setMax(timestampToProgress(mMaxTimestamp) + 1);
        } else {
            seekBar.setMax(timestampToProgress(mMaxTimestamp));
        }
        seekBar.setProgress(timestampToProgress(mInitTimestamp));

        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // The default layout params for the dialog's window is different across
        // different android devices. Here we force it so that it is consistent.
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        findViewById(R.id.dialog_quick_jump_wrapper).setOnClickListener(
                v -> cancel()
        );

        final TextView selectedDate = findViewById(R.id.selected_date);
        final TextView title = findViewById(R.id.quick_jump_dialog_title);

        // In the new complimentary themes, we have a default theme color, which we use to theme
        // non-text elements. The primary_text_color is used to theme text views
        int thumbColor, defaultTextColor;
        thumbColor = ContextCompat.getColor(getContext(), R.color.default_blue);
        defaultTextColor = ContextCompat.getColor(getContext(), R.color.primary_text_color);
        selectedDate.setTextColor(defaultTextColor);
        title.setTextColor(defaultTextColor);
        seekBar.getThumb().setColorFilter(thumbColor, PorterDuff.Mode.SRC_ATOP);
        seekBar.getProgressDrawable().setColorFilter(ContextCompat.getColor(getContext(),
                R.color.progress_bar_background_color), PorterDuff.Mode.SRC_ATOP);

        selectedDate.setText(mTagGenerator.get(
                mInitTimestamp
        ));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // this method is used to update the timestamp when we scroll the seek bar
            @Override
            public void onProgressChanged(@NonNull SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getProgress() == seekBar.getMax() && mEnableJumpToEnd) {
                    selectedDate.setText(mNow);
                } else {
                    selectedDate.setText(mTagGenerator.get(
                            progressToTimestamp(progress)
                    ));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            // this method is used to jump in the chart when we release the seek bar
            @Override
            public void onStopTrackingTouch(@NonNull SeekBar seekBar) {

                if (seekBar.getProgress() == seekBar.getMax()) {
                    mOnResultListener.onResult(new Result(Result.ACTION_JUMP_TO_END,
                            progressToTimestamp(seekBar.getProgress())));
                } else {
                    mOnResultListener.onResult(new Result(Result.ACTION_JUMP_TO_SELECTED_TIMESTAMP,
                            progressToTimestamp(seekBar.getProgress())));
                }
                cancel();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_MEASUREMENT_FRAGMENT_ON_QUICK_JUMP_DIALOG_START);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_MEASUREMENT_FRAGMENT_ON_QUICK_JUMP_DIALOG_STOP);
    }

    /**
     * Converts a timestamp to a progress to be set to the progressbar taking the minimum
     * timestamp and granularity into account.
     *
     * @param timestamp the timestamp to be converted to a progress
     * @return the progress that corresponds to the passed timestamp.
     */
    private int timestampToProgress(long timestamp) {
        return (int) ((timestamp - mMinTimestamp) / mGranularity);
    }

    /**
     * Converts the progress gotten from the progress bar to a timestamp taking the minimum
     * timestamp and granularity into account.
     *
     * @param progress the progress from the progress bar to be converted to a timestamp
     * @return The timestamp that corresponds to the passed progress
     */
    private long progressToTimestamp(int progress) {
        long timestamp = (progress * mGranularity + mMinTimestamp);
        timestamp = timestamp - timestamp % mGranularity;
        return timestamp;
    }

    /**
     * Defines an interface to the callback triggered when we select a timestamp
     */
    public interface OnResultListener {
        void onResult(Result result);
    }

    /**
     * Interface definition for a callback to be invoked when the quick jump timestamp
     * have been selected.
     */
    public class Result {

        // Action that says that the user choose to jump to the latest timestamp
        public static final int ACTION_JUMP_TO_END = 0;
        // Action that says that the user choose to jump to a specific timestamp
        // The timestamp is given with the getSelectedTimestamp()
        static final int ACTION_JUMP_TO_SELECTED_TIMESTAMP = 1;

        @IntRange(from = ACTION_JUMP_TO_END, to = ACTION_JUMP_TO_SELECTED_TIMESTAMP)
        private int mSelectedAction;
        @IntRange(from = 0)
        private long mSelectedTimestamp;

        Result(@IntRange(from = 0, to = 1) int selectedAction,
               @IntRange(from = 0) long selectedTimestamp) {
            mSelectedAction = selectedAction;
            mSelectedTimestamp = selectedTimestamp;
        }

        @IntRange(from = ACTION_JUMP_TO_END, to = ACTION_JUMP_TO_SELECTED_TIMESTAMP)
        public int getSelectedAction() {
            return mSelectedAction;
        }

        @IntRange(from = 0)
        public long getSelectedTimestamp() {
            return mSelectedTimestamp;
        }

    }
}

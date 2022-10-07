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

package fr.inria.es.electrosmart.chart;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * An object holding the chart's parameters that is the same for all the views
 */
class ChartParams {

    // The paint used to draw the curve representing existing data
    @NonNull
    final Paint mCurveTextPaint;
    // The paint used to draw the curve representing absent data
    @NonNull
    final Paint mCurveNoDataTextPaint;
    // The paint used to draw the shape in the background of the chart
    @NonNull
    final Paint mChartBackgroundPaint;

    // The chart's data structure
    @NonNull
    final ChartData mData;
    // A Rect object that holds the size dedicated for the chart on the screen
    @NonNull
    final Rect mWindowRect;
    // The horizontal size dedicated to draw one value of data
    @IntRange(from = 1)
    int mSlotWidth;
    // The maximum height that can be taken by text when drawn in the chart curve.
    // This parameter is used to adjust the position of the curves in order
    // to efficiently use the chart area without exceeding it and have truncated curves
    @IntRange(from = 1)
    int mCurveTextHeight;
    // The radius of the corners curvature in the chart background shape
    @IntRange(from = 0)
    int mBackgroundCornersRadius;
    // The current horizontal scroll position of the chart
    @IntRange(from = 0)
    int mScrollX;

    // The colors to be used to construct the gradient for the chart's curve.
    // These are the colors used by default. **They should not be changed here.**
    // Use the layout parameters or the corresponding method (`Chart#setCurvesColorGradient(...)`)
    // to change these values.
    @NonNull
    @ColorInt
    final int[] mCurvesColorGradient = {
            Color.WHITE, Color.WHITE, Color.GRAY, Color.GRAY, Color.BLACK, Color.BLACK,
    };
    // The positions where the colors should change for the chart's curve.
    // These are the default values. **They should not be changed here.**
    // Use the layout parameters or the corresponding method (`Chart#setCurvesColorGradient(...)`)
    // to change these values.
    @NonNull
    @FloatRange(from = 0.0f, to = 1.0f)
    final float[] mCurvesColorPosition = {
            0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f
    };

    /**
     * Constructor.
     *
     * @param data A {@code ChartData} object.
     */
    ChartParams(@NonNull ChartData data) {

        mCurveTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCurveNoDataTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mChartBackgroundPaint = new Paint();
        mChartBackgroundPaint.setAntiAlias(true);

        mData = data;

        mWindowRect = new Rect();
    }
}

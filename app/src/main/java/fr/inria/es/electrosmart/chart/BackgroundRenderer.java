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


import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;

/**
 * A renderer object to draw the shape in the background of the chart
 * <p>
 * Considers the drawing area to be
 * width = mParams.mWindowRect.width()
 * height = mParams.mWindowRect.height()
 * <p>
 * All Implemented Interfaces:
 * {@link Renderer}
 */
class BackgroundRenderer implements Renderer {

    // The width of one side (round rectangle) of the background shape
    private final float mSideWidth;
    // The height of the drawing area
    private final float mHeight;
    // The width of the drawing area
    private final float mWidth;
    // The radius of the corners curvature
    private final float mCircleRadius;

    // The paint used to draw the background
    @NonNull
    private final Paint mPaint;

    // A RectF object used to hold the size of the round corners semi circles
    @NonNull
    private final RectF mOval;

    /**
     * Constructor.
     *
     * @param params a ChartParams object that is used to retrieve the chart sizes
     *               (such as height and width and slot width)
     */
    BackgroundRenderer(@NonNull ChartParams params) {
        mPaint = params.mChartBackgroundPaint;

        mSideWidth = (params.mWindowRect.width() - params.mSlotWidth) / 2;
        mHeight = params.mWindowRect.height();
        mWidth = params.mWindowRect.width();
        mCircleRadius = params.mBackgroundCornersRadius;

        mOval = new RectF();
    }


    /**
     * Draws the background.
     *
     * @param canvas the canvas to draw on.
     */
    @Override
    public void draw(@NonNull Canvas canvas) {

        // Draw left-hand side rectangle
        canvas.drawRect(
                0,
                0,
                mSideWidth - mCircleRadius,
                mHeight,
                mPaint
        );

        canvas.drawRect(
                mSideWidth - mCircleRadius,
                mCircleRadius,
                mSideWidth,
                mHeight - mCircleRadius,
                mPaint
        );

        mOval.set(
                mSideWidth - 2 * mCircleRadius,
                0,
                mSideWidth,
                2 * mCircleRadius);
        canvas.drawArc(
                mOval,
                0, -90, true,
                mPaint
        );

        mOval.set(
                mSideWidth - 2 * mCircleRadius,
                mHeight - 2 * mCircleRadius,
                mSideWidth,
                mHeight);
        canvas.drawArc(
                mOval,
                0, 90, true,
                mPaint
        );

        //Draw right-hand side rectangle
        canvas.drawRect(
                mWidth - mSideWidth + mCircleRadius,
                0,
                mWidth,
                mHeight,
                mPaint
        );

        canvas.drawRect(
                mWidth - mSideWidth,
                mCircleRadius,
                mWidth - mSideWidth + mCircleRadius,
                mHeight - mCircleRadius,
                mPaint
        );

        mOval.set(
                mWidth - mSideWidth,
                0,
                mWidth - mSideWidth + 2 * mCircleRadius,
                2 * mCircleRadius);
        canvas.drawArc(
                mOval,
                180, 90, true,
                mPaint
        );

        mOval.set(
                mWidth - mSideWidth,
                mHeight - 2 * mCircleRadius,
                mWidth - mSideWidth + 2 * mCircleRadius,
                mHeight);
        canvas.drawArc(
                mOval,
                180, -90, true,
                mPaint
        );
    }
}

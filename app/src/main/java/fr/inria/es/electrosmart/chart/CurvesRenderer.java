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
import android.graphics.Path;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;


/**
 * A {@link Renderer} object that implements the algorithm to draw the curves. Because the
 * curve style changes according to the existence of the data in the chart's data structure,
 * this implementation takes a {@link Rule} object and a {@link Paint} object as a parameter
 * to be able to decide using the rule object if it should draw the curve using the paint object.
 * <p>
 * Considers the drawing area to be
 * width = mViewParams.mViewWidth
 * height = mViewParams.mViewHeight
 * <p>
 * All Implemented Interfaces:
 * {@link Renderer}
 */
class CurvesRenderer implements Renderer {

    // A ChartViewParams object used to hold parameters specific to this view
    // It also implements a method that does calculations that are common to
    // all the views (the results of these calculations are different for each view)
    @NonNull
    private final ChartViewParams mViewParams;
    // A ChartParams object holding parameters common to all views
    // such as the width of a slot, the chart's area size...
    @NonNull
    private final ChartParams mParams;
    // The rule to be used to decide whether or not to draw a segment of the curve
    @NonNull
    private final Rule mRule;
    // The paint to be used to draw the curve
    @NonNull
    private final Paint mPaint;

    /**
     * Constructor.
     *
     * @param viewParams a {@code ChartViewParams} object used to hold parameters specific to
     *                   a view.
     * @param params     a {@code ChartParams} object holding parameters common to all views
     *                   such as the width of a slot, the chart's area size...
     * @param rule       the {@code Rule} object to decide if it should draw a segment of the
     *                   curve
     * @param paint      the {@code Paint} object to be used to draw the curve
     */
    CurvesRenderer(@NonNull ChartViewParams viewParams, @NonNull ChartParams params,
                   @NonNull Rule rule, @NonNull Paint paint) {
        mViewParams = viewParams;
        mParams = params;
        mRule = rule;
        mPaint = paint;
    }

    /**
     * An interface for objects defining the rule to be used to decide whether or not we
     * should draw a segment of the curve.
     */
    interface Rule {
        /**
         * Tels the renderer whether or not to draw the segment that corresponds to these
         * parameters in the chart's data structure.
         *
         * @param index the index of the segment
         * @param label the label of the segment
         * @return true if the segment should be drawn and false otherwise
         */
        boolean isTrue(@IntRange(from = 0) int index, String label);
    }

    /**
     * Draws the curves according to the parameters in {@code mViewParams} and {@code mParams}.
     *
     * @param canvas the canvas to draw on.
     */
    @Override
    public void draw(@NonNull Canvas canvas) {
        for (String label : mParams.mData.getLabels()) {

            // Retrieve the path object (the curve representation) and string that should
            // be drawn over the curve that was already calculated and stored in
            // `mViewParams`.
            Path path = mViewParams.mPaths.get(label);
            String text = mViewParams.mCurveTexts.get(label);

            int offset = mViewParams.mFirstViewSlot;
            int size = mViewParams.mViewSlotCount;
            int slotWidth = mParams.mSlotWidth;

            // According to the `mRule` object the curve is drawn
            // as chunks for which mRule.isTrue(index, label) is true.
            // Each iteration of the following loop determines a chunk that
            // gets drawn at the end.
            for (int i = 0; i < size; i++) {

                // Locate the first slot of the chunk
                for (; i < size; i++) {
                    if (mRule.isTrue(i + offset, label)) {
                        break;
                    }
                }

                // Calculate the width of the chunk
                int chunkWidth = 0;
                int chunkOffset = slotWidth * i;
                for (; i < size; i++) {
                    if (!mRule.isTrue(i + offset, label)) {
                        break;
                    }
                    chunkWidth += slotWidth;
                }

                // Draw the chunk if it's not empty
                if (chunkWidth != 0) {
                    canvas.save();
                    canvas.clipRect(
                            chunkOffset,
                            0,
                            chunkOffset + chunkWidth,
                            mViewParams.mViewHeight
                    );
                    canvas.drawTextOnPath(text, path, 0,
                            mParams.mCurveTextHeight / 2, mPaint);
                    canvas.restore();
                }
            }
        }
    }
}

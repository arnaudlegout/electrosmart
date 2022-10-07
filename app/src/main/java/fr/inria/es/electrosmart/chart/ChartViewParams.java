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

import android.graphics.Path;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Provides a holder for all the data structures necessary to perform the processing
 * while generating chart views, graphics. For performance reasons, we instantiate one object
 * of this class. And pass it to the views to perform their calculations on it instead of
 * creating new data structures for every view.
 * <p>
 * The {@code init(int)} method does some of the processing that is common for all view types.
 * Such as, calculating the number of slots and creating the curves paths.
 * <p>
 * The views then are left with graphics rendering since it's view type
 * and android version dependent.
 */
class ChartViewParams {

    // The Path objects (A representation of the curve) to be drawn for each label in this view
    @NonNull
    final Map<String, Path> mPaths;
    // The text to be drawn over the curve for each label in this view
    @NonNull
    final Map<String, String> mCurveTexts;
    // The dimensions object to be used to calculate the views dimensions, slot numbers
    // and slot positions...
    @NonNull
    private final ChartDimensions mDimensions;
    // The chart params object to retrieve paint objects, data and slot width...
    @NonNull
    private final ChartParams mParams;
    // The object used to generate the path objects (the curves)
    @NonNull
    private final ChartPath mChartPath;
    // The object used to generate the text drawn over the curves
    @NonNull
    private final CurveText mCurveText;
    // The width of the current view
    @IntRange(from = 1)
    int mViewWidth;
    // The height of the current view
    @IntRange(from = 1)
    int mViewHeight;
    // The number of slots in the current view
    @IntRange(from = 0)
    int mViewSlotCount;
    // The absolute index of the first slot in the current view
    @IntRange(from = 0)
    int mFirstViewSlot;

    /**
     * Constructor.
     *
     * @param params     A {@code ChartParams} object.
     * @param dimensions A {@code ChartDimensions} object.
     * @param chartPath  A {@code ChartPath} object.
     * @param curveText  A {@code CurveText} object.
     */
    ChartViewParams(@NonNull ChartParams params, @NonNull ChartDimensions dimensions,
                    @NonNull ChartPath chartPath, @NonNull CurveText curveText) {
        mPaths = new HashMap<>(10);
        mCurveTexts = new HashMap<>(10);
        mDimensions = dimensions;
        mParams = params;
        mChartPath = chartPath;
        mCurveText = curveText;
    }

    /**
     * Initializes parameters necessary for views rendering
     *
     * @param index the index of the view
     */
    void init(int index) {

        mViewWidth = mDimensions.getViewWidth(index);
        mViewHeight = mParams.mWindowRect.height();

        mViewSlotCount = mDimensions.getViewSlotCount(index);
        mFirstViewSlot = mDimensions.getFirstViewSlot(index);

        for (String label : mParams.mData.getLabels()) {
            Path path = mPaths.get(label);
            if (path == null) {
                path = new Path();
                mPaths.put(label, path);
            }
            mChartPath.get(mFirstViewSlot, mViewSlotCount, label, path);
            mCurveTexts.put(label, mCurveText.get(label, path, index));
        }
    }
}

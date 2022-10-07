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


import android.content.Context;

import androidx.annotation.NonNull;

/**
 * A factory for generating standard chart views ({@link ChartViewImplAPI26}) implementing
 * the work around for the bug introduced in Android 8.
 * <p>
 * All Implemented Interfaces:
 * {@link ChartViewFactory}
 */
class ChartViewFactoryImplAPI26 implements ChartViewFactory {


    // A ChartParams object holding parameters common to all views
    // such as the width of a slot, the chart's area size...
    @NonNull
    private final ChartParams mParams;
    @NonNull
    private final ChartViewParams mViewParams;
    // The renderer object that is used to draw the part of the curves that corresponds
    // to existing data values in the chart's data structure
    @NonNull
    private final Renderer mCurvesRenderer;
    // The renderer object that is used to draw the part of the curves that corresponds
    // to absent data values in the chart's data structure
    @NonNull
    private final Renderer mNoDataCurvesRenderer;


    /**
     * Constructor.
     *
     * @param params     A {@code ChartParams} object holding parameters common to all views
     *                   such as the width of a slot, the chart's area size...
     * @param viewParams A {@code ChartViewParams} object used to hold parameters specific to
     *                   a view.
     */
    ChartViewFactoryImplAPI26(@NonNull ChartParams params, @NonNull ChartViewParams viewParams) {
        mParams = params;
        mViewParams = viewParams;
        mCurvesRenderer = new CurvesRenderer(mViewParams, params, (index, label) ->
                mParams.mData.get(index, label) != -1, params.mCurveTextPaint);
        mNoDataCurvesRenderer = new CurvesRenderer(mViewParams, params, (index, label) ->
                mParams.mData.get(index, label) == -1, params.mCurveNoDataTextPaint);
    }


    /**
     * Generates a chart view for API 26 and above.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @return A newly generated chart view for API 26 and above.
     */
    @Override
    public ChartView getView(Context context) {
        return new ChartViewImplAPI26(context, mParams, mViewParams, mCurvesRenderer,
                mNoDataCurvesRenderer);
    }
}

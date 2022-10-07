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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An implementation of chart view backed by a bitmap.
 * <p>
 * All Implemented Interfaces:
 * {@link ChartView}
 */
class ChartViewImpl extends ChartView {


    // A ChartViewParams object used to hold parameters specific- in onCreate: don't we need to shrink chart to the right also when we do removeValuesRight(Const.CHART_MAX_SLOTS_HISTORY);
    //We shring the chart to the left when we call removeValuesLeft()CVI to this view
    // It also implements a method that does calculations that are common to
    // all the views (the results of these calculations are different for each view)
    @NonNull
    private final ChartViewParams mViewParams;
    // The canvas object that is used to draw on the bitmap
    @NonNull
    private final Canvas mCanvas;
    // The renderer object that is used to draw the part of the curves that corresponds
    // to existing data values in the chart's data structure
    @NonNull
    private final Renderer mCurvesRenderer;
    // The renderer object that is used to draw the part of the curves that corresponds
    // to absent data values in the chart's data structure
    @NonNull
    private final Renderer mNoDataCurvesRenderer;
    // The bitmap that holds the view's drawing
    @Nullable
    private Bitmap mBitmap;
    // A boolean that indicates if the view has already been drawn or still have to draw itself
    private boolean mReady;

    /**
     * Constructor.
     *
     * @param context              The Context the view is running in, through which it can access
     *                             the current theme, resources, etc.
     * @param viewParams           a {@code ChartViewParams} object used to hold parameters specific to
     *                             a view.
     * @param curvesRenderer       a {@code Renderer} object for rendering the curves
     * @param noDataCurvesRenderer a {@code Renderer} object for rendering the curves
     *                             with no data.
     */
    public ChartViewImpl(@NonNull Context context, @NonNull ChartViewParams viewParams,
                         @NonNull Renderer curvesRenderer, @NonNull Renderer noDataCurvesRenderer) {
        this(context, null, viewParams, curvesRenderer, noDataCurvesRenderer);
    }

    /**
     * Constructor that is called when inflating from XML.
     *
     * @param context              The Context the view is running in, through which it can
     *                             access the current theme, resources, etc.
     * @param viewParams           a {@code ChartViewParams} object
     * @param curvesRenderer       a {@code Renderer} object for rendering the curves
     * @param noDataCurvesRenderer a {@code Renderer} object for rendering the curves
     *                             with no data.
     */
    private ChartViewImpl(@NonNull Context context, @Nullable AttributeSet attrs,
                          @NonNull ChartViewParams viewParams, @NonNull Renderer curvesRenderer,
                          @NonNull Renderer noDataCurvesRenderer) {
        super(context, attrs);
        mViewParams = viewParams;
        mCurvesRenderer = curvesRenderer;
        mNoDataCurvesRenderer = noDataCurvesRenderer;
        mCanvas = new Canvas();
        mReady = false;
    }


    /**
     * Prepares the view for the next on draw event.
     * It is not necessary to call this method before a "on draw" event has occurred.
     * The "on draw" event will automatically trigger initialization if it is not
     * already initialized.
     */
    @Override
    void init() {

        if (mReady) {
            return;
        }

        if (mFake) {
            return;
        }

        recycleBitmap();
        mViewParams.init(mViewIndex);
        buildBitmap();
        mReady = true;
    }

    /**
     * Creates the view's bitmap and attaches it to {@code mCanvas}.
     */
    private void buildBitmap() {
        mBitmap = Bitmap.createBitmap(mViewParams.mViewWidth, mViewParams.mViewHeight,
                Bitmap.Config.ARGB_8888);

        mCanvas.setBitmap(mBitmap);
        drawView(mCanvas);
    }

    /**
     * Draw on the bitmap.
     *
     * @param canvas the canvas attached to the
     */
    private void drawView(Canvas canvas) {
        mNoDataCurvesRenderer.draw(canvas);
        mCurvesRenderer.draw(canvas);
    }


    /**
     * Clears the view internal state rendering it ready to be recycled
     */
    @Override
    void clear() {
        recycleBitmap();
        mReady = false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {

        if (mFake) {
            return;
        }

        init();
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

    }

    /**
     * Clears the bitmaps for reuse.
     */
    private void recycleBitmap() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;
        mCanvas.setBitmap(null);
    }

}

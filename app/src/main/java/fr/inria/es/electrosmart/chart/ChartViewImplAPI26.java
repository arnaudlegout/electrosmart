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
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An implementation of chart view for API 26 and above.
 * Work around bug described in:
 * <a href="https://issuetracker.google.com/issues/68500073">
 * https://issuetracker.google.com/issues/68500073
 * </a>
 * <p>
 * The bug is that a paint with a color gradient does not work correctly with
 * the {@link Canvas#drawTextOnPath(String, Path, float, float, Paint)}. The implemented work
 * around draws the curve on an alpha channel bitmap and then the bitmap is drawn on the view
 * with the paint using the color gradient.
 * <p>
 * Note:
 * This implementation uses more memory than {@link ChartViewImpl} since it is backed with
 * two bitmaps, we should make sure it's only enabled on android versions where it's necessary.
 * <p>
 * All Implemented Interfaces:
 * {@link ChartView}
 */
class ChartViewImplAPI26 extends ChartView {

    // A ChartParams object holding parameters common to all views
    // such as the width of a slot, the chart's area size...
    @NonNull
    private final ChartParams mParams;
    // A ChartViewParams object used to hold parameters specific to this view
    // It also implements a method that does calculations that are common to
    // all the views (the results of these calculations are different for each view)
    @NonNull
    private final ChartViewParams mViewParams;
    // The canvas object used to perform drawing operations on the background bitmap
    @NonNull
    private final Canvas mBackgroundCanvas;
    // The canvas object used to perform drawing operations on the foreground bitmap
    @NonNull
    private final Canvas mForegroundCanvas;
    // The renderer object that is used to draw the part of the curves that corresponds
    // to existing data values in the chart's data structure
    @NonNull
    private final Renderer mCurvesRenderer;
    // The renderer object that is used to draw the part of the curves that corresponds
    // to absent data values in the chart's data structure
    @NonNull
    private final Renderer mNoDataCurvesRenderer;
    // A reference to bitmap that is put in background of the view
    @Nullable
    private Bitmap mBackgroundBitmap;
    // A reference to bitmap that is put in foreground of the view
    @Nullable
    private Bitmap mForegroundBitmap;
    // A boolean that indicates if the view has already been drawn or still have to draw itself
    private boolean mReady;

    /**
     * Constructor.
     *
     * @param context              The Context the view is running in, through which it can access
     *                             the current theme, resources, etc.
     * @param params               a {@code ChartParams} object holding parameters common to all views
     *                             such as the width of a slot, the chart's area size...
     * @param viewParams           a {@code ChartViewParams} object used to hold parameters specific to
     *                             a view.
     * @param curvesRenderer       a {@code Renderer} object for rendering the curves.
     * @param noDataCurvesRenderer a {@code Renderer} object for rendering the curves with no data.
     */
    public ChartViewImplAPI26(@NonNull Context context, @NonNull ChartParams params,
                              @NonNull ChartViewParams viewParams, @NonNull Renderer curvesRenderer,
                              @NonNull Renderer noDataCurvesRenderer) {
        this(context, null, params, viewParams, curvesRenderer, noDataCurvesRenderer);
    }

    /**
     * Constructor that is called when inflating from XML.
     *
     * @param context              The Context the view is running in, through which it can
     *                             access the current theme, resources, etc.
     * @param params               a {@code ChartParams} object
     * @param viewParams           a {@code ChartViewParams} object
     * @param curvesRenderer       a {@code Renderer} object for rendering the curves
     * @param noDataCurvesRenderer a {@code Renderer} object for rendering the curves
     *                             with no data.
     */
    private ChartViewImplAPI26(@NonNull Context context, @Nullable AttributeSet attrs,
                               @NonNull ChartParams params, @NonNull ChartViewParams viewParams,
                               @NonNull Renderer curvesRenderer,
                               @NonNull Renderer noDataCurvesRenderer) {
        super(context, attrs);
        mParams = params;
        mViewParams = viewParams;
        mCurvesRenderer = curvesRenderer;
        mNoDataCurvesRenderer = noDataCurvesRenderer;
        mBackgroundCanvas = new Canvas();
        mForegroundCanvas = new Canvas();
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

        recycleBitmaps();
        mViewParams.init(mViewIndex);
        buildBitmaps();
        mReady = true;
    }

    /**
     * Builds and draws all the needed bitmaps
     */
    private void buildBitmaps() {
        buildBackgroundBitmap();
        buildForegroundBitmap();
    }

    /**
     * Build the and draws the foreground bitmap
     */
    private void buildForegroundBitmap() {
        mForegroundBitmap = Bitmap.createBitmap(mViewParams.mViewWidth, mViewParams.mViewHeight,
                Bitmap.Config.ALPHA_8);
        mForegroundCanvas.setBitmap(mForegroundBitmap);
        drawForeground(mForegroundCanvas);
    }

    /**
     * Builds and draws the background bitmap
     */
    private void buildBackgroundBitmap() {
        mBackgroundBitmap = Bitmap.createBitmap(mViewParams.mViewWidth, mViewParams.mViewHeight,
                Bitmap.Config.ARGB_8888);
        mBackgroundCanvas.setBitmap(mBackgroundBitmap);
        drawBackground(mBackgroundCanvas);
    }

    /**
     * Draws the background bitmap
     */
    private void drawBackground(Canvas canvas) {
        mNoDataCurvesRenderer.draw(canvas);
    }


    /**
     * Draws the foreground bitmap
     */
    private void drawForeground(Canvas canvas) {
        mCurvesRenderer.draw(canvas);
    }


    /**
     * Clears the view internal state rendering it ready to be recycled
     */
    @Override
    void clear() {
        recycleBitmaps();
        mReady = false;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {

        if (mFake) {
            return;
        }

        init();

        if (mBackgroundBitmap != null && mForegroundBitmap != null) {
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, null);
            canvas.drawBitmap(mForegroundBitmap, 0, 0, mParams.mCurveTextPaint);
        }

    }

    /**
     * Releases all the bitmaps for reuse.
     */
    private void recycleBitmaps() {

        if (mBackgroundBitmap != null && !mBackgroundBitmap.isRecycled()) {
            mBackgroundBitmap.recycle();
        }

        if (mForegroundBitmap != null && !mForegroundBitmap.isRecycled()) {
            mForegroundBitmap.recycle();
        }

        mBackgroundBitmap = null;
        mForegroundBitmap = null;
        mBackgroundCanvas.setBitmap(null);
        mForegroundCanvas.setBitmap(null);
    }
}

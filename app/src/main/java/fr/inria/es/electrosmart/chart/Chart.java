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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import fr.inria.es.electrosmart.R;

/**
 * Implementation of the chart using a RecyclerView.
 * <p>
 * In order to make the chart capable of representing a long array of data without using a lot
 * of memory, this implementation of the chart is using multiple views ({@link ChartView}s)
 * that are stacked together to form the chart.
 * <p>
 * Since the recycler view offers great performance for long scrollable list of views, using smart
 * techniques such as recycling, caching, pooling... we took advantage of these capabilities
 * to build the chart. Moreover, the recycler view is extended to be able to manage additional
 * features such as scroll position tracking and events dispatching...
 * <p>
 * One of the essential parts of the chart is the data structure. It is represented by
 * {@link ChartData}. For more information about the data structure, see the documentation
 * in the class.
 * <p>
 * Secondly the chart is composed of views ({@link android.view.View} objects). However we
 * had to extend its interface adding some additional methods. There is different types
 * of views that are used in the chart (Different implementations). The {@link ChartView}
 * abstract class defines a uniform way to communicate with them without the need to know
 * which type it is.
 * <p>
 * Views are also composed of slots. A slot is the space in the view that is dedicated to represent
 * the data held by one index in the data structure.
 * <p>
 * Because the chart is represented to the user (the apps user) and client (the code) as one long
 * scrollable view whereas it is implemented using separated views, we need a way to connect
 * between the views to keep the consistency. This is why we have {@link ChartDimensions}.
 * The chart dimensions module
 * uses the chart's data structure to tell the adapter the size of each view. It also tels the
 * views what are the slots it is supposed to represent. Therefore, it knows which part
 * of the data it needs to retrieve to draw itself. From the other side we have the {@link CurveText}
 * that generates the strings that are used to draw the curves in such a way that the strings for
 * all the views looks as one long string when joined at the borders of every view.
 * <p>
 * The {@link ChartPath} is used to generate
 * the curves ({@link android.graphics.Path} objects) used to draw the chart. It also keeps the
 * consistency in the the views junctions keeping the same curve slope so that the curves look
 * smooth on the views boarders.
 * <p>
 * The {@link Renderer}s objects define algorithms used to draw shapes, text, curves...
 * on a view's canvas (A {@link Canvas} object). The {@link CurvesRenderer} is responsible for
 * drawing the curve. The {@link BackgroundRenderer} is used to draw the background of the chart.
 * <p>
 * The different type of views are:
 * <p>
 * - A standard view: Implemented in {@link ChartViewImpl} or {@link ChartViewImplAPI26}
 * which is a work around to a bug that have been introduced in Android 8. The maximum width of
 * the views is calculated at the initialization of the chart. The views, when created
 * they start with the size of a slot, and they keep growing until they reach the maximum size.
 * Another view is then created to represent the rest of the data. This is why the
 * {@link ChartAdapter} differentiates between two types of standard views.
 * The first one is the views that have already reached their maximum size and do not need to
 * change their width anymore. These are identified using the constant
 * {@link ChartAdapter#VIEW_TYPE_STANDARD}.
 * The second type is called standard last (or just last for short) and it is the last
 * (At the right most) standard view in the list and that did not yet reach the maximum width
 * and it is prone to grow in width if the data structure expands. It is identified with
 * {@link ChartAdapter#VIEW_TYPE_LAST}.
 * <p>
 * - A placeholder view: Implemented in {@link ChartViewPlaceholder} are views used to fill the space
 * in the chart that cannot be at center of the screen. All the slots in the chart
 * should be selectable, ie., the chart can be scrolled in such a way to make the slot at
 * the center of the screen. Therefore the most left and the most right part of the chart
 * cannot be used to represent slots. This is why this space is filled with a placeholder
 * view. The {@link ChartAdapter} differentiates between the left placeholder and the right
 * placeholder because they can have different width in some circumstances (if the
 * {@code chart_width - slot_width} is an odd number).
 * <p>
 * All Implemented Interfaces:
 * {@link Chart}
 */
public class Chart extends RecyclerView {

    // An object holding the parameters of the chart such as paints, size and scroll position...
    @NonNull
    private final ChartParams mParams;
    @NonNull
    // The callback that is executed when we click on the chart views.
    // This object is propagated to the chart adapter and the adapter is responsible
    // for attaching it to every view it creates.
    private final ChartAdapter.OnItemClickListener mOnItemClickListener;
    @Nullable
    // The recycler view adapter
    private ChartAdapter mAdapter;
    // The callback that is executed when the chart stops scrolling
    @Nullable
    private Chart.OnScrollEndListener mOnScrollEndListener;
    // The callback that is executed when the chart's position changes
    @Nullable
    private OnPositionChangedListener mOnPositionChangedListener;
    // The callback that gets executed whenever we click on the chart.
    @Nullable
    private Chart.OnClickListener mOnClickListener;
    @Nullable
    // The object that is responsible for drawing the background of the chart
    private Renderer mBackgroundRenderer;
    @Nullable
    // An object responsible of calculating dimensions of the chart components
    // such as views' sizes and scroll positions
    // It acts as an adapter around a ChartData object
    private ChartDimensions mDimensions;
    private boolean mReady = false;

    // The string used between two labels when generating the curve text
    // This is the value used by default. **Do not change it here**. Use the layout parameter
    // or the corresponding method (`Chart#setSeparator(String)`).
    @NonNull
    private String mSeparator = "\u2014";

    // Saves the position to scroll to after the chart is dimensioned
    // Used only in case scrollTo(int index) is called before the chart is ready (Dimensioned and laid out)
    private int mInitPosition = -1;

    /**
     * Constructor that is called when inflating from XML.
     *
     * @param context The Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public Chart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);


        // Initialize objects and data structures and attach listeners

        ChartData data = new ChartData();

        mParams = new ChartParams(data);

        mParams.mScrollX = 0;

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                final int oldPosition = position();
                mParams.mScrollX += dx;
                final int newPosition = position();
                if (oldPosition != newPosition && oldPosition >= 0 && newPosition >= 0) {
                    runOnPositionChangedListener(oldPosition, newPosition);
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int state) {
                switch (state) {
                    case SCROLL_STATE_IDLE:
                        if (stickToSlot()) {
                            onScrollEnd();
                        }
                        break;
                }
            }
        });


        // use a horizontal linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL,
                false);
        // Force enabling prefetch
        layoutManager.setItemPrefetchEnabled(true);
        // Increase the size of the prefetched items
        layoutManager.setInitialPrefetchItemCount(20);

        setLayoutManager(layoutManager);

        // Improving performance since we know that changes
        // in content do not change the layout size of the RecyclerView
        setHasFixedSize(true);
        // Improving performance by increasing the cache size
        setItemViewCacheSize(20);

        // Only one view of type VIEW_TYPE_LAST is needed
        // Tell the pool to create hold one of this type
        getRecycledViewPool().setMaxRecycledViews(ChartAdapter.VIEW_TYPE_LAST, 1);
        // Similarly only two placeholder views are needed
        getRecycledViewPool().setMaxRecycledViews(ChartAdapter.VIEW_TYPE_LEFT_PLACEHOLDER, 1);
        getRecycledViewPool().setMaxRecycledViews(ChartAdapter.VIEW_TYPE_RIGHT_PLACEHOLDER, 1);

        // Disable all recycler view animations
        setItemAnimator(null);

        mOnItemClickListener = this::runOnClickListener;

        // parse XML view params
        if (attrs != null) {
            obtainDefaultCustomAttributes(attrs);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        if (w > 0 && h > 0 && w != oldWidth && h != oldHeight) {
            getReady();
        }
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        drawBackground(canvas);
        super.dispatchDraw(canvas);
    }

    /**
     * The final initialization steps that cannot be done until we know the size
     * that is allocated for the chart on the screen. This should be called every
     * time the size of the chart changes (This is why it is called inside
     * {@link #onSizeChanged(int, int, int, int)}).
     */
    private void getReady() {

        mParams.mScrollX = 0;
        mParams.mWindowRect.set(0, 0, getWidth(), getHeight());
        mParams.mCurveTextHeight = curveTextHeight();
        mDimensions = new ChartDimensions(mParams.mData, mParams.mWindowRect.width(),
                mParams.mSlotWidth);
        mAdapter = new ChartAdapter(mParams, mDimensions, new ChartPath(mParams),
                mOnItemClickListener, new CurveText(mParams, mSeparator));
        setAdapter(mAdapter);
        mBackgroundRenderer = new BackgroundRenderer(mParams);

        mReady = true;

        if (mInitPosition != -1) {
            scrollTo(mInitPosition);
            mInitPosition = -1;
        }
    }

    /**
     * Draws the chart background using the {@code mBackgroundRenderer} strategy.
     *
     * @param canvas the canvas to draw on.
     */
    private void drawBackground(@NonNull Canvas canvas) {
        if (mBackgroundRenderer != null) {
            mBackgroundRenderer.draw(canvas);
        }
    }

    /**
     * Calculates the text space that should be accounted for to draw the curves. The calculation
     * is based on the curves text size ({@link #setCurvesTextSize(float)}).
     *
     * @return the text height
     */
    private int curveTextHeight() {

        Rect tmp = new Rect();

        // Only the height of the text is useful.
        // So we are using a upper case (to account for the largest height possible) dummy
        // string to calculate the bounds
        String str = "DUMMY";

        mParams.mCurveTextPaint.getTextBounds(str, 0, str.length(), tmp);

        int res = tmp.height();

        mParams.mCurveNoDataTextPaint.getTextBounds(str, 0, str.length(), tmp);
        if (res < tmp.height()) {
            res = tmp.height();
        }

        return res;
    }

    /**
     * Inserts a value into the data structure or overrides the old value if a value was inserted
     * before with the same {@code index}, and {@code label}. {@code value} -1 is used to just
     * increase the size of the chart if the index is greater than the current (size - 1) without
     * setting any value.
     *
     * @param index the index of the value
     * @param label the label of the value
     * @param value the value to insert
     * @throws IllegalArgumentException if {@code label == null}
     * @throws IllegalArgumentException if {@code value < -1 || value > 100}
     * @throws IllegalArgumentException if {@code index < 0}
     */
    public void put(@IntRange(from = 0) int index, @NonNull String label,
                    @IntRange(from = -1, to = 100) int value) {
        boolean firstUpdate = mParams.mData.size() == 0;
        mParams.mData.put(index, label, value);
        if (firstUpdate) {
            runOnPositionChangedListener(-1, 0);
        }
        if (mReady && mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Shrinks the chart to match the specified size if the current size is bigger.
     * The shrinkLeft is done by removing values to the left (from index 0) and shifting the indexes
     * to the left by the sizes difference.
     * <p>
     * If the slot the chart is positioned on is removed while shrinking, the chart simply jumps (scrolls to
     * the first slot available, ie. slot of index 0). In this case the
     * {@link fr.inria.es.electrosmart.chart.Chart.OnPositionChangedListener} and
     * {@link fr.inria.es.electrosmart.chart.Chart.OnScrollEndListener} are notified if they are set.
     * <p>
     * We did not implement a shrinkRight for the following reasons
     * - In live mode only shrinkLeft is used (we never need to shrink right in live). Note that
     * this is in live that the shrinking is critical and must be efficient as we must shrink
     * every 5 seconds when the max size of the chart is reached.
     * - In history mode, we might need to shrink right. We currently, only reset the chart each
     * time, but as it is done infrequently it is not a performance issue.
     * <p>
     * Implementation note: shrinking left means that we can also add values right. This is natural
     * for any sequence data structures. Shrinking right, means adding values at left, this is not
     * possible straight away with a sequence. We might change the data structure, but the
     * performance benefit does not mandate it.
     *
     * @param size the new size
     */
    public void shrinkLeft(@IntRange(from = 0) int size) {

        final int oldSize = mParams.mData.size();

        if (oldSize < size) {
            return;
        }

        int shift = oldSize - size;

        mParams.mData.shrinkLeft(size);

        int oldPosition = position();

        if (mReady && mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            if (position() - shift >= 0) {
                // Readjust the position of the chart if the old data is still in the chart
                Chart.OnScrollEndListener l = mOnScrollEndListener;
                // Remove the listener to not trigger the event
                mOnScrollEndListener = null;
                scrollTo(position() - shift);
                // Re enable the listener
                mOnScrollEndListener = l;
            } else {
                // The data in the old position was removed, we then scroll to the beginning
                // This is done automatically by the recycler view. But the we need to notify the listeners.
                runOnScrollEndListener(false);
                runOnPositionChangedListener(oldPosition, 0);
            }
        }
    }

    /**
     * Sets a listener to the scroll end events. This listener is also triggered when we call
     * {@link #shrinkLeft(int)} and the old position of the chart is removed.
     *
     * @param listener the listener to be executed when the event is triggered
     */
    public void setOnScrollEndListener(@Nullable Chart.OnScrollEndListener listener) {
        mOnScrollEndListener = listener;
    }

    /**
     * Sets a listener to the chart position changes events. This listener is also triggered when we
     * call {@link #shrinkLeft(int)} and the old position of the chart is removed.
     *
     * @param listener the listener to be executed when the event is triggered
     */
    public void setOnPositionChangedListener(@Nullable OnPositionChangedListener listener) {
        mOnPositionChangedListener = listener;
    }

    /**
     * Sets a listener to click events
     *
     * @param listener the listener to be executed when the event is triggered
     */
    public void setOnClickListener(@Nullable Chart.OnClickListener listener) {
        mOnClickListener = listener;
    }

    /**
     * Runs the on scroll end listener
     *
     * @param fromUser indicates whether the scroll was performed by the user or programmatically.
     */
    private void runOnScrollEndListener(boolean fromUser) {
        if (mOnScrollEndListener != null) {
            mOnScrollEndListener.onScrollEnd(position(), fromUser);
        }
    }

    /**
     * Runs the position changes listener.
     *
     * @param oldPosition the old position of the chart. -1 for the first time.
     * @param newPosition the new position of the chart.
     */
    private void runOnPositionChangedListener(@IntRange(from = -1) int oldPosition,
                                              @IntRange(from = 0) int newPosition) {
        if (mOnPositionChangedListener != null) {
            mOnPositionChangedListener.onPositionChanged(oldPosition, newPosition);
        }
    }

    /**
     * Runs the on click listener.
     */
    private void runOnClickListener() {
        if (mOnClickListener != null) {
            mOnClickListener.onClick();
        }
    }

    /**
     * Method to be called when a scroll performed by the user ended.
     */
    private void onScrollEnd() {
        runOnScrollEndListener(true);
    }

    /**
     * Scrolls to make the chart centred around a slot.
     *
     * @return true if chart is already well positioned or false otherwise.
     */
    private boolean stickToSlot() {
        if (mReady && mDimensions != null) {
            final int screenMiddle = mParams.mScrollX + mParams.mWindowRect.width() / 2;
            final int placeHolderWidth = mDimensions.getViewWidth(0);
            final int slotIndex = mDimensions.getXSlot(screenMiddle);
            final int slotX = mDimensions.getSlotX(slotIndex);
            final int newScrollX = slotX - placeHolderWidth;

            if (newScrollX == mParams.mScrollX) {
                return true;
            }

            smoothScrollBy(newScrollX - mParams.mScrollX, 0);
            return false;
        } else {
            return false;
        }
    }

    /**
     * Resets the chart. Clears the data structure rendering the chart as if it was
     * just initialized. This method does not reset the configuration though. The last set
     * configuration can still be used if we do not want to change it.
     */
    public void reset() {
        mParams.mData.clear();
        mInitPosition = -1;
        if (mReady) {
            getReady();
        }
    }

    /**
     * Makes the chart scroll to a specific slot.
     *
     * @param index the index of the slot to scroll to.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     */
    public void scrollTo(@IntRange(from = 0) int index) {
        if (mReady && mAdapter != null && mDimensions != null) {
            stopScroll();
            mAdapter.setFake(true);
            mAdapter.notifyDataSetChanged();
            final int placeHolderWidth = mDimensions.getViewWidth(0);
            final int slotX = mDimensions.getSlotX(index);
            final int newScrollX = slotX - placeHolderWidth;
            scrollBy(newScrollX - mParams.mScrollX, 0);
            mAdapter.setFake(false);
            mAdapter.notifyDataSetChanged();
            runOnScrollEndListener(false);
        } else {
            try {
                // Just to check if the index is in range
                mParams.mData.get(index, "");
                mInitPosition = index;
            } catch (IndexOutOfBoundsException ex) {
                // In case this IndexOutOfBoundsException is captured by the code calling scrollTo
                // the program will not stop and we must correctly set the mInitPosition
                mInitPosition = -1;
                throw ex;
            }
        }
    }

    /**
     * Returns the currently selected slot index. Or -1 if the chart doesn't contain any slots.
     *
     * @return the index of the selected slot.
     */
    public int position() {
        if (mReady && mDimensions != null) {
            final int midScreen = mParams.mScrollX + mParams.mWindowRect.width() / 2;
            return mDimensions.getXSlot(midScreen);
        } else {
            return -1;
        }
    }

    /**
     * Return the data size.
     *
     * @return the data size.
     */
    public int size() {
        return mParams.mData.size();
    }

    /**
     * Returns the total number of slots in the chart. This method is slightly different from
     * {@link #size()}. The chart has always at least one slot, even when the size is 0.
     * Hence unlike {@code size()} this method always return a value > 0.
     *
     * @return the number of slots in the chart.
     */
    public int slotCount() {
        if (mReady && mDimensions != null) {
            return mDimensions.getSlotCount();
        } else {
            return 1;
        }
    }

    /**
     * Sets the color for the shape in the background
     *
     * @param color the desired color
     */
    public void setBackgroundShapeColor(@ColorInt int color) {
        if (color != mParams.mChartBackgroundPaint.getColor()) {
            mParams.mChartBackgroundPaint.setColor(color);
            invalidate();
        }
    }

    /**
     * Sets the curve color to be used when no data is available.
     *
     * @param color the desired color
     */
    public void setCurvesNoDataColor(@ColorInt int color) {
        if (color != mParams.mCurveNoDataTextPaint.getColor()) {
            mParams.mCurveNoDataTextPaint.setColor(color);
            if (mReady && mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Sets the color gradient to be used to draw the curves.
     *
     * @param colorLow    The color to be used at the bottom of the chart
     * @param colorMedium The color to be used at middle of the chart
     * @param colorHigh   The color to be used at top of the chart
     * @param positions   May be null. The relative positions [0..1] of
     *                    each corresponding color in the colors. If this is null,
     *                    the the colors are distributed evenly along the gradient line.
     *                    It should be an array of size 4.
     * @throws IllegalArgumentException if {@code positions.length != 4}
     */
    public void setCurvesColorGradient(@ColorInt int colorLow, @ColorInt int colorMedium,
                                       @ColorInt int colorHigh,
                                       @Nullable @FloatRange(from = 0.0f, to = 1.0f)
                                               float[] positions) {
        mParams.mCurvesColorGradient[0] = mParams.mCurvesColorGradient[1] = colorLow;
        mParams.mCurvesColorGradient[2] = mParams.mCurvesColorGradient[3] = colorMedium;
        mParams.mCurvesColorGradient[4] = mParams.mCurvesColorGradient[5] = colorHigh;
        if (positions != null) {
            if (positions.length != 4) {
                throw new IllegalArgumentException("positions.length != 4");
            }
            mParams.mCurvesColorPosition[1] = positions[0];
            mParams.mCurvesColorPosition[2] = positions[1];
            mParams.mCurvesColorPosition[3] = positions[2];
            mParams.mCurvesColorPosition[4] = positions[3];
        } else {
            mParams.mCurvesColorPosition[1] = 0.2f;
            mParams.mCurvesColorPosition[2] = 0.4f;
            mParams.mCurvesColorPosition[3] = 0.6f;
            mParams.mCurvesColorPosition[4] = 0.8f;
        }
        if (mReady && mAdapter != null) {
            mReady = false;
            getReady();
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Sets curves text size, interpreted as scaled density points.
     * This size is converted to pixels based on the current screen density and
     * user font size preference (an android setting).
     *
     * @param size The desired text size in scaled density points
     */
    public void setCurvesTextSize(float size) {
        setCurvesTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    /**
     * Set curves text size to a given value by specifying the wanted unit.
     * See {@link TypedValue} for the possible dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     */
    private void setCurvesTextSize(int unit, float size) {

        setRawCurvesTextSize(TypedValue.applyDimension(
                unit, size, getResources().getDisplayMetrics()
        ));

    }

    /**
     * Set curves text size to a given and value in pixels.
     *
     * @param size The desired size in pixels
     */
    private void setRawCurvesTextSize(float size) {
        if (size != mParams.mCurveTextPaint.getTextSize()) {
            mParams.mCurveTextPaint.setTextSize(size);
            if (mReady && mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
        if (size != mParams.mCurveNoDataTextPaint.getTextSize()) {
            mParams.mCurveNoDataTextPaint.setTextSize(size);
            if (mReady && mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Sets the slot width. Interpreted density points.
     *
     * @param width The desired width in density points.
     */
    public void setSlotWidth(float width) {
        setSlotWidth(TypedValue.COMPLEX_UNIT_DIP, width);
    }

    /**
     * Sets the slot width to a given value by specifying the wanted unit.
     * See {@link TypedValue} for the possible dimension units.
     *
     * @param unit  the desired dimension unit
     * @param width the desired padding in the given unit
     */
    private void setSlotWidth(int unit, float width) {
        setRawSlotWidth((int) TypedValue.applyDimension(
                unit, width, getResources().getDisplayMetrics()
        ));
    }

    /**
     * Sets the slot width. Interpreted as pixels.
     *
     * @param width the desired width in pixels
     */
    private void setRawSlotWidth(int width) {
        if (mParams.mSlotWidth != width) {
            mParams.mSlotWidth = width;
            if (mReady && mAdapter != null) {
                mAdapter.notifyDataSetChanged();
                invalidate();
            }
        }
    }

    /**
     * Set the curvature radius for the corners in the background shape
     *
     * @param radius The desired width density points
     */
    public void setBackgroundCornersRadius(float radius) {
        setBackgroundCornersRadius(TypedValue.COMPLEX_UNIT_DIP, radius);
    }

    /**
     * Set the curvature radius for the corners in the background shape by specifying the wanted
     * unit.
     * See {@link TypedValue} for the possible dimension units.
     *
     * @param unit   the desired dimension unit
     * @param radius the desired padding in the given unit
     */
    private void setBackgroundCornersRadius(int unit, float radius) {
        setRawBackgroundCornersRadius((int) TypedValue.applyDimension(
                unit, radius, getResources().getDisplayMetrics()
        ));
    }

    /**
     * Set the curvature radius for the corners in the background shape. Interpreted as pixels.
     *
     * @param radius the desired radius in pixels
     */
    private void setRawBackgroundCornersRadius(int radius) {
        if (mParams.mBackgroundCornersRadius != radius) {
            mParams.mBackgroundCornersRadius = radius;
            if (mReady && mAdapter != null) {
                mAdapter.notifyDataSetChanged();
                invalidate();
            }
        }
    }

    /**
     * Sets the string that separates the labels used in the curve text
     *
     * @param separator the separator to be used in the chart curve text
     */
    public void setSeparator(@NonNull String separator) {
        mSeparator = separator;
        if (mReady && mAdapter != null) {
            mReady = false;
            getReady();
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Retrieve and apply XML attributes that were specified in the layout
     *
     * @param attrs contains the raw values for the XML attributes
     *              that were specified in the layout, which don't include
     *              attributes set by styles or themes, and which may have
     *              unresolved references.
     */
    private void obtainDefaultCustomAttributes(AttributeSet attrs) {
        // This call uses R.styleable.ChartView, which is an array of
        // the custom attributes that were declared in attrs.xml.
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ChartImpl,
                0, 0
        );


        /*
         * All the default values (`defValue` parameters) used here are only the values used by
         * default. **They should not be changed here**. Use the corresponding layout attributes
         * or method to change them.
         */
        try {
            setCurvesNoDataColor(
                    a.getColor(R.styleable.ChartImpl_curvesNoDataColor, Color.GRAY)
            );
            setRawCurvesTextSize(a.getDimensionPixelSize(
                    R.styleable.ChartImpl_curvesTextSize, 15
            ));
            setRawSlotWidth(
                    a.getDimensionPixelSize(R.styleable.ChartImpl_slotWidth, 120)
            );
            this.setBackgroundShapeColor(a.getColor(R.styleable.ChartImpl_backgroundShapeColor, Color.WHITE));
            setRawBackgroundCornersRadius(a.getDimensionPixelSize(R.styleable.ChartImpl_backgroundCornersRadius, 50));
            String separator = a.getString(R.styleable.ChartImpl_separator);
            if (separator != null) {
                setSeparator(separator);
            }
        } finally {
            // release the TypedArray so that it can be reused.
            a.recycle();
        }
    }

    /**
     * Interface definition for a callback to be invoked when the chart is clicked.
     */
    public interface OnClickListener {
        void onClick();
    }

    /**
     * Interface used to notify position updates. The position is the index of the
     * slot that is at the center of the chart.
     */
    public interface OnPositionChangedListener {
        /**
         * The callback to be executed when the chart's position changes.
         *
         * @param oldPosition the old position of the chart. -1 for the first time.
         * @param newPosition the new position of the chart.
         */
        void onPositionChanged(@IntRange(from = -1) int oldPosition,
                               @IntRange(from = 0) int newPosition);
    }

    public interface OnScrollEndListener {

        /**
         * The callback to be executed once the chart stops scrolling
         *
         * @param position the final position of the chart (the index of the slot)
         * @param fromUser indicates if the scroll was performed by the user or programmatically
         *                 using {@link #scrollTo(int)}.
         */
        void onScrollEnd(int position, boolean fromUser);
    }
}

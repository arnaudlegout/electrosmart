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

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Provides an adapter for the {@link RecyclerView} which is the implementation used for
 * {@link Chart}. This adapter is used to manage the instantiation, caching
 * and pooling of the views. The adapter differentiates between two types of views.
 * <p>
 * - Standard views: Those are views that hold slots
 * - Placeholder views: Those are views that do not hold slots and used only to fill up space.
 * <p>
 * It also makes a difference between left placeholder view and right placeholder view. This is
 * because they can have different widths. Therefore it has different layout parameters for each
 * one.
 * <p>
 * There also a specific type for the last standard view, because this one has a dynamic size
 * before it reaches it maximum and leaves the responsibility to a new view to hold the remaining
 * slots.
 * <p>
 * See {@link Chart} for more details on chart view types and the all the components of the
 * chart.
 * <p>
 * When we perform a long scroll it is unnecessary to draw the views. It creates a serious
 * performance degradation for this kind of scroll jumps. This is why we have the
 * {@link #setFake(boolean)}. It is used to tell the adapter to inform the views that we are
 * currently performing a long scroll and they don't need to render themselves. Once the scroll
 * is done we reset the parameter to {@code false} and then all the views are updated with
 * drawing activated.
 */
class ChartAdapter extends RecyclerView.Adapter<ChartAdapter.ViewHolder> {

    // A view that reached the maximum width
    final static int VIEW_TYPE_STANDARD = 0;

    // We have a different type for the right and left placeholder views because they can have
    // different widths and therefore need a layout parameters object for each.
    // The left placeholder view
    final static int VIEW_TYPE_LEFT_PLACEHOLDER = 1;
    // The right placeholder view
    final static int VIEW_TYPE_RIGHT_PLACEHOLDER = 2;
    // The right most non placeholder view
    // We are using a special view type for these because they do not always have the
    // same width as the standard ones (They have a dynamic width).
    final static int VIEW_TYPE_LAST = 3;

    // A {@code ChartDimensions} object to be used to calculate the views dimensions, slot numbers
    // and slot positions...
    @NonNull
    private final ChartDimensions mDimensions;
    // The factory to be used to generate the standard views
    @NonNull
    private final ChartViewFactory mViewFactory;
    // The factory to be used to generate the placeholder views
    @NonNull
    private final ChartViewFactory mPlaceholderViewFactory;
    // The layout parameters to be set to standard views
    @NonNull
    private final FrameLayout.LayoutParams mLayoutParamsStandard;
    // The layout parameters to be set to the left placeholder view
    @NonNull
    private final FrameLayout.LayoutParams mLayoutParamsLeftPlaceholder;
    // The layout parameters to be set to the right placeholder view
    @NonNull
    private final FrameLayout.LayoutParams mLayoutParamsRightPlaceholder;
    // The layout parameters to be set the last standard view
    @NonNull
    private final FrameLayout.LayoutParams mLayoutParamsLast;
    // The listener to be attached as the on click listener of all the views
    @NonNull
    private final View.OnClickListener mViewOnClickListener;
    // The boolean indicating if the generated views should be fake
    private boolean mFake;

    /**
     * Constructor.
     *
     * @param params     A {@code ChartParams} object holding parameters common to all views
     *                   such as the width of a slot, the chart's area size...
     * @param dimensions A {@code ChartDimensions} object to be used to calculate the views
     *                   dimensions, slot numbers and slot positions...
     * @param chartPath  A {@code ChartPath} object used to calculate the curves
     *                   ({@link android.graphics.Path} objects) to be drawn on the views.
     * @param listener   A {@code OnItemClickListener}, a callback that will be executed on chart
     *                   click events
     * @param curveText  A {@code CurveText} object used to generate the text to be drawn over the
     *                   curves
     */
    ChartAdapter(@NonNull ChartParams params, @NonNull ChartDimensions dimensions,
                 @NonNull ChartPath chartPath, @NonNull final OnItemClickListener listener,
                 @NonNull CurveText curveText) {

        mDimensions = dimensions;
        ChartViewParams viewParams = new ChartViewParams(params, mDimensions, chartPath, curveText);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O ||
                Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
            mViewFactory = new ChartViewFactoryImplAPI26(params, viewParams);
        } else {
            mViewFactory = new ChartViewFactoryImpl(params, viewParams);
        }

        mPlaceholderViewFactory = new ChartViewFactoryPlaceholder();

        final int slotPerWindow = params.mWindowRect.width() / params.mSlotWidth;
        final int maxViewWidth = slotPerWindow * params.mSlotWidth;
        mLayoutParamsStandard = new FrameLayout.LayoutParams(maxViewWidth,
                LinearLayout.LayoutParams.MATCH_PARENT);

        mLayoutParamsLeftPlaceholder = new FrameLayout.LayoutParams(
                mDimensions.getViewWidth(0),
                LinearLayout.LayoutParams.MATCH_PARENT);
        mLayoutParamsRightPlaceholder = new FrameLayout.LayoutParams(
                mDimensions.getViewWidth(mDimensions.getViewCount() - 1),
                LinearLayout.LayoutParams.MATCH_PARENT);

        mLayoutParamsLast = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        mViewOnClickListener = v -> listener.onClick();

    }


    /**
     * Called when a view created by this adapter has been recycled.
     * <p>
     * <p>A view is recycled when a {@link RecyclerView.LayoutManager} decides that it no longer
     * needs to be attached to its parent {@link RecyclerView}. This can be because it has
     * fallen out of visibility or a set of cached views represented by views are still
     * attached to the parent RecyclerView. If an item view has large or expensive data
     * bound to it such as large bitmaps, this may be a good place to release those
     * resources.</p>
     * <p>
     * RecyclerView calls this method right before clearing ViewHolder's internal data and
     * sending it to RecycledViewPool. This way, if ViewHolder was holding valid information
     * before being recycled, you can call {@link RecyclerView.ViewHolder#getAdapterPosition()} to get
     * its adapter position.
     *
     * @param holder The ViewHolder for the view being recycled
     */
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.mView.clear();
        super.onViewRecycled(holder);
    }


    /**
     * Called when RecyclerView needs a new {@link RecyclerView.ViewHolder} of the given type.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     * @see #getItemViewType(int)
     * @see #onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    @NonNull
    @Override
    public ChartAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ChartView v;
        switch (viewType) {
            case VIEW_TYPE_LEFT_PLACEHOLDER:
                v = mPlaceholderViewFactory.getView(parent.getContext());
                v.setLayoutParams(mLayoutParamsLeftPlaceholder);
                break;
            case VIEW_TYPE_RIGHT_PLACEHOLDER:
                v = mPlaceholderViewFactory.getView(parent.getContext());
                v.setLayoutParams(mLayoutParamsRightPlaceholder);
                break;
            case VIEW_TYPE_STANDARD:
                v = mViewFactory.getView(parent.getContext());
                v.setLayoutParams(mLayoutParamsStandard);
                break;
            default:
                v = mViewFactory.getView(parent.getContext());
                v.setLayoutParams(mLayoutParamsLast);
                break;
        }
        return new ViewHolder(v);
    }


    /**
     * Called by RecyclerView to display the data at the specified position. This method should
     * update the contents of the {@link RecyclerView.ViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ChartAdapter.ViewHolder holder, int position) {
        // All the view types have their width that is set at view holder creation,
        // but VIEW_TYPE_LAST which cannot be known until here
        if (getItemViewType(position) == VIEW_TYPE_LAST) {
            holder.mView.getLayoutParams().width = mDimensions.getViewWidth(position);
        }
        holder.mView.setFake(mFake);
        holder.mView.setIndex(position);
        holder.mView.init();
        holder.mView.setOnClickListener(mViewOnClickListener);
    }


    /**
     * Return the view type of the item at <code>position</code> for the purposes
     * of view recycling.
     *
     * @param position position to query
     * @return integer value identifying the type of the view needed to represent the item at
     * <code>position</code>. Type codes need not be contiguous.
     */
    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return VIEW_TYPE_LEFT_PLACEHOLDER;
        if (position == mDimensions.getViewCount() - 1)
            return VIEW_TYPE_RIGHT_PLACEHOLDER;
        if (position == mDimensions.getViewCount() - 2)
            return VIEW_TYPE_LAST;
        return VIEW_TYPE_STANDARD;
    }


    /**
     * Returns the total number of views in the chart.
     *
     * @return The total number of views.
     */
    @Override
    public int getItemCount() {
        return mDimensions.getViewCount();
    }

    /**
     * Tells the adapter to attach fake views (Used to improve programmatic scroll performance).
     *
     * @param fake if true disables view rendering
     */
    public void setFake(boolean fake) {
        mFake = fake;
    }


    /**
     * Interface definition for a callback to be invoked when the chart is clicked.
     */
    interface OnItemClickListener {
        void onClick();
    }

    /**
     * Provide a reference to the views for each index.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        @NonNull
        final ChartView mView;

        ViewHolder(@NonNull ChartView v) {
            super(v);
            mView = v;
        }
    }
}

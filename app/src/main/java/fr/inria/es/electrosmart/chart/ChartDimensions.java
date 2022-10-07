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


import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * Defines class for needed conversions from data to graphics dimensions and vise versa.
 * In this class we use terms like view and slots. These are terms that refer to components
 * of the chart. Below is a brief description of those components.
 * <p>
 * -  View (or ChartView) are segments of the chart that when stacked together horizontally
 * (using the recycler view) construct the whole chart.
 * <p>
 * -  Views are also composed of slots. A slot is the space in a view that is dedicated
 * to represent the data held by one index in the data structure. It is also the space used
 * to represent values in the same time period of width {@link Chart#setTimeGap(int)}.
 * <p>
 * More details about the chart components can be found in the {@link Chart} class
 * documentation.
 * <p>
 * This implementation acts as an adapter (or maybe more accurately a bridge) around the chart's
 * data structure to extract the dimensions needed to construct the chart.
 */
class ChartDimensions {

    // The chart's data structure that will be adapted to calculate dimensions
    @NonNull
    private final ChartData mData;
    /**
     * The width of a slot. See {@link ChartDimensions} for a definition of what is a slot
     */
    @IntRange(from = 1)
    private final int mSlotWidth;
    // The number of slots that can fit in the screen
    // It is also used in this implementation as the maximum number of slots that we put
    // in a single view
    @IntRange(from = 1)
    private final int mSlotPerWindow;
    /**
     * The size of left side placeholder view. See {@link Chart} documentation for
     * a definition of a placeholder view and the reason behind it and why the left placeholder
     * width can can be different from the right placeholder.
     */
    @IntRange(from = 1)
    private final int mLeftPlaceHolderWidth;
    /**
     * The size of right side placeholder view. See {@link Chart} documentation for
     * a definition of a placeholder view and the reason behind it and why the left placeholder
     * width can can be different from the right placeholder.
     */
    @IntRange(from = 1)
    private final int mRightPlaceHolderWidth;

    /**
     * Constructor.
     *
     * @param data        the chart's data structure to be adapted
     * @param windowWidth the width of the space dedicated to the chart on the screen
     * @param slotWidth   the width used to represent a slot
     */
    ChartDimensions(@NonNull ChartData data, @IntRange(from = 1) int windowWidth,
                    @IntRange(from = 1) int slotWidth) {
        mData = data;
        mSlotWidth = slotWidth;
        mSlotPerWindow = windowWidth / mSlotWidth;
        mLeftPlaceHolderWidth = (windowWidth - mSlotWidth) / 2;
        if (2 * mLeftPlaceHolderWidth + mSlotWidth == windowWidth) {
            mRightPlaceHolderWidth = mLeftPlaceHolderWidth;
        } else {
            mRightPlaceHolderWidth = mLeftPlaceHolderWidth + 1;
        }
    }

    /**
     * Returns the total number of views we need. In this implementation we always have
     * at least three views. One view at the center and two place holders at each side.
     * <p>
     * When the data size is not zero, we always add two views for the place holders.
     * <p>
     * In this implementation the number of views depends on the slots width and the window
     * width which are included in the {@code ChartParams} object.
     *
     * @return the total number of views needed to hold the chart
     */
    @IntRange(from = 3)
    public int getViewCount() {

        final int dataSize = mData.size();

        if (dataSize == 0) {
            return 3;
        }

        if (dataSize % mSlotPerWindow == 0) {
            return dataSize / mSlotPerWindow + 2;
        }
        return dataSize / mSlotPerWindow + 3;

    }

    /**
     * Returns the total number of slots (in all the views). We have at least one slot
     * (even when data size is zero). Otherwise the number of slots is the size of the data.
     *
     * @return Returns the total number of slots
     */
    @IntRange(from = 1)
    public int getSlotCount() {

        int size = mData.size();

        if (size != 0) {
            return size;
        } else {
            return 1;
        }


    }

    /**
     * Returns the number of slots in a view. For a placeholder
     * ({@code index == 0 || index == getViewCount() - 1}) it returns 0.
     * <p>
     * Otherwise it calculates it based on the data size, window size and slots size.
     *
     * @param index the index of the view
     * @return the number of the slots in the view
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= getViewCount()})
     */
    @IntRange(from = 0)
    public int getViewSlotCount(@IntRange(from = 0) int index) {
        checkView(index);
        return viewSlotCount(index);
    }

    /**
     * The same as {@link #getViewSlotCount(int)} but does not check if the index is out of bounds.
     *
     * @param index the index of the view
     * @return the number of the slots in the view
     */
    @IntRange(from = 0)
    private int viewSlotCount(@IntRange(from = 0) int index) {
        final int viewCount = getViewCount();

        if (index == 0 || index == viewCount - 1) {
            return 0;
        }

        final int dataSize = mData.size();

        if (dataSize == 0) {
            return 1;
        }

        if (index * mSlotPerWindow > dataSize) {
            return dataSize % mSlotPerWindow;
        }

        return mSlotPerWindow;
    }

    /**
     * Returns the (absolute) index of the first slot in a view.
     * Returns -1 if the view is a placeholder.
     *
     * @param index the index of the view
     * @return the index of the first slot in the view. Or -1 if none.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= getViewCount()})
     */
    @IntRange(from = -1)
    public int getFirstViewSlot(@IntRange(from = 0) int index) {
        checkView(index);
        return firstViewSlot(index);
    }

    /**
     * The same as {@link #getFirstViewSlot(int)} but it does not check if the index is out
     * of bounds.
     *
     * @param index the index of the view
     * @return the index of the first slot in the view. Or -1 if none.
     */
    @IntRange(from = -1)
    private int firstViewSlot(@IntRange(from = 0) int index) {
        final int viewCount = getViewCount();

        // If the view is a placeholder return -1
        if (index == 0 || index == viewCount - 1) {
            return -1;
        }

        return (index - 1) * mSlotPerWindow;
    }

    /**
     * Returns the (absolute) index of the view holding a slot.
     *
     * @param index the (absolute) index of the slot.
     * @return the index of the view holding the slot.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= getSlotCount()})
     */
    @IntRange(from = 1)
    public int getViewSlot(@IntRange(from = 0) int index) {
        checkSlot(index);
        return viewSlot(index);
    }

    /**
     * The same as {@link #getViewSlot(int)} but it does not check if the index is out of bounds.
     *
     * @param index the index of the slot.
     * @return the index of the view holding the slot.
     */
    @IntRange(from = 1)
    private int viewSlot(@IntRange(from = 0) int index) {
        return index / mSlotPerWindow + 1;
    }


    /**
     * Returns the width of a view in pixels.
     *
     * @param index the index of the view.
     * @return width of the view (in pixels).
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= getViewCount()})
     */
    @IntRange(from = 1)
    public int getViewWidth(@IntRange(from = 0) int index) {
        checkView(index);
        return viewWidth(index);
    }

    /**
     * The same as {@link #getViewWidth(int)} but it does not check if the index is out of bounds.
     *
     * @param index the index of the view
     * @return width of the view (in pixels)
     */
    @IntRange(from = 1)
    private int viewWidth(@IntRange(from = 0) int index) {
        final int viewCount = getViewCount();

        if (index == 0) {
            return mLeftPlaceHolderWidth;
        }
        if (index == viewCount - 1) {
            return mRightPlaceHolderWidth;
        }

        return mSlotWidth * viewSlotCount(index);
    }


    /**
     * Returns the overall width of the chart
     *
     * @return the total width in pixels
     */
    @IntRange(from = 1)
    public int getWidth() {
        return (getSlotCount() * mSlotWidth) + mLeftPlaceHolderWidth + mRightPlaceHolderWidth;
    }


    /**
     * Returns the index of the slot in the x position in pixel.
     * Returns -1 if the position doesn't contain a slot (if the position corresponds
     * to a placeholder view). See {@link #getSlotX(int)} to understand the difference.
     *
     * @param x the overall x position in pixels (placeholders included)
     * @return the index of the slot in the x position or -1 if none
     * @throws IllegalArgumentException if x is out range
     *                                  ({@code x < 0} or {@code x > getWidth()})
     */
    @IntRange(from = -1)
    public int getXSlot(@IntRange(from = 0) int x) {
        checkX(x);
        return xSlot(x);
    }

    /**
     * The same as {@link #getSlotX(int)} but does not check if the x position is out of bounds
     *
     * @param x the overall x position in pixels (placeholders included)
     * @return the index of the slot in the x position or -1 if none
     */
    @IntRange(from = -1)
    private int xSlot(@IntRange(from = 0) int x) {
        final int width = getWidth();

        if (x < mLeftPlaceHolderWidth || x >= width - mRightPlaceHolderWidth) {
            return -1;
        }

        return (x - mLeftPlaceHolderWidth) / mSlotWidth;
    }


    /**
     * Returns the horizontal position in the chart of the left bound of a slot. The difference
     * with {@link #getXSlot(int)} is that this one converts the index of a slot to its position
     * in the chart in pixels, whereas the other one converts a position in pixel to the index
     * of the slot.
     *
     * @param index the index of the slot.
     * @return the position in pixels of the left bound of a slot.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= getSlotCount()}
     */
    public int getSlotX(@IntRange(from = 0) int index) {
        checkSlot(index);
        return slotX(index);
    }

    /**
     * The same as {@link #getSlotX(int)} but it does not check if the index is out of bounds.
     *
     * @param index the index of the slot.
     * @return the position in pixels of the left bound of a slot.
     */
    private int slotX(@IntRange(from = 0) int index) {
        return mLeftPlaceHolderWidth + index * mSlotWidth;
    }

    /**
     * Checks that the index is not out of the bounds, otherwise throws an exception.
     *
     * @param index index of the view
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= getViewCount()})
     */
    private void checkView(@IntRange(from = 0) int index) {
        final int viewCount = getViewCount();

        if (index < 0 || index >= viewCount) {
            throw new IndexOutOfBoundsException
                    ("View index: " + index + ", View count: " + viewCount);
        }
    }

    /**
     * Checks the index of a slot is not out of bounds, throws an exception otherwise.
     *
     * @param index index of a slot
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= getSlotCount()})
     */
    private void checkSlot(@IntRange(from = 0) int index) {
        final int slotCount = getSlotCount();

        if (index < 0 || index >= slotCount) {
            throw new IndexOutOfBoundsException
                    ("Slot index: " + index + ", Slot count: " + slotCount);
        }
    }

    /**
     * Checks the pixel position is not out of bounds, throws an exception otherwise.
     *
     * @param x a horizontal position in the chart (in pixels)
     * @throws IllegalArgumentException if x is out range
     *                                  ({@code x < 0} or {@code x > getWidth()})
     */
    private void checkX(@IntRange(from = 0) int x) {
        final int width = getWidth();

        if (x < 0 || x > width) {
            throw new IllegalArgumentException("Illegal x value: " + x + ", width: " + width);
        }
    }
}

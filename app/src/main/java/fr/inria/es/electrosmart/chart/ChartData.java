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

import android.util.SparseIntArray;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A memory optimized implementation  to represent the chart's internal data structure.
 * <p>
 * This data structure holds multiple lists of integer values. More specifically, one list of
 * integers for each string label.  The integers in the list are indexes. However the structure
 * doesn't require all the points to be present (The lists may contain holes).
 * <p>
 * It uses a {@link Map} of {@link SparseIntArray}s so that the arrays can have holes in the
 * middle (possible to have indexes in the middle of the array with no value). With the sparse
 * array those holes do not use any memory.
 * <p>
 * SparseIntArray are inefficient for search, insert, remove, but more compact than a HashMap.
 * An alternative implementation based on {@code Map<Integer, Integer>} will be faster but more
 * memory will be used because of the autoboxing. We decided to favor memory over efficiency as we
 * never practically observed an efficiency issue due to the {@link SparseIntArray}.
 */
class ChartData {


    // The actual data structure that is adapted to the interface.
    @NonNull
    private final Map<String, SparseIntArray> mData;
    // The size of the data structure including the holes and as if all the values for all
    // the labels were merged into the same list.
    @IntRange(from = 0)
    private int mSize;
    // Used to avoid shifting all the arrays when we shrink left. The values appear to the client
    // to be shifted where as we just add this value to index.
    @IntRange(from = 0)
    private int mShift;

    /**
     * Constructor.
     */
    ChartData() {
        mData = new HashMap<>();
        mSize = 0;
        mShift = 0;
    }

    /**
     * Adds a new label to the data structure and creates a new sparse array for it.
     *
     * @param label the label to add.
     */
    private void addLabel(@NonNull String label) {
        mData.put(label, new SparseIntArray());
    }


    /**
     * Inserts a value into the data structure or overrides the old value if a value was inserted
     * before with the same {@code index}, and {@code label}. {@code value} -1 is used to just
     * increase the size of the chart if the index is greater than the current (size - 1) without
     * setting any value.
     *
     * @param index the index of the value
     * @param label the label of the value
     * @param level the value to insert
     * @throws IllegalArgumentException if {@code label == null}
     * @throws IllegalArgumentException if {@code level < 0 || level > 100}
     * @throws IllegalArgumentException if {@code index < 0}
     */
    public void put(@IntRange(from = 0) int index, @NonNull String label,
                    @IntRange(from = -1, to = 100) int level) {

        if (label == null) {
            throw new IllegalArgumentException("label == null");
        }

        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        }

        if (level < -1) {
            throw new IllegalArgumentException("level < -1");
        }

        if (level > 100) {
            throw new IllegalArgumentException("level > 100");
        }


        if (level != -1) {
            if (!mData.containsKey(label)) {
                addLabel(label);
            }
            mData.get(label).put(index + mShift, level);
        }

        if (index + 1 > mSize) {
            mSize = index + 1;
        }
    }

    /**
     * Shrinks the data structure to match the specified size if the current size is bigger.
     * The shrinkLeft is done by removing values to the left (from index 0) and shifting the indexes
     * to the left by the sizes difference.
     *
     * @param size the new size
     */
    public void shrinkLeft(@IntRange(from = 0) int size) {

        final int oldSize = mSize;

        if (oldSize < size) {
            return;
        }

        // we remove for each label data
        Iterator<Map.Entry<String, SparseIntArray>> iterator = mData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SparseIntArray> entry = iterator.next();
            SparseIntArray array = entry.getValue();
            for (int i = 0; i < oldSize - size; i++) {
                array.delete(i + mShift);
            }
            // If the data structure for a label in empty, we remove that label from mData
            if (array.size() == 0) {
                iterator.remove();
            }

        }

        mShift += mSize - size;
        mSize = size;
    }

    /**
     * Resets the data structure. Note that the time origin and time gap are kept.
     */
    public void clear() {
        mData.clear();
        mSize = 0;
        mShift = 0;
    }


    /**
     * Gets the set of labels for which we hold values.
     *
     * @return a {@link Set} holding all the labels we have.
     */
    @NonNull
    public Set<String> getLabels() {
        return mData.keySet();
    }


    /**
     * Gets a value that was inserted into the data structure with the passed {@code index}
     * and {@code label}.
     *
     * @param index the index of the value to get
     * @param label the label of the value to get
     * @return value that is inserted before into the data structure with the passed {@code index}
     * and {@code label}. -1 if none.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     * @throws IllegalArgumentException  if {@code label == null}
     */
    @IntRange(from = -1, to = 100)
    public int get(@IntRange(from = 0) int index, @NonNull String label) {

        if (label == null) {
            throw new IllegalArgumentException("label == null");
        }

        if (index < 0 || index >= mSize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }

        if (!mData.containsKey(label)) {
            return -1;
        }

        return mData.get(label).get(index + mShift, -1);

    }

    /**
     * Gets a value having the closest index to the parameter {@code index} that was
     * inserted with the label {@code label}. If none it returns -1.
     *
     * @param index the target index we should be closest to
     * @param label the label we should be looking at
     * @return the closest index to {@code index}
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     * @throws IllegalArgumentException  if {@code label == null}
     */
    @IntRange(from = -1, to = 100)
    public int getClosest(@IntRange(from = 0) int index, @NonNull String label) {

        if (label == null) {
            throw new IllegalArgumentException("label == null");
        }

        if (index < 0 || index >= mSize) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + mSize);
        }

        if (!mData.containsKey(label)) {
            return -1;
        }

        final SparseIntArray array = mData.get(label);

        /*
         * The algorithm used here has a linear complexity.
         *
         * It uses two indexes that are initialized with the value of the target index.
         * One of these indexes is incremented and the other is decremented every iteration
         * of the while loop. The first index that happens to find an existing value returns.
         * The priority is given to index that is incrementing. The loop stops when both
         * the indexes are out of bounds (ie. {@code index < 0 || index >= size()})
         */
        int value;
        int indexRight = index + mShift;
        int indexLeft = index + mShift;
        while (indexLeft >= 0 || indexRight < mSize) {
            value = array.get(indexRight, -1);
            if (value != -1) {
                return value;
            }
            value = array.get(indexLeft, -1);
            if (value != -1) {
                return value;
            }
            indexRight++;
            indexLeft--;
        }

        return -1;
    }

    /**
     * Returns the size of the data structure including the holes and as if all the values for all
     * the labels were merged into the same list.
     *
     * @return the size of the data structure.
     */
    @IntRange(from = 0)
    public int size() {
        return mSize;
    }

}

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

package fr.inria.es.electrosmart.signalsdatastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * A wrapper around a list of {@link SignalsSlot} to make it capable of mapping indexes to timestamps.
 */
public class Timeline implements List<SignalsSlot> {

    // The timestamp that corresponds to index 0
    @IntRange(from = 0)
    private long mTimeOrigin;

    // The time difference between two subsequent indexes
    @IntRange(from = 0)
    private int mTimeGap;

    // The inner data holder
    @NonNull
    private List<SignalsSlot> mData;

    /**
     * Constructs an empty timeline with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     * @param timeGap         the time difference between two subsequent indexes
     * @throws IllegalArgumentException if the specified initial capacity
     *                                  is negative ({@code initialCapacity < 0})
     */
    public Timeline(@IntRange(from = 0) int initialCapacity, @IntRange(from = 0) int timeGap) {
        mData = new ArrayList<>(initialCapacity);
        mTimeGap = timeGap;
    }

    /**
     * Gets the timestamp for values with index 0 (the time origin).
     *
     * @return the timestamp for values with index 0.
     * @see #setTimeOrigin(long)
     * @see #getTimestamp(int)
     */
    @IntRange(from = 0)
    public long getTimeOrigin() {
        return mTimeOrigin;
    }


    /**
     * Sets the timestamp for values with index 0 (the time origin).
     *
     * @param time the value to set to time origin.
     * @throws IllegalArgumentException if {@code time < 0}
     * @see #getTimeOrigin()
     * @see #getTimestamp(int)
     */
    public void setTimeOrigin(@IntRange(from = 0) long time) {

        if (time < 0)
            throw new IllegalArgumentException("time < 0");

        mTimeOrigin = time;
    }


    /**
     * Returns the timestamp of an index considering that time origin ({@code getTimeOrigin()})
     * is the timestamp for index 0, and the time difference between two subsequent indexes is
     * time gap ({@code getTimeGap()}).
     *
     * @param index the data index (not necessarily inserted)
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()})
     * @see #getTimeOrigin()
     * @see #setTimeOrigin(long)
     * @see #getTimeGap()
     */
    public long getTimestamp(@IntRange(from = 0) int index) {

        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException
                    ("Index: " + index + ", Size: " + size());
        }

        return mTimeOrigin + index * mTimeGap;
    }


    /**
     * Gets the time gap, which is the difference in timestamp between two subsequent indexes.
     *
     * @return the time gap.
     */
    @IntRange(from = 0)
    private int getTimeGap() {
        return mTimeGap;
    }

    /**
     * Returns the index in the Timeline that corresponds to a timestamp.
     *
     * @param timestamp the timestamp
     * @return the index
     * @throws TimestampOutOfBoundsException if the timestamp is out of range
     *                                       ({@code timestamp < getTimeOrigin() ||
     *                                       timestamp >= getTimestamp(size() - 1) + getTimeGap()})
     */
    public int indexOfTimestamp(long timestamp) {
        long lastTimestamp = getTimeOrigin() + size() * getTimeGap();
        if (timestamp < getTimeOrigin()) {
            throw new TimestampOutOfBoundsException
                    ("Timestamp: " + timestamp + ", Time origin: " + getTimeOrigin());
        } else if (timestamp >= lastTimestamp) {
            throw new TimestampOutOfBoundsException
                    ("Timestamp: " + timestamp + ", Last timestamp: " + lastTimestamp);
        }
        return (int) ((timestamp - getTimeOrigin()) / getTimeGap());
    }

    /**
     * Shrinks the data structure to match the specified size if the current size is bigger.
     * The shrink is done by removing values to the left (from index 0) and shifting the indexes
     * to the left by the sizes difference. It also updates the time origin accordingly.
     *
     * @param size the new size
     */
    public void shrinkLeft(@IntRange(from = 0) int size) {
        final int oldSize = size();

        while (size() > size) {
            remove(0);
        }

        if (oldSize > size) {
            int sizeDiff = oldSize - size;
            setTimeOrigin(getTimeOrigin() + getTimeGap() * sizeDiff);
        }
    }

    /**
     * Shrinks the data structure to match the specified size if the current size is bigger.
     * The shrink is done by removing values to the right (from index {@code size() - 1}).
     *
     * @param size the new size
     */
    public void shrinkRight(@IntRange(from = 0) int size) {
        while (size() > size) {
            remove(size() - 1);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return mData.size();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return mData.isEmpty();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o) {
        return mData.contains(o);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Iterator<SignalsSlot> iterator() {
        return mData.iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public Object[] toArray() {
        return mData.toArray();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public <T> T[] toArray(T[] a) {
        return mData.toArray(a);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(SignalsSlot signalsSlot) {
        return mData.add(signalsSlot);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o) {
        return mData.remove(o);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return mData.containsAll(c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(@NonNull Collection<? extends SignalsSlot> c) {
        return mData.addAll(c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(int index, @NonNull Collection<? extends SignalsSlot> c) {
        return mData.addAll(index, c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return mData.removeAll(c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return mData.retainAll(c);
    }


    /**
     * Removes all of the elements from this list and sets a new time origin.
     *
     * @param timeOrigin the value to set to time origin.
     * @see #getTimeOrigin()
     * @see #setTimeOrigin(long)
     * @see #getTimestamp(int)
     */
    public void reset(@IntRange(from = 0) long timeOrigin) {
        mData.clear();
        setTimeOrigin(timeOrigin);
    }

    /**
     * {@inheritDoc}
     * <p>
     * WARNING: If the intent of calling clear is to reuse the same timeline object for building a completely new
     * data structure, you should probably use {@link #reset(long)} instead, and specify the new time origin that
     * should be used.
     */
    @Deprecated
    @Override
    public void clear() {
        mData.clear();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SignalsSlot get(int index) {
        return mData.get(index);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SignalsSlot set(int index, SignalsSlot element) {
        return mData.set(index, element);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int index, SignalsSlot element) {
        mData.add(index, element);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SignalsSlot remove(int index) {
        return mData.remove(index);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(Object o) {
        return mData.indexOf(o);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(Object o) {
        return mData.lastIndexOf(o);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ListIterator<SignalsSlot> listIterator() {
        return mData.listIterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public ListIterator<SignalsSlot> listIterator(int index) {
        return mData.listIterator(index);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public List<SignalsSlot> subList(int fromIndex, int toIndex) {
        return mData.subList(fromIndex, toIndex);
    }

    /**
     * Thrown to indicate that the timestamp we are trying to access is out of range.
     */
    public class TimestampOutOfBoundsException extends RuntimeException {

        /**
         * Constructs an {@code TimestampOutOfBoundsException} with the
         * specified detail message.
         *
         * @param s the detail message.
         */
        TimestampOutOfBoundsException(String s) {
            super(s);
        }
    }
}

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

import android.graphics.LinearGradient;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


/**
 * Generates a smooth curve using multiple segments of cubic bezier curves (using the method
 * {@link Path#cubicTo(float, float, float, float, float, float)}). The generated curves
 * are designed to look smooth over the views and in the borders of the views, so that
 * the chart looks like one long scrollable view.
 * <p>
 * In order to generate the curves, objects of this class retrieve the data values from
 * the chart's data structure and according to chart dimensions it converts them into pixel
 * points. This means that the object has to deal with lists of points each time it tries to
 * generate a curve. In order to do not generate point objects each time we generate a curve,
 * we make sure we instantiate one object of this class. And the lists of point objects is reused
 * to perform the curve processing at each new curve creation. If the number of point objects
 * needed is not sufficient for the current curve, the lists are expanded using
 * {@link #EXPANSION_SIZE} point objects.
 */
class ChartPath {

    // The number of points added to expand the reusable lists of points when it is not
    // sufficient for the current curve processing
    private final static int EXPANSION_SIZE = 20;
    // A ChartParams object holding parameters common to all views
    // such as the width of a slot, the chart's area size...
    @NonNull
    private final ChartParams mParams;
    // The y value used to represent a data value of 0
    private final float mMinY;
    // The y value used to represent a data value of 100
    private final float mMaxY;
    // The list of points the curve should pass through
    @NonNull
    private final List<PointF> mCoordinates;
    // A point object to hold the coordinates of the first control point of a cubic bezier curve
    @NonNull
    private final PointF mControlPoint1;
    // A point object to hold the coordinates of the second control point of a cubic bezier curve
    @NonNull
    private final PointF mControlPoint2;
    // A point object used to swap points
    @NonNull
    private final PointF mTmpControlPoint;

    ChartPath(@NonNull ChartParams params) {
        mParams = params;
        float maxY = mParams.mWindowRect.height();
        // The algorithm used to smooth the curves can cause the chart to go out of the bounds
        // of the view and look truncated from the top or the bottom.
        // Here we adjust the maximum and minimum y values to avoid this issue.
        // This formula is not arbitrary.
        // It comes from the fact that the maximum slope used to calculate control points is
        // delta = maxY / (mParams.mSlotRect.width() * 2).
        // to simplify let's replace `mParams.mSlotRect.width()` with sw (for slot width).
        // ie.
        // delta = maxY / ( sw * 2 )
        // and the corresponding deviation is
        // deviation = delta * 0.25 * sw
        // ie.
        // deviation = delta * sw / 4
        // <=> deviation = (maxY / (sw * 2) ) * sw / 4
        // <=> deviation = (maxY / 2) / 4
        // <=> deviation = maxY / 8
        float maxDeviation = maxY / 8;
        mMinY = maxDeviation + mParams.mCurveTextHeight / 2;
        mMaxY = maxY - maxDeviation - mParams.mCurveTextHeight / 2;
        // Update the color gradient accordingly
        Shader linearGradient = new LinearGradient(
                0,
                mMaxY,
                0,
                mMinY,
                mParams.mCurvesColorGradient,
                mParams.mCurvesColorPosition,
                Shader.TileMode.CLAMP
        );
        mParams.mCurveTextPaint.setShader(linearGradient);

        mCoordinates = new ArrayList<>();
        mControlPoint1 = new PointF();
        mControlPoint2 = new PointF();
        mTmpControlPoint = new PointF();
    }


    /**
     * Sets the coordinates to the point object that is in the position {@code index} of the
     * {@code list} if it exists.
     * Otherwise it expands the {@code list} and then sets the coordinates.
     *
     * @param list  The list to be modified. Should be instantiated by the caller.
     * @param index The index of the object to alter.
     * @param x     The x coordinate to set to the object
     * @param y     The y coordinate to set to the object
     */
    private static void put(List<PointF> list, int index, float x, float y) {
        while (index >= list.size()) {
            expand(list);
        }
        list.get(index).set(x, y);
    }

    /**
     * Expands the {@code list} by adding {@code EXPANSION_SIZE} objects to it.
     *
     * @param list The list to expand
     */
    private static void expand(List<PointF> list) {
        final int targetSize = list.size() + EXPANSION_SIZE;
        while (list.size() < targetSize) {
            list.add(new PointF(0, 0));
        }
    }

    /**
     * Calculates the midpoint of the segment [p1, p2]
     *
     * @param p1  first segment point
     * @param p2  second segment point
     * @param dst the object where to put the result. Should be instantiated by the caller.
     */
    private static void midpoint(PointF p1, PointF p2, PointF dst) {
        dst.set(
                (p1.x + p2.x) / 2,
                (p1.y + p2.y) / 2
        );
    }

    /**
     * Calculates the slope of the line passing through two points
     *
     * @param p1 first point
     * @param p2 second point
     * @return slope of the line represented by p1 and p2
     */
    private static float delta(PointF p1, PointF p2) {
        return (p2.y - p1.y) / (p2.x - p1.x);
    }

    /**
     * Calculates the control point in the neighborhood of the point {@code point}
     * in the direction of the point {@code neighbor}.
     * <p>
     * The control points are calculated so that the slope at the point {@code point} towards
     * the point {@code neighbor} is equal to {@code delta} and is equal to the slope at it's right.
     *
     * @param neighbor the point toward which we want to go using the calculated control point
     * @param point    the point for which we calculate the control point
     * @param delta    the slope
     * @param res      the calculated control point. It should be instantiated by the caller.
     */
    private static void controlPoint(@NonNull PointF neighbor, @NonNull PointF point, float delta,
                                     @NonNull PointF res) {
        final float xDist = 0.25f * (neighbor.x - point.x);

        float x = point.x + xDist;
        float y = point.y + xDist * delta;
        res.set(x, y);
    }

    /**
     * Calculates the control points in the neighborhood of the point {@code point}
     * according to the point before ({@code leftPoint}) and the point after ({@code rightPoint}).
     * <p>
     * The control points are calculated so that the slope at the left to the point {@code point}
     * is equal to the slope at it's right.
     *
     * @param leftPoint  the point before the point {@code point}
     * @param rightPoint the point after the point {@code point}
     * @param point      the point for which we calculate the control points
     * @param leftRes    the calculated control point that is before {@code point}.
     *                   Should be instantiated by the caller.
     * @param rightRes   the calculated control point that is after {@code point}.
     *                   Should be instantiated by the caller.
     */
    private static void controlPoints(@NonNull PointF leftPoint, @NonNull PointF rightPoint,
                                      @NonNull PointF point, @NonNull PointF leftRes,
                                      @NonNull PointF rightRes) {

        final float delta = delta(leftPoint, rightPoint);

        controlPoint(leftPoint, point, delta, leftRes);
        controlPoint(rightPoint, point, delta, rightRes);

    }

    /**
     * Calculates the graphical y coordinate between {@code mMinY} and {@code mMaxY}
     * from a percentage value.
     *
     * @param value the percentage value.
     * @return returns the y coordinate
     */
    private float normalizeY(float value) {
        return (mMinY - mMaxY) * value / 100 + mMaxY;
    }

    /**
     * Requests the chart's data structure for a value, if the value doesn't exist (the
     * data structure returns -1) requests the value for the closest index.
     *
     * @param index the index of the value to retrieve
     * @param label the label for the value to retrieve
     * @return the value from the chart's data structure or the closest one to it
     */
    private int getDataValue(int index, String label) {
        int value = mParams.mData.get(index, label);
        if (value == -1) {
            value = mParams.mData.getClosest(index, label);
        }
        return value;
    }

    /**
     * Extracts graphical coordinates form the {@code ChartParams} to a list of {@code PointF}.
     * It converts the values in the chart's data structure to pixel positions according to
     * the view dimensions.
     *
     * @param list   The list to extract the point into. Should be instantiated by the caller.
     * @param offset The data index to start from
     * @param size   The number of data values to use for extracting the points
     * @param label  The {@code ChartData} label to extract from
     */
    private void extractCoordinates(@NonNull List<PointF> list, int offset, int size,
                                    @NonNull String label) {

        final int slotWidth = mParams.mSlotWidth;

        int value;
        int index;

        // If the offset is not 0. We retrieve one point before the offset. This point
        // is used to keep the curve smooth with the curve in the last view
        // (the view directly at the left).
        // If there is no view at the left (This is the first view) we use the first value
        // in this view so that the curve starts with a horizontal slope.
        if (offset == 0) {
            index = 0;
        } else {
            index = offset - 1;
        }

        value = getDataValue(index, label);

        // The x value used here is not in the current view. It is `slotWidth / 2`
        // at the left of the view. So it's value is `-slotWidth / 2`
        float x = -slotWidth / 2;
        float y = normalizeY(value);

        put(list, 0, x, y);

        // For each data value we calculate the x value
        // as `slotWidth / 2 + i * slotWidth` (at the center of the slot)
        // and y value using the data value and converting it using the `normalizeY(float)`
        // method.
        for (int i = 0; i < size; i++) {

            index = i + offset;

            value = getDataValue(index, label);

            x = slotWidth / 2 + i * slotWidth;
            y = normalizeY(value);

            put(list, i + 1, x, y);
        }

        // We perform here the same operation we did if the offset is not equal to 0.
        // If there is still data after the last index in this extracted segment,
        // we retrieve it to keep the curve smooth with curve in the following view
        // (the one directly at the right).
        // If there is no data after this segment (This is the last view) we use the same last
        // value so that we end the curve with a horizontal slope
        index = offset + size;
        if (index >= mParams.mData.size()) {
            index--;
        }

        value = getDataValue(index, label);

        x = size * slotWidth + slotWidth / 2;
        y = normalizeY(value);

        put(list, size + 1, x, y);

        // Since the first and last extracted points are out of the view's area
        // we replace them with point inside the drawing area while keeping the slope of the
        // curve the same at the start and at the end.
        // We do this by replacing the first point by the midpoint between
        // the first and second, and the last point with the midpoint between the before last
        // and the last point.
        PointF first = list.get(0);
        PointF second = list.get(1);
        PointF last = list.get(size + 1);
        PointF beforeLast = list.get(size);

        midpoint(first, second, first);
        midpoint(last, beforeLast, last);
    }


    /**
     * Implements the algorithm generating the curve.
     *
     * @param offset the data index used to generate the first point of the curve.
     * @param size   the number of data point to use for drawing the curve
     * @param label  the label for the data points to use
     * @param dst    a path object to which the curve will be stored. The object must be instantiated
     *               by the client.
     */
    public void get(int offset, int size, @NonNull String label, @NonNull Path dst) {

        // Clear the path
        dst.rewind();

        // Extract the coordinates
        extractCoordinates(mCoordinates, offset, size, label);

        // Calculating the path...
        // First, move to the first extracted point
        PointF first = mCoordinates.get(0);
        dst.moveTo(first.x, first.y);

        // If there is only one slot to draw in this view we just draw a
        // line between the first and the last point
        if (size == 1) {
            first = mCoordinates.get(2);
            dst.lineTo(first.x, first.y);
            return;
        }

        // There is no curve segment between the first and second extracted points
        // we just draw a strait line between them.
        PointF second = mCoordinates.get(1);
        dst.lineTo(second.x, second.y);

        // We calculate the first control point between the second and third point
        // using the slope defined by the first and the second point (to keep the same
        // slope used to draw the line just before).
        // This will be the first control point used to draw the first cubic bezier curve
        // segment. Remember that each segment needs two control points. The second control
        // point for the first segment is calculated in the first iteration of the loop
        // below.
        PointF third = mCoordinates.get(2);
        controlPoint(third, second, delta(first, second), mTmpControlPoint);

        // For each iteration we extract three consecutive points
        // (`leftPoint`, `point` and `rightPoint`) then we calculate the control points
        // between `leftPoint` and `point` and between `point` and `rightPoint` in such a way
        // that the two curve segments that meet at the point `point` will have the same slope
        // at `point` (This is what the method controlPoints(...) does).
        // However, the two calculated control points in iteration are not the one that are
        // used to calculate the bezier segment in the same iteration. We use the second
        // control point that was calculated in the last iteration (For the first iteration
        // we use the one that was calculated below) and the first control point calculated in
        // the current iteration. The target point is the point `point` and we don't need to
        // specify the starting point since we use the last target point (This is how the
        // Path objects work).
        for (int i = 2; i < size; i++) {

            PointF leftPoint = mCoordinates.get(i - 1);
            PointF point = mCoordinates.get(i);
            PointF rightPoint = mCoordinates.get(i + 1);

            mControlPoint1.set(mTmpControlPoint);

            controlPoints(leftPoint, rightPoint, point,
                    mControlPoint2, mTmpControlPoint);

            dst.cubicTo(
                    mControlPoint1.x, mControlPoint1.y,
                    mControlPoint2.x, mControlPoint2.y,
                    point.x, point.y
            );
        }

        // The last control point that the loop above left us in the variable `mTmpControlPoint`
        // is the first control point that will be used to compute the last bezier segment.
        // The second one is calculated in such a way that the segment touches the point
        // `beforeLast` with the same slope defined by the points `beforeLast` and `last`.
        PointF last = mCoordinates.get(size + 1);
        PointF beforeLast = mCoordinates.get(size);
        PointF beforeBeforeLast = mCoordinates.get(size - 1);
        mControlPoint1.set(mTmpControlPoint);
        controlPoint(beforeBeforeLast, beforeLast, delta(beforeLast, last), mControlPoint2);

        dst.cubicTo(
                mControlPoint1.x, mControlPoint1.y,
                mControlPoint2.x, mControlPoint2.y,
                beforeLast.x, beforeLast.y
        );

        // Between the `beforeLast` and `last` point we just draw a strait line.
        dst.lineTo(last.x, last.y);
    }
}

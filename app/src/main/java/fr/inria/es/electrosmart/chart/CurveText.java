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


import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates the text to be drawn over a path
 * using {@link android.graphics.Canvas#drawTextOnPath(String, Path, float, float, Paint)}
 * <p>
 * The generated string follows the following rules:
 * - The text when drawn with the {@code {@link Paint} mParams.mCurveTextPaint} should have the
 * same length of the corespondent path with the precision of one character. If the text is
 * longer than the path the last character is removed.
 * - The generated string is a repetition of the label separated with the passed {@code separator}
 * string.
 * - The generated strings are rotated so that it is consistent with the string that was generated
 * for the views before. That is if the generated string for {@code mViewParams.mViewIndex - 1}
 * end with character number {@code N} of the {@code label + m-dash}
 * the generated string for the current view should start from character {@code N+1}
 */
class CurveText {

    // The string to use as a separator between the repeated labels
    private final String mSeparator;
    // A ChartParams object used to retrieve the paint used to draw the curved text
    final private ChartParams mParams;
    // The data structure to remember the last character in the last view
    private final SparseArrayCompat<Map<String, Integer>> mSymbolsTracker;
    // A StringBuilder object used to manipulate strings
    private final StringBuilder mStringBuilder;
    // A rect object used to hold size of text calculated with mPathMeasure
    private final Rect mTextBounds;
    // A PathMeasure object used to calculate the size that would be taken by the text
    // it was to be drawn in the view.
    private final PathMeasure mPathMeasure;

    /**
     * Constructor.
     *
     * @param params    A {@code ChartParams} object.
     * @param separator the string to be used as the separator between the repeated labels
     */
    CurveText(ChartParams params, @NonNull String separator) {
        mParams = params;
        mSeparator = separator;
        mSymbolsTracker = new SparseArrayCompat<>();
        mStringBuilder = new StringBuilder();
        mTextBounds = new Rect();
        mPathMeasure = new PathMeasure();
    }


    /**
     * Generates the string to be drawn over the curve represented by the {@code path}.
     * The object also remembers the last character drawn on a view.
     * The strings are then generated so that they join correctly at the borders.
     *
     * @param label the label that will be used to generate the string
     * @param path  the path over which the text will be drawn
     * @param index the index of the view
     * @return the generated text
     */
    public String get(String label, Path path, int index) {
        mPathMeasure.setPath(path, false);
        final float pathLength = mPathMeasure.getLength();
        Paint textPaint = mParams.mCurveTextPaint;

        // Constructing the text to draw over the path

        // Create a string: -<label>, example: -Wi-Fi
        mStringBuilder.setLength(0);
        final String baseString = mStringBuilder.append(mSeparator).append(label).toString();
        final int baseLength = baseString.length();

        int symbolIndex = 0;
        if (mSymbolsTracker.get(index - 1) != null &&
                mSymbolsTracker.get(index - 1).get(label) != null) {
            symbolIndex = mSymbolsTracker.get(index - 1).get(label);
        }

        // Rotate the string according to the last symbol in last view
        // Example: If the baseString is "-Wi-Fi"
        // And last view ended with a "...-Wi"
        // The rotated string will be: "-Fi-Wi"
        mStringBuilder.setLength(0);
        for (int i = 0; i < baseLength; i++) {
            mStringBuilder.append(baseString.charAt(
                    (symbolIndex + i) % baseLength
            ));
        }
        final String baseRotated = mStringBuilder.toString();

        // Use the rotated string to build the biggest part
        // that can be represented as a concatenation of the rotated string
        // (This is an optimization to reduce the number of the strings constructed
        // in the process and also the calls to Paint::getTextBounds())
        textPaint.getTextBounds(baseRotated,
                0, baseRotated.length(), mTextBounds);
        final int baseWidth = mTextBounds.width();
        final int baseCount = (int) (pathLength / baseWidth);
        mStringBuilder.setLength(0);
        for (int i = 0; i < baseCount; i++) {
            mStringBuilder.append(baseRotated);
        }

        // Construct the rest char by char
        textPaint.getTextBounds(mStringBuilder.toString(),
                0, mStringBuilder.length(), mTextBounds);
        while (mTextBounds.width() < pathLength) {
            mStringBuilder.append(baseString.charAt(symbolIndex));
            textPaint.getTextBounds(mStringBuilder.toString(),
                    0, mStringBuilder.length(), mTextBounds);
            symbolIndex = (symbolIndex + 1) % baseLength;
        }
        // If we exceed remove the last char
        if (mTextBounds.width() > pathLength) {
            mStringBuilder.deleteCharAt(mStringBuilder.length() - 1);
            if (symbolIndex == 0)
                symbolIndex = baseLength - 1;
            else
                symbolIndex = (symbolIndex - 1) % baseLength;
        }

        if (mSymbolsTracker.get(index) == null) {
            mSymbolsTracker.put(index, new HashMap<String, Integer>());
        }

        // Save index of last symbol for the next view
        mSymbolsTracker.get(index).put(label, symbolIndex);

        return mStringBuilder.toString();
    }
}

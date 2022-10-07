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
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An interface defining the interactions possible with a chart view.
 * <p>
 * Direct Known Subclasses:
 * {@link ChartViewImplAPI26}, {@link ChartViewImpl}, {@link ChartViewPlaceholder}
 */
abstract class ChartView extends View {

    // Holds the index of the view in the chart
    protected int mViewIndex;
    // Indicates if the view is fake (it does not draw itself and it is left empty)
    // This is used to improve performance when the chart is performing a long scroll
    // to a far away position. In this case, we prevent views from being drawn until
    // we reach the final scroll position.
    protected boolean mFake;

    /**
     * Constructor.
     *
     * @param context The Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     */
    public ChartView(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating from XML.
     *
     * @param context The Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Prepares the view for the next on draw event.
     * It is not necessary to call this method before a "on draw" event has occurred.
     * The "on draw" event will automatically trigger initialization if it is not
     * already initialized.
     */
    abstract void init();

    /**
     * Attaches the view to an index so that it can pick the right
     * data points to use while drawing the curves.
     *
     * @param index the index of the view
     */
    void setIndex(int index) {
        mViewIndex = index;
    }

    /**
     * Clears the view internal state rendering it ready to be recycled
     */
    abstract void clear();

    /**
     * Tells the view to not render itself (Used to improve programmatic scroll performance).
     *
     * @param fake true if the view is fake false if not
     */
    void setFake(boolean fake) {
        mFake = fake;
    }
}

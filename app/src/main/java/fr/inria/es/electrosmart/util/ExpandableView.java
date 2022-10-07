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

package fr.inria.es.electrosmart.util;

import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An expandable view. This implementation assumes a parent view and an indicator view to be passed
 * to the constructor. The indicator is used to indicate the expansion state by rotating it
 * 180 degrees when it is collapsed. The parent view is always visible unless the
 * visibility of the whole expandable list view is set to {@link View#INVISIBLE} or
 * {@link View#GONE}. After the object is constructed a set of children view can be added
 * to the object. These object will only be visible if the expandable view is expanded.
 * If it is collapsed the children are set to {@link View#GONE}.
 */
public class ExpandableView implements VisibilityGroup {

    /*
     * The parent view that is always visible unless `setVisibility(int)` is used to hide
     * everything.
     */
    @NonNull
    private final View mParent;
    /*
     * The indicator view that is rotated to indicate the expansion state.
     */
    @NonNull
    private final View mIndicator;
    /*
     * The visibility group object that is used to control the children views
     * visibility.
     */
    @NonNull
    private final VisibilityGroup mChildren;

    // The possible expansion states
    private static final int EXPANDED = 1;
    private static final int COLLAPSED = 2;

    // Holds the current state of the expandable view
    @State
    private int mState;

    /**
     * Annotation to control the {@code int} values that can be used as a state representation.
     */
    @IntDef({EXPANDED, COLLAPSED})
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD})
    @interface State {
    }

    /**
     * Constructor.
     *
     * @param parent    the parent view
     * @param indicator the expansion indicator view
     */
    public ExpandableView(@NonNull View parent, @NonNull View indicator) {
        mParent = parent;
        mIndicator = indicator;
        mChildren = new VisibilityGroupImpl();
        mState = EXPANDED;
        mParent.setOnClickListener(v -> {
            if (mState == EXPANDED) {
                collapse();
            } else if (mState == COLLAPSED) {
                expand();
            }
        });
    }

    /**
     * Adds a child.
     *
     * @param item the child to be added.
     */
    @Override
    public void addItem(@NonNull VisibilityItem item) {
        mChildren.addItem(item);
    }

    /**
     * Hides the children views and changes the state to {@link #COLLAPSED}
     */
    private void collapse() {
        mChildren.setVisibility(View.GONE);
        mIndicator.setScaleY(-1);
        mState = COLLAPSED;
    }


    /**
     * Displays the children views and changes the state to {@link #EXPANDED}
     */
    private void expand() {
        mChildren.setVisibility(View.VISIBLE);
        mIndicator.setScaleY(1);
        mState = EXPANDED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisibility(@Visibility int visibility) {
        mParent.setVisibility(visibility);
        collapse();
    }

}

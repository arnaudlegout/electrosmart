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

package fr.inria.es.electrosmart.ui;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

import fr.inria.es.electrosmart.R;


/**
 * Used to animate a {@link TextView} showing only numbers or the string {@link R.string#na}.
 * Makes the number on the text view progressively change from the previous value to the set value.
 * <p>
 * If the method {@link #setNA()} is used the number changes to progressively to 0 and then to
 * {@link R.string#na} at the end of the animation. Similarly if the previous
 * value is {@link R.string#na} the animation starts from 0 towards the set value
 * ({@link #setValue(int)}).
 */
public class NumberTextViewAnimation extends Animation {

    private TextView mTextView;
    private int mFrom;
    private int mTo;
    private boolean mIsNA;

    public NumberTextViewAnimation(TextView textView) {
        super();
        mTextView = textView;
        mTextView.setText(R.string.na);
        mIsNA = false;
        setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // the only way the mIsNA can be true is when setNA()
                // is called. This means that at the end of the animation
                // we should set the text to R.string#na.
                if (mIsNA) {
                    mTextView.clearAnimation();
                    mTextView.setText(R.string.na);
                    mIsNA = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    /**
     * Sets a number value to the text view. Makes the number value changes progressively
     * from the previous value to the new one. If the previous
     * value is {@link R.string#na} the animation starts from 0 towards the set value.
     *
     * @param to the value to set to the text view
     */
    public void setValue(int to) {
        mTextView.clearAnimation();
        mIsNA = false;
        try {
            // The text view is supposedly never changed outside of this object.
            // Hence the only non int value it can have is R.string#na.
            // So the exception catch below is supposed to handle the na case.
            mFrom = Integer.parseInt(mTextView.getText().toString());
        } catch (NumberFormatException ex) {
            mFrom = 0;
        }
        mTo = to;
        mTextView.startAnimation(this);
    }

    /**
     * Sets the string {@link R.string#na} to the text view by progressively changing the previous
     * value to 0 and then sets {@link R.string#na} at the end of the animation.
     */
    public void setNA() {
        mTextView.clearAnimation();
        try {
            // parseInt() throwing a NumberFormatException
            // means that the text view already set to R.string#na.
            // In this case we have no animation to run. We just return.
            mFrom = Integer.parseInt(mTextView.getText().toString());
        } catch (NumberFormatException ex) {
            return;
        }
        mTo = 0;
        mIsNA = true;
        mTextView.startAnimation(this);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);
        int value = (int) (mFrom + (mTo - mFrom) * interpolatedTime);
        mTextView.setText(Integer.toString(value));
    }
}

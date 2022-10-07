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

package fr.inria.es.electrosmart.fragments.owayn;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.fragments.MainActivityFragment;
import fr.inria.es.electrosmart.fragments.OwaynFragment;

/**
 * The fragment to show when the test finishes with a significant reduction value, that is
 * a reduction percentage value between [Const.PROTECTION_NO_REDUCTION_THRESHOLD, 99]
 * The protection is great if it is  in [Const.PROTECTION_GREAT_REDUCTION_THRESHOLD, 99]
 */
public class OwaynResultFragment extends Fragment {

    // The text view that shows the exposure reduction as a percentage
    private TextView mResultPercentValueTextView;
    // The text view that gives an assessment on the reduction percentage
    // "Great reduction" if the percentage is between [95..99]
    // "Not bad" if the percentage is between [80..94]
    private TextView mResultProtectionTextView;
    // The text view that suggests to the user the thing that they can do next
    // "Measure waves around you" if the percentage is between [95..99]
    // "You may try again to verify" if the percentage is between [80..94]
    private TextView mResultTodoTextView;
    // The listener to notify when the percentage is between [80..94] and the bottom button is
    // clicked.
    // When requested it returns to the explanation fragment.
    // Its behavior is handled by the parent fragment.
    private OnRestartListener mOnRestartListener;
    // The listener to notify when the percentage is between [95..99] and the bottom button is
    // clicked.
    // When requested it goes to the fragment with the specified id.
    // Its behavior is handled by the parent activity.
    private MainActivityFragment.OnNavigateToFragmentListener mOnNavigateToFragmentListener;
    // A boolean variable that keeps track of the fragment's visibility
    private boolean mIsVisibleToUser;
    // The bottom action button "Try again" or "Measure" according to the reduction percentage
    private AppCompatButton mTodoButton;
    // The current context
    private Context mContext;

    // A wrapper (Adapter) around mOnRestartListener
    private View.OnClickListener mTryAgainOnClickListener;
    // A wrapper (Adapter) around mOnCloseListener
    private View.OnClickListener mMeasureOnClickListener;

    // A clickable text view on top of the page that can be used to restart the test
    // when the bottom action button is set to close the test.
    private TextView mTryAgainTextView;
    // Adds an icon the mTryAgainTextView
    private ImageView mTryAgainImageView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owayn_result, container, false);
        mResultPercentValueTextView = view.findViewById(R.id.text_owayn_result_percent_value);
        mResultProtectionTextView = view.findViewById(R.id.text_owayn_result_protection);
        mResultTodoTextView = view.findViewById(R.id.text_owayn_result_to_do);
        mTodoButton = view.findViewById(R.id.button_owayn_result_todo);
        mTryAgainOnClickListener = v -> mOnRestartListener.onRestart();
        mMeasureOnClickListener = v -> mOnNavigateToFragmentListener.onBottomNavigateTo(R.id.bottom_nav_home);
        mTryAgainTextView = view.findViewById(R.id.text_owayn_result_try_again);
        mTryAgainImageView = view.findViewById(R.id.image_owayn_result_try_again);
        mTryAgainTextView.setOnClickListener(mTryAgainOnClickListener);
        mTryAgainImageView.setOnClickListener(mTryAgainOnClickListener);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof OnRestartListener) {
            mOnRestartListener = (OnRestartListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.getClass().getName()
                    + " must implement " + OnRestartListener.class.getName());
        }
        Activity parentActivity = getActivity();
        if (parentActivity instanceof MainActivityFragment.OnNavigateToFragmentListener) {
            mOnNavigateToFragmentListener = (MainActivityFragment.OnNavigateToFragmentListener)
                    parentActivity;
        } else {
            throw new RuntimeException(parentActivity.getClass().getName()
                    + " must implement " + MainActivityFragment.OnNavigateToFragmentListener
                    .class.getName());
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && !mIsVisibleToUser) {
            onShow();
        }
        mIsVisibleToUser = isVisibleToUser;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsVisibleToUser) {
            onShow();
        }
    }

    void onShow() {
        if (getContext() != null) {
            mResultPercentValueTextView.setText(getString(R.string.owayn_result_waves_blocked_percentage,
                    OwaynFragment.sResultReductionPercent));
            if (OwaynFragment.sResultReductionPercent >= Const.PROTECTION_GREAT_REDUCTION_THRESHOLD) {
                mTryAgainTextView.setVisibility(View.VISIBLE);
                mTryAgainImageView.setVisibility(View.VISIBLE);
                mResultProtectionTextView.setText(R.string.owayn_result_protection_great);
                mResultTodoTextView.setText(R.string.owayn_result_to_do_measure_waves);
                mTodoButton.setText(R.string.owayn_button_measure);
                mTodoButton.setOnClickListener(mMeasureOnClickListener);
            } else {
                mTryAgainTextView.setVisibility(View.INVISIBLE);
                mTryAgainImageView.setVisibility(View.INVISIBLE);
                mResultProtectionTextView.setText(R.string.owayn_result_protection_not_bad);
                mResultTodoTextView.setText(R.string.owayn_result_to_do_may_try_again);
                mTodoButton.setText(R.string.owayn_button_try_again);
                mTodoButton.setOnClickListener(mTryAgainOnClickListener);
            }
        }
    }

    /**
     * A listener interface that is used to implement the behavior to be executed
     * when the owayn test has to be redone.
     */
    public interface OnRestartListener {
        void onRestart();
    }

    public static class OwaynFragmentFactory implements OwaynFragment.OwaynFragmentFactory {
        @Override
        public Fragment getInstance() {
            return new OwaynResultFragment();
        }
    }
}

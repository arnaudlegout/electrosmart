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
package fr.inria.es.electrosmart.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import fr.inria.es.electrosmart.activities.OnBoardingActivity;

public class OnBoardingFragment extends Fragment {

    private static final String TAG = "OnBoardingFragment";
    // The argument key for the fragment id as defined in OnBoardingActivity#*__FRAGMENT_ID constants
    private static final String ARG_FRAGMENT_ID = "ARG_FRAGMENT_ID";
    // The behaviour of the fragment
    private OnBoardingActivity.OnBoardingFragmentStrategy mFragmentStrategy;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given fragment id
     */
    public static OnBoardingFragment create(int id) {
        OnBoardingFragment fragment = new OnBoardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_FRAGMENT_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return mFragmentStrategy.onCreateView(inflater, container);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity parentActivity = getActivity();
        if (parentActivity instanceof OnBoardingActivity) {
            mFragmentStrategy = ((OnBoardingActivity) parentActivity)
                    .mOnBoardingFragmentStrategies[getArguments().getInt(ARG_FRAGMENT_ID)];
        } else {
            throw new RuntimeException(parentActivity.getClass().getName()
                    + " must be instance of " + OnBoardingActivity.class.getName());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "in onResume()");
        mFragmentStrategy.onShown();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (isVisible()) {
            mFragmentStrategy.onLeft();
        }
    }
}


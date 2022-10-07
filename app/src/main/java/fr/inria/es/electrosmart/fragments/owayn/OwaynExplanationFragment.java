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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.fragments.OwaynFragment;

/**
 * The first fragment to show when the owayn test is started.
 */
public class OwaynExplanationFragment extends Fragment {

    // The listener to notify when the start button is clicked.
    // When requested it goes to the next step, that is the scan out fragment.
    // Its behavior is handled by the parent fragment.
    private OnStartClickListener mOnStartClickListener;

    // The current context
    Context mContext;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owayn_explanation, container, false);
        Button startButton = view.findViewById(R.id.button_owayn_start);
        startButton.setOnClickListener(v -> mOnStartClickListener.onStartClick());
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof OnStartClickListener) {
            mOnStartClickListener = (OnStartClickListener) parentFragment;
        } else {
            throw new RuntimeException(parentFragment.getClass().getSimpleName()
                    + " must implement " + OnStartClickListener.class.getName());
        }
        mContext = context;
    }

    /**
     * A listener interface that is used to implement the action to execute when the start button
     * is clicked.
     */
    public interface OnStartClickListener {
        void onStartClick();
    }

    public static class OwaynFragmentFactory implements OwaynFragment.OwaynFragmentFactory {
        @Override
        public Fragment getInstance() {
            return new OwaynExplanationFragment();
        }
    }
}

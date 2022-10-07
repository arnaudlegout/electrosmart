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

import android.content.Context;
import android.util.Log;

import androidx.fragment.app.Fragment;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.activities.MainActivity;

/**
 * A uniform interface to communicate with the fragments inflated in the
 * {@link fr.inria.es.electrosmart.activities.MainActivity}.
 */
public class MainActivityFragment extends Fragment {
    private static final String TAG = "MainActivityFragment";

    /**
     * A method to call in order to handle back button pressed event.
     *
     * @return true if the event is handled, false otherwise.
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * Check if there is a permission error or airplane mode. If not, we display
     * the {@link HomeFragment}, else we display the corresponding {@link ErrorFragment}
     *
     * @param mContext The fragment context
     * @param activity The activity object
     */
    void checkAndShowErrorIfExists(Context mContext, MainActivity activity) {
        Log.d(TAG, "checkAndShowErrorIfExists: ");
        int error = ErrorFragment.checkErrors(mContext);

        if (error == Const.ERROR_TYPE_AIRPLANE_MODE) {
            activity.showErrorFragment(Const.ERROR_TYPE_AIRPLANE_MODE);
        } else if (error == Const.ERROR_TYPE_NO_LOCATION_PERMISSION_DEVICE) {
            activity.showErrorFragment(Const.ERROR_TYPE_NO_LOCATION_PERMISSION_DEVICE);
        } else if (error == Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP) {
            activity.showErrorFragment(Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP);
        } else if (error == Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP_BACKGROUND) {
            activity.showErrorFragment(Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP_BACKGROUND);
        }
    }

    /**
     * To be called to check if the error is gone. It will display the home fragment is there is
     * no more error conditions.
     *
     * @param mContext The fragment context
     * @param activity The activity object
     */
    void checkErrorIsGoneAndShowHomeFragment(Context mContext, MainActivity activity) {
        Log.d(TAG, "checkErrorIsGoneAndShowHomeFragment: ");
        int error = ErrorFragment.checkErrors(mContext);
        if (error == Const.ERROR_TYPE_NO_ERROR) {
            activity.showHomeFragment();
        }
    }

    /**
     * Interface to communicate with the parent activity in order to navigate to a fragment
     */
    public interface OnNavigateToFragmentListener {
        /**
         * Navigates to a fragment from the bottom navigation bar
         *
         * @param itemId to id of the fragment to navigate to
         */
        void onBottomNavigateTo(int itemId);
    }
}

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

package fr.inria.es.electrosmart.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;

/*
 * The on-boarding process is started from the MainActivity.onResume() if
 * Const.IS_TERMS_OF_USE_ACCEPTED is false.
 *
 * 1. WelcomeActivity --(if Const.IS_TERMS_OF_USE_ACCEPTED is false)--> stars WelcomeTermsOfUseActivity, go to 2.
 *                    --(else if Const.IS_ONBOARDING_DONE is false)--> starts OnBoardingActivity, go to 3.
 *                    --(else)--> go to 4.
 *
 * 2. WelcomeTermsOfUseActivity  --(terms denied)--> we close the app
 *                               --(terms accepted)-->  Const.IS_TERMS_OF_USE_ACCEPTED set to true
 *                                                      OnBoardingActivity started
 *
 * 3. OnBoardingActivity  --(completed)--> Const.IS_ONBOARDING_DONE set to true (go to 4.)
 *
 *    Note that to complete the OnBoardingActivity, we just need to reach the last screen and hit
 *    Done. No information is mandatory, we just have to go through all screens.
 *
 * 4. We validate the location authorization (see below) and if it is granted, we start the services
 *    and the sync task.
 *
 * For the location authorization, electroSmart makes its best to reflect users' authorizations
 * in the behavior of the app. We have 2 cases to handle.
 *
 * 1) Starting with M, coarse location must be granted at runtime to authorize Wi-Fi and Cellular
 *    scans (it is not however required for BT scans). An easy solution would have been to start
 *    the foreground services when the shared preference Const.IS_TERMS_OF_USE_ACCEPTED is true.
 *    We decided to implement a slightly more complex strategy.
 *
 *    If the user does not grant the fine location (note that coarse location is enough to make
 *    scans, but both fine and coarse locations are dangerous locations, and we prefer fine
 *    location when we locate the position of the phone, so we directly ask for fine location), we
 *    DO NOT start the services and sync task. This choice is reflected in the shared
 *    preference Const.IS_AGREEMENT_FLOW_DONE. The reason for this strategy is that even if
 *    BT scans are still possible if the location permission is denied, it would not be much
 *    useful for us. In addition, managing user feedback for the different cases would be
 *    significantly harder. So we decided to consider all scans as a whole, either you have
 *    fine location grant and you can scan everything, or you don't and you cannot scan anything.
 *    Also this strategy is a way to give an incentive to the user to grant the location (if you
 *    don't authorize everything, you have no measurement).
 *
 *    Another design choice we made is that once you accept the fine grain location
 *    authorisation, Const.IS_AGREEMENT_FLOW_DONE is set to true, and never modified again whatever
 *    the change to the location authorization. Indeed, managing reverting back the agreement would
 *    be quite complex for a corner case. Indeed, we would need to decouple services and sync task
 *    management because even if you cannot make measurements anymore, you might still have data
 *    to synchronize.
 *
 * 2) If the user decide to revoke the location authorization for ElectroSmart, as we don't revert
 *    back the Const.IS_AGREEMENT_FLOW_DONE, we gracefully handle in the monitors this case in
 *    order to do not crash. Note that for consistency, we also decided to prevent BT scans in that
 *    case. This can be disputed and changed in the future.
 */
public class WelcomeTermsOfUseActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeTerms Activity";

    /**
     * Set the given app compat button disabled and change the color of the button accordingly
     *
     * @param context The context of the button
     * @param button  the button instance to be disabled
     */
    private void setButtonDisabled(Context context, AppCompatButton button) {
        button.setTextColor(ContextCompat.getColor(context, R.color.primary_text_color));
        button.setBackgroundResource(R.color.secondary_button_color);
        button.setEnabled(false);
    }

    /**
     * Set the given app compat button enabled and change the color of the button accordingly
     *
     * @param context The context of the button
     * @param button  the button instance to be enabled
     */
    private void setButtonEnabled(Context context, AppCompatButton button) {
        button.setTextColor(ContextCompat.getColor(context, R.color.inverted_primary_text_color));
        button.setBackgroundResource(R.drawable.es_button_blue);
        button.setEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tools.setStatusBarColor(this);
        setContentView(R.layout.activity_welcome_terms_of_use);

        // define the "let's go" button
        final AppCompatButton btnLetsGo = findViewById(R.id.btn_lets_go);
        // by default the "let's go" button is disabled, we must check the check box to enable it
        setButtonDisabled(this, btnLetsGo);

        // define the checkbox. If the check box is checked, we enable the "let's go" button
        CheckBox checkBoxIAccept = findViewById(R.id.checkbox_terms_of_use);
        checkBoxIAccept.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setButtonEnabled(WelcomeTermsOfUseActivity.this, btnLetsGo);
            } else {
                setButtonDisabled(WelcomeTermsOfUseActivity.this, btnLetsGo);
            }
        });

        // add the action to the "let's go" button. Note that the button can only be clicked if
        // the check box is checked.
        btnLetsGo.setOnClickListener(v -> {
            // To prevent the user from opening multiple instances of the onboarding activity
            // we disable the "Let's go" button once it is clicked.
            v.setEnabled(false);
            SharedPreferences.Editor edit =
                    getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE).edit();
            edit.putBoolean(Const.IS_TERMS_OF_USE_ACCEPTED, true).apply();

            // The user has agreed the terms of use, we now go on the on-boarding process
            Intent intent = new Intent(WelcomeTermsOfUseActivity.this, OnBoardingActivity.class);
            startActivityForResult(intent, Const.REQUEST_CODE_ONBOARDING_ACTIVITY);

            // Obtain the profileId for the user in an async task
            // Note: Here we don't check whether the user is on cellular or Wi-Fi as the
            // request/response is tiny
            new Tools.GetProfileIdAsyncTask().execute();
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "in onActivityResult(): requestCode " + requestCode + " resultCode " + resultCode);
        if (requestCode == Const.REQUEST_CODE_ONBOARDING_ACTIVITY) {
            if (resultCode == Const.RESULT_CODE_FINISH_ES) {
                Log.d(TAG, "in onActivityResult(): User either back pressed or refused the terms of " +
                        "use in the WelcomeTermsOfUseActivity. Hence, we close the application.");
                setResult(Const.RESULT_CODE_FINISH_ES);
            } else if (resultCode == Const.RESULT_CODE_ONBOARDING_DONE) {
                Log.d(TAG, "in onActivityResult(): Onboarding done");
                setResult(Const.RESULT_CODE_ONBOARDING_DONE);
            }
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "in onBackPressed(): Setting Activity result code - " + Const.RESULT_CODE_FINISH_ES);
        setResult(Const.RESULT_CODE_FINISH_ES);
        super.onBackPressed();
    }

}

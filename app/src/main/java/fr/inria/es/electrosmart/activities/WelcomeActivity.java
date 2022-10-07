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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;

public class WelcomeActivity extends AppCompatActivity {
    private static final String TAG = "WelcomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tools.setStatusBarColor(this);
        setContentView(R.layout.activity_welcome);

        // we add the action to the start button. When we touch it,
        // we start the WelcomeTermsOfUseActivity
        AppCompatButton textViewStart = findViewById(R.id.welcomButtonStart);
        textViewStart.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, WelcomeTermsOfUseActivity.class);
            startActivityForResult(intent, Const.REQUEST_CODE_WELCOME_TERMS_OF_USE);
        });

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "in onBackPressed(): Setting Activity result code - " + Const.RESULT_CODE_FINISH_ES);
        setResult(Const.RESULT_CODE_FINISH_ES);
        super.onBackPressed();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "in onActivityResult(): requestCode " + requestCode + " resultCode " + resultCode);
        if (requestCode == Const.REQUEST_CODE_WELCOME_TERMS_OF_USE) {
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

}

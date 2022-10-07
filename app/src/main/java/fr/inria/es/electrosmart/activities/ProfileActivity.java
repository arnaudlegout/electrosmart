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

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.UserProfile;
import fr.inria.es.electrosmart.database.DbRequestHandler;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    EditText editTextAge = null;
    RadioButton radioButtonMale = null;
    RadioButton radioButtonFemale = null;
    RadioButton radioButtonCurious = null;
    RadioButton radioButtonConcerned = null;
    RadioButton radioButtonElectroSensible = null;
    EditText editTextEmail = null;
    EditText editTextName = null;
    AppCompatButton btnClear = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tools.setStatusBarColor(this);
        setContentView(R.layout.activity_profile);

        // We retrieve all the components in the profile activity
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextName = findViewById(R.id.edit_text_name);
        editTextAge = findViewById(R.id.edit_text_age);
        radioButtonMale = findViewById(R.id.radio_sex_male);
        radioButtonFemale = findViewById(R.id.radio_sex_female);
        radioButtonCurious = findViewById(R.id.radio_user_segment_curious);
        radioButtonConcerned = findViewById(R.id.radio_user_segment_concerned);
        radioButtonElectroSensible = findViewById(R.id.radio_user_segment_electrosensible);
        btnClear = findViewById(R.id.btn_clear);
        btnClear.setEnabled(true);

        if (MainActivity.sUserProfile != null) {
            initializeViewData();
        }

        // We define the action for the clear profile button
        btnClear.setOnClickListener(v -> Tools.createYesNoDialog(
                ProfileActivity.this,
                getString(R.string.profile_activity_clear_dialog_title),          // dialog title
                getString(R.string.profile_activity_clear_dialog_description),  // dialog text
                getString(R.string.yes),                    // ok button text
                new DialogInterface.OnClickListener() {                           // ok button click listener
                    public void onClick(DialogInterface dialog, int id) {
                        // clear the profile
                        MainActivity.sUserProfile = new UserProfile(
                                Const.PROFILE_NAME_UNKNOWN,
                                Const.PROFILE_EMAIL_UNKNOWN,
                                Const.PROFILE_SEX_UNKNOWN,
                                Const.PROFILE_AGE_UNKNOWN,
                                Const.PROFILE_SEGMENT_UNKNOWN
                        );
                        // reset all user profile fields in the database
                        DbRequestHandler.updateUserProfile(
                                Const.PROFILE_NAME_UNKNOWN,
                                Const.PROFILE_EMAIL_UNKNOWN,
                                Const.PROFILE_SEX_UNKNOWN,
                                Const.PROFILE_AGE_UNKNOWN,
                                Const.PROFILE_SEGMENT_UNKNOWN,
                                System.currentTimeMillis());
                        // update the profile activity components
                        initializeViewData();
                    }
                },
                getString(R.string.no),        // cancel button text
                new DialogInterface.OnClickListener() {                           // cancel button click listener
                    public void onClick(DialogInterface dialog, int id) {
                        // cancel and do nothing
                        dialog.cancel();
                    }
                }
        ));
    }

    /**
     * Sets all the profile activity components to the values stored in MainActivity.sUserProfile
     */
    void initializeViewData() {
        // set the existing name MainActivity.sUserProfile
        editTextName.setText(MainActivity.sUserProfile.getName());
        // set the existing email in MainActivity.sUserProfile
        editTextEmail.setText(MainActivity.sUserProfile.getEmail());
        // set the existing age in MainActivity.sUserProfile
        int age = MainActivity.sUserProfile.getAge();
        if (age == -1) {
            editTextAge.setText("");
        } else {
            editTextAge.setText(String.valueOf(age));
        }
        // set the existing sex in MainActivity.sUserProfile
        radioButtonMale.setChecked(false);
        radioButtonFemale.setChecked(false);
        if (MainActivity.sUserProfile.getSex() == Const.PROFILE_SEX_MALE) {
            radioButtonMale.setChecked(true);
        } else if (MainActivity.sUserProfile.getSex() == Const.PROFILE_SEX_FEMALE) {
            radioButtonFemale.setChecked(true);
        }
        // set the existing segment in MainActivity.sUserProfile
        radioButtonConcerned.setChecked(false);
        radioButtonCurious.setChecked(false);
        radioButtonElectroSensible.setChecked(false);
        if (MainActivity.sUserProfile.getSegment() == Const.PROFILE_SEGMENT_CURIOUS) {
            radioButtonCurious.setChecked(true);
        } else if (MainActivity.sUserProfile.getSegment() == Const.PROFILE_SEGMENT_CONCERNED) {
            radioButtonConcerned.setChecked(true);
        } else if (MainActivity.sUserProfile.getSegment() == Const.PROFILE_SEGMENT_ELECTROSENSITIVE) {
            radioButtonElectroSensible.setChecked(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_PROFILE_ACTIVITY_ON_PAUSE);
        saveData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_PROFILE_ACTIVITY_ON_RESUME);
    }

    /**
     * Saves the data in the profile activity to the database. This is called when the activity
     * goes onPause
     */
    private void saveData() {
        Log.d(TAG, "Going to save data");
        int age;
        try {
            // editTextAge contains either an empty string or the string representation of a number
            age = Integer.parseInt(editTextAge.getText().toString());
        } catch (NumberFormatException nfe) {
            age = -1;
        }

        Tools.easterEggDumbledore(this, age);

        String name = editTextName.getText().toString();
        String email = editTextEmail.getText().toString();
        int sex = radioButtonMale.isChecked() ? Const.PROFILE_SEX_MALE :
                radioButtonFemale.isChecked() ? Const.PROFILE_SEX_FEMALE :
                        Const.PROFILE_SEX_UNKNOWN;
        int segment = radioButtonElectroSensible.isChecked() ? Const.PROFILE_SEGMENT_ELECTROSENSITIVE :
                radioButtonConcerned.isChecked() ? Const.PROFILE_SEGMENT_CONCERNED :
                        radioButtonCurious.isChecked() ? Const.PROFILE_SEGMENT_CURIOUS :
                                Const.PROFILE_SEGMENT_UNKNOWN;

        // we update the DB with the profile activity fields
        DbRequestHandler.updateUserProfile(name, email, sex, age, segment,
                System.currentTimeMillis());
        // we update the MainActivity.sUserProfile object
        MainActivity.sUserProfile = new UserProfile(name, email, sex, age, segment);
    }
}

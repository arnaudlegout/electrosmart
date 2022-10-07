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
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.util.ArrayList;
import java.util.List;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;

public class FeedbackActivity extends AppCompatActivity {
    private static final String TAG = "FeedbackActivity";
    private static final String MESSAGE_BODY_KEY = "message_body";
    private static final String MESSAGE_TYPE_KEY = "message_type";
    private static final String CHECKBOX_STATE_KEY = "checkbox_state";

    private static final String EMAIL_SUBJECT_HIDDEN_TEXT = "hidden";

    private List<String> categories = new ArrayList<>();
    private String feedbackType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tools.setStatusBarColor(this);
        setContentView(R.layout.activity_feedback);

        //init spinner Drop down elements
        categories.add(getResources().getString(R.string.feedback_type_bug));
        categories.add(getResources().getString(R.string.feedback_type_feature));
        categories.add(getResources().getString(R.string.feedback_type_general));
        categories.add(getResources().getString(R.string.feedback_type_suggest_solution));

        // Creating and attach adapter for the spinner
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.feedback_spinner);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(spinnerAdapter);

        // Spinner selection listener
        ((Spinner) findViewById(R.id.feedback_spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //Note: this event handler is called even when the spinner is initialized and its first item is selected.
                //remember the chosen feedback type
                feedbackType = adapterView.getItemAtPosition(i).toString();

                Log.d(TAG, "onItemSelected: feedbackType = " + feedbackType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //the send button onClick listener
        AppCompatButton button = findViewById(R.id.send_feedback_button);
        button.setOnClickListener(view -> {
            //check for the message body correctness
            String feedbackBody = ((EditText) findViewById(R.id.feedback_text_body)).getText().toString();
            if (feedbackBody.isEmpty()) {
                Toast.makeText(getApplicationContext(), getResources().getString(
                        R.string.toast_feedback_empty_message), Toast.LENGTH_LONG).show();
                return;
            }

            //open default email client with the user's registered account and fill in the subject/body of the email from the filled in form
            sendEmail(feedbackType, feedbackBody);
        });
    }

    private void sendEmail(String type, String emailBody) {
        //create the email sender intent to show up only email clients
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "", null));

        String profileId = DbRequestHandler.getProfileIdFromDB(); //get the profile ID from the DB

        if (type.equals(getResources().getString(R.string.feedback_type_feature))) {
            String[] to = {Const.FEEDBACK_EMAIL_BUGREPORT};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                    String.format(getString(R.string.feedback_message_subject_format),
                            Const.FEEDBACK_EMAIL_SUBJECT_PREFIX_FEATURE,
                            Tools.getAppVersionName(),
                            Tools.getAppVersionNumber(),
                            ((CheckBox) findViewById(R.id.checkbox_profileid)).isChecked() ? profileId : EMAIL_SUBJECT_HIDDEN_TEXT)
            );
        } else if (type.equals(getResources().getString(R.string.feedback_type_general))) {
            String[] to = {Const.FEEDBACK_EMAIL_SUPPORT};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                    String.format(getString(R.string.feedback_message_subject_format),
                            Const.FEEDBACK_EMAIL_SUBJECT_PREFIX_FEEDBACK,
                            Tools.getAppVersionName(),
                            Tools.getAppVersionNumber(),
                            ((CheckBox) findViewById(R.id.checkbox_profileid)).isChecked() ? profileId : EMAIL_SUBJECT_HIDDEN_TEXT)
            );
        } else if (type.equals(getResources().getString(R.string.feedback_type_bug))) {
            String[] to = {Const.FEEDBACK_EMAIL_BUGREPORT};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                    String.format(getString(R.string.feedback_message_subject_format),
                            Const.FEEDBACK_EMAIL_SUBJECT_PREFIX_BUG,
                            Tools.getAppVersionName(),
                            Tools.getAppVersionNumber(),
                            ((CheckBox) findViewById(R.id.checkbox_profileid)).isChecked() ? profileId : EMAIL_SUBJECT_HIDDEN_TEXT)
            );
        } else if (type.equals(getString(R.string.feedback_type_suggest_solution))) {
            String[] to = {Const.FEEDBACK_EMAIL_SUPPORT};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT,
                    String.format(getString(R.string.feedback_message_subject_format),
                            Const.FEEDBACK_EMAIL_SUBJECT_PREFIX_SUGGEST_SOLUTION,
                            Tools.getAppVersionName(),
                            Tools.getAppVersionNumber(),
                            ((CheckBox) findViewById(R.id.checkbox_profileid)).isChecked() ? profileId : EMAIL_SUBJECT_HIDDEN_TEXT)
            );
        } else {
            Log.d(TAG, "sendEmail: unexpected type of the feedback message!");
            return;
        }

//        String[] cc = {""};
//        emailIntent.putExtra(Intent.EXTRA_CC, cc);

        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);

        try {
            //open the email program chooser so that the user may choose from the list of installed email clients
            startActivityForResult(Intent.createChooser(emailIntent, getResources().getString(R.string.feedback_send_email_title)), Const.REQUEST_CODE_SEND_EMAIL);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_feedback_no_email_client), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: started");

        // the resultCode and the returned Intent cannot be exploited, because there is no standard
        // way for an email client to set this code and Intent. If ever the mail client is called,
        // we just reset the fields in the UI.
        if (requestCode == Const.REQUEST_CODE_SEND_EMAIL) {
            Log.d(TAG, "onActivityResult: requestCode positive!");
            flushUI();  //reset the UI elements' state
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_FEEDBACK_ACTIVITY_ON_RESUME);
        String string = getIntent().getStringExtra(Const.FEEDBACK_DEFAULT_TYPE_EXTRA_KEY);
        if (string != null) {
            // Check if we come from another fragment and set the spinner accordingly
            if (string.equals(Const.FEEDBACK_DEFAULT_EXTRA_VALUE_SUGGEST_SOLUTION)) {
                ((Spinner) findViewById(R.id.feedback_spinner)).setSelection(3);
                ((EditText) findViewById(R.id.feedback_text_body)).setHint(
                        getString(R.string.feedback_edit_text_solution));
            }
        } else {
            //restore the UI elements' state from the shared preferences
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            ((Spinner) findViewById(R.id.feedback_spinner)).setSelection((int) sharedPref.getLong(MESSAGE_TYPE_KEY, 0));
            ((CheckBox) findViewById(R.id.checkbox_profileid)).setChecked(sharedPref.getBoolean(CHECKBOX_STATE_KEY, true));
            ((EditText) findViewById(R.id.feedback_text_body)).setText(sharedPref.getString(MESSAGE_BODY_KEY, ""));
        }
    }

    @Override
    protected void onPause() {
        //save the field values/states in the activity's shared preferences before leaving this activity
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putLong(MESSAGE_TYPE_KEY, ((Spinner) findViewById(R.id.feedback_spinner)).getSelectedItemId());
        editor.putBoolean(CHECKBOX_STATE_KEY, ((CheckBox) findViewById(R.id.checkbox_profileid)).isChecked());
        editor.putString(MESSAGE_BODY_KEY, ((EditText) findViewById(R.id.feedback_text_body)).getText().toString());
        editor.apply();

        super.onPause();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_FEEDBACK_ACTIVITY_ON_PAUSE);
    }

    /**
     * Reinitialize all the states of the UI components in the shared preferences.
     */
    private void flushUI() {
        //clear data from the fields
        ((Spinner) findViewById(R.id.feedback_spinner)).setSelection(0);
        ((CheckBox) findViewById(R.id.checkbox_profileid)).setChecked(true);
        ((EditText) findViewById(R.id.feedback_text_body)).setText("");

        //clear data from the shared prefs as well
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(MESSAGE_TYPE_KEY, ((Spinner) findViewById(R.id.feedback_spinner)).getSelectedItemId());
        editor.putBoolean(CHECKBOX_STATE_KEY, true);
        editor.putString(MESSAGE_BODY_KEY, "");
        editor.apply();
    }

}

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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.UserProfile;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.fragments.OnBoardingFragment;
import fr.inria.es.electrosmart.serversync.SyncUtils;

public class OnBoardingActivity extends AppCompatActivity {
    private static final String TAG = "OnBoardingActivity";

    // IMPORTANT: update the number of fragments according to the number of uniquely
    // defined fragments below
    private static final int NB_FRAGMENTS = 6;
    // Uniquely defines the fragments
    //private static final int EMAIL_FRAGMENT_ID = 0;
    private static final int LOCATION_FRAGMENT_ID = 0;
    private static final int LOCATION_BACKGROUND_FRAGMENT_ID = 1;
    private static final int SEGMENT_FRAGMENT_ID = 2;
    private static final int ONBOARDING_DONE_FRAGMENT_ID = 3;
    private static final int LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID = 4;
    private static final int LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID = 5;

    // Used to decide whether to show the location fragment when there is no granted
    // location permission. We show it on first app start only. However, we do not persist this
    // variable, so on app restart we show again the location fragment if there is no
    // location permission. This is a good strategy to avoid showing an error fragment after
    // a long period. It is better to start with an explanation.
    private static boolean shouldShowLocationFragment = false;

    // This variable take as value the uniquely defined fragments ID above.
    // private static int currently_show_fragment = -1;
    // Defines the behaviour of each fragment when created, shows, and left.
    public final OnBoardingFragmentStrategy[] mOnBoardingFragmentStrategies =
            new OnBoardingFragmentStrategy[NB_FRAGMENTS];
    OnBoardingFragmentStrategy currentFragment;
    private boolean isOnboardingDone = false;
    // Variables to retrieve the on-boarding data
    private UserProfile mUserProfile;

    public OnBoardingActivity() {

/*        // We create the EMAIL_FRAGMENT_ID strategy
        mOnBoardingFragmentStrategies[EMAIL_FRAGMENT_ID] = new OnBoardingFragmentStrategy() {
            EditText mEditTextEmail;
            TextView mEmailExplanation;
            AppCompatButton mBtnNext;
            KeyboardVisibilityListener mKeyboardVisibilityListener;

            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
                ViewGroup view = (ViewGroup) inflater.inflate(
                        R.layout.fragment_onboarding_email, container, false
                );

                mEditTextEmail = view.findViewById(R.id.edit_text_email);
                mEmailExplanation = view.findViewById(R.id.text_view_question_description);

                mBtnNext = view.findViewById(R.id.btn_next);
                mBtnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: mBtnNext onClicked");
                        onLeft();
                        loadFragment(LOCATION_FRAGMENT_ID);
                    }
                });

                // we set the existing email previously saved if it exists
                mEditTextEmail.setText(mUserProfile.getEmail());

                // When the keyboard is shown, as it takes a lot of space, we remove the explanation
                // TextView
                mKeyboardVisibilityListener = new KeyboardVisibilityListener() {
                    @Override
                    public void onKeyboardVisibilityChanged(boolean keyboardVisible) {
                        Log.d(TAG, "onKeyboardStateChanged: state = " + keyboardVisible);
                        if (keyboardVisible) {
                            mEmailExplanation.setVisibility(View.INVISIBLE);
                        } else {
                            mEmailExplanation.setVisibility(View.VISIBLE);
                        }
                    }
                };
                setKeyboardVisibilityListener(OnBoardingActivity.this, mKeyboardVisibilityListener);
                return view;
            }

            @Override
            public void onLeft() {
                Log.d(TAG, "onLeft: EMAIL_FRAGMENT_ID");
                mUserProfile.setEmail(mEditTextEmail.getText().toString());
                closeSoftKeyboard();
            }

            @Override
            public void onShown() {
                Log.d(TAG, "onShown: EMAIL_FRAGMENT_ID");
                currently_show_fragment = EMAIL_FRAGMENT_ID;

                mEditTextEmail.setText(mUserProfile.getEmail());
            }
        };*/

        // We create the LOCATION_FRAGMENT_ID strategy
        mOnBoardingFragmentStrategies[LOCATION_FRAGMENT_ID] = new OnBoardingFragmentStrategy() {
            AppCompatButton mButtonAuthorize;

            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {

                int layout_id;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    layout_id = R.layout.fragment_onboarding_location_api_23_28;
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    layout_id = R.layout.fragment_onboarding_location_api_29;
                } else {
                    layout_id = R.layout.fragment_onboarding_location_foreground_api_30;
                }

                ViewGroup view = (ViewGroup) inflater.inflate(
                        layout_id, container, false
                );

                mButtonAuthorize = view.findViewById(R.id.btn_authorize);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    mButtonAuthorize.setText(getString(R.string.onboarding_btn_app_location_permission_authorize));
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    mButtonAuthorize.setText(getString(R.string.onboarding_btn_app_location_permission_always_authorize));
                } else {
                    mButtonAuthorize.setText(getString(R.string.onboarding_btn_app_location_permission_authorize));
                }

                mButtonAuthorize.setOnClickListener(v -> {
                    Log.d(TAG, "onClick: Authorize clicked");
                    shouldShowLocationFragment = true;
                    Tools.grantLocationToApp(OnBoardingActivity.this, OnBoardingActivity.this, true);
                });

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AppCompatTextView textView = view.findViewById(R.id.text_error_description);
                    Tools.setHtmlTextToTextView(textView,
                            getString(R.string.onboarding_location_permission_explanation_foreground_api31));
                }


                return view;
            }

            @Override
            public void onLeft() {
                Log.d(TAG, "onLeft: LOCATION_FRAGMENT_ID");
            }

            @Override
            public void onShown() {
                Log.d(TAG, "onShown: LOCATION_FRAGMENT_ID");
                closeSoftKeyboard();
            }

            @Override
            public int getFragmentId() {
                return LOCATION_FRAGMENT_ID;
            }
        };

        // We create the LOCATION_BACKGROUND_FRAGMENT_ID strategy
        mOnBoardingFragmentStrategies[LOCATION_BACKGROUND_FRAGMENT_ID] = new OnBoardingFragmentStrategy() {
            AppCompatButton mButtonAuthorize;

            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {

                ViewGroup view = (ViewGroup) inflater.inflate(
                        R.layout.fragment_onboarding_location_backgorund_api_30, container, false
                );

                mButtonAuthorize = view.findViewById(R.id.btn_authorize);

                // wrong lint warning, we enter this portion of code only for SDK R+
                mButtonAuthorize.setText(getPackageManager().getBackgroundPermissionOptionLabel());

                mButtonAuthorize.setOnClickListener(v -> {
                    Log.d(TAG, "onClick: Authorize clicked");
                    Tools.grantLocationToApp(OnBoardingActivity.this, OnBoardingActivity.this, false);
                });

                return view;
            }

            @Override
            public void onLeft() {
                Log.d(TAG, "onLeft: LOCATION_BACKGROUND_FRAGMENT_ID");
            }

            @Override
            public void onShown() {
                Log.d(TAG, "onShown: LOCATION_BACKGROUND_FRAGMENT_ID");
                closeSoftKeyboard();
            }

            @Override
            public int getFragmentId() {
                return LOCATION_BACKGROUND_FRAGMENT_ID;
            }
        };

        // We create the SEGMENT_FRAGMENT_ID strategy
        mOnBoardingFragmentStrategies[SEGMENT_FRAGMENT_ID] = new OnBoardingFragmentStrategy() {
            RadioButton mRadioButtonElectrosensitive;
            RadioButton mRadioButtonConcerned;
            RadioButton mRadioButtonCurious;
            AppCompatButton mBtnStartElectroSmart;

            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
                ViewGroup view = (ViewGroup) inflater.inflate(
                        R.layout.fragment_onboarding_segment, container, false
                );

                mRadioButtonElectrosensitive = view.findViewById(R.id.radio_button_electrosensitive);
                mRadioButtonConcerned = view.findViewById(R.id.radio_button_concerned);
                mRadioButtonCurious = view.findViewById(R.id.radio_button_curious);
                mBtnStartElectroSmart = view.findViewById(R.id.btn_start_es);
                mBtnStartElectroSmart.setOnClickListener(v -> {
                    Log.d(TAG, "onClick: ");
                    loadFragment(ONBOARDING_DONE_FRAGMENT_ID);
                });
                return view;
            }

            @Override
            public void onLeft() {
                Log.d(TAG, "onLeft: SEGMENT_FRAGMENT_ID");
                if (mRadioButtonElectrosensitive.isChecked()) {
                    mUserProfile.setSegment(Const.PROFILE_SEGMENT_ELECTROSENSITIVE);
                } else if (mRadioButtonConcerned.isChecked()) {
                    mUserProfile.setSegment(Const.PROFILE_SEGMENT_CONCERNED);
                } else if (mRadioButtonCurious.isChecked()) {
                    mUserProfile.setSegment(Const.PROFILE_SEGMENT_CURIOUS);
                } else {
                    mUserProfile.setSegment(Const.PROFILE_SEGMENT_UNKNOWN);
                }
            }

            @Override
            public void onShown() {
                Log.d(TAG, "onShown: SEGMENT_FRAGMENT_ID");

                closeSoftKeyboard();
                if (mUserProfile.getSegment() == Const.PROFILE_SEGMENT_ELECTROSENSITIVE) {
                    mRadioButtonElectrosensitive.setChecked(true);
                } else if (mUserProfile.getSegment() == Const.PROFILE_SEGMENT_CONCERNED) {
                    mRadioButtonConcerned.setChecked(true);
                } else if (mUserProfile.getSegment() == Const.PROFILE_SEGMENT_CURIOUS) {
                    mRadioButtonCurious.setChecked(true);
                }

                /*
                 We are in the User Segment fragment. Therefore, it means that we have at least
                 fine location permission (starting with Android Q, at least location permission
                 while the app is in use) and the user agreement is done. So, it is safe to start
                 the services as soon as we show the segment fragment to the user

                 Note that we have the guarantee that this method (onShown) will be called
                 before the on boarding is done (so before the HomeFragment is displayed).
                 Indeed, the only one way to go to the ONBOARDING_DONE_FRAGMENT_ID is from
                 this SEGMENT_FRAGMENT_ID.

                 If ever this logic is changed in the future, we must take care of properly setting
                 Const.IS_AGREEMENT_FLOW_DONE because it is the only one way to start the measures.
                */
                SharedPreferences settings = MainApplication.getContext().
                        getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = settings.edit();
                edit.putBoolean(Const.IS_AGREEMENT_FLOW_DONE, true).apply();

                MainActivity.startAllServices();
            }

            @Override
            public int getFragmentId() {
                return SEGMENT_FRAGMENT_ID;
            }
        };

        // We create the ONBOARDING_DONE_FRAGMENT_ID strategy
        mOnBoardingFragmentStrategies[ONBOARDING_DONE_FRAGMENT_ID] = new OnBoardingFragmentStrategy() {
            ProgressBar mConfigurationProgressBar;
            AppCompatImageView mImageStartOfService, mImageAlertPersonalization, mImageStartOfMeasure;
            AppCompatTextView mTextStartOfService, mTextAlertPersonalization, mTextStartOfMeasure;

            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
                ViewGroup view = (ViewGroup) inflater.inflate(
                        R.layout.fragment_onboarding_done, container, false
                );
                mConfigurationProgressBar = view.findViewById(R.id.progress_bar_es_configuration);
                mImageStartOfService = view.findViewById(R.id.image_start_of_service);
                mImageAlertPersonalization = view.findViewById(R.id.image_personalization_of_alerts);
                mImageStartOfMeasure = view.findViewById(R.id.image_start_of_measures);

                mTextStartOfService = view.findViewById(R.id.start_of_service);
                mTextAlertPersonalization = view.findViewById(R.id.personalization_of_alerts);
                mTextStartOfMeasure = view.findViewById(R.id.start_of_measures);
                return view;
            }

            @Override
            public void onLeft() {
                Log.d(TAG, "onLeft: ONBOARDING_DONE_FRAGMENT_ID");
                onBoardingDone();
            }

            @Override
            public void onShown() {
                Log.d(TAG, "onShown: ONBOARDING_DONE_FRAGMENT_ID");

                closeSoftKeyboard();

                // define the time in milliseconds to show each item in the animation
                final int firstItemTime = 1_500;
                final int secondItemTime = 3_000;
                final int thirdItemTime = 4_500;

                // define the time in milliseconds after which we start the HomeFragment
                final int maxProgressTime = 7_000;

                mConfigurationProgressBar.setMax(maxProgressTime);
                mConfigurationProgressBar.setProgress(0);
                new CountDownTimer(maxProgressTime,
                        1) {

                    // This method is called very frequently during the progress of the progress bar
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int millisSinceStart = (int) (maxProgressTime - millisUntilFinished);
                        mConfigurationProgressBar.setProgress(millisSinceStart);
                        if (firstItemTime < millisSinceStart && millisSinceStart < secondItemTime) {
                            mImageStartOfService.setVisibility(View.VISIBLE);
                            mTextStartOfService.setTextColor(
                                    ContextCompat.getColor(
                                            getApplicationContext(),
                                            R.color.primary_text_color
                                    )
                            );

                        } else if (secondItemTime <= millisSinceStart && millisSinceStart < thirdItemTime) {
                            mImageAlertPersonalization.setVisibility(View.VISIBLE);
                            mTextAlertPersonalization.setTextColor(
                                    ContextCompat.getColor(
                                            getApplicationContext(),
                                            R.color.primary_text_color
                                    )
                            );
                        } else if (thirdItemTime <= millisSinceStart) {
                            mImageStartOfMeasure.setVisibility(View.VISIBLE);
                            mTextStartOfMeasure.setTextColor(
                                    ContextCompat.getColor(
                                            getApplicationContext(),
                                            R.color.primary_text_color
                                    )
                            );
                        }
                    }

                    // This method is called when the progress bar is done
                    @Override
                    public void onFinish() {
                        Log.d(TAG, "onFinish: ");
                        onLeft();
                    }
                }.start();
            }

            @Override
            public int getFragmentId() {
                return ONBOARDING_DONE_FRAGMENT_ID;
            }
        };

        // We create the LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID strategy
        // NOTE: this fragment is shown to Android 10+ (Q+) devices only
        mOnBoardingFragmentStrategies[LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID] = new OnBoardingFragmentStrategy() {
            AppCompatButton mBtnAuthorizeAllTime, mBtnNext;

            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
                ViewGroup view = (ViewGroup) inflater.inflate(
                        R.layout.fragment_onboarding_loc_error_foreground_only, container, false
                );

                mBtnAuthorizeAllTime = view.findViewById(R.id.btn_authorize);
                mBtnNext = view.findViewById(R.id.btn_next);
                mBtnAuthorizeAllTime.setOnClickListener(v -> {
                    Log.d(TAG, "onClick: ");
                    onLeft();
                    Tools.grantLocationToApp(OnBoardingActivity.this, OnBoardingActivity.this, false);
                });
                mBtnNext.setOnClickListener(v -> {
                    Log.d(TAG, "onClick: ");

                    // The user clicked "continue anyway", so we must not show the
                    // the error fragment for the foreground only location permission when
                    // we start the app. It will be shown at the next app restart only.
                    SharedPreferences settings = getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = settings.edit();
                    edit.putBoolean(Const.SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR, false).apply();

                    // we must call this method manually, because onClick on this button there is
                    // no onResume of this activity (and checkPermissionAndSDKAndLoadFragment() is
                    // normally called onResume)
                    checkPermissionAndSDKAndLoadFragment();
                });

                // We are in the onboarding, so don't show the checkbox
                AppCompatCheckBox checkBoxDontAskAgain = view.findViewById(R.id.checkbox_dont_ask_again);
                checkBoxDontAskAgain.setVisibility(View.GONE);

                return view;
            }

            @Override
            public void onLeft() {
                Log.d(TAG, "onLeft: LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID");
            }

            @Override
            public void onShown() {
                Log.d(TAG, "onShown: LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID");

                closeSoftKeyboard();
            }

            @Override
            public int getFragmentId() {
                return LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID;
            }
        };


        // We create the LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID strategy
        // NOTE: this fragment is shown both to devices >= android 10+ and < android 10+
        mOnBoardingFragmentStrategies[LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID] = new OnBoardingFragmentStrategy() {
            AppCompatButton mBtnAuthorizeAllTime;

            @Override
            public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
                ViewGroup view = (ViewGroup) inflater.inflate(
                        R.layout.fragment_onboarding_loc_error_no_localisation, container, false
                );

                mBtnAuthorizeAllTime = view.findViewById(R.id.btn_authorize);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    mBtnAuthorizeAllTime.setText(getString(R.string.onboarding_btn_app_location_permission_authorize));
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    mBtnAuthorizeAllTime.setText(getString(R.string.onboarding_btn_app_location_permission_always_authorize));
                } else {
                    mBtnAuthorizeAllTime.setText(getString(R.string.onboarding_btn_app_location_permission_authorize));
                }

                AppCompatTextView textViewTitle = view.findViewById(R.id.text_view_error_title);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Tools.setHtmlTextToTextView(textViewTitle,
                            getString(R.string.onboarding_error_no_app_location_api31));
                }

                mBtnAuthorizeAllTime.setOnClickListener(v -> {
                    Log.d(TAG, "onClick: LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID");
                    onLeft();
                    Tools.grantLocationToApp(OnBoardingActivity.this, OnBoardingActivity.this, true);
                });
                return view;
            }

            @Override
            public void onLeft() {
                Log.d(TAG, "onLeft: LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID");
            }

            @Override
            public void onShown() {
                Log.d(TAG, "onShown: LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID");

                closeSoftKeyboard();
            }

            @Override
            public int getFragmentId() {
                return LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID;
            }

        };
    }

    /**
     * This set a listener to check if the keyboard is visible and invisible. The logic is to detect
     * a change in the height of the view. If the view height is shrunk, it means the keyboard
     * appeared.
     *
     * @param activity                   The activity
     * @param keyboardVisibilityListener a keyboard listener that will be called when the keyboard
     *                                   appear or disappear.
     */
    public static void setKeyboardVisibilityListener(AppCompatActivity activity,
                                                     final KeyboardVisibilityListener keyboardVisibilityListener) {
        final View contentView = activity.findViewById(android.R.id.content);
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int mPreviousHeight;

            @Override
            public void onGlobalLayout() {
                int newHeight = contentView.getHeight();
                if (mPreviousHeight != 0) {
                    if (mPreviousHeight > newHeight) {
                        // Height decreased: keyboard was shown
                        keyboardVisibilityListener.onKeyboardVisibilityChanged(true);
                    } else if (mPreviousHeight < newHeight) {
                        // Height increased: keyboard was hidden
                        keyboardVisibilityListener.onKeyboardVisibilityChanged(false);
                    }
                }
                mPreviousHeight = newHeight;
            }
        });
    }

    private void onBoardingDone() {
        Log.d(TAG, "onBoardingDone()");
        // We update the shared preference Const.IS_ONBOARDING_DONE to true
        SharedPreferences settings = getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean(Const.IS_ONBOARDING_DONE, true).apply();

        // We return the code Const.RESULT_CODE_ONBOARDING_DONE to the calling activity
        setResult(Const.RESULT_CODE_ONBOARDING_DONE);

        // Used by onPause to know that the data must be sent to our server
        isOnboardingDone = true;

        finish();
    }

    /**
     * This method contains the entire logic to load on-boarding fragments depending on user
     * permission choices and SDK possibilities. This method must be called onResume of the
     * {@link OnBoardingActivity}.
     * <p>
     * The logic is the following
     * <p>
     * - If SDK version < M
     * => jump to the segment fragment (no location permission requested)
     * <p>
     * - If  M <= SDK version  < Q
     * There is only a allow of deny dialog for the location permission (no distinction between
     * fine and background location).
     * <p>
     * A: allow
     * D: deny
     * <pre>
     *                                 M <= SDK < Q
     *                                                                              D
     *                                                                  ----------------------|
     *                                                                  |                     |
     *                                        D                        \/                     |
     *                LOCATION_FRAGMENT --------------> LOC_ERROR_NO_LOCALISATION_FRAGMENT ---|
     *                      |                                           |
     *                      | A                                         |
     *                     \/                      A                    |
     *              SEGMENT_FRAGMENT <----------------------------------
     *                      |
     *                      |
     *                     \/
     *             ONBOARDING_DONE_FRAGMENT
     * </pre>
     * <p>
     * - If SDK version == Q
     * User can select among no location, fine only (allow while the app is in use), and background
     * location (allow all the time).
     * <p>
     * ATT: Allow all the time (background)
     * WAU: Allow while the app is in use (foreground only)
     * OTT: Allow only this time (foreground only)
     * D: Deny
     * <pre>
     *                                      SDK == Q
     *                                                                              D
     *                                                                  ----------------------|
     *                                                                  |                     |
     *                                        D                        \/                     |
     *                LOCATION_FRAGMENT --------------> LOC_ERROR_NO_LOCALISATION_FRAGMENT ---|
     *                      |   |                            |                 |      /\
     *                      |   |                   ---------|         WAU/OTT |      |  D
     *                      |   |                   |                         \/      |
     *                      |   |-------------------|-> LOC_ERROR_FOREGROUND_ONLY_FRAGMENT
     *                 ATT  |          WAU/OTT      |                   |
     *                      |                       |                   |
     *                      |   --------------------|                   |
     *                      |   |      ATT                              |
     *                     \/  \/                  ATT                  |
     *              SEGMENT_FRAGMENT <----------------------------------
     *                      |
     *                      |
     *                     \/
     *             ONBOARDING_DONE_FRAGMENT
     * </pre>
     * <p>
     * Note that we manage two more cases with shouldShowLocErrorForegroundOnlyFragment and
     * shouldShowLocationFragment that we do not show in the drawing.
     * <p>
     * - If R <= SQK
     * <p>
     * <p>
     * ATT: Allow all the time (background)
     * WAU: Allow while the app is in use (foreground only)
     * OTT: Allow only this time (foreground only)
     * D: Deny
     * <pre>
     *                                      SDK >= R
     *                                                                              D
     *                                                                  ----------------------|
     *                                                                  |                     |
     *                                        D                        \/                     |
     *                LOCATION_FRAGMENT --------------> LOC_ERROR_NO_LOCALISATION_FRAGMENT ---|
     *                      |                                |  /\                    /\
     *                      |          ----------------------|  |                     |
     *                      |          |                        |  D                  | D
     *                      |          |    |--------------------                     |
     *              WAU/OTT |  WAU/OTT |    |                                         |
     *                      |          |    |           LOC_ERROR_FOREGROUND_ONLY_FRAGMENT
     *                      |          |    |                         /\   |  |        /\
     *                      |          |    |                         |    |  |        |
     *                      |          |    |                WAU/OTT  |    |  |---------
     *                     \/         \/    |                         |    |     WAU/OTT
     *             LOCATION_BACKGROUND_FRAGMENT -----------------------    |
     *                      |                                              |
     *                      |                                              |
     *                  ATT |                                              |
     *                      |                                              |
     *                      |                                              |
     *                      |                                              |
     *                     \/                      ATT                     |
     *              SEGMENT_FRAGMENT <--------------------------------------
     *                      |
     *                      |
     *                     \/
     *             ONBOARDING_DONE_FRAGMENT
     * </pre>
     * Note that we manage two more cases with shouldShowLocErrorForegroundOnlyFragment and
     * shouldShowLocationFragment that we do not show in the drawing.
     * <p>
     */
    private void checkPermissionAndSDKAndLoadFragment() {
        Log.d(TAG, "in checkPermissionAndSDKAndJumpToFragment() ");

        SharedPreferences settings = getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
        // set to false when the user click "Continue anyway" on the foreground only error fragment
        boolean shouldShowLocErrorForegroundOnlyFragment =
                settings.getBoolean(Const.SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR, true);

        // Depending on the SDK version and the already granted permissions, we might
        // jump to another fragment
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "checkPermissionAndSDKAndLoadFragment:  SDK < M (no permission requested)");
            // If the SDK version is before M, there is no localization permission to ask,
            // so we jump to the segment fragment
            loadFragment(SEGMENT_FRAGMENT_ID);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: M <= SDK < Q (allow only permission)");
            /*
             If the SDK version is between M and Q, and the location permission has
             been granted, we jump to the segment fragment.

             This might happen if we interrupt the on-boarding and come back to it
            */
            if (Tools.isAccessFineLocationGranted(OnBoardingActivity.this)) {
                Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: access fine already granted");
                loadFragment(SEGMENT_FRAGMENT_ID);
            } else if (shouldShowLocationFragment) {
                Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: access fine NOT granted and " +
                        "wasInitialFragmentShown true");
                loadFragment(LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID);
            } else {
                Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: access fine NOT granted and " +
                        "wasInitialFragmentShown false");
                loadFragment(LOCATION_FRAGMENT_ID);
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: SDK = Q (fine and background location)");
            /*
             If the SDK version is Q or later, and the location permission for for both
             foreground and background has been granted, we jump to the segment fragment.

             If only the location for foreground has been granted, we jump to the
             foreground only error fragment.

             This might happen if we interrupt the on-boarding and come back to it
            */
            if (Tools.isAccessFineLocationGranted(OnBoardingActivity.this) &&
                    Tools.isAccessBackgroundLocationGranted(OnBoardingActivity.this)) {
                Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: fine and background location " +
                        "granted");
                loadFragment(SEGMENT_FRAGMENT_ID);
            } else if (Tools.isAccessFineLocationGranted(OnBoardingActivity.this)) {
                Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: fine only location granted");
                if (shouldShowLocErrorForegroundOnlyFragment) {
                    Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: " +
                            "shouldShowLocErrorForegroundOnlyFragment true");
                    loadFragment(LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID);
                } else {
                    Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: " +
                            "shouldShowLocErrorForegroundOnlyFragment false");
                    loadFragment(SEGMENT_FRAGMENT_ID);
                }
            } else {
                Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: no location granted");
                if (shouldShowLocationFragment) {
                    Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: wasInitialFragmentShown " +
                            "true");
                    loadFragment(LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID);
                } else {
                    Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: wasInitialFragmentShown " +
                            "false");
                    loadFragment(LOCATION_FRAGMENT_ID);
                }
            }
        } else {
            // Here we are in SDK R+
            Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: SDK >= R (fine and background " +
                    "permissions in two different requests)");
            if (Tools.isAccessFineLocationGranted(OnBoardingActivity.this) &&
                    Tools.isAccessBackgroundLocationGranted(OnBoardingActivity.this)) {
                Log.d(TAG, "checkPermissionAndSDKAndJumpToFragment: fine and background" +
                        "location granted");
                loadFragment(SEGMENT_FRAGMENT_ID);
            } else if (Tools.isAccessFineLocationGranted(OnBoardingActivity.this)) {
                Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: fine location only");
                if (currentFragment != null) {
                    Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: " +
                            "currentFragment.getFragmentId(): " + currentFragment.getFragmentId());
                }
                if ((currentFragment == null) || (currentFragment.getFragmentId() == LOCATION_FRAGMENT_ID) ||
                        (currentFragment.getFragmentId() == LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID)) {
                    Log.d(TAG, "checkPermissionAndSDKAndJumpToFragment: show the " +
                            "LOCATION_BACKGROUND_FRAGMENT_ID");
                    loadFragment(LOCATION_BACKGROUND_FRAGMENT_ID);
                } else {
                    Log.d(TAG, "checkPermissionAndSDKAndJumpToFragment: do NOT show the " +
                            "LOCATION_BACKGROUND_FRAGMENT_ID");
                    if (shouldShowLocErrorForegroundOnlyFragment) {
                        Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: " +
                                "shouldShowLocErrorForegroundOnlyFragment true");
                        loadFragment(LOC_ERROR_FOREGROUND_ONLY_FRAGMENT_ID);
                    } else {
                        Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: " +
                                "shouldShowLocErrorForegroundOnlyFragment false");
                        loadFragment(SEGMENT_FRAGMENT_ID);
                    }
                }
            } else {
                Log.d(TAG, "checkPermissionAndSDKAndJumpToFragment: no location permission");
                if (shouldShowLocationFragment) {
                    Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: wasInitialFragmentShown true");
                    loadFragment(LOC_ERROR_NO_LOCALISATION_FRAGMENT_ID);
                } else {
                    Log.d(TAG, "checkPermissionAndSDKAndLoadFragment: wasInitialFragmentShown false");
                    loadFragment(LOCATION_FRAGMENT_ID);
                }
            }
        }
    }

    private void loadFragment(int fragmentId) {
        // we do not load two times the same fragment to avoid recursive calls
        if ((currentFragment == null) || (currentFragment.getFragmentId() != fragmentId)) {
            // we first close the currently opened fragment
            if (currentFragment != null) {
                currentFragment.onLeft();
            }
            currentFragment = mOnBoardingFragmentStrategies[fragmentId];
            OnBoardingFragment mFragment = OnBoardingFragment.create(fragmentId);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.onboarding_fragment_container, mFragment)
                    .commitAllowingStateLoss();
        }
    }

    private void closeSoftKeyboard() {
        Log.d(TAG, "closeSoftKeyboard: ");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View focusedView = getCurrentFocus();
        if (imm != null && focusedView != null) {
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "in onCreate()");

        Tools.setStatusBarColor(this);

        setContentView(R.layout.activity_onboarding);

        if (MainActivity.sUserProfile != null) {
            mUserProfile = MainActivity.sUserProfile;
        } else {
            mUserProfile = new UserProfile();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "in onResume()");
        checkPermissionAndSDKAndLoadFragment();
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "in onPause()");
        Log.d(TAG, "onPause: write in the DB, update the user profile, " +
                "and update the notification pref");
        // 1) Save data to the database
        DbRequestHandler.updateUserProfile(mUserProfile.getName(), mUserProfile.getEmail(),
                mUserProfile.getSex(), mUserProfile.getAge(), mUserProfile.getSegment(),
                System.currentTimeMillis());

        // 2) We update the user profile in the MainActivity
        MainActivity.sUserProfile = mUserProfile;

        // 3) If we need to save preferences set in the on-boarding we can do it here.
        // We currently have no preference to save, we just keep this comment as a reminder.
        // In SettingsPreferenceFragment, we use PreferenceManager API's
        // getDefaultSharedPreferences method with MainApplication's context to retrieve
        // the shared preference value. Hence, below we are obliged to use the same API.
        // Without this, the preference is not set as intended.

        // We force a manual data sync on whatever connection the user has in order to
        // recover his profile information.
        // Notes:
        // 1. This call runs in a separate sync process and hence does not require
        // an async task
        // 2. if isOnboardingDone is true we are done with the on-boarding and we can send the
        // data to your server
        if (isOnboardingDone) {
            SyncUtils.requestManualDataSync(false);
        }
    }

    /**
     * When the user back press on any fragment, call super.onBackPressed(), except during the
     * on boarding fragment. In this case, the back press jump to the HomeFragment.
     */
    @Override
    public void onBackPressed() {
        Log.d(TAG, "in onBackPressed()");
        if (currentFragment.getFragmentId() != ONBOARDING_DONE_FRAGMENT_ID) {
            setResult(Const.RESULT_CODE_FINISH_ES);
            super.onBackPressed();
        } else {
            onBoardingDone();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "in onRequestPermissionsResult()");

        // this method keep track of a user refusing to be asked for permissions. As we use
        // this information on subsequent permission grants, we must call this method to keep
        // track of a refusal.
        Tools.processRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Defines the behaviour of each on-boarding fragment. An object respecting this interface is passed to
     * {@link OnBoardingFragment} instances to tell them how they should construct their view hierarchy and
     * what actions to perform when they are left by the user or shown to the user.
     */
    public interface OnBoardingFragmentStrategy {
        /**
         * Defines how the fragment's view should be constructed.
         *
         * @param inflater  the inflater object to be used to inflate layout resources.
         * @param container the parent layout that will contain the fragment view.
         * @return the fragment view hierarchy.
         */
        View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container);

        /**
         * The method to call when the fragment is left
         */
        void onLeft();

        /**
         * The method to call when the fragment is shown
         */
        void onShown();

        /**
         * This method returns the fragment ID
         */
        int getFragmentId();
    }

    public interface KeyboardVisibilityListener {
        void onKeyboardVisibilityChanged(boolean keyboardVisible);
    }
}

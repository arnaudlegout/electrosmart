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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.MainActivity;


public class ErrorFragment extends MainActivityFragment {
    private static final String TAG = "ErrorFragment";
    private Context mContext;
    private TextView mTextViewTitle;
    private TextView mTextViewDescription;
    private AppCompatImageView mImageViewError;
    private AppCompatButton mButtonGeneric;
    private MainActivity mActivity;

    private final BroadcastReceiver mWindowFocusChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "mWindowFocusChangedBroadcastReceiver.onReceive: ");
            checkAndShowErrorIfExists(mContext, mActivity);
            checkErrorIsGoneAndShowHomeFragment(mContext, mActivity);
        }
    };

    /**
     * Creates a new instance of the fragment to which we pass {@code arguments} as arguments bundle.
     *
     * @param arguments the arguments bundle to pass to the newly created fragment
     * @return the created fragment
     */
    public static ErrorFragment newInstance(Bundle arguments) {
        ErrorFragment fragment = new ErrorFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    /**
     * Return an error code based on the permission enabled or the airplane mode status
     *
     * @param mContext fragment context
     * @return an error code (int)
     */
    static int checkErrors(Context mContext) {
        if (Tools.isAirplaneModeActivated(mContext)) {
            return Const.ERROR_TYPE_AIRPLANE_MODE;
        } else if (Tools.isLocationDisabled(mContext)) {
            return Const.ERROR_TYPE_NO_LOCATION_PERMISSION_DEVICE;
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Tools.isAccessFineLocationGranted(mContext)) {
            return Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP;
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                !Tools.isAccessBackgroundLocationGranted(mContext)) {
            SharedPreferences settings = MainApplication.getContext().getSharedPreferences(
                    Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
            Log.d(TAG, "checkErrors: background location is denied");
            if (settings.getBoolean(Const.SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR, true)) {
                Log.d(TAG, "checkErrors: setting SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR " +
                        "is true. So taking to ERROR_TYPE_NO_LOCATION_PERMISSION_APP_BACKGROUND");
                return Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP_BACKGROUND;
            } else {
                Log.d(TAG, "checkErrors: SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR is false. " +
                        "So, not showing this error any more");
                return Const.ERROR_TYPE_NO_ERROR;
            }
        } else {
            return Const.ERROR_TYPE_NO_ERROR;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        int errorType = getArguments().getInt(Const.MAIN_ACTIVITY_ERROR_TYPE_ARG_KEY, Const.ERROR_TYPE_AIRPLANE_MODE);

        // configure the fragment (what should be displayed) based on the error code
        return makeErrorFragment(errorType, inflater, container);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        checkErrorIsGoneAndShowHomeFragment(mContext, mActivity);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.registerReceiver(mWindowFocusChangedBroadcastReceiver,
                new IntentFilter(Const.MAIN_ACTIVITY_WINDOW_FOCUS_CHANGED_ACTION));
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        localBroadcastManager.unregisterReceiver(mWindowFocusChangedBroadcastReceiver);
    }

    private View makeErrorFragment(int errorType, @NonNull LayoutInflater inflater,
                                   @Nullable ViewGroup container) {
        View view = null;
        Bundle bundle;
        switch (errorType) {
            case Const.ERROR_TYPE_AIRPLANE_MODE:
                view = inflater.inflate(R.layout.fragment_error_airplane_mode, container,
                        false);
                break;
            case Const.ERROR_TYPE_NO_LOCATION_PERMISSION_DEVICE:
                view = inflater.inflate(R.layout.fragment_onboarding_loc_error_no_localisation, container,
                        false);
                mTextViewTitle = view.findViewById(R.id.text_view_error_title);
                mImageViewError = view.findViewById(R.id.image_location_permission);
                mButtonGeneric = view.findViewById(R.id.btn_authorize);
                mButtonGeneric.setOnClickListener(v -> {
                    Intent viewIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(viewIntent);
                });
                mButtonGeneric.setText(R.string.error_frag_btn_use_location);

                // show no location permission texts and image
                mTextViewTitle.setText(getText(R.string.error_frag_no_device_location));
                mImageViewError.setImageDrawable(
                        mContext.getResources().getDrawable(R.drawable.ic_location_off_black_24dp)
                );
                break;
            case Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP:
                view = inflater.inflate(R.layout.fragment_onboarding_loc_error_no_localisation, container,
                        false);
                mTextViewTitle = view.findViewById(R.id.text_view_error_title);
                mImageViewError = view.findViewById(R.id.image_location_permission);
                mButtonGeneric = view.findViewById(R.id.btn_authorize);

                mButtonGeneric.setOnClickListener(v ->
                        Tools.grantLocationToApp(mContext, mActivity, true));

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    mButtonGeneric.setText(getString(R.string.onboarding_btn_app_location_permission_authorize));
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    mButtonGeneric.setText(getString(R.string.onboarding_btn_app_location_permission_always_authorize));
                } else {
                    mButtonGeneric.setText(getString(R.string.onboarding_btn_app_location_permission_authorize));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Tools.setHtmlTextToTextView(mTextViewTitle,
                            getString(R.string.onboarding_error_no_app_location_api31));
                }

                mImageViewError.setImageDrawable(
                        mContext.getResources().getDrawable(R.drawable.ic_warning_black_48dp)
                );
                break;
            case Const.ERROR_TYPE_NO_LOCATION_PERMISSION_APP_BACKGROUND:
                view = inflater.inflate(R.layout.fragment_onboarding_loc_error_foreground_only, container,
                        false);
                mTextViewTitle = view.findViewById(R.id.text_view_error_title);
                mImageViewError = view.findViewById(R.id.image_location_permission);
                mButtonGeneric = view.findViewById(R.id.btn_authorize);
                AppCompatButton btnNext = view.findViewById(R.id.btn_next);
                AppCompatCheckBox checkBoxDontAskAgain = view.findViewById(R.id.checkbox_dont_ask_again);
                checkBoxDontAskAgain.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    mButtonGeneric.setText(mContext.getPackageManager().getBackgroundPermissionOptionLabel());
                }
                mButtonGeneric.setOnClickListener(v ->
                        Tools.grantLocationToApp(mContext, mActivity, false));
                btnNext.setOnClickListener(v -> {
                    SharedPreferences settings = MainApplication.getContext().getSharedPreferences(
                            Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = settings.edit();
                    // if we click continue anyway, we will not show this error fragment again
                    // until the app is restarted (that is we enter MainApplication onCreate())
                    edit.putBoolean(Const.SHOULD_SHOW_APP_BACKGROUND_LOCATION_ERROR, false).apply();
                    onResume();
                });

                checkBoxDontAskAgain.setOnCheckedChangeListener((compoundButton, b) -> {
                    Log.d(TAG, "onCheckedChanged: checkBoxDontAskAgain state = " + b);
                    SharedPreferences settings = MainApplication.getContext().getSharedPreferences(
                            Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
                    SharedPreferences.Editor edit = settings.edit();
                    edit.putBoolean(Const.DONT_SHOW_APP_BACKGROUND_LOCATION_ERROR, b).apply();
                });

                break;
        }
        return view;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach: ");
        super.onAttach(context);
        mContext = context;
        mActivity = (MainActivity) getActivity();
    }
}

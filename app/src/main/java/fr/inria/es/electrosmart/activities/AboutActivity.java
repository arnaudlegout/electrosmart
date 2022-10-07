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

import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import fr.inria.es.electrosmart.BuildConfig;
import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.serversync.SyncManager;
import fr.inria.es.electrosmart.serversync.SyncUtils;

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = "AboutActivity";

    // handle to our sync observer (that notifies us about changes in our sync state)
    private Object mSyncObserverHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.setStatusBarColor(this);
        setContentView(R.layout.activity_about);

        AppCompatButton appLogo = findViewById(R.id.app_logo);

        appLogo.setOnClickListener(v -> {
            if (SyncUtils.isSyncPossibleOnCurrentDataConnection(MainApplication.getContext())) {
                Log.d(TAG, "isSyncPossibleOnCurrentDataConnection is true. So, we do a sync.");
                SyncUtils.requestManualDataSync(false);
            } else {
                Log.d(TAG, "isSyncPossibleOnCurrentDataConnection is false. We do not manually sync.");
            }
        });

        setProfileText("");

        TextView appVersion = findViewById(R.id.app_version);

        TextView appCopyright = findViewById(R.id.app_copyright);

        Date buildDate = new Date(BuildConfig.BUILD_TIME);

        SimpleDateFormat yearSimpleDateFormat = new SimpleDateFormat("yyyy", Locale.US);
        yearSimpleDateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));

        appCopyright.setText(String.format(getString(R.string.copyright),
                yearSimpleDateFormat.format(buildDate)));

        // For release version, we show version name and version number (1.6R62), in the debug
        // version we show in addition the build date
        if (Const.IS_RELEASE_BUILD) {
            appVersion.setText(String.format(getString(R.string.version), Tools.getAppVersionName()
                    + "R" + Tools.getAppVersionNumber()));
        } else {
            SimpleDateFormat ft = new SimpleDateFormat("dd-MMM-yy HH:mm", Locale.US);
            ft.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));

            appVersion.setText(String.format(getString(R.string.version), Tools.getAppVersionName()
                    + "D" + Tools.getAppVersionNumber()
                    + String.format(" (%s)", ft.format(buildDate))));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_ABOUT_ACTIVITY_ON_RESUME);

        if (!Const.IS_RELEASE_BUILD) {
            /*
             Watch for sync state changes. We use it to notify the user when the synchronization
             starts and stops. We do this only for the debug version of the app.
            */
            SyncStatusObserver mSyncStatusObserver = which -> runOnUiThread(() -> {
                Log.d(TAG, "Synchronization status changed -- which = " + which);
                boolean syncActive = ContentResolver.isSyncActive(
                        SyncManager.getSyncAccount(MainApplication.getContext()),
                        Const.AUTHORITY);
                if (syncActive) {
                    Log.d(TAG, "Sync is running...");
                    setProfileText(getString(R.string.syncing));
                } else {
                    setProfileText("");
                    Log.d(TAG, "Sync is done.");
                }
            });
            final int mask = ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
            mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_ABOUT_ACTIVITY_ON_PAUSE);

        if (mSyncObserverHandle != null && !Const.IS_RELEASE_BUILD) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    /**
     * A function that queries the local database for the profileId of the device and sets it in the
     * profileId TextView.
     *
     * @param syncText A string value that is concatenated alongside the profileId. We use this
     *                 text to indicate if a data synchronization is in progress.
     */
    private void setProfileText(String syncText) {
        String profileId = DbRequestHandler.getProfileIdFromDB();
        TextView profile = findViewById(R.id.profile_id);
        if (profileId.isEmpty()) {
            profile.setText(String.format(getString(R.string.profile_not_created_text), syncText));
        } else {
            profile.setText(String.format(getString(R.string.profile_id), profileId, syncText));
        }
    }
}

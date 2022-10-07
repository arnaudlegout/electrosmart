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

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private SettingsPreferenceFragment settingsPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsPreferenceFragment = new SettingsPreferenceFragment();

        Tools.setStatusBarColor(this);

        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsPreferenceFragment)
                .commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "Window focus changed");
        // We call the fragment's onWindowFocusChanged only when SettingActivity got a focus, not in
        // the opposite case
        if (hasFocus) {
            settingsPreferenceFragment.onWindowFocusChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_SETTINGS_ACTIVITY_ON_RESUME);
        settingsPreferenceFragment.onWindowFocusChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_SETTINGS_ACTIVITY_ON_PAUSE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
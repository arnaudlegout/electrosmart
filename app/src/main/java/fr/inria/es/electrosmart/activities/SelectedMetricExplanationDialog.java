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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDialog;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.database.DbRequestHandler;

public class SelectedMetricExplanationDialog extends AppCompatDialog {
    private static final String TAG = "SelectedMetricDialog";

    public SelectedMetricExplanationDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_selected_metric_explanation);
        TextView dialogMetricTitle = findViewById(R.id.dialog_metric_title);
        TextView dialogMetricDescription = findViewById(R.id.dialog_metric_description);
        TextView dialogMetricTableHeaderMetric = findViewById(R.id.dialog_metric_table_header_metric);
        TextView dialogMetricTableContentMetric = findViewById(R.id.dialog_metric_table_content_metric);

        if (dialogMetricTitle != null) {
            dialogMetricTitle.setText(getTitleText());
        }
        if (dialogMetricDescription != null) {
            dialogMetricDescription.setText(getDescriptionText());
        }
        if (dialogMetricTableHeaderMetric != null) {
            dialogMetricTableHeaderMetric.setText(getTableHeaderMetricText());
        }
        if (dialogMetricTableContentMetric != null) {
            dialogMetricTableContentMetric.setText(getTableContentMetricText());
        }

        Window activityWindow = getWindow();
        if (activityWindow != null) {
            activityWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // The default layout params for the dialog's window is different across
            // different android devices. Here we force it so that it is consistent.
            activityWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        LinearLayout dialogMetricWrapper = findViewById(R.id.dialog_metric_wrapper);
        if (dialogMetricWrapper != null) {
            dialogMetricWrapper.setOnClickListener(view -> cancel());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_MEASUREMENT_FRAGMENT_ON_SELECTED_METRIC_DIALOG_START);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        DbRequestHandler.dumpEventToDatabase(Const.EVENT_MEASUREMENT_FRAGMENT_ON_SELECTED_METRIC_DIALOG_STOP);
    }

    private String getTitleText() {
        String metricString = "";
        String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
        if (pref.equals(getContext().getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
            metricString = getContext().getString(R.string.dialog_metric_title_escore);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_DBM_METRIC))) {
            metricString = getContext().getString(R.string.dialog_metric_title_dBm);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_POWER_METRIC))) {
            metricString = getContext().getString(R.string.dialog_metric_title_watt);
        }
        return metricString;
    }

    private String getDescriptionText() {
        String metricDescription = "";
        String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
        if (pref.equals(getContext().getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
            metricDescription = getContext().getString(R.string.dialog_metric_description_escore);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_DBM_METRIC))) {
            metricDescription = getContext().getString(R.string.dialog_metric_description_dBm);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_POWER_METRIC))) {
            metricDescription = getContext().getString(R.string.dialog_metric_description_power);
        }
        return metricDescription;
    }

    private String getTableHeaderMetricText() {
        String tableHeaderMetricText = "";
        String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
        if (pref.equals(getContext().getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
            tableHeaderMetricText = getContext().getString(R.string.dialog_metric_text_escore);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_DBM_METRIC))) {
            tableHeaderMetricText = getContext().getString(R.string.dialog_metric_text_dBm);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_POWER_METRIC))) {
            tableHeaderMetricText = getContext().getString(R.string.dialog_metric_text_power);
        }
        return tableHeaderMetricText;
    }

    private String getTableContentMetricText() {
        String tableContentMetricText = "";
        String pref = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
        if (pref.equals(getContext().getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
            tableContentMetricText = getContext().getString(R.string.dialog_metric_table_metric_content_escore);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_DBM_METRIC))) {
            tableContentMetricText = getContext().getString(R.string.dialog_metric_table_metric_content_dBm);
        } else if (pref.equals(getContext().getString(R.string.PREF_VALUE_POWER_METRIC))) {
            tableContentMetricText = getContext().getString(R.string.dialog_metric_table_metric_content_power);
        }
        return tableContentMetricText;
    }
}

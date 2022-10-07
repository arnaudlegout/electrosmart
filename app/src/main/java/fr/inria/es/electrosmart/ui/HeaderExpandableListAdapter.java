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

package fr.inria.es.electrosmart.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.BluetoothProperty;
import fr.inria.es.electrosmart.signalproperties.CdmaProperty;
import fr.inria.es.electrosmart.signalproperties.GsmProperty;
import fr.inria.es.electrosmart.signalproperties.LteProperty;
import fr.inria.es.electrosmart.signalproperties.NewRadioProperty;
import fr.inria.es.electrosmart.signalproperties.WcdmaProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;

/**
 * Extends an expandable list view adapter ({@link CustomExpandableListAdapter})
 * to add a header view for the group children elements.
 */
public class HeaderExpandableListAdapter extends CustomExpandableListAdapter {

    private final boolean mAdvancedMode;
    private final String mPreferenceMetric;

    /**
     * Provides a header matching {@link CustomExpandableListAdapter}
     *
     * @param context                             the context used to inflate layouts and retrieve resources
     * @param lastExpandedGroupViewAntennaDisplay is the antenna type of the currently expanded group view.
     *                                            If it is set to null, there is no expanded group view.
     * @param signalsSlot                         represent the signal slot to display in the UI
     * @param showTopSignalHighlighted            true if the signal in the exposure details needs
     *                                            to be highlighted, false otherwise
     * @param signalIndex                         the index of the BaseProperty in the list
     *                                            childViewSignals/sortedWifiGroupSignals that needs
     *                                            to be highlighted
     */
    public HeaderExpandableListAdapter(Context context,
                                       MeasurementFragment.AntennaDisplay lastExpandedGroupViewAntennaDisplay,
                                       SignalsSlot signalsSlot, boolean showTopSignalHighlighted, int signalIndex) {

        super(context, lastExpandedGroupViewAntennaDisplay, signalsSlot);
        mAdvancedMode = SettingsPreferenceFragment.get_PREF_KEY_ADVANCED_MODE();
        mPreferenceMetric = SettingsPreferenceFragment.get_PREF_KEY_EXPOSURE_METRIC();
        mShowTopSignalHighlighted = showTopSignalHighlighted;
        mSignalIndex = signalIndex;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return super.getChildrenCount(groupPosition) + 1;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        if (childPosition == 0) {
            convertView = buildHeaderChildView(parent, convertView);
            bindHeaderChildView(convertView, groupPosition);
            return convertView;
        } else {
            return super.getChildView(groupPosition, childPosition - 1, isLastChild,
                    convertView, parent);
        }
    }


    /**
     * Validates {@code convertView} for being a valid Header child view.
     * Otherwise returns a newly created one.
     *
     * @param parent      view to be the parent of the generated hierarchy
     * @param convertView the view to check for validity
     * @return convertView if valid or a newly created one
     */
    private View buildHeaderChildView(ViewGroup parent, View convertView) {
        if (convertView == null || convertView.getId() != R.id.child_layout_header) {
            convertView = layoutInflater
                    .inflate(R.layout.list_child_header, parent, false);
        }
        return convertView;
    }

    /**
     * Populates the header view with the correct text according to the group type it belongs to.
     * And sets the text colors according to the exposure level.
     *
     * @param view          the view to be bound to data.
     * @param groupPosition the position of the group the header belongs to.
     */
    private void bindHeaderChildView(View view, int groupPosition) {
        BaseProperty group = getGroup(groupPosition);

        if (group != null) {
            TextView name = view.findViewById(R.id.child_header_name);
            if (group instanceof BluetoothProperty) {
                name.setText(R.string.device);
            } else if (group instanceof GsmProperty || group instanceof WcdmaProperty
                    || group instanceof LteProperty || group instanceof CdmaProperty
                    || group instanceof NewRadioProperty) {
                if (mAdvancedMode)
                    name.setText(R.string.antenna_details);
                else
                    name.setText(R.string.operator);
            } else if (group instanceof WifiProperty) {
                name.setText(R.string.ssid);
            }

            TextView metric = view.findViewById(R.id.child_header_metric);
            if (mPreferenceMetric.equals(mContext.
                    getString(R.string.PREF_VALUE_EXPOSURE_SCORE_METRIC))) {
                metric.setText(R.string.exposure_metric);
            } else if (mPreferenceMetric.equals(mContext.
                    getString(R.string.PREF_VALUE_DBM_METRIC))) {
                metric.setText(R.string.dbm_metric);
            } else if (mPreferenceMetric.equals(mContext.
                    getString(R.string.PREF_VALUE_POWER_METRIC))) {
                metric.setText(R.string.power_metric);
            }
        }
    }
}

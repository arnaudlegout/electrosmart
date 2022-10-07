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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.R;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.activities.SettingsPreferenceFragment;
import fr.inria.es.electrosmart.fragments.MeasurementFragment;
import fr.inria.es.electrosmart.monitors.BluetoothMonitor;
import fr.inria.es.electrosmart.monitors.CellularMonitor;
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
 * Provides an adapter for the expandable list view.
 * Adapts the data given as a SignalsSlot and generates the views for the expandable list view.
 */
public class CustomExpandableListAdapter extends BaseExpandableListAdapter {
    private static final String TAG = "CustomExpandableListAdp";
    boolean mShowTopSignalHighlighted;
    // the index of the baseproperty in the childViewSignals for bluetooth/cellular and
    // sortedWifiGroupSignals list where we can find the signal to highlight
    int mSignalIndex;
    Context mContext;
    LayoutInflater layoutInflater;
    private List<BaseProperty> sortedGroupViewSignals;
    private ConcurrentHashMap<MeasurementFragment.AntennaDisplay, List<BaseProperty>> childViewSignals;
    private List<BaseProperty> wifiGroupSignals;
    private ConcurrentHashMap<BaseProperty, List<BaseProperty>> childViewWifiSignals;
    private boolean isGroupViewExpanded;


    /**
     * Constructor.
     *
     * @param context                             the context used to inflate layouts and retrieve resources
     * @param lastExpandedGroupViewAntennaDisplay contains the antenna type if a group view is expanded,
     *                                            null otherwise
     * @param signalsSlot                         Contains the SignalsSlot object to be displayed
     */
    CustomExpandableListAdapter(Context context,
                                MeasurementFragment.AntennaDisplay lastExpandedGroupViewAntennaDisplay,
                                SignalsSlot signalsSlot) {
        super();
        this.mContext = context;
        layoutInflater = LayoutInflater.from(mContext);

        // lastExpandedGroupViewAntennaDisplay is null when the group view is collapsed.
        isGroupViewExpanded = lastExpandedGroupViewAntennaDisplay != null;

        if (signalsSlot != null && signalsSlot.containsValidSignals()) {
            sortedGroupViewSignals = signalsSlot.getSortedGroupViewSignals();
            childViewSignals = signalsSlot.getChildViewSignals();
            wifiGroupSignals = signalsSlot.getSortedWifiGroupSignals();
            childViewWifiSignals = signalsSlot.getChildViewWifiSignals();

        }

        /*
        If the lastExpandedGroupViewAntennaDisplay is null, it means that the group views are collapsed.
        If it is not null, it has the value of the AntennaDisplay of the expanded group view, in that
        case, we just keep in sortedGroupViewSignals the expanded group view so that when a group
        view is expanded, it is the only one displayed.  If there is no BaseProperty for the
        extended group view, we return a BaseProperty of the same type at INVALID_SIGNAL.
         */
        List<BaseProperty> sortedCellularSignals = new ArrayList<>();
        if (lastExpandedGroupViewAntennaDisplay != null) {
            boolean hasBreaked = false;
            for (BaseProperty es : sortedGroupViewSignals) {
                if (lastExpandedGroupViewAntennaDisplay.equals(MeasurementFragment.AntennaDisplay.CELLULAR)) {
                    if (es.getAntennaDisplay() == MeasurementFragment.AntennaDisplay.CELLULAR) {
                        sortedCellularSignals.addAll(Collections.singletonList(es));
                    }
                } else {
                    if (es.getAntennaDisplay().equals(lastExpandedGroupViewAntennaDisplay)) {
                        sortedGroupViewSignals = new ArrayList<>(Collections.singletonList(es));
                        hasBreaked = true;
                        break;
                    }
                }
            }

            if (sortedCellularSignals.size() > 0) {
                Tools.arraySortDbm(sortedCellularSignals, true);
                sortedGroupViewSignals = sortedCellularSignals;
                hasBreaked = true;
            }

            // we execute this portion of code iif there is no signal for the expanded group view.
            // We create a BaseProperty with an INVALID_SIGNAL dbm value to get a hook to an
            // empty group view (displayed in grey) in order to be able to collapse it
            if (!hasBreaked) {
                BaseProperty tmp = null;
                if (MeasurementFragment.AntennaDisplay.BLUETOOTH == lastExpandedGroupViewAntennaDisplay) {
                    tmp = new BluetoothProperty(false);
                } else if (MeasurementFragment.AntennaDisplay.WIFI == lastExpandedGroupViewAntennaDisplay) {
                    tmp = new WifiProperty(false);
                } else if (MeasurementFragment.AntennaDisplay.CELLULAR == lastExpandedGroupViewAntennaDisplay) {
                    tmp = new GsmProperty(false);
                }

                sortedGroupViewSignals = new ArrayList<>(Collections.singletonList(tmp));
            }
        }
    }

    private MeasurementFragment.AntennaDisplay getAntennaDisplayWithGroupPosition(int groupPosition) {
        if (sortedGroupViewSignals != null) {
            BaseProperty es = sortedGroupViewSignals.get(groupPosition);
            return es.getAntennaDisplay();
        }
        return null;
    }

    /*
    This method return the child object in the group defined by the groupPosition at the
    childPosition in that group.  This method is mainly used by the getChildView method to retrieve
    the objects to be displayed by their groupPosition and childPosition.
     */
    @Override
    public BaseProperty getChild(int groupPosition, int childPosition) {
        // we get the antenna type of the group at groupPosition
        MeasurementFragment.AntennaDisplay antennaDisplay = getAntennaDisplayWithGroupPosition(groupPosition);

        // we get the list of child signals for the antenna type
        List<BaseProperty> childSignals = null;
        if (antennaDisplay != null) {
            if (antennaDisplay == MeasurementFragment.AntennaDisplay.WIFI) {
                childSignals = wifiGroupSignals;
            } else {
                if (childViewSignals != null) {
                    if (antennaDisplay == MeasurementFragment.AntennaDisplay.BLUETOOTH) {
                        childSignals = childViewSignals.get(antennaDisplay);
                    } else {
                        childSignals = childViewSignals.get(MeasurementFragment.AntennaDisplay.CELLULAR);
                    }
                }
            }
        }

        // we return the child at childPosition
        if (childSignals != null && childPosition < childSignals.size()) {
            return childSignals.get(childPosition);
        }
        // in case one of the previous condition fails, we have no child to return.
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        Log.d(TAG, "in getChildId: " + childPosition);
        return childPosition;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return getSourcesCount(groupPosition);
    }

    /**
     * Returns the number of sources on each group
     *
     * @param groupPosition the position of the group
     * @return the number of sources
     */
    private int getSourcesCount(int groupPosition) {
        List<BaseProperty> childSignals = null;
        MeasurementFragment.AntennaDisplay antennaDisplay = getAntennaDisplayWithGroupPosition(groupPosition);
        if (antennaDisplay != null) {
            if (antennaDisplay == MeasurementFragment.AntennaDisplay.WIFI) {
                childSignals = wifiGroupSignals;
            } else {
                if (childViewSignals != null) {
                    if (antennaDisplay == MeasurementFragment.AntennaDisplay.CELLULAR) {
                        return Tools.getNumberOfValidCellularSignals(childViewSignals);
                    } else {
                        childSignals = childViewSignals.get(antennaDisplay);
                    }
                }
            }
        }
        if (childSignals != null) {
            return childSignals.size();
        } else {
            return 0;
        }
    }

    @Override
    public BaseProperty getGroup(int groupPosition) {
        if (sortedGroupViewSignals != null) {
            return sortedGroupViewSignals.get(groupPosition);
        } else {
            return null;
        }
    }

    @Override
    public int getGroupCount() {
        if (sortedGroupViewSignals != null) {
            return sortedGroupViewSignals.size();
        } else {
            return -1;
        }
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    /**
     * Set the text in the passed TextView to "serving" and possibly the network type if available
     * in the installed version of Android.
     * <p>
     * Starting with API 30, the method to get the network type TelephonyManager.getNetworkType()
     * is deprecated, and the new method requires the dangerous permission READ_PHONE_STATE. Starting
     * with API 30, the method TelephonyManager.getNetworkType() also requires the READ_PHONE_STATE
     * permission.
     * <p>
     * Asking for the dangerous permission READ_PHONE_STATE for API >= 30 is overkill for its
     * benefit. Therefore, to drop the network type in the display for API >= 30
     *
     * @param child_serv_type The TextView that will receive the text
     * @param selectedChild   The BaseProperty that contains the information to display
     */
    private void setServingAndNetworkTypeText(TextView child_serv_type, BaseProperty selectedChild) {
        String networkType = Tools.getNetworkTypeName(selectedChild.type);
        Log.d(TAG, "setServingAndNetworkTypeText networkType:" + networkType);
        if (networkType.isEmpty()) {
            child_serv_type.setText(
                    String.format(
                            mContext.getString(R.string.show_serving_or_neighbor),
                            mContext.getResources().getString(R.string.serving)));
        } else {
            child_serv_type.setText(
                    String.format(mContext.getString(R.string.show_serve_type),
                            mContext.getResources().getString(R.string.serving),
                            Tools.getNetworkTypeName(selectedChild.type)));
        }
    }

    /**
     * This method takes in charge the inflation and population of all the child views (that is views
     * that appear when a group view is expanded)
     */
    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        // Log.d(TAG, "in getChildView()");
        boolean is_advanced_mode = SettingsPreferenceFragment.get_PREF_KEY_ADVANCED_MODE();
        BaseProperty selectedChild = getChild(groupPosition, childPosition);
        if (selectedChild != null) {
            if (selectedChild instanceof BluetoothProperty) {
                if (is_advanced_mode) {
                    // get the BT view
                    if (convertView == null || convertView.getId() != R.id.child_layout_bluetooth) {
                        convertView = layoutInflater.inflate(R.layout.list_child_bluetooth, parent, false);
                    }
                    RelativeLayout child_bt_layout = convertView.findViewById(R.id.child_layout_bluetooth);
                    TextView child_bt_name = convertView.findViewById(R.id.child_bt_name);
                    TextView child_bssid = convertView.findViewById(R.id.child_bt_bssid);
                    TextView child_bt_class = convertView.findViewById(R.id.child_bt_class);
                    TextView child_dbm = convertView.findViewById(R.id.child_bt_dbm);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the BT view
                    if (selectedChild.isValidSignal) {
                        if (selectedChild.bt_device_name == null || selectedChild.bt_device_name.isEmpty()) {
                            child_bt_name.setText(R.string.not_available);
                        } else {
                            child_bt_name.setText(selectedChild.bt_device_name);
                            if (selectedChild.bt_bond_state == BluetoothDevice.BOND_BONDED) {
                                image_view_connected.setVisibility(View.VISIBLE);
                            } else {
                                image_view_connected.setVisibility(View.INVISIBLE);
                            }
                        }
                        child_bssid.setText(selectedChild.bt_address);
                        child_bt_class.setText(String.format(mContext.getString(R.string.bt_device_class_name),
                                BluetoothMonitor.bTClassToName(selectedChild.bt_device_class)));
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_bt_layout);
                        } else {
                            child_bt_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                } else {
                    // get the BT view
                    if (convertView == null || convertView.getId() != R.id.child_layout_bluetooth_simple) {
                        convertView = layoutInflater.inflate(R.layout.list_child_bluetooth_simple, parent, false);
                    }
                    RelativeLayout child_bt_layout = convertView.findViewById(R.id.child_layout_bluetooth_simple);
                    TextView child_bt_name = convertView.findViewById(R.id.child_bt_name_simple);
                    TextView child_dbm = convertView.findViewById(R.id.child_bt_dbm_simple);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the BT view
                    if (selectedChild.isValidSignal) {
                        if (selectedChild.bt_device_name == null || selectedChild.bt_device_name.isEmpty()) {
                            child_bt_name.setText(R.string.not_available);
                        } else {
                            // I display bonded devices in bold
                            child_bt_name.setText(selectedChild.bt_device_name);
                            if (selectedChild.bt_bond_state == BluetoothDevice.BOND_BONDED) {
                                image_view_connected.setVisibility(View.VISIBLE);
                            } else {
                                image_view_connected.setVisibility(View.INVISIBLE);
                            }
                        }
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_bt_layout);
                        } else {
                            child_bt_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                }
                return convertView;

            } else if (selectedChild instanceof GsmProperty) {
                if (is_advanced_mode) {
                    // get the GSM view
                    if (convertView == null || convertView.getId() != R.id.child_layout_gsm) {
                        convertView = layoutInflater.inflate(R.layout.list_child_gsm, parent, false);
                    }
                    ConstraintLayout child_gsm_layout = convertView.findViewById(R.id.child_layout_gsm);
                    TextView child_mcc = convertView.findViewById(R.id.child_gsm_mcc);
                    TextView child_mnc = convertView.findViewById(R.id.child_gsm_mnc);
                    TextView child_operator_name = convertView.findViewById(R.id.child_gsm_operator_name);
                    TextView child_lac = convertView.findViewById(R.id.child_gsm_lac);
                    TextView child_cid = convertView.findViewById(R.id.child_gsm_cid);
                    TextView child_arfcn = convertView.findViewById(R.id.child_gsm_arfcn);
                    TextView child_bsic = convertView.findViewById(R.id.child_gsm_bsic);
                    TextView child_timing_advance = convertView.findViewById(R.id.child_gsm_timing_advance);
                    TextView child_rssi = convertView.findViewById(R.id.child_gsm_rssi);
                    TextView child_dbm = convertView.findViewById(R.id.child_gsm_dbm);
                    TextView child_ber = convertView.findViewById(R.id.child_gsm_ber);
                    TextView child_serv_type = convertView.findViewById(R.id.child_gsm_serv_type);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the GSM view
                    if (selectedChild.isValidSignal) {
                        child_mcc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mcc, Const.MIN_MCC, Const.MAX_MCC));
                        child_mnc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mnc, Const.MIN_MNC, Const.MAX_MNC));
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            setServingAndNetworkTypeText(child_serv_type, selectedChild);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                            child_serv_type.setText(
                                    String.format(mContext.getString(R.string.show_serving_or_neighbor),
                                            mContext.getResources().getString(R.string.neighboring)));
                        }

                        child_cid.setText(Tools.getStringForValueInRange(mContext, selectedChild.cid, Const.GSM_MIN_CID, Const.GSM_MAX_CID));
                        child_lac.setText(Tools.getStringForValueInRange(mContext, selectedChild.lac, Const.GSM_MIN_LAC, Const.GSM_MAX_LAC));
                        child_ber.setText(Tools.getStringForValueInRange(mContext, selectedChild.ber, Const.GSM_MIN_BER, Const.GSM_MAX_BER));

                        // only available starting with Android N. But the values are initialized to
                        // Integer.MAX_VALUE
                        child_arfcn.setText(Tools.getStringForValueInRange(mContext, selectedChild.arfcn, Const.GSM_MIN_ARFCN, Const.GSM_MAX_ARFCN));
                        child_bsic.setText(Tools.getStringForValueInRange(mContext, selectedChild.bsic, Const.GSM_MIN_BSIC, Const.GSM_MAX_BSIC));

                        // only available starting with Android O. But the value is initialized to
                        // Integer.MAX_VALUE
                        child_timing_advance.setText(
                                Tools.getStringForValueInRange(mContext, selectedChild.timing_advance, Const.GSM_MIN_TA, Const.GSM_MAX_TA));


                        child_rssi.setText(Tools.getStringForValueInRange(mContext, selectedChild.dbm, Const.GSM_MIN_DBM, Const.GSM_MAX_DBM));
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_gsm_layout);
                        } else {
                            child_gsm_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                } else {
                    if (convertView == null || convertView.getId() != R.id.child_layout_gsm_simple) {
                        convertView = layoutInflater.inflate(R.layout.list_child_gsm_simple, parent, false);
                    }
                    RelativeLayout child_gsm_layout = convertView.findViewById(R.id.child_layout_gsm_simple);
                    TextView child_operator_name = convertView.findViewById(R.id.child_gsm_operator_name_simple);
                    TextView child_dbm = convertView.findViewById(R.id.child_gsm_dbm_simple);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the GSM view
                    if (selectedChild.isValidSignal) {
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                        }
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_gsm_layout);
                        } else {
                            child_gsm_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                }
                return convertView;

            } else if (selectedChild instanceof WcdmaProperty) {
            /*
            IMPORTANT NOTE ON UCID, CID, RNC
            According to the ETSI documentation
            http://www.etsi.org/deliver/etsi_ts/123000_123099/123003/10.05.00_60/ts_123003v100500p.pdf
            ETSI TS 123 003 V10.5.0 (2012-04) section 19.4.2.7 (page 61) and to
            http://www.etsi.org/deliver/etsi_ts/125400_125499/125401/04.02.00_60/ts_125401v040200p.pdf
            ETSI TS 125 401 V4.2.0(2001-09) section 6.1.5 (page 14)
            there is no clear order in which the CID and RNC are composed,
            it might be an operator decision in its architecture (even if I am not 100% sure).

            However, according to https://en.wikipedia.org/wiki/Cell_ID the decomposition is
            UCID (28bits) = RNC(12bits) + CID (16bits)

            This is the decomposition I use even if I am not 100% it is correct for all operators.

             */
                if (is_advanced_mode) {
                    // get the WCDMA view
                    if (convertView == null || convertView.getId() != R.id.child_layout_wcdma) {
                        convertView = layoutInflater.inflate(R.layout.list_child_wcdma, parent, false);
                    }
                    ConstraintLayout child_wcdma_layout = convertView.findViewById(R.id.child_layout_wcdma);
                    TextView child_mcc = convertView.findViewById(R.id.child_wcdma_mcc);
                    TextView child_mnc = convertView.findViewById(R.id.child_wcdma_mnc);
                    TextView child_operator_name = convertView.findViewById(R.id.child_wcdma_operator_name);
                    TextView child_lac = convertView.findViewById(R.id.child_wcdma_lac);
                    TextView child_ucid = convertView.findViewById(R.id.child_wcdma_ucid);
                    TextView child_cid = convertView.findViewById(R.id.child_wcdma_cid);
                    TextView child_rnc = convertView.findViewById(R.id.child_wcdma_rnc);
                    TextView child_psc = convertView.findViewById(R.id.child_wcdma_psc);
                    TextView child_uarfcn = convertView.findViewById(R.id.child_wcdma_uarfcn);
                    TextView child_rssi = convertView.findViewById(R.id.child_wcdma_rssi);
                    //TextView child_power = convertView.findViewById(R.id.child_wcdma_power);
                    TextView child_ber = convertView.findViewById(R.id.child_wcdma_ber);
                    TextView child_ecno = convertView.findViewById(R.id.child_wcdma_ecno);
                    TextView child_serv_type = convertView.findViewById(R.id.child_wcdma_serv_type);
                    TextView child_dbm = convertView.findViewById(R.id.child_wcdma_dbm);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the WCDMA view
                    if (selectedChild.isValidSignal) {
                        child_mcc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mcc, Const.MIN_MCC, Const.MAX_MCC));
                        child_mnc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mnc, Const.MIN_MNC, Const.MAX_MNC));
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                            setServingAndNetworkTypeText(child_serv_type, selectedChild);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                            child_serv_type.setText(
                                    String.format(mContext.getString(R.string.show_serving_or_neighbor),
                                            mContext.getResources().getString(R.string.neighboring)));
                        }

                        child_ucid.setText(Tools.getStringForValueInRange(mContext, selectedChild.ucid, Const.WCDMA_MIN_UCID, Const.WCDMA_MAX_UCID));
                        if (Const.WCDMA_MIN_UCID <= selectedChild.ucid && selectedChild.ucid <= Const.WCDMA_MAX_UCID) {
                            child_cid.setText(Tools.getCidFromUcid(selectedChild.ucid));
                            child_rnc.setText(Tools.getRncFromUcid(selectedChild.ucid));
                        } else {
                            child_cid.setText(mContext.getString(R.string.na));
                            child_rnc.setText(mContext.getString(R.string.na));
                        }
                        child_lac.setText(Tools.getStringForValueInRange(mContext, selectedChild.lac, Const.WCDMA_MIN_LAC, Const.WCDMA_MAX_LAC));
                        child_ber.setText(Tools.getStringForValueInRange(mContext, selectedChild.ber, Const.WCDMA_MIN_BER, Const.WCDMA_MAX_BER));
                        child_psc.setText(Tools.getStringForValueInRange(mContext, selectedChild.psc, Const.WCDMA_MIN_PSC, Const.WCDMA_MAX_PSC));

                        // Only available starting with API 30 (Android R). Before the value is set
                        // to UNAVAILABLE
                        child_ecno.setText(Tools.getStringForValueInRange(mContext, selectedChild.ecno, Const.WCDMA_MIN_ECNO, Const.WCDMA_MAX_ECNO));

                        // only available starting with Android N. But the value is initialized to Integer.MAX_VALUE
                        child_uarfcn.setText(Tools.getStringForValueInRange(mContext, selectedChild.uarfcn, Const.WCDMA_MIN_UARFCN, Const.WCDMA_MAX_UARFCN));

                        child_rssi.setText(Tools.getStringForValueInRange(mContext, selectedChild.dbm, Const.WCDMA_MIN_DBM, Const.WCDMA_MAX_DBM));
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            //child_power.setText(Tools.dBmToWattWithSIPrefix(selectedChild.dbm, true));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                            child_dbm.setText(mContext.getString(R.string.na));
                            //child_power.setText(mContext.getString(R.string.na));
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_wcdma_layout);
                        } else {
                            child_wcdma_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                } else {
                    // get the WCDMA view
                    if (convertView == null || convertView.getId() != R.id.child_layout_wcdma_simple) {
                        convertView = layoutInflater.inflate(R.layout.list_child_wcdma_simple, parent, false);
                    }
                    RelativeLayout child_wcdma_layout = convertView.findViewById(R.id.child_layout_wcdma_simple);
                    TextView child_operator_name = convertView.findViewById(R.id.child_wcdma_operator_name_simple);
                    TextView child_dbm = convertView.findViewById(R.id.child_wcdma_dbm_simple);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);
                    // populate the WCDMA view
                    if (selectedChild.isValidSignal) {
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                        }
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                            child_dbm.setText(mContext.getString(R.string.na));
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_wcdma_layout);
                        } else {
                            child_wcdma_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                }
                return convertView;
            } else if (selectedChild instanceof LteProperty) {
            /*
            IMPORTANT NOTE ON ECI, eNB, CID

            I did not find the ETSI document specifying the ECI, but according
            to many sources we can apply the formula ECI (28bits) = eNB (20 bits) + CID (8 bits)
            */
                if (is_advanced_mode) {
                    // get the LTE view
                    if (convertView == null || convertView.getId() != R.id.child_layout_lte) {
                        convertView = layoutInflater.inflate(R.layout.list_child_lte, parent, false);
                    }
                    ConstraintLayout child_lte_layout = convertView.findViewById(R.id.child_layout_lte);
                    TextView child_mcc = convertView.findViewById(R.id.child_lte_mcc);
                    TextView child_mnc = convertView.findViewById(R.id.child_lte_mnc);
                    TextView child_operator_name = convertView.findViewById(R.id.child_lte_operator_name);
                    TextView child_bandwidth = convertView.findViewById(R.id.child_lte_bandwidth);
                    TextView child_tac = convertView.findViewById(R.id.child_lte_tac);
                    TextView child_eci = convertView.findViewById(R.id.child_lte_eci);
                    TextView child_earfcn = convertView.findViewById(R.id.child_lte_earfcn);
                    TextView child_enb = convertView.findViewById(R.id.child_lte_enb);
                    TextView child_cid = convertView.findViewById(R.id.child_lte_cid);
                    TextView child_timing_advance = convertView.findViewById(R.id.child_lte_timing_advance);
                    TextView child_pci = convertView.findViewById(R.id.child_lte_pci);
                    TextView child_rsrp = convertView.findViewById(R.id.child_lte_rsrp);
                    TextView child_rssi = convertView.findViewById(R.id.child_lte_rssi);
                    TextView child_rsrq = convertView.findViewById(R.id.child_lte_rsrq);
                    TextView child_cqi = convertView.findViewById(R.id.child_lte_cqi);
                    TextView child_rssnr = convertView.findViewById(R.id.child_lte_rssnr);
                    TextView child_serv_type = convertView.findViewById(R.id.child_lte_serv_type);
                    TextView child_dbm = convertView.findViewById(R.id.child_lte_dbm);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the LTE view
                    if (selectedChild.isValidSignal) {
                        child_mcc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mcc, Const.MIN_MCC, Const.MAX_MCC));
                        child_mnc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mnc, Const.MIN_MNC, Const.MAX_MNC));
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                            setServingAndNetworkTypeText(child_serv_type, selectedChild);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                            child_serv_type.setText(
                                    String.format(mContext.getString(R.string.show_serving_or_neighbor),
                                            mContext.getResources().getString(R.string.neighboring)));
                        }

                        child_bandwidth.setText(Tools.getStringForValueInRange(mContext, selectedChild.bandwidth, Const.LTE_MIN_BANDWIDTH, Const.LTE_MAX_BANDWIDTH));
                        child_tac.setText(Tools.getStringForValueInRange(mContext, selectedChild.tac, Const.LTE_MIN_TAC, Const.LTE_MAX_TAC));
                        child_eci.setText(Tools.getStringForValueInRange(mContext, selectedChild.eci, Const.LTE_MIN_ECI, Const.LTE_MAX_ECI));

                        if (Const.LTE_MIN_ECI <= selectedChild.eci && selectedChild.eci <= Const.LTE_MAX_ECI) {
                            child_enb.setText(Tools.getEnbFromEci(selectedChild.eci));
                            child_cid.setText(Tools.getCidFromEci(selectedChild.eci));
                        } else {
                            child_enb.setText(mContext.getString(R.string.na));
                            child_cid.setText(mContext.getString(R.string.na));
                        }

                        // only available starting with Android N. But the values are initialized to Integer.MAX_VALUE
                        child_earfcn.setText(Tools.getStringForValueInRange(mContext, selectedChild.earfcn, Const.LTE_MIN_EARFCN, Const.LTE_MAX_EARFCN));

                        child_pci.setText(Tools.getStringForValueInRange(mContext, selectedChild.pci, Const.LTE_MIN_PCI, Const.LTE_MAX_PCI));
                        child_timing_advance.setText(Tools.getStringForValueInRange(mContext, selectedChild.timing_advance, Const.LTE_MIN_TA, Const.LTE_MAX_TA));

                        child_rssi.setText(Tools.getStringForValueInRange(mContext,
                                CellularMonitor.convertLteRssiAsuToDbm(selectedChild.lte_rssi),
                                CellularMonitor.convertLteRssiAsuToDbm(Const.LTE_MIN_RSSI),
                                CellularMonitor.convertLteRssiAsuToDbm(Const.LTE_MAX_RSSI)));
                        child_rsrp.setText(Tools.getStringForValueInRange(mContext, selectedChild.lte_rsrp,
                                Const.LTE_MIN_RSRP, Const.LTE_MAX_RSRP));
                        child_rsrq.setText(Tools.getStringForValueInRange(mContext, selectedChild.lte_rsrq, Const.LTE_MIN_RSRQ, Const.LTE_MAX_RSRQ));
                        child_rssnr.setText(Tools.getStringForValueInRange(mContext, selectedChild.lte_rssnr, Const.LTE_MIN_RSSNR, Const.LTE_MAX_RSSNR));
                        child_cqi.setText(Tools.getStringForValueInRange(mContext, selectedChild.lte_cqi, Const.LTE_MIN_CQI, Const.LTE_MAX_CQI));

                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_lte_layout);
                        } else {
                            child_lte_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                } else {
                    // get the LTE view
                    if (convertView == null || convertView.getId() != R.id.child_layout_lte_simple) {
                        convertView = layoutInflater.inflate(R.layout.list_child_lte_simple, parent, false);
                    }
                    RelativeLayout child_lte_layout = convertView.findViewById(R.id.child_layout_lte_simple);
                    TextView child_operator_name = convertView.findViewById(R.id.child_lte_operator_name_simple);
                    TextView child_dbm = convertView.findViewById(R.id.child_lte_dbm_simple);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the LTE view
                    if (selectedChild.isValidSignal) {
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                        }
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_lte_layout);
                        } else {
                            child_lte_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                }
                return convertView;
            } else if (selectedChild instanceof NewRadioProperty) {
                if (is_advanced_mode) {
                    // get the LTE view
                    if (convertView == null || convertView.getId() != R.id.child_layout_new_radio) {
                        convertView = layoutInflater.inflate(R.layout.list_child_new_radio, parent, false);
                    }
                    ConstraintLayout child_new_radio_layout = convertView.findViewById(R.id.child_layout_new_radio);
                    TextView child_mcc = convertView.findViewById(R.id.child_new_radio_mcc);
                    TextView child_mnc = convertView.findViewById(R.id.child_new_radio_mnc);
                    TextView child_operator_name = convertView.findViewById(R.id.child_new_radio_operator_name);
                    TextView child_pci = convertView.findViewById(R.id.child_new_radio_pci);
                    TextView child_tac = convertView.findViewById(R.id.child_new_radio_tac);
                    TextView child_nci = convertView.findViewById(R.id.child_new_radio_nci);
                    TextView child_nrarfcn = convertView.findViewById(R.id.child_new_radio_nrarfcn);
                    TextView child_csi_rsrp = convertView.findViewById(R.id.child_new_radio_csi_rsrp);
                    TextView child_csi_rsrq = convertView.findViewById(R.id.child_new_radio_csi_rsrq);
                    TextView child_csi_sinr = convertView.findViewById(R.id.child_new_radio_csi_sinr);
                    TextView child_ss_rsrp = convertView.findViewById(R.id.child_new_radio_ss_rsrp);
                    TextView child_ss_rsrq = convertView.findViewById(R.id.child_new_radio_ss_rsrq);
                    TextView child_ss_sinr = convertView.findViewById(R.id.child_new_radio_ss_sinr);
                    TextView child_serv_type = convertView.findViewById(R.id.child_new_radio_serv_type);
                    TextView child_dbm = convertView.findViewById(R.id.child_new_radio_dbm);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the 5G view
                    if (selectedChild.isValidSignal) {
                        child_mcc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mcc, Const.MIN_MCC, Const.MAX_MCC));
                        child_mnc.setText(Tools.getStringForValueInRange(mContext, selectedChild.mnc, Const.MIN_MNC, Const.MAX_MNC));
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                            setServingAndNetworkTypeText(child_serv_type, selectedChild);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                            child_serv_type.setText(
                                    String.format(mContext.getString(R.string.show_serving_or_neighbor),
                                            mContext.getResources().getString(R.string.neighboring)));
                        }
                        child_pci.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.pci, Const.NR_MIN_PCI, Const.NR_MAX_PCI));
                        child_tac.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.tac, Const.NR_MIN_TAC, Const.NR_MAX_TAC));
                        child_nci.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.nci,
                                Const.NR_MIN_NCI, Const.NR_MAX_NCI));
                        child_nrarfcn.setText(Tools.getStringForValueInRange(
                                mContext, selectedChild.nrarfcn,
                                Const.NR_MIN_NRARFCN, Const.NR_MAX_NRARFCN));
                        child_csi_rsrp.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.csiRsrp,
                                Const.NR_MIN_CSI_RSRP, Const.NR_MAX_CSI_RSRP));
                        child_csi_rsrq.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.csiRsrq,
                                Const.NR_MIN_CSI_RSRQ, Const.NR_MAX_CSI_RSRQ));
                        child_csi_sinr.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.csiSinr,
                                Const.NR_MIN_CSI_SINR, Const.NR_MAX_CSI_SINR));
                        child_ss_rsrp.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.ssRsrp,
                                Const.NR_MIN_SS_RSRP, Const.NR_MAX_SS_RSRP));
                        child_ss_rsrq.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.ssRsrq,
                                Const.NR_MIN_SS_RSRQ, Const.NR_MAX_SS_RSRQ));
                        child_ss_sinr.setText(Tools.getStringForValueInRange(mContext,
                                selectedChild.ssSinr,
                                Const.NR_MIN_SS_SINR, Const.NR_MAX_SS_SINR));

                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_new_radio_layout);
                        } else {
                            child_new_radio_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                } else {
                    // get the NewRadio simple view
                    if (convertView == null || convertView.getId() != R.id.child_layout_new_radio_simple) {
                        convertView = layoutInflater.inflate(R.layout.list_child_new_radio_simple, parent, false);
                    }
                    RelativeLayout child_new_radio_layout = convertView.findViewById(R.id.child_layout_new_radio_simple);
                    TextView child_operator_name = convertView.findViewById(R.id.child_new_radio_operator_name_simple);
                    TextView child_dbm = convertView.findViewById(R.id.child_new_radio_dbm_simple);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the NewRadio view
                    if (selectedChild.isValidSignal) {
                        selectedChild.prepareOperatorName();
                        child_operator_name.setText(selectedChild.mOperatorName);
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                        }
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_new_radio_layout);
                        } else {
                            child_new_radio_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                }
                return convertView;
            } else if (selectedChild instanceof CdmaProperty) {
                if (is_advanced_mode) {
                    // get the CDMA view
                    if (convertView == null || convertView.getId() != R.id.child_layout_cdma) {
                        convertView = layoutInflater.inflate(R.layout.list_child_cdma, parent, false);
                    }
                    ConstraintLayout child_cdma_layout = convertView.findViewById(R.id.child_layout_cdma);
                    TextView child_nid = convertView.findViewById(R.id.child_cdma_network_id);
                    TextView child_sid = convertView.findViewById(R.id.child_cdma_system_id);
                    TextView child_bsid = convertView.findViewById(R.id.child_cdma_base_station_id);
                    TextView child_latitude = convertView.findViewById(R.id.child_cdma_latitude);
                    TextView child_longitude = convertView.findViewById(R.id.child_cdma_longitude);
                    TextView child_snr = convertView.findViewById(R.id.child_cdma_snr);
                    TextView child_rssi = convertView.findViewById(R.id.child_cdma_rssi);
                    TextView child_ecio = convertView.findViewById(R.id.child_cdma_ecio);
                    //TextView child_power = convertView.findViewById(R.id.child_cdma_power);
                    TextView child_serv_type = convertView.findViewById(R.id.child_cdma_serv_type);
                    TextView child_dbm = convertView.findViewById(R.id.child_cdma_dbm);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    // populate the CDMA view
                    if (selectedChild.isValidSignal) {
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                            setServingAndNetworkTypeText(child_serv_type, selectedChild);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                            child_serv_type.setText(String.format(mContext.getString(R.string.show_serving_or_neighbor),
                                    mContext.getResources().getString(R.string.neighboring)));
                        }

                        child_nid.setText(Tools.getStringForValueInRange(mContext, selectedChild.network_id, Const.CDMA_MIN_NID, Const.CDMA_MAX_NID));
                        child_sid.setText(Tools.getStringForValueInRange(mContext, selectedChild.system_id, Const.CDMA_MIN_SID, Const.CDMA_MAX_SID));
                        child_bsid.setText(Tools.getStringForValueInRange(mContext, selectedChild.base_station_id, Const.CDMA_MIN_BSID, Const.CDMA_MAX_BSID));
                        child_latitude.setText(Tools.getStringForValueInRange(mContext, selectedChild.cdma_latitude, Const.CDMA_MIN_LATITUDE, Const.CDMA_MAX_LATITUDE));
                        child_longitude.setText(Tools.getStringForValueInRange(mContext, selectedChild.cdma_longitude, Const.CDMA_MIN_LONGITUDE, Const.CDMA_MAX_LONGITUDE));
                        if (selectedChild.type == TelephonyManager.NETWORK_TYPE_CDMA) {
                            child_ecio.setText(Tools.getStringForValueInRange(mContext, selectedChild.cdma_ecio, Const.CDMA_MIN_ECIO, Const.CDMA_MAX_ECIO));
                            // SNR is only available for EVDO not for CDMA
                            child_snr.setText(mContext.getResources().getString(R.string.na));
                        } else {
                            // if we enter here, we have an EVDO signal
                            child_ecio.setText(Tools.getStringForValueInRange(mContext, selectedChild.evdo_ecio, Const.CDMA_MIN_ECIO, Const.CDMA_MAX_ECIO));
                            child_snr.setText(Tools.getStringForValueInRange(mContext, selectedChild.evdo_snr, Const.CDMA_MIN_SNR, Const.CDMA_MAX_SNR));
                        }
                        child_rssi.setText(Tools.getStringForValueInRange(mContext, selectedChild.dbm, Const.CDMA_MIN_DBM, Const.CDMA_MAX_DBM));

                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            //child_power.setText(Tools.dBmToWattWithSIPrefix(selectedChild.dbm, true));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            //child_power.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_cdma_layout);
                        } else {
                            child_cdma_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                } else {
                    // get the CDMA view
                    if (convertView == null || convertView.getId() != R.id.child_layout_cdma_simple) {
                        convertView = layoutInflater.inflate(R.layout.list_child_cdma_simple, parent, false);
                    }
                    RelativeLayout child_cdma_layout = convertView.findViewById(R.id.child_layout_cdma_simple);
                    TextView child_nid = convertView.findViewById(R.id.child_cdma_network_id_simple);
                    View child_exposition_dot = convertView.findViewById(R.id.exposition_dot);
                    View image_view_connected = convertView.findViewById(R.id.image_view_connected);

                    TextView child_dbm = convertView.findViewById(R.id.child_cdma_dbm_simple);

                    // populate the CDMA view
                    if (selectedChild.isValidSignal) {
                        child_nid.setText(Tools.getStringForValueInRange(mContext, selectedChild.network_id, Const.CDMA_MIN_NID, Const.CDMA_MAX_NID));
                        if (selectedChild.connected) {
                            image_view_connected.setVisibility(View.VISIBLE);
                        } else {
                            image_view_connected.setVisibility(View.INVISIBLE);
                        }
                        if (selectedChild.isDbmValueInRange()) {
                            child_dbm.setText(Tools.getExpositionInCurrentMetric(selectedChild.dbm, false));
                            Tools.setRecommendationDotBasedOnDbm(mContext, selectedChild.dbm, child_exposition_dot);
                        } else {
                            child_dbm.setText(mContext.getString(R.string.na));
                            Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                        }
                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                            highlightTopExposingChild(selectedChild.dbm, child_cdma_layout);
                        } else {
                            child_cdma_layout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                        }
                    } else {
                        convertView.setVisibility(View.GONE);
                    }
                }
                return convertView;
            } else if (selectedChild instanceof WifiProperty) {
                // get the Wifi view
                if (convertView == null || convertView.getId() != R.id.list_child_wifi) {
                    convertView = layoutInflater.inflate(R.layout.list_child_wifi, parent, false);
                }

                LinearLayout linearLayout = convertView.findViewById(R.id.list_child_wifi);

                // populate the Wifi view
                if (childViewWifiSignals != null) {
                    //    for (ConcurrentHashMap<BaseProperty, List<BaseProperty>> wp : childViewWifiSignals) {
                    for (Map.Entry<BaseProperty, List<BaseProperty>> map : childViewWifiSignals.entrySet()) {
                        List<BaseProperty> wifiGroup = map.getValue();
                        BaseProperty wifiGroupKey = map.getKey();
                        linearLayout.removeAllViews();

                        // we create the view corresponding to the selectedChild
                        if (wifiGroupKey.equals(selectedChild)) {
                            for (int i = 0; i < wifiGroup.size(); i++) {
                                if (is_advanced_mode) {
                                    View list_child_wifi_row = layoutInflater.inflate(R.layout.list_child_wifi_row, parent, false);
                                    RelativeLayout child_wifi_row = list_child_wifi_row.findViewById(R.id.child_wifi_row);
                                    TextView ssid = child_wifi_row.findViewById(R.id.child_wifi_row_ssid);
                                    TextView dbm = child_wifi_row.findViewById(R.id.child_wifi_row_dbm);
                                    TextView freq = child_wifi_row.findViewById(R.id.child_wifi_row_freq);
                                    TextView bssid = child_wifi_row.findViewById(R.id.child_wifi_row_bssid);
                                    View child_exposition_dot = child_wifi_row.findViewById(R.id.exposition_dot);
                                    View image_view_connected = child_wifi_row.findViewById(R.id.image_view_connected);

                                    if (wifiGroup.get(i).isValidSignal) {
                                        if (wifiGroup.get(i).ssid.isEmpty()) {
                                            ssid.setText(R.string.hidden_ssid);
                                        } else {
                                            ssid.setText(wifiGroup.get(i).ssid);
                                        }

                                        if (wifiGroup.get(i).isDbmValueInRange()) {
                                            dbm.setText(Tools.getExpositionInCurrentMetric(wifiGroup.get(i).dbm, false));
                                            if (i == 0) {
                                                Tools.setRecommendationDotBasedOnDbm(mContext, wifiGroup.get(i).dbm, child_exposition_dot);
                                            } else {
                                                child_exposition_dot.setVisibility(View.INVISIBLE);
                                            }

                                        } else {
                                            dbm.setText(mContext.getString(R.string.na));
                                            if (i == 0) {
                                                Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                                            } else {
                                                child_exposition_dot.setVisibility(View.INVISIBLE);
                                            }
                                        }

                                        // if there is a single signal in the wifi group or it is the
                                        // last signal of the group, we display all elements of the view
                                        if (wifiGroup.size() != 1 && i < wifiGroup.size() - 1) {
                                            freq.setVisibility(View.GONE);
                                            bssid.setVisibility(View.GONE);
                                        } else {
                                            if (wifiGroup.size() == 1) {
                                                bssid.setText(wifiGroup.get(i).bssid);
                                            } else {
                                                bssid.setText(wifiGroupKey.starredBssid);
                                            }
                                            if (wifiGroup.get(i).freq >= 2412 && wifiGroup.get(i).freq <= 2484) {
                                                freq.setText(String.format(mContext.getString(R.string.ghzch),
                                                        "2.4", Tools.convertWifiFrequencyToChannel(wifiGroup.get(i).freq),
                                                        Tools.getWifiStandardString(mContext, wifiGroup.get(i).wifiStandard)));
                                            } else {
                                                freq.setText(String.format(mContext.getString(R.string.ghzch),
                                                        "5", Tools.convertWifiFrequencyToChannel(wifiGroup.get(i).freq),
                                                        Tools.getWifiStandardString(mContext, wifiGroup.get(i).wifiStandard)));
                                            }
                                        }
                                        if (wifiGroup.get(i).connected) {
                                            image_view_connected.setVisibility(View.VISIBLE);
                                        } else {
                                            image_view_connected.setVisibility(View.INVISIBLE);
                                        }

                                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                                            highlightTopExposingChild(wifiGroup.get(0).dbm, child_wifi_row);
                                        } else {
                                            child_wifi_row.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                                        }
                                        linearLayout.addView(child_wifi_row);
                                    } else {
                                        convertView.setVisibility(View.GONE);
                                    }
                                } else {
                                    View list_child_wifi_row = layoutInflater.inflate(R.layout.list_child_wifi_row_simple, parent, false);
                                    RelativeLayout child_wifi_row = list_child_wifi_row.findViewById(R.id.child_wifi_row_simple);
                                    TextView ssid = child_wifi_row.findViewById(R.id.child_wifi_row_ssid_simple);
                                    TextView dbm = child_wifi_row.findViewById(R.id.child_wifi_row_dbm_simple);
                                    TextView freq = child_wifi_row.findViewById(R.id.child_wifi_row_freq_simple);
                                    View child_exposition_dot = child_wifi_row.findViewById(R.id.exposition_dot);
                                    View image_view_connected = child_wifi_row.findViewById(R.id.image_view_connected);

                                    if (wifiGroup.get(i).isValidSignal) {
                                        // if there is a single signal in the wifi group or it is the
                                        // last signal of the group, we display all elements of the view
                                        if (i == 0) {
                                            if (wifiGroup.get(i).isDbmValueInRange()) {
                                                dbm.setText(Tools.getExpositionInCurrentMetric(wifiGroup.get(i).dbm, false));
                                                Tools.setRecommendationDotBasedOnDbm(mContext, wifiGroup.get(i).dbm, child_exposition_dot);
                                            } else {
                                                dbm.setText(mContext.getString(R.string.na));
                                                Tools.setDotColor(mContext, child_exposition_dot, R.color.regular_background_color);
                                            }
                                        } else {
                                            child_exposition_dot.setVisibility(View.INVISIBLE);
                                            dbm.setVisibility(View.INVISIBLE);
                                        }

                                        if (wifiGroup.size() != 1 && i < wifiGroup.size() - 1) {
                                            freq.setVisibility(View.GONE);
                                        } else {
                                            if (wifiGroup.get(i).freq >= 2412 && wifiGroup.get(i).freq <= 2484) {
                                                freq.setText(String.format(mContext.getString(R.string.ghzch_simple),
                                                        "2.4", Tools.convertWifiFrequencyToChannel(wifiGroup.get(0).freq)));
                                            } else {
                                                freq.setText(String.format(mContext.getString(R.string.ghzch_simple),
                                                        "5", Tools.convertWifiFrequencyToChannel(wifiGroup.get(0).freq)));
                                            }
                                        }
                                        if (wifiGroup.get(i).ssid.isEmpty()) {
                                            ssid.setText(R.string.hidden_ssid);
                                        } else {
                                            ssid.setText(wifiGroup.get(i).ssid);
                                        }
                                        if (wifiGroup.get(i).connected) {
                                            image_view_connected.setVisibility(View.VISIBLE);
                                        } else {
                                            image_view_connected.setVisibility(View.INVISIBLE);
                                        }

                                        if (childPosition == mSignalIndex && mShowTopSignalHighlighted) {
                                            highlightTopExposingChild(wifiGroup.get(0).dbm, child_wifi_row);
                                        } else {
                                            child_wifi_row.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                                        }
                                        linearLayout.addView(child_wifi_row);
                                    } else {
                                        convertView.setVisibility(View.GONE);
                                    }
                                }
                            }
                            linearLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.regular_background_color));
                            return convertView;
                        }
                    }
                }
            }
        }
        return convertView;
    }

    /**
     * Set the top exposing source tile to 10% transparent background, if we are coming from
     * AdviceFragment or StatisticsFragment
     *
     * @param dbm    the integer valued dbm which when
     *               - greater than RECOMMENDATION_HIGH_THRESHOLD,the layout is highlighted in red
     *               - else if greater than RECOMMENDATION_LOW_THRESHOLD,the layout is highlighted
     *               in orange
     *               - else the layout is highlighted in green
     * @param layout the view that needs to be highlighted
     */
    private void highlightTopExposingChild(int dbm, View layout) {
        int backgroundColor;

        Tools.SignalLevel level = Tools.dbmToLevel(dbm);
        if (level == Tools.SignalLevel.HIGH) {
            backgroundColor = ContextCompat.getColor(mContext, R.color.recommendation_dot_red_10percent_transparent);
        } else if (level == Tools.SignalLevel.MODERATE) {
            backgroundColor = ContextCompat.getColor(mContext, R.color.recommendation_dot_orange_10percent_transparent);
        } else {
            backgroundColor = ContextCompat.getColor(mContext, R.color.recommendation_dot_green_10percent_transparent);
        }

        layout.setBackgroundColor(backgroundColor);
    }

    //the group view corresponds to the top tile and the child view correspond to the expandable child view
    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        // Log.d(TAG, "in getGroupView()");
        //Log.d(TAG, "group view called + isExpanded: " + isExpanded);
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_group, parent, false);
        }
        LinearLayout group_view_tile = convertView.findViewById(R.id.group_view_tile);
        // The icon of the signal protocol (Wi-Fi, BT, Cellular)
        AppCompatImageView protocol_icon = convertView.findViewById(R.id.protocol_image);

        TextView highest_signal_name = convertView.findViewById(R.id.highest_signal_name);
        TextView nb_sources = convertView.findViewById(R.id.nb_sources);
        AppCompatImageView image_view_connected = convertView.findViewById(R.id.image_view_connected);
        View exposition_dot = convertView.findViewById(R.id.exposition_dot);

        if (getGroup(groupPosition) != null) {
            ExpandableListView mExpandableListView = (ExpandableListView) parent;
            if (isGroupViewExpanded) {
                mExpandableListView.expandGroup(groupPosition);
            } else {
                mExpandableListView.collapseGroup(groupPosition);
            }
            /*
            When we reach this portion of code we can have three cases:
            1) a signal with valid dbm values (regular case)
            2) a signal with invalid dbm values (that is a regular signal for which the dbm value
               cannot be retrieved. When we reach this part of code, this invalid dbm value has been
               converted to the MIN_DBM - 1 for the signal type
            3) an invalid signal (BaseProperty with the isValidSignal field set to false)

            In case 1), we display the corresponding color, in case
            2) we display the dbm color corresponding to the lowest valid dbm value, in case 3)
            we display a gray tile.

            Note that to reach case 3) the only one possibility to have signals with invalid dBm
            is because they are created by the measurement fragment to represent paused events or scroll
            of the chart with an expanded tile to a position where there is no more signal for this
            expanded tile. In that case, there is a single child per GroupView. The only one goal
            of the following if clause is to show that there is no source in such a case.
             */
            int sourcesCount = getSourcesCount(groupPosition);
            if ((sourcesCount == 1 &&
                    getChild(groupPosition, 0) != null &&
                    !getChild(groupPosition, 0).isValidSignal)) {
                nb_sources.setText("");
            } else {
                // We deduct 1 to exclude the source that's already displayed in the tile
                sourcesCount -= 1;
                if (sourcesCount > 0) {
                    nb_sources.setText(
                            mContext.getResources().getQuantityString(
                                    R.plurals.home_others,
                                    sourcesCount,
                                    sourcesCount
                            )
                    );
                    nb_sources.setVisibility(View.VISIBLE);
                } else {
                    nb_sources.setVisibility(View.GONE);
                }
            }

            BaseProperty groupBaseProperty = getGroup(groupPosition);

            // We set the exposition dot of the group view according to groupBaseProperty,
            // and not as per dbm of connectedWifiSignal/servingCellularSignal
            int groupBasePropertyCumulDbm = groupBaseProperty.cumul_dbm;

            if (groupBaseProperty instanceof WifiProperty) {

                BaseProperty connectedWifiSignal = Tools.getConnectedWifiSignal(
                        childViewWifiSignals.get(groupBaseProperty));

                // if there is a connected Wi-Fi signal, we use this one for the display
                if (connectedWifiSignal != null) {
                    groupBaseProperty = connectedWifiSignal;
                }

                protocol_icon.setImageResource(R.drawable.baseline_wifi_24);
                group_view_tile.setTag(MeasurementFragment.AntennaDisplay.WIFI);
                if (groupBaseProperty.ssid == null) {
                    highest_signal_name.setText(R.string.not_available);
                } else {
                    if (groupBaseProperty.ssid.isEmpty()) {
                        highest_signal_name.setText(R.string.hidden_ssid);
                    } else {
                        highest_signal_name.setText(groupBaseProperty.ssid);
                    }
                }
                // we display the connected image if we are connected to the Wi-Fi antenna
                if (groupBaseProperty.connected) {
                    image_view_connected.setVisibility(View.VISIBLE);
                } else {
                    image_view_connected.setVisibility(View.INVISIBLE);
                }
            } else if (groupBaseProperty instanceof BluetoothProperty) {
                protocol_icon.setImageResource(R.drawable.baseline_bluetooth_24);
                group_view_tile.setTag(MeasurementFragment.AntennaDisplay.BLUETOOTH);
                if (groupBaseProperty.bt_device_name != null) {
                    highest_signal_name.setText(groupBaseProperty.bt_device_name);
                } else {
                    highest_signal_name.setText(R.string.not_available);
                }
                // we display the connected image if we are connected to the BT device
                if (groupBaseProperty.bt_bond_state == BluetoothDevice.BOND_BONDED) {
                    image_view_connected.setVisibility(View.VISIBLE);
                } else {
                    image_view_connected.setVisibility(View.INVISIBLE);
                }
            } else {
                // here we have a cellular signal
                BaseProperty servingCellularSignal = Tools.getServingCellularSignal(childViewSignals);
                // if there is a serving cell, we use this one for the display
                if (servingCellularSignal != null) {
                    groupBaseProperty = servingCellularSignal;
                }

                if (groupBaseProperty instanceof LteProperty ||
                        groupBaseProperty instanceof WcdmaProperty ||
                        groupBaseProperty instanceof GsmProperty ||
                        groupBaseProperty instanceof NewRadioProperty) {
                    protocol_icon.setImageResource(R.drawable.baseline_signal_cellular_4_bar_24);
                    group_view_tile.setTag(MeasurementFragment.AntennaDisplay.CELLULAR);
                    groupBaseProperty.prepareOperatorName();
                    highest_signal_name.setText(groupBaseProperty.mOperatorName);
                } else if (groupBaseProperty instanceof CdmaProperty) {
                    protocol_icon.setImageResource(R.drawable.baseline_signal_cellular_4_bar_24);
                    group_view_tile.setTag(MeasurementFragment.AntennaDisplay.CELLULAR);
                    // we don't set a highest signal name for CDMA as the MCC and MNC does not exist
                    // for CDMA/EVDO networks. It might me possible to link the system_id to a
                    // network operator name, but I did not find a reliable source for that.
                    //TODO: we might want to use the SIM card operator name instead, but it will not
                    // work when in roaming.
                    // We just set NID : xxxxxx so that the Operator Name highest_signal_name is
                    // not empty
                    highest_signal_name.setText(String.format(
                            mContext.getString(R.string.network_id_number),
                            Tools.getStringForValueInRange(
                                    mContext, groupBaseProperty.network_id, Const.CDMA_MIN_NID,
                                    Const.CDMA_MAX_NID))
                    );
                }

                // we display the connected image if this is the serving cell
                if (groupBaseProperty.connected) {
                    image_view_connected.setVisibility(View.VISIBLE);
                } else {
                    image_view_connected.setVisibility(View.INVISIBLE);
                }
            }

            if (groupBaseProperty.isValidSignal) {
                // set the exposition dot color based on dbm of the groupBaseProperty
                Tools.setRecommendationDotBasedOnDbm(mContext, groupBasePropertyCumulDbm, exposition_dot);
                exposition_dot.setVisibility(View.VISIBLE);
            } else {
                // hide the dot
                exposition_dot.setVisibility(View.INVISIBLE);
            }
        }

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}

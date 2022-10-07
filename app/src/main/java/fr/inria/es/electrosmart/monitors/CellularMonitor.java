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

package fr.inria.es.electrosmart.monitors;


import android.content.Context;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthNr;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;
import fr.inria.es.electrosmart.signalhandler.RawSignalHandler;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.CdmaProperty;
import fr.inria.es.electrosmart.signalproperties.GsmProperty;
import fr.inria.es.electrosmart.signalproperties.LteProperty;
import fr.inria.es.electrosmart.signalproperties.NewRadioProperty;
import fr.inria.es.electrosmart.signalproperties.WcdmaProperty;

// documentation of the various cellular technologies
//---------------------------------------------------
//
// Android knows the following 18 network types:
// NETWORK_TYPE_1xRTT, NETWORK_TYPE_CDMA (either IS95A or IS95B), NETWORK_TYPE_EDGE,
// NETWORK_TYPE_EHRPD, NETWORK_TYPE_EVDO_0, NETWORK_TYPE_EVDO_A, NETWORK_TYPE_EVDO_B,
// NETWORK_TYPE_GPRS, NETWORK_TYPE_HSDPA, NETWORK_TYPE_HSPA, NETWORK_TYPE_HSPAP (HSPA+),
// NETWORK_TYPE_HSUPA, NETWORK_TYPE_IDEN, NETWORK_TYPE_LTE, NETWORK_TYPE_UMTS, NETWORK_TYPE_UNKNOWN,
// NETWORK_TYPE_NR, NETWORK_TYPE_IWLAN
//
// and the following 2 phone types:
// PHONE_TYPE_GSM, PHONE_TYPE_CDMA
//
// Here are their relations with standards
// 2G: GSM -> PHONE_TYPE_GSM
//      cdmaOne -> PHONE_TYPE_CDMA (voice), NETWORK_TYPE_CDMA (data either IS95A or IS95B)
//      iDEN -> NETWORK_TYPE_IDEN (rare and declining use)
//
// 2.5, 2.75G: GPRS -> NETWORK_TYPE_GPRS
//             EDGE -> NETWORK_TYPE_EDGE
//             CDMA2000 1X -> NETWORK_TYPE_1xRTT
//
// 3G: CDMA2000 1xEV-DO Release 0 -> NETWORK_TYPE_EVDO_0
//      UMTS -> NETWORK_TYPE_UMTS (3GPP release'99, initial UMTS specification, see https://en.wikipedia.org/wiki/Universal_Mobile_Telecommunications_System)
//
// 3.5G: CDMA2000 1xEV-DO Revision A and Revision B -> NETWORK_TYPE_EVDO_A, NETWORK_TYPE_EVDO_B
//       HSPA -> NETWORK_TYPE_HSPA
//       HSDPA -> NETWORK_TYPE_HSDPA (3GPP release 5 see https://en.wikipedia.org/wiki/Universal_Mobile_Telecommunications_System)
//       HSUPA -> NETWORK_TYPE_HSUPA (3GPP release 6 see https://en.wikipedia.org/wiki/Universal_Mobile_Telecommunications_System)
//       HSPA+ -> NETWORK_TYPE_HSPAP (3GPP release 7 see https://en.wikipedia.org/wiki/3GPP)
//       eHRPD -> NETWORK_TYPE_EHRPD (techno to transition from EV-DO to LTE)
//
//       Note: according to https://en.wikipedia.org/wiki/High_Speed_Packet_Access, the HSPA is an
//             improvement to WCDMA by upgrading to downlink with HSDPA et and the uplink with HSUPA
//             So I don't understand why there are three different categories HSPA, HSDPA, HSUPA, as
//             HSPA gather together HSDPA and HSUPA
//
// 4G: LTE Advanced    -> NETWORK_TYPE_LTE
//     Data offloading -> NETWORK_TYPE_IWLAN (https://en.wikipedia.org/wiki/Mobile_data_offloading)
//
// 5G: 5G -> NETWORK_TYPE_NR
//
//-------------------------------------------------------------------------------------------------
//
// We have in Android 5 CellInfo:  CellInfoCdma, CellInfoGsm, CellInfoLte, CellInfoWcdma, CellInfoNr
// I assume they corresponds to the following network types. There is no clear Android documentation,
// I am using documentation in
// https://en.wikipedia.org/wiki/Universal_Mobile_Telecommunications_System
// https://en.wikipedia.org/wiki/Evolved_HSPA
//
// CellInfoGsm: NETWORK_TYPE_EDGE, NETWORK_TYPE_GPRS
// CellInfoCdma: NETWORK_TYPE_CDMA, NETWORK_TYPE_1xRTT, NETWORK_TYPE_EVDO_0, NETWORK_TYPE_EVDO_A, NETWORK_TYPE_EVDO_B, NETWORK_TYPE_EHRPD
// CellInfoWcdma: NETWORK_TYPE_UMTS, NETWORK_TYPE_HSDPA, NETWORK_TYPE_HSPA, NETWORK_TYPE_HSPAP, NETWORK_TYPE_HSUPA
// CellInfoLte: NETWORK_TYPE_LTE
// CellInfoNr: NETWORK_TYPE_NR
//
//       Note: I don't know where to classify NETWORK_TYPE_IDEN

// Wilysis that makes the networkCellInfo app documents issues on the phone models to access cell infos
// http://wilysis.com/networkcellinfo/10-android-issues

// TODO: test what is obtained from the PhoneStateListener  LISTEN_CELL_INFO  and LISTEN_CELL_LOCATION as an alternative method

public final class CellularMonitor {

    private static final String TAG = "CellularMonitor";
    /*
    value of the signal strength for the current cell populated by the signal strength
    PhoneStateListener. Note that these values are only used as a fallback method when the
    getAllCellInfo fails to return any data.

    For LTE, the value returned by the CellSignalStrengthLte might be invalid, in that case
    we fall back for the current cell to the value returned by the PhoneStateListener
    */
    private static int gsm_dbm = BaseProperty.UNAVAILABLE;
    private static int gsm_ber = BaseProperty.UNAVAILABLE;
    private static int lte_rssi = BaseProperty.UNAVAILABLE;
    private static int lte_rsrp = BaseProperty.UNAVAILABLE;
    private static int lte_rsrq = BaseProperty.UNAVAILABLE;
    private static int lte_rssnr = BaseProperty.UNAVAILABLE;
    private static int lte_cqi = BaseProperty.UNAVAILABLE;
    private static int cdma_dbm = BaseProperty.UNAVAILABLE;
    private static int cdma_ecio = BaseProperty.UNAVAILABLE;
    private static int evdo_dbm = BaseProperty.UNAVAILABLE;
    private static int evdo_ecio = BaseProperty.UNAVAILABLE;
    private static int evdo_snr = BaseProperty.UNAVAILABLE;
    private static myPhoneStateListener phoneStateListener;

    private CellularMonitor() {
    }

    /**
     * This method takes a CellSignalStrength child class and extract from this class string
     * representation the bit error rate.
     * <p/>
     * Surprisingly in API 23 CellSignalStrengthGsm does not provide any method
     * to directly access the bit error rate. However, this value is saved
     * and can be accessed by parsing the string representation of the
     * CellSignalStrengthGsm
     *
     * @param strength a CellSignalStrength child class
     * @return the bit error rate extracted from the string representation or
     * BaseProperty.UNAVAILABLE if the extraction fails
     */
    private static int getBerFromCellSignalStrength(CellSignalStrength strength) {
        String[] tokens = strength.toString().split("ber=");
        try {
            return Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            // set it to BaseProperty.UNAVAILABLE if we cannot extract it. The unknown value is 99 for the
            // CellSignalStrengthGsm class. So we can distinguish in the database where
            // this comes from.
            return BaseProperty.UNAVAILABLE;
        }
    }

    /**
     * This method returns the network type when available.
     * <p>
     * Starting with API 30, the method to get the network type TelephonyManager.getNetworkType()
     * is deprecated, and the new method requires the dangerous permission READ_PHONE_STATE. Starting
     * with API 30, the method TelephonyManager.getNetworkType() also requires the READ_PHONE_STATE
     * permission.
     * <p>
     * We use this network type in two places:
     * 1) we use it as an information stored in the DB (this is not a critical information)
     * 2) we use it for creatMonitoredCellsFromGetCellLocation() to select the correct signal type.
     * However, this method is supposed to be called only before API 17, or if the new method
     * getCellInfo() does not return valid data (which should not be the case anymore for API
     * higher than 30).
     * <p>
     * Asking for the dangerous permission READ_PHONE_STATE for API >= 30 is overkill for its
     * benefit. Therefore, I decided to just return TelephonyManager.NETWORK_TYPE_UNKNOWN for
     * API larger or equal to 30.
     *
     * @param manager A TelephonyManager instance
     * @return the network type (for API <=30) or TelephonyManager.NETWORK_TYPE_UNKNOWN otherwise
     */
    private static int getNetworkType(TelephonyManager manager) {
        int networkType;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            networkType = manager.getNetworkType();
        } else {
            networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        }
        Log.d(TAG, "getNetworkType: networkType: " + networkType);
        return networkType;
    }

    /**
     * This method creates a list of BaseProperty with a CellInfo list.
     * <p>
     * We can get a CellInfo list in two different ways
     * - From API 17 to API 28, we get it by calling getAllCellInfo()
     * <p>
     * - Starting with API 29, getAllCellInfo() does not trigger anymore update of the CellInfo data,
     * but instead access the cached data (therefore, it is still valid, but does not update the
     * data is the system did not do it before). The preferred way starting with Android Q is
     * to call requestCellInfoUpdate() that will trigger an update that will be notified through
     * a call back to a register onCellInfoChanged().
     *
     * @param cellInfoList A list of CellInfo obtained from getCellInfo or requestCellInfoUpdate.
     *                     If null or empty, the method will return an empty list.
     * @param manager      A TelephonyManager instance
     * @return A list of BaseProperty
     */
    @NonNull
    private static List<BaseProperty> createMonitoredCellsFromCellInfoList(List<CellInfo> cellInfoList,
                                                                           TelephonyManager manager) {

        List<BaseProperty> monitoredCells = new ArrayList<>();

        int currentNetworkType = getNetworkType(manager);

        LocationMonitor.updateCoordinatesWithLastKnownLocation();
        double latitude = LocationMonitor.getLatitude();
        double longitude = LocationMonitor.getLongitude();
        long now = System.currentTimeMillis();

        if (cellInfoList != null && !cellInfoList.isEmpty() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Log.d(TAG, "------------ use the getAllCellInfo method (best option) ----------------");
            for (CellInfo cell : cellInfoList) {
                Log.d(TAG, "createMonitoredCellsFromCellInfoList: cell: " + cell);
                //True when it is the current cell (may have multiple current cells in case of multiple SIM cards)
                if (cell.isRegistered()) {
                    Log.d(TAG, "this cell is CURRENT: " + cell);
                }
                if (cell instanceof CellInfoCdma) {
                    CellIdentityCdma cellIdentityCdma = ((CellInfoCdma) cell).getCellIdentity();
                    CellSignalStrengthCdma cellSignalStrengthCdma = ((CellInfoCdma) cell).getCellSignalStrength();

                    //Log.d(TAG, "## cell of type CellInfoCdma");
                    Log.d(TAG, "cell content: " + cell);
 /*                   Log.d(TAG, "cellIdentityCdma: " + cellIdentityCdma);
                    Log.d(TAG, "Base station Id: " + cellIdentityCdma.getBasestationId() + " Latitude: " + cellIdentityCdma.getLatitude() +
                            " Longitude: " + cellIdentityCdma.getLongitude() + " networkId: " + cellIdentityCdma.getNetworkId() +
                            " system ID: " + cellIdentityCdma.getSystemId());
                    Log.d(TAG, "cellSignalStrengthCdma: " + cellSignalStrengthCdma);*/
                    Log.d(TAG, " cdma Dbm: " + cellSignalStrengthCdma.getCdmaDbm() +
                            " cdma ecio: " + cellSignalStrengthCdma.getCdmaEcio() +
                            " evdo dbm " + cellSignalStrengthCdma.getEvdoDbm() +
                            " evdo ecio " + cellSignalStrengthCdma.getEvdoEcio() +
                            " evdo snr " + cellSignalStrengthCdma.getEvdoSnr());

                    // operator and operator name may be unreliable for CDMA networks
                    // see http://developer.android.com/reference/android/telephony/TelephonyManager.html#getNetworkOperator%28%29
                    CdmaProperty cdma = new CdmaProperty(
                            currentNetworkType,
                            cellIdentityCdma.getNetworkId(),
                            cellIdentityCdma.getSystemId(),
                            cellIdentityCdma.getBasestationId(),
                            cellIdentityCdma.getLatitude(),
                            cellIdentityCdma.getLongitude(),
                            cellSignalStrengthCdma.getCdmaDbm(),
                            cellSignalStrengthCdma.getCdmaEcio(),
                            cellSignalStrengthCdma.getEvdoDbm(),
                            cellSignalStrengthCdma.getEvdoEcio(),
                            cellSignalStrengthCdma.getEvdoSnr(),
                            Tools.getDbmForCdma(
                                    cellSignalStrengthCdma.getCdmaDbm(),
                                    cellSignalStrengthCdma.getEvdoDbm()),
                            cell.isRegistered(),
                            latitude,
                            longitude,
                            true,
                            now);
                    Log.d(TAG, "getCellularAntennas: " + cdma);
                    monitoredCells.add(cdma);

                } else if (cell instanceof CellInfoGsm) {
                    CellIdentityGsm cellIdentityGsm = ((CellInfoGsm) cell).getCellIdentity();
                    CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm) cell).getCellSignalStrength();

                    //Log.d(TAG, "## cell of type CellInfoGsm");
                    Log.d(TAG, "cell content: " + cell);
                    //Log.d(TAG, "cellIdentityGsm: " + cellIdentityGsm);
                    //Log.d(TAG, "cid: " + cellIdentityGsm.getCid() + " Lac: " + cellIdentityGsm.getLac() +
                    //        " Mcc: " + cellIdentityGsm.getMcc() + " Mnc: " + cellIdentityGsm.getMnc());
                    //Log.d(TAG, "cellSignalStrengthGsm: " + cellSignalStrengthGsm);
                    Log.d(TAG, " Dbm: " + cellSignalStrengthGsm.getDbm() + " Asu: " + cellSignalStrengthGsm.getAsuLevel() +
                            " Level: " + cellSignalStrengthGsm.getLevel());
                    //Log.d(TAG, "extracted BER: " + getBerFromCellSignalStrength(cellSignalStrengthGsm));

                    int arfcn = BaseProperty.UNAVAILABLE;
                    int bsic = BaseProperty.UNAVAILABLE;
                    int timing_advance = BaseProperty.UNAVAILABLE;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        arfcn = cellIdentityGsm.getArfcn();
                        bsic = cellIdentityGsm.getBsic();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        timing_advance = cellSignalStrengthGsm.getTimingAdvance();
                    }
                    int mcc, mnc;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mcc = Tools.parseMccMncString(cellIdentityGsm.getMccString());
                        mnc = Tools.parseMccMncString(cellIdentityGsm.getMncString());
                    } else {
                        mcc = cellIdentityGsm.getMcc();
                        mnc = cellIdentityGsm.getMnc();
                    }
                    int ber;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ber = cellSignalStrengthGsm.getBitErrorRate();
                    } else {
                        ber = getBerFromCellSignalStrength(cellSignalStrengthGsm);

                    }

                    /*
                    Starting with API 30 (R) Android provides a getRssi() method that returns
                    the same value as getDbm (as for API 31). However to use the more recent APIs
                    and make what we access clearer, we use the getRssi() API starting with API 30.
                     */
                    int gsm_rssi;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        gsm_rssi = cellSignalStrengthGsm.getRssi();
                    } else {
                        gsm_rssi = cellSignalStrengthGsm.getDbm();
                    }

                    GsmProperty gsm = new GsmProperty(
                            currentNetworkType,
                            mcc,
                            mnc,
                            cellIdentityGsm.getCid(),
                            cellIdentityGsm.getLac(),
                            arfcn,
                            bsic,
                            timing_advance,
                            /*
                            cellSignalStrengthGsm.getDbm() might return buggy values. To work around
                            this issue, if isValidDBMValueForCellular() returns false and the cell is the
                            current one used by the phone, we use instead the value returned by the
                            PhoneStateListener (that is supposed to be more correct, but only valid
                            for the current cell)
                             */
                            !isValidDBMValueForCellular(gsm_rssi) && cell.isRegistered() ? gsm_dbm : gsm_rssi,
                            ber,
                            cell.isRegistered(),
                            latitude,
                            longitude,
                            true,
                            now);

                    if (!isValidDBMValueForCellular(cellSignalStrengthGsm.getDbm()) && cell.isRegistered()) {
                        Log.w(TAG, "getCellularAntennas: Invalid dBm value corrected: " + gsm + " gsm_dbm=" + gsm_dbm);
                    } else {
                        Log.d(TAG, "getCellularAntennas: " + gsm + " gsm_dbm=" + gsm_dbm);
                    }

                    monitoredCells.add(gsm);

                } else if (cell instanceof CellInfoLte) {
                    CellIdentityLte cellIdentityLte = ((CellInfoLte) cell).getCellIdentity();
                    CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte) cell).getCellSignalStrength();

                    //Log.d(TAG, "## cell of type CellInfoLte");
                    Log.d(TAG, "cell content: " + cell);
/*                    Log.d(TAG, "cellIdentityLte: " + cellIdentityLte);
                    Log.d(TAG, "eci: " + cellIdentityLte.getCi() + " Tac: " + cellIdentityLte.getTac() +
                            " Mcc: " + cellIdentityLte.getMcc() + " Mnc: " + cellIdentityLte.getMnc() +
                            " Pci: " + cellIdentityLte.getPci());
                    Log.d(TAG, "cellSignalStrengthLte: " + cellSignalStrengthLte);*/
                    Log.d(TAG, " Dbm: " + cellSignalStrengthLte.getDbm() + " Asu: " + cellSignalStrengthLte.getAsuLevel() +
                            " Level: " + cellSignalStrengthLte.getLevel() + " timing advance: " +
                            cellSignalStrengthLte.getTimingAdvance());

                    /*
                    we extract values that are not publicly exposed in CellSignalStrengthLte.java,
                    but available in the string representation of CellSignalStrengthLte.java
                    we catch the possible exceptions to be robust to change in the format of the
                    string. If ever we have values at Const.PARSING_DBM_ERROR in the database,
                    it means that there are parsing errors.

                    Starting with Android O, we have direct API to access the RSRQ, RSSNR and CQI
                    values. Note that there is also an API to access the RSRP, but the returned
                    value is the same as a call to cellSignalStrengthLte.getDbm()

                    Starting with Android Q (API 29), we have direct API access to the RSSI (that
                    was the SS in previous APIs). Note that the SS is in ASU and the RSSI is in
                    dBm. To ensure consistency, we convert the RSSI to its ASU value.

                    RSSI:  Carrier  Received  Signal  Strength  Indicator (RSSI), called Signal Strength (SS) before API 29
                    RSRP: Reference  Signal  Received  Power  (RSRP). This value is the one
                    returned by cellSignalStrengthLte.getDbm()
                    RSRQ: Reference  Signal Received  Quality  (RSRQ)
                    RSSNR: Signal  to  Interference  plus  Noise  Ratio (SINR)? (not sure)
                    CQI: Channel Quality Indicator (CQI)

                    */
                    int rssi;
                    int rsrq;
                    int rssnr;
                    int cqi;
                    int bandwidth;

                    String strength = cellSignalStrengthLte.toString();
                    Log.d(TAG, "cellSignalStrengthLte.toString(): " + strength);

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        try {
                            rssi = Integer.parseInt(strength.substring(strength.indexOf("ss=") + 3, strength.indexOf("rsrp=")).trim());
                        } catch (IndexOutOfBoundsException | NumberFormatException e) {
                            rssi = Const.PARSING_DBM_ERROR;
                        }
                    } else {
                        rssi = convertLteRssiDbmToAsu(cellSignalStrengthLte.getRssi());
                    }

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        Log.d(TAG, "getCellularAntennas: (before Android O, we parse toString()): " + strength);
                        try {
                            rsrq = Integer.parseInt(strength.substring(strength.indexOf("rsrq=") + 5, strength.indexOf("rssnr=")).trim());
                        } catch (IndexOutOfBoundsException | NumberFormatException e) {
                            rsrq = Const.PARSING_DBM_ERROR;
                        }
                        try {
                            rssnr = Integer.parseInt(strength.substring(strength.indexOf("rssnr=") + 6, strength.indexOf("cqi=")).trim());
                        } catch (IndexOutOfBoundsException | NumberFormatException e) {
                            rssnr = Const.PARSING_DBM_ERROR;
                        }
                        try {
                            cqi = Integer.parseInt(strength.substring(strength.indexOf("cqi=") + 4, strength.indexOf("ta=")).trim());
                        } catch (IndexOutOfBoundsException | NumberFormatException e) {
                            cqi = Const.PARSING_DBM_ERROR;
                        }
                    } else {
                        rsrq = cellSignalStrengthLte.getRsrq();
                        rssnr = cellSignalStrengthLte.getRssnr();
                        cqi = cellSignalStrengthLte.getCqi();
                    }
                    Log.d(TAG, "rssi in ASU: " + rssi + ", rsrq: " + rsrq + ", rssnr: " + rssnr + ", cqi: " + cqi);

                    int earfcn = BaseProperty.UNAVAILABLE;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        earfcn = cellIdentityLte.getEarfcn();
                    }
                    int mcc, mnc;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mcc = Tools.parseMccMncString(cellIdentityLte.getMccString());
                        mnc = Tools.parseMccMncString(cellIdentityLte.getMncString());
                    } else {
                        mcc = cellIdentityLte.getMcc();
                        mnc = cellIdentityLte.getMnc();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bandwidth = cellIdentityLte.getBandwidth();
                    } else {
                        bandwidth = BaseProperty.UNAVAILABLE;
                    }

                    /*
                    getDbm() returns the RSRP.

                    Starting with Android API 26, there is a dedicated method that returns the
                    RSRP (so both getDbm() and getRsrp() return the same value). We decided starting
                    with API 26 to use the getRsrp() method to have a more explicit API access.

                    cellSignalStrengthLte.getDbm() might return buggy values. To work around
                    this issue, if isValidDBMValueForCellular() returns false and the cell is the
                    current one used by the phone, we use instead the value returned by the
                    PhoneStateListener (that is supposed to be more correct, but only valid
                    for the current cell)
                    */
                    int rsrp;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        rsrp = !isValidDBMValueForCellular(cellSignalStrengthLte.getDbm()) && cell.isRegistered() ? lte_rsrp : cellSignalStrengthLte.getDbm();
                    } else {
                        rsrp = !isValidDBMValueForCellular(cellSignalStrengthLte.getRsrp()) && cell.isRegistered() ? lte_rsrp : cellSignalStrengthLte.getRsrp();
                    }

                    LteProperty lte = new LteProperty(
                            currentNetworkType,
                            mcc,
                            mnc,
                            cellIdentityLte.getCi(),
                            cellIdentityLte.getPci(),
                            cellIdentityLte.getTac(),
                            earfcn,
                            bandwidth,
                            cellSignalStrengthLte.getTimingAdvance(),
                            rsrp,
                            rssi,
                            rsrq,
                            rssnr,
                            cqi,
                            cell.isRegistered(),
                            latitude,
                            longitude,
                            true,
                            now);

                    if (!isValidDBMValueForCellular(cellSignalStrengthLte.getDbm()) && cell.isRegistered()) {
                        Log.w(TAG, "getCellularAntennas: Invalid dBm value corrected: " + lte + " lte_rsrp=" + lte_rsrp);
                    } else {
                        Log.d(TAG, "getCellularAntennas: " + lte + " lte_rsrp=" + lte_rsrp);
                    }

                    monitoredCells.add(lte);

                    // CellInfoWcdma has been added in version 18 (jelly bean MR2)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                        cell instanceof CellInfoWcdma) {
                    CellIdentityWcdma cellIdentityWcdma = ((CellInfoWcdma) cell).getCellIdentity();
                    CellSignalStrengthWcdma cellSignalStrengthWcdma = ((CellInfoWcdma) cell).getCellSignalStrength();

                    //Log.d(TAG, "## cell of type CellInfoWcdma");
                    Log.d(TAG, "cell content: " + cell);
/*                    Log.d(TAG, "cellIdentityWcdma: " + cellIdentityWcdma);
                    Log.d(TAG, "cid: " + cellIdentityWcdma.getCid() + " Lac: " + cellIdentityWcdma.getLac() +
                            " Mcc: " + cellIdentityWcdma.getMcc() + " Mnc: " + cellIdentityWcdma.getMnc() +
                            " Psc: " + cellIdentityWcdma.getPsc());
                    Log.d(TAG, "cellSignalStrengthWcdma: " + cellSignalStrengthWcdma);*/
                    Log.d(TAG, " Dbm: " + cellSignalStrengthWcdma.getDbm() + " Asu: " + cellSignalStrengthWcdma.getAsuLevel() +
                            " Level: " + cellSignalStrengthWcdma.getLevel());

                    int uarfcn = BaseProperty.UNAVAILABLE;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        uarfcn = cellIdentityWcdma.getUarfcn();
                    }
                    int mcc, mnc;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        mcc = Tools.parseMccMncString(cellIdentityWcdma.getMccString());
                        mnc = Tools.parseMccMncString(cellIdentityWcdma.getMncString());
                    } else {
                        mcc = cellIdentityWcdma.getMcc();
                        mnc = cellIdentityWcdma.getMnc();
                    }

                    // code to test the string representation of cellSignalStrengthWcdma and extract
                    // hidden fields. However, on my test, before API31, none of the reported
                    // fields in the strings are valid.
//                    String strength = cellSignalStrengthWcdma.toString();
//                    int wcdma_rssi;
//                    int wcdma_ber;
//                    int wcdma_rscp;
//                    int wcdma_ecno;
//                    Log.d(TAG, "createMonitoredCellsFromCellInfoList: (before Android O, we parse cellSignalStrengthWcdma.toString()): " + cellSignalStrengthWcdma);
//                    try {
//                        wcdma_rssi = Integer.parseInt(strength.substring(strength.indexOf("ss=") + 3, strength.indexOf("ber=")).trim());
//                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
//                        wcdma_rssi = Const.PARSING_DBM_ERROR;
//                    }
//                    try {
//                        wcdma_ber = Integer.parseInt(strength.substring(strength.indexOf("ber=") + 4, strength.indexOf("rscp=")).trim());
//                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
//                        wcdma_ber = Const.PARSING_DBM_ERROR;
//                    }
//                    try {
//                        wcdma_rscp = Integer.parseInt(strength.substring(strength.indexOf("rscp=") + 5, strength.indexOf("ecno=")).trim());
//                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
//                        wcdma_rscp = Const.PARSING_DBM_ERROR;
//                    }
//                    try {
//                        wcdma_ecno = Integer.parseInt(strength.substring(strength.indexOf("ecno=") + 5, strength.indexOf("level=")).trim());
//                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
//                        wcdma_ecno = Const.PARSING_DBM_ERROR;
//                    }
//                    Log.d(TAG, "createMonitoredCellsFromCellInfoList: wcdma_rssi: " + wcdma_rssi
//                            + " wcdma_ber: " + wcdma_ber + " wcdma_rscp: " + wcdma_rscp + " wcdma_ecno: " +
//                            wcdma_ecno);

                    int wcdma_ecno = BaseProperty.UNAVAILABLE;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        wcdma_ecno = cellSignalStrengthWcdma.getEcNo();
                    }

                    WcdmaProperty wcdma = new WcdmaProperty(
                            currentNetworkType,
                            mcc,
                            mnc,
                            cellIdentityWcdma.getCid(),
                            cellIdentityWcdma.getLac(),
                            cellIdentityWcdma.getPsc(),
                            uarfcn,
                            /*
                            cellSignalStrengthGsm.getDbm() might return buggy values. To work around
                            this issue, if isValidDBMValueForCellular() returns false and the cell is the
                            current one used by the phone, we use instead the value returned by the
                            PhoneStateListener (that is supposed to be more correct, but only valid
                            for the current cell)
                             */
                            !isValidDBMValueForCellular(cellSignalStrengthWcdma.getDbm()) && cell.isRegistered() ? gsm_dbm : cellSignalStrengthWcdma.getDbm(),
                            getBerFromCellSignalStrength(cellSignalStrengthWcdma),
                            wcdma_ecno,
                            cell.isRegistered(),
                            latitude,
                            longitude,
                            true,
                            now);

                    if (!isValidDBMValueForCellular(cellSignalStrengthWcdma.getDbm()) && cell.isRegistered()) {
                        Log.w(TAG, "getCellularAntennas: Invalid dBm value corrected: " + wcdma + " lte_rsrp=" + gsm_dbm);
                    } else {
                        Log.d(TAG, "getCellularAntennas: " + wcdma + " lte_rsrp=" + gsm_dbm);
                    }

                    monitoredCells.add(wcdma);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cell instanceof CellInfoNr) {
                    CellInfoNr cellInfoNr = ((CellInfoNr) cell);
                    CellIdentityNr cellIdentityNr = (CellIdentityNr) cellInfoNr.getCellIdentity();
                    CellSignalStrengthNr cellSignalStrengthNr = (CellSignalStrengthNr) cellInfoNr.getCellSignalStrength();

                    int mcc = Tools.parseMccMncString(cellIdentityNr.getMccString());
                    int mnc = Tools.parseMccMncString(cellIdentityNr.getMncString());

                    NewRadioProperty newRadioProperty = new NewRadioProperty(
                            currentNetworkType,
                            mcc,
                            mnc,
                            cellIdentityNr.getNci(),
                            cellIdentityNr.getNrarfcn(),
                            cellIdentityNr.getPci(),
                            cellIdentityNr.getTac(),
                            cellSignalStrengthNr.getCsiRsrp(),
                            cellSignalStrengthNr.getCsiRsrq(),
                            cellSignalStrengthNr.getCsiSinr(),
                            cellSignalStrengthNr.getSsRsrp(),
                            cellSignalStrengthNr.getSsRsrq(),
                            cellSignalStrengthNr.getSsSinr(),
                            cellInfoNr.isRegistered(),
                            latitude,
                            longitude,
                            true,
                            now
                    );
                    monitoredCells.add(newRadioProperty);
                }
            }
            Log.d(TAG, "-----------------------------------------------------------");
        }
        return monitoredCells;
    }

    /**
     * This method creates a list of BaseProperty with the result to a call to getCellLocation().
     * This is a fallback method.
     * <p>
     * This method must only be used before API 17 or in case the newest and preferred
     * getCellInfo() method failed.
     *
     * @param manager A TelephonyManager instance
     * @return A list of BaseProperty
     */
    private static List<BaseProperty> createMonitoredCellsFromGetCellLocation(TelephonyManager manager) {
        List<BaseProperty> monitoredCells = new ArrayList<>();

        String currentNetworkOperator = manager.getNetworkOperator();
        int currentNetworkType = getNetworkType(manager);

        int currentMcc = BaseProperty.UNAVAILABLE;
        int currentMnc = BaseProperty.UNAVAILABLE;
        if (currentNetworkOperator.length() > 0) {
            /*
            If any of mcc or mnc cannot be parsed, we cannot rely on any of them, so we default to
            BaseProperty.UNAVAILABLE

            According to https://developer.android.com/reference/android/telephony/TelephonyManager.html#getNetworkOperator()
            getNetworkOperatorName() is only available when user is registered to a network and
            the result may be unreliable on CDMA networks.
            */
            try {
                currentMcc = Integer.parseInt(currentNetworkOperator.substring(0, 3));
                currentMnc = Integer.parseInt(currentNetworkOperator.substring(3));
            } catch (NumberFormatException e) {
                currentMcc = BaseProperty.UNAVAILABLE;
                currentMnc = BaseProperty.UNAVAILABLE;
            }
        }

        LocationMonitor.updateCoordinatesWithLastKnownLocation();
        double latitude = LocationMonitor.getLatitude();
        double longitude = LocationMonitor.getLongitude();
        long now = System.currentTimeMillis();

        Log.d(TAG, "currentNetworkOperator: " + currentNetworkOperator +
                " currentNetworkType: " + currentNetworkType +
                " currentMcc: " + currentMcc +
                " currentMnc: " + currentMnc +
                " latitude: " + latitude +
                " longitude: " + longitude +
                " now: " + now);

        CellLocation cellLocation;
        try {
            cellLocation = manager.getCellLocation();
        } catch (SecurityException e) {
            cellLocation = null;
        }
        if (cellLocation != null) {
            Log.d(TAG, "------------ use the getCellLocation method (fallback option) ----------------");
            if (cellLocation instanceof GsmCellLocation) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;

                    /*
                    According to the documentation of gsmCellLocation.getPsc if the Psc is -1, then
                    the returned cell is Gsm or unknown otherwise it is UMTS. However, according
                    to my tests, I see a psc of -1 even for UMTS is some cases.

                    A possible, solution would be to take also into account the length of the Cid
                    A valid CID ranges from 0 to 65535 (2^16-1) on GSM and CDMA networks and
                    from 0 to 268435455 (2^28-1) on UMTS.

                    However, even in that case, on a samsung device I found cases of a LTE signal
                    returned by  getCellLocation() even if the documentation of this method
                    explicitly say that it must never be the case.

                    The method I decided to use here is to check for the cellular class (2g, 3g, 4g)
                    using the currentNetworkType. Indeed, the getCellLocation() is only returning the
                    current cell, so the class of this cell will the one returned by
                    getNetworkClass(currentNetworkType)
                    */
                if (getNetworkClass(currentNetworkType) == NetworkClass.CELLULAR3G) {
                    WcdmaProperty wcdma = new WcdmaProperty(
                            currentNetworkType,
                            currentMcc,
                            currentMnc,
                            gsmCellLocation.getCid(),
                            gsmCellLocation.getLac(),
                            gsmCellLocation.getPsc(),
                            BaseProperty.UNAVAILABLE,       // not available with the old getCellLocation() API
                            gsm_dbm,
                            gsm_ber,
                            BaseProperty.UNAVAILABLE,
                            true,
                            latitude,
                            longitude,
                            true,
                            now);
                    Log.d(TAG, "getCellularAntennas: " + wcdma);
                    monitoredCells.add(wcdma);
                } else if (getNetworkClass(currentNetworkType) == NetworkClass.CELLULAR4G) {
                    LteProperty lte = new LteProperty(
                            currentNetworkType,
                            currentMcc,
                            currentMnc,
                            gsmCellLocation.getCid(), // work around to access the CI
                                /*
                                The gsmCellLocation.getPsc() contains the 4G PCI and
                                gsmCellLocation.getLac() contains the 4G TAC
                                 */
                            gsmCellLocation.getPsc(), // work around to access the PCI
                            gsmCellLocation.getLac(), // work around to access the TAC
                            BaseProperty.UNAVAILABLE,       // not available with the old getCellLocation() API
                            BaseProperty.UNAVAILABLE,
                            BaseProperty.UNAVAILABLE,
                                /*
                                on some devices, the lte_rsrp is invalid, so we fallback to the
                                gsm_dbm value that might be correct in that case.
                                */
                            isValidDBMValueForCellular(lte_rsrp) ? lte_rsrp : gsm_dbm,
                            lte_rssi,
                            lte_rsrq,
                            lte_rssnr,
                            lte_cqi,
                            true,
                            latitude,
                            longitude,
                            true,
                            now);
                    Log.d(TAG, "getCellularAntennas: " + lte);
                    monitoredCells.add(lte);
                } else {
                    GsmProperty gsm = new GsmProperty(
                            currentNetworkType,
                            currentMcc,
                            currentMnc,
                            gsmCellLocation.getCid(),
                            gsmCellLocation.getLac(),
                            BaseProperty.UNAVAILABLE,       // not available with the old getCellLocation() API
                            BaseProperty.UNAVAILABLE,       // not available with the old getCellLocation() API
                            BaseProperty.UNAVAILABLE,       // not available with the old getCellLocation() API
                            gsm_dbm,
                            gsm_ber,
                            true,
                            latitude,
                            longitude,
                            true,
                            now);
                    Log.d(TAG, "getCellularAntennas: " + gsm);
                    monitoredCells.add(gsm);
                }

                Log.d(TAG, "## cell of type CellInfoGsm: " + gsmCellLocation.toString());
                Log.d(TAG, " cid: " + ((GsmCellLocation) cellLocation).getCid() + " Lac: " +
                        ((GsmCellLocation) cellLocation).getLac() + " Psc: " +
                        ((GsmCellLocation) cellLocation).getPsc());
                int tmp = gsmCellLocation.getCid();
                // decoding of UMTS CID seems to work
                Log.d(TAG, "CID 3G: " + (tmp & 0xffff) + " RNC: " + ((tmp >> 16) & 0xffff));
                // tentative decoding of LTE CI, does not seem to work
                Log.d(TAG, "CID LTE: " + (tmp & 0xfffff) + " RNC: " + ((tmp >> 20) & 0xfffff));

            } else if (cellLocation instanceof CdmaCellLocation) {
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLocation;

                CdmaProperty cdma = new CdmaProperty(
                        currentNetworkType,
                        cdmaCellLocation.getNetworkId(),
                        cdmaCellLocation.getSystemId(),
                        cdmaCellLocation.getBaseStationId(),
                        cdmaCellLocation.getBaseStationLatitude(),
                        cdmaCellLocation.getBaseStationLongitude(),
                        cdma_dbm,
                        cdma_ecio,
                        evdo_dbm,
                        evdo_ecio,
                        evdo_snr,
                        Tools.getDbmForCdma(cdma_dbm, evdo_dbm),
                        true,
                        latitude,
                        longitude,
                        true,
                        now);
                Log.d(TAG, "getCellularAntennas: " + cdma);
                monitoredCells.add(cdma);
                Log.d(TAG, "## cell of type CellInfoCdma");

            }
            Log.d(TAG, "-----------------------------------------------------------");
        }

        // use getNeighboringCellInfo to get neighboring cells (if not returned by getAllCellInfo)

            /*
            I never implemented the support for getNeighboringCellInfo(), but the method was called
            just for logging. However, this method was replaced by getAllCellInfo since API17 and is
            deprecated since API 23. In addition, in bug #207 we found an ANR that can be due a call to
            this method. There is therefore no reason to continue to call it as it is not used and
            as it might produce an ANR.
             */
            /*
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                List<NeighboringCellInfo> neighboringCellInfos = manager.getNeighboringCellInfo();
                if (neighboringCellInfos != null) {
                    Log.d(TAG, "------------ use the getNeighboringCellInfo method (fallback option) ----------------");
                    for (NeighboringCellInfo cell : neighboringCellInfos) {
                        Log.d(TAG, "## cell of type neighboringCellInfos");
                        Log.d(TAG, "cell content: " + cell);
                    }
                    Log.d(TAG, "-----------------------------------------------------------");
                }
            }
            */

        return monitoredCells;
    }

    /**
     * Start the cellular monitor.
     * <p>
     * Starting with Android Q, we must request for a cellular scan and wait for a callback As it
     * might take time, we allow with the parameter doRequestScan to control this behavior. If
     * it is set to true, we use the regular scan request with asynchronous callback, if
     * it is set to false, we get the currently cached value, which is instantaneous.
     * <p>
     * In foreground, for the first scan, we must use doRequestScan set to false, and true otherwise.
     *
     * @param doRequestScan (only useful for API >= 29) if set to true, we request a cellular scan,
     *                      otherwise, we just get the result for the latest cached cellular scan.
     *                      Before API 29, this parameter has no effect.
     */
    public static void run(boolean doRequestScan) {
        Log.d(TAG, "running service CellularMonitor. doRequestScan: " + doRequestScan);
        List<BaseProperty> monitoredCells = null;

        // set to true if (starting with API 29), there an on-going requestCellInfoUpdate()
        boolean onGoingRequestCellInfoUpdate = false;

        // We are not supposed to receive cellular signals in airplane mode. We found in BF292
        // that some devices might return wrong signals in airplane mode,  but we cannot gracefully
        // handle this case. Therefore, we prefer to simply return when in air plane mode to be sure
        // no cellular signals are processed by the signal handler.
        if (Tools.isAirplaneModeActivated(MainApplication.getContext())) {
            if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
                MeasurementScheduler.monitorCompletedMeasurement(
                        MeasurementScheduler.MonitorType.CELLULAR_MONITOR, null
                );
            }
            return;
        }

        if (Tools.isAccessFineLocationGranted(MainApplication.getContext())) {
            final TelephonyManager manager = (TelephonyManager) (MainApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE));
            if (manager != null) {

                // debugging information only
                String currentNetworkOperatorName = manager.getNetworkOperatorName();
                int currentPhoneType = manager.getPhoneType();
                Log.d(TAG, "run: currentNetworkOperatorName: " + currentNetworkOperatorName +
                        " currentPhoneType: " + currentPhoneType);

                List<CellInfo> cellInfoList;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && doRequestScan) {
                        // Beginning Android Q, a new API method was introduced requestCellInfoUpdate()
                        // https://developer.android.com/reference/android/telephony/TelephonyManager.html#requestCellInfoUpdate(java.util.concurrent.Executor,%20android.telephony.TelephonyManager.CellInfoCallback)
                        // It is recommended to use requestCellInfoUpdate instead of getAllCellInfo

                        Log.d(TAG, "getCellularAntennas: call to requestCellInfoUpdate");
                        // We wait for the asynchronous callback to return
                        manager.requestCellInfoUpdate(MainApplication.getContext().getMainExecutor(),
                                new CellInfoCallbackListener(manager));
                        onGoingRequestCellInfoUpdate = true;
                    } else {
                        /*
                         getAllCellInfo is the newest method that should be used all the time
                         and returns the largest number of cells and neighboring cells.
                         Unfortunately, it does not work well on old devices and with some
                         brand makers.

                         This is why we should use getCellLocation that returns only the current
                         cell and getNeighboringCellInfo that is deprecated but returns
                         neighboring cells on some devices.

                         1) use first getAllCellInfo iff OS version is larger or equal
                         than 17 (jelly bean mr1) otherwise, getAllCellInfo is not available.
                         */
                        try {
                            Log.d(TAG, "getCellularAntennas: call to getAllCellInfo");
                            cellInfoList = manager.getAllCellInfo(); // lint raises a false alarm here (see https://issuetracker.google.com/issues/63962416)
                        } catch (SecurityException e) {
                            cellInfoList = null;
                        }
                        monitoredCells = createMonitoredCellsFromCellInfoList(cellInfoList, manager);
                    }
                }
                /*
                 2) use getCellLocation to get the current cell (if getAllCellInfo returns null or is empty)
                    This is a fallback solution for Samsung devices that return null for
                    getAllCellInfo and for devices with API lower than 17.
                    With getCellLocation we get at least the current cell. With this method we get the
                    signal strength from a broadcast receiver that is registered with
                    registerMyPhoneStateListener()
                */
                if (monitoredCells == null || monitoredCells.isEmpty()) {
                    // we do not call this fallback method if there is an on-going
                    // requestCellInfoUpdate. We defer its possible call to the callback in case
                    // the result is null of empty
                    if (!onGoingRequestCellInfoUpdate) {
                        monitoredCells = createMonitoredCellsFromGetCellLocation(manager);
                    }
                }
            }
        } else {
            Log.d(TAG, "in run(): Permission fine location not granted");
        }
        processMonitoredCells(monitoredCells);
    }

    /**
     * This method takes a list of BaseProperty (monitoredCells) and complete the process of
     * the monitor. If there are monitored cells, send them to the {@link RawSignalHandler}, or
     * create a white zone entry.
     *
     * @param monitoredCells a list of BaseProperty
     */
    private static void processMonitoredCells(List<BaseProperty> monitoredCells) {
        Log.d(TAG, "processMonitoredCells: ");
        if (monitoredCells != null && !monitoredCells.isEmpty()) {
            OrientationMonitor.getInstance().start();
            RawSignalHandler dataHandler = RawSignalHandler.getInstance();
            dataHandler.processRawSignals(monitoredCells, RawSignalHandler.SignalMonitorType.CELLULAR);
        } else {
            Log.d(TAG, "processMonitoredCells: monitoredCells is either empty or null");
            if (!Tools.isAirplaneModeActivated(MainApplication.getContext()) && Tools.isAccessFineLocationGranted(MainApplication.getContext())) {
                if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
                    new DbRequestHandler.addWhiteZoneToDBAsyncTask().execute(Const.WHITE_ZONE_CELLULAR);
                } else {
                    Log.d(TAG, "onSensorChanged: we are in BACKGROUND, we addWhiteZoneToDB in current thread!");
                    DbRequestHandler.addWhiteZoneToDB(Const.WHITE_ZONE_CELLULAR);
                }
            }
        }
        if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.BACKGROUND) {
            MeasurementScheduler.monitorCompletedMeasurement(MeasurementScheduler.MonitorType.CELLULAR_MONITOR, monitoredCells);
        }
    }


    /**
     * Register the myPhoneStateListener broadcast receiver to monitor if the signal strength
     * changes when using the getCellLocation() method because getAllCellInfo is not available
     * <p/>
     * The receiver is registered only once. All subsequent calls to this method will no more
     * register the receiver. The receiver will be unregistered in the stopMeasurementScheduler()
     * method of {@link MeasurementScheduler}
     */
    public static void registerMyPhoneStateListener() {
        Log.d(TAG, "in registerMyPhoneStateListener");
        if (phoneStateListener == null) {
            TelephonyManager telephonyManager = (TelephonyManager) (MainApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE));
            if (telephonyManager != null) {
                phoneStateListener = new myPhoneStateListener();
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
                Log.d(TAG, "registerMyPhoneStateListener created");
            } else {
                Log.e(TAG, "registerMyPhoneStateListener: no telephony service on that device");
            }
        } else {
            Log.d(TAG, "registerMyPhoneStateListener already created");
        }
    }

    /**
     * Unregister the myPhoneStateListener. Can be called safely even if the phoneStateListener
     * is not currently registered.
     */
    public static void unregisterMyPhoneStateListener() {
        Log.d(TAG, "unregisterMyPhoneStateListener");
        if (phoneStateListener != null) {
            TelephonyManager telephonyManager = (TelephonyManager) (MainApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE));
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
                phoneStateListener = null;
            } else {
                Log.e(TAG, "unregisterMyPhoneStateListener: no telephony service on that device");
            }
        }
    }


    /**
     * This method if a slightly modified copy/paste from the Android source code in the
     * Telephony manager, because this method in the telephonyManager is hidden, so it cannot be
     * used. If ever this method becomes public, we must use it
     *
     * @param networkType The network type to be mapped to a cellular class (2g, 3g, 4g)
     * @return the NetworkClass: CELLULAR2G, CELLULAR3G, CELLULAR4G
     */
    private static NetworkClass getNetworkClass(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NetworkClass.CELLULAR2G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NetworkClass.CELLULAR3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return NetworkClass.CELLULAR4G;
            default:
                return NetworkClass.CELLULAR2G;
        }
    }

    /**
     * Note: in API 29 where the NETWORK_TYPE_NR has been introduced for 5G,
     * the {@link TelephonyManager} changed its API, so we do not need anymore to identify the
     * current network type to find the correct BaseProperty object to instantiate. In API 29,
     * the correct SignalStrength object is directly returned.
     * <p>
     * Consequently, we do not need to add a CELLULAR5G here.
     */
    enum NetworkClass {
        CELLULAR2G,
        CELLULAR3G,
        CELLULAR4G
    }

    private static class CellInfoCallbackListener extends TelephonyManager.CellInfoCallback {
        private TelephonyManager manager;

        public CellInfoCallbackListener(TelephonyManager manager) {
            this.manager = manager;
        }

        @Override
        public void onCellInfo(@NonNull List<CellInfo> list) {
            Log.d(TAG, "in CellInfoCallbackListener.onCellInfo()");
            Log.d(TAG, "onCellInfo, CellInfo list: " + list);

            // 1) we build the monitoredCells based on the list returned by the callback
            List<BaseProperty> monitoredCells = createMonitoredCellsFromCellInfoList(list, manager);

            // 2) if monitoredCells is still empty, we fallback to getAllCellInfo() that will
            //    retrieve the cached values
            List<CellInfo> cellInfoList;
            // monitoredCells cannot be null because createMonitoredCellsFromCellInfoList() always
            // returns at least an empty list
            // Note that the SDK test is useless as this portion of the code can only be reached
            // for API 29 and higher. We add it to remove a lint warning on getAllCellInfo()
            if (monitoredCells.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                try {
                    Log.d(TAG, "getCellularAntennas: call to getAllCellInfo");
                    // lint raises a false alarm here. This portion of the code can only be
                    // reached for API 29 and higher
                    cellInfoList = manager.getAllCellInfo();
                } catch (SecurityException e) {
                    cellInfoList = null;
                }
                monitoredCells = createMonitoredCellsFromCellInfoList(cellInfoList, manager);
            }

            // 3) if monitoredCells is still empty, we fallback to the last method that call
            //    getCellLocation()
            if (monitoredCells.isEmpty()) {
                monitoredCells = createMonitoredCellsFromGetCellLocation(manager);
            }

            processMonitoredCells(monitoredCells);
        }
    }

    private static class myPhoneStateListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            Log.d(TAG, "in onSignalStrengthsChanged");

            if (signalStrength.isGsm()) {
                int gsmSignalStrength = signalStrength.getGsmSignalStrength();
                if (gsmSignalStrength != 99) {
                    gsm_dbm = (2 * gsmSignalStrength) - 113;
                } else {
                    gsm_dbm = BaseProperty.UNAVAILABLE;
                }
                gsm_ber = signalStrength.getGsmBitErrorRate();

                /*
                we extract values that are not publicly exposed in SignalStrength.java,
                but available in the string representation of SignalStrength.java
                we catch the possible exceptions to be robust to change in the format of the
                string. If ever we have values at Const.PARSING_DBM_ERROR in the database,
                it means that there are parsing errors.

                SS:  Carrier  Received  Signal  Strength  Indicator (RSSI)
                RSRP: Reference  Signal  Received  Power  (RSRP). This value is the one
                returned by cellSignalStrengthLte.getDbm()
                RSRQ: Reference  Signal Received  Quality  (RSRQ)
                RSSNR: Signal  to  Interference  plus  Noise  Ratio (SINR)? (not sure)
                CQI: Channel Quality Indicator (CQI)

                */
                String[] strength = signalStrength.toString().split(" ");
                try {
                    try {
                        lte_rssi = Integer.parseInt(strength[8]);
                    } catch (NumberFormatException e) {
                        lte_rssi = Const.PARSING_DBM_ERROR;
                    }
                    try {
                        lte_rsrp = Integer.parseInt(strength[9]);
                    } catch (NumberFormatException e) {
                        lte_rsrp = Const.PARSING_DBM_ERROR;
                    }
                    try {
                        lte_rsrq = Integer.parseInt(strength[10]);
                    } catch (NumberFormatException e) {
                        lte_rsrq = Const.PARSING_DBM_ERROR;
                    }
                    try {
                        lte_rssnr = Integer.parseInt(strength[11]);
                    } catch (NumberFormatException e) {
                        lte_rssnr = Const.PARSING_DBM_ERROR;
                    }
                    try {
                        lte_cqi = Integer.parseInt(strength[12]);
                    } catch (NumberFormatException e) {
                        lte_cqi = Const.PARSING_DBM_ERROR;
                    }

                } catch (ArrayIndexOutOfBoundsException ex) {
                    Log.e(TAG, "onSignalStrengthsChanged: ArrayIndexOutOfBoundsException" + Arrays.toString(strength));
                    Log.e(TAG, "onSignalStrengthsChanged: exception: " + ex.getMessage());
                }
                Log.d(TAG, "onSignalStrengthsChanged: gsm_dbm: " + gsm_dbm + " gsm_ber: " + gsm_ber +
                        " lte_rssi: " + lte_rssi + " lte_rsrp: " + lte_rsrp + " lte_rssnr: " + lte_rssnr +
                        " lte_cqi: " + lte_cqi);
            } else {
                cdma_dbm = signalStrength.getCdmaDbm();
                cdma_ecio = signalStrength.getCdmaEcio();
                evdo_dbm = signalStrength.getEvdoDbm();
                evdo_ecio = signalStrength.getEvdoEcio();
                evdo_snr = signalStrength.getEvdoSnr();
            }
            Log.d(TAG, "onSignalStrengthsChanged: " + signalStrength.toString());
        }
    }

    /**
     * Return true if the passed dBm value is valid, false otherwise. This method can be used for
     * GSM, WCDMA, LTE, but *NOT for CDMA*
     * <p>
     * We base this logic on a heuristic taking into account the following elements.
     * <p/>
     * in CellSignalStrengthGsm the default dbm is set to Integer.MAX_VALUE
     * in CellSignalStrengthLte the default dbm is set to Integer.MAX_VALUE
     * in CellSignalStrengthWcdma the default dbm is set to Integer.MAX_VALUE
     * in CellSignalStrengthCdma the default dbm is set to Integer.MAX_VALUE
     * <p>
     * in SignalStrength used only by fallback methods, the default dbm is set to Integer.MAX_VALUE
     * <p>
     * Therefore a valid dBm value cannot be Integer.MAX_VALUE. In addition, it does not make sense
     * to have a positive dbm value, therefore, the value must be strictly lower than 0.
     * <p/>
     *
     * @param dbm The dBm value to be tested
     * @return true if the passed dBm value is valid, false otherwise
     */
    private static boolean isValidDBMValueForCellular(int dbm) {
        return dbm < 0 && dbm != BaseProperty.UNAVAILABLE;
    }

    /**
     * Return true if the passed dBm value is valid, false otherwise. This method can only be used
     * with CDMA
     * <p/>
     * in CellSignalStrengthCdma the default dbm is set to Integer.MAX_VALUE
     * <p>
     * in SignalStrength used only by fallback methods, the default dbm is set to -1 for CDMA only
     * <p>
     * Therefore a valid dBm value cannot be Integer.MAX_VALUE. In addition, it does not make sense
     * to have a positive dbm value, and as -1 is a default value for fallback methods,
     * the value must be strictly lower than -1.
     * <p/>
     *
     * @param dbm The dBm value to be tested
     * @return true if the passed dBm value is valid, false otherwise
     */
    public static boolean isValidDBMValueForCellularCdma(int dbm) {
        return dbm < -1;
    }

    /**
     * This method converts the rssi obtained from the method {@link CellSignalStrengthLte#getRssi()}
     * that is a dBm value to an ASU value.
     * <p>
     * This method can only be called for API levels larger or equal to API 29 (Android Q) otherwise
     * it throws a RuntimeException.
     * <p>
     * It is largely inspired from CellSignalStrengthLte.convertRssiAsuToDBm() that is a
     * private method
     * <p>
     * Before API 29, the SignalStrength field (ss) was the RSSI in ASU. Starting the API 29,
     * the ss field is the RSSI in dBm. To keep consistency of this field in the database,
     * starting with API 29, we convert the RSSI to an ASU value.
     *
     * @param rssiDBm The LTE RSSI value in dBm [-113, -51]
     * @return the RSSI value converted to an ASU value [0, 31] and 99 in case it is unavailable
     */
    private static int convertLteRssiDbmToAsu(int rssiDBm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (rssiDBm == CellInfo.UNAVAILABLE) {
                // should be CellSignalStrengthLte.SIGNAL_STRENGTH_LTE_RSSI_ASU_UNKNOWN
                // but this field is private
                return 99;
            } else {
                return (rssiDBm + 113) / 2;
            }
        } else {
            throw new RuntimeException("This method has been called from an API lower than Q (API 29)");
        }

    }

    /**
     * convert an RSSI in ASU to a dBm value.
     *
     * @param rssiAsu a value in ASU
     * @return a value in dBm
     */
    public static int convertLteRssiAsuToDbm(int rssiAsu) {
        if (rssiAsu == 99) {
            return BaseProperty.UNAVAILABLE;
        }
        if ((rssiAsu < 0 || rssiAsu > 31)) {
            return BaseProperty.UNAVAILABLE;
        }
        return -113 + (2 * rssiAsu);
    }

}
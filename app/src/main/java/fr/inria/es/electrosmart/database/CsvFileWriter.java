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

package fr.inria.es.electrosmart.database;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.signalproperties.BaseProperty;

class CsvFileWriter {
    private static final String TAG = "CsvFileWriter";
    private static final String COMMA_SEP = ",";
    private static final String NEW_LINE = "\n";
    // file extension to be used for all CSV files.
    private static final String CSV_EXTENSION = ".csv";

    // this class must not be instantiated
    private CsvFileWriter() {
    }

    /**
     * Remove all files in the passed directory
     *
     * @param dirName The directory in which we need to remove files as a String
     */
    static void cleanDirectory(String dirName) {
        Log.d(TAG, "in cleanDirectory()");

        // clean up all existing files
        File folder = new File(dirName);
        File[] fileList = folder.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (!file.isDirectory()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        Log.d(TAG, "cleanDirectory: Deleted file " + file.getAbsolutePath());
                    }
                }
            }
        }
    }


    /**
     * This method creates one CSV file per signal table as given by the passed
     * allSignalTablesInJSON JSON object, and write all these CSV files to the passed
     * csvTempOutputDirPath directory.
     *
     * @param allSignalTablesInJSON A JSONObject containing all signal tables from the DB
     * @param csvTempOutputDirPath  A String giving the directory to which the CSV files must be
     *                              written
     * @throws IOException if an I/O error occurs
     */
    static void writeJsonToCsv(JSONObject allSignalTablesInJSON, String csvTempOutputDirPath) throws IOException {
        Log.d(TAG, "in writeJsonToCsv()");
        String DEFAULT_CHARSET = "UTF-8";

        // we create the csvTempOutputDirPath dir if it does not exist yet
        boolean isFolderCreated = (new File(csvTempOutputDirPath)).mkdir();
        if (isFolderCreated) {
            Log.d(TAG, "writeJsonToCsv: created folder " + csvTempOutputDirPath);
        } else {
            Log.d(TAG, "writeJsonToCsv: folder " + csvTempOutputDirPath + " already exists");
        }

        String outputFileAbsolutePath;
        OutputStreamWriter outputStreamWriter;

        // GSM
        JSONArray gsmArray = allSignalTablesInJSON.optJSONArray(DbContract.GSM.TABLE_NAME);
        if (gsmArray != null && gsmArray.length() > 0) {
            outputFileAbsolutePath = csvTempOutputDirPath + DbContract.GSM.TABLE_NAME + CSV_EXTENSION;
            outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(new File(outputFileAbsolutePath)), DEFAULT_CHARSET
            );
            Log.d(TAG, "writeJsonToCsv: Writing to file " + outputFileAbsolutePath +
                    " with " + gsmArray.length() + " lines");

            // Write the CSV file header
            outputStreamWriter.write(
                    DbContract.GSM.COLUMN_NAME_MNC + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_MCC + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_CID + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_LAC + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_ARFCN + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_BSIC + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_TIMING_ADVANCE + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_DBM + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_BER + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_CONNECTED + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_LATITUDE + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_LONGITUDE + COMMA_SEP +
                            DbContract.GSM.COLUMN_NAME_CREATED + NEW_LINE);

            for (int i = 0; i < gsmArray.length(); i++) {
                try {
                    JSONObject gsmItem = (JSONObject) gsmArray.get(i);
                    outputStreamWriter.write(
                            gsmItem.optInt(DbContract.GSM.COLUMN_NAME_MNC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_MCC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_CID, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_LAC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_ARFCN, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_BSIC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_TIMING_ADVANCE, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_DBM, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_BER, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    gsmItem.optInt(DbContract.GSM.COLUMN_NAME_CONNECTED, 0) + COMMA_SEP +
                                    gsmItem.optDouble(DbContract.GSM.COLUMN_NAME_LATITUDE, Const.INVALID_LATITUDE) + COMMA_SEP +
                                    gsmItem.optDouble(DbContract.GSM.COLUMN_NAME_LONGITUDE, Const.INVALID_LONGITUDE) + COMMA_SEP +
                                    gsmItem.optLong(DbContract.GSM.COLUMN_NAME_CREATED, Const.INVALID_TIME) + NEW_LINE);
                } catch (JSONException jsonException) {
                    Log.d(TAG, "writeJsonToCsv: JsonException " + jsonException.toString());
                }
            }
            outputStreamWriter.close();
        }

        // WCDMA
        JSONArray wcdmaArray = allSignalTablesInJSON.optJSONArray(DbContract.WCDMA.TABLE_NAME);
        if (wcdmaArray != null && wcdmaArray.length() > 0) {
            outputFileAbsolutePath = csvTempOutputDirPath + DbContract.WCDMA.TABLE_NAME + CSV_EXTENSION;
            outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(new File(outputFileAbsolutePath)), DEFAULT_CHARSET
            );
            Log.d(TAG, "writeJsonToCsv: Writing to file " + outputFileAbsolutePath +
                    " with " + wcdmaArray.length() + " lines");

            // Write the CSV file header
            outputStreamWriter.write(
                    DbContract.WCDMA.COLUMN_NAME_MNC + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_MCC + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_UCID + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_LAC + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_PSC + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_UARFCN + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_DBM + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_BER + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_CONNECTED + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_LATITUDE + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_LONGITUDE + COMMA_SEP +
                            DbContract.WCDMA.COLUMN_NAME_CREATED + NEW_LINE);

            for (int i = 0; i < wcdmaArray.length(); i++) {
                try {
                    JSONObject wcdmaItem = (JSONObject) wcdmaArray.get(i);
                    outputStreamWriter.write(
                            wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_MNC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_MCC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_UCID, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_LAC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_PSC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_UARFCN, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_DBM, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_BER, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wcdmaItem.optInt(DbContract.WCDMA.COLUMN_NAME_CONNECTED, 0) + COMMA_SEP +
                                    wcdmaItem.optDouble(DbContract.WCDMA.COLUMN_NAME_LATITUDE, Const.INVALID_LATITUDE) + COMMA_SEP +
                                    wcdmaItem.optDouble(DbContract.WCDMA.COLUMN_NAME_LONGITUDE, Const.INVALID_LONGITUDE) + COMMA_SEP +
                                    wcdmaItem.optLong(DbContract.WCDMA.COLUMN_NAME_CREATED, Const.INVALID_TIME) + NEW_LINE);
                } catch (JSONException jsonException) {
                    Log.d(TAG, "writeJsonToCsv: JsonException " + jsonException.toString());
                }
            }
            outputStreamWriter.close();
        }

        // LTE
        JSONArray lteArray = allSignalTablesInJSON.optJSONArray(DbContract.LTE.TABLE_NAME);
        if (lteArray != null && lteArray.length() > 0) {
            outputFileAbsolutePath = csvTempOutputDirPath + DbContract.LTE.TABLE_NAME + CSV_EXTENSION;
            outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(new File(outputFileAbsolutePath)), DEFAULT_CHARSET
            );
            Log.d(TAG, "writeJsonToCsv: Writing to file " + outputFileAbsolutePath +
                    " with " + lteArray.length() + " lines");

            // Write the CSV file header
            outputStreamWriter.write(
                    DbContract.LTE.COLUMN_NAME_MNC + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_MCC + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_ECI + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_PCI + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_TAC + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_EARFCN + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_BANDWIDTH + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_TIMING_ADVANCE + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_RSRP + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_RSSI + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_RSRQ + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_RSSNR + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_CQI + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_CONNECTED + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_LATITUDE + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_LONGITUDE + COMMA_SEP +
                            DbContract.LTE.COLUMN_NAME_CREATED + NEW_LINE);

            for (int i = 0; i < lteArray.length(); i++) {
                try {
                    JSONObject lteItem = (JSONObject) lteArray.get(i);
                    outputStreamWriter.write(
                            lteItem.optInt(DbContract.LTE.COLUMN_NAME_MNC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_MCC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_ECI, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_PCI, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_TAC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_EARFCN, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_BANDWIDTH, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_TIMING_ADVANCE, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_RSRP, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_RSSI, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_RSRQ, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_RSSNR, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_CQI, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    lteItem.optInt(DbContract.LTE.COLUMN_NAME_CONNECTED, 0) + COMMA_SEP +
                                    lteItem.optDouble(DbContract.LTE.COLUMN_NAME_LATITUDE, Const.INVALID_LATITUDE) + COMMA_SEP +
                                    lteItem.optDouble(DbContract.LTE.COLUMN_NAME_LONGITUDE, Const.INVALID_LONGITUDE) + COMMA_SEP +
                                    lteItem.optLong(DbContract.LTE.COLUMN_NAME_CREATED, Const.INVALID_TIME) + NEW_LINE);
                } catch (JSONException jsonException) {
                    Log.d(TAG, "writeJsonToCsv: JsonException " + jsonException.toString());
                }
            }
            outputStreamWriter.close();
        }

        // NewRadio
        JSONArray newRadioArray = allSignalTablesInJSON.optJSONArray(DbContract.NEW_RADIO.TABLE_NAME);
        if (newRadioArray != null && newRadioArray.length() > 0) {
            outputFileAbsolutePath = csvTempOutputDirPath + DbContract.NEW_RADIO.TABLE_NAME + CSV_EXTENSION;
            outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(new File(outputFileAbsolutePath)), DEFAULT_CHARSET
            );
            Log.d(TAG, "writeJsonToCsv: Writing to file " + outputFileAbsolutePath +
                    " with " + newRadioArray.length() + " lines");

            // Write the CSV file header
            outputStreamWriter.write(
                    DbContract.NEW_RADIO.COLUMN_NAME_MNC + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_MCC + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_NCI + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_PCI + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_TAC + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRQ + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_CSI_SINR + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRP + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRQ + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_SS_SINR + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_LATITUDE + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_LONGITUDE + COMMA_SEP +
                            DbContract.NEW_RADIO.COLUMN_NAME_CREATED + NEW_LINE);

            for (int i = 0; i < newRadioArray.length(); i++) {
                try {
                    JSONObject newRadioItem = (JSONObject) newRadioArray.get(i);
                    outputStreamWriter.write(
                            newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_MNC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_MCC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_NCI, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_NRARFCN, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_PCI, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_TAC, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRP, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_CSI_RSRQ, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_CSI_SINR, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRP, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_SS_RSRQ, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_SS_SINR, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    newRadioItem.optInt(DbContract.NEW_RADIO.COLUMN_NAME_CONNECTED, 0) + COMMA_SEP +
                                    newRadioItem.optDouble(DbContract.NEW_RADIO.COLUMN_NAME_LATITUDE, Const.INVALID_LATITUDE) + COMMA_SEP +
                                    newRadioItem.optDouble(DbContract.NEW_RADIO.COLUMN_NAME_LONGITUDE, Const.INVALID_LONGITUDE) + COMMA_SEP +
                                    newRadioItem.optLong(DbContract.NEW_RADIO.COLUMN_NAME_CREATED, Const.INVALID_TIME) + NEW_LINE);
                } catch (JSONException jsonException) {
                    Log.d(TAG, "writeJsonToCsv: JsonException " + jsonException.toString());
                }
            }
            outputStreamWriter.close();
        }

        // CDMA
        JSONArray cdmaArray = allSignalTablesInJSON.optJSONArray(DbContract.CDMA.TABLE_NAME);
        if (cdmaArray != null && cdmaArray.length() > 0) {
            outputFileAbsolutePath = csvTempOutputDirPath + DbContract.CDMA.TABLE_NAME + CSV_EXTENSION;
            outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(new File(outputFileAbsolutePath)), DEFAULT_CHARSET
            );
            Log.d(TAG, "writeJsonToCsv: Writing to file " + outputFileAbsolutePath +
                    " with " + cdmaArray.length() + " lines");

            // Write the CSV file header
            outputStreamWriter.write(
                    DbContract.CDMA.COLUMN_NAME_BASE_STATION_ID + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_NETWORK_ID + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_SYSTEM_ID + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_STATION_LATITUDE + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_STATION_LONGITUDE + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_CDMA_DBM + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_CDMA_ECIO + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_EVDO_DBM + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_EVDO_ECIO + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_EVDO_SNR + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_CONNECTED + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_LATITUDE + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_LONGITUDE + COMMA_SEP +
                            DbContract.CDMA.COLUMN_NAME_CREATED + NEW_LINE);

            for (int i = 0; i < cdmaArray.length(); i++) {
                try {
                    JSONObject cdmaItem = (JSONObject) cdmaArray.get(i);
                    outputStreamWriter.write(
                            cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_BASE_STATION_ID, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_NETWORK_ID, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_SYSTEM_ID, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_STATION_LATITUDE, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_STATION_LONGITUDE, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_CDMA_DBM, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_CDMA_ECIO, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_EVDO_DBM, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_EVDO_ECIO, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_EVDO_SNR, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    cdmaItem.optInt(DbContract.CDMA.COLUMN_NAME_CONNECTED, 0) + COMMA_SEP +
                                    cdmaItem.optDouble(DbContract.CDMA.COLUMN_NAME_LATITUDE, Const.INVALID_LATITUDE) + COMMA_SEP +
                                    cdmaItem.optDouble(DbContract.CDMA.COLUMN_NAME_LONGITUDE, Const.INVALID_LONGITUDE) + COMMA_SEP +
                                    cdmaItem.optLong(DbContract.CDMA.COLUMN_NAME_CREATED, Const.INVALID_TIME) + NEW_LINE);
                } catch (JSONException jsonException) {
                    Log.d(TAG, "writeJsonToCsv: JsonException " + jsonException.toString());
                }
            }
            outputStreamWriter.close();
        }

        // WIFI
        JSONArray wifiArray = allSignalTablesInJSON.optJSONArray(DbContract.WIFI.TABLE_NAME);
        if (wifiArray != null && wifiArray.length() > 0) {
            outputFileAbsolutePath = csvTempOutputDirPath + DbContract.WIFI.TABLE_NAME + CSV_EXTENSION;
            outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(new File(outputFileAbsolutePath)), DEFAULT_CHARSET
            );
            Log.d(TAG, "writeJsonToCsv: Writing to file " + outputFileAbsolutePath +
                    " with " + wifiArray.length() + " lines");

            // Write the CSV file header
            outputStreamWriter.write(
                    DbContract.WIFI.COLUMN_NAME_SSID + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_BSSID + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_OPERATOR_FRIENDLY_NAME + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_VENUE_NAME + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_IS_PASSPOINT_NETWORK + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_FREQ + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_CENTERFREQ0 + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_CENTERFREQ1 + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_CHANNELWIDTH + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_CAPABILITIES + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_DBM + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_CONNECTED + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_LATITUDE + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_LONGITUDE + COMMA_SEP +
                            DbContract.WIFI.COLUMN_NAME_CREATED + NEW_LINE);

            for (int i = 0; i < wifiArray.length(); i++) {
                try {
                    JSONObject wifiItem = (JSONObject) wifiArray.get(i);
                    outputStreamWriter.write(
                            wifiItem.optString(DbContract.WIFI.COLUMN_NAME_SSID, "") + COMMA_SEP +
                                    // if the BSSID as a long is 0, its corresponding MAC address is 00:00:00:00:00:00
                                    Tools.Long2MAC(wifiItem.optLong(DbContract.WIFI.COLUMN_NAME_BSSID, 0)) + COMMA_SEP +
                                    wifiItem.optString(DbContract.WIFI.COLUMN_NAME_OPERATOR_FRIENDLY_NAME, "") + COMMA_SEP +
                                    wifiItem.optString(DbContract.WIFI.COLUMN_NAME_VENUE_NAME, "") + COMMA_SEP +
                                    wifiItem.optInt(DbContract.WIFI.COLUMN_NAME_IS_PASSPOINT_NETWORK, Const.INVALID_IS_PASSPOINT_NETWORK) + COMMA_SEP +
                                    wifiItem.optInt(DbContract.WIFI.COLUMN_NAME_FREQ, Integer.MAX_VALUE) + COMMA_SEP +
                                    wifiItem.optInt(DbContract.WIFI.COLUMN_NAME_CENTERFREQ0, Const.INVALID_CENTERFREQ0) + COMMA_SEP +
                                    wifiItem.optInt(DbContract.WIFI.COLUMN_NAME_CENTERFREQ1, Const.INVALID_CENTERFREQ1) + COMMA_SEP +
                                    wifiItem.optInt(DbContract.WIFI.COLUMN_NAME_CHANNELWIDTH, Const.INVALID_CHANNELWIDTH) + COMMA_SEP +
                                    wifiItem.optString(DbContract.WIFI.COLUMN_NAME_CAPABILITIES, "") + COMMA_SEP +
                                    wifiItem.optInt(DbContract.WIFI.COLUMN_NAME_DBM, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    wifiItem.optInt(DbContract.WIFI.COLUMN_NAME_CONNECTED, 0) + COMMA_SEP +
                                    wifiItem.optDouble(DbContract.WIFI.COLUMN_NAME_LATITUDE, Const.INVALID_LATITUDE) + COMMA_SEP +
                                    wifiItem.optDouble(DbContract.WIFI.COLUMN_NAME_LONGITUDE, Const.INVALID_LONGITUDE) + COMMA_SEP +
                                    wifiItem.optLong(DbContract.WIFI.COLUMN_NAME_CREATED, Const.INVALID_TIME) + NEW_LINE);
                } catch (JSONException jsonException) {
                    Log.d(TAG, "writeJsonToCsv: JsonException " + jsonException.toString());
                }
            }
            outputStreamWriter.close();
        }

        // Bluetooth
        JSONArray bluetoothArray = allSignalTablesInJSON.optJSONArray(DbContract.BLUETOOTH.TABLE_NAME);
        if (bluetoothArray != null && bluetoothArray.length() > 0) {
            outputFileAbsolutePath = csvTempOutputDirPath + DbContract.BLUETOOTH.TABLE_NAME + CSV_EXTENSION;
            outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(new File(outputFileAbsolutePath)), DEFAULT_CHARSET
            );
            Log.d(TAG, "writeJsonToCsv: Writing to file " + outputFileAbsolutePath +
                    " with " + bluetoothArray.length() + " lines");

            // Write the CSV file header
            outputStreamWriter.write(DbContract.BLUETOOTH.COLUMN_NAME_NAME + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_DBM + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_LATITUDE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_LONGITUDE + COMMA_SEP +
                    DbContract.BLUETOOTH.COLUMN_NAME_CREATED + NEW_LINE);

            for (int i = 0; i < bluetoothArray.length(); i++) {
                try {
                    JSONObject bluetoothItem = (JSONObject) bluetoothArray.get(i);
                    outputStreamWriter.write(
                            bluetoothItem.optString(DbContract.BLUETOOTH.COLUMN_NAME_NAME, "") + COMMA_SEP +
                                    bluetoothItem.optString(DbContract.BLUETOOTH.COLUMN_NAME_NAME_ALIAS, "") + COMMA_SEP +
                                    bluetoothItem.optInt(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_CLASS, Integer.MAX_VALUE) + COMMA_SEP +
                                    bluetoothItem.optInt(DbContract.BLUETOOTH.COLUMN_NAME_DEVICE_TYPE, Integer.MAX_VALUE) + COMMA_SEP +
                                    // if the BSSID as a long is 0, its corresponding MAC address is 00:00:00:00:00:00
                                    Tools.Long2MAC(bluetoothItem.optLong(DbContract.BLUETOOTH.COLUMN_NAME_ADDRESS, 0)) + COMMA_SEP +
                                    bluetoothItem.optInt(DbContract.BLUETOOTH.COLUMN_NAME_BOND_STATE, Integer.MAX_VALUE) + COMMA_SEP +
                                    bluetoothItem.optInt(DbContract.BLUETOOTH.COLUMN_NAME_DBM, BaseProperty.UNAVAILABLE) + COMMA_SEP +
                                    bluetoothItem.optDouble(DbContract.BLUETOOTH.COLUMN_NAME_LATITUDE, Const.INVALID_LATITUDE) + COMMA_SEP +
                                    bluetoothItem.optDouble(DbContract.BLUETOOTH.COLUMN_NAME_LONGITUDE, Const.INVALID_LONGITUDE) + COMMA_SEP +
                                    bluetoothItem.optLong(DbContract.BLUETOOTH.COLUMN_NAME_CREATED, Const.INVALID_TIME) + NEW_LINE);
                } catch (JSONException jsonException) {
                    Log.d(TAG, "writeJsonToCsv: JsonException " + jsonException.toString());
                }
            }
            outputStreamWriter.close();
        }
    }


    /**
     * Create a zip file with all the files in the csvTempOutputDirPath directory.
     *
     * @param csvTempOutputDirPath the directory from which we zip files
     * @param uri                  the URI of the created zip file
     * @throws IOException in case the zip file cannot be created
     */
    static void zipCsvFiles(String csvTempOutputDirPath, Uri uri) throws IOException {
        final int DEFAULT_BUFFER_SIZE = 2 * 1024;

        // create the zipOutputStream from the passed uri
        OutputStream outputStream = MainApplication.getContext().getContentResolver()
                .openOutputStream(uri);
        if (outputStream == null) {
            throw new IOException("OutputStream is null. Can't create zip file.");
        }
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        ZipOutputStream zos = new ZipOutputStream(bos);

        BufferedInputStream bis = null;

        // loop on all files in csvTempOutputDirPath to write them in the zipOutputStream
        File folder = new File(csvTempOutputDirPath);
        File[] fileList = folder.listFiles();
        if (fileList != null) {
            try {
                for (File file : fileList) {
                    if (!file.isDirectory()) {
                        // create a new zipEntry
                        try {
                            bis = new BufferedInputStream(new FileInputStream(file));
                            zos.putNextEntry(new ZipEntry(file.getName()));
                            // write the new zipEntry
                            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                            int count;
                            while ((count = bis.read(buffer)) != -1) {
                                zos.write(buffer, 0, count);
                            }
                        } finally {
                            if (bis != null) {
                                bis.close();
                            }
                            zos.closeEntry();
                        }
                    }
                }
            } finally {
                zos.close();
                bos.close();
            }
        }
    }
}

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

package fr.inria.es.electrosmart.serversync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import fr.inria.es.electrosmart.BuildConfig;
import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;

public class DataUploader {

    private static final String TAG = "DataUploader";
    private static final String ENCODING = "UTF-8";

    public static String getUniqueID(String urlString) {
        Log.d(TAG, "in getUniqueID()");
        if (BuildConfig.IS_RELEASE_BUILD) {
            return makeWebRequest(urlString + "?" + Const.APP_VERSION_NUMBER_URL_PARAMETER + Tools.getAppVersionNumber());
        } else {
            return makeWebRequest(urlString + "?" + Const.APP_VERSION_NUMBER_URL_PARAMETER + Tools.getAppVersionNumber() + "&isDebug=true");
        }
    }

    /**
     * Returns the last date successfully synchronized on the server or -1 in case of error.
     *
     * @param urlString The URL string to make the request
     * @return The last successful date of -1 in case of error
     */
    private static long getLastSuccessfulDate(String urlString) {
        String dateResult = makeWebRequest(urlString + "&" + Const.APP_VERSION_NUMBER_URL_PARAMETER + Tools.getAppVersionNumber());
        long returnValue = -1;
        if (dateResult != null && !dateResult.isEmpty()) {
            try {
                returnValue = Long.parseLong(dateResult);
            } catch (NumberFormatException e) { //we received some bad values
                e.printStackTrace();
            }
        }
        return returnValue;
    }

    /**
     * Tries to connect to the server using the supplied web-service url as a parameter.
     * In case of no prior data submission for a particular user (with this uniqueID), the returned value will be an empty string.
     *
     * @param urlString the url string of the web-service.
     * @return depends on the supplied parameter: either the last successful committed data's
     * date-time (or empty string or null in case of any network errors) or a new uniqueID.
     */
    private static String makeWebRequest(String urlString) {
        Log.d(TAG, "In makeWebRequest(): urlString " + urlString);

        String webServiceReturnedResult = null; //to be returned
        HttpsURLConnection urlConnection = null;
        InputStream inputStreamForResponse = null;

        try {
            URL url = new URL(urlString);
            urlConnection = (HttpsURLConnection) url.openConnection();
            // If not set, the Timeout values are set to 0, which means infinite timeout.
            final int TIMEOUT = 30000; // in milliseconds. 30 seconds
            urlConnection.setConnectTimeout(TIMEOUT); //in milliseconds
            urlConnection.setReadTimeout(TIMEOUT);    //in milliseconds

            inputStreamForResponse = new BufferedInputStream(urlConnection.getInputStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStreamForResponse, ENCODING), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            webServiceReturnedResult = sb.toString();
        } catch (IOException e1) {
            e1.printStackTrace();

            //try to get the error response returned by the server and log it
            if (urlConnection != null && inputStreamForResponse != null) {
                InputStream inputStreamForError = new BufferedInputStream(urlConnection.getErrorStream());
                BufferedReader reader;
                try {
                    reader = new BufferedReader(new InputStreamReader(inputStreamForResponse, ENCODING), 8);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    Log.d(TAG, sb.toString());      //just log the connection error reason
                } catch (IOException e2) {
                    e2.printStackTrace();
                } finally {
                    try {
                        inputStreamForError.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } finally {
            if (inputStreamForResponse != null) {
                try {
                    inputStreamForResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        Log.d(TAG, "returning from makeWebRequest with the result [" + webServiceReturnedResult + "]");
        return webServiceReturnedResult;
    }   //end of makeWebRequest()

    /**
     * Tries to sync all the available data between the last successful sync (as given by the
     * server) and now.
     *
     * @param uniqueId gathered from the server
     * @return True if all the data was successfully uploaded, false otherwise. In case of partial
     * upload, the method returns false.
     */
    static boolean uploadData(String uniqueId) {
        Log.d(TAG, "in uploadData()");

        //get the last sync date from the server and save it into the lastSuccessfulDate (it may be null in case of errors)
        Long lastSuccessfulDate = getLastSuccessfulDate(Const.LAST_SUCCESSFUL_UPLOAD_DATE_GET_URL + uniqueId);
        Log.d(TAG, "uploadData: The server has data up to (lastSuccessfulDate) " + lastSuccessfulDate +
                ", we upload everything after it.");

        //date since when we should send the new data, so for the beginning init it to
        // the oldest possible date
        Date from;
        //check if there was an error to get the last successful date
        if (lastSuccessfulDate == -1) {
            Log.d(TAG, "lastSuccessfulDate is -1. Thus, an error happened during the retrieval of " +
                    "the lastSuccessfulDate date from the server! We abort the sync!");
            //interrupt the execution of the function so we'll be delaying the sync
            // until the next opportunity
            return false;
        } else {
            /*
            The server request data starting at lastSuccessfulDate, but on the device the
            earliest date is firstDateInDb, so the date we will use to start the search for rows in the
            DB will be the latest date between firstDateInDb and lastSuccessfulDate.
             */
            SharedPreferences settings = MainApplication.getContext()
                    .getSharedPreferences(Const.SHARED_PREF_FILE, Context.MODE_PRIVATE);
            /*
            In version 1.3R59, we introduced a bug, where we set DATE_FIRST_ENTRY_IN_DB after we
            have created DeviceInfo. As a result, we do not have DeviceInfo data for most of the
            devices that installed this version.
            More info: https://bitbucket.org/es-inria/es-android/issues/199/no-deviceinfo-osinfo-siminfo

            To recover this missing data from these devices, we propose the following hack. The idea
            is:
                 - When we get DATE_FIRST_ENTRY_IN_DB shared preference, we subtract 1 minute
                  (1 * 60 * 1000 ms) for the comparison we do with lastSuccessfulDate below.
                  - On the server, we know these devices that do not have DeviceInfo. We set
                  lastSuccessfulDate for them to 23 May, 2017 (the release date of 1.3R59), so that
                  they can send their DeviceInfo data.
            */
            final long ONE_MINUTE_IN_MS = (long) 60 * 1000;
            Date firstDateInDb = new Date(Tools.firstEntryInDb() - ONE_MINUTE_IN_MS);
            Date lastSuccessfulDate_dateFormat = new Date(lastSuccessfulDate);

            Log.d(TAG, "uploadData: firstDateInDb is " + firstDateInDb + " and lastSuccessfulDate " +
                    "is " + lastSuccessfulDate_dateFormat);
            if (firstDateInDb.compareTo(lastSuccessfulDate_dateFormat) > 0) {
                from = firstDateInDb;
            } else {
                from = lastSuccessfulDate_dateFormat;
            }
        }

        Log.d(TAG, "uploadData: from is " + from);
        boolean isHttpSuccess = true; //for checking the "header" field of the response to be 200 OK
        Calendar calendar = Calendar.getInstance();
        Date to;    //date up to where should be queried from the database
        final Date now = new Date(); // get the current date


        /*
        We have a bunch of data between time 'from' and 'to'. We send all the data (as long as
        there is no network failure) by chunks of at most Const.MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK
        lines in the database.
         */
        long numberOfRows;
        Log.d(TAG, "uploadData: The max number of rows sent in a single chunk is " +
                Const.MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK);
        /*
        We continue until we don't get a network error or reach to the required time bound
        to be synced with the server
        */
        while (isHttpSuccess && from.getTime() < now.getTime()) {
            // setup the "to" date equal to the date we want to sync for starting the first loop of
            // while block right below
            to = now;
            // get a size estimate of the number of rows to be uploaded between from and to
            numberOfRows = DbRequestHandler.getNbRowsForAllTables(from, to);
            Log.d(TAG, "uploadData: Number of rows being selected in [" + from + "," + to + "] in " +
                    "SQLite db is: " + numberOfRows);

            /*
            If the number of rows is larger than Const.MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK,
            we divide the time interval by two and compute the number of rows in this new interval.
            We continue until the number of rows is smaller or equal to
            Const.MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK.
            */
            while (numberOfRows > Const.MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK) {
                /*
                If the [from - to] time bound is bigger than Const.MIN_DICHOTOMY_INTERVAL
                halve the [from - to] time segment.
                If it is smaller, then we just quit the while loop and send the data anyway
                (even if the number of lines is larger than Const.MAX_NB_ROWS_TO_SYNC_IN_SINGLE_CHUNK
                */
                if (to.getTime() - from.getTime() > Const.MIN_DICHOTOMY_INTERVAL) {
                    long newTime = (to.getTime() + from.getTime()) / 2;
                    calendar.setTimeInMillis(newTime);
                    to = calendar.getTime();

                    //get the number of rows respective to the [from - to] time segment
                    numberOfRows = DbRequestHandler.getNbRowsForAllTables(from, to);
                    Log.d(TAG, "uploadData:           New number of rows being selected in [" +
                            from + "," + to + "] in " + "SQLite db is: " + numberOfRows);
                } else {
                    break;
                }
            }

            if (numberOfRows == 0) {
                // There is no row in the DB in [from, to]
                from = to;  //advance this empty time frame
                isHttpSuccess = true;  //let it make the next step of this while() loop
            } else {
                // There are rows to send in [from, to], so we build the JSON string and send it.
                Log.d(TAG, "uploadData: We send " + numberOfRows + " rows in this chunk.");
                String jsonString = DbRequestHandler.getJsonForSync(from, to);
                //Log.d(TAG, "uploadData: jsonString: " + jsonString);
                isHttpSuccess = sendPostRequest(Const.DATA_UPLOAD_POST_URL + uniqueId + "&" +
                        Const.APP_VERSION_NUMBER_URL_PARAMETER + Tools.getAppVersionNumber(), jsonString);
                if (isHttpSuccess) {    //check if upload was successful
                    from = to;  //next loop (if any) will continue from the successful upload date position
                }
            }
        }
        Log.d(TAG, "End of uploadData()");
        return isHttpSuccess;
    }

    /**
     * Take a string, compress it with gzip and returns the compressed string in the form of an
     * array of bytes
     *
     * @param jsonString the string to compress
     * @return The compressed string as an array of bytes or null if the string cannot be compressed
     */
    @Nullable
    private static byte[] gzipCompress(String jsonString) {
        Log.d(TAG, "in gzipCompress()");
        Log.d(TAG, "Data size before compression = " + jsonString.length() + " bytes");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = null;

        try {
            gzipOutputStream = new GZIPOutputStream(os);
            gzipOutputStream.write(jsonString.getBytes());
        } catch (IOException e) {
            Log.d(TAG, "Exception occurred while trying to compress a jsonString", e);
            return null;
        } finally {
            try {
                if (gzipOutputStream != null) {
                    gzipOutputStream.close();
                }
            } catch (IOException e) {
                Log.d(TAG, "Cannot close the gzipOutputStream. ", e);
            }
        }

        byte[] gzippedBytes = os.toByteArray();
        Log.d(TAG, "Data size after compression = " + gzippedBytes.length + " bytes");
        return gzippedBytes;
    }

    /**
     * This function does the actual POST request.
     *
     * @param webServiceUrl the url for the POST web-service
     * @param jsonString    the actual data to be sent by the POST request
     * @return true if the POST request has totally succeeded (no exceptions occurred), false otherwise
     */
    private static boolean sendPostRequest(String webServiceUrl, String jsonString) {
        Log.d(TAG, "in sendPostRequest()");

        byte[] gzippedBytes = gzipCompress(jsonString);

        if (gzippedBytes == null) {
            return false;
        }

        URL url;
        try {
            url = new URL(webServiceUrl);
        } catch (MalformedURLException e) {
            Log.d(TAG, "Invalid url: " + webServiceUrl, e);
            return false;
        }

        HttpsURLConnection urlConnection;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
        } catch (IOException e) {
            Log.d(TAG, "Exception occurred while trying create connection to: "
                    + webServiceUrl, e);
            return false;
        }

        // If not set, the Timeout values are set to 0, which means infinite timeout.
        final int TIMEOUT = 30000; // in milliseconds. 30 seconds
        urlConnection.setConnectTimeout(TIMEOUT); //in milliseconds
        urlConnection.setReadTimeout(TIMEOUT);    //in milliseconds
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setChunkedStreamingMode(0);   //for letting data to be sent in several packets

        //HTTP headers
        urlConnection.setRequestProperty("Content-Type", "application/json;charset=" + ENCODING);
        urlConnection.setRequestProperty("Accept", "application/json");
        // indicate server that we have encoded the data in gzip
        urlConnection.setRequestProperty("Content-Encoding", "gzip");

        OutputStream os = null;
        try {
            urlConnection.connect();
            os = urlConnection.getOutputStream();
            os = new BufferedOutputStream(os);
            os.write(gzippedBytes);
            os.flush();
            Log.d(TAG, "HTTP Response: " + urlConnection.getResponseCode() + " " +
                    urlConnection.getResponseMessage());
            return urlConnection.getResponseCode() == HttpsURLConnection.HTTP_OK;
        } catch (IOException e) {
            Log.d(TAG, "Exception occurred while communicating with: " + webServiceUrl, e);
            return false;
        } finally {
            // Calling disconnect even if connect fails is safe.
            // Hakim even tried with removing the call to connect and it worked without an exception
            // Here is the source code documentation on calling disconnect
            // https://android.googlesource.com/platform/external/okhttp/+/14ee8c116a297178e4168e3e4d1bb7e33f541bae/src/main/java/com/squareup/okhttp/internal/http/HttpURLConnectionImpl.java#94
            urlConnection.disconnect();
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    Log.d(TAG, "Exception occurred while closing output stream", e);
                }
            }
        }
    }

//    /**
//     * Makes a GET request to our server to record that a user with profileId is visiting
//     * the Web site for a given solution
//     *
//     * @param solution  The string representing the solution the user clicked
//     * @param profileId The profile ID of the user who clicked on the solution
//     */
//    public static void recordUserIsVisitingSolutionWebSite(String solution, String profileId) {
//        String requestUrl = Const.ES_SOLUTIONS_URL +
//                "?" + Const.SOLUTION_URL_PARAMETER + solution +
//                "&" + Const.UNIQUE_ID_URL_PARAMETER + profileId +
//                "&" + Const.APP_VERSION_NUMBER_URL_PARAMETER + Tools.getAppVersionNumber();
//        makeWebRequest(requestUrl);
//    }
}
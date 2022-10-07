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

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;

/**
 * This class is in charge of all the async task requests to the DB in order to retrieve signals
 * per time slots.
 * <p>
 * To make the request, we must call callBackground with a list of Pair<Date, Date> representing
 * the time slots on which to aggregate the signals, and a
 * DbHistoryAsyncTaskRequest.AsyncPreAndPostExecuteCallbacks that will define the behavior once the
 * request is complete and the result in the form of List<SignalsSlot> is available.
 * <p>
 * Each SignalsSlot will contain all the signals for a given Pair<Date, Date>
 */
public class DbHistoryAsyncTaskRequest {
    private static final String TAG = "DbHistoryAsyncTaskReq";
    public static boolean test;


    // Holds a reference of an async task used to request data from the local database.
    public static DbHistoryAsyncTaskRequest.BackgroundTask backgroundTask;

    /**
     * An interface used to define the onPreExecute() and onPostExecute() for a DB request when
     * calling callBackground()
     */
    public interface AsyncPreAndPostExecuteCallbacks {
        void onPreExecute();

        void onPostExecute(List<SignalsSlot> result);
    }


    /**
     * Starts a background task to load new page data from the database.
     *
     * @param list a list of the time slots to load
     */
    public static void callBackground(List<Pair<Date, Date>> list,
                                      AsyncPreAndPostExecuteCallbacks listener) {
        Log.d(TAG, "in callBackground()");
        if (backgroundTask != null && backgroundTask.isRunning()) {
            Log.d(TAG, "A backgroundTask is already running, we DO NOT execute a new one");
            // To avoid concurrency conflict we make sure we always have
            // one data retrieval thread run at a time.
            return;
        }
        backgroundTask = new BackgroundTask(listener);
        backgroundTask.execute(list);
    }

    public static void cancelCurrentAsyncTask() {
        if (DbHistoryAsyncTaskRequest.backgroundTask != null) {
            Log.d(TAG, "Cancelling async background task");
            DbHistoryAsyncTaskRequest.backgroundTask.cancel(true);
        }
    }

    /**
     * Returns a list of timeGap spaced date ranges to be used for requesting signals information
     * from the database.
     *
     * @param timeOrigin The timestamp to start from (in ms)
     * @param size       The size of the list to be returned (in unit of timeGap)
     * @param timeGap    the timeGap between two dates (in seconds)
     * @param reverse    Whether or not to reverse the order of the dates. False if the dates
     *                   should be in ascending order and true if they should be in
     *                   descending order.
     * @return A list of date ranges
     */
    public static List<Pair<Date, Date>> timeSlots(long timeOrigin, int size,
                                                   long timeGap, boolean reverse) {

        List<Pair<Long, Long>> timestamps = new ArrayList<>(size);
        long timestamp = timeOrigin;

        timeGap = timeGap * 1000;
        for (int i = 0; i < size; i++) {
            timestamps.add(new Pair<>(timestamp, timestamp + timeGap));
            timestamp += timeGap;
        }
        if (reverse) {
            for (int i = 0; i < size; i++) {
                timestamps.set(i, new Pair<>(
                        timestamps.get(i).first - timeGap * (size - 1),
                        timestamps.get(i).second - timeGap * (size - 1)
                ));
            }
        }

        List<Pair<Date, Date>> result = new ArrayList<>(size);

        for (Pair<Long, Long> p : timestamps) {
            result.add(new Pair<>(new Date(p.first), new Date(p.second)));
        }

        return result;
    }


    /**
     * An async task used to query the locale database in history mode.
     */
    static public class BackgroundTask extends AsyncTask<List<Pair<Date, Date>>, Void, List<SignalsSlot>[]> {

        private final AsyncPreAndPostExecuteCallbacks mAsyncPreAndPostExecuteCallbacks;
        private boolean mIsRunning;

        BackgroundTask(AsyncPreAndPostExecuteCallbacks asyncPreAndPostExecuteCallbacks) {
            mAsyncPreAndPostExecuteCallbacks = asyncPreAndPostExecuteCallbacks;
        }

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "in onPreExecute()");
            mIsRunning = true;
            mAsyncPreAndPostExecuteCallbacks.onPreExecute();
        }

        @Override
        protected List<SignalsSlot>[] doInBackground(List<Pair<Date, Date>>... params) {
            Log.d(TAG, "in doInBackground()");

            List<Pair<Date, Date>> timeSlots = params[0];

            Log.d(TAG, "timeSlots.size(): " + timeSlots.size());
            Log.d(TAG, "timeSlots: " + timeSlots);

            List<SignalsSlot> timeline = DbRequestHandler.getSignalsSlotsFromTimeSlots(timeSlots);

            Log.d(TAG, "doInBackground: END processing");
            return new List[]{timeline};
        }

        @Override
        protected void onPostExecute(List<SignalsSlot>[] lists) {
            Log.d(TAG, "in onPostExecute()");
            //update the global data structures from the GUI thread.
            List<SignalsSlot> result = lists[0];
            mAsyncPreAndPostExecuteCallbacks.onPostExecute(result);

            mIsRunning = false;
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "background task is onCancelled");
            mIsRunning = false;
        }

        public boolean isRunning() {
            return mIsRunning;
        }
    }
}





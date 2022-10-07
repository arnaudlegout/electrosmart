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

package fr.inria.es.electrosmart.util;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import fr.inria.es.electrosmart.R;

/**
 * Implementation of {@link TagGenerator} generating slot tags for the Live mode
 * <p>
 * Note that this generator does not support dates older than two days.
 * <p>
 * For the current day it generates only the time using the format using the string
 * resource {@code live_date_format}.
 * <p>
 * For days other than the current day it generates the date using the string
 * resource {@code yesterday} concatenated to the date generated using the format
 * in the string resource {@code live_date_format}
 * (ie. {@code "<yesterday> <formatted date>"}).
 */

public class LiveTagGenerator implements TagGenerator {

    // The date formatter that is used to generate dates
    @NonNull
    final private DateFormat mDateFormat;
    // A date object used to communicate the date to the date formatter
    @NonNull
    final private Date mDate;
    // A calender used to perform date related calculations
    @NonNull
    final private Calendar mCalendar;
    // The string to be used as a prefix when the date does not correspond to the current day.
    @NonNull
    final private String mYesterday;
    // The timestamp of the current date and relative to which we decide whether
    // it is today or yesterday
    private long mCurrentTimestamp;

    /**
     * Constructor.
     *
     * @param context          The Context the application is running in, through which we can
     *                         retrieve the string resources.
     * @param locale           The {@link Locale} that should be used to format the dates.
     * @param currentTimestamp The value with which we initialize the current timestamp.
     */
    public LiveTagGenerator(@NonNull Context context, @NonNull Locale locale,
                            long currentTimestamp) {
        mDateFormat = new SimpleDateFormat(context.getString(R.string.live_date_format), locale);
        mYesterday = context.getString(R.string.yesterday);
        mDate = new Date();
        mCurrentTimestamp = currentTimestamp;
        mCalendar = new GregorianCalendar();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String get(long timestamp) {
        mCalendar.setTimeInMillis(mCurrentTimestamp * 1000);
        long currentDay = mCalendar.get(Calendar.DAY_OF_YEAR);
        mDate.setTime(timestamp * 1000);
        mCalendar.setTimeInMillis(timestamp * 1000);
        if (mCalendar.get(Calendar.DAY_OF_YEAR) == currentDay) {
            return mDateFormat.format(mDate);
        }
        return mYesterday + " " + mDateFormat.format(mDate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrent(long timestamp) {
        mCurrentTimestamp = timestamp;
    }


}

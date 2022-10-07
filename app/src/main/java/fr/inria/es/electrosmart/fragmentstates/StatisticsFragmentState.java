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

package fr.inria.es.electrosmart.fragmentstates;


import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalsdatastructures.SignalsSlot;
import fr.inria.es.electrosmart.signalsdatastructures.Timeline;

/**
 * A class to store the state of the StatisticsFragment of a given date.
 */
public class StatisticsFragmentState {
    private double cumulativeDayBluetoothMwValue;
    private double cumulativeDayCellularMwValue;
    private double cumulativeDayTotalMwValue;
    private double cumulativeDayWiFiMwValue;
    private int previousDayAverageDbmValue;
    private double dayAverageMwValue;
    private int nbSourcesWifiDay;
    private int nbSourcesBluetoothDay;
    private int nbSourcesCellularDay;
    private List<BaseProperty> topFiveSignalsDay;
    private Hashtable<BaseProperty, SignalsSlot> signalToSignalsSlotTable;
    private Hashtable<SignalsSlot, Long> signalsSlotToTimeTable;
    private Calendar mFirstDay;
    private Timeline mTimeline;

    // Variable to store when the fragment state was created
    private long mCreatedAt;

    public StatisticsFragmentState(double cumulativeDayBluetoothMwValue,
                                   double cumulativeDayCellularMwValue,
                                   double cumulativeDayTotalMwValue,
                                   double cumulativeDayWiFiMwValue,
                                   int previousDayAverageDbmValue,
                                   double dayAverageMwValue,
                                   int nbSourcesWifiDay,
                                   int nbSourcesBluetoothDay,
                                   int nbSourcesCellularDay,
                                   List<BaseProperty> topFiveSignalsDay,
                                   Hashtable<BaseProperty, SignalsSlot> signalToSignalsSlotTable,
                                   Hashtable<SignalsSlot, Long> signalsSlotToTimeTable,
                                   Calendar firstDay,
                                   Timeline timeline) {
        this.cumulativeDayBluetoothMwValue = cumulativeDayBluetoothMwValue;
        this.cumulativeDayCellularMwValue = cumulativeDayCellularMwValue;
        this.cumulativeDayTotalMwValue = cumulativeDayTotalMwValue;
        this.cumulativeDayWiFiMwValue = cumulativeDayWiFiMwValue;
        this.previousDayAverageDbmValue = previousDayAverageDbmValue;
        this.dayAverageMwValue = dayAverageMwValue;
        this.nbSourcesWifiDay = nbSourcesWifiDay;
        this.nbSourcesBluetoothDay = nbSourcesBluetoothDay;
        this.nbSourcesCellularDay = nbSourcesCellularDay;
        this.topFiveSignalsDay = topFiveSignalsDay;
        this.signalToSignalsSlotTable = signalToSignalsSlotTable;
        this.signalsSlotToTimeTable = signalsSlotToTimeTable;
        this.mFirstDay = firstDay;
        this.mTimeline = timeline;
        this.mCreatedAt = System.currentTimeMillis();
    }

    public Calendar getStatDay() {
        return mFirstDay;
    }

    public double getCumulativeDayBluetoothMwValue() {
        return cumulativeDayBluetoothMwValue;
    }

    public double getCumulativeDayCellularMwValue() {
        return cumulativeDayCellularMwValue;
    }

    public double getCumulativeDayTotalMwValue() {
        return cumulativeDayTotalMwValue;
    }

    public double getCumulativeDayWiFiMwValue() {
        return cumulativeDayWiFiMwValue;
    }

    public int getPreviousDayAverageDbmValue() {
        return previousDayAverageDbmValue;
    }

    public double getDayAverageMwValue() {
        return dayAverageMwValue;
    }

    public int getNbSourcesWifiDay() {
        return nbSourcesWifiDay;
    }

    public int getNbSourcesBluetoothDay() {
        return nbSourcesBluetoothDay;
    }

    public int getNbSourcesCellularDay() {
        return nbSourcesCellularDay;
    }

    public List<BaseProperty> getTopFiveSignalsDay() {
        return topFiveSignalsDay;
    }

    public Hashtable<SignalsSlot, Long> getSignalsSlotToTimeTable() {
        return signalsSlotToTimeTable;
    }

    public Hashtable<BaseProperty, SignalsSlot> getSignalToSignalsSlotTable() {
        return signalToSignalsSlotTable;
    }

    public Timeline getTimeline() {
        return mTimeline;
    }

    /**
     * Returns true if the created state has been created today, false otherwise.
     *
     * @return true or false
     */
    public boolean hasBeenCachedToday() {
        return DateUtils.isToday(this.mCreatedAt);
    }
}
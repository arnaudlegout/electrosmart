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
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Locale;

import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.Tools;
import fr.inria.es.electrosmart.database.DbRequestHandler;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;

public final class DeviceInfoMonitor {


    private static final String TAG = "DeviceInfoMonitor";

    private DeviceInfoMonitor() {
    }

    /**
     * This method is the entry point of the monitor. The {@link MeasurementScheduler} calls this
     * method when a scan must be performed.
     */
    public static void run(boolean isForeground) {
        Log.d(TAG, "run DeviceInfoMonitor");
        if (isForeground) {
            new runDeviceInfoMonitorAsyncTask().execute();
        } else {
            Log.d(TAG, "we are in BACKGROUND, we runDeviceInfoMonitor in current thread!");
            runDeviceInfoMonitor();
        }
        Log.d(TAG, "run DeviceInfoMonitor: DONE!");

    }

    private static void runDeviceInfoMonitor() {
        Log.d(TAG, "in runDeviceInfoMonitor");
        long currentTime = System.currentTimeMillis();

        String socManufacturer = "";
        String socModel = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            socManufacturer = Build.SOC_MANUFACTURER;
            socModel = Build.SOC_MODEL;
        }
        // updates the device info
        DbRequestHandler.updateDeviceInfoTable(Build.BRAND, Build.DEVICE, Build.HARDWARE,
                Build.MANUFACTURER, Build.MODEL, Build.PRODUCT, socManufacturer, socModel,
                currentTime);

        Log.d(TAG, "runDeviceInfoMonitor: Build.SOC_MANUFACTURER: " + socManufacturer
                + " Build.SOC_MODEL: " + socModel + " Build.BRAND: " + Build.BRAND + " Build.DEVICE: " +
                Build.DEVICE + " Build.HARDWARE: " + Build.HARDWARE + " Build.MANUFACTURER: " +
                Build.MANUFACTURER + " Build.MODEL: " + Build.MODEL + " Build.PRODUCT: " + Build.PRODUCT);

        // updates the OS info
        DbRequestHandler.updateOSInfoTable(Build.VERSION.RELEASE, Build.VERSION.SDK_INT, currentTime);

        // update the SIM info
        TelephonyManager telephonyManager = (TelephonyManager) MainApplication.getContext().getSystemService(Context.TELEPHONY_SERVICE);
        DbRequestHandler.updateSIMInfoTable(telephonyManager.getSimCountryIso(),
                telephonyManager.getSimOperator(),
                telephonyManager.getSimOperatorName(),
                currentTime);

        // update the app version info
        int version = Tools.getAppVersionNumber();
        if (version != -1) {
            DbRequestHandler.updateAppVersionTable(version, currentTime);
        }

        // update the DeviceLocale info
        // Locale.getDefault().toString() returns "en_US" aka language and country if exists
        DbRequestHandler.updateDeviceLocaleTable(Locale.getDefault().toString(), currentTime);
        Log.d(TAG, "runDeviceInfoMonitor: DONE!");
    }

    /*
      Asynchronous task to run all device monitors.

      The order of execution described in
      https://developer.android.com/reference/android/os/AsyncTask.html
      guarantees that starting with HONEYCOMB (3.0) submitted tasks are executed sequentially on a
      single thread.
    */
    private static class runDeviceInfoMonitorAsyncTask extends AsyncTask<Void, Void, Void> {
        protected final Void doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground: deviceInfoMonitor starting DB dump...");
            runDeviceInfoMonitor();
            Log.d(TAG, "doInBackground: deviceInfoMonitor DONE!");
            return null;
        }
    }

}

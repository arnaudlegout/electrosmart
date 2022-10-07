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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.Tools;

/**
 * Here is the logic behind the location monitor.
 * <p>
 * When in foreground, the location monitor is always running with highest accuracy.
 * <p>
 * When in background, we must have an accurate location without draining the battery. The heuristic
 * we use to achieve that is the following.
 * We start the network provider, and the GPS provider if not power constrained. The duration of the
 * measurement period depends whether we are in Doze or not.
 * <p>
 * If in Doze, we start and stop the
 * providers in the same alarm, so the measurement period is very short (typically in the order
 * of a second). In this case, it is unlikely that the GPS, and to a less extent, the network
 * provider will get an update. However, under excellent conditions (possibly another app is making
 * updates, the GPS found its fix, etc.) we might get update so it is still worth to try.
 * <p>
 * If not in Doze, we start the providers for 30 seconds with a single update (to do not drain the
 * battery) and stop the location monitor in a second alarm. 30 seconds should be enough for
 * both the network and GPS providers to get an update.
 * <p>
 * In all cases, before getting a coordinate, the measurement monitors must call
 * updateCoordinatesWithLastKnownLocation() in order to get the freshest last known location.
 * <p>
 * In summary, many apps are making location measurements (including system apps such as the weather
 * app), so the getLastKnownLocation() on the providers already gives a good coordinate estimate.
 * As we want to increase the accuracy, we start ourselves a location measurement for a short period
 * of time before the signal measurements.
 */
public final class LocationMonitor {


    private static final String TAG = "LocationMonitor";
    private static double latitude = -1;
    private static double longitude = -1;
    private static float bearing = -1f;
    private static double altitude = -1;
    private static float accuracy = -1f;
    private static float speed = -1f;
    private static String provider = "";
    private static Location lastKnownLocation;
    private static LocationManager locationManager;
    private static LocationListener locationListener;

    private LocationMonitor() {
    }

    /**
     * Start the location listener. After calling this method the location is monitored, which lead
     * to a higher battery consumption. So this method must be called only when needed.
     */
    public static void startLocationMonitor(boolean isForeground) {
        Log.d(TAG, "in startLocationMonitor");
        Context context = MainApplication.getContext();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // we must not have more than a single location listener
        if (locationListener == null) {
            locationListener = new MyLocationLister();
        }

        long MIN_TIME_BETWEEN_LOCATION_UPDATES;
        long MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES;
        if (isForeground) {
            MIN_TIME_BETWEEN_LOCATION_UPDATES = Const.MIN_TIME_BETWEEN_LOCATION_UPDATES_FOREGROUND;
            MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES = Const.MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES_FOREGROUND;
        } else {
            MIN_TIME_BETWEEN_LOCATION_UPDATES = Const.MIN_TIME_BETWEEN_LOCATION_UPDATES_BACKGROUND;
            MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES = Const.MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES_BACKGROUND;
        }

        // Register the listener with the Location Manager to receive location updates
        if (Tools.isAccessFineLocationGranted(context)) {
            Log.d(TAG, "startLocationMonitor: locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)" +
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.d(TAG, "startLocationMonitor: starting the NETWORK_PROVIDER ");
                if (isForeground) {
                    Log.d(TAG, "startLocationMonitor: in foreground, calling requestLocationUpdates");
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BETWEEN_LOCATION_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES,
                            locationListener);
                } else {
                    Log.d(TAG, "startLocationMonitor: in background, calling requestSingleUpdate");
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                            locationListener, Looper.getMainLooper());
                }
            }

            // we start the GPS_PROVIDER only if we are not power constrained
            Log.d(TAG, "startLocationMonitor: locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)" +
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !isPowerConstrained(isForeground)) {
                Log.d(TAG, "startLocationMonitor: starting the GPS_PROVIDER");
                if (isForeground) {
                    Log.d(TAG, "startLocationMonitor: in foreground, calling requestLocationUpdates");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            MIN_TIME_BETWEEN_LOCATION_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_LOCATION_UPDATES,
                            locationListener);
                } else {
                    Log.d(TAG, "startLocationMonitor: in background, calling requestSingleUpdate");
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER,
                            locationListener, Looper.getMainLooper());
                }
            }

            updateCoordinatesWithLastKnownLocation();

        } else {
            Log.w(TAG, "startLocationMonitor: ACCESS_FINE_LOCATION not granted. Cannot get location for measurements!");
            // set locations to unavailable, that is, coordinates at -1
            latitude = -1;
            longitude = -1;
        }
    }


    /**
     * Update the location with the last known location from all available providers. This method
     * must be called when starting the location monitor and each time we retrieve the latitude
     * and longitude to be sure we always get the freshest coordinates.
     */
    public static void updateCoordinatesWithLastKnownLocation() {
        Log.d(TAG, "in updateCoordinatesWithLastKnownLocation");
        Context context = MainApplication.getContext();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Register the listener with the Location Manager to receive location updates
        if (Tools.isAccessFineLocationGranted(context)) {
            Location lastKnownLocationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            boolean isBetterLocationResult = isBetterLocation(lastKnownLocationNetwork, lastKnownLocationGPS);
            Log.d(TAG, "updateCoordinatesWithLastKnownLocation: lastKnownLocationNetwork: " +
                    lastKnownLocationNetwork + " lastKnownLocationGPS: " + lastKnownLocationGPS +
                    " isBetterLocationResult: " + isBetterLocationResult);
            lastKnownLocation = isBetterLocationResult ? lastKnownLocationNetwork : lastKnownLocationGPS;
            if (lastKnownLocation != null) {
                updateCoordinates(lastKnownLocation);
            }
            Log.d(TAG, "updateCoordinatesWithLastKnownLocation result: " + lastKnownLocation);
        } else {
            Log.w(TAG, "updateCoordinatesWithLastKnownLocation: ACCESS_FINE_LOCATION not granted." +
                    " Cannot get last known location!");
        }
        Log.d(TAG, "updateCoordinatesWithLastKnownLocation: current location state - " +
                "(latitude, longitude) = " + getLatitude() + ", " + getLongitude());
    }


    /**
     * Stop the location listener, after calling this method the location is no more monitored.
     * This method can be called safely even if startLocationMonitor() has not been called before.
     */
    public static void stopLocationMonitor() {
        Log.d(TAG, "in stopLocationMonitor");
        Context context = MainApplication.getContext();
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (locationManager != null && locationListener != null) {
                Log.d(TAG, "stopLocationMonitor: removeUpdates");
                locationManager.removeUpdates(locationListener);
            } else {
                Log.d(TAG, "stopLocationMonitor:  nothing to stop - locationManager or locationLister null");
            }
        } else {
            Log.w(TAG, "ACCESS_FINE_LOCATION not granted. Cannot remove location listener!");
        }
    }


    private static void updateCoordinates(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        bearing = location.getBearing();
        accuracy = location.getAccuracy();
        speed = location.getSpeed();
        provider = location.getProvider();
    }

    /**
     * Determines whether one Location reading is better than the current Location fix. Returns
     * true is location is better than currentBestLocation, false otherwise
     * <p/>
     * This code is from Android API guide here
     * http://developer.android.com/guide/topics/location/strategies.html
     *
     * @param newLocation         The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    private static boolean isBetterLocation(Location newLocation, Location currentBestLocation) {
        Log.d(TAG, "in isBetterLocation()");
        final int TWO_MINUTES = 1000 * 60 * 2;

        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        } else if (newLocation == null) {
            // An old location is always better than no location
            return false;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * This method returns true if the app is in background mode AND the device is not plugged in AND
     * the battery level if below MIN_BATTERY_LEVEL. This method
     * make use of the latest M API when available to know the current charging state.
     * <p>
     * to test with a battery level of, e.g. 30%, use the command line
     * adb shell dumpsys battery unplug
     * adb shell dumpsys battery set level 30
     * <p>
     * Here we add additional documentation on isCharging (made by A. Akodadi in a mail sent on
     * 23/05/2017).
     * <p>
     * The method isCharging(), introduced in Android M API 23, is implemented in the file
     * `frameworks/base/core/java/com/android/internal/os/BatteryStatsImpl.java`  that can be found
     * at https://source.android.com/
     * <p>
     * In summary, we found that whenever the device is unplugged, isCharging() returns false.
     * It returns true when the device is plugged in and the battery level has increased by at
     * least 1%, or when the battery level is greater or equal to 90%.
     * However, even if the device is plugged in but the battery level has dropped by at least 1%
     * isCharging is returning false (this might happen if the charger is not powerful enough
     * to charge the battery)
     * <p>
     * The consequence of this logic is that when we plug in a device, isCharging() needs to wait
     * for the battery level to increase by 1% before returning true. In our test, it takes between
     * 30s to 7 minutes depending on the device when plugged in a laptop USB port.
     * <p>
     * Note that this behavior of isCharging() is perfectly fine for our purposes as we want to be
     * sure that enabling the GPS will not drain the battery in the background. Waiting for a few
     * minutes to get the correct charging state has no practical impact.
     *
     * @return true is the application must be power constrained, false otherwise
     */
    private static boolean isPowerConstrained(boolean isForeground) {
        Log.d(TAG, "in isPowerConstrained()");
        boolean isCharging = false; // if we don't know, in doubt, we are conservative
        /*
        Between 0 (empty) and 1 (full)
        When set to 1.1, it means that the battery level is no more a constraint
        */
        final float MIN_BATTERY_LEVEL = 1.1f;

        // according to the code available in
        // http://developer.android.com/training/monitoring-device-state/battery-monitoring.html
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = MainApplication.getContext().registerReceiver(null, ifilter);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Are we charging / charged?
            if (batteryStatus != null) {
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
            }
        } else {
            BatteryManager batteryManager = (BatteryManager) MainApplication.getContext().
                    getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                isCharging = batteryManager.isCharging();
            }
        }

        float batteryPercentage;
        if (batteryStatus != null) {
            // level: battery level from 0 to scale
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            // scale: maximum battery level
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPercentage = level / (float) scale;
        } else {
            batteryPercentage = -1;
        }

        Log.d(TAG, "location isPowerConstrained: isForeground: " + isForeground +
                " isCharging: " + isCharging + " batteryPercentage: " + batteryPercentage);
        boolean isPowerConstraint = !isForeground && !isCharging && (batteryPercentage < MIN_BATTERY_LEVEL);
        Log.d(TAG, "location isPowerConstrained: " + isPowerConstraint);
        return isPowerConstraint;
    }

    public static double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return longitude;
    }

    public static double getBearing() {
        return bearing;
    }

    public static double getAltitude() {
        return altitude;
    }

    public static double getAccuracy() {
        return accuracy;
    }

    public static double getSpeed() {
        return speed;
    }

    public static String getProvider() {
        return provider;
    }

    /**
     * This method returns true if (latitude, longitude) corresponds to a real measured location,
     * false otherwise
     * <p>
     * We initialize latitude and longitude to -1 so if any of them is set to -1, it means that
     * the location is not valid. In addition, the android.location.Location class initializes
     * latitude and longitude to 0.0. This seems a bad choice because the location 0.0, 0.0
     * is a valid one (in the atlantic ocean where equator and greenwich cross), but some devices
     * return 0.0, 0.0 when they do not find a location. Therefore, we have no way
     * to know whether 0.0, 0.0 is a real location or a default value. However, as it is frequent
     * that devices return 0.0, 0.0 and it is very unlikely that 0.0, 0.0 will be a real
     * location, we decided to also consider 0.0, 0.0 as an invalid location.
     *
     * @param latitude  the latitude to be tested
     * @param longitude the longitude to be tested
     * @return true is (latitude, longitude) corresponds to a real measured location
     */
    public static boolean isValidLocation(double latitude, double longitude) {
        return latitude >= 0.0 && longitude >= 0.0 && !(latitude == 0.0 && longitude == 0.0);
    }

    private static class MyLocationLister implements LocationListener {
        // Called when a new location is found by the network location provider.
        public void onLocationChanged(Location newLocation) {
            Log.d(TAG, "in onLocationChanged");
            boolean isBetterLocationResult = isBetterLocation(newLocation, lastKnownLocation);
            Log.d(TAG, "onLocationChanged: lastKnownLocation: " + lastKnownLocation + " new location: " + newLocation +
                    " isBetterLocationResult: " + isBetterLocationResult);
            lastKnownLocation = isBetterLocationResult ? newLocation : lastKnownLocation;
            Log.d(TAG, "onLocationChanged: lastKnownLocation (after isBetterLocation): " + lastKnownLocation);
            if (lastKnownLocation != null) {
                updateCoordinates(lastKnownLocation);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    }
}
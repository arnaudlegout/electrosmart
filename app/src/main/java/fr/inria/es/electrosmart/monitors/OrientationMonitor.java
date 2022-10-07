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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Arrays;

import fr.inria.es.electrosmart.Const;
import fr.inria.es.electrosmart.MainApplication;
import fr.inria.es.electrosmart.database.DbContract;
import fr.inria.es.electrosmart.database.DbHelper;
import fr.inria.es.electrosmart.scheduling.MeasurementScheduler;

/**
 * OrientationMonitor is a singleton whose instance can be accessed
 * with OrientationMonitor.getInstance()
 */
class OrientationMonitor {

    private static final String TAG = "OrientationMonitor";
    private static OrientationMonitor orientationMonitor = new OrientationMonitor(); //singleton
    private RotationListener rotationListener = new RotationListener();

    private OrientationMonitor() {
    }

    /**
     * Method to access the singleton instance of the OrientationMonitor
     *
     * @return the instance of the OrientationMonitor
     */
    public static OrientationMonitor getInstance() {
        return orientationMonitor;
    }

    /**
     * This method starts the orientation listener. After the first event received, the orientation
     * listener writes into the DB the orientation vector, and automatically stop the listener.
     * <p/>
     * The Appropriate way to measure an orientation is by using the following command:
     * OrientationMonitor.getInstance().start();
     */
    public void start() {
        rotationListener.start();
    }

    private class RotationListener implements SensorEventListener {

        private SensorManager sensorManager;
        private Sensor rotationSensor;

        private RotationListener() {
            sensorManager = (SensorManager) MainApplication.getContext().getSystemService(Context.SENSOR_SERVICE);
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        private void start() {
            Log.d(TAG, "RotationListener start");
            // enable our sensor
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
        }

        /**
         * this method is automatically called by the event listener when the first event is received
         */
        private void stop() {
            Log.d(TAG, "RotationListener stop");
            // make sure to turn our sensor off when we have the orientation
            sensorManager.unregisterListener(this);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            Log.d(TAG, "in onSensorChanged. event.sensor.getType(): " + event.sensor.getType());
            // we received a sensor event. We check if it is the correct event.
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {

                // we stop the listener after the first received event
                this.stop();
                // we dump the rotation vector in the database
                if (MeasurementScheduler.schedulerMode == MeasurementScheduler.SchedulerMode.FOREGROUND) {
                    Log.d(TAG, "onSensorChanged: start DB dump async task");
                    new dumpOrientationToDatabaseAsyncTask().execute(event);
                    Log.d(TAG, "onSensorChanged: end DB dump async task");
                } else {
                    Log.d(TAG, "onSensorChanged: we are in BACKGROUND, we dumpOrientationToDatabase in current thread!");
                    dumpOrientationToDatabase(event);
                }

                /*
                The computation of the rotation matrix is only available for debugging and
                experimental purposes. We don't currently use it for correct behavior of ES.

                The earth reference direct orthonormal basis is the following

                E is defined as the vector product N x G. It is tangential to the ground at
                    the device's current location and *points approximately East*.
                N is tangential to the ground at the device's current location and points
                    toward the geomagnetic North Pole.
                G points toward the sky and is perpendicular to the ground plane.

                The phone basis is the following when a device is held in its default orientation,
                X axis is horizontal and points to the right,
                Y axis is vertical and points up,
                Z axis points toward the outside of the screen face

                The rotation matrix gives the coordinate of the earth reference basis in the
                phone basis.
                (Ex Ey Ez,
                 Nx Ny Nz,
                 Gx, Gy, Gz)
                 where Ex, Ey, Ez are the coordinate of the E axis in the phone basis
                       Nx, Ny, Nz are the coordinate of the N axis in the phone basis
                       Gx, Gy, Gz are the coordinate of the G axis in the phone basis
                 */


                /*
                IMPORTANT NOTE ON SensorManager.getRotationMatrixFromVector()

                This method is buggy on Android 4.3 on Samsung devices. The problem is that
                event.values can be an array of more than 4 elements, but in the buggy Android 4.3
                version on Samsung devices, instead of ignoring the extra values (as it should be)
                the method throw an exception

                java.lang.IllegalArgumentException: R array length must be 3 or 4
                (see https://bitbucket.org/es-inria/es-android/issues/150/javalangillegalargumentexception-r-array)

                Indeed, in API 18 (that is Android 4.3) a new value was added to event.values
                see https://developer.android.com/reference/android/hardware/SensorEvent.html for
                Sensor.TYPE_ROTATION_VECTOR

                Most likely this change was not taken into account on the Samsung custom rom and
                they performed an extra check compared to the vanilla Android 4.3 that led to this
                exception.

                Some discussion on this issue can be found in
                https://github.com/nvanbenschoten/motion/issues/16
                https://github.com/cgeo/cgeo/issues/4255

                and here they present a possible fix
                https://github.com/cgeo/cgeo/commit/37cdda2751c1c74918f32c58bb2c5952d692cbcd

                The fix is easy, we simply pass to SensorManager.getRotationMatrixFromVector()
                and array that does not exceed 4 values by removing the fifth one if it exists.
                As only the 4 first values are used in the method getRotationMatrixFromVector(),
                it will not impact the correctness of the computation.

                Here is an example of the code

                // populate rotation_matrix by side effect
                float[] rotation_matrix = new float[9];
                float[] stripped_event_values = new float[4];
                if (event.values.length > 4) {
                    // we strip from event.values all values after the 4th one
                    System.arraycopy(event.values, 0, stripped_event_values, 0, 4);
                    SensorManager.getRotationMatrixFromVector(rotation_matrix, stripped_event_values);
                } else {
                    SensorManager.getRotationMatrixFromVector(rotation_matrix, event.values);
                }
                Log.d(TAG, "onSensorChanged: rotation_matrix: " + Arrays.toString(rotation_matrix));

                However, I decided not to use this code for two reasons. First, whereas I have a
                Samsung S3 phone with Android 4.3, I am not able to reproduce the bug, so testing
                that the bug is fixed with this new code (even if online resources claim it is) will
                be hard. Second, I do not need to call SensorManager.getRotationMatrixFromVector()
                for the correct behavior of ES, I was only using it for debugging purposes. So there
                is not strong reason to keep calling this code that might lead to a crash of the app
                even if it is in rare circumstances and the above code most likely fix the issue.
                 */


                // FOR DEBUGGING PURPOSES ONLY
                // Never uncomment the following code below unless thoroughly tested taking into
                // account the bug reported above
                /*
                float[] rotation_matrix = new float[9];
                SensorManager.getRotationMatrixFromVector(rotation_matrix, event.values);
                Log.d(TAG, "onSensorChanged: rotation_matrix: " + Arrays.toString(rotation_matrix));
                */

                /*
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotation_matrix, orientation);
                Log.d(TAG, "onSensorChanged: orientation: " + Arrays.toString(orientation));
                */
            }
        }

        private void dumpOrientationToDatabase(SensorEvent event) {
            /* see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
               and
               https://source.android.com/devices/sensors/sensor-types.html#attitude_composite_sensors
               for a description of the meaning of each value in the event

               The orientation of the phone is represented by the rotation necessary to align
               the East-North-Up coordinates with the phone's coordinates. That is, applying the
               rotation to the world frame (X,Y,Z) would align them with the phone coordinates (x,y,z).

               The rotation can be seen as rotating the phone by an angle theta around an axis
               rot_axis to go from the reference (East-North-Up aligned) device orientation to
               the current device orientation.

               values[0]: x*sin(θ/2)
               values[1]: y*sin(θ/2)
               values[2]: z*sin(θ/2)
               values[3]: cos(θ/2)
               values[4]: estimated heading Accuracy (in radians) (-1 if unavailable)

               values[3], originally optional, will always be present from SDK Level 18 onwards.
               values[4] is a new value that has been added in SDK Level 18.

               event.timestamp is the time in nanosecond since the last boot see:
               https://code.google.com/p/android/issues/detail?id=7981
            */

            Log.d(TAG, "in dumpOrientationToDatabase");
            long currentTime = System.currentTimeMillis();
            Log.d(TAG, "dumpOrientationToDatabase: currentTime: " + currentTime +
                    " accuracy: " + event.accuracy +
                    " sensor: " + event.sensor +
                    " timestamp: " + event.timestamp +
                    " values length: " + event.values.length);
            Log.d(TAG, "dumpOrientationToDatabase: events.values: " + Arrays.toString(event.values));

            /*
              Depending on the version of Android values[3] and values[4] might not be present.
              However, as values[3] is optional before 18, it is not clear whether it will use a
              default value, or be missing. To simplify the number of cases to test, we do not test
              for the Android version, but just for the length of event.values.
            */
            ContentValues contentValues;
            if (event.values.length == 3) {
                contentValues = DbHelper.createOrientationContentValues(
                        event.values[0],
                        event.values[1],
                        event.values[2],
                        Const.INVALID_ORIENTATION_FIELD,
                        Const.INVALID_ORIENTATION_FIELD,
                        currentTime);
            } else if (event.values.length == 4) {
                contentValues = DbHelper.createOrientationContentValues(
                        event.values[0],
                        event.values[1],
                        event.values[2],
                        event.values[3],
                        Const.INVALID_ORIENTATION_FIELD,
                        currentTime);
            } else {
                contentValues = DbHelper.createOrientationContentValues(
                        event.values[0],
                        event.values[1],
                        event.values[2],
                        event.values[3],
                        event.values[4],
                        currentTime);
            }

            SQLiteDatabase db = MainApplication.dbHelper.getWritableDatabase();
            db.insert(DbContract.ORIENTATION.TABLE_NAME, null, contentValues);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        /*
          Asynchronous task to update the database.

          The order of execution described in
          https://developer.android.com/reference/android/os/AsyncTask.html
          guarantees that starting with HONEYCOMB (3.0) submitted tasks are executed sequentially on a
          single thread.

          About the compiler warnings related to varargs methods (or non-reifiable types, or
          heap pollution) here are some explanation
          http://docs.oracle.com/javase/tutorial/java/generics/nonReifiableVarargsType.html
          http://docs.oracle.com/javase/7/docs/technotes/guides/language/non-reifiable-varargs.html

          For short, what I am doing is safe, but the compiler cannot check it at compile time, thus
          the warnings.
         */
        private class dumpOrientationToDatabaseAsyncTask extends AsyncTask<SensorEvent, Void, Void> {
            protected final Void doInBackground(SensorEvent... event) {
                Log.d(TAG, "doInBackground: dumpOrientationToDatabase starting DB dump...");
                dumpOrientationToDatabase(event[0]);
                Log.d(TAG, "doInBackground: dumpOrientationToDatabase DONE!");
                return null;
            }
        }
    }


}

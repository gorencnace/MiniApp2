/*
 * ACCELERATION SERVICE
 *
 * here we handle gestures
 *
 */

package si.uni_lj.fri.pbd.miniapp2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class AccelerationService extends Service implements SensorEventListener {

    /*
     * BINDING
     */

    public class RunServiceBinder extends Binder {
        AccelerationService getService() {
            return AccelerationService.this;
        }
    }

    private final IBinder serviceBinder = new AccelerationService.RunServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding service");
        return this.serviceBinder;
    }

    /*
     * FIELDS
     */

    // TAG
    private static final String TAG = AccelerationService.class.getSimpleName();

    // sensor management
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // ACTION FEILDS
    public static final String IDLE = "si.uni_lj.fri.pbd.miniapp2.IDLE";
    public static final String HORIZONTAL = "si.uni_lj.fri.pbd.miniapp2.HORIZONTAL";
    public static final String VERTICAL = "si.uni_lj.fri.pbd.miniapp2.VERTICAL";
    public static final String GESTURE_BROADCAST = "si.uni_lj.fri.pbd.miniapp2.GESTURE_BROADCAST";
    public static final String MOVEMENT = "si.uni_lj.fri.pbd.miniapp2.MOVEMENT";

    // fields used for sensor calculations
    public static final int noiseThreshold = 5;
    private float x0, y0, z0;
    protected ArrayList<float[]> mSensorReadings;
    private long lastTime = 0;

    public AccelerationService() {

    }

    /*
     * SERVICE LIFECYCLE METHODS
     */

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating service");
        // we get sensor manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // we get accelerometer sensor
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorReadings = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying service");
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        getAccelerometer(event);
        String command = IDLE;
        // when we have 50 readings of sensor we calculate if we want to play or pause song
        if (mSensorReadings.size() > 49) {
            int horizontal = 0;
            int vertical = 0;
            for (float[] d : mSensorReadings) {
                if (d[0] > d[2]) {
                    horizontal++;
                } else if (d[0] < d[2]) {
                    vertical++;
                }
            }
            if (vertical < horizontal) {
                command = HORIZONTAL;
                Log.d(TAG, "PAUSE");
            } else if (horizontal < vertical) {
                command = VERTICAL;
                Log.d(TAG, "PLAY");
            }
            mSensorReadings.clear();
            // we broadcast only if command doesn't equal to IDLE and it has been 2 seconds since last broadcast
            if (!command.equals(IDLE) && ((System.currentTimeMillis() - lastTime) > 2000)) {
                Log.d(TAG, command);
                Intent broadcast = new Intent(GESTURE_BROADCAST);
                broadcast.putExtra(MOVEMENT, command);
                sendBroadcast(broadcast);
                lastTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /*
     * UTILITY
     */

    // calculating events
    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        if (x0 != 0 || y0 != 0 || z0 != 0) {
            float dx = Math.abs(values[0] - x0) <= noiseThreshold ? 0 : Math.abs(values[0] - x0);
            float dy = Math.abs(values[1] - y0) <= noiseThreshold ? 0 : Math.abs(values[1] - y0);
            float dz = Math.abs(values[2] - z0) <= noiseThreshold ? 0 : Math.abs(values[2] - z0);
            mSensorReadings.add(new float[]{dx, dy, dz});
        }
        x0 = values[0];
        y0 = values[1];
        z0 = values[2];

    }
}

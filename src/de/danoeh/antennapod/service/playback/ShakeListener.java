// Copyright (c) 2014 geir54

package de.danoeh.antennapod.service.playback;

import android.hardware.SensorManager;
import android.content.Context;
import java.lang.UnsupportedOperationException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class ShakeListener implements SensorEventListener
{
    private static final String TAG = "ShakeListener";

    private Sensor mAccelerometer;
    private SensorManager mSensorMgr;
    private OnShakeListener mShakeListener;
    private Context mContext;

    float lastForce = 99;

    public interface OnShakeListener
    {
        public void onShake();
    }

    public ShakeListener(Context context)
    {
        mContext = context;
        resume();
    }

    public void setOnShakeListener(OnShakeListener listener)
    {
        mShakeListener = listener;
    }

    public void resume() {
        mSensorMgr = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        mAccelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (!mSensorMgr.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)) { // if not supported
            mSensorMgr.unregisterListener(this);
            throw new UnsupportedOperationException("Accelerometer not supported");
        }
    }

    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this);
            mSensorMgr = null;
        }
    }

    public void onAccuracyChanged(Sensor s, int accuracy) { } // not used

    public void onSensorChanged(SensorEvent event)
    {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float force = Math.abs(x)+Math.abs(y)+Math.abs(z);
        float t = force-lastForce;
        if (t > 15) {
            Log.d(TAG, "Detected shake "+ t);
            if (mShakeListener != null) mShakeListener.onShake();
        }
        lastForce = force;
    }

}
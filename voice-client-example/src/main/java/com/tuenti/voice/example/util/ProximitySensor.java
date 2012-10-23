package com.tuenti.voice.example.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.tuenti.voice.example.ui.CallView;

public class ProximitySensor implements SensorEventListener {
    private static final float ON_EAR_DISTANCE = 3.0f;

    // Proximity Sensor
    private final SensorManager mSensorManager;

    private final Sensor mProximity;

    private final float mMaxRangeProximity;

    private Context mContext;

    private CallView mCallInProgressCallback;

    public ProximitySensor(CallView callInProgress) {
        mContext = (Context) callInProgress;
        mCallInProgressCallback = callInProgress;
        mSensorManager = (SensorManager) mContext
                .getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (mProximity != null) {
            mMaxRangeProximity = mProximity.getMaximumRange();
            mSensorManager.registerListener(this, mProximity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mMaxRangeProximity = 10;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Proximity Sensor Event returns cm from phone.
        // Proximity in some devices is anything less than mMaxRangeProximity
        // on my test phone 9.0f or 0.0f for the two states.
        // Others might measure it more accurately.
        // TODO(Luke): Headset case isn't covered here at all, in which case we
        // probably
        // want to probably do partial_wake_lock and not change the screen
        // brightness.
        if (event.values[0] < mMaxRangeProximity
                && event.values[0] <= ON_EAR_DISTANCE) {
            mCallInProgressCallback.onProximity();
        } else {
            mCallInProgressCallback.onUnProximity();
        }
    }

    public void destroy() {
        if (mProximity != null) {
            mSensorManager.unregisterListener(this);
        }
    }
}

package com.tuenti.voice.example.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.tuenti.voice.example.ProximitySensor;
import com.tuenti.voice.example.R;

public class CallInProgressActivity
    extends Activity
    implements View.OnClickListener
{
    // UI lock flag
    private boolean mUILocked = false;

    private final String TAG = "s-libjingle-webrtc";
    private ProximitySensor mProximitySensor;
    
    // Wake lock
    private PowerManager mPowerManager;

    private WakeLock mWakeLock;

    private int mWakeLockState;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate - CallInProgressActivity");
        setContentView( R.layout.callinprogress );
        mProximitySensor = new ProximitySensor(this);
        initWakeLock();
    }

    @Override
    protected void onStop(){
        super.onStop();
        mProximitySensor.destroy();
        mProximitySensor = null;
        onUnProximity();
        releaseWakeLock();
    }

    public void onProximity(){
        mUILocked = true;
        turnScreenOn(false);
        setWakeLockState(PowerManager.PARTIAL_WAKE_LOCK);
    }

    public void onUnProximity(){
        setWakeLockState(PowerManager.FULL_WAKE_LOCK);
        mUILocked = false;
        turnScreenOn(true);
    }
    
    private void turnScreenOn(boolean on) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= LayoutParams.FLAG_KEEP_SCREEN_ON;
        if ( on ) {
            // less than 0 returns to default behavior.
            params.screenBrightness = -1;
        } else {
            params.screenBrightness = 0;
        }
        getWindow().setAttributes(params);
    }

    /* Wake lock related logic */
    private void initWakeLock(){
        if ( mPowerManager == null ) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
    }

    private void setWakeLockState(int newState){
        if ( mWakeLockState != newState ) {
            if ( mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
            mWakeLockState = newState;
            mWakeLock = mPowerManager.newWakeLock(newState, "In Call wake lock: " + newState);
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock(){
        if ( mWakeLock != null ) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
    /* End wake lock related logic */
    
    @Override
    public void onClick( View view ) {
        if( mUILocked == false ) {
            //handle button clicks
        }
    }
}

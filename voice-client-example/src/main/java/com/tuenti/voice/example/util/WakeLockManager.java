package com.tuenti.voice.example.util;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WakeLockManager {
    
    private PowerManager mPowerManager;

    private WakeLock mWakeLock;

    private int mWakeLockState;
    
    public WakeLockManager(Context context){
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    public void setWakeLockState(int newState) {
        if (mWakeLockState != newState) {
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
            mWakeLockState = newState;
            mWakeLock = mPowerManager.newWakeLock(newState,
                    "In Call wake lock: " + newState);
            mWakeLock.acquire();
        }
    }

    public void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    /* End wake lock related logic */
}

package com.tuenti.voice.example.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class WakeLockManager {

	private final static String LOG_TAG = "WakeLockManager";
	private PowerManager mPowerManager;

	private WakeLock mPartialLock;
	private WakeLock mFullLock;
	private WakeLock mProximityLock;
	private Method mPowerManagerReleaseIntMethod;
	private final static int WAIT_FOR_PROXIMITY_NEGATIVE = 1;
	private final static int WAKE_UP_IMMEDIATELY = 0;

	public WakeLockManager(Context context){
		mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mFullLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
											  | PowerManager.ACQUIRE_CAUSES_WAKEUP
											  | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
		mPartialLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);

		// Check if PROXIMITY_SCREEN_OFF_WAKE_LOCK is implemented, not part of public api.
		try {
			Method method = mPowerManager.getClass().getDeclaredMethod("getSupportedWakeLockFlags");
			int supportedWakeLockFlags = (Integer) method.invoke(mPowerManager);
			Field field = PowerManager.class.getDeclaredField("PROXIMITY_SCREEN_OFF_WAKE_LOCK");
			int proximityScreenOffWakeLock = (Integer) field.get(null);
			if ((supportedWakeLockFlags & proximityScreenOffWakeLock) != 0x0) {
				mProximityLock = mPowerManager.newWakeLock(proximityScreenOffWakeLock, LOG_TAG);
			}
		} catch (Exception e){
			Log.e(LOG_TAG, "Failed to get proximity wake lock.");
		}

		if( mProximityLock != null ){
			try {
				mPowerManagerReleaseIntMethod = mProximityLock.getClass().getDeclaredMethod("release", int.class);
			}catch (Exception e) {
				Log.e(LOG_TAG, "Failed to get release method.");
			}
		}
	}

	public boolean isProximityWakeLockEnabled() {
		return mProximityLock != null;
	}

	public void setProximityWakeLock(boolean enable, boolean screenOnImmediately ){
		if (isProximityWakeLockEnabled()){
			synchronized (mProximityLock){
				if ( enable ){
					if ( !mProximityLock.isHeld() ) {
						mProximityLock.acquire();
					}
				} else {
					if ( mProximityLock.isHeld()){
						if (mPowerManagerReleaseIntMethod != null){
							try {
								int flags = screenOnImmediately ? WAKE_UP_IMMEDIATELY : WAIT_FOR_PROXIMITY_NEGATIVE;
								mPowerManagerReleaseIntMethod.invoke(mProximityLock, flags);
							} catch (Exception e){
								Log.e(LOG_TAG, "Wake lock release failed");
							}
						}
					}
				}
			}
		}
	}

	public void setWakeLock(boolean enabled, boolean screenOn ) {
		synchronized (this ) {
			if ( enabled ) {
				if ( screenOn) {
					if (!mFullLock.isHeld()){
						mFullLock.acquire();
					}
					if (mPartialLock.isHeld()){
						mPartialLock.release();
					}
				} else {
					if (!mPartialLock.isHeld()){
						mPartialLock.acquire();
					}
					if (mFullLock.isHeld()){
						mFullLock.release();
					}
				}
			} else {
				if (mFullLock.isHeld()){
					mFullLock.release();
				}
				if (mPartialLock.isHeld()){
					mPartialLock.release();
				}
			}
		}
	}

	public void releaseWakeLock() {
		setWakeLock(false, false);
		setProximityWakeLock(false, false);
	}
}

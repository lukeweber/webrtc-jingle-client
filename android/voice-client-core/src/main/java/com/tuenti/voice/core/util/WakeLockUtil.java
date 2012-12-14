package com.tuenti.voice.core.util;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class WakeLockUtil
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "WakeLockUtil";

    private PowerManager mPowerManager;

    private boolean mProximityEnabled;

    private PowerManager.WakeLock mProximityWakeLock;

// --------------------------- CONSTRUCTORS ---------------------------

    public WakeLockUtil( final Context context )
    {
        mPowerManager = (PowerManager) context.getSystemService( Context.POWER_SERVICE );
        initProximity();
    }

// -------------------------- OTHER METHODS --------------------------

    public void startProximityLock()
    {
        if ( mProximityWakeLock != null && !mProximityWakeLock.isHeld() )
        {
            Log.d( TAG, "updateProximitySensorMode: acquiring..." );
            mProximityWakeLock.acquire();
        }
    }

    public void stopProximityLock()
    {
        if ( mProximityWakeLock != null && mProximityWakeLock.isHeld() )
        {
            Log.d( TAG, "updateProximitySensorMode: releasing..." );
            mProximityWakeLock.release();
        }
    }

    public boolean supportsProximity()
    {
        return mProximityEnabled;
    }

    private int getFlag( String key )
    {
        try
        {
            Field field = PowerManager.class.getDeclaredField( key );
            if ( field != null )
            {
                return (Integer) field.get( null );
            }
        }
        catch ( Exception e )
        {
            Log.d( TAG, e.getMessage(), e );
        }
        return 0;
    }

    private void initProximity()
    {
        int proximityScreenOffWakeLock = getFlag( "PROXIMITY_SCREEN_OFF_WAKE_LOCK" );
        if ( proximityScreenOffWakeLock > 0 )
        {
            try
            {
                if ( Compatibility.isCompatible( 17 ) )
                {
                    Method method = mPowerManager.getClass().getDeclaredMethod( "isWakeLockLevelSupported", int.class );
                    mProximityEnabled = (Boolean) method.invoke( mPowerManager, proximityScreenOffWakeLock );
                }
                else
                {
                    Method method = mPowerManager.getClass().getDeclaredMethod( "getSupportedWakeLockFlags" );
                    int supportedWakeLockFlags = (Integer) method.invoke( mPowerManager );
                    mProximityEnabled = ( ( supportedWakeLockFlags & proximityScreenOffWakeLock ) != 0x0 );
                }
            }
            catch ( Exception e )
            {
                Log.d( TAG, e.getMessage(), e );
            }

            if ( mProximityEnabled )
            {
                mProximityWakeLock = mPowerManager.newWakeLock( proximityScreenOffWakeLock, TAG );
            }
        }
    }
}

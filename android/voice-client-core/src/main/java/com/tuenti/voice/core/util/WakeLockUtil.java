package com.tuenti.voice.core.util;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.tuenti.voice.core.util.AccelerometerListener.ORIENTATION_HORIZONTAL;
import static com.tuenti.voice.core.util.AccelerometerListener.OrientationListener;

public final class WakeLockUtil
    implements OrientationListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "WakeLockUtil";

    private AccelerometerListener mAccelerometerListener;

    private boolean mProximityLockStarted;

    private PowerManager.WakeLock mProximityWakeLock;

// --------------------------- CONSTRUCTORS ---------------------------

    public WakeLockUtil( final Context context )
    {
        initProximity( context );
        initAccelerometer( context );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OrientationListener ---------------------

    @Override
    public void orientationChanged( int orientation )
    {
        if ( mProximityLockStarted )
        {
            updateProximitySensorMode( orientation != ORIENTATION_HORIZONTAL );
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public void startProximityLock()
    {
        updateProximitySensorMode( true );
        if ( mAccelerometerListener != null )
        {
            mAccelerometerListener.enable( true );
        }

        mProximityLockStarted = true;
    }

    public void stopProximityLock()
    {
        updateProximitySensorMode( false );
        if ( mAccelerometerListener != null )
        {
            mAccelerometerListener.enable( false );
        }
        mProximityLockStarted = false;
    }

    public boolean supportsProximity()
    {
        return ( mProximityWakeLock != null );
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

    private void initAccelerometer( final Context context )
    {
        if ( supportsProximity() )
        {
            mAccelerometerListener = new AccelerometerListener( context, this );
        }
    }

    private void initProximity( final Context context )
    {
        PowerManager mPowerManager = (PowerManager) context.getSystemService( Context.POWER_SERVICE );
        int proximityScreenOffWakeLock = getFlag( "PROXIMITY_SCREEN_OFF_WAKE_LOCK" );
        if ( proximityScreenOffWakeLock > 0 )
        {
            boolean proximityLockSupported = false;
            try
            {
                if ( Compatibility.isCompatible( 17 ) )
                {
                    Method method = mPowerManager.getClass().getDeclaredMethod( "isWakeLockLevelSupported", int.class );
                    proximityLockSupported = (Boolean) method.invoke( mPowerManager, proximityScreenOffWakeLock );
                }
                else
                {
                    Method method = mPowerManager.getClass().getDeclaredMethod( "getSupportedWakeLockFlags" );
                    int supportedWakeLockFlags = (Integer) method.invoke( mPowerManager );
                    proximityLockSupported = ( ( supportedWakeLockFlags & proximityScreenOffWakeLock ) != 0x0 );
                }
            }
            catch ( Exception e )
            {
                Log.d( TAG, e.getMessage(), e );
            }

            if ( proximityLockSupported )
            {
                mProximityWakeLock = mPowerManager.newWakeLock( proximityScreenOffWakeLock, TAG );
            }
        }
    }

    private void updateProximitySensorMode( boolean enabled )
    {
        if ( mProximityWakeLock != null )
        {
            synchronized ( this )
            {
                if ( enabled && !mProximityWakeLock.isHeld() )
                {
                    Log.d( TAG, "updateProximitySensorMode: acquiring..." );
                    mProximityWakeLock.acquire();
                }
                if ( !enabled && mProximityWakeLock.isHeld() )
                {
                    Log.d( TAG, "updateProximitySensorMode: releasing..." );
                    mProximityWakeLock.release();
                }
            }
        }
    }
}
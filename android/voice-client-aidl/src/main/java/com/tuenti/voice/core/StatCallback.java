package com.tuenti.voice.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.service.IStatService;
import com.tuenti.voice.core.service.IStatServiceCallback;

public abstract class StatCallback
    extends IStatServiceCallback.Stub
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "BuddyCallback";

    private Activity mActivity;

    private IStatService mService;

    private boolean mServiceConnected;

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mService = IStatService.Stub.asInterface( service );
                mService.registerCallback( StatCallback.this );
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceConnected", e );
            }
        }

        @Override
        public void onServiceDisconnected( ComponentName name )
        {
            try
            {
                mService.unregisterCallback( StatCallback.this );
                mService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

// --------------------------- CONSTRUCTORS ---------------------------

    public StatCallback( final Activity activity )
    {
        mActivity = activity;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface IStatServiceCallback ---------------------

    @Override
    public void handleStatsUpdate( String stats )
    {
    }

// -------------------------- OTHER METHODS --------------------------

    public void bind()
    {
        if ( !mServiceConnected )
        {
            mServiceConnected = true;
            Intent connectionIntent = new Intent( IStatService.class.getName() );
            mActivity.bindService( connectionIntent, mServiceConnection, Context.BIND_AUTO_CREATE );
        }
    }

    public void unbind()
    {
        if ( mServiceConnected )
        {
            mActivity.unbindService( mServiceConnection );
            mServiceConnected = false;
        }
    }
}

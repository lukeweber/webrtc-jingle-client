package com.tuenti.voice.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.core.service.IConnectionService;
import com.tuenti.voice.core.service.IConnectionServiceCallback;

public abstract class ConnectionCallback
    extends IConnectionServiceCallback.Stub
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "ConnectionCallback";

    private Activity mActivity;

    private IConnectionService mService;

    private boolean mServiceConnected;

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mService = IConnectionService.Stub.asInterface( service );
                mService.registerCallback( ConnectionCallback.this );
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
                mService.unregisterCallback( ConnectionCallback.this );
                mService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

// --------------------------- CONSTRUCTORS ---------------------------

    public ConnectionCallback( final Activity activity )
    {
        mActivity = activity;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface IConnectionServiceCallback ---------------------

    @Override
    public void handleLoggedIn()
    {
    }

    @Override
    public void handleLoggedOut()
    {
    }

    @Override
    public void handleLoggingIn()
    {
    }

// -------------------------- OTHER METHODS --------------------------

    public void bind()
    {
        if ( !mServiceConnected )
        {
            mServiceConnected = true;
            Intent connectionIntent = new Intent( IConnectionService.class.getName() );
            mActivity.bindService( connectionIntent, mServiceConnection, Context.BIND_AUTO_CREATE );
        }
    }

    public void login( Connection connection )
    {
        try
        {
            mService.login( connection );
        }
        catch ( RemoteException e )
        {
            Log.d( TAG, e.getMessage(), e );
        }
    }

    public void logout()
    {
        try
        {
            mService.logout();
        }
        catch ( RemoteException e )
        {
            Log.d( TAG, e.getMessage(), e );
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

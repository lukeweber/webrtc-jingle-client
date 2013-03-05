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

    private IConnectionService mConnectionService;

    private boolean mConnectionServiceConnected;

    private final ServiceConnection mConnectionServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mConnectionService = IConnectionService.Stub.asInterface( service );
                mConnectionService.registerCallback( ConnectionCallback.this );
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
                mConnectionService.unregisterCallback( ConnectionCallback.this );
                mConnectionService = null;
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
        if ( !mConnectionServiceConnected )
        {
            mConnectionServiceConnected = true;
            Intent connectionIntent = new Intent( IConnectionService.class.getName() );
            mActivity.bindService( connectionIntent, mConnectionServiceConnection, Context.BIND_AUTO_CREATE );
        }
    }

    public void login( Connection connection )
    {
        try
        {
            mConnectionService.login( connection );
        }
        catch ( RemoteException e )
        {
            Log.d( TAG, e.getMessage(), e );
        }
    }

    public void unbind()
    {
        if ( mConnectionServiceConnected )
        {
            mActivity.unbindService( mConnectionServiceConnection );
            mConnectionServiceConnected = false;
        }
    }
}

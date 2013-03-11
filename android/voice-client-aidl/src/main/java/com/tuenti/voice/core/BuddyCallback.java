package com.tuenti.voice.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.service.IBuddyService;
import com.tuenti.voice.core.service.IBuddyServiceCallback;

public abstract class BuddyCallback
    extends IBuddyServiceCallback.Stub
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "BuddyCallback";

    private Activity mActivity;

    private IBuddyService mService;

    private boolean mServiceConnected;

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mService = IBuddyService.Stub.asInterface( service );
                mService.registerCallback( BuddyCallback.this );
                BuddyCallback.this.onServiceConnected();
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
                mService.unregisterCallback( BuddyCallback.this );
                mService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

// --------------------------- CONSTRUCTORS ---------------------------

    public BuddyCallback( final Activity activity )
    {
        mActivity = activity;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface IBuddyServiceCallback ---------------------

    @Override
    public void handleBuddyUpdated( Buddy[] buddies )
    {
    }

// -------------------------- OTHER METHODS --------------------------

    public void bind()
    {
        if ( !mServiceConnected )
        {
            mServiceConnected = true;
            Intent connectionIntent = new Intent( IBuddyService.class.getName() );
            mActivity.bindService( connectionIntent, mServiceConnection, Context.BIND_AUTO_CREATE );
        }
    }

    public void onServiceConnected()
    {
    }

    public void requestBuddyUpdate()
    {
        try
        {
            if ( mService != null )
            {
                mService.requestBuddyUpdate();
            }
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
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

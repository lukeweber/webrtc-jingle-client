package com.tuenti.voice.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.core.service.ICallService;
import com.tuenti.voice.core.service.ICallServiceCallback;

public abstract class CallCallback
    extends ICallServiceCallback.Stub
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallCallback";

    private Activity mActivity;

    private ICallService mService;

    private boolean mServiceConnected;

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mService = ICallService.Stub.asInterface( service );
                mService.registerCallback( CallCallback.this );
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
                mService.unregisterCallback( CallCallback.this );
                mService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

// --------------------------- CONSTRUCTORS ---------------------------

    public CallCallback( final Activity activity )
    {
        mActivity = activity;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface ICallServiceCallback ---------------------

    @Override
    public void handleCallInProgress()
    {
    }

    @Override
    public void handleIncomingCall( Call call )
    {
    }

    @Override
    public void handleIncomingCallAccepted()
    {
    }

    @Override
    public void handleIncomingCallTerminated( Call call )
    {
    }

    @Override
    public void handleOutgoingCall( Call call )
    {
    }

    @Override
    public void handleOutgoingCallAccepted()
    {
    }

    @Override
    public void handleOutgoingCallTerminated( Call call )
    {
    }

// -------------------------- OTHER METHODS --------------------------

    public void acceptCall( long callId )
    {
        try
        {
            mService.acceptCall( callId );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    public void bind()
    {
        if ( !mServiceConnected )
        {
            mServiceConnected = true;
            Intent connectionIntent = new Intent( ICallService.class.getName() );
            mActivity.bindService( connectionIntent, mServiceConnection, Context.BIND_AUTO_CREATE );
        }
    }

    public void call( String remoteJid )
    {
        try
        {
            mService.call( remoteJid );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    public void declineCall( long callId, boolean busy )
    {
        try
        {
            mService.declineCall( callId, busy );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    public void endCall( long callId )
    {
        try
        {
            mService.endCall( callId );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    public void toggleHold( long callId )
    {
        try
        {
            mService.toggleHold( callId );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    public void toggleMute( long callId )
    {
        try
        {
            mService.toggleMute( callId );
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

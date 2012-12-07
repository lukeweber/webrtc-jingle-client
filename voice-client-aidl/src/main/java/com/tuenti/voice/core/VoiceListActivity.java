package com.tuenti.voice.core;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.core.service.IBuddyService;
import com.tuenti.voice.core.service.IBuddyServiceCallback;
import com.tuenti.voice.core.service.ICallService;
import com.tuenti.voice.core.service.ICallServiceCallback;
import com.tuenti.voice.core.service.IConnectionService;
import com.tuenti.voice.core.service.IConnectionServiceCallback;

public abstract class VoiceListActivity
    extends ListActivity
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "VoiceListActivity";

    private IBuddyService mBuddyService;

    private final IBuddyServiceCallback.Stub mBuddyServiceCallback = new IBuddyServiceCallback.Stub()
    {
        @Override
        public void handleBuddyUpdated( final Buddy[] buddies )
        {
            onBuddyUpdated( buddies );
        }
    };

    private boolean mBuddyServiceConnected;

    private final ServiceConnection mBuddyServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mBuddyService = IBuddyService.Stub.asInterface( service );
                mBuddyService.registerCallback( mBuddyServiceCallback );
                onRegisterBuddyListener();
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
                mBuddyService.unregisterCallback( mBuddyServiceCallback );
                mBuddyService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

    private ICallService mCallService;

    private ICallServiceCallback.Stub mCallServiceCallback = new ICallServiceCallback.Stub()
    {
        @Override
        public void handleCallInProgress()
        {
            onCallInProgress();
        }

        @Override
        public void handleIncomingCall( Call call )
        {
            onIncomingCall( call );
        }

        @Override
        public void handleIncomingCallAccepted()
        {
            onIncomingCallAccepted();
        }

        @Override
        public void handleIncomingCallTerminated( Call call )
        {
            onIncomingCallTerminated( call );
        }

        @Override
        public void handleOutgoingCall( Call call )
        {
            onOutgoingCall( call );
        }

        @Override
        public void handleOutgoingCallAccepted()
        {
            onOutgoingCallAccepted();
        }

        @Override
        public void handleOutgoingCallTerminated( Call call )
        {
            onOutgoingCallTerminated( call );
        }
    };

    private boolean mCallServiceConnected;

    private final ServiceConnection mCallServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mCallService = ICallService.Stub.asInterface( service );
                mCallService.registerCallback( mCallServiceCallback );
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
                mCallService.unregisterCallback( mCallServiceCallback );
                mCallService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

    private IConnectionService mConnectionService;

    private final IConnectionServiceCallback mConnectionServiceCallback = new IConnectionServiceCallback.Stub()
    {
        @Override
        public void handleLoggingIn()
        {
            onLoggingIn();
        }

        @Override
        public void handleLoggedIn()
        {
            onLoggedIn();
        }

        @Override
        public void handleLoggedOut()
        {
            onLoggedOut();
        }
    };

    private boolean mConnectionServiceConnected;

    private final ServiceConnection mConnectionServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mConnectionService = IConnectionService.Stub.asInterface( service );
                mConnectionService.registerCallback( mConnectionServiceCallback );
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
                mConnectionService.unregisterCallback( mConnectionServiceCallback );
                mConnectionService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

// -------------------------- OTHER METHODS --------------------------

    public void acceptCall( long callId )
    {
        try
        {
            mCallService.acceptCall( callId );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    public void call( String remoteJid )
    {
        try
        {
            mCallService.call( remoteJid );
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
            mCallService.declineCall( callId, busy );
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
            mCallService.endCall( callId );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
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

    public void onBuddyUpdated( Buddy[] buddies )
    {
    }

    public void onCallInProgress()
    {
    }

    public void onIncomingCall( Call call )
    {
    }

    public void onIncomingCallAccepted()
    {
    }

    public void onIncomingCallTerminated( Call call )
    {
    }

    public void onLoggedIn()
    {
    }

    public void onLoggedOut()
    {
    }

    public void onLoggingIn()
    {
    }

    public void onOutgoingCall( Call call )
    {
    }

    public void onOutgoingCallAccepted()
    {
    }

    public void onOutgoingCallTerminated( Call call )
    {
    }

    public void onRegisterBuddyListener()
    {
    }

    public void requestBuddyUpdate()
    {
        try
        {
            mBuddyService.requestBuddyUpdate();
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
            mCallService.toggleHold( callId );
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
            mCallService.toggleMute( callId );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if ( mConnectionServiceConnected && this instanceof OnConnectionListener )
        {
            unbindService( mConnectionServiceConnection );
            mConnectionServiceConnected = false;
        }
        if ( mBuddyServiceConnected && this instanceof OnBuddyListener )
        {
            unbindService( mBuddyServiceConnection );
            mBuddyServiceConnected = false;
        }
        if ( mCallServiceConnected && this instanceof OnCallListener )
        {
            unbindService( mCallServiceConnection );
            mCallServiceConnected = false;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if ( !mConnectionServiceConnected && this instanceof OnConnectionListener )
        {
            mConnectionServiceConnected = true;
            Intent connectionIntent = new Intent( IConnectionService.class.getName() );
            bindService( connectionIntent, mConnectionServiceConnection, Context.BIND_AUTO_CREATE );
        }
        if ( !mBuddyServiceConnected && this instanceof OnBuddyListener )
        {
            mBuddyServiceConnected = true;
            Intent connectionIntent = new Intent( IBuddyService.class.getName() );
            bindService( connectionIntent, mBuddyServiceConnection, Context.BIND_AUTO_CREATE );
        }
        if ( !mCallServiceConnected && this instanceof OnCallListener )
        {
            mCallServiceConnected = true;
            Intent connectionIntent = new Intent( ICallService.class.getName() );
            bindService( connectionIntent, mCallServiceConnection, Context.BIND_AUTO_CREATE );
        }
    }
}

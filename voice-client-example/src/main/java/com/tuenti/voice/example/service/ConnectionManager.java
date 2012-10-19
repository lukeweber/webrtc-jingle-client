package com.tuenti.voice.example.service;

import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.ConnectionListener;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.example.data.User;
import com.tuenti.voice.example.util.ConnectionMonitor;
import com.tuenti.voice.example.util.IConnectionMonitor;

public class ConnectionManager
    implements ConnectionListener, IConnectionMonitor
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "ConnectionManager";

    private final IConnectionService.Stub mBinder = new IConnectionService.Stub()
    {
        @Override
        public void login( User user )
        {
            receiveLogin( user );
        }

        @Override
        public void logout()
        {
            receiveLogout();
        }

        @Override
        public void registerCallback( IConnectionServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.register( cb );
            }
        }

        @Override
        public void unregisterCallback( IConnectionServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.unregister( cb );
            }
        }
    };

    private final RemoteCallbackList<IConnectionServiceCallback> mCallbacks =
        new RemoteCallbackList<IConnectionServiceCallback>();

    private VoiceClient mClient;

    private boolean mClientInited;

    private boolean mReconnect;

    private boolean mReconnectTimerRunning;

    private User mUser;

    private int mXmppState;

// --------------------------- CONSTRUCTORS ---------------------------

    public ConnectionManager( VoiceClient client )
    {
        mClient = client;
        mClient.addConnectionListener( this );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface ConnectionListener ---------------------

    @Override
    public void handleXmppError( int error )
    {
        switch ( XmppError.fromInteger( error ) )
        {
            case XML:
                Log.e( TAG, "Malformed XML or encoding error" );
                break;
            case STREAM:
                Log.e( TAG, "XMPP stream error" );
                break;
            case VERSION:
                Log.e( TAG, "XMPP version error" );
                break;
            case UNAUTHORIZED:
                Log.e( TAG, "User is not authorized (Check your username and password)" );
                break;
            case TLS:
                Log.e( TAG, "TLS could not be negotiated" );
                break;
            case AUTH:
                Log.e( TAG, "Authentication could not be negotiated" );
                break;
            case BIND:
                Log.e( TAG, "Resource or session binding could not be negotiated" );
                break;
            case CONNECTION_CLOSED:
                Log.e( TAG, "Connection closed by output handler." );
                break;
            case DOCUMENT_CLOSED:
                Log.e( TAG, "Closed by </stream:stream>" );
                break;
            case SOCKET:
                Log.e( TAG, "Socket error" );
                break;
        }
    }

    @Override
    public void handleXmppSocketClose( int state )
    {
        sendLoggedOut();
    }

    @Override
    public void handleXmppStateChanged( int state )
    {
        mXmppState = state;
        switch ( XmppState.fromInteger( state ) )
        {
            case NONE:
                sendConnectionReady();
            case OPEN:
                sendLoggedIn();
                break;
            case CLOSED:
                sendLoggedOut();
                break;
        }
    }

// --------------------- Interface IConnectionMonitor ---------------------

    @Override
    public void onConnectionEstablished()
    {
        if ( mReconnect && XmppState.fromInteger( mXmppState ) == XmppState.CLOSED )
        {
            internalLogin();
        }
    }

    @Override
    public void onConnectionLost()
    {
    }

    @Override
    public void onConnectivityLost()
    {
        stopReconnectTimer();
        //Could blank out voip icons as being available.
        Log.i( TAG, "Connectivity lost" );
    }

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind()
    {
        return mBinder;
    }

    private void internalLogin()
    {
        if ( mClientInited )
        {
            mClient.login( mUser.getUsername(),
                           mUser.getPassword(),
                           mUser.getTurnPassword(),
                           mUser.getXmppHost(),
                           mUser.getXmppPort(),
                           mUser.getXmppUseSsl() );
        }
        else
        {
            // We run login after xmpp_none event, meaning our client is
            // initialized
            mClient.init( mUser.getStunHost(),
                          mUser.getRelayHost(),
                          mUser.getRelayHost(),
                          mUser.getRelayHost(),
                          mUser.getTurnHost() );
        }
    }

    private void receiveLogin( User user )
    {
        mUser = user;
        if ( mUser != null )
        {
            internalLogin();
        }
    }

    private void receiveLogout()
    {
        mClient.logout();
    }

    private void sendConnectionReady()
    {
        mClientInited = true;
        if ( mUser != null )
        {
            internalLogin();
        }

        // handleConnectionReady
    }

    private void sendLoggedIn()
    {
        stopReconnectTimer();

        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleLoggedIn();
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void sendLoggedOut()
    {
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleLoggedOut();
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();

        if ( ConnectionMonitor.isOnline() && mReconnect )
        {
            startReconnectTimer();
        }
        else
        {
            stopReconnectTimer();
        }
    }

    private void startReconnectTimer()
    {
        // Reconnect in 10 seconds if there isn't already one running.
        if ( !mReconnectTimerRunning )
        {
            mReconnectTimerRunning = true;
        }
    }

    private void stopReconnectTimer()
    {
        mReconnectTimerRunning = false;
    }
}

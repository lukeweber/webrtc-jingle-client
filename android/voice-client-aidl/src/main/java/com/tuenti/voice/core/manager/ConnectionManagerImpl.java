package com.tuenti.voice.core.manager;

import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.core.service.IConnectionService;
import com.tuenti.voice.core.service.IConnectionServiceCallback;

public class ConnectionManagerImpl
    implements ConnectionManager
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "ConnectionManager";

    private final IConnectionService.Stub mBinder = new IConnectionService.Stub()
    {
        @Override
        public void login( Connection connection )
        {
            handleLogin( connection );
        }

        @Override
        public void logout()
        {
            handleLogout();
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

// --------------------------- CONSTRUCTORS ---------------------------

    public ConnectionManagerImpl( VoiceClient client )
    {
        mClient = client;
        mClient.setConnectionManager( this );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface ConnectionManager ---------------------

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
            default:
                break;
        }
    }

    @Override
    public void handleXmppSocketClose( int state )
    {
        handleLoggedOut();
    }

    @Override
    public void handleXmppStateChanged( int state )
    {
        switch ( XmppState.fromInteger( state ) )
        {
            case NONE:
                handleConnectionReady();
                break;
            case OPENING:
                handleLoggingIn();
                break;
            case OPEN:
                handleLoggedIn();
                break;
            case CLOSED:
                handleLoggedOut();
                break;
            default:
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind()
    {
        return mBinder;
    }

    private void handleConnectionReady()
    {
        Log.d( TAG, "handleConnectionReady" );
    }

    private void handleLoggedIn()
    {
        Log.d( TAG, "handleLoggedIn" );

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

    private void handleLoggedOut()
    {
        Log.d( TAG, "handleLoggedOut" );

        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleLoggedOut();
            }
            catch ( RemoteException e )
            {
                // NOOP
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void handleLoggingIn()
    {
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleLoggingIn();
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void handleLogin( Connection connection )
    {
        Log.d( TAG, "handleLogin" );
        if ( connection != null )
        {
            mClient.login( connection.getUsername(),
                           connection.getPassword(),
                           connection.getStunHost(),
                           connection.getTurnHost(),
                           connection.getTurnUsername(),
                           connection.getTurnPassword(),
                           connection.getXmppHost(),
                           connection.getXmppPort(),
                           connection.getXmppUseSsl() );
        }
    }

    private void handleLogout()
    {
        Log.d( TAG, "handleLogout" );
        mClient.logout();
    }
}

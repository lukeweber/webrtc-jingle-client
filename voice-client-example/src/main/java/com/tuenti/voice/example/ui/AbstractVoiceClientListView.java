package com.tuenti.voice.example.ui;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.example.data.Buddy;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.service.ICallService;
import com.tuenti.voice.example.service.ICallServiceCallback;
import com.tuenti.voice.example.service.IConnectionService;
import com.tuenti.voice.example.service.IConnectionServiceCallback;
import com.tuenti.voice.example.service.IRosterService;
import com.tuenti.voice.example.service.IRosterServiceCallback;

public abstract class AbstractVoiceClientListView
    extends ListActivity
{
// ------------------------------ FIELDS ------------------------------

    protected static final String TAG = "AbstractVoiceClientListView";

    private boolean mBindCall;

    private boolean mBindConnection;

    private boolean mBindRoster;

    private ICallService mCallService;

    private final ICallServiceCallback.Stub mCallServiceCallback = new ICallServiceCallback.Stub()
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
        public void handleIncomingCallTerminated()
        {
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
        public void handleOutgoingCallTerminated()
        {
            onOutgoingCallTerminated();
        }
    };

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

    private IRosterService mRosterService;

    private final IRosterServiceCallback.Stub mRosterServiceCallback = new IRosterServiceCallback.Stub()
    {
        @Override
        public void handleRosterUpdated( final Buddy[] buddies )
        {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    onRosterUpdated( buddies );
                }
            } );
        }
    };

    private final ServiceConnection mRosterServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mRosterService = IRosterService.Stub.asInterface( service );
                mRosterService.registerCallback( mRosterServiceCallback );
                onRosterServiceConnected();
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
                mRosterService.unregisterCallback( mRosterServiceCallback );
                mRosterService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

// --------------------------- CONSTRUCTORS ---------------------------

    public AbstractVoiceClientListView( boolean bindConnection, boolean bindRoster, boolean bindCall )
    {
        mBindConnection = bindConnection;
        mBindRoster = bindRoster;
        mBindCall = bindCall;
    }

    protected ICallService getCallService()
    {
        return mCallService;
    }

    protected IConnectionService getConnectionService()
    {
        return mConnectionService;
    }

    protected IRosterService getRosterService()
    {
        return mRosterService;
    }

    protected void onCallInProgress()
    {
    }

    protected void onIncomingCall( Call call )
    {
    }

    protected void onIncomingCallAccepted()
    {
    }

    protected void onLoggedIn()
    {
    }

    protected void onLoggedOut()
    {
    }

    protected void onLoggingIn()
    {
    }

    protected void onOutgoingCall( Call call )
    {
    }

    protected void onOutgoingCallAccepted()
    {
    }

    protected void onOutgoingCallTerminated()
    {
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // unbind service
        if ( mBindCall )
        {
            unbindService( mCallServiceConnection );
        }
        if ( mBindRoster )
        {
            unbindService( mRosterServiceConnection );
        }
        if ( mBindConnection )
        {
            unbindService( mConnectionServiceConnection );
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // bind service
        if ( mBindConnection )
        {
            Intent connectionIntent = new Intent( IConnectionService.class.getName() );
            bindService( connectionIntent, mConnectionServiceConnection, Context.BIND_AUTO_CREATE );
        }
        if ( mBindRoster )
        {
            Intent rosterIntent = new Intent( IRosterService.class.getName() );
            bindService( rosterIntent, mRosterServiceConnection, Context.BIND_AUTO_CREATE );
        }
        if ( mBindCall )
        {
            Intent callIntent = new Intent( ICallService.class.getName() );
            bindService( callIntent, mCallServiceConnection, Context.BIND_AUTO_CREATE );
        }
    }

    protected void onRosterServiceConnected()
    {
    }

    protected void onRosterUpdated( Buddy[] buddies )
    {
    }
}


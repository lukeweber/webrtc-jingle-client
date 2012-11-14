package com.tuenti.voice.core.manager.aidl;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.OnConnectionListener;
import com.tuenti.voice.core.data.User;
import com.tuenti.voice.core.service.IConnectionService;
import com.tuenti.voice.core.service.IConnectionServiceCallback;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareMixin;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ConnectionManagerCallback
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "ConnectionManagerCallback";

    private IConnectionService mConnectionService;

    private final IConnectionServiceCallback mConnectionServiceCallback = new IConnectionServiceCallback.Stub()
    {
        @Override
        public void handleLoggingIn()
        {
            mOnConnectionListener.onLoggingIn();
        }

        @Override
        public void handleLoggedIn()
        {
            mOnConnectionListener.onLoggedIn();
        }

        @Override
        public void handleLoggedOut()
        {
            mOnConnectionListener.onLoggedOut();
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

    private OnConnectionListener mOnConnectionListener;

// -------------------------- OTHER METHODS --------------------------

    @Pointcut(value = "within(@com.tuenti.voice.core.annotations.ConnectionListener *)")
    public void activityConnectionAnnotated()
    {
    }

    @DeclareMixin(value = "(@com.tuenti.voice.core.annotations.ConnectionListener *)")
    @SuppressWarnings("UnusedDeclaration")
    public OnConnectionListener createImplementation()
    {
        return new OnConnectionListenerImpl();
    }

    @Before(value = "execution(* android.app.Activity.onPause(..)) && activityConnectionAnnotated()")
    public void onPause()
    {
        if ( mConnectionServiceConnected )
        {
            ( (Activity) mOnConnectionListener ).unbindService( mConnectionServiceConnection );
            mConnectionServiceConnected = false;
        }
    }

    @Before(value = "execution(* android.app.Activity.onResume(..)) && activityConnectionAnnotated() && this(listener)",
            argNames = "listener")
    public void onResume( OnConnectionListener listener )
    {
        Log.d( TAG, "onResume" );

        mOnConnectionListener = listener;
        if ( !mConnectionServiceConnected )
        {
            mConnectionServiceConnected = true;
            Intent connectionIntent = new Intent( IConnectionService.class.getName() );
            ( (Activity) mOnConnectionListener ).bindService( connectionIntent,
                                                              mConnectionServiceConnection,
                                                              Context.BIND_AUTO_CREATE );
        }
    }

// -------------------------- INNER CLASSES --------------------------

    public class OnConnectionListenerImpl
        implements OnConnectionListener
    {
        @Override
        public void login( User user )
        {
            try
            {
                mConnectionService.login( user );
            }
            catch ( RemoteException e )
            {
                Log.d( TAG, e.getMessage(), e );
            }
        }

        @Override
        public void onLoggedIn()
        {
        }

        @Override
        public void onLoggedOut()
        {
        }

        @Override
        public void onLoggingIn()
        {
        }
    }
}

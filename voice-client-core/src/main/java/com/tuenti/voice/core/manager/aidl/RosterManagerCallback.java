package com.tuenti.voice.core.manager.aidl;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.OnRosterListener;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.service.IRosterService;
import com.tuenti.voice.core.service.IRosterServiceCallback;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareMixin;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class RosterManagerCallback
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "RosterManagerCallback";

    private OnRosterListener mOnRosterListener;

    private IRosterService mRosterService;

    private final IRosterServiceCallback.Stub mRosterServiceCallback = new IRosterServiceCallback.Stub()
    {
        @Override
        public void handleRosterUpdated( final Buddy[] buddies )
        {
            mOnRosterListener.onRosterUpdated( buddies );
        }
    };

    private boolean mRosterServiceConnected;

    private final ServiceConnection mRosterServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mRosterService = IRosterService.Stub.asInterface( service );
                mRosterService.registerCallback( mRosterServiceCallback );
                mOnRosterListener.onRegisterOnRosterListener();
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

// -------------------------- OTHER METHODS --------------------------

    @Pointcut(value = "within(@com.tuenti.voice.core.annotations.RosterListener *)")
    public void activityRosterAnnotated()
    {
    }

    @DeclareMixin(value = "(@com.tuenti.voice.core.annotations.RosterListener *)")
    @SuppressWarnings("UnusedDeclaration")
    public OnRosterListener createImplementation()
    {
        return new OnRosterListenerImpl();
    }

    @Before(value = "execution(* android.app.Activity.onPause(..)) && activityRosterAnnotated()")
    public void onPause()
    {
        if ( mRosterServiceConnected )
        {
            ( (Activity) mOnRosterListener ).unbindService( mRosterServiceConnection );
            mRosterServiceConnected = false;
        }
    }

    @Before(value = "execution(* android.app.Activity.onResume(..)) && activityRosterAnnotated() && this(listener)",
            argNames = "listener")
    public void onResume( OnRosterListener listener )
    {
        Log.d( TAG, "onResume" );

        mOnRosterListener = listener;
        if ( !mRosterServiceConnected )
        {
            mRosterServiceConnected = true;
            Intent connectionIntent = new Intent( IRosterService.class.getName() );
            ( (Activity) mOnRosterListener ).bindService( connectionIntent,
                                                          mRosterServiceConnection,
                                                          Context.BIND_AUTO_CREATE );
        }
    }

// -------------------------- INNER CLASSES --------------------------

    public class OnRosterListenerImpl
        implements OnRosterListener
    {
        @Override
        public void onRegisterOnRosterListener()
        {
        }

        @Override
        public void onRosterUpdated( Buddy[] buddies )
        {
        }

        @Override
        public void requestRosterUpdate()
        {
            try
            {
                mRosterService.requestRosterUpdate();
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, e.getMessage(), e );
            }
        }
    }
}

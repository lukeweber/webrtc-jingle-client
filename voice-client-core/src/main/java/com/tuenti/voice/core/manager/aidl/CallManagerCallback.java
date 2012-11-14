package com.tuenti.voice.core.manager.aidl;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.OnCallListener;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.core.service.ICallService;
import com.tuenti.voice.core.service.ICallServiceCallback;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareMixin;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class CallManagerCallback
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallManagerCallback";

    private ICallService mCallService;

    private ICallServiceCallback.Stub mCallServiceCallback = new ICallServiceCallback.Stub()
    {
        @Override
        public void handleCallInProgress()
        {
            mOnCallListener.onCallInProgress();
        }

        @Override
        public void handleIncomingCall( Call call )
        {
            mOnCallListener.onIncomingCall( call );
        }

        @Override
        public void handleIncomingCallAccepted()
        {
            mOnCallListener.onIncomingCallAccepted();
        }

        @Override
        public void handleIncomingCallTerminated()
        {
            mOnCallListener.onIncomingCallTerminated();
        }

        @Override
        public void handleOutgoingCall( Call call )
        {
            mOnCallListener.onOutgoingCall( call );
        }

        @Override
        public void handleOutgoingCallAccepted()
        {
            mOnCallListener.onOutgoingCallAccepted();
        }

        @Override
        public void handleOutgoingCallTerminated()
        {
            mOnCallListener.onOutgoingCallTerminated();
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

    private OnCallListener mOnCallListener;

// -------------------------- OTHER METHODS --------------------------

    @Pointcut(value = "within(@com.tuenti.voice.core.annotations.CallListener *)")
    public void activityConnectionAnnotated()
    {
    }

    @DeclareMixin(value = "(@com.tuenti.voice.core.annotations.CallListener *)")
    @SuppressWarnings("UnusedDeclaration")
    public OnCallListener createImplementation()
    {
        return new OnCallListenerImpl();
    }

    @Before(value = "execution(* android.app.Activity.onPause(..)) && activityConnectionAnnotated()")
    public void onPause()
    {
        if ( mCallServiceConnected )
        {
            ( (Activity) mOnCallListener ).unbindService( mCallServiceConnection );
            mCallServiceConnected = false;
        }
    }

    @Before(value = "execution(* android.app.Activity.onResume(..)) && activityConnectionAnnotated() && this(listener)",
            argNames = "listener")
    public void onResume( OnCallListener listener )
    {
        Log.d( TAG, "onResume" );

        mOnCallListener = listener;
        if ( !mCallServiceConnected )
        {
            mCallServiceConnected = true;
            Intent connectionIntent = new Intent( ICallService.class.getName() );
            ( (Activity) mOnCallListener ).bindService( connectionIntent,
                                                        mCallServiceConnection,
                                                        Context.BIND_AUTO_CREATE );
        }
    }

// -------------------------- INNER CLASSES --------------------------

    public class OnCallListenerImpl
        implements OnCallListener
    {
        @Override
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

        @Override
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

        @Override
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

        @Override
        public void onCallInProgress()
        {
        }

        @Override
        public void onIncomingCall( Call call )
        {
        }

        @Override
        public void onIncomingCallAccepted()
        {
        }

        @Override
        public void onIncomingCallTerminated()
        {
        }

        @Override
        public void onOutgoingCall( Call call )
        {
        }

        @Override
        public void onOutgoingCallAccepted()
        {
        }

        @Override
        public void onOutgoingCallTerminated()
        {
        }

        @Override
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

        @Override
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
    }
}

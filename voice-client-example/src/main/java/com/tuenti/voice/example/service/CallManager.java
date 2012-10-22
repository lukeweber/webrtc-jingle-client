package com.tuenti.voice.example.service;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.CallError;
import com.tuenti.voice.core.CallListener;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.util.CallNotification;
import com.tuenti.voice.example.util.ConnectionMonitor;
import com.tuenti.voice.example.util.NetworkPreference;
import com.tuenti.voice.example.util.RingManager;

import java.util.LinkedHashMap;

public class CallManager
    implements CallListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallManager";

    private AudioManager mAudioManager;

    private final ICallService.Stub mBinder = new ICallService.Stub()
    {
        @Override
        public void acceptCall( long callId )
        {
        }

        @Override
        public void call( String remoteJid )
        {
            handleCall( remoteJid );
        }

        @Override
        public void declineCall( long callId, boolean busy )
        {
        }

        @Override
        public void endCall( long callId )
        {
        }

        @Override
        public void toggleMute( long callId )
        {
            handleToggleMute( callId );
        }

        @Override
        public void toggleHold( long callId )
        {
            handleToggleHold( callId );
        }

        @Override
        public void registerCallback( ICallServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.register( cb );
            }
        }

        @Override
        public void unregisterCallback( ICallServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.unregister( cb );
            }
        }
    };

    private boolean mCallInProgress;

    private LinkedHashMap<Long, Call> mCallMap = new LinkedHashMap<Long, Call>();

    private final RemoteCallbackList<ICallServiceCallback> mCallbacks = new RemoteCallbackList<ICallServiceCallback>();

    private final VoiceClient mClient;

    private final Context mContext;

    private long mCurrentCallId;

    private NetworkPreference mNetworkPreference;

    private CallNotification mNotificationManager;

    private RingManager mRingManager;

// --------------------------- CONSTRUCTORS ---------------------------

    public CallManager( VoiceClient client, Context context )
    {
        mClient = client;
        mClient.addCallListener( this );

        mContext = context;

        mAudioManager = (AudioManager) context.getSystemService( Context.AUDIO_SERVICE );
        mNetworkPreference = new NetworkPreference( context );
        mNotificationManager = new CallNotification( context );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface CallListener ---------------------

    @Override
    public void handleCallError( int error, long callId )
    {
        switch ( CallError.fromInteger( error ) )
        {
            case ERROR_NONE:
                // no error
            case ERROR_TIME:
                // no response to signaling
            case ERROR_RESPONSE:
                // error during signaling
            case ERROR_NETWORK:
                // network error, could not allocate network resources
            case ERROR_CONTENT:
                // channel errors in SetLocalContent/SetRemoteContent
            case ERROR_TRANSPORT:
                // transport error of some kind
            case ERROR_ACK_TIME:
                // no ack response to signaling, client not available
        }
        Log.e( TAG, "call error ------------------, callid " + callId + "error " + error );
    }

    /**
     * Handle callbacks from the VoiceClient object
     *
     * @param state     CallState
     * @param remoteJid Remote JID
     * @param callId    Remote call ID
     */
    @Override
    public void handleCallStateChanged( int state, String remoteJid, long callId )
    {
        Log.d( TAG, state + " | " + remoteJid + " | " + callId );
        remoteJid = cleanJid( remoteJid );
        switch ( CallState.fromInteger( state ) )
        {
            case SENT_INITIATE:
                handleOutgoingCall( callId, remoteJid );
                break;
            case RECEIVED_INITIATE:
                handleIncomingCall( callId, remoteJid );
                break;
            case RECEIVED_TERMINATE:
                handleOutgoingCallTerminated( callId );
            case RECEIVED_REJECT:
                Log.i( TAG, "Call ended" );
                endCall( callId, 0 );// Add reason to end call.
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind()
    {
        return mBinder;
    }

    private String cleanJid( String jid )
    {
        if ( jid == null )
        {
            return "";
        }

        int index = jid.indexOf( '/' );
        if ( index > 0 )
        {
            return jid.substring( 0, index );
        }
        return jid;
    }

    private void endCall( long callId, int reason )
    {
        // Makes sure we don't change state for calls
        // we decline as busy while in a call.
        if ( mCallMap.containsKey( callId ) )
        {
            mCallInProgress = false;
            mCurrentCallId = 0;
            mNetworkPreference.unsetNetworkPreference();
            Call call = mCallMap.get( Long.valueOf( callId ) );
            mCallMap.remove( callId );
            stopRing();
            resetAudio();

            //dispatchCallState( CallUIIntent.CALL_ENDED, callId, call.getRemoteJid() );

            // Cancel notification
            mNotificationManager.cancelCallNotification();
        }
    }

    private Call getCurrentCall()
    {
        return mCallMap.get( mCurrentCallId );
    }

    private void handleCall( String remoteJid )
    {
        if ( ConnectionMonitor.hasSlowConnection() )
        {
            //Throw warning to user.
        }
        else
        {
            mClient.call( remoteJid );
        }
    }

    private void handleIncomingCall( long callId, String remoteJid )
    {
        if ( ConnectionMonitor.hasSlowConnection() )
        {
            //Warn that you can't accept the incoming call
            //TODO(Luke): Notification of missed call./Explanation about bad connection
            Log.i( TAG, "Declining call because of slow connection" );
            mClient.declineCall( callId, true );
        }
        else if ( mCallInProgress || ConnectionMonitor.getInstance( mContext ).isCallInProgress() )
        {
            //TODO(Luke): Notification of missed call.
            Log.i( TAG, "Declining call because call in progress" );
            mClient.declineCall( callId, true );
        }
        else
        {
            initCallState( callId, remoteJid );
            startRing( true, false );

            // show alert pop up for incoming call + tray notification
            // startIncomingCallDialog( callId, remoteJid );
            // sendIncomingCallNotification( remoteJid );
        }
    }

    private void handleOutgoingCall( long callId, String remoteJid )
    {
        initCallState( callId, remoteJid );
        startRing( false, false );

        // dispatch the callback
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleOutgoingCall( getCurrentCall() );
            }
            catch ( RemoteException e )
            {
                //NOOP
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void handleOutgoingCallTerminated( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        Log.d( TAG, "handleOutgoingCallTerminated: " + callId );
        mCallMap.remove( callId );
        stopRing();
        resetAudio();

        // dispatch the callback
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleOutgoingCallTerminated();
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void handleToggleHold( long callId )
    {
        Call call = mCallMap.get( Long.valueOf( callId ) );
        if ( call != null )
        {
            call.setHold( !call.isHold() );
            mClient.holdCall( callId, call.isHold() );
        }
    }

    private void handleToggleMute( long callId )
    {
        Call call = mCallMap.get( Long.valueOf( callId ) );
        if ( call != null )
        {
            call.setMute( !call.isMute() );
            mClient.muteCall( callId, call.isMute() );
        }
    }

    private void initCallState( long callId, String remoteJid )
    {
        mNetworkPreference.enableStickyNetworkPreference();
        mCallInProgress = true;
        mCurrentCallId = callId;
        mCallMap.put( callId, new Call( callId, remoteJid ) );
    }

    private void resetAudio()
    {
        mAudioManager.setMode( AudioManager.MODE_NORMAL );
        mAudioManager.abandonAudioFocus( null );
    }

    private void setAudioForCall()
    {
        mAudioManager.setMode( ( Build.VERSION.SDK_INT < 11 )
                                   ? AudioManager.MODE_NORMAL
                                   : AudioManager.MODE_IN_COMMUNICATION );
        mAudioManager.requestAudioFocus( null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
    }

    private void startRing( boolean isIncoming, boolean callInProgress )
    {
        stopRing();
        mRingManager = new RingManager( mContext, isIncoming, callInProgress );
    }

    private void stopRing()
    {
        if ( mRingManager != null )
        {
            mRingManager.stop();
            mRingManager = null;
        }
    }
}

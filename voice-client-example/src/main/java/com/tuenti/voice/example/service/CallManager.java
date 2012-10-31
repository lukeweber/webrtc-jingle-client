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
            handleAcceptCall( callId );
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
            handleEndCall( callId );
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

    private LinkedHashMap<Long, Call> mCallMap = new LinkedHashMap<Long, Call>();

    private final RemoteCallbackList<ICallServiceCallback> mCallbacks = new RemoteCallbackList<ICallServiceCallback>();

    private final VoiceClient mClient;

    private final Context mContext;

    private Call mCurrentCall;

    private NetworkPreference mNetworkPreference;

    private RingManager mRingManager;

// --------------------------- CONSTRUCTORS ---------------------------

    public CallManager( VoiceClient client, Context context )
    {
        mClient = client;
        mClient.addCallListener( this );

        mContext = context;

        mAudioManager = (AudioManager) context.getSystemService( Context.AUDIO_SERVICE );
        mNetworkPreference = new NetworkPreference( context );
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
        remoteJid = cleanJid( remoteJid );
        switch ( CallState.fromInteger( state ) )
        {
            case RECEIVED_INITIATE:
                handleIncomingCall( callId, remoteJid );
                break;
            case RECEIVED_ACCEPT:
                handleOutgoingCallAccepted( callId );
                break;
            case RECEIVED_TERMINATE:
                handleIncomingCallTerminated( callId );
                break;
            case SENT_INITIATE:
                handleOutgoingCall( callId, remoteJid );
                break;
            case SENT_ACCEPT:
                handleIncomingCallAccepted( callId );
                break;
            case SENT_TERMINATE:
                handleOutgoingCallTerminated( callId );
                break;
            case IN_PROGRESS:
                handleCallInProgress();
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

    private void dispatchCallback( CallState state, Call call )
    {
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            ICallServiceCallback callback = mCallbacks.getBroadcastItem( i );
            try
            {
                switch ( state )
                {
                    case RECEIVED_ACCEPT:
                        callback.handleOutgoingCallAccepted();
                        break;
                    case RECEIVED_INITIATE:
                        callback.handleIncomingCall( call );
                        break;
                    case RECEIVED_TERMINATE:
                        callback.handleIncomingCallTerminated();
                        break;
                    case SENT_ACCEPT:
                        callback.handleIncomingCallAccepted();
                        break;
                    case SENT_INITIATE:
                        callback.handleOutgoingCall( call );
                        break;
                    case SENT_TERMINATE:
                        callback.handleOutgoingCallTerminated();
                        break;
                    case IN_PROGRESS:
                        callback.handleCallInProgress();
                        break;
                }
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private void handleAcceptCall( long callId )
    {
        mClient.acceptCall( callId );
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

    private void handleCallInProgress()
    {
        dispatchCallback( CallState.IN_PROGRESS, null );
    }

    private void handleEndCall( long callId )
    {
        mClient.endCall( callId );
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
        else if ( mCurrentCall != null || ConnectionMonitor.getInstance( mContext ).isCallInProgress() )
        {
            //TODO(Luke): Notification of missed call.
            Log.i( TAG, "Declining call because call in progress" );
            mClient.declineCall( callId, true );
        }
        else
        {
            initCallState( callId, remoteJid, true );
            dispatchCallback( CallState.RECEIVED_INITIATE, mCurrentCall );
            startRing( true, false );
        }
    }

    private void handleIncomingCallAccepted( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        mCurrentCall = mCallMap.get( callId );
        dispatchCallback( CallState.SENT_ACCEPT, null );
        stopRing();
        setAudioForCall();
    }

    private void handleIncomingCallTerminated( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        Log.d( TAG, "handleIncomingCallTerminated: " + callId );
        stopCall( callId );
        dispatchCallback( CallState.RECEIVED_TERMINATE, null );
    }

    private void handleOutgoingCall( long callId, String remoteJid )
    {
        initCallState( callId, remoteJid, false );
        dispatchCallback( CallState.SENT_INITIATE, mCurrentCall );
        startRing( false, false );
    }

    private void handleOutgoingCallAccepted( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        mCurrentCall = mCallMap.get( callId );
        dispatchCallback( CallState.RECEIVED_ACCEPT, null );
        stopRing();
        setAudioForCall();
    }

    private void handleOutgoingCallTerminated( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        Log.d( TAG, "handleOutgoingCallTerminated: " + callId );
        stopCall( callId );
        dispatchCallback( CallState.SENT_TERMINATE, null );
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

    private void initCallState( long callId, String remoteJid, boolean incoming )
    {
        mNetworkPreference.enableStickyNetworkPreference();

        mCurrentCall = new Call( callId, remoteJid, incoming );
        mCallMap.put( callId, mCurrentCall );
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

    private void stopCall( long callId )
    {
        // remove the call
        mCallMap.remove( callId );

        // reset current call ID
        if ( mCurrentCall.getCallId() == callId )
        {
            mCurrentCall = null;
        }

        // stop ringing
        stopRing();

        // reset the audio
        resetAudio();
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

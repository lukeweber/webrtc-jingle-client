package com.tuenti.voice.core.manager;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.CallError;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.core.service.ICallService;
import com.tuenti.voice.core.service.ICallServiceCallback;

import java.util.LinkedHashMap;

public class CallManagerImpl
    implements CallManager
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallManagerImpl";

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
            handleDeclineCall( callId, busy );
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

    private Call mCurrentCall;

// --------------------------- CONSTRUCTORS ---------------------------

    public CallManagerImpl( VoiceClient client, Context context )
    {
        mClient = client;
        mClient.setCallManager( this );

        mAudioManager = (AudioManager) context.getSystemService( Context.AUDIO_SERVICE );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface CallManager ---------------------

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
            default:
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
                        callback.handleIncomingCallTerminated( call );
                        break;
                    case SENT_ACCEPT:
                        callback.handleIncomingCallAccepted();
                        break;
                    case SENT_INITIATE:
                        callback.handleOutgoingCall( call );
                        break;
                    case SENT_TERMINATE:
                        callback.handleOutgoingCallTerminated( call );
                        break;
                    case IN_PROGRESS:
                        callback.handleCallInProgress();
                        break;
                    default:
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
        mClient.call( remoteJid );
    }

    private void handleCallInProgress()
    {
        dispatchCallback( CallState.IN_PROGRESS, null );
    }

    private void handleDeclineCall( long callId, boolean busy )
    {
        mClient.declineCall( callId, busy );
    }

    private void handleEndCall( long callId )
    {
        mClient.endCall( callId );
    }

    private void handleIncomingCall( long callId, String remoteJid )
    {
        initCallState( callId, remoteJid, true );
        dispatchCallback( CallState.RECEIVED_INITIATE, mCurrentCall );
    }

    private void handleIncomingCallAccepted( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        mCurrentCall = mCallMap.get( callId );
        dispatchCallback( CallState.SENT_ACCEPT, null );
        setAudioForCall();
    }

    private void handleIncomingCallTerminated( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        Log.d( TAG, "handleIncomingCallTerminated: " + callId );
        dispatchCallback( CallState.RECEIVED_TERMINATE, stopCall( callId ) );
    }

    private void handleOutgoingCall( long callId, String remoteJid )
    {
        initCallState( callId, remoteJid, false );
        dispatchCallback( CallState.SENT_INITIATE, mCurrentCall );
    }

    private void handleOutgoingCallAccepted( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        mCurrentCall = mCallMap.get( callId );
        dispatchCallback( CallState.RECEIVED_ACCEPT, null );
        setAudioForCall();
    }

    private void handleOutgoingCallTerminated( long callId )
    {
        if ( !mCallMap.containsKey( callId ) )
        {
            return;
        }

        Log.d( TAG, "handleOutgoingCallTerminated: " + callId );
        dispatchCallback( CallState.SENT_TERMINATE, stopCall( callId ) );
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

    private Call stopCall( long callId )
    {
        // remove the call
        Call call = mCallMap.remove( callId );

        // reset current call ID
        if ( mCurrentCall.getCallId() == callId )
        {
            mCurrentCall = null;
        }

        // reset the audio
        resetAudio();

        // now return the removed Call object
        return call;
    }
}

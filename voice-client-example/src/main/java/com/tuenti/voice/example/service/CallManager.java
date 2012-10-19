package com.tuenti.voice.example.service;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import com.tuenti.voice.core.CallError;
import com.tuenti.voice.core.CallListener;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.ui.activity.CallInProgressActivity;
import com.tuenti.voice.example.ui.dialog.IncomingCallDialog;
import com.tuenti.voice.example.util.CallNotification;
import com.tuenti.voice.example.util.ConnectionMonitor;
import com.tuenti.voice.example.util.NetworkPreference;
import com.tuenti.voice.example.util.RingManager;

import java.util.HashMap;

import static android.content.Intent.*;

public class CallManager
    implements CallListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallManager";

    private static final int CALL_UPDATE_INTERVAL = 1000;

    private Handler callProgressHandler;

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
            receiveToggleMute( callId );
        }

        @Override
        public void toggleHold( long callId )
        {
            receiveToggleHold( callId );
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

    private HashMap<Long, Call> mCallMap = new HashMap<Long, Call>();

    private final RemoteCallbackList<ICallServiceCallback> mCallbacks = new RemoteCallbackList<ICallServiceCallback>();

    private final VoiceClient mClient;

    private final Context mContext;

    private long mCurrentCallId;

    private NetworkPreference mNetworkPreference;

    private CallNotification mNotificationManager;

    private RingManager mRingManager;

    private Runnable updateCallDurationTask;

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
        remoteJid = cleanJid( remoteJid );
        switch ( CallState.fromInteger( state ) )
        {
            case SENT_INITIATE:
                sendOutgoingCall( callId, remoteJid );
                break;
            case RECEIVED_INITIATE:
                sendIncomingCall( callId, remoteJid );
                break;
            case RECEIVED_REJECT:
                Log.i( TAG, "Call ended" );
                endCall( callId, 0 );// Add reason to end call.
                break;
            case IN_PROGRESS:
                Log.i( TAG, "IN_PROGRESS" );
                callStarted( callId );
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind()
    {
        return mBinder;
    }

    private void callStarted( long callId )
    {
        stopRing();

        setAudioForCall();

        Call call = mCallMap.get( callId );
        call.setCallStartTime( SystemClock.elapsedRealtime() );
        startCallInProgressActivity( callId, call.getRemoteJid() );

        // do callback        
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleCallStarted( call );
            }
            catch ( RemoteException e )
            {
                //NOOP
            }
        }
        // Intent call started

        // Change notification to call in progress notification, that points to
        // call in progress activity on click.
        sendCallInProgressNotification( call.getRemoteJid(), 0 );
        // dispatchCallState( CallUIIntent.CALL_PROGRESS, mCurrentCallId, call.getRemoteJid(), call.getElapsedTime() );

        // start timer on method updateCallUI every second.
        startCallProgressTimer();
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
            mCallMap.remove( Long.valueOf( callId ) );
            stopRing();
            resetAudio();

            //dispatchCallState( CallUIIntent.CALL_ENDED, callId, call.getRemoteJid() );
            // Store reason in call history with jid.

            // Intent call ended, store in history, return call time
            stopCallProgressTimer();

            // Cancel notification
            mNotificationManager.cancelCallNotification();
        }
    }

    private Call getCurrentCall()
    {
        return mCallMap.get( mCurrentCallId );
    }

    private void initCallState( long callId, String remoteJid )
    {
        mNetworkPreference.enableStickyNetworkPreference();
        mCallInProgress = true;
        mCurrentCallId = callId;
        mCallMap.put( callId, new Call( callId, remoteJid ) );
    }

    private void receiveToggleHold( long callId )
    {
        Call call = mCallMap.get( Long.valueOf( callId ) );
        if ( call != null )
        {
            call.setHold( !call.isHold() );
            mClient.holdCall( callId, call.isHold() );
        }
    }

    private void receiveToggleMute( long callId )
    {
        Call call = mCallMap.get( Long.valueOf( callId ) );
        if ( call != null )
        {
            call.setMute( !call.isMute() );
            mClient.muteCall( callId, call.isMute() );
        }
    }

    private void resetAudio()
    {
        mAudioManager.setMode( AudioManager.MODE_NORMAL );
        mAudioManager.abandonAudioFocus( null );
    }

    /**
     * Sends a call progress (duration) notification.
     *
     * @param remoteJid The remote party.
     * @param duration  Call duration.
     */
    private void sendCallInProgressNotification( String remoteJid, long duration )
    {
        long minutes = duration / 60;
        long seconds = duration % 60;
        String formattedDuration = String.format( "%02d:%02d", minutes, seconds );
        String message =
            String.format( mContext.getString( R.string.notification_during_call ), formattedDuration, remoteJid );
        String tickerText = String.format( mContext.getString( R.string.notification_during_call_ticker ), remoteJid );

        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleCallInProgress();
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();

        //Intent intent = getNotificationIntent( remoteJid, CallUIIntent.CALL_PROGRESS, CallInProgressActivity.class );
        //mNotificationManager.sendCallNotification( tickerText, message, intent, CallIntent.END_CALL );
    }

    private void sendIncomingCall( long callId, String remoteJid )
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

            // Ringer.
            startRing( true, false );

            // show alert pop up for incoming call + tray notification
            startIncomingCallDialog( callId, remoteJid );
            sendIncomingCallNotification( remoteJid );
        }
    }

    /**
     * Sends an incoming call notification.
     *
     * @param remoteJid The user that is calling.
     */
    private void sendIncomingCallNotification( String remoteJid )
    {
        String message = String.format( mContext.getString( R.string.notification_incoming_call ), remoteJid );
        //Intent intent = getNotificationIntent( remoteJid, "", IncomingCallDialog.class );
        //mNotificationManager.sendCallNotification( message, message, intent, CallIntent.REJECT_CALL );
    }

    private void sendOutgoingCall( long callId, String remoteJid )
    {
        initCallState( callId, remoteJid );

        // Intent call started, show call in progress activity
        Intent intent = new Intent( mContext, CallInProgressActivity.class );
        intent.putExtra( "call", getCurrentCall() );
        intent.addFlags( FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK | FLAG_FROM_BACKGROUND );
        mContext.startActivity( intent );

        // Ring in the headphone
        startRing( false, false );

        // Show notification
        sendOutgoingCallNotification( remoteJid );
    }

    /**
     * Sends an outgoing call notification.
     *
     * @param remoteJid The user that is being called.
     */
    private void sendOutgoingCallNotification( String remoteJid )
    {
        String message = String.format( mContext.getString( R.string.notification_outgoing_call ), remoteJid );
        //Intent intent = getNotificationIntent( remoteJid, CallUIIntent.CALL_PROGRESS, CallInProgressActivity.class );
        //mNotificationManager.sendCallNotification( message, message, intent, CallIntent.END_CALL );
    }

    private void setAudioForCall()
    {
        mAudioManager.setMode( ( Build.VERSION.SDK_INT < 11 )
                                   ? AudioManager.MODE_NORMAL
                                   : AudioManager.MODE_IN_COMMUNICATION );
        mAudioManager.requestAudioFocus( null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
    }

    private void startCallInProgressActivity( long callId, String remoteJid )
    {
        Intent intent = new Intent( mContext, CallInProgressActivity.class );
        intent.putExtra( "callId", callId );
        intent.putExtra( "remoteJid", remoteJid );
        intent.addFlags( FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_FROM_BACKGROUND );
        mContext.startActivity( intent );
    }

    /**
     * Starts the call progress update timer.
     */
    private void startCallProgressTimer()
    {
        callProgressHandler.removeCallbacks( updateCallDurationTask );
        callProgressHandler.postDelayed( updateCallDurationTask, CALL_UPDATE_INTERVAL );
    }

    private void startIncomingCallDialog( long callId, String remoteJid )
    {
        Intent intent = new Intent( mContext, IncomingCallDialog.class );
        intent.putExtra( "callId", callId );
        intent.putExtra( "remoteJid", remoteJid );
        intent.addFlags( FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_FROM_BACKGROUND );
        mContext.startActivity( intent );
    }

    private void startRing( boolean isIncoming, boolean callInProgress )
    {
        stopRing();
        mRingManager = new RingManager( mContext, isIncoming, callInProgress );
    }

    /**
     * Stops the call progress update timer.
     */
    private void stopCallProgressTimer()
    {
        callProgressHandler.removeCallbacks( updateCallDurationTask );
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

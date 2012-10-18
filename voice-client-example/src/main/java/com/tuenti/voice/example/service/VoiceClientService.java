package com.tuenti.voice.example.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.CallError;
import com.tuenti.voice.core.CallListener;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.IVoiceClientServiceInt;
import com.tuenti.voice.core.RosterListener;
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

public class VoiceClientService
    extends Service
    implements IVoiceClientServiceInt, CallListener, RosterListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "VoiceClientService";

    private static final int CALL_UPDATE_INTERVAL = 1000;

    /**
     * This is a list of callbacks that have been registered with the service.
     * Note that this is package scoped (instead of private) so that it can be
     * accessed more efficiently from inner classes.
     */
    final RemoteCallbackList<IVoiceClientServiceCallback> mCallbacks =
        new RemoteCallbackList<IVoiceClientServiceCallback>();

    private Handler callProgressHandler;

    private AudioManager mAudioManager;

    /*
     * Binder Interface implementation.
     */
    private final IVoiceClientService.Stub mBinder = new IVoiceClientService.Stub()
    {
        public void acceptCall( long callId )
            throws RemoteException
        {
            mClient.acceptCall( callId );
        }

        public void call( String remoteJid )
            throws RemoteException
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

        public void declineCall( long callId, boolean busy )
        {
            mClient.declineCall( callId, busy );
        }

        public void toggleMute( long callId )
        {
            Call call = mCallMap.get( Long.valueOf( callId ) );
            if ( call != null )
            {
                call.setMute( !call.isMuted() );
                mClient.muteCall( callId, call.isMuted() );
            }
        }

        public void toggleHold( long callId )
        {
            Call call = mCallMap.get( Long.valueOf( callId ) );
            if ( call != null )
            {
                call.setHold( !call.isHeld() );
                mClient.holdCall( callId, call.isHeld() );
            }
        }

        public void endCall( long callId )
            throws RemoteException
        {
            mClient.endCall( callId );
        }

        public void registerCallback( IVoiceClientServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.register( cb );
            }
        }

        public void unregisterCallback( IVoiceClientServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.unregister( cb );
            }
        }
    };

    private boolean mCallInProgress = false;

    private HashMap<Long, Call> mCallMap = new HashMap<Long, Call>();

    private VoiceClient mClient;

    private ConnectionManager mConnectionManager;

    private long mCurrentCallId = 0;

    private boolean mExternalCallInProgress = false;

    private NetworkPreference mNetworkPreference;

    private CallNotification mNotificationManager;

    private RingManager mRingManager;

    private SharedPreferences mSettings;

    private Runnable updateCallDurationTask;

    private static String cleanJid( String jid )
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

    // ------------------- End Service Methods -------------------------------
    @Override
    public void handleCallStateChanged( int state, String remoteJid, long callId )
    {
        remoteJid = cleanJid( remoteJid );
        switch ( CallState.fromInteger( state ) )
        {
            case SENT_INITIATE:
                Log.i( TAG, "Outgoing call" );
                outgoingCall( callId, remoteJid );
                break;
            case RECEIVED_INITIATE_ACK:
                Log.i( TAG, "Initiate was acked" );
                break;
            case RECEIVED_INITIATE:
                Log.i( TAG, "Incoming call" );
                if ( ConnectionMonitor.hasSlowConnection() )
                {
                    //Warn that you can't accept the incoming call
                    //TODO(Luke): Notification of missed call./Explanation about bad connection
                    Log.i( TAG, "Declining call because of slow connection" );
                    mClient.declineCall( callId, true );
                }
                else if ( mCallInProgress ||
                    ConnectionMonitor.getInstance( getApplicationContext() ).isCallInProgress() )
                {
                    //TODO(Luke): Notification of missed call.
                    Log.i( TAG, "Declining call because call in progress" );
                    mClient.declineCall( callId, true );
                }
                else
                {
                    incomingCall( callId, remoteJid );
                }
                break;
            case SENT_TERMINATE:
            case RECEIVED_TERMINATE:
            case SENT_BUSY:
            case RECEIVED_BUSY:
            case SENT_REJECT:
            case RECEIVED_REJECT:
                Log.i( TAG, "Call ended" );
                endCall( callId, 0 );// Add reason to end call.
                break;
            case RECEIVED_ACCEPT:
                Log.i( TAG, "Call accepted" );
                break;
            case IN_PROGRESS:
                Log.i( TAG, "IN_PROGRESS" );
                callStarted( callId );
                break;
            case DE_INIT:
                Log.i( TAG, "DE_INIT" );
                break;
        }
        Log.i( TAG, "call state ------------------" + state );
    }

// --------------------- Interface IVoiceClientServiceInt ---------------------

    public void sendBundle( Bundle bundle )
    {
        final int N = mCallbacks.beginBroadcast();
        for ( int i = 0; i < N; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).sendBundle( bundle );
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

// --------------------- Interface RosterListener ---------------------

    @Override
    public void handleBuddyListChanged( int state, String remoteJid )
    {
        switch ( BuddyListState.fromInteger( state ) )
        {
            case ADD:
                Log.v( TAG, "Adding buddy " + remoteJid );
                // Intent add buddy
                // mBuddyList.add(remoteJid);
                break;
            case REMOVE:
                Log.v( TAG, "Removing buddy" + remoteJid );
                // Intent remove buddy
                // mBuddyList.remove(remoteJid);
                break;
            case RESET:
                Log.v( TAG, "Reset buddy list" );
                // intent reset buddy list
                // mBuddyList.clear();
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public void callStarted( long callId )
    {
        stopRing();
        Call call = mCallMap.get( Long.valueOf( callId ) );
        call.startCallTimer();
        String remoteJid = call.getRemoteJid();
        setAudioForCall();
        startCallInProgressActivity( callId, remoteJid );
        dispatchCallState( CallUIIntent.CALL_STARTED, callId, call.getRemoteJid() );
        // Intent call started

        // Change notification to call in progress notification, that points to
        // call in progress activity on click.
        sendCallInProgressNotification( remoteJid, 0 );
        dispatchCallState( CallUIIntent.CALL_PROGRESS, mCurrentCallId, call.getRemoteJid(), call.getElapsedTime() );

        // start timer on method updateCallUI every second.
        startCallProgressTimer();
    }

    public void dispatchLocalIntent( Intent intent )
    {
        final int N = mCallbacks.beginBroadcast();
        for ( int i = 0; i < N; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).dispatchLocalIntent( intent );
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    public void endCall( long callId, int reason )
    {
        // Makes sure we don't change state for calls
        // we decline as busy while in a call.
        if ( mCallMap.containsKey( Long.valueOf( callId ) ) )
        {
            mCallInProgress = false;
            mCurrentCallId = 0;
            mNetworkPreference.unsetNetworkPreference();
            Call call = mCallMap.get( Long.valueOf( callId ) );
            mCallMap.remove( Long.valueOf( callId ) );
            stopRing();
            resetAudio();
            dispatchCallState( CallUIIntent.CALL_ENDED, callId, call.getRemoteJid() );
            // Store reason in call history with jid.

            // Intent call ended, store in history, return call time
            stopCallProgressTimer();

            // Cancel notification
            mNotificationManager.cancelCallNotification();

            long callTime = call.getElapsedTime();
        }
    }

    public Intent getCallIntent( String intentString, long callId, String remoteJid )
    {
        Intent intent = new Intent( intentString );
        intent.putExtra( "callId", callId );
        intent.putExtra( "remoteJid", remoteJid );
        Call call = mCallMap.get( Long.valueOf( callId ) );
        intent.putExtra( "isHeld", call.isHeld() );
        intent.putExtra( "isMuted", call.isMuted() );
        return intent;
    }

    public void incomingCall( long callId, String remoteJid )
    {
        initCallState( callId, remoteJid );

        // Ringer.
        startRing( true, false );

        // show alert pop up for incoming call + tray notification
        startIncomingCallDialog( callId, remoteJid );
        sendIncomingCallNotification( remoteJid );
    }

    public void initCallState( long callId, String remoteJid )
    {
        mNetworkPreference.enableStickyNetworkPreference();
        mCallInProgress = true;
        mCurrentCallId = callId;
        mCallMap.put( callId, new Call( callId, remoteJid ) );
    }

    @Override
    public IBinder onBind( Intent intent )
    {
        if ( IConnectionService.class.getName().equals( intent.getAction() ) )
        {
            return mConnectionManager.onBind();
        }
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Set default preferences
        mSettings = PreferenceManager.getDefaultSharedPreferences( this );

        mNotificationManager = new CallNotification( this );
        mNetworkPreference = new NetworkPreference( this );
        initClientWrapper();
        initAudio();
        initCallDurationTask();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        mCallbacks.kill();
        mClient.release();
        mClient = null;
        mNetworkPreference.unsetNetworkPreference();
        ConnectionMonitor.getInstance( getApplicationContext() ).destroy();
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId )
    {
        return START_STICKY;
    }

    public void outgoingCall( long callId, String remoteJid )
    {
        initCallState( callId, remoteJid );
        Intent dialogIntent = new Intent( getBaseContext(), CallInProgressActivity.class );
        dialogIntent.putExtra( "callId", callId );
        dialogIntent.putExtra( "remoteJid", remoteJid );
        dialogIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_FROM_BACKGROUND );

        // Intent call started, show call in progress activity
        getApplication().startActivity( dialogIntent );

        // Ring in the headphone
        startRing( false, false );

        // Show notification
        sendOutgoingCallNotification( remoteJid );
    }

    public void startCallInProgressActivity( long callId, String remoteJid )
    {
        Intent dialogIntent = new Intent( getBaseContext(), CallInProgressActivity.class );
        dialogIntent.putExtra( "callId", callId ).putExtra( "remoteJid", remoteJid ).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_FROM_BACKGROUND );
        getApplication().startActivity( dialogIntent );
    }

    public void startIncomingCallDialog( long callId, String remoteJid )
    {
        Intent dialogIntent = new Intent( getBaseContext(), IncomingCallDialog.class );
        dialogIntent.putExtra( "callId", callId ).putExtra( "remoteJid", remoteJid ).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_FROM_BACKGROUND );
        getApplication().startActivity( dialogIntent );
    }

    public void updateCallUI()
    {
        if ( mCurrentCallId > 0 )
        {
            // update duration of call in tray notification via changeData.
            // send message to controller, in case there is one, to tell it to
            // update call duration on the UI.
        }
    }

    private void dispatchCallState( String callState, long callId, String remoteJid )
    {
        dispatchCallState( callState, callId, remoteJid, -1 );
    }

    private void dispatchCallState( String callState, long callId, String remoteJid, long duration )
    {
        Intent intent = new Intent( callState );
        // TODO: Stick these extra values in some constants!
        intent.putExtra( "callId", callId );
        intent.putExtra( "remoteJid", remoteJid );
        intent.putExtra( "duration", duration );
        dispatchLocalIntent( intent );
    }

    /**
     * Generates an Intent to send with a Notification.
     *
     * @param remoteJid The remote party.
     * @param action    The action to pass to the Intent.
     * @param context   The context (i.e. class) to start with this intent.
     */
    private Intent getNotificationIntent( String remoteJid, String action, Class<?> context )
    {
        Intent intent = getCallIntent( action, mCurrentCallId, remoteJid );
        intent.setClass( this, context );
        intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
        return intent;
    }

    private void initAudio()
    {
        mAudioManager = (AudioManager) getSystemService( Context.AUDIO_SERVICE );
    }

    private void initCallDurationTask()
    {
        callProgressHandler = new Handler();

        updateCallDurationTask = new Runnable()
        {
            @Override
            public void run()
            {
                Call currentCall = mCallMap.get( mCurrentCallId );

                // Send Intent.
                dispatchCallState( CallUIIntent.CALL_PROGRESS,
                                   mCurrentCallId,
                                   currentCall.getRemoteJid(),
                                   currentCall.getElapsedTime() );

                // Send Notification.
                sendCallInProgressNotification( currentCall.getRemoteJid(), currentCall.getElapsedTime() );

                // Add another call for update.
                callProgressHandler.postDelayed( this, CALL_UPDATE_INTERVAL );
            }
        };
    }

    private void initClientWrapper()
    {
        mClient = new VoiceClient();
        mClient.addRosterListener( this );
        mClient.addCallListener( this );

        mConnectionManager = new ConnectionManager( mClient );

        ConnectionMonitor.getInstance( getApplicationContext() );
        ConnectionMonitor.registerCallback( mConnectionManager );
    }

// ----------------- End Connection Monitor interface ---------------


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
        String message = String.format( getString( R.string.notification_during_call ), formattedDuration, remoteJid );
        String tickerText = String.format( getString( R.string.notification_during_call_ticker ), remoteJid );

        Intent intent = getNotificationIntent( remoteJid, CallUIIntent.CALL_PROGRESS, CallInProgressActivity.class );
        mNotificationManager.sendCallNotification( tickerText, message, intent, CallIntent.END_CALL );
    }

    /**
     * Sends an incoming call notification.
     *
     * @param remoteJid The user that is calling.
     */
    private void sendIncomingCallNotification( String remoteJid )
    {
        String message = String.format( getString( R.string.notification_incoming_call ), remoteJid );
        Intent intent = getNotificationIntent( remoteJid, "", IncomingCallDialog.class );
        mNotificationManager.sendCallNotification( message, message, intent, CallIntent.REJECT_CALL );
    }

    /**
     * Sends an outgoing call notification.
     *
     * @param remoteJid The user that is being called.
     */
    private void sendOutgoingCallNotification( String remoteJid )
    {
        String message = String.format( getString( R.string.notification_outgoing_call ), remoteJid );
        Intent intent = getNotificationIntent( remoteJid, CallUIIntent.CALL_PROGRESS, CallInProgressActivity.class );

        mNotificationManager.sendCallNotification( message, message, intent, CallIntent.END_CALL );
    }

    private void setAudioForCall()
    {
        mAudioManager.setMode( ( Build.VERSION.SDK_INT < 11 )
                                   ? AudioManager.MODE_NORMAL
                                   : AudioManager.MODE_IN_COMMUNICATION );
        mAudioManager.requestAudioFocus( null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
    }

    /**
     * Starts the call progress update timer.
     */
    private void startCallProgressTimer()
    {
        callProgressHandler.removeCallbacks( updateCallDurationTask );
        callProgressHandler.postDelayed( updateCallDurationTask, CALL_UPDATE_INTERVAL );
    }

    private void startRing( boolean isIncoming, boolean callInProgress )
    {
        stopRing();
        mRingManager = new RingManager( getApplicationContext(), isIncoming, callInProgress );
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

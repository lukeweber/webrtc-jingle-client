package com.tuenti.voice.example.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import android.util.Log;

import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.VoiceClientEventCallback;
import com.tuenti.voice.core.VoiceClientEventHandler;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.core.IVoiceClientServiceInt;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.service.IVoiceClientService;
import com.tuenti.voice.example.service.IVoiceClientServiceCallback;
import java.util.Map;
import java.lang.Long;

public class VoiceClientService 
    extends Service
    implements IVoiceClientServiceInt, VoiceClientEventCallback
{
    private VoiceClient mClient;

    private static final String TAG = "s-libjingle-webrtc";
    
    private Map<Long, Call> callMap = new HashMap<Long, Call>;
    
    private boolean mCallInProgress = false;

    private long mCurrentCallId = 0;

    /**
     * This is a list of callbacks that have been registered with the
     * service.  Note that this is package scoped (instead of private) so
     * that it can be accessed more efficiently from inner classes.
     */
    final RemoteCallbackList<IVoiceClientServiceCallback> mCallbacks
            = new RemoteCallbackList<IVoiceClientServiceCallback>();

// --------------------- Service Methods --------------------------------------- 
    @Override
    public void onCreate() {
        super.onCreate();
        initClientWrapper();
        
        // Set default preferences
        //mSettings = PreferenceManager.getDefaultSharedPreferences( this );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i( TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e( TAG, "onBind Received" );
        return mBinder;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mCallbakcks.kill();
        mBuddyList.clear();
        mClient = null;
    }
    
    public void initCallState( long callId){
        mCallInProgress = true;
        mCurrentCallId = callId;
        callMap.put(Long(callId), new Call(callId));
    }

    public void outgoingCall( long CallId ){
        initCallState( callId );
        //Intent call started, show call in progress activity
        //Ring in the headphone
    }

    public void incomingCall( long callId ){
        initCallState( callId );
        //show alert pop up for incoming call + tray notification
        // Ringer.
    }

    public void rejectCall( long callId ){
        //Cancel notification alert for incoming call + tray
    }

    public void callStarted( long callId ){
        callMap.get(Long(callId)).startCallTimer();
        //start timer on method updateCallUI every second.
        //Intent call started,
        //Change notification to call in progress notification, that points to call in progress activity on click.
    }

    public void updateCallUI(){
        if( currentCallId > 0 ){
            //update duration of call in tray notification via changeData.
            //send message to controller, in case there is one, to tell it to update call duration on the UI.
        }
    }

    public void endCall( long callId, int Reason){
        // Makes sure we don't change state for calls
        // we decline as busy while in a call.
        if (callMap.containsKey(Long(callId))) {
            mCallInProgress = false;
            mCurrentCallId = 0;
            Call call = callMap.get(Long(callId));
            callMap.remove(Long(callId));
            //Store reason in call history with jid.
            //Intent call ended, store in history, return call time
            //cancel ringer
            //cancel notification
            long callTime = call.getElapsedTime();
        }
    }
    
    public void endAllCalls(){
        //iterate over hash map
        //end all calls.
        //we do this only on xmpp disconnect
    }
// --------------------- Interface VoiceClientEventCallback ---------------------
    @Override
    public void handleCallStateChanged( int state, String remoteJid, long callId )
    {
        switch ( CallState.fromInteger( state ) )
        {
            case SENT_INITIATE:
                Log.i(TAG, "Outgoing call");
                outgoingCall( callId );
                break;
            case RECEIVED_INITIATE:
                Log.i(TAG, "Incoming call");
                if( mCallInProgress == false ) {
                    incomingCall( callId );
                } else {
                    mClient.decline(callId, true);//Decline busy;
                }
                break;
            case SENT_TERMINATE:
            case RECEIVED_TERMINATE:
            case SENT_BUSY:
            case RECEIVED_BUSY:
            case SENT_REJECT:
            case RECEIVED_REJECT:
                Log.i(TAG, "Call ended");
                endCall( callId );            
                break;
            case IN_PROGRESS:
            //case RECEIVED_ACCEPT:
                Log.i(TAG, "IN_PROGRESS");
                callStarted( callId );
                break;
            case DE_INIT:
                Log.i(TAG, "DE_INIT");
                break;
        }
    }

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
        }
    }

    @Override
    public void handleXmppStateChanged( int state )
    {
        switch ( XmppState.fromInteger( state ) )
        {
            case START:
                // changeStatus( "connecting..." );
                break;
            case OPENING:
                // changeStatus( "logging in..." );
                break;
            case OPEN:
                // changeStatus( "logged in..." );
                break;
            case CLOSED:
                endAllCalls();
                //Intent disconnected.
                // - Connection listener can handle this event.
                // - When we have a connection, it will try to
                // login again.
                break;
        }
    }


    @Override
    public void handleBuddyListChanged( int state , String remoteJid)
    {
        switch ( BuddyListState.fromInteger( state ) ){
            case ADD:
                Log.v( TAG, "Adding buddy " + remoteJid );
                //Intent add buddy
                //mBuddyList.add(remoteJid);
                break;
            case REMOVE:
                Log.v( TAG, "Removing buddy" + remoteJid );
                //Intent remove buddy
                //mBuddyList.remove(remoteJid);
                break;
            case RESET:
                Log.v( TAG, "Reset buddy list" );
                //intent reset buddy list
                //mBuddyList.clear();
                break;
        }
    }

    public void sendBundle(Bundle bundle){
        final int N = mCallbacks.beginBroadcast();
        for (int i=0; i<N; i++) {
            try {
                mCallbacks.getBroadcastItem(i).sendBundle(bundle);
            } catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }
    
    /*
     * Binder Interface implementation.
     */
    private final IVoiceClientService.Stub mBinder = new IVoiceClientService.Stub() {
        public void acceptCall( long callId )  throws RemoteException {
            mClient.acceptCall( callId );
        }
        public void call( String remoteJid ) throws RemoteException {
            mClient.call( remoteJid );
        }
        public void declineCall( long callId, boolean busy ) throws RemoteException {
            mClient.declineCall( callId, busy );
        }
        public void muteCall( long callId, boolean mute ) throws RemoteException {
            mClient.muteCall( callId, mute );
        }
        public void holdCall( long callId, boolean hold ) throws RemoteException {
            mClient.holdCall( callId, hold );
        }
        public void endCall( long callId ) throws RemoteException {
            mClient.endCall( callId );
        }
        public void init( String stunServer, String relayServerUdp, String relayServerTcp, String relayServerSsl, String turnServer ) throws RemoteException {
            mClient.init( stunServer, relayServerUdp, relayServerTcp, relayServerSsl, turnServer );
        }
        public void login( String username, String password, String xmppHost, int xmppPort, boolean xmppUseSsl ) throws RemoteException {
            mClient.login( username, password, xmppHost, xmppPort, xmppUseSsl);
        }
        public void logout() throws RemoteException {
            mClient.logout();
        }
        public void release() throws RemoteException {
            mClient.release();
        }
        public BuddyList getBuddyList() throws RemoteException {
            return mBuddyList;
            //Implement me.
        }
        public CallHistoryList getCallHistory() throws RemoteException {
            return mCallHistory;
            // Implement me., list of remoteJids with states, call duration.
            // Should probably be stored in phone storage.
        }
        public void registerCallback(IVoiceClientServiceCallback cb) {
            if (cb != null) mCallbacks.register(cb);
        }
        public void unregisterCallback(IVoiceClientServiceCallback cb) {
            if (cb != null) mCallbacks.unregister(cb);
        }
    };

    private void initClientWrapper()
    {
        mClient = VoiceClient.getInstance();
        mClient.setHandler( this );
    }
}

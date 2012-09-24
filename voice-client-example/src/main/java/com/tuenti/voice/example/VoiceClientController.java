package com.tuenti.voice.example;

import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.VoiceClientEventCallback;
import com.tuenti.voice.core.VoiceClientEventHandler;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;

import com.tuenti.voice.example.service.VoiceClientService;
import com.tuenti.voice.example.service.IVoiceClientService;
import com.tuenti.voice.example.service.IVoiceClientServiceCallback;

public class VoiceClientController
    implements VoiceClientEventCallback
{
    IVoiceClientService mService;
    private VoiceClientEventHandler mHandler;
    private Context mContext;
    
    private static final String TAG = "controller-libjingle-webrtc";

    private boolean mIsBound = false;
    
    public VoiceClientController(Context context){
        mContext = context;
        mHandler = new VoiceClientEventHandler( this );
    }
    
    public void bind() {
        mContext.bindService(new Intent(IVoiceClientService.class.getName()),
                mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

// --------------------- Interface VoiceClientEventCallback ---------------------
    @Override
    public void handleCallStateChanged( int state, String remoteJid, long callId )
    {
        //Intent intent = null;
        switch ( CallState.fromInteger( state ) )
        {
            case SENT_INITIATE:
                Log.i(TAG, "SENT_INITIATE"); 
                //intent = new Intent(CallIntent.SENT_INITIATE);
                //intent.putExtra("callId", callId);
                break;
            case RECEIVED_INITIATE:
                Log.i(TAG, "RECEIVED_INITIATE");
                //intent = new Intent(CallIntent.SENT_INITIATE);
                //intent.putExtra("callId", callId);
                //intent.putExtra("remoteJid", remoteJid);
                break;
            case SENT_TERMINATE:
                Log.i(TAG, "SENT_TERMINATE");
                //intent = new Intent(CallIntent.SENT_TERMINATE);
                //intent.putExtra("callId", callId);
                break;
            case RECEIVED_TERMINATE:
                Log.i(TAG, "RECEIVED_TERMINATE");
                //intent = new Intent(CallIntent.RECEIVED_TERMINATE);
                //intent.putExtra("callId", callId);
                break;
            case RECEIVED_ACCEPT:
                Log.i(TAG, "RECEIVED_ACCEPT");
                //intent = new Intent(CallIntent.RECEIVED_ACCEPT);
                //intent.putExtra("callId", callId);
                break;
            case RECEIVED_REJECT:
                Log.i(TAG, "RECEIVED_REJECT");
                //intent = new Intent(CallIntent.RECEIVED_REJECT);
                //intent.putExtra("callId", callId);
                break;
            case RECEIVED_BUSY:
                Log.i(TAG, "RECEIVED_BUSY");
                //intent = new Intent(CallIntent.RECEIVED_REJECT);
                //intent.putExtra("callId", callId);
                break;
            case IN_PROGRESS:
                Log.i(TAG, "IN_PROGRESS");
                //intent = new Intent(CallIntent.IN_PROGRESS);
                //intent.putExtra("callId", callId);
                break;
            case DE_INIT:
                Log.i(TAG, "DE_INIT");
                //intent = new Intent(CallIntent.DE_INIT);
                break;
        }

       // if ( intent != null ) {
            //sendBroadcast(intent);
       // }
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
                //sendBroadcast(new Intent());
                // changeStatus( "logging in..." );
                break;
            case OPEN:
                //sendBroadcast(new Intent());
                // changeStatus( "logged in..." );
                break;
            case CLOSED:
                //sendBroadcast(new Intent());
                // changeStatus( "logged out..." );
                break;
        }
    }

    @Override
    public void handleBuddyListChanged( int state , String remoteJid)
    {
        switch ( BuddyListState.fromInteger( state ) ){
            case ADD:
                Log.v( TAG, "Adding buddy " + remoteJid );
                break;
            case REMOVE:
                Log.v( TAG, "Removing buddy" + remoteJid );
                break;
            case RESET:
                Log.v( TAG, "Reset buddy list" );
                break;
        }
    }

// --------------------- Interface VoiceClientEventCallback ---------------------
 
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = IVoiceClientService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            } catch ( RemoteException $e ) {

            }
            // We want to monitor the service for as long as we are
            // connected to it.
            Log.i( TAG, "Connected to service" );
        }

        public void onServiceDisconnected(ComponentName className) {
            try {
                mService.unregisterCallback(mCallback);
            } catch ( RemoteException $e ) {

            }
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            Log.i( TAG, "Disconnected from service" );
        }
    };

    /**
     * This implementation is used to receive callbacks from the remote
     * service.
     */
    private IVoiceClientServiceCallback mCallback = new IVoiceClientServiceCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about
         * new values.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        public void sendBundle( Bundle bundle ){
            Message msg = Message.obtain();
            msg.what = bundle.getInt("what");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    };
    
    public void onDestroy(){
        if( mIsBound ){
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }
}

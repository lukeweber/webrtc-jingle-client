package com.tuenti.voice.service;

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
import com.tuenti.voice.service.IVoiceClientService;
import com.tuenti.voice.service.IVoiceClientServiceCallback;

public class VoiceClientService 
    extends Service
{
    private VoiceClient mClient;

    private static final String TAG = "s-libjingle-webrtc";
    
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
        mClient.setService( this );
    }
}

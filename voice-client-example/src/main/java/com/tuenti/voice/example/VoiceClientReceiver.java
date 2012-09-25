package com.tuenti.voice.example;

import com.tuenti.voice.example.service.IVoiceClientService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.RemoteException;

import com.tuenti.voice.example.service.CallIntent;

public class VoiceClientReceiver extends BroadcastReceiver {

    private static final String TAG = "broadcastreceiver-libjingle-webrtc";

    @Override
    public void onReceive(Context context, Intent intent) {
        IVoiceClientService service = VoiceClientApplication.getService();
        Log.i( TAG, "Received intent: " + intent.getAction());
        String intentString = intent.getAction();
        long callId = intent.getLongExtra("callId", 0);
        if( CallIntent.HOLD_CALL == intentString ) {
            try{
                service.toggleHold( callId );
            } catch( RemoteException e ){}
        } else if( CallIntent.MUTE_CALL == intentString ) {
            try{
                service.toggleMute( callId );
            } catch( RemoteException e ){}
        } else if( CallIntent.END_CALL == intentString ) {
            try{
                service.endCall( callId );
            } catch( RemoteException e ){}
        } else if( CallIntent.PLACE_CALL == intentString ) {
            String remoteJid = intent.getStringExtra("remoteJid");
            try{
                service.call( remoteJid );
            } catch( RemoteException e ){}
        }
    }
}

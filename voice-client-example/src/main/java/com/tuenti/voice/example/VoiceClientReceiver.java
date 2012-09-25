package com.tuenti.voice.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class VoiceClientReceiver extends BroadcastReceiver {

    private static final String TAG = "broadcastreceiver-libjingle-webrtc";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i( TAG, "Received intent: " + intent.getAction());
        String intentString = intent.getAction();
    }
}

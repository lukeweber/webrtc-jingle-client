package com.tuenti.voice.service;
import android.os.Message;

oneway interface IVoiceClientServiceCallback {
    void sendBundle( out Bundle bundle );
}

package com.tuenti.voice.example.service;
import android.os.Message;

oneway interface IVoiceClientServiceCallback {
    void sendBundle( out Bundle bundle );
}

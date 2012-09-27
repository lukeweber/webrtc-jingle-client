package com.tuenti.voice.example.service;
import android.os.Bundle;
import android.content.Intent;

oneway interface IVoiceClientServiceCallback {
    void sendBundle( in Bundle bundle );
    void dispatchLocalIntent( in Intent intent );
}

package com.tuenti.voice.example.service;
import android.os.Bundle;
import android.content.Intent;

oneway interface IVoiceClientServiceCallback {
    void sendBundle( out Bundle bundle );
    void dispatchIntent( out Intent intent );
}

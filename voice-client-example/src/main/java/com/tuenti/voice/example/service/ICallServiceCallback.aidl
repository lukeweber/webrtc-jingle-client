package com.tuenti.voice.example.service;

import com.tuenti.voice.example.data.Call;

oneway interface ICallServiceCallback {

    void handleCallInProgress();
    void handleOutgoingCall( in Call call );
    void handleOutgoingCallTerminated();
}
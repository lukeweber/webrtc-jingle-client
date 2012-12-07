package com.tuenti.voice.core.service;

import com.tuenti.voice.core.data.Call;

oneway interface ICallServiceCallback {

    void handleCallInProgress();
    void handleIncomingCall( in Call call );
    void handleIncomingCallAccepted();
    void handleIncomingCallTerminated( in Call call );
    void handleOutgoingCall( in Call call );
    void handleOutgoingCallAccepted();
    void handleOutgoingCallTerminated( in Call call );
}
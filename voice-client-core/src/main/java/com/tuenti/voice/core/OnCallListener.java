package com.tuenti.voice.core;

import com.tuenti.voice.core.data.Call;

public interface OnCallListener
{
// -------------------------- OTHER METHODS --------------------------

    void acceptCall( long callId );

    void call( String remoteJid );

    void endCall( long callId );

    void onCallInProgress();

    void onIncomingCall( Call call );

    void onIncomingCallAccepted();

    void onIncomingCallTerminated();

    void onOutgoingCall( Call call );

    void onOutgoingCallAccepted();

    void onOutgoingCallTerminated();

    void toggleHold( long callId );

    void toggleMute( long callId );
}

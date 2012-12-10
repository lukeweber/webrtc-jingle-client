package com.tuenti.voice.core.service;

import com.tuenti.voice.core.service.ICallServiceCallback;

interface ICallService {

    void acceptCall( long callId );
    void call( String remoteJid );
    void declineCall( long callId, boolean busy );
    void endCall( long callId );
    void toggleMute( long callId );
    void toggleHold( long callId );
    void registerCallback( ICallServiceCallback cb );
    void unregisterCallback( ICallServiceCallback cb );

}
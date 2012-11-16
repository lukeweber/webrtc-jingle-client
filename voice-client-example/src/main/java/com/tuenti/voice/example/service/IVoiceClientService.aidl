package com.tuenti.voice.example.service;

import com.tuenti.voice.example.service.IVoiceClientServiceCallback;

interface IVoiceClientService {
    void acceptCall( long callId );
    void call( String remoteJid );
    void declineCall( long callId, boolean busy );
    void toggleMute( long callId );
    void toggleHold( long callId );
    void endCall( long callId );
    void login( String username, String password, String turnUsername, String turnPassword, String xmppHost, int xmppPort, boolean xmppUseSSl );
    void release();
    void logout();
    void registerCallback( IVoiceClientServiceCallback cb );
    void unregisterCallback( IVoiceClientServiceCallback cb );
}

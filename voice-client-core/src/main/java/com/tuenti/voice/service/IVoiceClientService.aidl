package com.tuenti.voice.service;

import com.tuenti.voice.service.IVoiceClientServiceCallback;

interface IVoiceClientService {
    void acceptCall( long callId );
    void call( String remoteJid );
    void declineCall( long callId, boolean busy );
    void muteCall( long callId, boolean mute );
    void holdCall( long callId, boolean hold );
    void endCall( long callId );
    void init( String stunServer, String relayServerUdp, String relayServerTcp, String relayServerSsl, String turnServer );
    void login( String username, String password, String xmppHost, int xmppPort, boolean xmppUseSSl );
    void logout();
    void release();
    void registerCallback( IVoiceClientServiceCallback cb );
    void unregisterCallback( IVoiceClientServiceCallback cb );
}

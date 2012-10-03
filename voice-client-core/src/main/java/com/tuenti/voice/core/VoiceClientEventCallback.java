package com.tuenti.voice.core;

public interface VoiceClientEventCallback
{
// -------------------------- OTHER METHODS --------------------------

    void handleCallStateChanged( int state, String remoteJid, long callId );

    void handleXmppError( int error );
    
    void handleXmppSocketClose( int state );

    void handleXmppStateChanged( int state );

    void handleBuddyListChanged( int state, String remoteJid );
}

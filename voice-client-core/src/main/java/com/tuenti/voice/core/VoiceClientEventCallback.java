package com.tuenti.voice.core;

public interface VoiceClientEventCallback
{
// -------------------------- OTHER METHODS --------------------------

    void handleCallStateChanged( int state, String remoteJid );

    void handleXmppError( int error );

    void handleXmppStateChanged( int state );

    void handleBuddyListChanged( int state, String remoteJid );
}

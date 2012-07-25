package com.tuenti.voice;

public interface VoiceClientEventCallback
{
// -------------------------- OTHER METHODS --------------------------

    void handleCallStateChanged( int state, String remoteJid );

    void handleXmppError( int error );

    void handleXmppStateChanged( int state );
}

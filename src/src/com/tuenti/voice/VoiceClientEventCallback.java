package com.tuenti.voice;

public interface VoiceClientEventCallback
{
// -------------------------- OTHER METHODS --------------------------

    void handleCallStateChanged( int state, String remoteJid );

    void handleXmppEngineStateChanged( int state, String message );

    void handleXmppError( int error );
}

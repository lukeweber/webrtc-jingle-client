package com.tuenti.voice.core.manager;

public interface CallManager
{
// -------------------------- OTHER METHODS --------------------------

    void handleCallError( int error, long callId );

    void handleCallStateChanged( int state, String remoteJid, long callId );
}

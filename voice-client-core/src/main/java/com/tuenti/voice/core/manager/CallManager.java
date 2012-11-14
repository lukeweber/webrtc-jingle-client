package com.tuenti.voice.core.manager;

public interface CallManager
{
    void handleCallError( int error, long callId );

    void handleCallStateChanged( int state, String remoteJid, long callId );
}

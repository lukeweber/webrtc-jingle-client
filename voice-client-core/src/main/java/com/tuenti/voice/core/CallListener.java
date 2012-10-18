package com.tuenti.voice.core;

public interface CallListener
{
// -------------------------- OTHER METHODS --------------------------

    void handleCallError( int error, long callId );

    void handleCallStateChanged( int state, String remoteJid, long callId );
}

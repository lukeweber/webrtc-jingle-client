package com.tuenti.voice.core;

public interface ConnectionListener
{
// -------------------------- OTHER METHODS --------------------------

    void handleXmppError( int error );

    void handleXmppSocketClose( int state );

    void handleXmppStateChanged( int state );
}

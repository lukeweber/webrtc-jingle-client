package com.tuenti.voice.core.manager;

public interface ConnectionManager
{
// -------------------------- OTHER METHODS --------------------------

    void handleXmppError( int error );

    void handleXmppSocketClose( int state );

    void handleXmppStateChanged( int state );
}

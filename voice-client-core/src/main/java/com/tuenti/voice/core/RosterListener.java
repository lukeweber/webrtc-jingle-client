package com.tuenti.voice.core;

public interface RosterListener
{
// -------------------------- OTHER METHODS --------------------------

    void handleBuddyListChanged( int state, String remoteJid );
}

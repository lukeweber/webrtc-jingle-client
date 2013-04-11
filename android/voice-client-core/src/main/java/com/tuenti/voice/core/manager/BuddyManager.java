package com.tuenti.voice.core.manager;

public interface BuddyManager
{
// -------------------------- OTHER METHODS --------------------------

    void handleBuddyAdded( String remoteJid, String nick, int available, int show );

    void handleBuddyListChanged( int state, String remoteJid );

    void handlePresenceChanged( String remoteJid, int available, int show );
}

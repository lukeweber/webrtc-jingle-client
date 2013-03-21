package com.tuenti.voice.core.manager;

public interface BuddyManager
{
// -------------------------- OTHER METHODS --------------------------

    void handleBuddyAdded( String remoteJid, String nick, int available );

    void handleBuddyListChanged( int state, String remoteJid );
}

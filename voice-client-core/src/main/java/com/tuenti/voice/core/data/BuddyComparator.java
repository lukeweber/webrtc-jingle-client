package com.tuenti.voice.core.data;

import java.util.Comparator;

public class BuddyComparator
    implements Comparator<Buddy>
{
// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface Comparator ---------------------

    @Override
    public int compare( Buddy b1, Buddy b2 )
    {
        return b1.getRemoteJid().compareTo( b2.getRemoteJid() );
    }
}

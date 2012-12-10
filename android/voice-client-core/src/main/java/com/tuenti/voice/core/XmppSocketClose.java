package com.tuenti.voice.core;

public enum XmppSocketClose
{
    CLOSED;

// ------------------------------ FIELDS ------------------------------

    private static final XmppSocketClose[] xmppSocketCloseValues = XmppSocketClose.values();

// -------------------------- STATIC METHODS --------------------------

    public static XmppSocketClose fromInteger( int i )
    {
        return CLOSED;
    }
}

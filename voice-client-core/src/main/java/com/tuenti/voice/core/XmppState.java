package com.tuenti.voice.core;

public enum XmppState
{
    NONE,
    START,
    OPENING,
    OPEN,
    CLOSED;

// ------------------------------ FIELDS ------------------------------

    private static final XmppState[] xmppStateValues = XmppState.values();

// -------------------------- STATIC METHODS --------------------------

    public static XmppState fromInteger( int i )
    {
        return xmppStateValues[i];
    }
}

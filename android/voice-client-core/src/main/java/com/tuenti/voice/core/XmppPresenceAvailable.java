package com.tuenti.voice.core;

public enum XmppPresenceAvailable
{
    XMPP_PRESENCE_UNAVAILABLE,
    XMPP_PRESENCE_AVAILABLE,
    XMPP_PRESENCE_ERROR;

// ------------------------------ FIELDS ------------------------------

    private static final XmppPresenceAvailable[] values = XmppPresenceAvailable.values();

// -------------------------- STATIC METHODS --------------------------

    public static XmppPresenceAvailable fromInteger( int i )
    {
        return values[i];
    }
}

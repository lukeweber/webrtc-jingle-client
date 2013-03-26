package com.tuenti.voice.core;

public enum XmppPresenceShow
{
    XMPP_PRESENCE_CHAT,
    XMPP_PRESENCE_DEFAULT,
    XMPP_PRESENCE_AWAY,
    XMPP_PRESENCE_XA,
    XMPP_PRESENCE_DND;

// ------------------------------ FIELDS ------------------------------

    private static final XmppPresenceShow[] values = XmppPresenceShow.values();

// -------------------------- STATIC METHODS --------------------------

    public static XmppPresenceShow fromInteger( int i )
    {
        return values[i];
    }
}

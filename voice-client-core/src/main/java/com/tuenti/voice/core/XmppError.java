package com.tuenti.voice.core;

public enum XmppError
{
    NONE,
    XML,
    STREAM,
    VERSION,
    UNAUTHORIZED,
    TLS,
    AUTH,
    BIND,
    CONNECTION_CLOSED,
    DOCUMENT_CLOSED,
    SOCKET,
    NETWORK_TIMEOUT,
    MISSING_USERNAME;

// ------------------------------ FIELDS ------------------------------

    private static final XmppError[] xmppErrorValues = XmppError.values();

// -------------------------- STATIC METHODS --------------------------

    public static XmppError fromInteger( int i )
    {
        return xmppErrorValues[i];
    }
}

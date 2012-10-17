package com.tuenti.voice.core;

public enum CallState
{
    INIT,
    SENT_INITIATE,
    RECEIVED_INITIATE,
    RECEIVED_INITIATE_ACK,
    SENT_PR_ACCEPT,
    SENT_ACCEPT,
    RECEIVED_PR_ACCEPT,
    RECEIVED_ACCEPT,
    SENT_MODIFY,
    RECEIVED_MODIFY,
    SENT_BUSY,
    SENT_REJECT,
    RECEIVED_BUSY,
    RECEIVED_REJECT,
    SENT_REDIRECT,
    SENT_TERMINATE,
    RECEIVED_TERMINATE,
    IN_PROGRESS,
    DE_INIT;

// ------------------------------ FIELDS ------------------------------

    private static final CallState[] callStateValues = CallState.values();

// -------------------------- STATIC METHODS --------------------------

    public static CallState fromInteger( int i )
    {
        return callStateValues[i];
    }
}

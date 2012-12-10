package com.tuenti.voice.core;

public enum CallError
{
    ERROR_NONE,       // no error
    ERROR_TIME,       // no response to signaling
    ERROR_RESPONSE,   // error during signaling
    ERROR_NETWORK,    // network error, could not allocate network resources
    ERROR_CONTENT,    // channel errors in SetLocalContent/SetRemoteContent
    ERROR_TRANSPORT,  // transport error of some kind
    ERROR_ACK_TIME;   // no ack response to signaling, client not available

// ------------------------------ FIELDS ------------------------------

    private static final CallError[] callErrorValues = CallError.values();

// -------------------------- STATIC METHODS --------------------------

    public static CallError fromInteger( int i )
    {
        return callErrorValues[i];
    }
}

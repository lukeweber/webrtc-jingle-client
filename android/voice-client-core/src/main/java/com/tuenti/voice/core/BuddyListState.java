package com.tuenti.voice.core;

public enum BuddyListState
{
    ADD,
    REMOVE,
    RESET;

// ------------------------------ FIELDS ------------------------------

    private static final BuddyListState[] buddyListStateValues = BuddyListState.values();

// -------------------------- STATIC METHODS --------------------------

    public static BuddyListState fromInteger( int i )
    {
        return buddyListStateValues[i];
    }
}

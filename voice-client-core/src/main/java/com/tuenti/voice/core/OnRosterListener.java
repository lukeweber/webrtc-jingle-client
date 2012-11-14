package com.tuenti.voice.core;

import com.tuenti.voice.core.data.Buddy;

public interface OnRosterListener
{
// -------------------------- OTHER METHODS --------------------------

    void onRegisterOnRosterListener();

    void onRosterUpdated( Buddy[] buddies );

    void requestRosterUpdate();
}

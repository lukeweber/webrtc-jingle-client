package com.tuenti.voice.core;

import com.tuenti.voice.core.data.Buddy;

public interface OnBuddyListener
{
// -------------------------- OTHER METHODS --------------------------

    void onBuddyUpdated( Buddy[] buddies );

    void onRegisterBuddyListener();

    void requestBuddyUpdate();
}

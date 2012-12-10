package com.tuenti.voice.core.service;

import com.tuenti.voice.core.data.Buddy;

oneway interface IBuddyServiceCallback {

    void handleBuddyUpdated( in Buddy[] buddies );

}
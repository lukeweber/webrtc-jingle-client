package com.tuenti.voice.core.service;

import com.tuenti.voice.core.data.Buddy;

oneway interface IRosterServiceCallback {

    void handleRosterUpdated( in Buddy[] buddies );

}
package com.tuenti.voice.example.service;

import com.tuenti.voice.example.data.Buddy;

oneway interface IRosterServiceCallback {

    void handleRosterUpdated( in Buddy[] buddies );

}
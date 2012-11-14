package com.tuenti.voice.core.service;

import com.tuenti.voice.core.service.IRosterServiceCallback;

interface IRosterService {

    void requestRosterUpdate();
    void registerCallback( IRosterServiceCallback cb );
    void unregisterCallback( IRosterServiceCallback cb );

}
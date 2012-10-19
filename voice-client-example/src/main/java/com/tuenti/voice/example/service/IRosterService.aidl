package com.tuenti.voice.example.service;

import com.tuenti.voice.example.service.IRosterServiceCallback;

interface IRosterService {

    void registerCallback( IRosterServiceCallback cb );
    void unregisterCallback( IRosterServiceCallback cb );

}
package com.tuenti.voice.core.service;

import com.tuenti.voice.core.service.IStatServiceCallback;

interface IStatService {
    void registerCallback( IStatServiceCallback cb );
    void unregisterCallback( IStatServiceCallback cb );
}

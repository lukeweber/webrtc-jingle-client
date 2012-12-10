package com.tuenti.voice.core.service;

import com.tuenti.voice.core.service.IBuddyServiceCallback;

interface IBuddyService {

    void requestBuddyUpdate();
    void registerCallback( IBuddyServiceCallback cb );
    void unregisterCallback( IBuddyServiceCallback cb );

}
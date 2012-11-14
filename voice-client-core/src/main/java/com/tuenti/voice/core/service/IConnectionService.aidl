package com.tuenti.voice.core.service;

import com.tuenti.voice.core.data.User;
import com.tuenti.voice.core.service.IConnectionServiceCallback;

interface IConnectionService {

    void login( in User user );
    void logout();
    void registerCallback( IConnectionServiceCallback cb );
    void unregisterCallback( IConnectionServiceCallback cb );

}

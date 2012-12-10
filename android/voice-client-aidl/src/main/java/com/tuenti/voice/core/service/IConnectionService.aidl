package com.tuenti.voice.core.service;

import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.core.service.IConnectionServiceCallback;

interface IConnectionService {

    void login( in Connection connection );
    void logout();
    void registerCallback( IConnectionServiceCallback cb );
    void unregisterCallback( IConnectionServiceCallback cb );

}

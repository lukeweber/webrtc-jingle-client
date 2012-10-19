package com.tuenti.voice.example.service;

import com.tuenti.voice.example.data.User;
import com.tuenti.voice.example.service.IConnectionServiceCallback;

interface IConnectionService {

    void login( in User user );
    void logout();
    void registerCallback( IConnectionServiceCallback cb );
    void unregisterCallback( IConnectionServiceCallback cb );

}

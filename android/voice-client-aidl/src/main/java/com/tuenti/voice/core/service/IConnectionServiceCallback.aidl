package com.tuenti.voice.core.service;

oneway interface IConnectionServiceCallback {

    void handleLoggingIn();
    void handleLoggedIn();
    void handleLoggedOut();

}
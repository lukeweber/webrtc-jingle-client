package com.tuenti.voice.example.service;

oneway interface IConnectionServiceCallback {

    void handleLoggingIn();
    void handleLoggedIn();
    void handleLoggedOut();

}
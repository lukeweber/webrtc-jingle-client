package com.tuenti.voice.example.service;

oneway interface IConnectionServiceCallback {

    void handleLoggedIn();
    void handleLoggedOut();

}
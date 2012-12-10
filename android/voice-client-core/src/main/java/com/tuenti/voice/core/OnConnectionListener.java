package com.tuenti.voice.core;

import com.tuenti.voice.core.data.Connection;

public interface OnConnectionListener
{
// -------------------------- OTHER METHODS --------------------------

    void login( Connection connection );

    void onLoggedIn();

    void onLoggedOut();

    void onLoggingIn();
}

package com.tuenti.voice.core;

import com.tuenti.voice.core.data.User;

public interface OnConnectionListener
{
// -------------------------- OTHER METHODS --------------------------

    void login( User user );

    void onLoggedIn();

    void onLoggedOut();

    void onLoggingIn();
}

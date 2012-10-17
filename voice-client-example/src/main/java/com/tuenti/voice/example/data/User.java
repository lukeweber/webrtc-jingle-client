package com.tuenti.voice.example.data;

public class User {

    public String mUsername;
    public String mPassword;
    public String mTurnPassword;
    public String mXmppHost;
    public int mXmppPort;
    public boolean mXmppUseSsl;

    public User( String user, String password, String turnPassword, String xmppHost,
            int xmppPort, boolean xmppUseSsl){
        mUsername = user;
        mPassword = password;
        mTurnPassword = turnPassword;
        mXmppHost = xmppHost;
        mXmppPort = xmppPort;
        mXmppUseSsl = xmppUseSsl;
    }
}

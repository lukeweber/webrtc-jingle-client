package com.tuenti.voice.example.data;

public class User {

    public String mUsername;
    public String mPassword;
    public String mStun;
    public String mTurn;
    public String mTurnPassword;
    public String mXmppHost;
    public int mXmppPort;
    public boolean mXmppUseSsl;

    public User( String user, String password, String stun, String turn, String turnPassword, String xmppHost,
            int xmppPort, boolean xmppUseSsl){
        mUsername = user;
        mPassword = password;
        mStun = stun;
        mTurn = turn;
        mTurnPassword = turnPassword;
        mXmppHost = xmppHost;
        mXmppPort = xmppPort;
        mXmppUseSsl = xmppUseSsl;
    }
}

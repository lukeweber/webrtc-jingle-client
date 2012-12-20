package com.tuenti.voice.example.util;

public interface IConnectionMonitor {
    public void onConnectionEstablished();
    public void onConnectionLost();
    public void onConnectivityLost();
}

package com.tuenti.voice.example.data;

import android.os.SystemClock;

public class Call {
    private String mRemoteJid;
    private long mCallId;
    private long mTime;

    public Call(long callId){
        mCallId = callId;
    }

    public void set(String remoteJid) {
        mRemoteJid = remoteJid;
    }

    public void startCallTimer(){
        mTime = SystemClock.elapsedRealtime();
    }

    public long getElapsedTime(){
        return (SystemClock.elapsedRealtime() - mTime) / 1000;
    }
}

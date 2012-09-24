package com.tuenti.voice.example.data;

import android.os.SystemClock;

public class Call {
    private String mRemoteJid;
    private long mCallId;
    private long mTime;

    public call(long callId){
        this.callId = callId;
    }

    public void set(String remoteJid) {
        mRemoteJid = remoteJid;
    }

    public void startCallTimer(){
        mTime = elapsedRealtime();
    }

    public long getElapsedTime(){
        return (currentTimeMillis() - mTime) / 1000;
    }
}

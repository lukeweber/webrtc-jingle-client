package com.tuenti.voice.example.data;

import android.os.SystemClock;

public class Call {
    private String mRemoteJid;
    private long mCallId;
    private long mTime;
    private boolean mMute;
    private boolean mHold;

    public Call(long callId, String remoteJid) {
        mCallId = callId;
        mRemoteJid = remoteJid;
        mHold = false;
        mMute = false;
    }

    public void startCallTimer() {
        mTime = SystemClock.elapsedRealtime();
    }

    public long getElapsedTime() {
        return (SystemClock.elapsedRealtime() - mTime) / 1000;
    }

    public String getRemoteJid() {
        return mRemoteJid;
    }

    public boolean isHeld() {
        return mHold;
    }

    public boolean isMuted() {
        return mMute;
    }

    public void setHold(boolean isHeld) {
        mHold = isHeld;
    }

    public void setMute(boolean isMuted) {
        mMute = isMuted;
    }
}

package com.tuenti.voice.example.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

public class RingManager {
    private AudioManager mAudioManager;

    private Ringtone mRingerPlayer;

    private Vibrator mVibrator;
    private boolean mCallInProgress;
    private Context mContext;
    private final String TAG = "RingManager - libjingle";

    public RingManager(Context context, boolean isIncoming,
            boolean callInProgress) {
        mContext = context;
        initAudio();
        mCallInProgress = callInProgress;
        if (isIncoming) {
            ringIncoming();
        } else {
            startOutgoingRinging();
        }
    }

    private void initAudio() {
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
    }

    private synchronized void ringIncoming() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        int ringerMode = mAudioManager.getRingerMode();
        mVibrator = (Vibrator) mContext
                .getSystemService(Context.VIBRATOR_SERVICE);
        if (mCallInProgress) {
            // Notify with single vibrate.
            mVibrator.vibrate((long) 200);
        } else {
            if (AudioManager.RINGER_MODE_NORMAL == ringerMode) {
                mAudioManager.setMode(AudioManager.MODE_RINGTONE);
                ring(uri, AudioManager.STREAM_RING);
            } else if (AudioManager.RINGER_MODE_VIBRATE == ringerMode) {

                // Start immediately
                // Vibrate 400, break 200, Vibrate 400, break 1000
                long[] pattern = { 0, 400, 200, 400, 1000 };

                // Vibrate until cancelled.
                mVibrator.vibrate(pattern, 0);
            } // else RINGER_MODE_SILENT
        }
    }

    private synchronized void ring(Uri uri, int streamType) {
        mAudioManager.requestAudioFocus(null, streamType,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        try {
            if (mRingerPlayer != null) {
                mRingerPlayer.stop();
            }
            mRingerPlayer = RingtoneManager.getRingtone(mContext, uri);
            mRingerPlayer.setStreamType(streamType);
            mRingerPlayer.play();
        } catch (Exception e) {
            Log.e(TAG, "error ringing", e);
        }
    }

    private synchronized void ringOutgoing(Uri uri) {
        int streamType = AudioManager.STREAM_VOICE_CALL;
        ring(uri, streamType);
    }

    /*
     * private synchronized void playNotification() { Uri notification =
     * RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
     * ringOutgoing(notification); }
     */
    private synchronized void startOutgoingRinging() {
        Uri notification = Uri
                .parse("android.resource://com.tuenti.voice.example/raw/outgoing_call_ring");
        ringOutgoing(notification);
    }

    public synchronized void stop() {
        if (mRingerPlayer != null) {
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            mAudioManager.abandonAudioFocus(null);
            mRingerPlayer.stop();
            mRingerPlayer = null;
        }

        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
        }
    }
}

package com.tuenti.voice.core.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

public final class AudioUtil
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "AudioUtil";

    private final AudioManager mAudioManager;

    private AudioMode mAudioMode;

    private final Context mContext;

    private final OnAudioChangeListener mListener;

    private final BroadcastReceiver mReceiver = new AudioBroadcastReceiver();

    private boolean mWiredHeadsetEnabled;

// --------------------------- CONSTRUCTORS ---------------------------

    public AudioUtil( final Context context, final OnAudioChangeListener listener )
    {
        mContext = context;
        mListener = listener;
        mAudioManager = (AudioManager) context.getSystemService( Context.AUDIO_SERVICE );
        initReceiver();
    }

// -------------------------- OTHER METHODS --------------------------

    public void destroy()
    {
        // unregister the receiver
        mContext.unregisterReceiver( mReceiver );
    }

    public boolean isHeadsetOn()
    {
        return mAudioMode == AudioMode.DEVICE;
    }

    public boolean isSpeakerOn()
    {
        Log.d( TAG, " - isSpeakerphoneOn: " + mAudioManager.isSpeakerphoneOn() );
        return mAudioManager.isSpeakerphoneOn();
    }

    public void toggleSpeaker()
    {
        if ( isSpeakerOn() )
        {
            turnOnHeadset();
        }
        else
        {
            turnOnSpeaker();
        }
    }

    public void turnOnHeadset()
    {
        switchAudioMode( AudioMode.DEVICE );
    }

    public void turnOnSpeaker()
    {
        switchAudioMode( AudioMode.SPEAKER );
    }

    public void turnOnWiredHeadset()
    {
        if ( mWiredHeadsetEnabled )
        {
            switchAudioMode( AudioMode.EARPIECE );
        }
    }

    public boolean wiredHeadsetEnabled()
    {
        return mWiredHeadsetEnabled;
    }

    private void initReceiver()
    {
        IntentFilter intentFilter = new IntentFilter( Intent.ACTION_HEADSET_PLUG );
        mContext.registerReceiver( mReceiver, intentFilter );
    }

    private void switchAudioMode( AudioMode mode )
    {
        Log.d( TAG, " - switching audio to " + mode.toString() );
        mAudioManager.setWiredHeadsetOn( mode == AudioMode.EARPIECE );
        mAudioManager.setSpeakerphoneOn( mode == AudioMode.SPEAKER );
        mAudioMode = mode;
        if ( mListener != null )
        {
            mListener.onAudioChange();
        }
    }

// -------------------------- ENUMERATIONS --------------------------

    public enum AudioMode
    {
        SPEAKER, EARPIECE, BLUETOOTH, DEVICE
    }

// -------------------------- INNER CLASSES --------------------------

    public interface OnAudioChangeListener
    {
        void onAudioChange();
    }

    class AudioBroadcastReceiver
        extends BroadcastReceiver
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            String action = intent.getAction();
            if ( action.equals( Intent.ACTION_HEADSET_PLUG ) )
            {
                int state = intent.getIntExtra( "state", 0 );
                mWiredHeadsetEnabled = ( state == 1 );
                if ( mWiredHeadsetEnabled )
                {
                    turnOnWiredHeadset();
                    return;
                }
            }
            turnOnHeadset();
        }
    }
}

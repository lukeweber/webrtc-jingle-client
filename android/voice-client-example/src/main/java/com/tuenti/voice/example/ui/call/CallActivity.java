package com.tuenti.voice.example.ui.call;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.Window;
import android.media.AudioManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.actionbarsherlock.app.SherlockActivity;
import com.tuenti.voice.core.CallCallback;
import com.tuenti.voice.core.StatCallback;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.core.util.AudioUtil;
import com.tuenti.voice.core.util.WakeLockUtil;
import com.tuenti.voice.example.Intents;
import com.tuenti.voice.example.R;
import com.tuenti.voice.core.util.CallTimer;

import static android.view.View.OnClickListener;
import static android.view.WindowManager.LayoutParams;

import static com.tuenti.voice.core.util.AudioUtil.OnAudioChangeListener;
import static com.tuenti.voice.core.util.CallTimer.OnTickListener;

public class CallActivity
    extends SherlockActivity
    implements OnClickListener, OnTickListener, OnAudioChangeListener
{
// ------------------------------ FIELDS ------------------------------

    private ImageButton mAcceptButton;

    private ToggleButton mAudioButton;

    private AudioUtil mAudioUtil;

    private LinearLayout mBottomBar;

    private Call mCall;

    private CallCallback mCallCallback;

    private boolean mCallOnHold;

    private TextView mCallStateLabel;

    private CallTimer mCallTimer;

    private TextView mElapsedTime;

    private TextView mName;

    private StatCallback mStatCallback;

    private TextView mStatsTextView;

    private WakeLockUtil mWakeLockUtil;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnAudioChangeListener ---------------------

    /**
     * Updates the state of the audio button.
     */
    @Override
    public void onAudioChange()
    {
        // only use the proximity lock on headset
        if ( mAudioUtil.isHeadsetOn() )
        {
            mWakeLockUtil.startProximityLock();
        }
        else
        {
            mWakeLockUtil.stopProximityLock();
        }

        // checked the audio button
        mAudioButton.setChecked( mAudioUtil.isSpeakerOn() );
    }

// --------------------- Interface OnClickListener ---------------------

    @Override
    public void onClick( View view )
    {
        switch ( view.getId() )
        {
            case R.id.accept_btn:
                mCallCallback.acceptCall( mCall.getCallId() );
                break;
            case R.id.hang_up_btn:
                mCallCallback.endCall( mCall.getCallId() );
                finish();
                break;
            case R.id.audio_btn:
                toggleAudio();
                break;
            case R.id.mute_btn:
                mCallCallback.toggleMute( mCall.getCallId() );
                break;
            case R.id.hold_btn:
                mCallOnHold = !mCallOnHold;
                mCallStateLabel.setText( "ON HOLD" );
                mCallStateLabel.setVisibility( mCallOnHold ? View.VISIBLE : View.GONE );
                mCallCallback.toggleHold( mCall.getCallId() );
                break;
        }
    }

// --------------------- Interface OnTickListener ---------------------

    @Override
    public void onTickForCallTimeElapsed( long timeElapsed )
    {
        mElapsedTime.setText( DateUtils.formatElapsedTime( timeElapsed ) );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        mCallCallback = new CallCallback( this )
        {
            @Override
            public void handleCallInProgress()
            {
                onCallInProgress();
            }

            @Override
            public void handleIncomingCallAccepted()
            {
                onStartCall();
            }

            @Override
            public void handleIncomingCallTerminated( Call call )
            {
                onEndCall();
            }

            @Override
            public void handleOutgoingCallAccepted()
            {
                onStartCall();
            }

            @Override
            public void handleOutgoingCallTerminated( Call call )
            {
                onEndCall();
            }
        };
        mStatCallback = new StatCallback( this )
        {
            @Override
            public void handleStatsUpdate( final String stats )
            {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mStatsTextView.setText( stats );
                    }
                } );
            }
        };

        super.onCreate( savedInstanceState );

        mCall = getIntent().getParcelableExtra( Intents.EXTRA_CALL );
        mCallTimer = new CallTimer( this );
        mWakeLockUtil = new WakeLockUtil( this );
        mAudioUtil = new AudioUtil( this, this );

        LayoutParams lp = getWindow().getAttributes();
        lp.flags |= LayoutParams.FLAG_SHOW_WHEN_LOCKED | LayoutParams.FLAG_TURN_SCREEN_ON |
            LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;
        getWindow().setAttributes( lp );

        requestWindowFeature( Window.FEATURE_NO_TITLE );

        setContentView( R.layout.call_view );
        findViewById( R.id.accept_btn ).setOnClickListener( this );
        findViewById( R.id.hang_up_btn ).setOnClickListener( this );
        findViewById( R.id.audio_btn ).setOnClickListener( this );
        findViewById( R.id.mute_btn ).setOnClickListener( this );
        findViewById( R.id.hold_btn ).setOnClickListener( this );

        //mPhoto = (ImageView) findViewById( R.id.photo );
        mName = (TextView) findViewById( R.id.name );
        mElapsedTime = (TextView) findViewById( R.id.elapsed_time );
        mCallStateLabel = (TextView) findViewById( R.id.callStateLabel );
        mBottomBar = (LinearLayout) findViewById( R.id.bottom_bar );
        mAcceptButton = (ImageButton) findViewById( R.id.accept_btn );
        mAudioButton = (ToggleButton) findViewById( R.id.audio_btn );
        mStatsTextView = (TextView) findViewById( R.id.call_stats_textview );
    }

    @Override
    protected void onDestroy()
    {
        mCallCallback.unbind();
        mStatCallback.unbind();
        mWakeLockUtil.stopProximityLock();
        mAudioUtil.destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mStatCallback.bind();
        mCallCallback.bind();
        setVolumeControlStream( AudioManager.STREAM_VOICE_CALL );
        updateCallDisplay();
    }

    private void onCallInProgress()
    {
        if ( mBottomBar.getVisibility() != View.VISIBLE )
        {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    mBottomBar.setVisibility( View.VISIBLE );
                }
            } );
        }
    }

    /**
     * End a call
     */
    private void onEndCall()
    {
        mWakeLockUtil.stopProximityLock();
        mCallTimer.cancelTimer();
        finish();
    }

    /**
     * Start a call
     */
    private void onStartCall()
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                mCallTimer.startTimer( mCall );
                mCallStateLabel.setVisibility( View.GONE );
                mElapsedTime.setVisibility( View.VISIBLE );
                mBottomBar.setVisibility( View.VISIBLE );
                mAcceptButton.setVisibility( View.GONE );
                onAudioChange();
            }
        } );
    }

    /**
     * Toggle between device/speaker or wired headset/speaker when a wired headset is plugged.
     */
    private void toggleAudio()
    {
        if ( mAudioUtil.isSpeakerOn() )
        {
            if ( mAudioUtil.wiredHeadsetEnabled() )
            {
                mAudioUtil.turnOnWiredHeadset();
            }
            else
            {
                mAudioUtil.turnOnHeadset();
            }
        }
        else
        {
            mAudioUtil.turnOnSpeaker();
        }
    }

    private void updateCallDisplay()
    {
        mName.setText( mCall.getRemoteJid() );
        mCallStateLabel.setText( mCall.isIncoming() ? "INCOMING CALL" : "CALLING" );
        mCallStateLabel.setVisibility( View.VISIBLE );
        mAcceptButton.setVisibility( mCall.isIncoming() ? View.VISIBLE : View.GONE );
    }
}

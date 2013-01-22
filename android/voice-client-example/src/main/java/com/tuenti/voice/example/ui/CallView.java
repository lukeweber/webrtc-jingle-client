package com.tuenti.voice.example.ui;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.tuenti.voice.core.OnCallListener;
import com.tuenti.voice.core.OnStatListener;
import com.tuenti.voice.core.VoiceActivity;
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

public class CallView
    extends VoiceActivity
    implements OnClickListener, OnTickListener, OnCallListener, OnAudioChangeListener, OnStatListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallView";

    private ImageButton mAcceptButton;

    private ToggleButton mAudioButton;

    private AudioUtil mAudioUtil;

    private LinearLayout mBottomBar;

    private Call mCall;

    private boolean mCallOnHold;

    private TextView mCallStateLabel;

    private CallTimer mCallTimer;

    private TextView mElapsedTime;

    private TextView mName;

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

// --------------------- Interface OnCallListener ---------------------

    @Override
    public void onCallInProgress()
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

    @Override
    public void onIncomingCallAccepted()
    {
        Log.d( TAG, "onIncomingCallAccepted" );
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                startCall();
            }
        } );
    }

    @Override
    public void onIncomingCallTerminated( Call call )
    {
        endCall();
    }

    @Override
    public void onOutgoingCallAccepted()
    {
        Log.d( TAG, "onOutgoingCallAccepted" );
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                startCall();
            }
        } );
    }

    @Override
    public void onOutgoingCallTerminated( Call call )
    {
        endCall();
    }

    @Override
    public void toggleHold( long callId )
    {
        mCallOnHold = !mCallOnHold;
        mCallStateLabel.setText( "ON HOLD" );
        mCallStateLabel.setVisibility( mCallOnHold ? View.VISIBLE : View.GONE );
        super.toggleHold( callId );
    }

// --------------------- Interface OnClickListener ---------------------

    @Override
    public void onClick( View view )
    {
        switch ( view.getId() )
        {
            case R.id.accept_btn:
                acceptCall( mCall.getCallId() );
                break;
            case R.id.hang_up_btn:
                endCall( mCall.getCallId() );
                finish();
                break;
            case R.id.audio_btn:
                toggleAudio();
                break;
            case R.id.mute_btn:
                toggleMute( mCall.getCallId() );
                break;
            case R.id.hold_btn:
                toggleHold( mCall.getCallId() );
                break;
        }
    }

// --------------------- Interface OnStatListener ---------------------

    @Override
    public void onStatsUpdated( final String stats )
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

// --------------------- Interface OnTickListener ---------------------

    @Override
    public void onTickForCallTimeElapsed( long timeElapsed )
    {
        mElapsedTime.setText( DateUtils.formatElapsedTime( timeElapsed ) );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
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
        mWakeLockUtil.stopProximityLock();
        mAudioUtil.destroy();
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        updateCallDisplay();
    }

    /**
     * End a call
     */
    private void endCall()
    {
        mWakeLockUtil.stopProximityLock();
        mCallTimer.cancelTimer();
        finish();
    }

    /**
     * Start a call
     */
    private void startCall()
    {
        mCallTimer.startTimer( mCall );
        mCallStateLabel.setVisibility( View.GONE );
        mElapsedTime.setVisibility( View.VISIBLE );
        mBottomBar.setVisibility( View.VISIBLE );
        mAcceptButton.setVisibility( View.GONE );
        onAudioChange();
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

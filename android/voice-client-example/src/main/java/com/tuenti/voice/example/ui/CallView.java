package com.tuenti.voice.example.ui;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.tuenti.voice.core.OnCallListener;
import com.tuenti.voice.core.VoiceActivity;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.example.Intents;
import com.tuenti.voice.example.R;
import com.tuenti.voice.core.util.CallTimer;
import com.tuenti.voice.example.util.WakeLockManager;

import static android.view.View.OnClickListener;
import static com.tuenti.voice.core.util.CallTimer.OnTickListener;

public class CallView
    extends VoiceActivity
    implements OnClickListener, OnTickListener, OnCallListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallView";

    private ImageButton mAcceptButton;

    private LinearLayout mBottomBar;

    private Call mCall;

    private boolean mCallOnHold;

    private TextView mCallStateLabel;

    private CallTimer mCallTimer;

    private TextView mElapsedTime;

    private TextView mName;

    private WakeLockManager mWakeLock;

// ------------------------ INTERFACE METHODS ------------------------

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
                mCallStateLabel.setVisibility( View.GONE );
                mElapsedTime.setVisibility( View.VISIBLE );
                mBottomBar.setVisibility( View.VISIBLE );
                mAcceptButton.setVisibility( View.GONE );
                mCallTimer.startTimer( mCall );
            }
        } );
    }

    @Override
    public void onIncomingCallTerminated( Call call )
    {
        mCallTimer.cancelTimer();
        finish();
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
                mCallStateLabel.setVisibility( View.GONE );
                mElapsedTime.setVisibility( View.VISIBLE );
                mBottomBar.setVisibility( View.VISIBLE );
                mAcceptButton.setVisibility( View.GONE );
                mCallTimer.startTimer( mCall );
            }
        } );
    }

    @Override
    public void onOutgoingCallTerminated( Call call )
    {
        mCallTimer.cancelTimer();
        finish();
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
            case R.id.mute_btn:
                toggleMute( mCall.getCallId() );
                break;
            case R.id.hold_btn:
                toggleHold( mCall.getCallId() );
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
        super.onCreate( savedInstanceState );

        getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED );
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES );

        mCall = getIntent().getParcelableExtra( Intents.EXTRA_CALL );
        mCallTimer = new CallTimer( this );

        setContentView( R.layout.call_view );
        findViewById( R.id.accept_btn ).setOnClickListener( this );
        findViewById( R.id.hang_up_btn ).setOnClickListener( this );
        findViewById( R.id.mute_btn ).setOnClickListener( this );
        findViewById( R.id.hold_btn ).setOnClickListener( this );

        //mPhoto = (ImageView) findViewById( R.id.photo );
        mName = (TextView) findViewById( R.id.name );
        mElapsedTime = (TextView) findViewById( R.id.elapsed_time );
        mCallStateLabel = (TextView) findViewById( R.id.callStateLabel );
        mBottomBar = (LinearLayout) findViewById( R.id.bottom_bar );
        mAcceptButton = (ImageButton) findViewById( R.id.accept_btn );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mWakeLock.releaseWakeLock();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mWakeLock = new WakeLockManager( getBaseContext() );
        updateCallDisplay();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mWakeLock.releaseWakeLock();
    }

    private void updateCallDisplay()
    {
        mName.setText( mCall.getRemoteJid() );
        mCallStateLabel.setText( mCall.isIncoming() ? "INCOMING CALL" : "CALLING" );
        mCallStateLabel.setVisibility( View.VISIBLE );
        mAcceptButton.setVisibility( mCall.isIncoming() ? View.VISIBLE : View.GONE );
    }
}

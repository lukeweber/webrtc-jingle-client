package com.tuenti.voice.example.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.tuenti.voice.core.OnCallListener;
import com.tuenti.voice.core.annotations.CallListener;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.example.Intents;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.util.CallTimer;
import com.tuenti.voice.example.util.ProximitySensor;
import com.tuenti.voice.example.util.WakeLockManager;

import static android.view.View.OnClickListener;
import static com.tuenti.voice.example.util.CallTimer.OnTickListener;

@CallListener
public class CallView
    extends Activity
    implements OnClickListener, OnTickListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallView";

    private ImageButton mAcceptButton;

    private LinearLayout mBottomBar;

    private Call mCall;

    private TextView mCallStateLabel;

    private CallTimer mCallTimer;

    private TextView mElapsedTime;

    private TextView mName;

    private ProximitySensor mProximitySensor;

    // UI lock flag
    private boolean mUILocked;

    private WakeLockManager mWakeLock;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    @Override
    public void onClick( View view )
    {
        if ( !mUILocked )
        {
            switch ( view.getId() )
            {
                case R.id.accept_btn:
                    getOnCallListener().acceptCall( mCall.getCallId() );
                    break;
                case R.id.hang_up_btn:
                    getOnCallListener().endCall( mCall.getCallId() );
                    finish();
                    break;
                case R.id.mute_btn:
                    getOnCallListener().toggleMute( mCall.getCallId() );
                    break;
                case R.id.hold_btn:
                    getOnCallListener().toggleHold( mCall.getCallId() );
                    break;
            }
        }
    }

// --------------------- Interface OnTickListener ---------------------

    @Override
    public void onTickForCallTimeElapsed( long timeElapsed )
    {
        mElapsedTime.setText( DateUtils.formatElapsedTime( timeElapsed ) );
    }

// -------------------------- OTHER METHODS --------------------------

    @SuppressWarnings("UnusedDeclaration")
    public void onCallInProgress()
    {
        if ( mBottomBar.getVisibility() != View.VISIBLE )
        {
            mBottomBar.setVisibility( View.VISIBLE );
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onIncomingCallAccepted()
    {
        Log.d( TAG, "onIncomingCallAccepted" );
        mCallStateLabel.setVisibility( View.GONE );
        mElapsedTime.setVisibility( View.VISIBLE );
        mBottomBar.setVisibility( View.VISIBLE );
        mCallTimer.startTimer( mCall );
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onIncomingCallTerminated()
    {
        mCallTimer.cancelTimer();
        finish();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onOutgoingCallAccepted()
    {
        Log.d( TAG, "onOutgoingCallAccepted" );
        mCallStateLabel.setVisibility( View.GONE );
        mElapsedTime.setVisibility( View.VISIBLE );
        mBottomBar.setVisibility( View.VISIBLE );
        mAcceptButton.setVisibility( View.GONE );
        mCallTimer.startTimer( mCall );
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onOutgoingCallTerminated()
    {
        mCallTimer.cancelTimer();
        finish();
    }

    public void onProximity()
    {
        mUILocked = true;
        turnScreenOn( false );
        //mWakeLock.setWakeLockState( PARTIAL_WAKE_LOCK );
    }

    public void onUnProximity()
    {
        turnScreenOn( true );
        //mWakeLock.setWakeLockState( FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP );
        mUILocked = false;
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

        mProximitySensor.destroy();
        mProximitySensor = null;
        onUnProximity();
        mWakeLock.releaseWakeLock();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mProximitySensor = new ProximitySensor( this );
        mWakeLock = new WakeLockManager( getBaseContext() );

        updateCallDisplay();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mWakeLock.releaseWakeLock();
    }

    private OnCallListener getOnCallListener()
    {
        return (OnCallListener) this;
    }

    private void turnScreenOn( boolean on )
    {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        if ( on )
        {
            // less than 0 returns to default behavior.
            params.screenBrightness = -1;
        }
        else
        {
            // Samsung Galaxy Ace locks if you turn the screen off.
            // To be safe, we're just going to dim. Dimming more is
            // also considered off, i.e. 0.001f.
            params.screenBrightness = 0.01f;
        }
        getWindow().setAttributes( params );
    }

    private void updateCallDisplay()
    {
        mName.setText( mCall.getRemoteJid() );
        mCallStateLabel.setText( mCall.isIncoming() ? "INCOMING CALL" : "CALLING" );
        mCallStateLabel.setVisibility( View.VISIBLE );
        mAcceptButton.setVisibility( mCall.isIncoming() ? View.VISIBLE : View.GONE );
    }
}

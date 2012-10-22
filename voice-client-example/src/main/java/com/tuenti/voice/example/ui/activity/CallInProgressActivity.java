package com.tuenti.voice.example.ui.activity;

import android.os.Bundle;
import android.os.RemoteException;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.ui.AbstractVoiceClientView;
import com.tuenti.voice.example.util.ProximitySensor;
import com.tuenti.voice.example.util.WakeLockManager;

import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.PARTIAL_WAKE_LOCK;
import static android.view.View.OnClickListener;

public class CallInProgressActivity
    extends AbstractVoiceClientView
    implements OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "CallInProgressActivity";

    private Call mCall;

    private TextView mElapsedTime;

    private ProximitySensor mProximitySensor;

    // UI lock flag
    private boolean mUILocked;

    private WakeLockManager mWakeLock;

// --------------------------- CONSTRUCTORS ---------------------------

    public CallInProgressActivity()
    {
        super( false, true );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    @Override
    public void onClick( View view )
    {
        if ( !mUILocked )
        {
            try
            {
                switch ( view.getId() )
                {
                    case R.id.hang_up_btn:
                        getCallService().endCall( mCall.getCallId() );
                        finish();
                        break;
                    case R.id.mute_btn:
                        getCallService().toggleMute( mCall.getCallId() );
                        break;
                    case R.id.hold_btn:
                        getCallService().toggleHold( mCall.getCallId() );
                        break;
                }
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, e.getMessage(), e );
            }
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public void onProximity()
    {
        mUILocked = true;
        turnScreenOn( false );
        mWakeLock.setWakeLockState( PARTIAL_WAKE_LOCK );
    }

    public void onUnProximity()
    {
        turnScreenOn( true );
        mWakeLock.setWakeLockState( FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP );
        mUILocked = false;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED );
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES );

        setContentView( R.layout.callinprogress );
        findViewById( R.id.hang_up_btn ).setOnClickListener( this );
        findViewById( R.id.mute_btn ).setOnClickListener( this );
        findViewById( R.id.hold_btn ).setOnClickListener( this );

        mCall = getIntent().getParcelableExtra( "call" );

        mElapsedTime = (TextView) findViewById( R.id.duration_textview );
    }

    @Override
    protected void onOutgoingCallTerminated()
    {
        finish();
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
        changeStatus( "Talking to " + mCall.getRemoteJid() );
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mWakeLock.releaseWakeLock();
    }

    private void changeStatus( String status )
    {
        ( (TextView) findViewById( R.id.status_view ) ).setText( status );
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

    private void updateElapsedTime( long timeElapsed )
    {
        mElapsedTime.setText( DateUtils.formatElapsedTime( timeElapsed ) );
    }
}

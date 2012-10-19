package com.tuenti.voice.example.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.service.ICallService;
import com.tuenti.voice.example.service.ICallServiceCallback;
import com.tuenti.voice.example.util.ProximitySensor;
import com.tuenti.voice.example.util.WakeLockManager;

public class CallInProgressActivity
    extends Activity
    implements View.OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private final String TAG = "CallInProgressActivity";

    private TextView durationTextView;

    private Call mCall;

    private final ICallServiceCallback mCallback = new ICallServiceCallback.Stub()
    {
        @Override
        public void handleCallInProgress()
        {
        }

        @Override
        public void handleCallStarted( Call call )
        {
            mCall = call;
        }
    };

    private ProximitySensor mProximitySensor;

    private ICallService mService;

    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mService = ICallService.Stub.asInterface( service );
                mService.registerCallback( mCallback );
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceConnected", e );
            }
        }

        @Override
        public void onServiceDisconnected( ComponentName name )
        {
            try
            {
                mService.unregisterCallback( mCallback );
                mService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

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
            try
            {
                switch ( view.getId() )
                {
                    case R.id.hang_up_btn:
                        mService.endCall( mCall.getCallId() );
                        updateCallDuration( 0 );
                        finish();
                        break;
                    case R.id.mute_btn:
                        mService.toggleMute( mCall.getCallId() );
                        break;
                    case R.id.hold_btn:
                        mService.toggleHold( mCall.getCallId() );
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

    public void initClickListeners()
    {
        findViewById( R.id.hang_up_btn ).setOnClickListener( this );
        findViewById( R.id.mute_btn ).setOnClickListener( this );
        findViewById( R.id.hold_btn ).setOnClickListener( this );
    }

    public void onProximity()
    {
        mUILocked = true;
        turnScreenOn( false );
        mWakeLock.setWakeLockState( PowerManager.PARTIAL_WAKE_LOCK );
    }

    public void onUnProximity()
    {
        turnScreenOn( true );
        mWakeLock.setWakeLockState( PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP );
        mUILocked = false;
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.callinprogress );
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED );
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES );

        mCall = getIntent().getParcelableExtra( "call" );

        durationTextView = (TextView) findViewById( R.id.duration_textview );
        updateCallDuration( 0 );

        initClickListeners();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // unbind the service
        unbindService( mServiceConnection );

        mProximitySensor.destroy();
        mProximitySensor = null;
        onUnProximity();
        mWakeLock.releaseWakeLock();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.v( TAG, "onResume()" );

        // bind service
        Intent callIntent = new Intent( ICallService.class.getName() );
        bindService( callIntent, mServiceConnection, Context.BIND_AUTO_CREATE );

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

    /**
     * Updates the call duration TextView with the new duration.
     *
     * @param duration The new duration to display.
     */
    private void updateCallDuration( long duration )
    {
        if ( duration >= 0 )
        {
            long minutes = duration / 60;
            long seconds = duration % 60;
            String formattedDuration = String.format( "%02d:%02d", minutes, seconds );

            durationTextView.setText( formattedDuration );
        }
    }
}

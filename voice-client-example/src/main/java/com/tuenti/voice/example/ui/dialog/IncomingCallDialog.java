package com.tuenti.voice.example.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.service.ICallService;
import com.tuenti.voice.example.service.ICallServiceCallback;
import com.tuenti.voice.example.util.WakeLockManager;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.content.DialogInterface.OnClickListener;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

public class IncomingCallDialog
    extends Activity
    implements OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "IncomingDialog";

    private AlertDialog mAlertDialog;

    private Call mCall;

    private ICallService mCallService;

    private final ICallServiceCallback mCallServiceCallback = new ICallServiceCallback.Stub()
    {
        @Override
        public void handleCallInProgress()
        {
            finish();
        }

        @Override
        public void handleCallStarted( Call call )
        {
        }
    };

    private final ServiceConnection mCallServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mCallService = ICallService.Stub.asInterface( service );
                mCallService.registerCallback( mCallServiceCallback );
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
                mCallService.unregisterCallback( mCallServiceCallback );
                mCallService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

    private KeyguardLock mKeyguardLock;

    /*
    if ( intent.getAction().equals( CallUIIntent.CALL_PROGRESS ) ||
        intent.getAction().equals( CallUIIntent.CALL_ENDED ) ||
        intent.getAction().equals( CallUIIntent.LOGGED_OUT ) )
    {
        finish();
    }
    */

    private WakeLockManager mWakeLock;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    @Override
    public void onClick( DialogInterface dialog, int which )
    {
        try
        {
            switch ( which )
            {
                case BUTTON_POSITIVE:
                    mCallService.acceptCall( mCall.getCallId() );
                    break;
                case BUTTON_NEGATIVE:
                    mCallService.declineCall( mCall.getCallId(), false );
                    break;
            }
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }

        mAlertDialog.hide();
        finish();
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onPause()
    {
        super.onPause();

        // unbind the service
        unbindService( mCallServiceConnection );

        if ( mKeyguardLock != null )
        {
            mKeyguardLock.reenableKeyguard();
        }

        // release the WakeLock
        if ( mWakeLock != null )
        {
            mWakeLock.releaseWakeLock();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // bind service
        Intent callIntent = new Intent( ICallService.class.getName() );
        bindService( callIntent, mCallServiceConnection, Context.BIND_AUTO_CREATE );

        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService( KEYGUARD_SERVICE );
        mKeyguardLock = mKeyGuardManager.newKeyguardLock( "screenunlock" );
        mKeyguardLock.disableKeyguard();

        mWakeLock = new WakeLockManager( getBaseContext() );
        mWakeLock.setWakeLockState( FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // get the Call object
        mCall = getIntent().getParcelableExtra( "call" );

        getWindow().addFlags( FLAG_DISMISS_KEYGUARD | FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON );

        final Builder builder = new AlertDialog.Builder( this );
        builder.setTitle( R.string.voice_chat_invite );
        builder.setMessage( mCall.getRemoteJid() );
        builder.setPositiveButton( R.string.accept_call, this );
        builder.setNegativeButton( R.string.decline_call, this );
        builder.setCancelable( false );

        mAlertDialog = builder.create();
        mAlertDialog.show();
    }
}

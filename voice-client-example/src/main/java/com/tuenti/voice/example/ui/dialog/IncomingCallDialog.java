package com.tuenti.voice.example.ui.dialog;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.ui.AbstractVoiceClientView;
import com.tuenti.voice.example.util.WakeLockManager;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.content.DialogInterface.OnClickListener;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

public class IncomingCallDialog
    extends AbstractVoiceClientView
    implements OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "IncomingDialog";

    private AlertDialog mAlertDialog;

    private Call mCall;

    private KeyguardLock mKeyguardLock;

    private WakeLockManager mWakeLock;

// --------------------------- CONSTRUCTORS ---------------------------

    public IncomingCallDialog()
    {
        super( false, true );
    }

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
                    getCallService().acceptCall( mCall.getCallId() );
                    break;
                case BUTTON_NEGATIVE:
                    getCallService().declineCall( mCall.getCallId(), false );
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

        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService( KEYGUARD_SERVICE );
        mKeyguardLock = mKeyGuardManager.newKeyguardLock( "screenunlock" );
        mKeyguardLock.disableKeyguard();

        mWakeLock = new WakeLockManager( getBaseContext() );
        //mWakeLock.setWakeLockState( FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP );
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

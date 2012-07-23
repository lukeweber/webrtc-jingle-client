package com.tuenti.voice;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class VoiceClientActivity
    extends Activity
    implements View.OnClickListener, VoiceClientEventCallback
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "VoiceClientActivity";

    // Template Google Settings
    private static final String TO_USER = "user@gmail.com";

    private static final String MY_USER = "username@mydomain.com";

    private static final String MY_PASS = "pass";

    private static final String MY_XMPP_SERVER = "talk.google.com";

    private static final boolean USE_SSL = true;

    private static Vibrator mVibrator;

    private AudioManager mAudioManager;

    private VoiceClient mClient;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    public void onClick( View view )
    {
        switch ( view.getId() )
        {
            case R.id.init_btn:
                mClient.init();
                break;
            case R.id.release_btn:
                mClient.release();
                break;
            case R.id.login_btn:
                mClient.login( MY_USER, MY_PASS, MY_XMPP_SERVER, USE_SSL );
                break;
            case R.id.logout_btn:
                mClient.logout();
                break;
            case R.id.place_call_btn:
                mClient.call( TO_USER );
                break;
            case R.id.hang_up_btn:
                mClient.endCall();
                break;
            case R.id.accept_call_btn:
                mClient.acceptCall();
                break;
            case R.id.decline_call_btn:
                mClient.declineCall();
                break;
        }
    }

// --------------------- Interface VoiceClientEventCallback ---------------------

    @Override
    public void handleCallStateChanged( int state, String remoteJid )
    {
        switch ( state )
        {
            case VoiceClient.CALL_ANSWERED:
                mVibrator.cancel();
                changeStatus( "call answered" );
                break;

            case VoiceClient.CALL_CALLING:
                changeStatus( "calling..." );
                break;

            case VoiceClient.CALL_INCOMING:
                changeStatus( "incoming call from " + remoteJid );
                mVibrator.vibrate( 300 );
                mClient.acceptCall();
                break;

            case VoiceClient.CALL_RECIVEDTERMINATE:
                mClient.endCall();
                changeStatus( "Call hung up" );
                break;

            case VoiceClient.CALL_REJECTED:
                mVibrator.cancel();
                changeStatus( "call rejected" );
                break;
        }
    }

    @Override
    public void handleXmppEngineStateChanged( int state, String message )
    {
        changeStatus( message );
    }

    @Override
    public void handleXmppError( int error )
    {
        Log.e( TAG, "error code " + error );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );

        initAudio();
        initVibration();
        initClient();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mClient.destroy();
    }

    private void changeStatus( String status )
    {
        ( (TextView) findViewById( R.id.status_view ) ).setText( status );
    }

    private void initAudio()
    {
        //If it's playing audio out of the speaker, switch this to get earpiece.
        if ( mAudioManager == null )
        {
            mAudioManager = (AudioManager) getSystemService( Context.AUDIO_SERVICE );
        }

        if ( mAudioManager == null )
        {
            Log.e( TAG, "Could not change audio routing - no audio manager" );
        }
        else
        {
            mAudioManager.setMode( AudioManager.MODE_IN_CALL );
        }
    }

    private void initClient()
    {
        VoiceClientEventHandler handler = new VoiceClientEventHandler( this );

        mClient = VoiceClient.getInstance();
        mClient.setHandler( handler );

        findViewById( R.id.init_btn ).setOnClickListener( this );
        findViewById( R.id.release_btn ).setOnClickListener( this );
        findViewById( R.id.login_btn ).setOnClickListener( this );
        findViewById( R.id.logout_btn ).setOnClickListener( this );
        findViewById( R.id.place_call_btn ).setOnClickListener( this );
        findViewById( R.id.hang_up_btn ).setOnClickListener( this );
        findViewById( R.id.accept_call_btn ).setOnClickListener( this );
        findViewById( R.id.decline_call_btn ).setOnClickListener( this );
    }

    private void initVibration()
    {
        mVibrator = (Vibrator) getSystemService( Context.VIBRATOR_SERVICE );
    }
}

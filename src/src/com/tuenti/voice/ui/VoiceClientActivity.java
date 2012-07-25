package com.tuenti.voice.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.tuenti.voice.CallState;
import com.tuenti.voice.R;
import com.tuenti.voice.VoiceClient;
import com.tuenti.voice.VoiceClientEventCallback;
import com.tuenti.voice.VoiceClientEventHandler;
import com.tuenti.voice.XmppError;
import com.tuenti.voice.XmppState;
import com.tuenti.voice.ui.dialog.IncomingCallDialog;

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

    private AudioManager mAudioManager;

    private VoiceClient mClient;

    private Ringtone mRingerPlayer;

    private SharedPreferences mSettings;

    private static String cleanJid( String jid )
    {
        if ( jid == null )
        {
            return "";
        }

        int index = jid.indexOf( '/' );
        if ( index > 0 )
        {
            return jid.substring( 0, index );
        }
        return jid;
    }

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
                login();
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
        }
    }

// --------------------- Interface VoiceClientEventCallback ---------------------

    @Override
    public void handleCallStateChanged( int state, String remoteJid )
    {
        switch ( CallState.fromInteger( state ) )
        {
            case SENT_INITIATE:
                startOutgoingRinging();
                changeStatus( "calling..." );
                break;
            case SENT_TERMINATE:
                stopRinging();
                changeStatus( "call hang up" );
                break;
            case RECEIVED_INITIATE:
                displayIncomingCall( remoteJid );
                break;
            case RECEIVED_ACCEPT:
                stopRinging();
                changeStatus( "call answered" );
                break;
            case RECEIVED_REJECT:
                stopRinging();
                changeStatus( "call not answered" );
                break;
            case RECEIVED_TERMINATE:
                stopRinging();
                changeStatus( "other side hung up" );
                break;
            case IN_PROGRESS:
                setAudioForCall();
                changeStatus( "call in progress" );
                break;
            case DE_INIT:
                resetAudio();
                break;
        }
    }

    @Override
    public void handleXmppError( int error )
    {
        switch ( XmppError.fromInteger( error ) )
        {
            case XML:
                Log.e( TAG, "Malformed XML or encoding error" );
                break;
            case STREAM:
                Log.e( TAG, "XMPP stream error" );
                break;
            case VERSION:
                Log.e( TAG, "XMPP version error" );
                break;
            case UNAUTHORIZED:
                Log.e( TAG, "User is not authorized (Check your username and password)" );
                break;
            case TLS:
                Log.e( TAG, "TLS could not be negotiated" );
                break;
            case AUTH:
                Log.e( TAG, "Authentication could not be negotiated" );
                break;
            case BIND:
                Log.e( TAG, "Resource or session binding could not be negotiated" );
                break;
            case CONNECTION_CLOSED:
                Log.e( TAG, "Connection closed by output handler." );
                break;
            case DOCUMENT_CLOSED:
                Log.e( TAG, "Closed by </stream:stream>" );
                break;
            case SOCKET:
                Log.e( TAG, "Socket error" );
                break;
        }
    }

    @Override
    public void handleXmppStateChanged( int state )
    {
        switch ( XmppState.fromInteger( state ) )
        {
            case START:
                changeStatus( "connecting..." );
                break;
            case OPENING:
                changeStatus( "logging in..." );
                break;
            case OPEN:
                changeStatus( "logged in..." );
                break;
            case CLOSED:
                changeStatus( "logged out... " );
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );

        // Set default preferences
        mSettings = PreferenceManager.getDefaultSharedPreferences( this );

        initAudio();
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

    private void displayIncomingCall( String remoteJid )
    {
        // start ringing
        startIncomingRinging();

        // and display the incoming call dialog
        Dialog incomingCall = new IncomingCallDialog( this, mClient, cleanJid( remoteJid ) ).create();
        incomingCall.show();
    }

    private boolean getBooleanPref( int key, int defaultValue )
    {
        return Boolean.valueOf( getStringPref( key, defaultValue ) );
    }

    private int getIntPref( int key, int defaultValue )
    {
        return Integer.valueOf( getStringPref( key, defaultValue ) );
    }

    private String getStringPref( int key, int defaultValue )
    {
        return mSettings.getString( getString( key ), getString( defaultValue ) );
    }

    private void initAudio()
    {
        mAudioManager = (AudioManager) getSystemService( Context.AUDIO_SERVICE );
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
    }

    private void login()
    {
        String xmppHost = getStringPref( R.string.xmpp_host_key, R.string.xmpp_host_value );
        int xmppPort = getIntPref( R.string.xmpp_port_key, R.string.xmpp_port_value );
        boolean xmppUseSSL = getBooleanPref( R.string.xmpp_use_ssl_key, R.string.xmpp_use_ssl_value );
        String stunHost = getStringPref( R.string.stun_host_key, R.string.stun_host_value );
        int stunPort = getIntPref( R.string.stun_port_key, R.string.stun_port_value );
        mClient.login( MY_USER, MY_PASS, xmppHost, xmppPort, stunHost, stunPort, xmppUseSSL );
    }

    private void resetAudio()
    {
        mAudioManager.setMode( AudioManager.MODE_NORMAL );
    }

    private synchronized void ring( Uri uri )
    {
        mAudioManager.requestAudioFocus( null, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
        mAudioManager.setMode( AudioManager.MODE_RINGTONE );
        mAudioManager.setSpeakerphoneOn( true );

        try
        {
            if ( mRingerPlayer != null )
            {
                mRingerPlayer.stop();
            }
            mRingerPlayer = RingtoneManager.getRingtone( getApplicationContext(), uri );
            mRingerPlayer.play();
        }
        catch ( Exception e )
        {
            Log.e( TAG, "error ringing", e );
        }
    }

    private void setAudioForCall()
    {
        mAudioManager.setMode( ( Build.VERSION.SDK_INT < 11 )
                                   ? AudioManager.MODE_IN_CALL
                                   : AudioManager.MODE_IN_COMMUNICATION );
        mAudioManager.requestAudioFocus( null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
    }

    private synchronized void startIncomingRinging()
    {
        Uri notification = RingtoneManager.getDefaultUri( RingtoneManager.TYPE_RINGTONE );
        ring( notification );
    }

    private synchronized void startOutgoingRinging()
    {
        Uri notification = Uri.parse( "android.resource://com.tuenti.voice/raw/outgoing_call_ring" );
        ring( notification );
    }

    private synchronized void stopRinging()
    {
        if ( mRingerPlayer != null )
        {
            mAudioManager.abandonAudioFocus( null );
            mAudioManager.setMode( AudioManager.MODE_NORMAL );
            mRingerPlayer.stop();
            mRingerPlayer = null;
        }
    }
}

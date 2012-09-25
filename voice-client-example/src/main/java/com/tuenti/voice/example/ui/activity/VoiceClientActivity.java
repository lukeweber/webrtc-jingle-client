package com.tuenti.voice.example.ui.activity;

// android imports
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.VoiceClientEventCallback;
import com.tuenti.voice.core.VoiceClientEventHandler;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.ui.dialog.IncomingCallDialog;
import com.tuenti.voice.example.VoiceClientApplication;
import com.tuenti.voice.example.service.IVoiceClientService;
import com.tuenti.voice.example.service.CallIntent;

public class VoiceClientActivity
    extends Activity
    implements View.OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "VoiceClientActivity";

    // Template Google Settings
    //private static final String TO_USER = "user@gmail.com";

    //private static final String MY_USER = "username@mydomain.com";

    //private static final String MY_PASS = "pass";
    private static final String TO_USER = "luke@tuenti.com";

    private static final String MY_USER = "lukewebertest@gmail.com";

    private static final String MY_PASS = "testtester";

    private static final float ON_EAR_DISTANCE = 3.0f;

    // Service Related Methods.
    IVoiceClientService mService = null;

    private boolean mIsBound;

    private SharedPreferences mSettings;

    private long currentCallId = 0;

    private boolean callInProgress = false;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    public void onClick( View view )
    {
        if( mService == null ){
           mService = VoiceClientApplication.getService(); 
        }
        Intent intent;
        switch ( view.getId() )
        {
            case R.id.init_btn:
                try{
                    String stunServer = getStringPref( R.string.stunserver_key, R.string.stunserver_value );
                    String relayServer = getStringPref( R.string.relayserver_key, R.string.relayserver_value );
                    String turnServer = getStringPref( R.string.turnserver_key, R.string.turnserver_value );
                    mService.init( stunServer, relayServer, relayServer, relayServer, turnServer );
                } catch ( RemoteException $e ) {
                }
                break;
            case R.id.release_btn:
                try {
                    mService.release();
                } catch ( RemoteException $e ) {
                }
                break;
            case R.id.login_btn:
                try {
                    String xmppHost = getStringPref( R.string.xmpp_host_key, R.string.xmpp_host_value );
                    int xmppPort = getIntPref( R.string.xmpp_port_key, R.string.xmpp_port_value );
                    boolean xmppUseSSL = getBooleanPref( R.string.xmpp_use_ssl_key, R.string.xmpp_use_ssl_value );
                    mService.login( MY_USER, MY_PASS, xmppHost, xmppPort, xmppUseSSL);
                } catch ( RemoteException $e ) {
                }
                break;
            case R.id.logout_btn:
                    intent = new Intent(CallIntent.LOGOUT);
                    sendBroadcast(intent);
                break;
            case R.id.place_call_btn:
                    intent = new Intent(CallIntent.PLACE_CALL);
                    intent.putExtra("remoteJid", TO_USER);
                    sendBroadcast(intent);
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

        initClientWrapper();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    private void changeStatus( String status )
    {
        ( (TextView) findViewById( R.id.status_view ) ).setText( status );
    }

    private void displayIncomingCall( String remoteJid, long callId )
    {
        // start ringing
        //startIncomingRinging();

        // and display the incoming call dialog
        //Dialog incomingCall = new IncomingCallDialog( this, mClient, cleanJid( remoteJid ), callId ).create();
        //incomingCall.show();
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

    private void initClientWrapper()
    {
        findViewById( R.id.init_btn ).setOnClickListener( this );
        findViewById( R.id.release_btn ).setOnClickListener( this );
        findViewById( R.id.login_btn ).setOnClickListener( this );
        findViewById( R.id.logout_btn ).setOnClickListener( this );
        findViewById( R.id.place_call_btn ).setOnClickListener( this );
    }
}

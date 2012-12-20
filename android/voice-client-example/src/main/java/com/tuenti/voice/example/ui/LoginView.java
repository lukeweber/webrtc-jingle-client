package com.tuenti.voice.example.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import com.tuenti.voice.core.OnConnectionListener;
import com.tuenti.voice.core.VoiceActivity;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.core.service.VoiceClientService;
import com.tuenti.voice.example.R;

import static android.view.View.OnClickListener;

public class LoginView
    extends VoiceActivity
    implements OnClickListener, OnConnectionListener
{
// ------------------------------ FIELDS ------------------------------

    // Template Google Settings
    private static final String MY_USER = "";

    private static final String MY_PASS = "";

    private Handler mHandler = new Handler();

    private SharedPreferences mSettings;

// --------------------- GETTER / SETTER METHODS ---------------------

    private Connection getConnection()
    {
        Connection connection = new Connection();
        connection.setUsername( MY_USER );
        connection.setPassword( MY_PASS );
        connection.setStunHost( getStringPref( R.string.stun_server_key, R.string.stun_server_value ) );
        connection.setTurnHost( getStringPref( R.string.turn_server_key, R.string.turn_server_value ) );
        connection.setTurnUsername( getStringPref( R.string.turn_username_key, R.string.turn_username_value ) );
        connection.setTurnPassword( getStringPref( R.string.turn_password_key, R.string.turn_password_value ) );
        connection.setXmppHost( getStringPref( R.string.xmpp_host_key, R.string.xmpp_host_value ) );
        connection.setXmppPort( getIntPref( R.string.xmpp_port_key, R.string.xmpp_port_value ) );
        connection.setXmppUseSsl( getBooleanPref( R.string.xmpp_use_ssl_key, R.string.xmpp_use_ssl_value ) );
        connection.setRelayHost( getStringPref( R.string.relay_server_key, R.string.relay_server_value ) );
        return connection;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    public void onClick( View view )
    {
        login( getConnection() );
    }

// --------------------- Interface OnConnectionListener ---------------------

    @Override
    public void onLoggedIn()
    {
        changeStatus( "Logged in" );
        mHandler.postDelayed( new Runnable()
        {
            @Override
            public void run()
            {
                displayRosterView();
            }
        }, 2000 );
    }

    @Override
    public void onLoggedOut()
    {
        changeStatus( "Logged out" );
    }

    @Override
    public void onLoggingIn()
    {
        changeStatus( "Logging in" );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        // start the service
        Intent intent = new Intent( this, VoiceClientService.class );
        startService( intent );

        setContentView( R.layout.login_view );
        findViewById( R.id.login_btn ).setOnClickListener( this );

        // Set default preferences
        mSettings = PreferenceManager.getDefaultSharedPreferences( this );
    }

    private void changeStatus( final String status )
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                ( (TextView) findViewById( R.id.status_view ) ).setText( status );
            }
        } );
    }

    private void displayRosterView()
    {
        Intent intent = new Intent( this, RosterView.class );
        startActivity( intent );

        finish();
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
}

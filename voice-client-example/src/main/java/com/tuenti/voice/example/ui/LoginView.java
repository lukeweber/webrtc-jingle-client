package com.tuenti.voice.example.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import com.tuenti.voice.core.OnConnectionListener;
import com.tuenti.voice.core.annotations.ConnectionListener;
import com.tuenti.voice.core.data.User;
import com.tuenti.voice.core.service.VoiceClientService;
import com.tuenti.voice.example.R;

import static android.view.View.OnClickListener;

@ConnectionListener
public class LoginView
    extends Activity
    implements OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    // Template Google Settings
    private static final String MY_USER = "";

    private static final String MY_PASS = "";

    private Handler mHandler = new Handler();

    private SharedPreferences mSettings;

// --------------------- GETTER / SETTER METHODS ---------------------

    private User getUser()
    {
        User user = new User();
        user.setUsername( MY_USER );
        user.setPassword( MY_PASS );
        user.setTurnPassword( MY_PASS );
        user.setXmppHost( getStringPref( R.string.xmpp_host_key, R.string.xmpp_host_value ) );
        user.setXmppPort( getIntPref( R.string.xmpp_port_key, R.string.xmpp_port_value ) );
        user.setXmppUseSsl( getBooleanPref( R.string.xmpp_use_ssl_key, R.string.xmpp_use_ssl_value ) );
        user.setStunHost( getStringPref( R.string.stunserver_key, R.string.stunserver_value ) );
        user.setRelayHost( getStringPref( R.string.relayserver_key, R.string.relayserver_value ) );
        user.setTurnHost( getStringPref( R.string.turnserver_key, R.string.turnserver_value ) );
        return user;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    public void onClick( View view )
    {
        ( (OnConnectionListener) this ).login( getUser() );
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

    @SuppressWarnings("UnusedDeclaration")
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

    @SuppressWarnings("UnusedDeclaration")
    public void onLoggedOut()
    {
        changeStatus( "Logged out" );
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onLoggingIn()
    {
        changeStatus( "Logging in" );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
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

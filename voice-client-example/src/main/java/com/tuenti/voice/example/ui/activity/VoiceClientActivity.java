package com.tuenti.voice.example.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.User;
import com.tuenti.voice.example.ui.AbstractVoiceClientView;
import com.tuenti.voice.example.ui.RosterView;

import static android.view.View.OnClickListener;

public class VoiceClientActivity
    extends AbstractVoiceClientView
    implements OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    // Template Google Settings
    private static final String MY_USER = "";

    private static final String MY_PASS = "";

    private static final String TAG = "VoiceClientActivity";

    private SharedPreferences mSettings;

// --------------------------- CONSTRUCTORS ---------------------------

    public VoiceClientActivity()
    {
        super( true, false );
    }

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
        try
        {
            switch ( view.getId() )
            {
                case R.id.login_btn:
                    getConnectionService().login( getUser() );
                    break;
                case R.id.logout_btn:
                    getConnectionService().logout();
                    break;
            }
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.main );
        findViewById( R.id.login_btn ).setOnClickListener( this );
        findViewById( R.id.logout_btn ).setOnClickListener( this );

        // Set default preferences
        mSettings = PreferenceManager.getDefaultSharedPreferences( this );
    }

    @Override
    protected void onLoggedIn()
    {
        Intent intent = new Intent( this, RosterView.class );
        startActivity( intent );
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

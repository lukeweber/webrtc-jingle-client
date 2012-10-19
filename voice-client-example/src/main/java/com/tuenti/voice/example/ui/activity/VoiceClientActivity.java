package com.tuenti.voice.example.ui.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.User;
import com.tuenti.voice.example.service.IConnectionService;
import com.tuenti.voice.example.service.IConnectionServiceCallback;
import com.tuenti.voice.example.ui.RosterView;

import static android.view.View.*;

public class VoiceClientActivity
    extends Activity
    implements OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    // Template Google Settings
    private static final String MY_USER = "";

    private static final String MY_PASS = "";

    private static final String TAG = "VoiceClientActivity";

    private IConnectionService mConnectionService;

    private final IConnectionServiceCallback mConnectionServiceCallback = new IConnectionServiceCallback.Stub()
    {
        @Override
        public void handleLoggedIn()
        {
            Intent intent = new Intent( VoiceClientActivity.this, RosterView.class );
            startActivity( intent );
        }

        @Override
        public void handleLoggedOut()
        {
            changeStatus( "Logged out" );
        }
    };

    private final ServiceConnection mConnectionServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mConnectionService = IConnectionService.Stub.asInterface( service );
                mConnectionService.registerCallback( mConnectionServiceCallback );
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
                mConnectionService.unregisterCallback( mConnectionServiceCallback );
                mConnectionService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

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
        try
        {
            switch ( view.getId() )
            {
                case R.id.login_btn:
                    mConnectionService.login( getUser() );
                    break;
                case R.id.logout_btn:
                    mConnectionService.logout();
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

        // Set default preferences
        mSettings = PreferenceManager.getDefaultSharedPreferences( this );

        initClientWrapper();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // unbind the service
        unbindService( mConnectionServiceConnection );
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // bind service
        Intent connectionIntent = new Intent( IConnectionService.class.getName() );
        bindService( connectionIntent, mConnectionServiceConnection, Context.BIND_AUTO_CREATE );
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

    private void initClientWrapper()
    {
        findViewById( R.id.login_btn ).setOnClickListener( this );
        findViewById( R.id.logout_btn ).setOnClickListener( this );
    }
}

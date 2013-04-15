package com.tuenti.voice.example.ui.connection;

import android.accounts.Account;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import com.actionbarsherlock.app.SherlockActivity;
import com.github.kevinsawicki.wishlist.Toaster;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.example.R;

public class AddConnectionActivity
    extends SherlockActivity
{
// ------------------------------ FIELDS ------------------------------

    private EditText mPasswordText;

    private EditText mStunHost;

    private EditText mStunPort;

    private EditText mUsernameText;

    private EditText mXmppHost;

    private EditText mXmppPort;

    private EditText mXmppUseSsl;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.add_connection_activity );

        mUsernameText = (EditText) findViewById( R.id.username );
        mPasswordText = (EditText) findViewById( R.id.password );
        mXmppHost = setValue( R.id.xmpp_host, R.string.xmpp_host_value );
        mXmppPort = setValue( R.id.xmpp_port, R.string.xmpp_port_value );
        mXmppUseSsl = setValue( R.id.xmpp_use_ssl, R.string.xmpp_use_ssl_value );
        mStunHost = setValue( R.id.stun_host, R.string.stun_host_value );
        mStunPort = setValue( R.id.stun_port, R.string.stun_port_value );
        setValue( R.id.turn_host, R.string.turn_host_value );
        setValue( R.id.turn_username, R.string.turn_username_value );
        setValue( R.id.turn_password, R.string.turn_password_value );
        setValue( R.id.relay_host, R.string.relay_host_value );

        findViewById( R.id.login_button ).setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View v )
            {
                if ( TextUtils.isEmpty( mUsernameText.getText() ) || TextUtils.isEmpty( mPasswordText.getText() ) )
                {
                    Toaster.showLong( AddConnectionActivity.this, R.string.required_username_and_password );
                }
                else
                {
                    handleLogin();
                }
            }
        } );
    }

    private void handleLogin()
    {
        final Connection connection = new Connection();
        connection.setUsername( mUsernameText.getText().toString() );
        connection.setPassword( mPasswordText.getText().toString() );
        connection.setXmppHost( mXmppHost.getText().toString() );
        connection.setXmppPort( Integer.valueOf( mXmppPort.getText().toString() ) );
        connection.setXmppUseSsl( Boolean.valueOf( mXmppUseSsl.getText().toString() ) );
        connection.setStunHost( mStunHost.getText().toString() );
        connection.setStunPort( Integer.valueOf( mStunPort.getText().toString() ) );
        connection.setIsGtalk( true );

        AuthenticationTask mAuthenticationTask = new AuthenticationTask( this )
        {
            @Override
            public void onException( Exception e )
            {
            }

            @Override
            public void onSuccess( Account account )
            {
                setResult( RESULT_OK );
                finish();
            }
        };
        mAuthenticationTask.execute( connection );
    }

    private EditText setValue( int viewId, int valueId )
    {
        EditText input = (EditText) findViewById( viewId );
        input.setText( valueId );
        return input;
    }
}
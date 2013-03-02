package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.actionbarsherlock.app.SherlockFragment;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.example.R;

import static android.view.View.OnClickListener;

public class ServerSettingsFragment
    extends SherlockFragment
    implements OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private Account mAccount;

    private AuthenticationTask mAuthenticationTask;

    private EditText mStunHost;

    private EditText mStunPort;

    private EditText mXmppHost;

    private EditText mXmppPort;

    private EditText mXmppUseSsl;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    @Override
    public void onClick( View v )
    {
        handleLogin();
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View rootView = inflater.inflate( R.layout.server_settings_fragment, container, false );
        mXmppHost = setValue( rootView, R.id.xmpp_host, R.string.xmpp_host_value );
        mXmppPort = setValue( rootView, R.id.xmpp_port, R.string.xmpp_port_value );
        mXmppUseSsl = setValue( rootView, R.id.xmpp_use_ssl, R.string.xmpp_use_ssl_value );
        mStunHost = setValue( rootView, R.id.stun_host, R.string.stun_host_value );
        mStunPort = setValue( rootView, R.id.stun_port, R.string.stun_port_value );
        setValue( rootView, R.id.turn_host, R.string.turn_host_value );
        setValue( rootView, R.id.turn_username, R.string.turn_username_value );
        setValue( rootView, R.id.turn_password, R.string.turn_password_value );
        setValue( rootView, R.id.relay_host, R.string.relay_host_value );
        rootView.findViewById( R.id.login_button ).setOnClickListener( this );
        return rootView;
    }

    private void handleLogin()
    {
        String username = getArguments().getString( AddAccountActivity.PARAM_USERNAME );
        String password = getArguments().getString( AddAccountActivity.PARAM_PASSWORD );

        final Connection connection = new Connection();
        connection.setUsername( username );
        connection.setPassword( password );
        connection.setXmppHost( mXmppHost.getText().toString() );
        connection.setXmppPort( Integer.valueOf( mXmppPort.getText().toString() ) );
        connection.setXmppUseSsl( Boolean.valueOf( mXmppUseSsl.getText().toString() ) );
        connection.setStunHost( mStunHost.getText().toString() + ":" + mStunPort.getText().toString() );

        mAuthenticationTask = new AuthenticationTask( getActivity() )
        {
            @Override
            public void onException( Exception e )
            {
            }

            @Override
            public void onSuccess( Account account )
            {
                getActivity().finish();
            }
        };
        mAuthenticationTask.execute( connection );
    }

    private EditText setValue( View view, int viewId, int valueId )
    {
        EditText input = (EditText) view.findViewById( viewId );
        input.setText( valueId );
        return input;
    }
}

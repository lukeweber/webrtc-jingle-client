package com.tuenti.voice.example.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.actionbarsherlock.app.SherlockFragment;
import com.tuenti.voice.example.R;

public class ServerSettingsFragment
    extends SherlockFragment
{
// -------------------------- OTHER METHODS --------------------------

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View rootView = inflater.inflate( R.layout.server_settings_fragment, container, false );
        setValue( rootView, R.id.xmpp_host, R.string.xmpp_host_value );
        setValue( rootView, R.id.xmpp_port, R.string.xmpp_port_value );
        setValue( rootView, R.id.xmpp_use_ssl, R.string.xmpp_use_ssl_value );
        setValue( rootView, R.id.stun_host, R.string.stun_host_value );
        setValue( rootView, R.id.stun_port, R.string.stun_port_value );
        setValue( rootView, R.id.turn_host, R.string.turn_host_value );
        setValue( rootView, R.id.turn_username, R.string.turn_username_value );
        setValue( rootView, R.id.turn_password, R.string.turn_password_value );
        setValue( rootView, R.id.relay_host, R.string.relay_host_value );
        return rootView;
    }

    private void setValue( View view, int viewId, int valueId )
    {
        ( (EditText) view.findViewById( viewId ) ).setText( valueId );
    }
}

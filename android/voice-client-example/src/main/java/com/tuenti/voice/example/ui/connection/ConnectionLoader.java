package com.tuenti.voice.example.ui.connection;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import com.github.kevinsawicki.wishlist.AsyncLoader;
import com.tuenti.voice.core.data.Connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConnectionLoader
    extends AsyncLoader<List<Connection>>
{
// --------------------------- CONSTRUCTORS ---------------------------

    public ConnectionLoader( final Context context )
    {
        super( context );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public List<Connection> loadInBackground()
    {
        final AccountManager manager = AccountManager.get( getContext() );
        final Account[] accounts = manager.getAccountsByType( AccountConstants.ACCOUNT_TYPE );
        if ( accounts != null && accounts.length > 0 )
        {
            return getPasswordAccessibleAccounts( manager, accounts );
        }
        return Collections.emptyList();
    }

    private List<Connection> getPasswordAccessibleAccounts( final AccountManager manager, final Account[] accounts )
    {
        final List<Connection> accessible = new ArrayList<Connection>();
        for ( Account account : accounts )
        {
            try
            {
                String password = manager.getPassword( account );

                Connection connection = new Connection();
                connection.setUsername( account.name );
                connection.setPassword( password );
                connection.setXmppHost( manager.getUserData( account, "xmppHost" ) );
                connection.setXmppPort( Integer.valueOf( manager.getUserData( account, "xmppPort" ) ) );
                connection.setXmppUseSsl( Boolean.valueOf( manager.getUserData( account, "xmppUseSsl" ) ) );
                connection.setStunHost( manager.getUserData( account, "stunHost" ) );
                connection.setStunPort( Integer.valueOf( manager.getUserData( account, "stunPort" ) ) );
                connection.setTurnHost( manager.getUserData( account, "turnHost" ) );
                connection.setTurnUsername( manager.getUserData( account, "turnUsername" ) );
                connection.setTurnPassword( manager.getUserData( account, "turnPassword" ) );
                accessible.add( connection );
            }
            catch ( SecurityException ignored )
            {
                // ignore
            }
        }
        return accessible;
    }
}

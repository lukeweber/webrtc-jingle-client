package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import com.github.kevinsawicki.wishlist.AsyncLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountLoader
    extends AsyncLoader<List<Account>>
{
// --------------------------- CONSTRUCTORS ---------------------------

    public AccountLoader( final Context context )
    {
        super( context );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public List<Account> loadInBackground()
    {
        final AccountManager manager = AccountManager.get( getContext() );
        final Account[] accounts = manager.getAccountsByType( AccountConstants.ACCOUNT_TYPE );
        if ( accounts != null && accounts.length > 0 )
        {
            return getPasswordAccessibleAccounts( manager, accounts );
        }
        return Collections.emptyList();
    }

    private List<Account> getPasswordAccessibleAccounts( final AccountManager manager, final Account[] accounts )
    {
        final List<Account> accessible = new ArrayList<Account>();
        for ( Account account : accounts )
        {
            try
            {
                manager.getPassword( account );
                accessible.add( account );
            }
            catch ( SecurityException ignored )
            {
                // ignore
            }
        }
        return accessible;
    }
}

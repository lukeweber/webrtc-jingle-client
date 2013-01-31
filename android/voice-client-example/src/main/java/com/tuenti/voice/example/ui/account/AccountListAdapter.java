package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import android.view.LayoutInflater;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.example.R;

public class AccountListAdapter
    extends SingleTypeAdapter<Account>
{
// --------------------------- CONSTRUCTORS ---------------------------

    public AccountListAdapter( final LayoutInflater inflater, final Account[] accounts )
    {
        super( inflater, R.layout.account_item );
        setItems( accounts );
    }

    @Override
    protected int[] getChildViewIds()
    {
        return new int[]{R.id.account_name};
    }

    @Override
    protected void update( final int position, final Account account )
    {
        setText( 0, account.name );
    }
}

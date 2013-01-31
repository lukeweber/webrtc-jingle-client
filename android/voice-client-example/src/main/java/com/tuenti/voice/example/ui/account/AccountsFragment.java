package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import android.view.View;
import android.widget.ListView;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.example.ui.ItemListFragment;

import java.util.List;

public abstract class AccountsFragment
    extends ItemListFragment<Account>
{
// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onListItemClick( ListView l, View v, int position, long id )
    {
        Account account = (Account) l.getItemAtPosition( position );
        /*
        if ( !AccountUtils.isAccount( getActivity(), account ) )
        {
            startActivity( AccountViewActivity.createIntent( account ) );
        }
        */
    }

    @Override
    protected SingleTypeAdapter<Account> createAdapter( List<Account> items )
    {
        Account[] accounts = items.toArray( new Account[items.size()] );
        return new AccountListAdapter( getActivity().getLayoutInflater(), accounts );
    }
}

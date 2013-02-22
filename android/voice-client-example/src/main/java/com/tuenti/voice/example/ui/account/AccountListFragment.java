package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.example.ui.ItemListFragment;

import java.util.List;

public class AccountListFragment
    extends ItemListFragment<Account>
{
    @Override
    protected SingleTypeAdapter<Account> createAdapter( List<Account> items )
    {
        return new AccountListAdapter( getActivity().getLayoutInflater(), items.toArray( new Account[items.size()] ) );
    }
}

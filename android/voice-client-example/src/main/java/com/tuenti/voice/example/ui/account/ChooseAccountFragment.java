package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.ui.ItemListFragment;

import java.util.List;

public class ChooseAccountFragment
    extends ItemListFragment<Account>
{
// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface LoaderCallbacks ---------------------

    @Override
    public Loader<List<Account>> onCreateLoader( int id, Bundle bundle )
    {
        return new AccountLoader( getActivity() );
    }

// --------------------- Interface OnCreateOptionsMenuListener ---------------------

    @Override
    public void onCreateOptionsMenu( Menu menu, MenuInflater inflater )
    {
        inflater.inflate( R.menu.accounts, menu );
        super.onCreateOptionsMenu( menu, inflater );
    }

// --------------------- Interface OnOptionsItemSelectedListener ---------------------

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        if ( item.getItemId() == R.id.add_account )
        {
            Intent intent = new Intent( Settings.ACTION_ADD_ACCOUNT );
            intent.putExtra( Settings.EXTRA_AUTHORITIES, new String[]{AccountConstants.ACCOUNT_TYPE} );
            startActivityForResult( intent, 0 );
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        reloadAccounts();
    }

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setHasOptionsMenu( true );
    }

    @Override
    public void onListItemClick( ListView l, View v, int position, long id )
    {
        //Account account = (Account) l.getItemAtPosition(position);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        reloadAccounts();
    }

    @Override
    protected SingleTypeAdapter<Account> createAdapter( List<Account> items )
    {
        Account[] accounts = items.toArray( new Account[items.size()] );
        return new AccountListAdapter( getActivity().getLayoutInflater(), accounts );
    }

    private void reloadAccounts()
    {
        refresh();
    }
}

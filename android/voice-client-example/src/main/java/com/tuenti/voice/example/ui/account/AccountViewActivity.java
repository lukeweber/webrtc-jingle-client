package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.example.R;

import java.util.List;

import static com.actionbarsherlock.app.ActionBar.NAVIGATION_MODE_LIST;

public class AccountViewActivity
    extends SherlockFragmentActivity
    implements LoaderManager.LoaderCallbacks<List<Connection>>
{
// -------------------------- STATIC METHODS --------------------------

    public static Intent createIntent( Account account )
    {
        return null;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface LoaderCallbacks ---------------------

    @Override
    public Loader<List<Connection>> onCreateLoader( int i, Bundle bundle )
    {
        /*
        return new OrganizationLoader(this, accountDataManager,
                                      userComparatorProvider);
                                      */
        return null;
    }

    @Override
    public void onLoadFinished( Loader<List<Connection>> loader, List<Connection> connections )
    {
        configureActionBar();
    }

    @Override
    public void onLoaderReset( Loader<List<Connection>> loader )
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public boolean onCreateOptionsMenu( Menu optionMenu )
    {
        getSupportMenuInflater().inflate( R.menu.accounts, optionMenu );
        return super.onCreateOptionsMenu( optionMenu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch ( item.getItemId() )
        {
            case R.id.add_account:
                onAddAccount();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private void onAddAccount()
    {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        getSupportLoaderManager().initLoader( 0, null, this );
    }

    private void configureActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled( false );
        actionBar.setDisplayShowTitleEnabled( false );
        actionBar.setNavigationMode( NAVIGATION_MODE_LIST );
    }
}

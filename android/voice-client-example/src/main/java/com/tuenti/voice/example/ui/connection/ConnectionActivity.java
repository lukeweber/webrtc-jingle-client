package com.tuenti.voice.example.ui.connection;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.core.ConnectionCallback;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.service.AuthenticatedVoiceClientService;
import com.tuenti.voice.example.ui.ItemListActivity;
import com.tuenti.voice.example.ui.buddy.BuddyActivity;

import java.util.List;

public class ConnectionActivity
    extends ItemListActivity<Connection>
{
// ------------------------------ FIELDS ------------------------------

    private ConnectionCallback mConnectionCallback;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface LoaderCallbacks ---------------------

    @Override
    public Loader<List<Connection>> onCreateLoader( int id, Bundle bundle )
    {
        return new ConnectionLoader( this );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getSupportMenuInflater().inflate( R.menu.accounts, menu );
        //loginItem = optionMenu.findItem(id.m_login);
        return true;
    }

    @Override
    public void onListItemClick( ListView l, View v, int position, long id )
    {
        Connection connection = (Connection) l.getItemAtPosition( position );
        mConnectionCallback.login( connection );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        if ( item.getItemId() == R.id.add_account )
        {
            Intent intent = new Intent( this, AddConnectionActivity.class );
            startActivityForResult( intent, 0 );
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mConnectionCallback.bind();
        refresh();
    }

    @Override
    protected SingleTypeAdapter<Connection> createAdapter( List<Connection> items )
    {
        Connection[] connections = items.toArray( new Connection[items.size()] );
        return new ConnectionListAdapter( getLayoutInflater(), connections );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        mConnectionCallback = new ConnectionCallback( this )
        {
            @Override
            public void handleLoggedIn()
            {
                displayRosterView();
            }
        };

        super.onCreate( savedInstanceState );
    }

    @Override
    protected void onPause()
    {
        mConnectionCallback.unbind();
        super.onPause();
    }

    private void displayRosterView()
    {
        // start the service
        Intent intent = new Intent( this, AuthenticatedVoiceClientService.class );
        startService( intent );

        // now start the roster view
        intent = new Intent( this, BuddyActivity.class );
        startActivity( intent );
    }
}

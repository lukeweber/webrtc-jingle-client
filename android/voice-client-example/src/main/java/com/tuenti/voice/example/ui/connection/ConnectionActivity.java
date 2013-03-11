package com.tuenti.voice.example.ui.connection;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
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

import static com.tuenti.voice.example.Intents.EXTRA_CONNECTION;

public class ConnectionActivity
    extends ItemListActivity<Connection>
{
// ------------------------------ FIELDS ------------------------------

    private Connection mConnection;

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
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public void onListItemClick( ListView l, View v, int position, long id )
    {
        mConnection = (Connection) l.getItemAtPosition( position );
        if ( mConnection.getPresenceId() == R.string.presence_available )
        {
            displayRosterView();
        }
        else
        {
            mConnectionCallback.login( mConnection );
        }
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch ( item.getItemId() )
        {
            case R.id.menu_item_add_account:
                Intent intent = new Intent( this, AddConnectionActivity.class );
                startActivityForResult( intent, 0 );
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
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
        // set the presence
        for ( Connection connection : items )
        {
            if ( mConnection != null && connection.getUsername().equals( mConnection.getUsername() ) )
            {
                connection.setPresenceId( R.string.presence_available );
            }
            else
            {
                connection.setPresenceId( R.string.presence_offline );
            }
        }

        // now create the adapter
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

        mConnection = getParcelableExtra( EXTRA_CONNECTION );

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle( R.string.accounts );
        actionBar.setDisplayHomeAsUpEnabled( false );
    }

    @Override
    protected void onPause()
    {
        mConnectionCallback.unbind();
        mConnection = null;
        super.onPause();
    }

    private void displayRosterView()
    {
        // start the service
        Intent intent = new Intent( this, AuthenticatedVoiceClientService.class );
        startService( intent );

        // now start the roster view
        intent = new Intent( this, BuddyActivity.class );
        intent.putExtra( EXTRA_CONNECTION, mConnection );
        startActivity( intent );

        // end this activity
        finish();
    }
}

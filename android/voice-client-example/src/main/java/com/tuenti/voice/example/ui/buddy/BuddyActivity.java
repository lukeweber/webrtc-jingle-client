package com.tuenti.voice.example.ui.buddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.core.BuddyCallback;
import com.tuenti.voice.core.CallCallback;
import com.tuenti.voice.core.ConnectionCallback;
import com.tuenti.voice.core.XmppPresenceAvailable;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.service.AuthenticatedVoiceClientService;
import com.tuenti.voice.example.ui.call.CallActivity;
import com.tuenti.voice.example.ui.ItemListActivity;
import com.tuenti.voice.example.ui.connection.ConnectionActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static com.tuenti.voice.example.Intents.EXTRA_CALL;
import static com.tuenti.voice.example.Intents.EXTRA_CONNECTION;

public class BuddyActivity
    extends ItemListActivity<Buddy>
{
// ------------------------------ FIELDS ------------------------------

    private BuddyCallback mBuddyCallback;

    private CallCallback mCallCallback;

    private ConnectionCallback mConnectionCallback;

    private Call mCurrentCall;

    private Connection mCurrentConnection;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface LoaderCallbacks ---------------------

    @Override
    public Loader<List<Buddy>> onCreateLoader( int id, Bundle args )
    {
        return new BuddyLoader( this, mItems );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getSupportMenuInflater().inflate( R.menu.buddies, menu );
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public void onListItemClick( ListView l, View v, int position, long id )
    {
        Buddy buddy = (Buddy) l.getItemAtPosition( position );
        if ( XmppPresenceAvailable.XMPP_PRESENCE_AVAILABLE.equals( buddy.getAvailable() ) )
        {
            mCallCallback.call( buddy.getRemoteJid() );
        }
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch ( item.getItemId() )
        {
            case android.R.id.home:
                displayHome( mCurrentConnection );
                return true;
            case R.id.menu_item_sign_out:
                mConnectionCallback.logout();
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
        mBuddyCallback.bind();
        mCallCallback.bind();
    }

    @Override
    protected SingleTypeAdapter<Buddy> createAdapter( List<Buddy> items )
    {
        // sort items first
        Collections.sort( items, new Comparator<Buddy>()
        {
            @Override
            public int compare( Buddy o1, Buddy o2 )
            {
                if ( !o1.getAvailable().equals( o2.getAvailable() ) )
                {
                    if ( XmppPresenceAvailable.XMPP_PRESENCE_AVAILABLE.equals( o1.getAvailable() ) )
                    {
                        return -1;
                    }
                    return 1;
                }
                return o1.getName().toLowerCase().compareTo( o2.getName().toLowerCase() );
            }
        } );

        Buddy[] buddies = items.toArray( new Buddy[items.size()] );
        return new BuddyListAdapter( getLayoutInflater(), buddies );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        mConnectionCallback = new ConnectionCallback( this )
        {
            @Override
            public void handleLoggedOut()
            {
                displayHome( null );
            }
        };
        mBuddyCallback = new BuddyCallback( this )
        {
            @Override
            public void onServiceConnected()
            {
                requestBuddyUpdate();
            }

            @Override
            public void handleBuddyUpdated( final Buddy[] buddies )
            {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mItems = Arrays.asList( buddies );
                        refresh();
                    }
                } );
            }
        };
        mCallCallback = new CallCallback( this )
        {
            @Override
            public void handleIncomingCall( Call call )
            {
                if ( mCurrentCall != null )
                {
                    declineCall( call.getCallId(), true );
                }
                else
                {
                    mCurrentCall = call;

                    Intent intent = new Intent( BuddyActivity.this, CallActivity.class );
                    intent.putExtra( EXTRA_CALL, mCurrentCall );
                    startActivityForResult( intent, 0 );
                }
            }

            @Override
            public void handleIncomingCallTerminated( Call call )
            {
                if ( mCurrentCall != null && call != null && mCurrentCall.getCallId() == call.getCallId() )
                {
                    mCurrentCall = null;
                }
            }

            @Override
            public void handleOutgoingCall( Call call )
            {
                mCurrentCall = call;

                Intent intent = new Intent( BuddyActivity.this, CallActivity.class );
                intent.putExtra( EXTRA_CALL, mCurrentCall );
                startActivityForResult( intent, 0 );
            }

            @Override
            public void handleOutgoingCallTerminated( Call call )
            {
                if ( mCurrentCall != null && call != null && mCurrentCall.getCallId() == call.getCallId() )
                {
                    mCurrentCall = null;
                }
            }
        };

        super.onCreate( savedInstanceState );

        mCurrentConnection = getParcelableExtra( EXTRA_CONNECTION );

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle( mCurrentConnection.getUsername() );
        actionBar.setDisplayHomeAsUpEnabled( true );
    }

    @Override
    protected void onPause()
    {
        mCallCallback.unbind();
        mBuddyCallback.unbind();
        mConnectionCallback.unbind();
        super.onPause();
    }

    private void displayHome( Connection connection )
    {
        if ( connection == null )
        {
            // stop the service
            Intent intent = new Intent( this, AuthenticatedVoiceClientService.class );
            stopService( intent );
        }

        // go back to the homepage
        Intent intent = new Intent( this, ConnectionActivity.class );
        intent.putExtra( EXTRA_CONNECTION, connection );
        intent.addFlags( FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP );
        startActivity( intent );

        finish();
    }
}

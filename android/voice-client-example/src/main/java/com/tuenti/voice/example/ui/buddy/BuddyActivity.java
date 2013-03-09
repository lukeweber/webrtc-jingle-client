package com.tuenti.voice.example.ui.buddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.tuenti.voice.core.BuddyCallback;
import com.tuenti.voice.core.CallCallback;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.data.Call;
import com.tuenti.voice.example.ui.call.CallActivity;
import com.tuenti.voice.example.ui.ItemListActivity;

import java.util.Arrays;
import java.util.List;

import static com.tuenti.voice.example.Intents.EXTRA_CALL;

public class BuddyActivity
    extends ItemListActivity<Buddy>
{
// ------------------------------ FIELDS ------------------------------

    private BuddyCallback mBuddyCallback;

    private CallCallback mCallCallback;

    private Call mCurrentCall;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface LoaderCallbacks ---------------------

    @Override
    public Loader<List<Buddy>> onCreateLoader( int id, Bundle args )
    {
        return new BuddyLoader( this, mItems );
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onListItemClick( ListView l, View v, int position, long id )
    {
        Buddy buddy = (Buddy) l.getItemAtPosition( position );
        mCallCallback.call( buddy.getRemoteJid() );
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mBuddyCallback.bind();
        mCallCallback.bind();
    }

    @Override
    protected SingleTypeAdapter<Buddy> createAdapter( List<Buddy> items )
    {
        Buddy[] buddies = items.toArray( new Buddy[items.size()] );
        return new BuddyListAdapter( getLayoutInflater(), buddies );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
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
    }

    @Override
    protected void onPause()
    {
        mCallCallback.unbind();
        mBuddyCallback.unbind();
        super.onPause();
    }
}

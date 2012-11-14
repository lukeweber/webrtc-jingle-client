package com.tuenti.voice.example.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.tuenti.voice.core.OnCallListener;
import com.tuenti.voice.core.OnRosterListener;
import com.tuenti.voice.core.annotations.CallListener;
import com.tuenti.voice.core.annotations.RosterListener;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.data.Call;

import static android.widget.AdapterView.OnItemClickListener;
import static com.tuenti.voice.example.Intents.EXTRA_CALL;

@RosterListener
@CallListener
public class RosterView
    extends ListActivity
    implements OnItemClickListener
{
// ------------------------------ FIELDS ------------------------------

    private RosterAdapter mAdapter;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnItemClickListener ---------------------

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        Buddy buddy = mAdapter.getItem( position );
        getOnCallListener().call( buddy.getRemoteJid() );
    }

// -------------------------- OTHER METHODS --------------------------

    @SuppressWarnings("UnusedDeclaration")
    public void onIncomingCall( final Call call )
    {
        Intent intent = new Intent( this, CallView.class );
        intent.putExtra( EXTRA_CALL, call );
        startActivityForResult( intent, 0 );
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onOutgoingCall( final Call call )
    {
        Intent intent = new Intent( this, CallView.class );
        intent.putExtra( EXTRA_CALL, call );
        startActivityForResult( intent, 0 );
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onRegisterOnRosterListener()
    {
        getOnRosterListener().requestRosterUpdate();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onRosterUpdated( final Buddy[] buddies )
    {
        mAdapter = new RosterAdapter( this, buddies );
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                setListAdapter( mAdapter );
            }
        } );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        getListView().setOnItemClickListener( this );
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    private OnCallListener getOnCallListener()
    {
        return (OnCallListener) this;
    }

    private OnRosterListener getOnRosterListener()
    {
        return (OnRosterListener) this;
    }
}

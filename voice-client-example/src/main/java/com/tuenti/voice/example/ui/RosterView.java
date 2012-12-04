package com.tuenti.voice.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.tuenti.voice.core.OnBuddyListener;
import com.tuenti.voice.core.OnCallListener;
import com.tuenti.voice.core.VoiceListActivity;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.data.Call;

import static android.widget.AdapterView.OnItemClickListener;
import static com.tuenti.voice.example.Intents.EXTRA_CALL;

public class RosterView
    extends VoiceListActivity
    implements OnItemClickListener, OnCallListener, OnBuddyListener
{
// ------------------------------ FIELDS ------------------------------

    private RosterAdapter mAdapter;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnBuddyListener ---------------------

    @Override
    public void onRegisterBuddyListener()
    {
        requestBuddyUpdate();
    }

    @Override
    public void onBuddyUpdated( final Buddy[] buddies )
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

// --------------------- Interface OnCallListener ---------------------

    @Override
    public void onIncomingCall( final Call call )
    {
        Intent intent = new Intent( this, CallView.class );
        intent.putExtra( EXTRA_CALL, call );
        startActivityForResult( intent, 0 );
    }

    @Override
    public void onOutgoingCall( final Call call )
    {
        Intent intent = new Intent( this, CallView.class );
        intent.putExtra( EXTRA_CALL, call );
        startActivityForResult( intent, 0 );
    }

// --------------------- Interface OnItemClickListener ---------------------

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        Buddy buddy = mAdapter.getItem( position );
        call( buddy.getRemoteJid() );
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        getListView().setOnItemClickListener( this );
    }
}

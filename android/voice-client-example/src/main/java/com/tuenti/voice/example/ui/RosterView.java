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
import com.tuenti.voice.core.service.VoiceClientService;

import static android.widget.AdapterView.OnItemClickListener;
import static com.tuenti.voice.example.Intents.EXTRA_CALL;

public class RosterView
    extends VoiceListActivity
    implements OnItemClickListener, OnCallListener, OnBuddyListener
{
// ------------------------------ FIELDS ------------------------------

    private RosterAdapter mAdapter;

    private Call mCurrentCall;

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnBuddyListener ---------------------

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

    @Override
    public void onRegisterBuddyListener()
    {
        requestBuddyUpdate();
    }

// --------------------- Interface OnCallListener ---------------------

    @Override
    public void onIncomingCall( final Call call )
    {
        if ( mCurrentCall != null )
        {
            declineCall( call.getCallId(), true );
        }
        else
        {
            mCurrentCall = call;

            Intent intent = new Intent( this, CallView.class );
            intent.putExtra( EXTRA_CALL, mCurrentCall );
            startActivityForResult( intent, 0 );
        }
    }

    @Override
    public void onIncomingCallTerminated( final Call call )
    {
        if ( mCurrentCall != null && call != null && mCurrentCall.getCallId() == call.getCallId() )
        {
            mCurrentCall = null;
        }
    }

    @Override
    public void onOutgoingCall( final Call call )
    {
        mCurrentCall = call;

        Intent intent = new Intent( this, CallView.class );
        intent.putExtra( EXTRA_CALL, mCurrentCall );
        startActivityForResult( intent, 0 );
    }

    @Override
    public void onOutgoingCallTerminated( final Call call )
    {
        if ( mCurrentCall != null && call != null && mCurrentCall.getCallId() == call.getCallId() )
        {
            mCurrentCall = null;
        }
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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        // stop the service
        Intent intent = new Intent( this, VoiceClientService.class );
        stopService( intent );
    }
}

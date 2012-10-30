package com.tuenti.voice.example.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.tuenti.voice.example.data.Buddy;
import com.tuenti.voice.example.data.Call;

import static android.widget.AdapterView.OnItemClickListener;
import static com.tuenti.voice.example.Intents.EXTRA_CALL;

public class RosterView
    extends AbstractVoiceClientListView
    implements OnItemClickListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "RosterView";

    private RosterAdapter mAdapter;

// --------------------------- CONSTRUCTORS ---------------------------

    public RosterView()
    {
        super( false, true, true );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnItemClickListener ---------------------

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        try
        {
            Buddy buddy = mAdapter.getItem( position );
            getCallService().call( buddy.getRemoteJid() );
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
        
        /*
        if ( buddy.isOnline() )
        {
            Intent intent = new Intent( this, CallView.class );
            intent.putExtra( Intents.EXTRA_ROSTER_ITEM, item );
            startActivityForResult( intent, 1 );
        }
        */
    }

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        getListView().setOnItemClickListener( this );
    }

    @Override
    protected void onIncomingCall( final Call call )
    {
        Intent intent = new Intent( this, CallView.class );
        intent.putExtra( EXTRA_CALL, call );
        startActivityForResult( intent, 0 );
    }

    @Override
    protected void onOutgoingCall( final Call call )
    {
        Intent intent = new Intent( this, CallView.class );
        intent.putExtra( EXTRA_CALL, call );
        startActivityForResult( intent, 0 );
    }

    @Override
    protected void onRosterServiceConnected()
    {
        try
        {
            getRosterService().requestRosterUpdate();
        }
        catch ( RemoteException e )
        {
            Log.e( TAG, e.getMessage(), e );
        }
    }

    @Override
    protected void onRosterUpdated( final Buddy[] buddies )
    {
        mAdapter = new RosterAdapter( getLayoutInflater(), buddies );
        setListAdapter( mAdapter );
    }
}

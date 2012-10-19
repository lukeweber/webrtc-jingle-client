package com.tuenti.voice.example.ui;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.tuenti.voice.example.data.Buddy;
import com.tuenti.voice.example.service.ICallService;
import com.tuenti.voice.example.service.IRosterService;
import com.tuenti.voice.example.service.IRosterServiceCallback;

import static android.widget.AdapterView.OnItemClickListener;

public class RosterView
    extends ListActivity
    implements OnItemClickListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "RosterView";

    private RosterAdapter mAdapter;

    private ICallService mCallService;

    private ServiceConnection mCallServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            mCallService = ICallService.Stub.asInterface( service );
        }

        @Override
        public void onServiceDisconnected( ComponentName name )
        {
            mCallService = null;
        }
    };

    private IRosterService mRosterService;

    private final IRosterServiceCallback.Stub mRosterServiceCallback = new IRosterServiceCallback.Stub()
    {
        @Override
        public void handleRosterUpdated( final Buddy[] buddies )
        {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    mAdapter = new RosterAdapter( getLayoutInflater(), buddies );
                    setListAdapter( mAdapter );
                }
            } );
        }
    };

    private final ServiceConnection mRosterServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected( ComponentName name, IBinder service )
        {
            try
            {
                mRosterService = IRosterService.Stub.asInterface( service );
                mRosterService.registerCallback( mRosterServiceCallback );
                mRosterService.requestRosterUpdate();
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceConnected", e );
            }
        }

        @Override
        public void onServiceDisconnected( ComponentName name )
        {
            try
            {
                mRosterService.unregisterCallback( mRosterServiceCallback );
                mRosterService = null;
            }
            catch ( RemoteException e )
            {
                Log.e( TAG, "Error on ServiceConnection.onServiceDisconnected", e );
            }
        }
    };

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnItemClickListener ---------------------

    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id )
    {
        try
        {
            Buddy buddy = mAdapter.getItem( position );
            mCallService.call( buddy.getRemoteJid() );
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
    protected void onPause()
    {
        super.onPause();

        // unbind the service
        unbindService( mRosterServiceConnection );
        unbindService( mCallServiceConnection );
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // bind service
        Intent rosterIntent = new Intent( IRosterService.class.getName() );
        bindService( rosterIntent, mRosterServiceConnection, Context.BIND_AUTO_CREATE );
        Intent callIntent = new Intent( ICallService.class.getName() );
        bindService( callIntent, mCallServiceConnection, Context.BIND_AUTO_CREATE );
    }
}

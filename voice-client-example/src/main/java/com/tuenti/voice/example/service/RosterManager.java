package com.tuenti.voice.example.service;

import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.RosterListener;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.example.data.Buddy;

import java.util.LinkedHashMap;

public class RosterManager
    implements RosterListener
{
// ------------------------------ FIELDS ------------------------------

    private static final Object mLock = new Object();

    private static final String TAG = "RosterManager";

    private final IRosterService.Stub mBinder = new IRosterService.Stub()
    {
        @Override
        public void requestRosterUpdate()
        {
            handleRequestRosterUpdate();
        }

        @Override
        public void registerCallback( IRosterServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.register( cb );
            }
        }

        @Override
        public void unregisterCallback( IRosterServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.unregister( cb );
            }
        }
    };

    private LinkedHashMap<String, Buddy> mBuddies = new LinkedHashMap<String, Buddy>();

    private final RemoteCallbackList<IRosterServiceCallback> mCallbacks =
        new RemoteCallbackList<IRosterServiceCallback>();

    private VoiceClient mClient;

// --------------------------- CONSTRUCTORS ---------------------------

    public RosterManager( VoiceClient client )
    {
        mClient = client;
        mClient.addRosterListener( this );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface RosterListener ---------------------

    @Override
    public void handleBuddyListChanged( int state, String remoteJid )
    {
        switch ( BuddyListState.fromInteger( state ) )
        {
            case ADD:
                handleBuddyAdded( remoteJid );
                break;
            case REMOVE:
                handleBuddyRemoved( remoteJid );
                break;
            case RESET:
                handleBuddyReset();
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind()
    {
        return mBinder;
    }

    private void broadcastRosterUpdate()
    {
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleRosterUpdated( getBuddies() );
            }
            catch ( RemoteException e )
            {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mCallbacks.finishBroadcast();
    }

    private Buddy[] getBuddies()
    {
        return mBuddies.values().toArray( new Buddy[mBuddies.size()] );
    }

    private void handleBuddyAdded( String remoteJid )
    {
        if ( mBuddies.containsKey( remoteJid ) )
        {
            return;
        }

        Log.d( TAG, "buddy added " + remoteJid );
        synchronized ( mLock )
        {
            Buddy buddy = new Buddy();
            buddy.setRemoteJid( remoteJid );
            mBuddies.put( remoteJid, buddy );
        }

        broadcastRosterUpdate();
    }

    private void handleBuddyRemoved( String remoteJid )
    {
        if ( !mBuddies.containsKey( remoteJid ) )
        {
            return;
        }

        Log.d( TAG, "buddy removed " + remoteJid );
        synchronized ( mLock )
        {
            mBuddies.remove( remoteJid );
        }

        broadcastRosterUpdate();
    }

    private void handleBuddyReset()
    {
        Log.d( TAG, "handleBuddyReset" );
        synchronized ( mLock )
        {
            mBuddies.clear();
        }
        broadcastRosterUpdate();
    }

    private void handleRequestRosterUpdate()
    {
        Log.d( TAG, "handleRequestRosterUpdate" );
        broadcastRosterUpdate();
    }
}

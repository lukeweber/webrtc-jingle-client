package com.tuenti.voice.core.manager.aidl;

import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.data.BuddyComparator;
import com.tuenti.voice.core.manager.RosterManager;
import com.tuenti.voice.core.service.IRosterService;
import com.tuenti.voice.core.service.IRosterServiceCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class RosterManagerImpl
    implements RosterManager
{
// ------------------------------ FIELDS ------------------------------

    private static final Object mLock = new Object();

    private static final String TAG = "RosterManager";

    private BuddyComparator comparator = new BuddyComparator();

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

    public RosterManagerImpl( VoiceClient client )
    {
        mClient = client;
        mClient.setRosterManager( this );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface RosterManager ---------------------

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

    private void dispatchCallback()
    {
        List<Buddy> values = new ArrayList<Buddy>( mBuddies.values() );
        Collections.sort( values, comparator );

        final Buddy[] buddies = values.toArray( new Buddy[values.size()] );
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleRosterUpdated( buddies );
            }
            catch ( RemoteException e )
            {
                // NOOP
            }
        }
        mCallbacks.finishBroadcast();
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

        dispatchCallback();
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

        dispatchCallback();
    }

    private void handleBuddyReset()
    {
        Log.d( TAG, "handleBuddyReset" );
        synchronized ( mLock )
        {
            mBuddies.clear();
        }
        dispatchCallback();
    }

    private void handleRequestRosterUpdate()
    {
        Log.d( TAG, "handleRequestRosterUpdate" );
        dispatchCallback();
    }
}

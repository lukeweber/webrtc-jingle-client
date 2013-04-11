package com.tuenti.voice.core.manager;

import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.XmppPresenceAvailable;
import com.tuenti.voice.core.XmppPresenceShow;
import com.tuenti.voice.core.data.Buddy;
import com.tuenti.voice.core.data.BuddyComparator;
import com.tuenti.voice.core.service.IBuddyService;
import com.tuenti.voice.core.service.IBuddyServiceCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class BuddyManagerImpl
    implements BuddyManager
{
// ------------------------------ FIELDS ------------------------------

    private static final Object mLock = new Object();

    private static final String TAG = "BuddyManager";

    private BuddyComparator comparator = new BuddyComparator();

    private final IBuddyService.Stub mBinder = new IBuddyService.Stub()
    {
        @Override
        public void requestBuddyUpdate()
        {
            handleRequestBuddyUpdate();
        }

        @Override
        public void registerCallback( IBuddyServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.register( cb );
            }
        }

        @Override
        public void unregisterCallback( IBuddyServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.unregister( cb );
            }
        }
    };

    private LinkedHashMap<String, Buddy> mBuddies = new LinkedHashMap<String, Buddy>();

    private final RemoteCallbackList<IBuddyServiceCallback> mCallbacks =
        new RemoteCallbackList<IBuddyServiceCallback>();

    private VoiceClient mClient;

// --------------------------- CONSTRUCTORS ---------------------------

    public BuddyManagerImpl( VoiceClient client )
    {
        mClient = client;
        mClient.setBuddyManager( this );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface BuddyManager ---------------------


    @Override
    public void handleBuddyAdded( String remoteJid, String nick, int available, int show )
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
            buddy.setNick( nick );
            buddy.setAvailable( XmppPresenceAvailable.fromInteger( available ));
            buddy.setShow( XmppPresenceShow.fromInteger( show ) );
            mBuddies.put( remoteJid, buddy );
        }

        dispatchCallback();
    }

    @Override
    public void handleBuddyListChanged( int state, String remoteJid )
    {
        switch ( BuddyListState.fromInteger( state ) )
        {
            case REMOVE:
                handleBuddyRemoved( remoteJid );
                break;
            case RESET:
                handleBuddyReset();
                break;
        }
    }

    @Override
    public void handlePresenceChanged( String remoteJid, int available, int show )
    {
        Log.d( TAG, "presence changed " + remoteJid );
        if ( !mBuddies.containsKey( remoteJid ) )
        {
            return;
        }

        Log.d( TAG, "updating presence" );
        synchronized ( mLock )
        {
            Buddy buddy = mBuddies.get( remoteJid );
            buddy.setAvailable( XmppPresenceAvailable.fromInteger( available ) );
            buddy.setShow( XmppPresenceShow.fromInteger( show ) );
        }

        dispatchCallback();
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
                mCallbacks.getBroadcastItem( i ).handleBuddyUpdated( buddies );
            }
            catch ( RemoteException e )
            {
                // NOOP
            }
        }
        mCallbacks.finishBroadcast();
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

    private void handleRequestBuddyUpdate()
    {
        Log.d( TAG, "handleRequestBuddyUpdate" );
        dispatchCallback();
    }
}

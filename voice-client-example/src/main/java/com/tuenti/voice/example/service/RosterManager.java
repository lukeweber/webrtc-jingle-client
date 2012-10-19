package com.tuenti.voice.example.service;

import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.util.Log;
import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.RosterListener;
import com.tuenti.voice.core.VoiceClient;

public class RosterManager
    implements RosterListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "RosterManager";

    private final IRosterService.Stub mBinder = new IRosterService.Stub()
    {
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
                Log.v( TAG, "Adding buddy " + remoteJid );
                // Intent add buddy
                // mBuddyList.add(remoteJid);
                break;
            case REMOVE:
                Log.v( TAG, "Removing buddy" + remoteJid );
                // Intent remove buddy
                // mBuddyList.remove(remoteJid);
                break;
            case RESET:
                Log.v( TAG, "Reset buddy list" );
                // intent reset buddy list
                // mBuddyList.clear();
                break;
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind()
    {
        return mBinder;
    }
}

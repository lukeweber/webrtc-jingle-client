package com.tuenti.voice.example.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.RosterListener;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.example.util.ConnectionMonitor;

public class VoiceClientService
    extends Service
    implements RosterListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "VoiceClientService";

    private CallManager mCallManager;

    private VoiceClient mClient;

    private ConnectionManager mConnectionManager;

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

    @Override
    public IBinder onBind( Intent intent )
    {
        if ( IConnectionService.class.getName().equals( intent.getAction() ) )
        {
            return mConnectionManager.onBind();
        }
        if ( ICallService.class.getName().equals( intent.getAction() ) )
        {
            return mCallManager.onBind();
        }
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        mClient = new VoiceClient();
        mClient.addRosterListener( this );

        mConnectionManager = new ConnectionManager( mClient );
        mCallManager = new CallManager( mClient, getBaseContext() );

        ConnectionMonitor.getInstance( getApplicationContext() );
        ConnectionMonitor.registerCallback( mConnectionManager );
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        mClient.release();
        mClient = null;
        ConnectionMonitor.getInstance( getApplicationContext() ).destroy();
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId )
    {
        return START_STICKY;
    }
}

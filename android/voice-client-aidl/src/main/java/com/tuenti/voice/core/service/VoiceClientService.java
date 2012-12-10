package com.tuenti.voice.core.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.manager.BuddyManagerImpl;
import com.tuenti.voice.core.manager.CallManagerImpl;
import com.tuenti.voice.core.manager.ConnectionManagerImpl;

public class VoiceClientService
    extends Service
{
// ------------------------------ FIELDS ------------------------------

    private BuddyManagerImpl mBuddyManager;

    private CallManagerImpl mCallManager;

    private VoiceClient mClient;

    private ConnectionManagerImpl mConnectionManager;

// -------------------------- OTHER METHODS --------------------------

    @Override
    public IBinder onBind( Intent intent )
    {
        if ( IConnectionService.class.getName().equals( intent.getAction() ) )
        {
            return mConnectionManager.onBind();
        }
        if ( IBuddyService.class.getName().equals( intent.getAction() ) )
        {
            return mBuddyManager.onBind();
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

        // VoiceClient should only be created here
        // probably init here too.
        mClient = new VoiceClient();

        // init managers
        mConnectionManager = new ConnectionManagerImpl( mClient );
        mBuddyManager = new BuddyManagerImpl( mClient );
        mCallManager = new CallManagerImpl( mClient, getBaseContext() );

        mClient.init(getApplicationContext());
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // destroy the client
        mClient.release();
        mClient = null;
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId )
    {
        return START_STICKY;
    }
}

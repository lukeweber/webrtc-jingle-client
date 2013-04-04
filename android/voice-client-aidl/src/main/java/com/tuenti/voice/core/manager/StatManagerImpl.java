package com.tuenti.voice.core.manager;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.service.IStatService;
import com.tuenti.voice.core.service.IStatServiceCallback;

import java.util.Collections;
import java.util.LinkedHashMap;

public class StatManagerImpl
    implements StatManager
{
// ------------------------------ FIELDS ------------------------------

    private static final Object mLock = new Object();

    private static final String TAG = "StatManagerImpl";

    private final IStatService.Stub mBinder = new IStatService.Stub()
    {
        @Override
        public void registerCallback( IStatServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.register( cb );
            }
        }

        @Override
        public void unregisterCallback( IStatServiceCallback cb )
        {
            if ( cb != null )
            {
                mCallbacks.unregister( cb );
            }
        }
    };

    private String mStats = "";

    private final RemoteCallbackList<IStatServiceCallback> mCallbacks =
        new RemoteCallbackList<IStatServiceCallback>();

    private VoiceClient mClient;

// --------------------------- CONSTRUCTORS ---------------------------

    public StatManagerImpl( VoiceClient client )
    {
        mClient = client;
        mClient.setStatManager( this );
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface StatManager ---------------------

    @Override
    public void handleStatsUpdate( String remoteJid )
    {
        //TODO: worst name ever please change me!!!!
        mStats = remoteJid;
        dispatchCallback();
    }

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind()
    {
        return mBinder;
    }

    private void dispatchCallback()
    {
        final int callbackCount = mCallbacks.beginBroadcast();
        for ( int i = 0; i < callbackCount; i++ )
        {
            try
            {
                mCallbacks.getBroadcastItem( i ).handleStatsUpdate( mStats );
            }
            catch ( RemoteException e )
            {
                Log.w( TAG, "dispatchCallback - RemoteException");
                // NOOP
            }
        }
        mCallbacks.finishBroadcast();
    }
}

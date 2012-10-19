package com.tuenti.voice.example.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;

import static android.telephony.PhoneStateListener.*;

public class ConnectionMonitor
{
// ------------------------------ FIELDS ------------------------------

    private static final int NETWORK_TYPE_NONE = -1;

    private static int mActiveNetwork;

    private static ArrayList<IConnectionMonitor> mCallbacks = new ArrayList<IConnectionMonitor>();

    private static ConnectionMonitor instance;

    private static final Object mutex = new Object();

    private static boolean mSlowConnection;

    private BroadcastReceiver globalBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( final Context context, final Intent intent )
        {
            if ( intent.getAction().equals( ConnectivityManager.CONNECTIVITY_ACTION ) )
            {
                NetworkInfo networkInfo = intent.getParcelableExtra( ConnectivityManager.EXTRA_NETWORK_INFO );
                boolean noConnectiviy = intent.getBooleanExtra( ConnectivityManager.EXTRA_NO_CONNECTIVITY, false );
                boolean isConnected = networkInfo.isConnected();
                handleNetworkStateChange( networkInfo.getType(), isConnected );
                if ( noConnectiviy )
                {
                    onConnectivityLost();
                }
            }
        }
    };

    private ConnectivityManager mConnectivityManager;

    private Context mContext;

    private boolean mExternalCallInProgress;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener()
    {
        @Override
        public void onCallStateChanged( int state, String incomingNumber )
        {
            handleCallState( state );
            super.onCallStateChanged( state, incomingNumber );
        }

        @Override
        public void onDataConnectionStateChanged( int state, int networkType )
        {
            Log.i( "webrtc libjingle", "data conn state: " + state + " " + "network type" + networkType );
            if ( state == TelephonyManager.DATA_CONNECTED )
            {
                if ( networkType == TelephonyManager.NETWORK_TYPE_GPRS && !mSlowConnection )
                {
                    mSlowConnection = true;
                }
                else if ( mSlowConnection )
                {
                    mSlowConnection = false;
                }
            }
            else if ( state == TelephonyManager.DATA_DISCONNECTED && mSlowConnection )
            {
                mSlowConnection = false;
            }
            super.onDataConnectionStateChanged( state );
        }
    };

    private TelephonyManager mTelephonyManager;

// -------------------------- STATIC METHODS --------------------------

    public static ConnectionMonitor getInstance( Context context )
    {
        synchronized ( mutex )
        {
            if ( instance == null )
            {
                instance = new ConnectionMonitor( context );
            }
        }
        return instance;
    }

    public static boolean hasSlowConnection()
    {
        return mSlowConnection;
    }

    public static boolean isOnline()
    {
        return mActiveNetwork != NETWORK_TYPE_NONE;
    }

    public static void registerCallback( IConnectionMonitor handler )
    {
        mCallbacks.add( handler );
    }

    public static void unregisterCallback( IConnectionMonitor handler )
    {
        mCallbacks.remove( handler );
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private ConnectionMonitor( Context context )
    {
        mContext = context;
        mActiveNetwork = NETWORK_TYPE_NONE;

        mTelephonyManager = (TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE );
        mTelephonyManager.listen( mPhoneStateListener, LISTEN_CALL_STATE | LISTEN_DATA_CONNECTION_STATE );

        mConnectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if ( networkInfo != null && networkInfo.isConnected() )
        {
            handleNetworkStateChange( networkInfo.getType(), true );
        }

        handleCallState( mTelephonyManager.getCallState() );

        IntentFilter globalIntentFilter = new IntentFilter();
        globalIntentFilter.addAction( ConnectivityManager.CONNECTIVITY_ACTION );
        context.registerReceiver( globalBroadcastReceiver, globalIntentFilter );
    }

// -------------------------- OTHER METHODS --------------------------

    public void destroy()
    {
        mTelephonyManager.listen( mPhoneStateListener, LISTEN_NONE );
        mTelephonyManager = null;
        mContext.unregisterReceiver( globalBroadcastReceiver );
        mCallbacks.clear();
        instance = null;
    }

    public boolean isCallInProgress()
    {
        return mExternalCallInProgress;
    }

    private void handleCallState( int state )
    {
        switch ( state )
        {
            case TelephonyManager.CALL_STATE_IDLE:
                setInCall( false );
                break;
            case TelephonyManager.CALL_STATE_RINGING:// Incoming
            case TelephonyManager.CALL_STATE_OFFHOOK:// Outgoing
                setInCall( true );
                break;
        }
    }

    private void handleNetworkStateChange( int networkType, boolean connected )
    {
        if ( mActiveNetwork == NETWORK_TYPE_NONE && connected )
        {
            mActiveNetwork = networkType;
            onConnectionEstablished();
        }
        else if ( mActiveNetwork == networkType && !connected )
        {
            mActiveNetwork = NETWORK_TYPE_NONE;
            onConnectionLost();
        }
    }

    private void onConnectionEstablished()
    {
        for ( IConnectionMonitor manager : mCallbacks )
        {
            manager.onConnectionEstablished();
        }
    }

    private void onConnectionLost()
    {
        for ( IConnectionMonitor manager : mCallbacks )
        {
            manager.onConnectionLost();
        }
    }

    private void onConnectivityLost()
    {
        for ( IConnectionMonitor manager : mCallbacks )
        {
            manager.onConnectivityLost();
        }
    }

    private void setInCall( boolean callInProgress )
    {
        if ( mExternalCallInProgress != callInProgress )
        {
            mExternalCallInProgress = callInProgress;
        }
    }
}

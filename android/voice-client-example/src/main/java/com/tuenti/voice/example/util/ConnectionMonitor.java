package com.tuenti.voice.example.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.RemoteCallbackList;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

public class ConnectionMonitor {

    private static final int NETWORK_TYPE_NONE = -1;

    private boolean mExternalCallInProgress;

    private static int mActiveNetwork;
    private TelephonyManager mTelephonyManager;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private static ArrayList<IConnectionMonitor> mCallbacks = new ArrayList<IConnectionMonitor>();
    private static ConnectionMonitor instance;
    private static final Object mutex = new Object();
    private static boolean mSlowConnection;

    public static ConnectionMonitor getInstance(Context context){
        synchronized ( mutex )
        {
            if (instance == null){
                instance = new ConnectionMonitor(context);
            }
        }
        return instance;
    }

    public static boolean hasSlowConnection(){
        return mSlowConnection;
    }

    private ConnectionMonitor(Context context){
        mContext = context;
        mActiveNetwork = NETWORK_TYPE_NONE;

        TelephonyManager mTelephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int events = PhoneStateListener.LISTEN_CALL_STATE
                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
        mTelephonyManager.listen(mPhoneStateListener, events);

        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()){
            handleNetworkStateChange(networkInfo.getType(), true);
        }

        handleCallState(mTelephonyManager.getCallState());

        IntentFilter globalIntentFilter = new IntentFilter();
        globalIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(globalBroadcastReceiver, globalIntentFilter);
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            handleCallState(state);
            super.onCallStateChanged(state, incomingNumber);
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.i("webrtc libjingle", "data conn state: " + state + " " + "network type" + networkType);
            if (state == TelephonyManager.DATA_CONNECTED){
                if (networkType == TelephonyManager.NETWORK_TYPE_GPRS && !mSlowConnection){
                    mSlowConnection = true;
                } else if (mSlowConnection) {
                    mSlowConnection = false;
                }
            } else if (state == TelephonyManager.DATA_DISCONNECTED && mSlowConnection) {
                mSlowConnection = false;
            }
            super.onDataConnectionStateChanged(state);
        }
    };

    private BroadcastReceiver globalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                boolean noConnectiviy = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                boolean isConnected = networkInfo.isConnected();
                handleNetworkStateChange(networkInfo.getType(), isConnected);
                if (noConnectiviy) {
                    onConnectivityLost();
                }
            }
        }
    };

    public boolean isCallInProgress() {
        return mExternalCallInProgress;
    }

    public static boolean isOnline() {
        return mActiveNetwork != NETWORK_TYPE_NONE;
    }

    private void handleCallState(int state){
        switch (state) {
        case TelephonyManager.CALL_STATE_IDLE:
            setInCall(false);
            break;
        case TelephonyManager.CALL_STATE_RINGING:// Incoming
        case TelephonyManager.CALL_STATE_OFFHOOK:// Outgoing
            setInCall(true);
            break;
        }
    }

    private void handleNetworkStateChange(int networkType, boolean connected){
        if ( mActiveNetwork == NETWORK_TYPE_NONE && connected){
            mActiveNetwork = networkType;
            onConnectionEstablished();
        } else if ( mActiveNetwork == networkType && !connected ) {
            mActiveNetwork = NETWORK_TYPE_NONE;
            onConnectionLost();
        }
    }

    private void setInCall(boolean callInProgress){
        if (mExternalCallInProgress != callInProgress) {
            mExternalCallInProgress = callInProgress;
        }
    }

    public static void registerCallback(IConnectionMonitor handler){
        mCallbacks.add(handler);
    }

    public static void unregisterCallback(IConnectionMonitor handler){
        mCallbacks.remove(handler);
    }

    private void onConnectionEstablished(){
        Iterator<IConnectionMonitor> iter = mCallbacks.iterator();
        while (iter.hasNext()) {
            IConnectionMonitor manager = iter.next();
            manager.onConnectionEstablished();
        }
    }

    private void onConnectionLost(){
        Iterator<IConnectionMonitor> iter = mCallbacks.iterator();
        while (iter.hasNext()) {
            IConnectionMonitor manager = iter.next();
            manager.onConnectionLost();
        }
    }

    private void onConnectivityLost(){
        Iterator<IConnectionMonitor> iter = mCallbacks.iterator();
        while (iter.hasNext()) {
            IConnectionMonitor manager = iter.next();
            manager.onConnectivityLost();
        }
    }

    public void destroy(){
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mTelephonyManager = null;
        mContext.unregisterReceiver(globalBroadcastReceiver);
        mCallbacks.clear();
        instance = null;
    }
}

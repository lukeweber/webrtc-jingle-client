package com.tuenti.voice.example.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkPreference {

    
    private Context mContext;
    private ConnectivityManager mConnManager;
    
    public NetworkPreference(Context context){
        mContext = context;
    }
    
    public void enableStickyNetworkPreference(){
        mConnManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mConnManager.getActiveNetworkInfo();
        int type = networkInfo.getType();
        mConnManager.setNetworkPreference(type);
    }
    
    public void unsetNetworkPreference(){
    	if(mConnManager != null) {
    		mConnManager.setNetworkPreference(ConnectivityManager.DEFAULT_NETWORK_PREFERENCE);
    	}
    }
}

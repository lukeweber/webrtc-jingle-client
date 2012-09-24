package com.tuenti.voice.example.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import com.tuenti.voice.example.ProximitySensor;
import com.tuenti.voice.example.R;

public class CallInProgressActivity
    extends Activity
    implements View.OnClickListener
{
    // UI lock flag
    private boolean mUILocked = false;

    private final String TAG = "s-libjingle-webrtc";
    private ProximitySensor mProximitySensor;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate - CallInProgressActivity");
        setContentView( R.layout.callinprogress );
        mProximitySensor = new ProximitySensor(this);
    }

    protected void onStop(){
        super.onStop();
        mProximitySensor.destroy();
        mProximitySensor = null;
    }

    public void onClick( View view ) {
        
    }
}

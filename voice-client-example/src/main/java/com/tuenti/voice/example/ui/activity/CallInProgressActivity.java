package com.tuenti.voice.example.ui.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

import com.tuenti.voice.example.R;
import com.tuenti.voice.example.service.CallIntent;
import com.tuenti.voice.example.service.CallUIIntent;
import com.tuenti.voice.example.util.ProximitySensor;
import com.tuenti.voice.example.util.WakeLockManager;

public class CallInProgressActivity extends Activity implements
        View.OnClickListener {
    // UI lock flag
    private boolean mUILocked = false;

    private final String TAG = "CallInProgressActivity";
    private ProximitySensor mProximitySensor;
    private WakeLockManager mWakeLock;

    private long mCallId;
    private String mRemoteJid;
    private boolean mMute;
    private boolean mHold;
    
    private TextView durationTextView;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
        	
        	if (action.equals(CallUIIntent.LOGGED_OUT)
                    || action.equals(CallUIIntent.CALL_ENDED)) {
                finish();
            } else if(action.equals(CallUIIntent.CALL_PROGRESS)) {
            	updateCallDuration(intent.getLongExtra("duration", -1));
            }
            
        }
    };
    
    /**
     * Updates the call duration TextView with the new duration.
     * 
     * @param duration The new duration to display.
     */
    private void updateCallDuration(long duration) {
		if(duration >= 0) {
	    	long minutes = duration / 60;
	    	long seconds = duration % 60;
	    	String formattedDuration = String.format("%02d:%02d", minutes, seconds);
			
	    	durationTextView.setText(formattedDuration);
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.callinprogress);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        
        durationTextView = (TextView) findViewById(R.id.duration_textview);
        updateCallDuration(0);
        
        initClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        
        Intent intent = getIntent();
        mCallId = intent.getLongExtra("callId", 0);
        mRemoteJid = intent.getStringExtra("remoteJid");
        mMute = intent.getBooleanExtra("isMuted", false);
        mHold = intent.getBooleanExtra("isHeld", false);
        mProximitySensor = new ProximitySensor(this);
        mWakeLock = new WakeLockManager(getBaseContext());
        setupReceiver();
        changeStatus("Talking to " + mRemoteJid);
    }

    private void setupReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallUIIntent.CALL_STARTED);
        intentFilter.addAction(CallUIIntent.CALL_PROGRESS);
        intentFilter.addAction(CallUIIntent.CALL_ENDED);
        intentFilter.addAction(CallUIIntent.LOGGED_OUT);
        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(mReceiver,
                intentFilter);
    }

    public void initClickListeners() {
        findViewById(R.id.hang_up_btn).setOnClickListener(this);
        findViewById(R.id.mute_btn).setOnClickListener(this);
        findViewById(R.id.hold_btn).setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mProximitySensor.destroy();
        mProximitySensor = null;
        onUnProximity();
        mWakeLock.releaseWakeLock();
        LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(mReceiver);
    }
    
    @Override 
    protected void onStop() {
        super.onStop();
        mWakeLock.releaseWakeLock();
    }
    
    public void onProximity() {
        mUILocked = true;
        turnScreenOn(false);
        mWakeLock.setWakeLockState(PowerManager.PARTIAL_WAKE_LOCK);
    }

    private void changeStatus(String status) {
        ((TextView) findViewById(R.id.status_view)).setText(status);
    }

    public void onUnProximity() {
        mWakeLock.setWakeLockState(PowerManager.FULL_WAKE_LOCK);
        mUILocked = false;
        turnScreenOn(true);
    }

    private void turnScreenOn(boolean on) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= LayoutParams.FLAG_KEEP_SCREEN_ON;
        if (on) {
            // less than 0 returns to default behavior.
            params.screenBrightness = -1;
        } else {
            params.screenBrightness = 0;
        }
        getWindow().setAttributes(params);
    }

    @Override
    public void onClick(View view) {
        if (mUILocked == false) {
            Intent intent;
            switch (view.getId()) {
            case R.id.hang_up_btn:
                intent = new Intent(CallIntent.END_CALL);
                intent.putExtra("callId", mCallId);
                LocalBroadcastManager.getInstance(getBaseContext())
                        .sendBroadcast(intent);
                updateCallDuration(0);
                finish();
                break;
            case R.id.mute_btn:
                intent = new Intent(CallIntent.MUTE_CALL);
                intent.putExtra("callId", mCallId);
                LocalBroadcastManager.getInstance(getBaseContext())
                        .sendBroadcast(intent);
                break;
            case R.id.hold_btn:
                intent = new Intent(CallIntent.HOLD_CALL);
                intent.putExtra("callId", mCallId);
                LocalBroadcastManager.getInstance(getBaseContext())
                        .sendBroadcast(intent);
                break;
            }
        }
    }
}

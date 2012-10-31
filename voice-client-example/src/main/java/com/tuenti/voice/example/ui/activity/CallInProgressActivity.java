package com.tuenti.voice.example.ui.activity;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
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
	private boolean mUILocked;

	private final String LOG_TAG = "CallInProgressActivity";
	private WakeLockManager mWakeLock;
	private KeyguardLock mKeyguardLock;
	private ProximitySensor mProximitySensor;

	private long mCallId;
	private String mRemoteJid;
	private boolean mMute;
	private boolean mHold;

	private TextView durationTextView;
	private PowerManager mPowerManager;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action.equals(CallUIIntent.LOGGED_OUT)
					|| action.equals(CallUIIntent.CALL_ENDED)) {
				finish();
			} else if (action.equals(CallUIIntent.CALL_PROGRESS)) {
				updateCallDuration(intent.getLongExtra("duration", -1));
			}
		}
	};

	/**
	 * Updates the call duration TextView with the new duration.
	 *
	 * @param duration
	 *			The new duration to display.
	 */
	private void updateCallDuration(long duration) {
		if (duration >= 0) {
			long minutes = duration / 60;
			long seconds = duration % 60;
			String formattedDuration = String.format("%02d:%02d", minutes,
					seconds);

			durationTextView.setText(formattedDuration);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUILocked = false;

		setContentView(R.layout.callinprogress);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

		durationTextView = (TextView) findViewById(R.id.duration_textview);
		updateCallDuration(0);

		mWakeLock = new WakeLockManager(this);

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

		initClickListeners();
		setupReceiver();
	}

	@Override
	protected void onStart(){
		super.onStart();
		KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		mKeyguardLock = mKeyGuardManager.newKeyguardLock(LOG_TAG);
		mKeyguardLock.disableKeyguard();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mWakeLock.setProximityWakeLock(true, false);
		//fail over manual proximity management.
		if( !mWakeLock.isProximityWakeLockEnabled() ){
			mProximitySensor = new ProximitySensor(this);
			mProximitySensor.start();
		}
		Intent intent = getIntent();
		mCallId = intent.getLongExtra("callId", 0);
		mRemoteJid = intent.getStringExtra("remoteJid");
		mMute = intent.getBooleanExtra("isMuted", false);
		mHold = intent.getBooleanExtra("isHeld", false);

		changeStatus("Talking to " + mRemoteJid);
	}

	@Override
	protected void onPause() {
		super.onPause();

	   // Pausing while screen is on means it's not because of proximity.
	   if (mPowerManager.isScreenOn()){
		   if( mProximitySensor != null ){
			   mProximitySensor.stop();
		   }
		   mWakeLock.setProximityWakeLock(false, true);
	   }
	}

	private void setupReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(CallUIIntent.CALL_STARTED);
		intentFilter.addAction(CallUIIntent.CALL_PROGRESS);
		intentFilter.addAction(CallUIIntent.CALL_ENDED);
		intentFilter.addAction(CallUIIntent.LOGGED_OUT);
		LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(
				mReceiver, intentFilter);
	}

	public void initClickListeners() {
		findViewById(R.id.hang_up_btn).setOnClickListener(this);
		findViewById(R.id.mute_btn).setOnClickListener(this);
		findViewById(R.id.hold_btn).setOnClickListener(this);
	}

	@Override
	protected void onStop() {
		mKeyguardLock.reenableKeyguard();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(
				mReceiver);
		mWakeLock.setProximityWakeLock(false, true);
		if( mProximitySensor != null ){
			mProximitySensor.stop();
		}
		super.onDestroy();
	}

	private void changeStatus(String status) {
		((TextView) findViewById(R.id.status_view)).setText(status);
	}

	/**
	 * Method to manually turn the screen on. Not as reliable as the internal api
	 * used in WakeLockManager, but used by ProximitySensor to turn on and off the
	 * screen in case of fall back.
	 * @param screenOn - Whether to turn the screen on.
	 */
	public void turnScreenOn(boolean screenOn) {
		mUILocked = !screenOn;
		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		if (screenOn) {
			// less than 0 returns to default behavior.
			params.screenBrightness = -1;
		} else {
			// Some phone power management ignore FLAG_KEEP_SCREEN_ON, and will
			// shut down screen, thus locking device. Not turning the screen off
			// avoids this case.
			params.screenBrightness = 0.01f;
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

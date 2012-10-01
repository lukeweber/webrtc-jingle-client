package com.tuenti.voice.example.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.tuenti.voice.example.R;
import com.tuenti.voice.example.service.CallIntent;
import com.tuenti.voice.example.service.CallUIIntent;
import com.tuenti.voice.example.util.WakeLockManager;

public class IncomingCallDialog extends Activity implements
		DialogInterface.OnClickListener {

	// ------------------------------ FIELDS ------------------------------

	private long mCallId;
	private String mRemoteJid;
	private WakeLockManager mWakeLock;
	private KeyguardLock mKeyguardLock;
	private AlertDialog mAlertDialog;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("libjingle",
					"libjingle local receiver: " + intent.getAction());
			if (intent.getAction().equals(CallUIIntent.CALL_PROGRESS)
					|| intent.getAction().equals(CallUIIntent.CALL_ENDED)
					|| intent.getAction().equals(CallUIIntent.LOGGED_OUT)) {
				finish();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		mCallId = intent.getLongExtra("callId", 0);
		mRemoteJid = intent.getStringExtra("remoteJid");

		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		// setContentView(R.layout.incomingcalldialog);
		final Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(R.string.voice_chat_invite)
				.setMessage(mRemoteJid)
				.setPositiveButton(R.string.accept_call, this)
				.setNegativeButton(R.string.decline_call, this)
				.setCancelable(false);

		mAlertDialog = alertDialogBuilder.create();
		mAlertDialog.show();
	}

	@Override
	public void onResume() {
		super.onResume();
		KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		mKeyguardLock = mKeyGuardManager.newKeyguardLock("screenunlock");
		mKeyguardLock.disableKeyguard();

		mWakeLock = new WakeLockManager(getBaseContext());
		mWakeLock.setWakeLockState(PowerManager.FULL_WAKE_LOCK
				| PowerManager.ACQUIRE_CAUSES_WAKEUP);

		setupReceiver();
		checkIntentAction();
	}

	private void setupReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(CallUIIntent.CALL_PROGRESS);
		intentFilter.addAction(CallUIIntent.CALL_ENDED);
		intentFilter.addAction(CallUIIntent.LOGGED_OUT);
		LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(
				mReceiver, intentFilter);
	}

	private void checkIntentAction() {
		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();

			if (action != null && action.equals(CallIntent.ACCEPT_CALL)) {
				// When coming from the Notification, auto accept the call.
				onClick(null, DialogInterface.BUTTON_POSITIVE);
			}
		}
	}

	// ------------------------ INTERFACE METHODS ------------------------
	// --------------------- Interface OnClickListener ---------------------
	@Override
	public void onClick(DialogInterface dialog, int which) {
		Intent intent;
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			// Yes button clicked
			intent = new Intent(CallIntent.ACCEPT_CALL);
			intent.putExtra("callId", mCallId);
			LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
					intent);
			break;

		case DialogInterface.BUTTON_NEGATIVE:
			// No button clicked
			intent = new Intent(CallIntent.REJECT_CALL);
			intent.putExtra("callId", mCallId);
			LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
					intent);
			break;
		}
		mAlertDialog.hide();
		finish();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mKeyguardLock != null) {
			mKeyguardLock.reenableKeyguard();
		}

		if (mWakeLock != null) {
			mWakeLock.releaseWakeLock();
		}

		LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(
				mReceiver);
	}
}

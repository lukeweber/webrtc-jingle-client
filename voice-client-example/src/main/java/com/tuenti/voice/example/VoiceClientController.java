package com.tuenti.voice.example;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.tuenti.voice.example.service.CallIntent;
import com.tuenti.voice.example.service.IVoiceClientService;
import com.tuenti.voice.example.service.IVoiceClientServiceCallback;

public class VoiceClientController {
	IVoiceClientService mService;
	private Context mContext;

	private static final String TAG = "controller-libjingle-webrtc";

	private boolean mIsBound = false;

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String intentString = intent.getAction();
			long callId = intent.getLongExtra("callId", 0);
			if (intentString.equals(CallIntent.HOLD_CALL)) {
				try {
					mService.toggleHold(callId);
				} catch (RemoteException e) {
				}
			} else if (intentString.equals(CallIntent.MUTE_CALL)) {
				try {
					mService.toggleMute(callId);
				} catch (RemoteException e) {
				}
			} else if (intentString.equals(CallIntent.END_CALL)) {
				try {
					mService.endCall(callId);
				} catch (RemoteException e) {
				}
			} else if (intentString.equals(CallIntent.PLACE_CALL)) {
				String remoteJid = intent.getStringExtra("remoteJid");
				try {
					mService.call(remoteJid);
				} catch (RemoteException e) {
				}
			} else if (intentString.equals(CallIntent.ACCEPT_CALL)) {
				try {
					mService.acceptCall(callId);
				} catch (RemoteException e) {
				}
			} else if (intentString.equals(CallIntent.REJECT_CALL)) {
				try {
					mService.declineCall(callId, true);
				} catch (RemoteException e) {
				}
			} else if (intentString.equals(CallIntent.LOGIN)) {
				try {
					String username = intent.getStringExtra("username");
					String password = intent.getStringExtra("password");
					String turnUsername = intent.getStringExtra("turnUsername");
					String turnPassword = intent.getStringExtra("turnPassword");
					String xmppHost = intent.getStringExtra("xmppHost");
					int xmppPort = intent.getIntExtra("xmppPort", 0);
					boolean xmppUseSSl = intent.getBooleanExtra("xmppUseSSL", false);
					mService.login(username, password, turnUsername, turnPassword, xmppHost, xmppPort, xmppUseSSl);
				} catch (RemoteException e) {
				}
			} else if (intentString.equals(CallIntent.LOGOUT)) {
				try {
					mService.logout();
				} catch (RemoteException e) {
				}
			}
		}
	};

	//This is used to receive events for notifications, which can only broadcast global.
	private BroadcastReceiver globalBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// Thin layer of security to avoid others from killing calls.
			// TODO: Is this enough security?
			if (context.getApplicationContext() == mContext
					.getApplicationContext()) {
				mBroadcastReceiver.onReceive(context, intent);
			} else {
				Log.e(TAG,
						"Another app is trying to access things it shouldn't!\nOffender: "
								+ context);
			}
		}
	};

	public VoiceClientController(Context context) {
		mContext = context;

		// Local receiver.
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(CallIntent.PLACE_CALL);
		intentFilter.addAction(CallIntent.ACCEPT_CALL);
		intentFilter.addAction(CallIntent.REJECT_CALL);
		intentFilter.addAction(CallIntent.END_CALL);
		intentFilter.addAction(CallIntent.MUTE_CALL);
		intentFilter.addAction(CallIntent.HOLD_CALL);
		intentFilter.addAction(CallIntent.LOGIN);
		intentFilter.addAction(CallIntent.LOGOUT);
		LocalBroadcastManager.getInstance(mContext).registerReceiver(
				mBroadcastReceiver, intentFilter);

		// Global receiver.
		IntentFilter globalIntentFilter = new IntentFilter();
		globalIntentFilter.addAction(CallIntent.END_CALL);
		globalIntentFilter.addAction(CallIntent.REJECT_CALL);
		mContext.registerReceiver(globalBroadcastReceiver, globalIntentFilter);
	}

	public void bind() {
		mContext.bindService(new Intent(IVoiceClientService.class.getName()),
				mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			mService = IVoiceClientService.Stub.asInterface(service);
			try {
				mService.registerCallback(mCallback);
			} catch (RemoteException $e) {

			}
			// We want to monitor the service for as long as we are
			// connected to it.
			Log.i(TAG, "Connected to service");
		}

		public void onServiceDisconnected(ComponentName className) {
			try {
				mService.unregisterCallback(mCallback);
			} catch (RemoteException $e) {

			}
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mService = null;
			Log.i(TAG, "Disconnected from service");
		}
	};

	/**
	 * This implementation is used to receive callbacks from the remote service.
	 */
	private IVoiceClientServiceCallback mCallback = new IVoiceClientServiceCallback.Stub() {
		/**
		 * This is called by the remote service regularly to tell us about new
		 * values. Note that IPC calls are dispatched through a thread pool
		 * running in each process, so the code executing here will NOT be
		 * running in our main thread like most other things -- so, to update
		 * the UI, we need to use a Handler to hop over there.
		 */
		public void sendBundle(Bundle bundle) {
			/*
			 * Message msg = Message.obtain(); msg.what = bundle.getInt("what");
			 * msg.setData(bundle); mHandler.sendMessage(msg);
			 */
			// Implement me for intent or whatever we do here.
		}

		public void dispatchLocalIntent(Intent intent) {
			Intent newIntent = (Intent) intent.clone();
			LocalBroadcastManager.getInstance(mContext)
					.sendBroadcast(newIntent);
		}
	};

	public void onDestroy() {
		if (mIsBound) {
			mContext.unbindService(mConnection);
			LocalBroadcastManager.getInstance(mContext).unregisterReceiver(
					mBroadcastReceiver);
			mContext.unregisterReceiver(globalBroadcastReceiver);
			mIsBound = false;
		}
	}
}

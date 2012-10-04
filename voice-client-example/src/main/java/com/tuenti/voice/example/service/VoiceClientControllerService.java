package com.tuenti.voice.example.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class VoiceClientControllerService extends Service {
	IVoiceClientService mService;

	private static final String TAG = "controller-libjingle-webrtc";

	private boolean mIsBound = false;
	
	// ------------ Local service ----------------------------
	
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        VoiceClientControllerService getService() {
            return VoiceClientControllerService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate in controller");
        init();
        bind();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mIsBound) {
            unbindService(mConnection);
            LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(
                    mBroadcastReceiver);
            mIsBound = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    
    // ------------------- End local service code ---------------------------

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
					String xmppHost = intent.getStringExtra("xmppHost");
					int xmppPort = intent.getIntExtra("xmppPort", 0);
					boolean xmppUseSSl = intent.getBooleanExtra("xmppUseSSL",
							false);
					mService.login(username, password, xmppHost, xmppPort,
							xmppUseSSl);
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

	public void init() {

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
		LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(
				mBroadcastReceiver, intentFilter);
	}

	public void bind() {
		bindService(new Intent(IVoiceClientService.class.getName()),
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
			LocalBroadcastManager.getInstance(getBaseContext())
					.sendBroadcast(newIntent);
		}
	};
}

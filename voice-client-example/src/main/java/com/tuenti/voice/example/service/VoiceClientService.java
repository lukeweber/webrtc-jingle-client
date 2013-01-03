package com.tuenti.voice.example.service;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.CallError;
import com.tuenti.voice.core.IVoiceClientServiceInt;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.VoiceClientEventCallback;
import com.tuenti.voice.core.VoiceClientEventHandler;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.data.Call;
import com.tuenti.voice.example.data.User;
import com.tuenti.voice.example.ui.activity.CallInProgressActivity;
import com.tuenti.voice.example.ui.dialog.IncomingCallDialog;
import com.tuenti.voice.example.util.CallNotification;
import com.tuenti.voice.example.util.ConnectionMonitor;
import com.tuenti.voice.example.util.IConnectionMonitor;
import com.tuenti.voice.example.util.NetworkPreference;
import com.tuenti.voice.example.util.RingManager;
import com.tuenti.voice.example.util.WakeLockManager;

public class VoiceClientService extends Service implements
		IVoiceClientServiceInt, VoiceClientEventCallback, IConnectionMonitor {
	private VoiceClient mClient;

	private static final String TAG = "VoiceClientService";

	private static final int CALL_UPDATE_INTERVAL = 1000;

	private HashMap<Long, Call> mCallMap = new HashMap<Long, Call>();

	private boolean mCallInProgress = false;
	private boolean mExternalCallInProgress = false;

	private long mCurrentCallId = 0;

	private Handler mHandler;

	private Handler callProgressHandler;
	private Runnable updateCallDurationTask;

	private AudioManager mAudioManager;

	private RingManager mRingManager;

	private CallNotification mNotificationManager;

	private SharedPreferences mSettings;

	private NetworkPreference mNetworkPreference;

	private boolean mClientInited = false;

	private User mUser;

	private int mXmppState;

	private boolean mReconnectTimerRunning = false;
	private boolean mReconnect = false;

	private WakeLockManager mWakeLock;

	/**
	 * This is a list of callbacks that have been registered with the service.
	 * Note that this is package scoped (instead of private) so that it can be
	 * accessed more efficiently from inner classes.
	 */
	final RemoteCallbackList<IVoiceClientServiceCallback> mCallbacks = new RemoteCallbackList<IVoiceClientServiceCallback>();

// --------------------- Service Methods ---------------------------------------
	@Override
	public void onCreate() {
		super.onCreate();

		// Set default preferences
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		mNotificationManager = new CallNotification(this);
		mNetworkPreference = new NetworkPreference(this);
		mWakeLock = new WakeLockManager(this);

		initClientWrapper();
		initAudio();
		initCallDurationTask();
		initConnectionMonitor();
		// Set default preferences
		// mSettings = PreferenceManager.getDefaultSharedPreferences( this );
	}

	private void initConnectionMonitor(){
	    ConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance(getApplicationContext());
	    ConnectionMonitor.registerCallback(this);
	}

	private void initCallDurationTask() {
		callProgressHandler = new Handler();

		updateCallDurationTask = new Runnable() {

			@Override
			public void run() {
				Call currentCall = mCallMap.get(mCurrentCallId);

				// Send Intent.
				dispatchCallState(CallUIIntent.CALL_PROGRESS, mCurrentCallId,
						currentCall.getRemoteJid(),
						currentCall.getElapsedTime());

				// Send Notification.
				sendCallInProgressNotification(currentCall.getRemoteJid(),
						currentCall.getElapsedTime());

				// Add another call for update.
				callProgressHandler.postDelayed(this, CALL_UPDATE_INTERVAL);
			}

		};
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.e(TAG, "onBind Received");
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mCallbacks.kill();
		mClient.destroy();
		// mBuddyList.clear();
		mClient = null;
		mNetworkPreference.unsetNetworkPreference();
		ConnectionMonitor.getInstance(getApplicationContext()).destroy();
	}
// ------------------- End Service Methods -------------------------------

// --------------------- Interface VoiceClientEventCallback ---------------------
    @Override
    public void handleCallStateChanged(int state, String remoteJid, long callId) {
        remoteJid = cleanJid(remoteJid);
        switch (CallState.fromInteger(state)) {
        case SENT_INITIATE:
            Log.i(TAG, "Outgoing call");
            outgoingCall(callId, remoteJid);
            break;
        case RECEIVED_INITIATE_ACK:
            Log.i(TAG, "Initiate was acked");
            break;
        case RECEIVED_INITIATE:
            Log.i(TAG, "Incoming call");
            if ( ConnectionMonitor.hasSlowConnection() ){
                //Warn that you can't accept the incoming call
                //TODO(Luke): Notification of missed call./Explanation about bad connection
                Log.i(TAG, "Declining call because of slow connection");
                mClient.declineCall(callId, true);
            } else if ( mCallInProgress
                    || ConnectionMonitor.getInstance(getApplicationContext()).isCallInProgress()) {
                //TODO(Luke): Notification of missed call.
                Log.i(TAG, "Declining call because call in progress");
                mClient.declineCall(callId, true);
            } else {
                incomingCall(callId, remoteJid);
            }
            break;
        case SENT_TERMINATE:
        case RECEIVED_TERMINATE:
        case SENT_BUSY:
        case RECEIVED_BUSY:
        case SENT_REJECT:
        case RECEIVED_REJECT:
            Log.i(TAG, "Call ended");
            endCall(callId, 0);// Add reason to end call.
            break;
        case RECEIVED_ACCEPT:
            Log.i(TAG, "Call accepted");
            break;
        case IN_PROGRESS:
            Log.i(TAG, "IN_PROGRESS");
            callStarted(callId);
            break;
        case DE_INIT:
            Log.i(TAG, "DE_INIT");
            break;
        }
        Log.i(TAG, "call state ------------------" + state);
    }

    @Override
    public void handleCallError(int error, long callId){
        switch (CallError.fromInteger(error)){
        case ERROR_NONE:
            // no error
        case ERROR_TIME:
            // no response to signaling
        case ERROR_RESPONSE:
            // error during signaling
        case ERROR_NETWORK:
            // network error, could not allocate network resources
        case ERROR_CONTENT:
            // channel errors in SetLocalContent/SetRemoteContent
        case ERROR_TRANSPORT:
            // transport error of some kind
        case ERROR_ACK_TIME:
            // no ack response to signaling, client not available
        }
        Log.e(TAG, "call error ------------------, callid " + callId + "error " + error );
    }


    @Override
    public void handleAudioPlayout(){
        setAudioForCall();
    }

    @Override
    public void handleXmppError(int error) {
        switch (XmppError.fromInteger(error)) {
        case XML:
            Log.e(TAG, "Malformed XML or encoding error");
            break;
        case STREAM:
            Log.e(TAG, "XMPP stream error");
            break;
        case VERSION:
            Log.e(TAG, "XMPP version error");
            break;
        case UNAUTHORIZED:
            Log.e(TAG,
                    "User is not authorized (Check your username and password)");
            break;
        case TLS:
            Log.e(TAG, "TLS could not be negotiated");
            break;
        case AUTH:
            Log.e(TAG, "Authentication could not be negotiated");
            break;
        case BIND:
            Log.e(TAG, "Resource or session binding could not be negotiated");
            break;
        case CONNECTION_CLOSED:
            Log.e(TAG, "Connection closed by output handler.");
            break;
        case DOCUMENT_CLOSED:
            Log.e(TAG, "Closed by </stream:stream>");
            break;
        case SOCKET:
            Log.e(TAG, "Socket error");
            break;
        }
    }

    @Override
    public void handleXmppSocketClose(int state){
        loggedOut();
    }

    @Override
    public void handleXmppStateChanged(int state) {
        mXmppState = state;
        Intent intent;
        switch (XmppState.fromInteger(state)) {
        case NONE:
            mClientInited = true;
            runPendingLogin();
            break;
        case START:
            // changeStatus( "connecting..." );
            break;
        case OPENING:
            // changeStatus( "logging in..." );
            break;
        case OPEN:
            stopReconnectTimer();
            intent = new Intent(CallUIIntent.LOGGED_IN);
            dispatchLocalIntent(intent);
            break;
        case CLOSED:
            loggedOut();
            break;
        }
    }

    final Runnable mReconnectRunnable = new Runnable()
    {
        public void run()
        {
            internalLogin();
            //Try to reconnect again in 1000 seconds.
            mHandler.postDelayed(this, 1000000);
        }
    };

    private void startReconnectTimer(){
        // Reconnect in 10 seconds if there isn't already one running.
        if (!mReconnectTimerRunning){
            mReconnectTimerRunning = true;
            mHandler.postDelayed(mReconnectRunnable, 10000);
        }
    }

    private void stopReconnectTimer(){
        mHandler.removeCallbacks(mReconnectRunnable);
        mReconnectTimerRunning = false;
    }

    @Override
    public void handleBuddyListChanged(int state, String remoteJid) {
        switch (BuddyListState.fromInteger(state)) {
        case ADD:
            Log.v(TAG, "Adding buddy " + remoteJid);
            // Intent add buddy
            // mBuddyList.add(remoteJid);
            break;
        case REMOVE:
            Log.v(TAG, "Removing buddy" + remoteJid);
            // Intent remove buddy
            // mBuddyList.remove(remoteJid);
            break;
        case RESET:
            Log.v(TAG, "Reset buddy list");
            // intent reset buddy list
            // mBuddyList.clear();
            break;
        }
    }

    @Override
    public void handleStatsUpdate(String stats) {
    	 Intent intent = new Intent(CallUIIntent.UPDATE_CALL_STATS);
    	 intent.putExtra("statistics", stats);
    	 
    	 dispatchLocalIntent(intent);
    }
    
// --------------------- End Interface VoiceClientEventCallback ---------------------

// --------------------- Connection Monitor interface --------------
    public void onConnectionEstablished(){
        if ( mReconnect && XmppState.fromInteger(mXmppState) == XmppState.CLOSED ){
            internalLogin();
        }
    }

    public void onConnectionLost(){
        releaseClient();
    }

    public void onConnectivityLost(){
        stopReconnectTimer();
        //Could blank out voip icons as being available.
        Log.i(TAG, "Connectivity lost");
    }

// ----------------- End Connection Monitor interface ---------------

	private void releaseClient() {
		mClient.release();
		mClientInited = false;
	}

	private static String cleanJid(String jid) {
		if (jid == null) {
			return "";
		}

		int index = jid.indexOf('/');
		if (index > 0) {
			return jid.substring(0, index);
		}
		return jid;
	}

	private void initAudio() {
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	}

  private void setInCallAudio() {
		mAudioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
  }

	private void setAudioForCall() {
		mAudioManager.setMode((Build.VERSION.SDK_INT < 11) ? AudioManager.MODE_NORMAL
						: AudioManager.MODE_IN_COMMUNICATION);
	}

	private void resetAudio() {
		mAudioManager.setMode(AudioManager.MODE_NORMAL);
		mAudioManager.abandonAudioFocus(null);
	}

	/**
	 * Sends an incoming call notification.
	 *
	 * @param remoteJid
	 *            The user that is calling.
	 */
	private void sendIncomingCallNotification(String remoteJid) {
		String message = String.format(
				getString(R.string.notification_incoming_call), remoteJid);
		Intent intent = getNotificationIntent(remoteJid, "",
				IncomingCallDialog.class);

		mNotificationManager.sendCallNotification(message, message, intent,
				CallIntent.REJECT_CALL);
	}

	/**
	 * Sends an outgoing call notification.
	 *
	 * @param remoteJid
	 *            The user that is being called.
	 */
	private void sendOutgoingCallNotification(String remoteJid) {
		String message = String.format(
				getString(R.string.notification_outgoing_call), remoteJid);
		Intent intent = getNotificationIntent(remoteJid,
				CallUIIntent.CALL_PROGRESS, CallInProgressActivity.class);

		mNotificationManager.sendCallNotification(message, message, intent,
				CallIntent.END_CALL);
	}

	/**
	 * Sends a call progress (duration) notification.
	 *
	 * @param remoteJid
	 *            The remote party.
	 * @param duration
	 *            Call duration.
	 */
	private void sendCallInProgressNotification(String remoteJid, long duration) {
		long minutes = duration / 60;
		long seconds = duration % 60;
		String formattedDuration = String.format("%02d:%02d", minutes, seconds);
		String message = String.format(
				getString(R.string.notification_during_call),
				formattedDuration, remoteJid);
		String tickerText = String.format(
				getString(R.string.notification_during_call_ticker), remoteJid);

		Intent intent = getNotificationIntent(remoteJid,
				CallUIIntent.CALL_PROGRESS, CallInProgressActivity.class);

		mNotificationManager.sendCallNotification(tickerText, message, intent,
				CallIntent.END_CALL);
	}

	/**
	 * Starts the call progress update timer.
	 */
	private void startCallProgressTimer() {
		callProgressHandler.removeCallbacks(updateCallDurationTask);
		callProgressHandler.postDelayed(updateCallDurationTask,
				CALL_UPDATE_INTERVAL);
	}

	/**
	 * Stops the call progress update timer.
	 */
	private void stopCallProgressTimer() {
		callProgressHandler.removeCallbacks(updateCallDurationTask);
	}

	/**
	 * Generates an Intent to send with a Notification.
	 *
	 * @param remoteJid
	 *            The remote party.
	 * @param action
	 *            The action to pass to the Intent.
	 * @param context
	 *            The context (i.e. class) to start with this intent.
	 */
	private Intent getNotificationIntent(String remoteJid, String action,
			Class<?> context) {
		Intent intent = getCallIntent(action, mCurrentCallId, remoteJid);
		intent.setClass(this, context);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);

		return intent;
	}

	public Intent getCallIntent(String intentString, long callId,
			String remoteJid) {
		Intent intent = new Intent(intentString);
		intent.putExtra("callId", callId);
		intent.putExtra("remoteJid", remoteJid);
		Call call = mCallMap.get(Long.valueOf(callId));
		intent.putExtra("isHeld", call.isHeld());
		intent.putExtra("isMuted", call.isMuted());
		return intent;
	}

	public void initCallState(long callId, String remoteJid) {
        mNetworkPreference.enableStickyNetworkPreference();
		mCallInProgress = true;
		mCurrentCallId = callId;
		mCallMap.put(Long.valueOf(callId), new Call(callId, remoteJid));
	}

	public void outgoingCall(long callId, String remoteJid) {
		initCallState(callId, remoteJid);
		Intent dialogIntent = new Intent(getBaseContext(),
				CallInProgressActivity.class);
		dialogIntent.putExtra("callId", callId);
		dialogIntent.putExtra("remoteJid", remoteJid);
		dialogIntent
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
						| Intent.FLAG_ACTIVITY_CLEAR_TASK
						| Intent.FLAG_FROM_BACKGROUND );

		// Intent call started, show call in progress activity
		getApplication().startActivity(dialogIntent);

		// Ring in the headphone
		startRing(false, false);

		// Show notification
		sendOutgoingCallNotification(remoteJid);

	}

    public void startCallInProgressActivity(long callId, String remoteJid) {
        Intent dialogIntent = new Intent(getBaseContext(),
                CallInProgressActivity.class);
        dialogIntent.putExtra("callId", callId)
                    .putExtra("remoteJid", remoteJid)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_FROM_BACKGROUND );
        getApplication().startActivity(dialogIntent);
    }

    public void startIncomingCallDialog(long callId, String remoteJid) {
        Intent dialogIntent = new Intent(getBaseContext(),
                IncomingCallDialog.class);
        dialogIntent.putExtra("callId", callId)
                    .putExtra("remoteJid", remoteJid)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_FROM_BACKGROUND );
        getApplication().startActivity(dialogIntent);
    }

	public void incomingCall(long callId, String remoteJid) {
		initCallState(callId, remoteJid);

		// Ringer.
		startRing(true, false);

		// show alert pop up for incoming call + tray notification
		startIncomingCallDialog(callId, remoteJid);
		sendIncomingCallNotification(remoteJid);
	}

	public void callStarted(long callId) {
		stopRing();
        mWakeLock.setWakeLock(true, true);
		Call call = mCallMap.get(Long.valueOf(callId));
		call.startCallTimer();
		String remoteJid = call.getRemoteJid();
		setInCallAudio();
		startCallInProgressActivity(callId, remoteJid);
		dispatchCallState(CallUIIntent.CALL_STARTED, callId,
				call.getRemoteJid());
		// Intent call started

		// Change notification to call in progress notification, that points to
		// call in progress activity on click.
		sendCallInProgressNotification(remoteJid, 0);
		dispatchCallState(CallUIIntent.CALL_PROGRESS, mCurrentCallId,
				call.getRemoteJid(), call.getElapsedTime());

		// start timer on method updateCallUI every second.
		startCallProgressTimer();
	}

	public void updateCallUI() {
		if (mCurrentCallId > 0) {
			// update duration of call in tray notification via changeData.
			// send message to controller, in case there is one, to tell it to
			// update call duration on the UI.
		}
	}

	public void endCall(long callId, int reason) {
		// Makes sure we don't change state for calls
		// we decline as busy while in a call.
		if (mCallMap.containsKey(Long.valueOf(callId))) {
		    mWakeLock.setWakeLock(false, true);
			mCallInProgress = false;
			mCurrentCallId = 0;
			mNetworkPreference.unsetNetworkPreference();
			Call call = mCallMap.get(Long.valueOf(callId));
			mCallMap.remove(Long.valueOf(callId));
			stopRing();
			resetAudio();
			dispatchCallState(CallUIIntent.CALL_ENDED, callId,
					call.getRemoteJid());
			// Store reason in call history with jid.

			// Intent call ended, store in history, return call time
			stopCallProgressTimer();

			// Cancel notification
			mNotificationManager.cancelCallNotification();

			long callTime = call.getElapsedTime();
		}
	}

	/*
	 * Only called on XMPP disconnect as a cleanup operation.
	 */
	private void endAllCalls() {
		Iterator<Long> iter = mCallMap.keySet().iterator();
		while (iter.hasNext()) {
			Long key = iter.next();
			endCall(key, 0);
			// TODO(Luke): Add reason
		}
	}

	private void dispatchCallState(String callState, long callId,
			String remoteJid, long duration) {
		Intent intent = new Intent(callState);
		// TODO: Stick these extra values in some constants!
		intent.putExtra("callId", callId);
		intent.putExtra("remoteJid", remoteJid);
		intent.putExtra("duration", duration);
		dispatchLocalIntent(intent);
	}

	private void dispatchCallState(String callState, long callId,
			String remoteJid) {
		dispatchCallState(callState, callId, remoteJid, -1);
	}

	private void startRing(boolean isIncoming, boolean callInProgress) {
		stopRing();
		mRingManager = new RingManager(getApplicationContext(), isIncoming,
				callInProgress);
	}

	private void stopRing() {
		if (mRingManager != null) {
			mRingManager.stop();
			mRingManager = null;
		}
	}

	public void runPendingLogin() {
		if (mUser != null) {
			mClient.login(mUser.mUsername, mUser.mPassword, mUser.mStun, mUser.mTurn, mUser.mTurnUsername, mUser.mTurnPassword , mUser.mXmppHost,
			              mUser.mXmppPort, mUser.mXmppUseSsl, 0); //0 is port filtering probably should be disabled
		}
	}

    private void loggedOut(){
        Intent intent;
        intent = new Intent(CallUIIntent.LOGGED_OUT);
        dispatchLocalIntent(intent);
        endAllCalls();
        releaseClient();
        if (ConnectionMonitor.isOnline() && mReconnect){
            startReconnectTimer();
        } else {
            stopReconnectTimer();
        }
    }

	public void dispatchLocalIntent(Intent intent) {
		final int N = mCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mCallbacks.getBroadcastItem(i).dispatchLocalIntent(intent);
			} catch (RemoteException e) {
				// The RemoteCallbackList will take care of removing
				// the dead object for us.
			}
		}
		mCallbacks.finishBroadcast();
	}

	public void sendBundle(Bundle bundle) {
		final int N = mCallbacks.beginBroadcast();
		for (int i = 0; i < N; i++) {
			try {
				mCallbacks.getBroadcastItem(i).sendBundle(bundle);
			} catch (RemoteException e) {
				// The RemoteCallbackList will take care of removing
				// the dead object for us.
			}
		}
		mCallbacks.finishBroadcast();
	}

	public void storeLoginAndLogin(String username, String password, String turnUsername, 
            String turnPassword, String xmppHost, int xmppPort, boolean xmppUseSsl){
	   String stunServer = getStringPref(R.string.stunserver_key, R.string.stunserver_value);
	   String turnServer = getStringPref(R.string.turnserver_key, R.string.turnserver_value);

	   mUser = new User(username, password, stunServer, turnServer, turnUsername, turnPassword, xmppHost, xmppPort, xmppUseSsl);
       internalLogin();
	}

	public void internalLogin(){
	    if (mClientInited) {
            runPendingLogin();
        } else {
            mClient.init(getApplicationContext());
        }
	}

	/*
	 * Binder Interface implementation.
	 */
	private final IVoiceClientService.Stub mBinder = new IVoiceClientService.Stub() {
		public void acceptCall(long callId) throws RemoteException {
			mClient.acceptCall(callId);
		}

		public void call(String remoteJid) throws RemoteException {
		    if (ConnectionMonitor.hasSlowConnection()) {
		        //Throw warning to user.
		    } else {
            mClient.call(remoteJid);
		    }
		}

		public void declineCall(long callId, boolean busy)
				throws RemoteException {
			mClient.declineCall(callId, busy);
		}

		public void toggleMute(long callId) throws RemoteException {
			Call call = mCallMap.get(Long.valueOf(callId));
			if (call != null) {
				call.setMute(!call.isMuted());
				mClient.muteCall(callId, call.isMuted());
			}
		}

		public void toggleHold(long callId) throws RemoteException {
			Call call = mCallMap.get(Long.valueOf(callId));
			if (call != null) {
				call.setHold(!call.isHeld());
				mClient.holdCall(callId, call.isHeld());
			}
		}

		public void endCall(long callId) throws RemoteException {
			mClient.endCall(callId);
		}

		public void login(String username, String password, String turnUsername, String turnPassword,
				String xmppHost, int xmppPort, boolean xmppUseSsl)
				throws RemoteException {
		    mReconnect = true;
		    storeLoginAndLogin(username, password, turnUsername, turnPassword, xmppHost, xmppPort, xmppUseSsl);
		}

		public void logout() throws RemoteException {
		    mReconnect = false;
		    mUser = null;
			mClient.logout();
		}

		public void release() throws RemoteException {
			releaseClient();
		}

		/*
		 * public void getBuddyList() throws RemoteException { return
		 * mBuddyList; //Implement me. }
		 */
		/*
		 * public CallHistoryList getCallHistory() throws RemoteException {
		 * return mCallHistory; // Implement me., list of remoteJids with
		 * states, call duration. // Should probably be stored in phone storage.
		 * }
		 */
		public void registerCallback(IVoiceClientServiceCallback cb) {
			if (cb != null)
				mCallbacks.register(cb);
		}

		public void unregisterCallback(IVoiceClientServiceCallback cb) {
			if (cb != null)
				mCallbacks.unregister(cb);
		}
	};

	private void initClientWrapper() {
		mClient = VoiceClient.getInstance();
		mHandler = new VoiceClientEventHandler(this);
		mClient.setHandler(mHandler);
	}

	private boolean getBooleanPref(int key, int defaultValue) {
		return Boolean.valueOf(getStringPref(key, defaultValue));
	}

	private int getIntPref(int key, int defaultValue) {
		return Integer.valueOf(getStringPref(key, defaultValue));
	}

	private String getStringPref(int key, int defaultValue) {
		return mSettings.getString(getString(key), getString(defaultValue));
	}
}

package com.tuenti.voice.example.util;

import java.util.Random;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.tuenti.voice.example.R;

/**
 * Helper to send notifications.
 * 
 * @author Wijnand Warren
 */
public class CallNotification {

	private static final String LOG_TAG = "CallNotification";
	private static final String VOIP_CALL_NOTIFICATION_TAG = "com.tuenti.voice.example.util.CallNotification";

	private static final int VOIP_MISSED_CALL_BASE_ID = 1000;
	private static final int VOIP_INCOMING_CALL_ID = 10;

	private NotificationManager notificationManager;
	private Context context;

	/**
	 * CONSTRUCTOR
	 */
	public CallNotification(Context context) {
		this.context = context;

		init();
	}

	/**
	 * Initializes this class.
	 */
	private void init() {
		notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	/**
	 * Constructs a generic notification and sends it.
	 * 
	 * @param iconId
	 * @param message
	 * @param notificationId
	 */
	private void sendNotification(int iconId, String message,
			int notificationId, Intent intent) {
		Notification notification = null;
		// @TargetApi(11)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			notification = createNotificationLegacy(iconId, message, intent);
		} else {
			notification = createNotificationHoneycomb(iconId, message, intent);
		}

		notificationManager.notify(VOIP_CALL_NOTIFICATION_TAG, notificationId,
				notification);
	}

	/**
	 * Creates a Notification the old fashioned (aka < Honeycomb) way.
	 * 
	 * NOTE: Sucks that we use deprecated methods, but we're supporting API 8+.
	 * 
	 * @param iconId
	 * @param message
	 * @return
	 */
	private Notification createNotificationLegacy(int iconId, String message,
			Intent intent) {
		Notification notification = new Notification(iconId, message,
				System.currentTimeMillis());
		notification.defaults = Notification.FLAG_AUTO_CANCEL;

		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				intent, 0);
		notification.setLatestEventInfo(context,
				context.getString(R.string.app_name), message, pendingIntent);

		return notification;
	}

	/**
	 * Creates a Notification the new (aka >= Honeycomb) way.
	 * 
	 * @param iconId
	 * @param message
	 * @param intent
	 * @return
	 */
	@TargetApi(11)
	private Notification createNotificationHoneycomb(int iconId,
			String message, Intent intent) {
		return new Notification.Builder(context)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setContentTitle(context.getString(R.string.app_name))
				.setContentText(message)
				.setTicker(message)
				.setContentIntent(
						PendingIntent.getActivity(context, 0, intent, 0))
				.setSmallIcon(iconId).getNotification();
	}

	/**
	 * Sends a call notification.
	 * 
	 * @param message
	 */
	public void sendCallNotification(String message, Intent intent) {
		Log.v(LOG_TAG, "sendCallNotification(): " + message);

		sendNotification(R.drawable.notification_generic, message,
				VOIP_INCOMING_CALL_ID, intent);
	}

	/**
	 * Cancels the current call notification.
	 */
	public void cancelCallNotification() {
		notificationManager.cancel(VOIP_CALL_NOTIFICATION_TAG,
				VOIP_INCOMING_CALL_ID);
	}

	/**
	 * Adds a missed call notification.
	 * 
	 * @param message
	 */
	public void sendMissedCallNotification(String message, Intent intent) {
		Random randomGenerator = new Random();
		sendNotification(R.drawable.notification_generic, message,
				VOIP_MISSED_CALL_BASE_ID + randomGenerator.nextInt(1000),
				intent);
	}

}

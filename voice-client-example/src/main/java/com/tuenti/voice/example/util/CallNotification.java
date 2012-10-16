package com.tuenti.voice.example.util;

import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

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
	private void sendNotification(int iconId, String tickerText,
			String message, int notificationId, Intent intent,
			String cancelAction) {
		Notification notification = new NotificationCompat.Builder(context)
			.setWhen(System.currentTimeMillis())
			.setAutoCancel(true)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(message)
			.setTicker(tickerText)
			.setContentIntent(
					PendingIntent.getActivity(context, 0, intent,
							PendingIntent.FLAG_CANCEL_CURRENT))
			.setContent(
					getCustomNotificationView(
							context.getString(R.string.app_name), message,
							// TODO: Replace with constant.
							cancelAction, intent.getLongExtra("callId", 0)))
			.setSmallIcon(iconId)
			.getNotification();

		notificationManager.notify(VOIP_CALL_NOTIFICATION_TAG, notificationId,
				notification);
	}

	/**
	 * Creates a custom notification display.
	 * 
	 * @param title
	 *            The title to display.
	 * @param body
	 *            The body text.
	 */
	private RemoteViews getCustomNotificationView(String title, String body,
			String cancelAction, long callId) {
		RemoteViews customView = new RemoteViews(context.getPackageName(),
				R.layout.voip_notification);

		// Add button click handler.
		if (cancelAction != null) {
			Intent cancelIntent = new Intent(cancelAction);
			// TODO: Replace with constant.
			cancelIntent.putExtra("callId", callId);
			
			customView.setOnClickPendingIntent(R.id.cancel_button,
					PendingIntent.getBroadcast(context, 0, cancelIntent,
							PendingIntent.FLAG_CANCEL_CURRENT));
		}

		// Set all other props.
		customView.setTextViewText(R.id.title, title);
		customView.setTextViewText(R.id.body_text, body);

		return customView;
	}

	/**
	 * Sends a call notification.
	 * 
	 * @param message
	 */
	public void sendCallNotification(String tickerText, String message,
			Intent intent, String cancelAction) {
		sendNotification(R.drawable.notification_generic, tickerText, message,
				VOIP_INCOMING_CALL_ID, intent, cancelAction);
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
		sendNotification(R.drawable.notification_generic, message, message,
				VOIP_MISSED_CALL_BASE_ID + randomGenerator.nextInt(1000),
				intent, null);
	}

}

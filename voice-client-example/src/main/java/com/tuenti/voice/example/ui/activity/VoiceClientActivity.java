package com.tuenti.voice.example.ui.activity;

// android imports
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tuenti.voice.core.BuddyListState;
import com.tuenti.voice.core.CallState;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.VoiceClientEventCallback;
import com.tuenti.voice.core.VoiceClientEventHandler;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.example.R;
import com.tuenti.voice.example.ui.dialog.IncomingCallDialog;
import com.tuenti.voice.example.VoiceClientApplication;
import com.tuenti.voice.example.service.IVoiceClientService;
import com.tuenti.voice.example.service.CallIntent;
import com.tuenti.voice.example.service.CallUIIntent;

public class VoiceClientActivity extends Activity implements
        View.OnClickListener {
    // ------------------------------ FIELDS ------------------------------

    private static final String TAG = "VoiceClientActivity";

    // Template Google Settings

    private static final String TO_USER = "user@gmail.com";

    private static final String MY_USER = "username@mydomain.com";

    private static final String MY_PASS = "pass";

    private SharedPreferences mSettings;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("libjingle",
                    "libjingle local receiver: " + intent.getAction());
            if (intent.getAction().equals(CallUIIntent.LOGGED_OUT)) {
                changeStatus("Logged out");
            } else if (intent.getAction().equals(CallUIIntent.LOGGED_IN)) {
                changeStatus("Logged in");
            }
        }
    };

    // ------------------------ INTERFACE METHODS ------------------------

    // --------------------- Interface OnClickListener ---------------------

    public void onClick(View view) {
        Intent intent;

        switch (view.getId()) {
        case R.id.login_btn:
            intent = new Intent(CallIntent.LOGIN);
            changeStatus("Logging in");
            intent.putExtra("username", MY_USER);
            intent.putExtra("password", MY_PASS);
            intent.putExtra(
                    "xmppHost",
                    getStringPref(R.string.xmpp_host_key,
                            R.string.xmpp_host_value));
            intent.putExtra(
                    "xmppPort",
                    getIntPref(R.string.xmpp_port_key, R.string.xmpp_port_value));
            intent.putExtra(
                    "xmppUseSSL",
                    getBooleanPref(R.string.xmpp_use_ssl_key,
                            R.string.xmpp_use_ssl_value));
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    intent);
            break;
        case R.id.logout_btn:
            intent = new Intent(CallIntent.LOGOUT);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    intent);
            break;
        case R.id.place_call_btn:
            intent = new Intent(CallIntent.PLACE_CALL);
            intent.putExtra("remoteJid", TO_USER);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    intent);
            break;
        }
    }

    // -------------------------- OTHER METHODS --------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Set default preferences
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        initClientWrapper();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(
                mReceiver);
    }

    private void changeStatus(String status) {
        ((TextView) findViewById(R.id.status_view)).setText(status);
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

    private void initClientWrapper() {
        findViewById(R.id.login_btn).setOnClickListener(this);
        findViewById(R.id.logout_btn).setOnClickListener(this);
        findViewById(R.id.place_call_btn).setOnClickListener(this);
    }

    private void setupReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallUIIntent.LOGGED_IN);
        intentFilter.addAction(CallUIIntent.LOGGED_OUT);
        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(
                mReceiver, intentFilter);
    }
}

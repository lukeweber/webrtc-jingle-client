package com.tuenti.voice.example.ui.dialog;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tuenti.voice.example.R;
import com.tuenti.voice.example.service.CallIntent;
import com.tuenti.voice.example.service.CallUIIntent;

public class IncomingCallDialog extends Activity implements
        View.OnClickListener {

    // ------------------------------ FIELDS ------------------------------

    private long mCallId;
    private String mRemoteJid;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        mCallId = intent.getLongExtra("callId", 0);
        mRemoteJid = intent.getStringExtra("remoteJid");

        setContentView(R.layout.incomingcalldialog);
        initOnClickListeners();

        ((TextView) findViewById(R.id.title)).setText("Incoming call from "
                + mRemoteJid);
        setupReceiver();
    }

    private void initOnClickListeners() {
        findViewById(R.id.accept_call_btn).setOnClickListener(this);
        findViewById(R.id.decline_call_btn).setOnClickListener(this);
    }

    private void setupReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CallUIIntent.CALL_PROGRESS);
        intentFilter.addAction(CallUIIntent.CALL_ENDED);
        intentFilter.addAction(CallUIIntent.LOGGED_OUT);
        LocalBroadcastManager.getInstance(getBaseContext()).registerReceiver(
                mReceiver, intentFilter);
    }

    // ------------------------ INTERFACE METHODS ------------------------
    // --------------------- Interface OnClickListener ---------------------

    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
        case R.id.accept_call_btn:
            intent = new Intent(CallIntent.ACCEPT_CALL);
            intent.putExtra("callId", mCallId);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    intent);
            // intent call in progress start
            break;
        case R.id.decline_call_btn:
            intent = new Intent(CallIntent.REJECT_CALL);
            intent.putExtra("callId", mCallId);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(
                    intent);
            break;
        }
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getBaseContext()).unregisterReceiver(
                mReceiver);
    }
}

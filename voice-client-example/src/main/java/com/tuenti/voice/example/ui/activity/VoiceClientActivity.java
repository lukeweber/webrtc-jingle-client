package com.tuenti.voice.example.ui.activity;

// android imports
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
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

public class VoiceClientActivity
    extends Activity
    implements View.OnClickListener, SensorEventListener
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "VoiceClientActivity";

    // Template Google Settings
    //private static final String TO_USER = "user@gmail.com";

    //private static final String MY_USER = "username@mydomain.com";

    //private static final String MY_PASS = "pass";
    private static final String TO_USER = "luke@tuenti.com";

    private static final String MY_USER = "lukewebertest@gmail.com";

    private static final String MY_PASS = "testtester";

    private static final float ON_EAR_DISTANCE = 3.0f;

    // Service Related Methods.
    IVoiceClientService mService = null;

    private boolean mIsBound;

    // Ringtones
    private AudioManager mAudioManager;

    private Ringtone mRingerPlayer;

    private Vibrator mVibrator;

    // Sensors
    private SensorManager mSensorManager;

    private Sensor mProximity;

    private float mMaxRangeProximity;

    // Wake lock
    private PowerManager mPowerManager;

    private WakeLock mWakeLock;

    private int mWakeLockState;

    // UI lock flag
    private boolean mUILocked = false;

    private SharedPreferences mSettings;

    private long currentCallId = 0;

    private boolean callInProgress = false;

    private static String cleanJid( String jid )
    {
        if ( jid == null )
        {
            return "";
        }

        int index = jid.indexOf( '/' );
        if ( index > 0 )
        {
            return jid.substring( 0, index );
        }
        return jid;
    }
    

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    public void onClick( View view )
    {
        if( mService == null ){
           mService = VoiceClientApplication.getService(); 
        }
        
        if ( mUILocked ) {
            return;
        }
        switch ( view.getId() )
        {
            case R.id.init_btn:
                //sendBroadcast(new Intent(CallIntent.PLACE_CALL));
                try {
                    String stunServer = getStringPref( R.string.stunserver_key, R.string.stunserver_value );
                    String relayServer = getStringPref( R.string.relayserver_key, R.string.relayserver_value );
                    String turnServer = getStringPref( R.string.turnserver_key, R.string.turnserver_value );
                    mService.init( stunServer, relayServer, relayServer, relayServer, turnServer );
                } catch ( RemoteException $e ) {
                }
                break;
            case R.id.release_btn:
                try {
                    mService.release();
                } catch ( RemoteException $e ) {
                }
                break;
            case R.id.login_btn:
                try {
                    String xmppHost = getStringPref( R.string.xmpp_host_key, R.string.xmpp_host_value );
                    int xmppPort = getIntPref( R.string.xmpp_port_key, R.string.xmpp_port_value );
                    boolean xmppUseSSL = getBooleanPref( R.string.xmpp_use_ssl_key, R.string.xmpp_use_ssl_value );
                    mService.login( MY_USER, MY_PASS, xmppHost, xmppPort, xmppUseSSL);
                } catch ( RemoteException $e ) {
                }
                break;
            case R.id.logout_btn:
                try {
                    mService.logout();
                } catch ( RemoteException $e ) {
                }
                break;
            case R.id.place_call_btn:
                try {
                    mService.call( TO_USER );
                } catch ( RemoteException $e ) {
                }
                
                break;
            case R.id.hang_up_btn:
                /*
                Have to add a callid here.
                try {
                    mService.hangup();
                } catch ( RemoteException $e ) {
                }*/
                break;
        }
    }

    public void onCallInProgress(){
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mMaxRangeProximity = mProximity.getMaximumRange();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onCallDestroy(){
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        releaseWakeLock();
        turnScreenOn(true);
        mUILocked = false;
    }

    private void turnScreenOn(boolean on) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= LayoutParams.FLAG_KEEP_SCREEN_ON;
        if ( on ) {
            // less than 0 returns to default behavior.
            params.screenBrightness = -1;
        } else {
            params.screenBrightness = 0;
        }
        getWindow().setAttributes(params);
    }


    /* Methods for SensorEventListener */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Proximity Sensor Event returns cm from phone.
        // Proximity in some devices is anything less than mMaxRangeProximity
        // on my test phone 9.0f or 0.0f for the two states.
        // Others might measure it more accurately.
        // TODO(Luke): Headset case isn't covered here at all, in which case we probably
        // want to probably do partial_wake_lock and not change the screen brightness.
        if ( event.values[0] < mMaxRangeProximity && event.values[0] <= ON_EAR_DISTANCE) {
            setWakeLockState(PowerManager.PARTIAL_WAKE_LOCK);
            turnScreenOn(false);
            mUILocked = true;
        } else {
            setWakeLockState(PowerManager.FULL_WAKE_LOCK);
            turnScreenOn(true);
            mUILocked = false;
        }
    }
    /* End Methods for SensorEventListener */

    /* Wake lock related logic */
    private void initWakeLock(){
        if ( mPowerManager == null ) {
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
    }

    private void setWakeLockState(int newState){
        if ( mWakeLockState != newState ) {
            if ( mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
            mWakeLockState = newState;
            mWakeLock = mPowerManager.newWakeLock(newState, "In Call wake lock: " + newState);
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock(){
        if ( mWakeLock != null ) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
    /* End wake lock related logic */

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.main );

        // Set default preferences
        mSettings = PreferenceManager.getDefaultSharedPreferences( this );

        initAudio();
        initClientWrapper();
        initWakeLock();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        releaseWakeLock();
    }

    private void changeStatus( String status )
    {
        ( (TextView) findViewById( R.id.status_view ) ).setText( status );
    }

    private void displayIncomingCall( String remoteJid, long callId )
    {
        // start ringing
        startIncomingRinging();

        // and display the incoming call dialog
        //Dialog incomingCall = new IncomingCallDialog( this, mClient, cleanJid( remoteJid ), callId ).create();
        //incomingCall.show();
    }

    private boolean getBooleanPref( int key, int defaultValue )
    {
        return Boolean.valueOf( getStringPref( key, defaultValue ) );
    }

    private int getIntPref( int key, int defaultValue )
    {
        return Integer.valueOf( getStringPref( key, defaultValue ) );
    }

    private String getStringPref( int key, int defaultValue )
    {
        return mSettings.getString( getString( key ), getString( defaultValue ) );
    }

    private void initAudio()
    {
        mAudioManager = (AudioManager) getSystemService( Context.AUDIO_SERVICE );
    }

    private void initClientWrapper()
    {
        findViewById( R.id.init_btn ).setOnClickListener( this );
        findViewById( R.id.release_btn ).setOnClickListener( this );
        findViewById( R.id.login_btn ).setOnClickListener( this );
        findViewById( R.id.logout_btn ).setOnClickListener( this );
        findViewById( R.id.place_call_btn ).setOnClickListener( this );
        findViewById( R.id.hang_up_btn ).setOnClickListener( this );
    }

    private void resetAudio()
    {
        mAudioManager.setMode( AudioManager.MODE_NORMAL );
    }

    private synchronized void ringIncoming( Uri uri )
    {
        int ringerMode = mAudioManager.getRingerMode();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if( callInProgress ) {
            // Notify with single vibrate.
            mVibrator.vibrate((long)200);
        } else {
            if( AudioManager.RINGER_MODE_NORMAL == ringerMode) {
                mAudioManager.setMode( AudioManager.MODE_RINGTONE );
                ring(uri, AudioManager.STREAM_RING);
            } else if( AudioManager.RINGER_MODE_VIBRATE == ringerMode) {

                // Start immediately
                //Vibrate 400, break 200, Vibrate 400, break 1000
                long[] pattern = { 0, 400, 200, 400, 1000 };

                // Vibrate until cancelled.
                mVibrator.vibrate(pattern, 0);
            }  // else RINGER_MODE_SILENT
        }
    }

    private synchronized void ringOutgoing( Uri uri )
    {
        mAudioManager.setMode( AudioManager.MODE_NORMAL );
        ring(uri, AudioManager.STREAM_VOICE_CALL);
    }

    private synchronized void playNotification()
    {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringOutgoing(notification);
    }

    private synchronized void ring( Uri uri, int streamType)
    {
        try
        {
            if ( mRingerPlayer != null )
            {
                mRingerPlayer.stop();
            }
            mRingerPlayer = RingtoneManager.getRingtone( getApplicationContext(), uri );
            mRingerPlayer.setStreamType(streamType);
            mRingerPlayer.play();
        }
        catch ( Exception e )
        {
            Log.e( TAG, "error ringing", e );
        }
    }

    private void setAudioForCall()
    {
        mAudioManager.setMode( ( Build.VERSION.SDK_INT < 11 )
                                   ? AudioManager.MODE_IN_CALL
                                   : AudioManager.MODE_IN_COMMUNICATION );
        mAudioManager.requestAudioFocus( null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT );
    }

    private synchronized void startIncomingRinging()
    {
        Uri notification = RingtoneManager.getDefaultUri( RingtoneManager.TYPE_RINGTONE );
        ringIncoming( notification );
    }

    private synchronized void startOutgoingRinging()
    {
        Uri notification = Uri.parse( "android.resource://com.tuenti.voice.example/raw/outgoing_call_ring" );
        ringOutgoing( notification );
    }

    private synchronized void stopRinging()
    {
        if ( mRingerPlayer != null )
        {
            mAudioManager.setMode( AudioManager.MODE_NORMAL );
            mRingerPlayer.stop();
            mRingerPlayer = null;
        }

        if( mVibrator != null )
        {
            mVibrator.cancel();
            mVibrator = null;
        }
    }
}

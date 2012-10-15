/*
 * Copyright (c) 2011 The WebRTC project authors. All Rights Reserved.
 *
 * Use of this source code is governed by a BSD-style license that can be found
 * in the LICENSE file in the root of the source tree. An additional
 * intellectual property rights grant can be found in the file PATENTS. All
 * contributing project authors may be found in the AUTHORS file in the root of
 * the source tree.
 */

/*
 * VoiceEngine Android test application. It starts either auto test or acts like
 * a GUI test.
 */

package com.tuenti.voice.core;

import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.util.Log;

public class VoiceClient
{
// ------------------------------ FIELDS ------------------------------

    //Event constants
    /* Event Types */
    public static final int BUDDY_LIST_EVENT = 3;

    public static final int CALL_STATE_EVENT = 0;

    public static final int XMPP_ERROR_EVENT = 2;

    public static final int XMPP_SOCKET_CLOSE_EVENT = 4;

    public static final int XMPP_STATE_EVENT = 1;
    //End Event constants

    private final static String TAG = "j-libjingle-webrtc";

    private static VoiceClient instance;

    private static final Object mutex = new Object();

    private static Handler mHandler;

    private boolean initialized;

// -------------------------- STATIC METHODS --------------------------

    public static VoiceClient getInstance()
    {
        if ( instance == null )
        {
            instance = new VoiceClient();
        }
        return instance;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private VoiceClient()
    {
        synchronized ( mutex )
        {
            loadLibrary( "voiceclient" );
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public void acceptCall( long call_id )
    {
        Log.i( TAG, "native accept call " + call_id );
        nativeAcceptCall( call_id );
    }

    public void call( String remoteUsername )
    {
        nativeCall( remoteUsername );
    }

    public void declineCall( long call_id, boolean busy )
    {
        nativeDeclineCall( call_id, busy );
    }

    public void destroy()
    {
        instance = null;
    }

    public void endCall( long call_id )
    {
        nativeEndCall( call_id );
    }

    public void holdCall( long call_id, boolean hold )
    {
        nativeHoldCall( call_id, hold );
    }

    public void init( String stunServer, String relayServerUDP, String relayServerTCP, String relayServerSSL,
                      String turnServer )
    {
        if ( !initialized )
        {
            nativeInit( stunServer, relayServerUDP, relayServerTCP, relayServerSSL, turnServer );
            initialized = true;
        }
    }

    public void login( String username, String password, String turnPassword, String xmppServer, int xmppPort,
                       boolean useSsl )
    {
        nativeLogin( username, password, turnPassword, xmppServer, xmppPort, useSsl );
    }

    public void logout()
    {
        nativeLogout();
    }

    public void muteCall( long call_id, boolean mute )
    {
        nativeMuteCall( call_id, mute );
    }

    public void release()
    {
        if ( initialized )
        {
            initialized = false;
            nativeRelease();
        }
    }

    public void setHandler( Handler handler )
    {
        mHandler = handler;
    }

    @SuppressWarnings("UnusedDeclaration")
    private void dispatchNativeEvent( int what, int code, String remoteJid, long callId )
    {
        VoiceClient client = getInstance();
        if ( client != null && client.mHandler != null )
        {
            Message msg = Message.obtain( client.mHandler, what );
            Bundle bundle = new Bundle();
            bundle.putInt( "code", code );
            bundle.putString( "remoteJid", remoteJid );
            bundle.putLong( "callId", callId );
            msg.setData( bundle );
            msg.sendToTarget();
        }
    }

    private void loadLibrary( String name )
    {
        Log.i( TAG, "loading native library " + name );
        System.loadLibrary( name );
    }

    private native void nativeAcceptCall( long call_id );

    private native void nativeCall( String remoteJid );

    private native void nativeDeclineCall( long call_id, boolean busy );

    private native void nativeEndCall( long call_id );

    private native void nativeHoldCall( long call_id, boolean hold );

    private native void nativeInit( String stunServer, String relayServerUDP, String relayServerTCP,
                                    String relayServerSSL, String turnServer );

    private native void nativeLogin( String user_name, String password, String turnPassword, String xmppServer,
                                     int xmppPort, boolean UseSSL );

    private native void nativeLogout();

    private native void nativeMuteCall( long call_id, boolean mute );

    private native void nativeRelease();
}

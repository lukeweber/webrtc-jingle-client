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

package com.tuenti.voice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class VoiceClient
{
// ------------------------------ FIELDS ------------------------------

    //Event constants
    /* Event Types */
    public static final int CALL_STATE_EVENT = 0;

    public static final int XMPP_ENGINE_EVENT = 1;

    public static final int XMPP_ERROR_EVENT = 2;

    /* Call Event States */
    public final static int CALL_CALLING = 0;

    public final static int CALL_ANSWERED = 1;

    public final static int CALL_REJECTED = 2;

    public final static int CALL_INPROGRESS = 3;

    public final static int CALL_RECIVEDTERMINATE = 4;

    public final static int CALL_INCOMING = 5;

    /* Xmpp Engine Event States */ 
    public final static int XMPP_ENGINE_CLOSED = 0;

    public final static int XMPP_ENGINE_OPEN = 1;

    public final static int XMPP_ENGINE_OPENING = 2;

    public final static int XMPP_ENGINE_START = 3;


    /* XMPP Error Event States */
    public final static int XMPP_ERROR_NONE = 0;

    public final static int XMPP_ERROR_XML = 1;

    public final static int XMPP_ERROR_STREAM = 2;

    public final static int XMPP_ERROR_VERSION = 3;

    public final static int XMPP_ERROR_UNAUTH = 4;

    public final static int XMPP_ERROR_TLS = 5;

    public final static int XMPP_ERROR_AUTH = 6;

    public final static int XMPP_ERROR_BIND = 7;

    public final static int XMPP_ERROR_CONN_CLOSED = 8;

    public final static int XMPP_ERROR_DOC_CLOSED = 9;

    public final static int XMPP_ERROR_SOCK_ERR = 10;

    public final static int XMPP_ERROR_UNKNOWN = 11;
    //End Event constants

    private final static String TAG = "j-libjingle-webrtc";

    private static VoiceClient instance;

    private static final Object mutex = new Object();

    private Handler handler;

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

    private static void dispatchNativeEvent( int what, int code, String str1 )
    {
        VoiceClient client = getInstance();
        if ( client != null && client.handler != null )
        {
            Bundle bundle = new Bundle();
            bundle.putString( "str1", str1 );

            Message msg = client.handler.obtainMessage( what, code, -1 );
            msg.setData( bundle );
            client.handler.sendMessage( msg );
        }
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private VoiceClient()
    {
        synchronized ( mutex )
        {
            loadLibrary( "stlport_shared" );
            loadLibrary( "crypto_jingle" );
            loadLibrary( "webrtc_audio_preprocessing" );
            loadLibrary( "webrtc_voice" );
            loadLibrary( "jingle" );
            loadLibrary( "webrtc-voice-demo-jni" );
        }
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public void setHandler( VoiceClientEventHandler handler )
    {
        this.handler = handler;
    }

// -------------------------- OTHER METHODS --------------------------

    public void acceptCall()
    {
        nativeAcceptCall();
    }

    public void call( String remoteUsername )
    {
        nativeCall( remoteUsername );
    }

    public void declineCall()
    {
        nativeDeclineCall();
    }

    public void destroy()
    {
        nativeDestroy();
        instance = null;
    }

    public void endCall()
    {
        nativeEndCall();
    }

    public void init()
    {
        if ( !initialized )
        {
            nativeInit();
            initialized = true;
        }
    }

    public void login( String username, String password, String server, boolean useSsl )
    {
        nativeLogin( username, password, server, useSsl );
    }

    public void logout()
    {
        nativeLogout();
    }

    public void release()
    {
        if ( initialized )
        {
            initialized = false;
            nativeRelease();
        }
    }

    private void loadLibrary( String name )
    {
        Log.i( TAG, "loading native library " + name );
        System.loadLibrary( name );
    }

    private native void nativeAcceptCall();

    private native void nativeCall( String remoteJid );

    private native void nativeDeclineCall();

    private native void nativeDestroy();

    private native void nativeEndCall();

    private native void nativeInit();

    private native void nativeLogin( String user_name, String password, String server, boolean UseSSL );

    private native void nativeLogout();

    private native void nativeRelease();
}

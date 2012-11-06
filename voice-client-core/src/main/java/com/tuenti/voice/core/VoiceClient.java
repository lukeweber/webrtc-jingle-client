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

import android.util.Log;

import java.util.LinkedList;

public class VoiceClient
{
// ------------------------------ FIELDS ------------------------------

    //Event constants
    /* Event Types */
    public static final int BUDDY_LIST_EVENT = 3;

    public static final int CALL_ERROR_EVENT = 5;

    public static final int CALL_STATE_EVENT = 0;

    public static final int XMPP_ERROR_EVENT = 2;

    public static final int XMPP_SOCKET_CLOSE_EVENT = 4;

    public static final int XMPP_STATE_EVENT = 1;
    //End Event constants

    private final static String TAG = "j-libjingle-webrtc";

    private static final Object mLock = new Object();

    private boolean initialized;

    private LinkedList<CallListener> mCallListeners = new LinkedList<CallListener>();

    private LinkedList<ConnectionListener> mConnectionListeners = new LinkedList<ConnectionListener>();

    private LinkedList<RosterListener> mRosterListeners = new LinkedList<RosterListener>();

// --------------------------- CONSTRUCTORS ---------------------------

    public VoiceClient()
    {
        synchronized ( mLock )
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

    /**
     * Add a listener for call changes
     *
     * @param l CallListener
     */
    public void addCallListener( CallListener l )
    {
        synchronized ( mLock )
        {
            mCallListeners.add( l );
        }
    }

    /**
     * Add a listener for connection changes
     *
     * @param l ConnectionListener
     */
    public void addConnectionListener( ConnectionListener l )
    {
        synchronized ( mLock )
        {
            mConnectionListeners.add( l );
        }
    }

    /**
     * Add a listener for roster changes
     *
     * @param l RosterListener
     */
    public void addRosterListener( RosterListener l )
    {
        synchronized ( mLock )
        {
            mRosterListeners.add( l );
        }
    }

    public void call( String remoteUsername )
    {
        nativeCall( remoteUsername );
    }

    public void declineCall( long call_id, boolean busy )
    {
        nativeDeclineCall( call_id, busy );
    }

    public void endCall( long call_id )
    {
        nativeEndCall( call_id );
    }

    public void holdCall( long call_id, boolean hold )
    {
        nativeHoldCall( call_id, hold );
    }

    public void init( )
    {
        if ( !initialized )
        {
            nativeInit();
            initialized = true;
        }
    }

    public void login( String username, String password, String stunServer, String turnServer, String turnPassword, String xmppServer, int xmppPort,
                       boolean useSsl )
    {
        nativeLogin( username, password, stunServer, turnServer, turnPassword, xmppServer, xmppPort, useSsl );
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

    /**
     * Removes the call listener
     *
     * @param l CallListener
     */
    public void removeCallListener( CallListener l )
    {
        synchronized ( mLock )
        {
            mCallListeners.remove( l );
        }
    }

    /**
     * Removes the connection listener
     *
     * @param l ConnectionListener
     */
    public void removeConnectionListener( ConnectionListener l )
    {
        synchronized ( mLock )
        {
            mConnectionListeners.remove( l );
        }
    }

    /**
     * Removes the roster listener
     *
     * @param l RosterListener
     */
    public void removeRosterListener( RosterListener l )
    {
        synchronized ( mLock )
        {
            mRosterListeners.remove( l );
        }
    }

    /**
     * @see RosterListener#handleBuddyListChanged(int, String)
     */
    protected void handleBuddyListChanged( int state, String remoteJid )
    {
        synchronized ( mLock )
        {
            for ( RosterListener l : mRosterListeners )
            {
                l.handleBuddyListChanged( state, remoteJid );
            }
        }
    }

    /**
     * @see CallListener#handleCallError(int, long)
     */
    protected void handleCallError( int error, long callId )
    {
        synchronized ( mLock )
        {
            for ( CallListener l : mCallListeners )
            {
                l.handleCallError( error, callId );
            }
        }
    }

    /**
     * @see CallListener#handleCallStateChanged(int, String, long)
     */
    protected void handleCallStateChanged( int state, String remoteJid, long callId )
    {
        synchronized ( mLock )
        {
            for ( CallListener l : mCallListeners )
            {
                l.handleCallStateChanged( state, remoteJid, callId );
            }
        }
    }

    /**
     * @see ConnectionListener#handleXmppError(int)
     */
    protected void handleXmppError( int error )
    {
        synchronized ( mLock )
        {
            for ( ConnectionListener l : mConnectionListeners )
            {
                l.handleXmppError( error );
            }
        }
    }

    /**
     * @see ConnectionListener#handleXmppSocketClose(int)
     */
    protected void handleXmppSocketClose( int state )
    {
        synchronized ( mLock )
        {
            for ( ConnectionListener l : mConnectionListeners )
            {
                l.handleXmppSocketClose( state );
            }
        }
    }

    /**
     * @see ConnectionListener#handleXmppStateChanged(int)
     */
    protected void handleXmppStateChanged( int state )
    {
        synchronized ( mLock )
        {
            for ( ConnectionListener l : mConnectionListeners )
            {
                l.handleXmppStateChanged( state );
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private void dispatchNativeEvent( int what, int code, String remoteJid, long callId )
    {
        switch ( what )
        {
            case CALL_STATE_EVENT:
                handleCallStateChanged( code, remoteJid, callId );
                break;
            case CALL_ERROR_EVENT:
                handleCallError( code, callId );
                break;
            case BUDDY_LIST_EVENT:
                handleBuddyListChanged( code, remoteJid );
                break;
            case XMPP_STATE_EVENT:
                handleXmppStateChanged( code );
                break;
            case XMPP_ERROR_EVENT:
                handleXmppError( code );
                break;
            case XMPP_SOCKET_CLOSE_EVENT:
                handleXmppSocketClose( code );
                break;
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

    private native void nativeInit();

    private native void nativeLogin( String user_name, String password, String stunServer, String turnServer, String turnPassword, String xmppServer,
                                     int xmppPort, boolean UseSSL );

    private native void nativeLogout();

    private native void nativeMuteCall( long call_id, boolean mute );

    private native void nativeRelease();
}

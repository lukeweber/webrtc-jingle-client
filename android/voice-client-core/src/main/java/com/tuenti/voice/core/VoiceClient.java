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

import android.content.Context;
import android.util.Log;
import com.tuenti.voice.core.manager.BuddyManager;
import com.tuenti.voice.core.manager.CallManager;
import com.tuenti.voice.core.manager.ConnectionManager;
import com.tuenti.voice.core.manager.StatManager;

public class VoiceClient
{
// ------------------------------ FIELDS ------------------------------

    //Event constants
    /* Event Types */
    public static final int AUDIO_PLAYOUT_EVENT = 6;

    public static final int BUDDY_LIST_EVENT = 3;

    public static final int CALL_ERROR_EVENT = 5;

    public static final int CALL_STATE_EVENT = 0;

    public static final int CALL_TRACKER_ID_EVENT = 8;

    public static final int STATS_UPDATE_EVENT = 7;

    public static final int XMPP_ERROR_EVENT = 2;

    public static final int XMPP_SOCKET_CLOSE_EVENT = 4;

    public static final int XMPP_STATE_EVENT = 1;
    //End Event constants

    private final static String TAG = "j-VoiceClient";

    private static final Object mLock = new Object();

    private boolean initialized;

    private BuddyManager mBuddyManager;

    private CallManager mCallManager;

    private ConnectionManager mConnectionManager;

    private StatManager mStatManager;

    private boolean voiceClientLoaded;

// --------------------------- CONSTRUCTORS ---------------------------

    public VoiceClient()
    {
        synchronized ( mLock )
        {
            Log.i( TAG, "loading native library voiceclient" );
            try
            {
                System.loadLibrary( "voice-native" );
                voiceClientLoaded = true;
            }
            catch ( UnsatisfiedLinkError e )
            {
                // We need to do this, because Android will generate OOM error
                // loading an so file on older phones when they don't have
                // enough available memory at load time.
                voiceClientLoaded = false;
            }
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public void acceptCall( long call_id )
    {
        Log.i( TAG, "native accept call " + call_id );
        if ( loaded() )
        {
            nativeAcceptCall( call_id );
        }
    }

    public void ping() {
        if ( loaded() )
        {
            nativePing();
        }
    }

    public void call( String remoteUsername )
    {
        if ( loaded() )
        {
            nativeCall( remoteUsername );
        }
    }

    public void callWithTrackerId( String remoteUsername, String callTrackerId )
    {
        if ( loaded() )
        {
            nativeCallWithTrackerId( remoteUsername, callTrackerId );
        }
    }

    public void declineCall( long call_id, boolean busy )
    {
        if ( loaded() )
        {
            nativeDeclineCall( call_id, busy );
        }
    }

    public void endCall( long call_id )
    {
        if ( loaded() )
        {
            nativeEndCall( call_id );
        }
    }

    public void holdCall( long call_id, boolean hold )
    {
        if ( loaded() )
        {
            nativeHoldCall( call_id, hold );
        }
    }

    public void init( Object context )
    {
        if ( loaded() && !initialized )
        {
            nativeInit( context );
            initialized = true;
        }
    }

    public boolean loaded()
    {
        return voiceClientLoaded;
    }

    public void login( String username, String password, String stunServer, String turnServer, String turnUsername,
                       String turnPassword, String xmppServer, int xmppPort, boolean useSsl, int portAllocatorFilter,
                       boolean isGtalk)
    {
        if ( loaded() )
        {
            nativeLogin( username,
                         password,
                         stunServer,
                         turnServer,
                         turnUsername,
                         turnPassword,
                         xmppServer,
                         xmppPort,
                         useSsl,
                         portAllocatorFilter,
                         isGtalk);
        }
    }

    public void logout()
    {
        if ( loaded() )
        {
            nativeLogout();
        }
    }

    public void muteCall( long call_id, boolean mute )
    {
        if ( loaded() )
        {
            nativeMuteCall( call_id, mute );
        }
    }

    public void release()
    {
        if ( loaded() && initialized )
        {
            /**
             * Release deletes the signal thread.
             * Signal thread can not be deleted from
             * the scope of the signal thread, which
             * can occur if on some events from the lib
             * you call release syncronously, causing
             * nativeRelease to never return.
             */
            Thread thread = new Thread()
            {
                @Override
                public void run() {
                    initialized = false;
                    nativeRelease();
                }
            };

            thread.start();
        }
    }

    public void replaceTurn( String turnServer )
    {
        if ( loaded() )
        {
            nativeReplaceTurn( turnServer );
        }
    }

    public void setBuddyManager( BuddyManager buddyManager )
    {
        mBuddyManager = buddyManager;
    }

    public void setCallManager( CallManager callManager )
    {
        mCallManager = callManager;
    }

    public void setConnectionManager( ConnectionManager connectionManager )
    {
        mConnectionManager = connectionManager;
    }

    public void setStatManager( StatManager statManager )
    {
        mStatManager = statManager;
    }

    /**
     * @see CallManager#handleAudioPlayout()
     */
    protected void handleAudioPlayout()
    {
        if ( mCallManager != null )
        {
            synchronized ( mLock )
            {
                mCallManager.handleAudioPlayout();
            }
        }
    }

    /**
     * @see BuddyManager#handleBuddyAdded(String, String, int, int)
     */
    protected void handleBuddyAdded( String remoteJid, String nick, int available, int show )
    {
        if ( mBuddyManager != null )
        {
            synchronized ( mLock )
            {
                mBuddyManager.handleBuddyAdded( remoteJid, nick, available, show );
            }
        }
    }

    /**
     * @see BuddyManager#handleBuddyListChanged(int, String)
     */
    protected void handleBuddyListChanged( int state, String remoteJid )
    {
        if ( mBuddyManager != null )
        {
            synchronized ( mLock )
            {
                mBuddyManager.handleBuddyListChanged( state, remoteJid );
            }
        }
    }

    /**
     * @see CallManager#handleCallError(int, long)
     */
    protected void handleCallError( int error, long callId )
    {
        if ( mCallManager != null )
        {
            synchronized ( mLock )
            {
                mCallManager.handleCallError( error, callId );
            }
        }
    }

    /**
     * @see CallManager#handleCallStateChanged(int, String, long)
     */
    protected void handleCallStateChanged( int state, String remoteJid, long callId )
    {
        if ( mCallManager != null )
        {
            synchronized ( mLock )
            {
                mCallManager.handleCallStateChanged( state, remoteJid, callId );
            }
        }
    }

    /**
     * @see CallManager#handleCallTrackerId(long, String)
     */
    protected void handleCallTrackerId( long callId, String callTrackerId )
    {
        if ( mCallManager != null )
        {
            synchronized ( mLock )
            {
                mCallManager.handleCallTrackerId( callId, callTrackerId );
            }
        }
    }

    /**
     * @see BuddyManager#handlePresenceChanged(String, int, int)
     */
    protected void handlePresenceChanged( String remoteJid, int available, int show )
    {
        if ( mBuddyManager != null )
        {
            synchronized ( mLock )
            {
                mBuddyManager.handlePresenceChanged( remoteJid, available, show );
            }
        }
    }

    /**
     * @see StatManager#handleStatsUpdate(String)
     */
    protected void handleStatsUpdate( String stats )
    {
        if ( mStatManager != null )
        {
            synchronized ( mLock )
            {
                mStatManager.handleStatsUpdate( stats );
            }
        }
    }

    /**
     * @see ConnectionManager#handleXmppError(int)
     */
    protected void handleXmppError( int error )
    {
        if ( mConnectionManager != null )
        {
            synchronized ( mLock )
            {
                mConnectionManager.handleXmppError( error );
            }
        }
    }

    /**
     * @see ConnectionManager#handleXmppSocketClose(int)
     */
    protected void handleXmppSocketClose( int state )
    {
        if ( mConnectionManager != null )
        {
            synchronized ( mLock )
            {
                mConnectionManager.handleXmppSocketClose( state );
            }
        }
    }

    /**
     * @see ConnectionManager#handleXmppStateChanged(int)
     */
    protected void handleXmppStateChanged( int state )
    {
        if ( mConnectionManager != null )
        {
            synchronized ( mLock )
            {
                mConnectionManager.handleXmppStateChanged( state );
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    //TODO: change the signature to be:
    //dispatchNativeEvent( int what, int code, String data )
    private void dispatchNativeEvent( int what, int code, String data, long callId )
    {
        switch ( what )
        {
            case CALL_STATE_EVENT:
                // data contains remoteJid
                handleCallStateChanged( code, data, callId );
                break;
            case CALL_ERROR_EVENT:
                handleCallError( code, callId );
                break;
            case BUDDY_LIST_EVENT:
                // data contains remoteJid
                handleBuddyListChanged( code, data );
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
            case STATS_UPDATE_EVENT:
                // data constains stats
                handleStatsUpdate( data );
                break;
            case CALL_TRACKER_ID_EVENT:
                // data contains call_tracking_id
                handleCallTrackerId( callId, data );
                break;
            case AUDIO_PLAYOUT_EVENT:
                handleAudioPlayout();
                break;
        }
    }

    private native void nativeAcceptCall( long call_id );

    private native void nativeCall( String remoteJid );

    private native void nativeCallWithTrackerId( String remoteJid, String callTrackerId );

    private native void nativeDeclineCall( long call_id, boolean busy );

    private native void nativeEndCall( long call_id );

    private native void nativeHoldCall( long call_id, boolean hold );

    private native void nativeInit( Object context );

    private native void nativeLogin( String user_name, String password, String stunServer, String turnServer,
                                     String turnUsername, String turnPassword, String xmppServer, int xmppPort,
                                     boolean UseSSL, int portAllocatorFilter, boolean isGtalk );

    private native void nativeLogout();

    private native void nativeMuteCall( long call_id, boolean mute );

    private native void nativeRelease();

    private native void nativePing();

    private native void nativeReplaceTurn( String turn );
}

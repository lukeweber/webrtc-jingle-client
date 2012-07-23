package com.tuenti.voice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class VoiceClientEventHandler
    extends Handler
{
// ------------------------------ FIELDS ------------------------------

    private VoiceClientEventCallback mCallback;

// --------------------------- CONSTRUCTORS ---------------------------

    public VoiceClientEventHandler( VoiceClientEventCallback callback )
    {
        mCallback = callback;
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void handleMessage( Message msg )
    {
        assert ( mCallback != null );

        Bundle bundle = msg.getData();
        int code = msg.arg1;

        switch ( msg.what )
        {
            case VoiceClient.CALL_STATE_EVENT:
                String remoteJid = bundle.getString( "str1" );
                mCallback.handleCallStateChanged( code, remoteJid );
                break;
            case VoiceClient.XMPP_ENGINE_EVENT:
                String message = bundle.getString( "str1" );
                mCallback.handleXmppEngineStateChanged( code, message );
                break;
            case VoiceClient.XMPP_ERROR_EVENT:
                mCallback.handleXmppError( code );
                break;
        }
    }
}

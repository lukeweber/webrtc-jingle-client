package com.tuenti.voice.example.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.example.R;

public class IncomingCallDialog
    extends AlertDialog.Builder
    implements DialogInterface.OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private VoiceClient mClient;
    private long mcallId;

// --------------------------- CONSTRUCTORS ---------------------------

    public IncomingCallDialog( final Context context, final VoiceClient client, final String remoteJid, long callId )
    {
        super( context );

        mClient = client;
        mcallId = callId;

        setTitle( R.string.voice_chat_invite );
        setMessage( remoteJid );
        setPositiveButton( R.string.accept_call, this );
        setNegativeButton( R.string.decline_call, this );
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface OnClickListener ---------------------

    @Override
    public void onClick( DialogInterface dialog, int which )
    {
        switch ( which )
        {
            case DialogInterface.BUTTON_POSITIVE:
                mClient.acceptCall(mcallId);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                mClient.declineCall(mcallId, true);
                break;
        }
    }
}

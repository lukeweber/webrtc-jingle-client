package com.tuenti.voice.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.tuenti.voice.R;
import com.tuenti.voice.VoiceClient;

public class IncomingCallDialog
    extends AlertDialog.Builder
    implements DialogInterface.OnClickListener
{
// ------------------------------ FIELDS ------------------------------

    private VoiceClient mClient;

// --------------------------- CONSTRUCTORS ---------------------------

    public IncomingCallDialog( final Context context, final VoiceClient client, final String remoteJid )
    {
        super( context );

        mClient = client;

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
                mClient.acceptCall();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                mClient.declineCall();
                break;
        }
    }
}

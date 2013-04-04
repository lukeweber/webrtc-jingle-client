package com.tuenti.voice.example.service;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.IBinder;
import com.tuenti.voice.core.service.VoiceClientService;

public final class AuthenticatedVoiceClientService
    extends VoiceClientService
{
// ------------------------------ FIELDS ------------------------------

    private static AccountAuthenticator AUTHENTICATOR;

// -------------------------- OTHER METHODS --------------------------

    public IBinder onBind( Intent intent )
    {
        if ( intent.getAction().equals( AccountManager.ACTION_AUTHENTICATOR_INTENT ) )
        {
            return getAuthenticator().getIBinder();
        }
        return super.onBind( intent );
    }

    private AccountAuthenticator getAuthenticator()
    {
        if ( AUTHENTICATOR == null )
        {
            AUTHENTICATOR = new AccountAuthenticator( this );
        }
        return AUTHENTICATOR;
    }
}

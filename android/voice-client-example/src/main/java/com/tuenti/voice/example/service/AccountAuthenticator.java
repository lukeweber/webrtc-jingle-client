package com.tuenti.voice.example.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.tuenti.voice.example.ui.connection.AddConnectionActivity;

import static android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE;
import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static android.accounts.AccountManager.KEY_INTENT;
import static com.tuenti.voice.example.ui.connection.AccountConstants.ACCOUNT_TYPE;

class AccountAuthenticator
    extends AbstractAccountAuthenticator
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "AccountAuthenticator";

    private Context mContext;

// --------------------------- CONSTRUCTORS ---------------------------

    public AccountAuthenticator( final Context context )
    {
        super( context );
        mContext = context;
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    public Bundle addAccount( final AccountAuthenticatorResponse response, final String accountType,
                              final String authTokenType, final String[] requiredFeatures, final Bundle options )
        throws NetworkErrorException
    {
        Log.d( TAG, "addAccount" );


        final Intent intent = new Intent( mContext, AddConnectionActivity.class );
        //intent.putExtra( PARAM_AUTH_TOKEN_TYPE, authTokenType );
        intent.putExtra( KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response );

        final Bundle bundle = new Bundle();
        bundle.putParcelable( KEY_INTENT, intent );
        return bundle;
    }

    @Override
    public Bundle confirmCredentials( AccountAuthenticatorResponse response, Account account, Bundle options )
    {
        return null;
    }

    @Override
    public Bundle editProperties( AccountAuthenticatorResponse response, String accountType )
    {
        return null;
    }

    @Override
    public Bundle getAuthToken( final AccountAuthenticatorResponse response, final Account account,
                                final String authTokenType, final Bundle options )
        throws NetworkErrorException
    {
        final Bundle bundle = new Bundle();
        if ( !ACCOUNT_TYPE.equals( authTokenType ) )
        {
            return bundle;
        }

        AccountManager manager = AccountManager.get( mContext );
        String password = manager.getPassword( account );
        if ( TextUtils.isEmpty( password ) )
        {
            bundle.putParcelable( KEY_INTENT, createLoginIntent( response ) );
            return bundle;
        }
        return bundle;
    }

    @Override
    public String getAuthTokenLabel( final String authTokenType )
    {
        if ( ACCOUNT_TYPE.equals( authTokenType ) )
        {
            return authTokenType;
        }
        else
        {
            return null;
        }
    }

    @Override
    public Bundle hasFeatures( final AccountAuthenticatorResponse accountAuthenticatorResponse, final Account account,
                               final String[] features )
        throws NetworkErrorException
    {
        final Bundle result = new Bundle();
        result.putBoolean( KEY_BOOLEAN_RESULT, false );
        return result;
    }

    @Override
    public Bundle updateCredentials( final AccountAuthenticatorResponse response, final Account account,
                                     final String authTokenType, final Bundle options )
    {
        Log.d( TAG, "getAuthToken" );

        final Intent intent = new Intent( mContext, AddConnectionActivity.class );
        //intent.putExtra( PARAM_AUTH_TOKEN_TYPE, authTokenType );
        intent.putExtra( KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response );
        if ( !TextUtils.isEmpty( account.name ) )
        {
            //intent.putExtra( PARAM_USERNAME, connection.name );
        }

        final Bundle bundle = new Bundle();
        bundle.putParcelable( KEY_INTENT, intent );
        return bundle;
    }

    private Intent createLoginIntent( final AccountAuthenticatorResponse response )
    {
        final Intent intent = new Intent( mContext, AddConnectionActivity.class );
        //intent.putExtra( PARAM_AUTH_TOKEN_TYPE, ACCOUNT_TYPE );
        intent.putExtra( KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response );
        return intent;
    }
}

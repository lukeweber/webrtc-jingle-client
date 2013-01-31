package com.tuenti.voice.example.ui.account;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import static android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE;
import static android.accounts.AccountManager.KEY_INTENT;

public class AccountAuthenticator
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

        final Intent intent = new Intent( mContext, AccountViewActivity.class );
        //intent.putExtra( AccountViewActivity.PARAM_AUTHTOKEN_TYPE, authTokenType );
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
        Log.d( TAG, "getAuthToken" );

        final Bundle bundle = new Bundle();

        if ( !AccountConstants.ACCOUNT_TYPE.equals( authTokenType ) )
        {
            return bundle;
        }

        AccountManager manager = AccountManager.get( mContext );
        String password = manager.getPassword( account );
        if ( TextUtils.isEmpty( password ) )
        {
            bundle.putParcelable( KEY_INTENT, createLoginIntent( response ) );
        }

        return bundle;
    }

    @Override
    public String getAuthTokenLabel( String s )
    {
        Log.d( TAG, "getAuthTokenLabel" );
        return null;
    }

    @Override
    public Bundle hasFeatures( AccountAuthenticatorResponse accountAuthenticatorResponse, Account account,
                               String[] strings )
        throws NetworkErrorException
    {
        return null;
    }

    @Override
    public Bundle updateCredentials( final AccountAuthenticatorResponse response, final Account account,
                                     final String authTokenType, final Bundle options )
    {
        Log.d( TAG, "getAuthToken" );

        final Intent intent = new Intent( mContext, AccountViewActivity.class );
        //intent.putExtra( AccountViewActivity.PARAM_AUTHTOKEN_TYPE, authTokenType );
        intent.putExtra( KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response );
        if ( !TextUtils.isEmpty( account.name ) )
        {
            //intent.putExtra( AccountViewActivity.PARAM_USERNAME, account.name );
        }

        final Bundle bundle = new Bundle();
        bundle.putParcelable( KEY_INTENT, intent );
        return bundle;
    }

    private Intent createLoginIntent( final AccountAuthenticatorResponse response )
    {
        final Intent intent = new Intent( mContext, AccountViewActivity.class );
        //intent.putExtra( AccountViewActivity.PARAM_AUTHTOKEN_TYPE, AccountConstants.ACCOUNT_TYPE );
        intent.putExtra( KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response );
        return intent;
    }
}

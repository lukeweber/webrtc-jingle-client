package com.tuenti.voice.example.ui.connection;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.github.kevinsawicki.wishlist.Toaster;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.XmppError;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.core.manager.ConnectionManager;
import com.tuenti.voice.example.R;

public abstract class AuthenticationTask
    extends AsyncTask<Connection, Void, String>
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "AuthenticationTask";

    protected Account mAccount;

    private Activity mActivity;

    private VoiceClient mClient;

// --------------------------- CONSTRUCTORS ---------------------------

    public AuthenticationTask( final Activity activity )
    {
        mActivity = activity;
    }

// -------------------------- OTHER METHODS --------------------------

    public abstract void onException( Exception e );

    public abstract void onSuccess( Account account );

    @Override
    protected String doInBackground( Connection... params )
    {
        final Connection connection = params[0];

        mClient = new VoiceClient();
        mClient.setConnectionManager( new ConnectionManager()
        {
            @Override
            public void handleXmppError( int error )
            {
                Log.d( TAG, "handleXmppError " + error );
                switch ( XmppError.fromInteger( error ) )
                {
                    case UNAUTHORIZED:
                        Toaster.showLong( mActivity, R.string.invalid_login_or_password );
                        break;
                }
            }

            @Override
            public void handleXmppSocketClose( int state )
            {
                Log.d( TAG, "handleXmppSocketClose " + state );
            }

            @Override
            public void handleXmppStateChanged( int state )
            {
                Log.d( TAG, "handleXmppStateChanged " + state );
                switch ( XmppState.fromInteger( state ) )
                {
                    case OPEN:
                        mAccount = new Account( connection.getUsername(), AccountConstants.ACCOUNT_TYPE );
                        AccountManager manager = AccountManager.get( mActivity );
                        manager.addAccountExplicitly( mAccount, connection.getPassword(), null );
                        manager.setUserData( mAccount, "stunHost", connection.getStunHost() );
                        manager.setUserData( mAccount, "stunPort", String.valueOf( connection.getStunPort() ) );
                        manager.setUserData( mAccount, "turnHost", connection.getTurnHost() );
                        manager.setUserData( mAccount, "turnUsername", connection.getTurnUsername() );
                        manager.setUserData( mAccount, "turnPassword", connection.getTurnPassword() );
                        manager.setUserData( mAccount, "xmppHost", connection.getXmppHost() );
                        manager.setUserData( mAccount, "xmppPort", String.valueOf( connection.getXmppPort() ) );
                        manager.setUserData( mAccount, "xmppUseSsl", String.valueOf( connection.getXmppUseSsl() ) );
                        onSuccess( mAccount );

                        mClient.logout();
                        break;
                    case CLOSED:
                        if ( mClient != null )
                        {
                            mClient.release();
                            mClient = null;
                        }
                        break;
                }
            }
        } );
        mClient.init( mActivity );
        mClient.login( connection.getUsername(),
                       connection.getPassword(),
                       connection.getStunHost() + ":" + connection.getStunPort(),
                       connection.getTurnHost(),
                       connection.getTurnUsername(),
                       connection.getTurnPassword(),
                       connection.getXmppHost(),
                       connection.getXmppPort(),
                       connection.getXmppUseSsl(),
                       0,
                       true /*isGtalk*/);
        return null;
    }
}

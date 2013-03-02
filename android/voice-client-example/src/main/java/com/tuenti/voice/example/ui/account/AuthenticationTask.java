package com.tuenti.voice.example.ui.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.tuenti.voice.core.VoiceClient;
import com.tuenti.voice.core.XmppState;
import com.tuenti.voice.core.data.Connection;
import com.tuenti.voice.core.manager.ConnectionManager;

public abstract class AuthenticationTask
    extends AsyncTask<Connection, Void, String>
{
// ------------------------------ FIELDS ------------------------------

    private static final String TAG = "AuthenticationTask";

    private boolean finishing;

    private VoiceClient mClient;

    private Context mContext;

// --------------------------- CONSTRUCTORS ---------------------------

    public AuthenticationTask( final Context context )
    {
        mContext = context;
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
                        Account account = new Account( connection.getUsername(), AccountConstants.ACCOUNT_TYPE );
                        AccountManager.get( mContext ).addAccountExplicitly( account, connection.getPassword(), null );
                        onSuccess( account );

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
        mClient.init( mContext );
        mClient.login( connection.getUsername(),
                       connection.getPassword(),
                       connection.getStunHost() + ":19302",
                       connection.getTurnHost(),
                       connection.getTurnUsername(),
                       connection.getTurnPassword(),
                       connection.getXmppHost(),
                       connection.getXmppPort(),
                       connection.getXmppUseSsl(),
                       0 );
        return null;
    }

    private void finishLogin()
    {
        if ( !finishing )
        {
            finishing = true;
            mClient.logout();
        }
    }
}

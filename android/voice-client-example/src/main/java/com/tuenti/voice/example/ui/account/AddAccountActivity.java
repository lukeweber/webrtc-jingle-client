package com.tuenti.voice.example.ui.account;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AddAccountActivity
    extends SherlockFragmentActivity
{
// ------------------------------ FIELDS ------------------------------

    /**
     * Auth token type parameter
     */
    public static final String PARAM_AUTH_TOKEN_TYPE = "authTokenType";

    /**
     * Initial user name
     */
    public static final String PARAM_USERNAME = "username";

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace( android.R.id.content, new CredentialsFragment() );
        transaction.addToBackStack( "create_account" );
        transaction.commit();
    }

    /**
     * Sync period in seconds, currently every 8 hours
     *
     private static final long SYNC_PERIOD = 8L * 60L * 60L;

     private AccountAuthenticatorResponse mAccountAuthenticatorResponse;

     private AccountManager mAccountManager;

     private String mPassword;

     private EditText mPasswordText;

     private Bundle mResultBundle;

     private String mUsername;

     private EditText mUsernameText;

     private static void configureSyncFor(Account account) {
     ContentResolver.setIsSyncable(account, PROVIDER_AUTHORITY, 1);
     ContentResolver.setSyncAutomatically(account, PROVIDER_AUTHORITY, true);
     ContentResolver.addPeriodicSync(account, PROVIDER_AUTHORITY, new Bundle(), SYNC_PERIOD);
     }
     @Override protected void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);

     mAccountAuthenticatorResponse =
     getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
     if (mAccountAuthenticatorResponse != null) {
     mAccountAuthenticatorResponse.onRequestContinued();
     }

     //setContentView(R.layout.add_account);

     mAccountManager = AccountManager.get(this);

     mUsernameText = (EditText) findViewById(R.id.username);
     mPasswordText = (EditText) findViewById(R.id.password);
     mPasswordText.setOnKeyListener(new View.OnKeyListener() {
     @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
     if (event != null && ACTION_DOWN == event.getAction() && keyCode == KEYCODE_ENTER) {
     handleLogin();
     return true;
     }
     return false;
     }
     });
     mPasswordText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
     @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
     if (actionId == IME_ACTION_DONE) {
     handleLogin();
     return true;
     }
     return false;
     }
     });
     }

     @Override public void finish() {
     if (mAccountAuthenticatorResponse != null) {
     if (mResultBundle != null) {
     mAccountAuthenticatorResponse.onResult(mResultBundle);
     } else {
     mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
     }
     mAccountAuthenticatorResponse = null;
     }
     super.finish();
     }

     private void handleLogin() {
     mUsername = mUsernameText.getText().toString();
     mPassword = mPasswordText.getText().toString();

     AsyncTask<Void, Void, Account> mAuthenticationTask = new AsyncTask<Void, Void, Account>() {
     @Override protected Account doInBackground(Void... params) {
     Account account = new Account(mUsername, ACCOUNT_TYPE);
     mAccountManager.addAccountExplicitly(account, mPassword, null);
     configureSyncFor(account);
     return account;
     }

     @Override protected void onPostExecute(Account account) {
     onAuthenticationResult(true);
     }
     };
     mAuthenticationTask.execute();
     }

     public final void setAccountAuthenticatorResult(Bundle result) {
     mResultBundle = result;
     }

     /**
      * Called when the authentication process completes (see attemptLogin()).
      *
      * @param result
     *
    public void onAuthenticationResult(boolean result) {
    if (result) {
    final Intent intent = new Intent();
    intent.putExtra(KEY_ACCOUNT_NAME, mUsername);
    intent.putExtra(KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
    intent.putExtra(KEY_AUTHTOKEN, mPassword);
    setAccountAuthenticatorResult(intent.getExtras());
    setResult(RESULT_OK, intent);
    finish();
    }
    }
     */
}

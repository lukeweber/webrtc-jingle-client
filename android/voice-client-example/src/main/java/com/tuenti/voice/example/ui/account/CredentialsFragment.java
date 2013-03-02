package com.tuenti.voice.example.ui.account;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import com.actionbarsherlock.app.SherlockFragment;
import com.github.kevinsawicki.wishlist.Toaster;
import com.tuenti.voice.example.R;

import static android.view.View.OnClickListener;

public class CredentialsFragment
    extends SherlockFragment
{
// ------------------------------ FIELDS ------------------------------

    private EditText mPasswordText;

    private EditText mUsernameText;

// -------------------------- OTHER METHODS --------------------------

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View rootView = inflater.inflate( R.layout.credentials_fragment, container, false );

        mUsernameText = (EditText) rootView.findViewById( R.id.username );
        mPasswordText = (EditText) rootView.findViewById( R.id.password );

        Button nextButton = (Button) rootView.findViewById( R.id.next );
        nextButton.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                handleNext();
            }
        } );

        return rootView;
    }

    private void handleNext()
    {
        if ( TextUtils.isEmpty( mUsernameText.getText() ) || TextUtils.isEmpty( mPasswordText.getText() ) )
        {
            Toaster.showLong( getActivity(), R.string.required_username_and_password );
            return;
        }

        Bundle arguments = new Bundle();
        arguments.putString( AddAccountActivity.PARAM_USERNAME, mUsernameText.getText().toString() );
        arguments.putString( AddAccountActivity.PARAM_PASSWORD, mPasswordText.getText().toString() );

        ServerSettingsFragment fragment = new ServerSettingsFragment();
        fragment.setArguments( arguments );

        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace( android.R.id.content, fragment );
        transaction.addToBackStack( "create_account" );
        transaction.commit();
    }
}

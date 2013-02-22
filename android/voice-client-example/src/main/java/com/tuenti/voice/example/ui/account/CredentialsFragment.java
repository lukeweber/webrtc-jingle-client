package com.tuenti.voice.example.ui.account;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.actionbarsherlock.app.SherlockFragment;
import com.tuenti.voice.example.R;

import static android.view.View.OnClickListener;

public class CredentialsFragment
    extends SherlockFragment
{
// -------------------------- OTHER METHODS --------------------------

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        View rootView = inflater.inflate( R.layout.credentials_fragment, container, false );

        Button nextButton = (Button) rootView.findViewById( R.id.next );
        nextButton.setOnClickListener( new OnClickListener()
        {
            @Override
            public void onClick( View view )
            {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace( android.R.id.content, new ServerSettingsFragment() );
                transaction.addToBackStack( "create_account" );
                transaction.commit();
            }
        } );

        return rootView;
    }
}

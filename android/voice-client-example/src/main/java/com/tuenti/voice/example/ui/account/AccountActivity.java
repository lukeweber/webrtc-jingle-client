package com.tuenti.voice.example.ui.account;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AccountActivity
    extends SherlockFragmentActivity
{
    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        if ( savedInstanceState == null )
        {
            ChooseAccountFragment fragment = new ChooseAccountFragment();
            getSupportFragmentManager().beginTransaction().add( android.R.id.content, fragment ).commit();
        }
    }
}

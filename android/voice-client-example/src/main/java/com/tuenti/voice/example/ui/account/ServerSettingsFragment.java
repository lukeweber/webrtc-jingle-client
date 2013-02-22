package com.tuenti.voice.example.ui.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockFragment;
import com.tuenti.voice.example.R;

public class ServerSettingsFragment
    extends SherlockFragment
{
    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState )
    {
        return inflater.inflate( R.layout.server_settings_fragment, container, false );
    }
}

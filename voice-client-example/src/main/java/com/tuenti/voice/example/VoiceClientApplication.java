package com.tuenti.voice.example;

import android.app.Application;
import android.content.Intent;
import com.tuenti.voice.example.service.VoiceClientService;

public class VoiceClientApplication
    extends Application
{
// -------------------------- OTHER METHODS --------------------------

    @Override
    public void onCreate()
    {
        super.onCreate();

        Intent intent = new Intent( this, VoiceClientService.class );
        startService( intent );
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();

        Intent intent = new Intent( this, VoiceClientService.class );
        stopService( intent );
    }
}

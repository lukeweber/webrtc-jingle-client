package com.tuenti.voice.example;

import android.app.Application;
import android.content.Intent;

import com.tuenti.voice.example.service.VoiceClientService;
import com.tuenti.voice.example.service.VoiceClientControllerService;

public class VoiceClientApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(getApplicationContext(),
                VoiceClientService.class));
        startService(new Intent(getApplicationContext(),
                VoiceClientControllerService.class));
    }

    // TODO(LUKE): Figure out how we should clean up this binding
    // @Override
    // public void onDestroy() {

    // mVoiceClientController.onDestroy();
    // }
}

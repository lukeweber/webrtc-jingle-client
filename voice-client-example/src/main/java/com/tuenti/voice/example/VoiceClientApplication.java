package com.tuenti.voice.example;

import android.app.Application;
import android.content.Intent;

import com.tuenti.voice.example.service.VoiceClientService;

public class VoiceClientApplication extends Application {

    public static VoiceClientController mVoiceClientController;

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(getApplicationContext(),
                VoiceClientService.class));
        mVoiceClientController = new VoiceClientController(this);
        mVoiceClientController.bind();
    }

    // TODO(LUKE): Figure out how we should clean up this binding
    // @Override
    // public void onDestroy() {

    // mVoiceClientController.onDestroy();
    // }
}

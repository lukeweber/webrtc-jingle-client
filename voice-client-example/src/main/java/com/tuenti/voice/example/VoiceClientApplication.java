package com.tuenti.voice.example;

import android.app.Application;
import android.content.Intent;
import com.tuenti.voice.example.service.IVoiceClientService;
import com.tuenti.voice.example.service.VoiceClientService;
import com.tuenti.voice.example.VoiceClientController;

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

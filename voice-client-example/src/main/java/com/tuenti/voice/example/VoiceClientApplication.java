package com.tuenti.voice.example;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.tuenti.voice.example.service.VoiceClientService;
import com.tuenti.voice.example.util.ProcessUtil;

public class VoiceClientApplication extends Application {

    public static VoiceClientController mVoiceClientController;

    @Override
    public void onCreate() {
        super.onCreate();
        if( !ProcessUtil.isRemoteService(this) ){
            startService(new Intent(getApplicationContext(), VoiceClientService.class));
            mVoiceClientController = new VoiceClientController(this);
            mVoiceClientController.bind();
        }
    }

    // TODO(LUKE): Figure out how we should clean up this binding
    // @Override
    // public void onDestroy() {

    // mVoiceClientController.onDestroy();
    // }
}

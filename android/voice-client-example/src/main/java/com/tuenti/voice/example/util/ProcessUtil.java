package com.tuenti.voice.example.util;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.util.Log;
import android.os.Process;

public class ProcessUtil {

    private static final String VoiceClientSerivceProcessName = "com.tuenti.voice.example:VoiceClientService";

    public static boolean isRemoteService(Context context){
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
            if (VoiceClientSerivceProcessName.equals(process.processName)) {
                return Process.myPid() == process.pid;
            }
        }
        return false;
    }
}

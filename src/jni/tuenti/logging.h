#ifndef _TUENTI_LOGGING_H_
#define _TUENTI_LOGGING_H_

#include <android/log.h>

#if LOGGING
#define LOG_TAG "c-libjingle-webrtc" // As in WEBRTC Native...
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#define LOGI(...) (void)0
#define LOGE(...) (void)0
#endif

#endif //_TUENTI_LOGGING_H_

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_tuenti_voice_VoiceClient */

#ifndef _Included_com_tuenti_voice_VoiceClient
#define _Included_com_tuenti_voice_VoiceClient
#ifdef __cplusplus
extern "C" {
#endif
#undef com_tuenti_voice_VoiceClient_CALL_STATE_EVENT
#define com_tuenti_voice_VoiceClient_CALL_STATE_EVENT 0L
#undef com_tuenti_voice_VoiceClient_XMPP_ERROR_EVENT
#define com_tuenti_voice_VoiceClient_XMPP_ERROR_EVENT 2L
#undef com_tuenti_voice_VoiceClient_XMPP_STATE_EVENT
#define com_tuenti_voice_VoiceClient_XMPP_STATE_EVENT 1L
/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeAcceptCall
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeAcceptCall
  (JNIEnv *, jobject);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeCall
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeCall
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeDeclineCall
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeDeclineCall
  (JNIEnv *, jobject);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeDestroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeDestroy
  (JNIEnv *, jobject);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeEndCall
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeEndCall
  (JNIEnv *, jobject);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeInit
  (JNIEnv *, jobject);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeLogin
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;IZ)V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeLogin
  (JNIEnv *, jobject, jstring, jstring, jstring, jint, jboolean, jstring, jint);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeLogout
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeLogout
  (JNIEnv *, jobject);

/*
 * Class:     com_tuenti_voice_VoiceClient
 * Method:    nativeRelease
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeRelease
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif

/*
 * webrtc-jingle
 * Copyright 2012 Tuenti Technologies
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
#include <jni.h>
#include <assert.h>
#include <string>
#include <cstring>

#include "com_tuenti_voice_core_VoiceClient.h"
#include "tuenti/logging.h"
#include "tuenti/voiceclient.h"
#include "tuenti/threadpriorityhandler.h"
#include "tuenti/helpers.h"
#include "talk/base/criticalsection.h"

tuenti::VoiceClient *client_;
tuenti::StunConfig *stun_config_;
talk_base::CriticalSection native_release_cs_;

jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
  if (!vm) {
    LOGE("JNI_OnLoad did not receive a valid VM pointer");
    return JNI_ERR;
  }
  JNIEnv* env;
  if (JNI_OK != vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6)) {
    LOGE("JNI_OnLoad could not get JNI env");
    return JNI_ERR;
  }

  tuenti::ThreadPriorityHandler::Init(vm);

  return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeAcceptCall(
    JNIEnv *env, jobject object, jlong call_id) {
  if (client_) {
    LOGI("native accept call %d", call_id);
    client_->AcceptCall(call_id);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeCall(
    JNIEnv *env, jobject object, jstring remoteJid) {
  if (client_) {
    std::string nativeRemoteJid = env->GetStringUTFChars(remoteJid, NULL);
    client_->Call(nativeRemoteJid);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeMuteCall(
    JNIEnv *env, jobject object, jlong call_id, jboolean mute) {
  if (client_) {
    client_->MuteCall(call_id, mute);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeHoldCall(
    JNIEnv *env, jobject object, jlong call_id, jboolean hold) {
  if (client_) {
    client_->HoldCall(call_id, hold);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeDeclineCall(
    JNIEnv *env, jobject object, jlong call_id, jboolean busy) {
  if (client_) {
    client_->DeclineCall(call_id, busy);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeEndCall(
    JNIEnv *env, jobject object, jlong call_id) {
  if (client_) {
    client_->EndCall(call_id);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeInit(
    JNIEnv *env, jobject object){
  if (!client_) {
    LOGI("Java_com_tuenti_voice_VoiceClient_nativeInit - initializing "
      "client");

    JavaObjectReference *instance = NEW_OBJECT(JavaObjectReference, 1);
    RETURN_IF_FAIL(instance != NULL);
    SetJavaObject(instance, env, object);

    client_ = new tuenti::VoiceClient(instance);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeLogin(
    JNIEnv *env, jobject object, jstring username, jstring password,
    jstring stun, jstring turn, jstring turnPassword, jstring xmppHost,
    jint xmppPort, jboolean useSSL) {
  if (!client_) {
    LOGE("client not initialized");
    return;
  }
  const char* nativeUsername = env->GetStringUTFChars(username, NULL);
  const char* nativePassword = env->GetStringUTFChars(password, NULL);
  const char* nativeTurnPassword = env->GetStringUTFChars(turnPassword, NULL);
  const char* nativeXmppHost = env->GetStringUTFChars(xmppHost, NULL);
  const char* nativeStun = env->GetStringUTFChars(stun, NULL);
  const char* nativeTurn = env->GetStringUTFChars(turn, NULL);

  std::string nativeUsernameS(nativeUsername);
  std::string nativePasswordS(nativePassword);
  std::string nativeTurnPasswordS(nativeTurnPassword);
  std::string nativeXmppHostS(nativeXmppHost);

  size_t pos = nativeUsernameS.find("@");
  std::string turn_username;
  if (pos != std::string::npos) {
    turn_username = std::string(nativeUsernameS.substr(0, pos));
  } else {
    turn_username = std::string(nativeUsernameS);
  }

  stun_config_ = new tuenti::StunConfig;
  stun_config_->stun = std::string(nativeStun);
  stun_config_->turn = std::string(nativeTurn);
  stun_config_->turn_username = turn_username;
  stun_config_->turn_password = nativeTurnPasswordS;

  // login
  client_->Login(nativeUsernameS, nativePasswordS, stun_config_,
    nativeXmppHostS, xmppPort, useSSL);

  // release
  env->ReleaseStringUTFChars(username, nativeUsername);
  env->ReleaseStringUTFChars(password, nativePassword);
  env->ReleaseStringUTFChars(xmppHost, nativeXmppHost);
  env->ReleaseStringUTFChars(stun, nativeStun);
  env->ReleaseStringUTFChars(turn, nativeTurn);
  env->ReleaseStringUTFChars(turnPassword, nativeTurnPassword);
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeLogout(
        JNIEnv *env, jobject object) {
  if (client_) {
    client_->Disconnect();
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeRelease(
  JNIEnv *env, jobject object) {
  talk_base::CritScope lock(&native_release_cs_);
  if (client_) {
    client_->Destroy();
    delete client_;
    client_ = NULL;
    delete stun_config_;
    stun_config_ = NULL;
  }
}

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
#include "tuenti/voiceclientnotify.h"
#include "tuenti/callbackhelper.h"

tuenti::VoiceClient *client_;
tuenti::CallbackHelper *callback_helper_;
tuenti::StunConfig *stun_config_;

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
  callback_helper_ = new tuenti::CallbackHelper();
  callback_helper_->setJvm(vm);

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

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeDestroy(
    JNIEnv *env, jobject object) {
    Java_com_tuenti_voice_core_VoiceClient_nativeRelease(env, object);
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeEndCall(
    JNIEnv *env, jobject object, jlong call_id) {
  if (client_) {
    client_->EndCall(call_id);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeInit(
    JNIEnv *env, jobject object, jstring stun, jstring relay_udp,
    jstring relay_tcp, jstring relay_ssl, jstring turn) {
  const char* native_stun = env->GetStringUTFChars(stun, NULL);
  const char* native_relay_udp = env->GetStringUTFChars(relay_udp, NULL);
  const char* native_relay_tcp = env->GetStringUTFChars(relay_tcp, NULL);
  const char* native_relay_ssl = env->GetStringUTFChars(relay_ssl, NULL);
  const char* native_turn = env->GetStringUTFChars(turn, NULL);

  stun_config_ = new tuenti::StunConfig;
  stun_config_->stun = std::string(native_stun);
  stun_config_->relay_udp = std::string(native_relay_udp);
  stun_config_->relay_tcp = std::string(native_relay_tcp);
  stun_config_->relay_ssl = std::string(native_relay_ssl);
  stun_config_->turn = std::string(native_turn);

  env->ReleaseStringUTFChars(stun, native_stun);
  env->ReleaseStringUTFChars(relay_udp, native_relay_udp);
  env->ReleaseStringUTFChars(relay_tcp, native_relay_tcp);
  env->ReleaseStringUTFChars(relay_ssl, native_relay_ssl);
  env->ReleaseStringUTFChars(turn, native_turn);

  if (!client_) {
    LOGI("Java_com_tuenti_voice_VoiceClient_nativeInit - initializing "
      "client");
    callback_helper_->setReferenceObject(env->NewGlobalRef(object));
    client_ =
        new tuenti::VoiceClient(static_cast<tuenti::VoiceClientNotify*>
        (callback_helper_), stun_config_);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeLogin(
    JNIEnv *env, jobject object, jstring username, jstring password,
    jstring xmppHost, jint xmppPort, jboolean useSSL) {
  if (!client_) {
    LOGE("client not initialized");
    return;
  }

  const char* nativeUsername = env->GetStringUTFChars(username, NULL);
  const char* nativePassword = env->GetStringUTFChars(password, NULL);
  const char* nativeXmppHost = env->GetStringUTFChars(xmppHost, NULL);

  std::string nativeUsernameS(nativeUsername);
  std::string nativePasswordS(nativePassword);
  std::string nativeXmppHostS(nativeXmppHost);

  // login
  client_->Login(nativeUsernameS, nativePasswordS, nativeXmppHostS, xmppPort,
    useSSL);

  // release
  env->ReleaseStringUTFChars(username, nativeUsername);
  env->ReleaseStringUTFChars(password, nativePassword);
  env->ReleaseStringUTFChars(xmppHost, nativeXmppHost);
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeLogout(
        JNIEnv *env, jobject object) {
  if (client_) {
    client_->Disconnect();
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeRelease(
  JNIEnv *env, jobject object) {
  if (client_) {
    // Does an internal delete when all threads have stopped
    // but a callback to do the delete here would be better
    client_->Destroy(0);
    delete stun_config_;
    client_ = NULL;
  }
}

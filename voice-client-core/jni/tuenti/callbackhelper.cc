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
#include <string>

#include "com_tuenti_voice_core_VoiceClient.h"
#include "tuenti/logging.h"
#include "tuenti/callbackhelper.h"
#include "tuenti/voiceclientnotify.h"
#include "talk/p2p/base/session.h"
#include "talk/xmpp/xmppclient.h"

namespace tuenti {

void CallbackHelper::setJvm(JavaVM* jvm) {
  jvm_ = jvm;
}

void CallbackHelper::setReferenceObject(jobject reference_obj) {
  reference_object_ = reference_obj;
}

void CallbackHelper::CallNativeDispatchEvent(jint type, jint code,
    const std::string &msg, jlong call_id) {
  JNIEnv *env;
  int status = jvm_->AttachCurrentThread(&env, NULL);
  if (status < 0) {
    LOGE("failed to attach native thread");
    return;
  }
  jclass cls = env->GetObjectClass(reference_object_);
  if (!cls) {
    LOGE("Failed to get class reference");
    jvm_->DetachCurrentThread();
    return;
  }
  jmethodID method = env->GetStaticMethodID(cls, "dispatchNativeEvent",
    "(IILjava/lang/String;J)V");
  if (!method) {
    LOGE("Failed to get method ID");
    jvm_->DetachCurrentThread();
    return;
  }
  jstring message = env->NewStringUTF(msg.c_str());
  env->CallStaticVoidMethod(cls, method, type, code, message, call_id);
  jvm_->DetachCurrentThread();
  return;
}

void CallbackHelper::OnXmppStateChange(buzz::XmppEngine::State state) {
  CallNativeDispatchEvent(com_tuenti_voice_core_VoiceClient_XMPP_STATE_EVENT,
    state, "", 0);
  if (state == buzz::XmppEngine::STATE_CLOSED && !reference_object_) {
    JNIEnv *env;
    jvm_->AttachCurrentThread(&env, NULL);
    env->DeleteGlobalRef(reference_object_);
    reference_object_ = NULL;
    jvm_->DetachCurrentThread();
  }
}

void CallbackHelper::OnCallStateChange(cricket::Session* session,
      cricket::Session::State state, uint32 call_id) {
    buzz::Jid jid(session->remote_name());
    std::string remoteJid = jid.Str();
    CallNativeDispatchEvent(com_tuenti_voice_core_VoiceClient_CALL_STATE_EVENT,
        state, remoteJid, call_id);
}

void CallbackHelper::OnXmppError(buzz::XmppEngine::Error error) {
  CallNativeDispatchEvent(com_tuenti_voice_core_VoiceClient_XMPP_ERROR_EVENT,
    error, "", 0);
}

void CallbackHelper::OnBuddyListAdd(const std::string user_key,
    const std::string nick) {
  LOGI("Adding to buddy list: %s, %s", user_key.c_str(), nick.c_str());
  // TODO(Luke) We might want to pass nick back, but NativeDispatch only
  // supports one param.
  CallNativeDispatchEvent(com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT,
      ADD, user_key, 0);
}

void CallbackHelper::OnBuddyListRemove(const std::string user_key) {
  LOGI("Removing from buddy list: %s", user_key.c_str());
  CallNativeDispatchEvent(com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT,
      REMOVE, user_key, 0);
}

void CallbackHelper::OnBuddyListReset() {
  LOGI("Resetting buddy list");
  CallNativeDispatchEvent(com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT,
      RESET, "", 0);
}
}  // Namespace tuenti

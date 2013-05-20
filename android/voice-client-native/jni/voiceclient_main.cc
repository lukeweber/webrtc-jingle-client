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
#include "client/logging.h"
#include "client/voiceclient.h"
#include "client/threadpriorityhandler.h"
#include "client/helpers.h"
#include "talk/base/criticalsection.h"

class VoiceClientCallback : public sigslot::has_slots<> {

public:

	VoiceClientCallback(JavaObjectReference *reference) : reference_(reference) {}

	void OnSignalCallStateChange(int state, const char *remote_jid, int call_id) {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_CALL_STATE_EVENT, state, remote_jid, call_id);
	}

	void OnSignalAudioPlayout() {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_AUDIO_PLAYOUT_EVENT, 0, "", 0);
	}

	void OnSignalCallError(int error, int call_id) {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_CALL_ERROR_EVENT, error, "", call_id);
	}

	void OnSignalXmppError(int error) {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_XMPP_ERROR_EVENT, error, "", 0);
	}

	void OnSignalXmppSocketClose(int state) {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_XMPP_SOCKET_CLOSE_EVENT, state, "", 0);
	}

	void OnSignalXmppStateChange(int state) {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_XMPP_STATE_EVENT, state, "", 0);
	}

	void OnSignalBuddyListReset() {
	  LOGI("Resetting buddy list");
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT, 2, "", 0);
	}

	void OnSignalBuddyListRemove(const std::string& jid) {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_BUDDY_LIST_EVENT, 1, jid.c_str(), 0);
	}

	void OnSignalBuddyListAdd(const std::string& jid, const std::string& nick, int available, int show) {
	  CALLBACK_START("handleBuddyAdded", "(Ljava/lang/String;Ljava/lang/String;II)V", reference_);
	  if (mid != NULL) {
		jstring jid_jni = env->NewStringUTF(jid.c_str());
		jstring nick_jni = env->NewStringUTF(nick.c_str());
		jint available_jni = available;
		jint show_jni = show;
		env->CallVoidMethod(reference_->handler_object, mid, jid_jni, nick_jni, available_jni, show_jni);
      }
	  DETACH_FROM_VM(reference_);
	}

	void OnSignalPresenceChanged(const std::string& jid, int available, int show) {
	  CALLBACK_START("handlePresenceChanged", "(Ljava/lang/String;II)V", reference_);
	  if (mid != NULL) {
	    jstring jid_jni = env->NewStringUTF(jid.c_str());
		jint available_jni = available;
		jint show_jni = show;
		env->CallVoidMethod(reference_->handler_object, mid, jid_jni, available_jni, show_jni);
	  }
	  DETACH_FROM_VM(reference_);
	}

	void OnSignalStatsUpdate(const char *stats) {
	  LOGI("Updating stats=%s", stats);
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_STATS_UPDATE_EVENT, 0, stats, 0);
	}

	void OnSignalCallTrackerId(int call_id, const char* call_tracker_id) {
	  CALLBACK_DISPATCH(reference_, com_tuenti_voice_core_VoiceClient_CALL_TRACKER_ID_EVENT, 0, call_tracker_id, call_id);
	}

	void OnSignalXmppMessage(const tuenti::XmppMessage m){
	  //Implement me.
	}

private:

	JavaObjectReference *reference_;
};

tuenti::VoiceClient *client_;
VoiceClientCallback *callback_;
tuenti::StunConfig *stun_config_;
talk_base::CriticalSection native_release_cs_;
talk_base::CriticalSection init_cs_;

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

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeCallWithTrackerId(
    JNIEnv *env, jobject object, jstring remoteJid, jstring callTrackerId) {
  if (client_) {
    std::string nativeRemoteJid = env->GetStringUTFChars(remoteJid, NULL);
    std::string nativeCallTrackerId = env->GetStringUTFChars(callTrackerId, NULL);
    client_->CallWithTracker(nativeRemoteJid, nativeCallTrackerId);
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

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativePing(
    JNIEnv *env, jobject object) {
  if (client_) {
    client_->Ping();
  }
}


JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeInit(
    JNIEnv *env, jobject object, jobject context){
  if (!client_) {
    LOGI("Java_com_tuenti_voice_VoiceClient_nativeInit - initializing "
      "client");

    JavaObjectReference *instance = NEW_OBJECT(JavaObjectReference, 1);
    RETURN_IF_FAIL(instance != NULL);
    SetJavaObject(instance, env, object, context);
    callback_ = new VoiceClientCallback(instance);

    //We enforce that we have a client_ and VoiceClient returns before we allow events.
    talk_base::CritScope lock(&init_cs_);
    client_ = new tuenti::VoiceClient();
    client_->SignalingThread()->SignalCallStateChange.connect(callback_, &VoiceClientCallback::OnSignalCallStateChange);
    client_->SignalingThread()->SignalCallError.connect(callback_, &VoiceClientCallback::OnSignalCallError);
    client_->SignalingThread()->SignalAudioPlayout.connect(callback_, &VoiceClientCallback::OnSignalAudioPlayout);
    client_->SignalingThread()->SignalCallTrackerId.connect(callback_, &VoiceClientCallback::OnSignalCallTrackerId);
    client_->SignalingThread()->SignalXmppError.connect(callback_, &VoiceClientCallback::OnSignalXmppError);
    client_->SignalingThread()->SignalXmppSocketClose.connect(callback_, &VoiceClientCallback::OnSignalXmppSocketClose);;
    client_->SignalingThread()->SignalXmppStateChange.connect(callback_, &VoiceClientCallback::OnSignalXmppStateChange);
    client_->SignalingThread()->SignalBuddyListAdd.connect(callback_, &VoiceClientCallback::OnSignalBuddyListAdd);
    client_->SignalingThread()->SignalBuddyListRemove.connect(callback_, &VoiceClientCallback::OnSignalBuddyListRemove);
    client_->SignalingThread()->SignalPresenceChanged.connect(callback_, &VoiceClientCallback::OnSignalPresenceChanged);
    client_->SignalingThread()->SignalXmppMessage.connect(callback_, &VoiceClientCallback::OnSignalXmppMessage);
	#ifdef LOGGING
    client_->SignalingThread()->SignalStatsUpdate.connect(callback_, &VoiceClientCallback::OnSignalStatsUpdate);
	#endif
  }
}
JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeReplaceTurn(
    JNIEnv *env, jobject object, jstring turn) {

  //We enforce that we have a client_ and VoiceClient returns before we allow a login.
  talk_base::CritScope lock(&init_cs_);
  if (!client_) {
    LOGE("client not initialized");
    return;
  }
  //Only use turn if we have a TurnServer, User and Pass
  if (turn != NULL){
    const char* nativeTurn = env->GetStringUTFChars(turn, NULL);
    // replace
    client_->ReplaceTurn(nativeTurn);
    env->ReleaseStringUTFChars(turn, nativeTurn);
  }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_core_VoiceClient_nativeLogin(
    JNIEnv *env, jobject object, jstring username, jstring password,
    jstring stun, jstring turn, jstring turnUsername, jstring turnPassword,
    jstring xmppHost, jint xmppPort, jboolean useSSL, jint portAllocatorFilter, jboolean isGtalk) {

  //We enforce that we have a client_ and VoiceClient returns before we allow a login.
  talk_base::CritScope lock(&init_cs_);
  if (!client_) {
    LOGE("client not initialized");
    return;
  }
  if (username == NULL || password == NULL
      || xmppHost == NULL) {
    LOGE("Login failure: Username, password, and xmpphost are required.");
  } else {
    const char* nativeUsername = env->GetStringUTFChars(username, NULL);
    const char* nativePassword = env->GetStringUTFChars(password, NULL);
    const char* nativeXmppHost = env->GetStringUTFChars(xmppHost, NULL);

    //Copy and release.
    std::string nativeUsernameS(nativeUsername);
    std::string nativePasswordS(nativePassword);
    std::string nativeXmppHostS(nativeXmppHost);
    env->ReleaseStringUTFChars(username, nativeUsername);
    env->ReleaseStringUTFChars(password, nativePassword);
    env->ReleaseStringUTFChars(xmppHost, nativeXmppHost);

    stun_config_ = new tuenti::StunConfig;

    if (stun != NULL) {
      const char* nativeStun = env->GetStringUTFChars(stun, NULL);
      stun_config_->stun = std::string(nativeStun);
      env->ReleaseStringUTFChars(stun, nativeStun);
    }

    //Only use turn if we have a TurnServer, User and Pass
    if (turn != NULL && turnUsername != NULL
        && turnPassword != NULL){
      const char* nativeTurnUsername = env->GetStringUTFChars(turnUsername, NULL);
      const char* nativeTurnPassword = env->GetStringUTFChars(turnPassword, NULL);
      const char* nativeTurn = env->GetStringUTFChars(turn, NULL);
      stun_config_->turn = std::string(nativeTurn);
      stun_config_->turn_username = std::string(nativeTurnUsername);
      stun_config_->turn_password = std::string(nativeTurnPassword);
      env->ReleaseStringUTFChars(turnPassword, nativeTurnUsername);
      env->ReleaseStringUTFChars(turnPassword, nativeTurnPassword);
      env->ReleaseStringUTFChars(turn, nativeTurn);
    }

    // login
    client_->Login(nativeUsernameS, nativePasswordS, stun_config_,
        nativeXmppHostS, xmppPort, useSSL, portAllocatorFilter, isGtalk);
  }
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
  LOGI("Java_com_tuenti_voice_core_VoiceClient_nativeRelease");
  if (client_) {
    delete client_;
    client_ = NULL;
    delete stun_config_;
    stun_config_ = NULL;
  }
}

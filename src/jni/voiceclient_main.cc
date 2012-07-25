#include <jni.h>
#include <string.h>
#include <assert.h>

#include "com_tuenti_voice_VoiceClient.h"
#include "tuenti/logging.h"
#include "tuenti/voiceclient.h"
#include "tuenti/threadpriorityhandler.h"

JavaVM* jvm_;
jobject reference_object_;
tuenti::VoiceClient *client_;

class CallbackHelper: public tuenti::VoiceClientNotify {

public:

    CallbackHelper() {
    }

    void CallNativeDispatchEvent(jint type, jint code, const std::string &msg) {
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
        jmethodID method = env->GetStaticMethodID(cls, "dispatchNativeEvent", "(IILjava/lang/String;)V");
        if (!method) {
            LOGE("Failed to get method ID");
            jvm_->DetachCurrentThread();
            return;
        }
        jstring message = env->NewStringUTF(msg.c_str());
        env->CallStaticVoidMethod(cls, method, type, code, message);
        jvm_->DetachCurrentThread();
        return;
    }

    void OnXmppStateChange(buzz::XmppEngine::State state) {
        CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_XMPP_STATE_EVENT, state, "");
        switch (state) {
        case buzz::XmppEngine::STATE_CLOSED:
            if (!client_ && !reference_object_) {
                JNIEnv *env;
                jvm_->AttachCurrentThread(&env, NULL);
                env->DeleteGlobalRef(reference_object_);
                reference_object_ = NULL;
                jvm_->DetachCurrentThread();
            }
            break;
        }
    }

    void OnCallStateChange(cricket::Session* session, cricket::Session::State state) {
        buzz::Jid jid(session->remote_name());
        std::string remoteJid = jid.Str();
        CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT, state, remoteJid);
    }

    void OnXmppError(buzz::XmppEngine::Error error) {
        CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_XMPP_ERROR_EVENT, error, "");
    }
};
static CallbackHelper callback_;

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

    jvm_ = vm;

    tuenti::ThreadPriorityHandler::Init(vm);

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeAcceptCall(JNIEnv *env, jobject object) {
    if (client_) {
        client_->AcceptCall();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeCall(JNIEnv *env, jobject object, jstring remoteJid) {
    if (client_) {
        std::string nativeRemoteJid = env->GetStringUTFChars(remoteJid, NULL);
        client_->Call(nativeRemoteJid);
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeDeclineCall(JNIEnv *env, jobject object) {
    if (client_) {
        client_->DeclineCall();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeDestroy(JNIEnv *env, jobject object) {
    Java_com_tuenti_voice_VoiceClient_nativeRelease(env, object);
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeEndCall(JNIEnv *env, jobject object) {
    if (client_) {
        client_->EndCall();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeInit(JNIEnv *env, jobject object) {
    if (!client_) {
        LOGI("creating the client");
        reference_object_ = env->NewGlobalRef(object);
        client_ = new tuenti::VoiceClient(&callback_);
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeLogin(JNIEnv *env, jobject object, jstring username,
        jstring password, jstring xmppHost, jint xmppPort, jboolean useSSL, jstring stunHost, jint stunPort) {
    if (!client_) {
        LOGE("client not initialized");
        return;
    }

    //Although we're assigning this to std::string,
    //env->GetStringUTFChars is a malloc type operation.
    //I think this is a memory leak.
    const char* nativeUsername = env->GetStringUTFChars(username, NULL);
    const char* nativePassword = env->GetStringUTFChars(password, NULL);
    const char* nativeXmppHost = env->GetStringUTFChars(xmppHost, NULL);
    const char* nativeStunHost = env->GetStringUTFChars(stunHost, NULL);

    // login
    client_->Login(nativeUsername, nativePassword, nativeXmppHost, xmppPort, useSSL, nativeStunHost, stunPort);

    // release
    env->ReleaseStringUTFChars(username, nativeUsername);
    env->ReleaseStringUTFChars(password, nativePassword);
    env->ReleaseStringUTFChars(xmppHost, nativeXmppHost);
    env->ReleaseStringUTFChars(stunHost, nativeStunHost);
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeLogout(JNIEnv *env, jobject object) {
    if (client_) {
        client_->Disconnect();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeRelease(JNIEnv *env, jobject object) {
    if (client_) {
        client_->Destroy(); //Does an internal delete when all threads have stopped but a callback to do the delete here would be better
        client_ = NULL;
    }
}

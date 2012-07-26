#include <jni.h>
#include <string.h>
#include <assert.h>

#include "com_tuenti_voice_VoiceClient.h"
#include "tuenti/logging.h"
#include "tuenti/voiceclient.h"
#include "tuenti/threadpriorityhandler.h"


JavaVM* jvm_;
jobject reference_object_;
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
        jmethodID method = env->GetStaticMethodID(cls, "dispatchNativeEvent",
                "(IILjava/lang/String;)V");
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

    void OnStateChange(buzz::XmppEngine::State state) {
        switch (state) {
        case buzz::XmppEngine::STATE_START:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_XMPP_ENGINE_EVENT,
                    com_tuenti_voice_VoiceClient_XMPP_ENGINE_START, "connecting...");
            break;
        case buzz::XmppEngine::STATE_OPENING:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_XMPP_ENGINE_EVENT,
                    com_tuenti_voice_VoiceClient_XMPP_ENGINE_OPENING, "logging in...");
            break;
        case buzz::XmppEngine::STATE_OPEN:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_XMPP_ENGINE_EVENT,
                    com_tuenti_voice_VoiceClient_XMPP_ENGINE_OPEN, "logged in...");
            break;
        case buzz::XmppEngine::STATE_CLOSED:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_XMPP_ENGINE_EVENT,
                    com_tuenti_voice_VoiceClient_XMPP_ENGINE_CLOSED, "logged out...");
            break;
        default:
            LOGE("voiceclient_main::OnStateChange unknown state");
        }
    }

    void OnCallStateChange(cricket::Session* session, cricket::Session::State state) {
        buzz::Jid jid(session->remote_name());
        std::string remoteJid = jid.Str();

        switch (state) {
        case cricket::Session::STATE_SENTINITIATE:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT,
                    com_tuenti_voice_VoiceClient_CALL_CALLING, remoteJid);
            break;
        case cricket::Session::STATE_RECEIVEDINITIATE:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT,
                    com_tuenti_voice_VoiceClient_CALL_INCOMING, remoteJid);
            break;
        case cricket::Session::STATE_SENTACCEPT:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT,
                    com_tuenti_voice_VoiceClient_CALL_ANSWERED, remoteJid);
            break;
        case cricket::Session::STATE_RECEIVEDACCEPT:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT,
                    com_tuenti_voice_VoiceClient_CALL_ANSWERED, remoteJid);
            break;
        case cricket::Session::STATE_RECEIVEDREJECT:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT,
                    com_tuenti_voice_VoiceClient_CALL_REJECTED, remoteJid);
            break;
        case cricket::Session::STATE_RECEIVEDTERMINATE:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT,
                    com_tuenti_voice_VoiceClient_CALL_RECIVEDTERMINATE, remoteJid);
            break;
        case cricket::Session::STATE_INPROGRESS:
            CallNativeDispatchEvent(com_tuenti_voice_VoiceClient_CALL_STATE_EVENT,
                    com_tuenti_voice_VoiceClient_CALL_INPROGRESS, remoteJid);
            break;
        default:
            LOGE("voiceclient_main::OnCallStateChange unknown state");
        }
    }
};

static CallbackHelper callback_;
tuenti::VoiceClient *client_;

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

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeAcceptCall(JNIEnv *env,
        jobject object) {
    if (client_) {
        client_->AcceptCall();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeCall(JNIEnv *env, jobject object,
        jstring remoteJid) {
    if (client_) {
        std::string nativeRemoteJid = env->GetStringUTFChars(remoteJid, NULL);
        client_->Call(nativeRemoteJid);
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeDeclineCall(JNIEnv *env,
        jobject object) {
    if (client_) {
        client_->DeclineCall();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeDestroy(JNIEnv *env,
        jobject object) {
    Java_com_tuenti_voice_VoiceClient_nativeRelease(env, object);
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeEndCall(JNIEnv *env,
        jobject object) {
    if (client_) {
        client_->EndCall();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeInit(JNIEnv *env, jobject object) {
    if (!client_) {
        LOGI("Java_com_tuenti_voice_VoiceClient_nativeInit - initializing client");
        reference_object_ = env->NewGlobalRef(object);
        client_ = new tuenti::VoiceClient(&callback_);
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeLogin(JNIEnv *env, jobject object,
        jstring username, jstring password, jstring server, jboolean useSSL) {
    if (!client_) {
        LOGE("Java_com_tuenti_voice_VoiceClient_nativeLogin - client not initialized");
        return;
    }

    //Although we're assigning this to std::string,
    //env->GetStringUTFChars is a malloc type operation.
    //I think this is a memory leak.
    std::string nativeUsername = env->GetStringUTFChars(username, NULL);
    std::string nativePassword = env->GetStringUTFChars(password, NULL);
    std::string nativeServer = env->GetStringUTFChars(server, NULL);

    if (nativeUsername.empty() || nativePassword.empty()) {
        LOGE("Username/Password or Domain not set");
        return;
    }
    if (nativeUsername.find('@') == std::string::npos) {
        nativeUsername.append("@localhost");
    }

    // login
    client_->Login(nativeUsername, nativePassword, nativeServer, useSSL);
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeLogout(JNIEnv *env, jobject object) {
    if (client_) {
        client_->Disconnect();
    }
}

JNIEXPORT void JNICALL Java_com_tuenti_voice_VoiceClient_nativeRelease(JNIEnv *env,
        jobject object) {
    if (client_) {
        client_->Destroy(0);//Does an internal delete when all threads have stopped but a callback to do the delete here would be better
        client_ = NULL;
        env->DeleteGlobalRef(reference_object_);
    }
}

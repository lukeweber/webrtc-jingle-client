#include <string.h>
#include <stdio.h>
#include <jni.h>
#include "tuenti/threadpriorityhandler.h"
#include "tuenti/logging.h"

namespace tuenti{

void ThreadPriorityHandler::Init(JavaVM* jvm) {
	jvm_ = jvm;
}

void ThreadPriorityHandler::SetPriority(int tid, int priority) {
    JNIEnv *env;
    int status = jvm_->AttachCurrentThread(&env, NULL);
    if (status < 0) {
        LOGE("failed to attach native thread");
        return;
    }
    jclass clazz = (jclass)env->NewGlobalRef(env->FindClass("android/os/Process"));
    if (!clazz) {
        LOGE("Failed to get class reference");
        jvm_->DetachCurrentThread();
        return;
    }

    jmethodID method = env->GetStaticMethodID(clazz, "setThreadPriority", "(II)V");
    if (!method) {
        LOGE("Failed to get method ID");
        jvm_->DetachCurrentThread();
        return;
    }
    env->CallStaticVoidMethod(clazz, method, (jint) tid, (jint)priority);
    jvm_->DetachCurrentThread();
    return;
}
}

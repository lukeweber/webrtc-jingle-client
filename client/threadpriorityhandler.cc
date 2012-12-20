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
#include <string.h>
#include <stdio.h>
#include "client/threadpriorityhandler.h"
#include "client/logging.h"

#ifdef ANDROID
#include <jni.h>
#endif

namespace tuenti {

#ifdef ANDROID
JavaVM *ThreadPriorityHandler::jvm_ = NULL;

void ThreadPriorityHandler::Init(JavaVM* jvm) {
  jvm_ = jvm;
}
#endif

void ThreadPriorityHandler::SetPriority(int tid, int priority) {
#ifdef ANDROID
  JNIEnv *env;
  int status = jvm_->AttachCurrentThread(&env, NULL);
  if (status < 0) {
    LOGE("failed to attach native thread");
    return;
  }
  jclass clazz = (jclass) env->NewGlobalRef(
      env->FindClass("android/os/Process"));
  if (!clazz) {
    LOGE("Failed to get class reference");
    jvm_->DetachCurrentThread();
    return;
  }

  jmethodID method = env->GetStaticMethodID(clazz, "setThreadPriority",
      "(II)V");
  if (!method) {
    LOGE("Failed to get method ID");
    jvm_->DetachCurrentThread();
    return;
  }
  env->CallStaticVoidMethod(clazz, method, (jint) tid, (jint) priority);
  jvm_->DetachCurrentThread();
#endif
  return;
}
}

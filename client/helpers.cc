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

#include "client/helpers.h"
#include "voice_engine_defines.h"
#include "webrtc/voice_engine/include/voe_base.h"

#ifdef HAVE_WEBRTC_VIDEO
#include "webrtc/video_engine/include/vie_base.h"
#endif

jmethodID GetMethodIDCachedReferenced(JNIEnv *env, jclass clazz,
                                      JavaMethodIDCache *cache) {
  RETURN_VAL_IF_FAIL(env != NULL, NULL);
  RETURN_VAL_IF_FAIL(clazz != NULL, NULL);
  RETURN_VAL_IF_FAIL(cache != NULL, NULL);

  if (cache->mid == NULL) {
    cache->mid = env->GetMethodID(clazz, cache->name, cache->signature);
  }
  return cache->mid;
}

bool SetJavaObject(JavaObjectReference *ref, JNIEnv *env, jobject object, jobject context) {
  RETURN_VAL_IF_FAIL(env != NULL, false);
  RETURN_VAL_IF_FAIL(object != NULL, false);

  JavaVM *jvm;
  int success = env->GetJavaVM(&jvm);
  if (success != 0) {
    return false;
  }

  jclass localClass = env->GetObjectClass(object);
  if (localClass == NULL) {
    return false;
  }

  ref->java_class = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));
  ref->handler_object = env->NewGlobalRef(object);
  ref->jvm = jvm;

  webrtc::VoiceEngine::SetAndroidObjects(jvm, env, context);
#ifdef HAVE_WEBRTC_VIDEO
  webrtc::VideoEngine::SetAndroidObjects(jvm, context);
#endif

  env->DeleteLocalRef(localClass);

  return true;
}

void UnsetJavaObject(JavaObjectReference *ref, JNIEnv *env) {
  if (ref->java_class != NULL) {
    env->DeleteGlobalRef(ref->java_class);
  }
  if (ref->handler_object != NULL) {
    env->DeleteGlobalRef(ref->handler_object);
  }
  ref->java_class = NULL;
  ref->handler_object = NULL;
  ref->jvm = NULL;
}

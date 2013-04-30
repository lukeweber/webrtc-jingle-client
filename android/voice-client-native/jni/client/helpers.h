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

#ifndef CLIENT_HELPERS_H_
#define CLIENT_HELPERS_H_

#include <jni.h>
#include <sys/types.h>

#define RETURN_IF_FAIL(expr) { if (!(expr)) { return; }; }
#define RETURN_VAL_IF_FAIL(expr, val) { if (!(expr)) { return val; }; }

typedef struct {
  jobject handler_object;
  JavaVM *jvm;
  jclass java_class;
} JavaObjectReference;
#define JAVA_NULL_OBJECT_REF { NULL, NULL, NULL }

typedef struct {
  const char *name;
  const char *signature;
  jmethodID mid;
} JavaMethodIDCache;
#define METHOD_CACHE(name, signature, context, method) \
    static JavaMethodIDCache methodCache = { name, signature, NULL }; \
    method = GetMethodIDCachedReferenced(env, context->java_class, &methodCache);

#define NEW_OBJECT(type, count) ((type *) malloc ((unsigned) sizeof (type) * (count)))

#define ATTACH_TO_VM(context) \
    JNIEnv *env; \
    RETURN_IF_FAIL(reference_->jvm != NULL); \
    RETURN_IF_FAIL(reference_->handler_object != NULL); \
    RETURN_IF_FAIL(reference_->jvm->AttachCurrentThread(&env, NULL) == JNI_OK);

#define DETACH_FROM_VM(context) \
    RETURN_IF_FAIL(context->jvm != NULL); \
    RETURN_IF_FAIL(context->jvm->DetachCurrentThread() == JNI_OK);

#define CALLBACK_START(name, signature, context) \
    ATTACH_TO_VM(context); \
    jmethodID mid; \
    METHOD_CACHE(name, signature, context, mid);

#define CALLBACK_VOID(name, signature, context) \
    CALLBACK_START(name, signature, context); \
    if (mid != NULL) \
      env->CallVoidMethod(context->handler_object, mid); \
    DETACH_FROM_VM(context);

#define CALLBACK_DISPATCH(context, type, code, message, call) \
    CALLBACK_START("dispatchNativeEvent", "(IILjava/lang/String;J)V", context); \
    if (mid != NULL) { \
      jint type_jni = type; \
      jint code_jni = code; \
      jstring message_jni = env->NewStringUTF(message); \
      jlong call_jni = call; \
      env->CallVoidMethod(context->handler_object, mid, type_jni, code_jni, message_jni, call_jni); \
    } \
    DETACH_FROM_VM(context);


#define CALLBACK_JID(name, signature, context, jid) \
    CALLBACK_START(name, signature, context); \
    if (mid != NULL) { \
      jstring jid_obj = env->NewStringUTF(jid); \
      env->CallVoidMethod(context->handler_object, mid, jid_obj); \
    } \
    DETACH_FROM_VM(context);

jmethodID GetMethodIDCachedReferenced(JNIEnv *env, jclass clazz,
                                      JavaMethodIDCache *cache);

/**
 * Stores a reference to the given java object in the reference
 * location.
 */
bool SetJavaObject(JavaObjectReference *ref, JNIEnv *env, jobject object, jobject context);

#endif /* CLIENT_HELPERS_H_ */

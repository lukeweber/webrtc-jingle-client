#ifndef TUENTI_THREAD_PRIORITY_HANDLER_H_
#define TUENTI_THREAD_PRIORITY_HANDLER_H_

#include <string.h>
#include <stdio.h>
#include <jni.h>

namespace tuenti{

static JavaVM* jvm_;

class ThreadPriorityHandler {
public:
    static void Init(JavaVM* jvm);
    static void SetPriority(int tid, int priority);
};

};
#endif

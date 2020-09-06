#include "net_bytebuddy_test_c_NativeSample.h"

JNIEXPORT jint JNICALL Java_net_bytebuddy_test_c_NativeSample_foo
(JNIEnv* env, jobject obj, jint left, jint right) {
    return left * right;
}


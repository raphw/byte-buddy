#include <jni.h>

#ifndef _Included_net_bytebuddy_agent_VirtualMachine_ForHotSpot_Connection_ForJnaWindowsNamedPipe
#define _Included_net_bytebuddy_agent_VirtualMachine_ForHotSpot_Connection_ForJnaWindowsNamedPipe

/*
 * Class:     net_bytebuddy_agent_VirtualMachine_ForHotSpot_Connection_ForJnaWindowsNamedPipe
 * Method:    allocateRemoteCode
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_net_bytebuddy_agent_VirtualMachine_00024ForHotSpot_00024Connection_00024ForJnaWindowsNamedPipe_allocateRemoteCode
  (JNIEnv *, jclass, jlong);

/*
 * Class:     net_bytebuddy_agent_VirtualMachine_ForHotSpot_Connection_ForJnaWindowsNamedPipe
 * Method:    allocateRemoteArgument
 * Signature: (JLjava/lang/String;[Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_net_bytebuddy_agent_VirtualMachine_00024ForHotSpot_00024Connection_00024ForJnaWindowsNamedPipe_allocateRemoteArgument
  (JNIEnv *, jclass, jlong, jstring, jobjectArray);

#endif

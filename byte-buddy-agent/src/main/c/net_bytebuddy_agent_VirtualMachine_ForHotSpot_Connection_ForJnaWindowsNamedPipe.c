#include <jni.h>
#include <Windows.h>
#include "net_bytebuddy_agent_VirtualMachine_ForHotSpot_Connection_ForJnaWindowsNamedPipe.h"

#define ENQUEUE_ERROR 0xffff
#define CODE_SIZE 1024

typedef HMODULE (WINAPI *GetModuleHandle_t)(LPCTSTR);
typedef FARPROC (WINAPI *GetProcAddress_t)(HMODULE, LPCSTR);
typedef int (__stdcall *JVM_EnqueueOperation_t)(char *, char *, char *, char *, char *);

typedef struct {
    GetModuleHandle_t GetModuleHandleA;
    GetProcAddress_t GetProcAddress;
    char library[32];
    char command[32];
    char pipe[MAX_PATH];
    char argument[4][MAX_PATH];
} EnqueueOperation;

#pragma check_stack(off)

/**
 * Executes the attachment on the remote thread. This method is executed on the target JVM and must not reference any addresses unknown to that address.
 * @param argument The argument provided by the JVM executing the attachment.
 * @return The result of the attachment.
 */
DWORD WINAPI execute_remote_attach
  (LPVOID argument) 
{
    EnqueueOperation *operation = (EnqueueOperation *) argument;
    HMODULE library = operation->GetModuleHandleA(operation->library);
    if (library != NULL) {
        JVM_EnqueueOperation_t JVM_EnqueueOperation = (JVM_EnqueueOperation_t) operation->GetProcAddress(library, operation->command);
        if (JVM_EnqueueOperation != NULL) {
            return (DWORD) JVM_EnqueueOperation(operation->argument[0],
                                                operation->argument[1],
                                                operation->argument[2],
                                                operation->argument[3],
                                                operation->pipe);
        }
    }
    return ENQUEUE_ERROR;
}

#pragma check_stack

/**
 * Allocates the code to execute on the remote machine.
 * @param process The process handle of the remote process to which to attach.
 * @return A pointer to the allocated code or {@code NULL} if the allocation failed.
 */
LPVOID do_allocate_code
  (HANDLE process) 
{
    LPVOID code = VirtualAllocEx(process, NULL, CODE_SIZE, MEM_COMMIT, PAGE_EXECUTE_READWRITE);
    if (code == NULL) {
        return NULL;
    } else if (!WriteProcessMemory(process, code, execute_remote_attach, CODE_SIZE, NULL)) {
        VirtualFreeEx(process, NULL, CODE_SIZE, MEM_RELEASE);
        return NULL;
    } else {
        return code;
    }
}

/**
 * Allocates the argument to the remote execution.
 * @param process A handle to the remote process to which to attach.
 * @param pipe The name of the pipe to which the attachment result is written
 * @param argument An array of arguments to provide to the {@code JVM_EnqueueOperation}.
 * @return A pointer to the allocated argument or {@code NULL} if the allocation was not possible.
 */
static LPVOID do_allocate_remote_argument
  (HANDLE process, const char *pipe, const char *argument[4]) 
{
    EnqueueOperation payload;
    payload.GetModuleHandleA = GetModuleHandleA;
    payload.GetProcAddress = GetProcAddress;
    strcpy(payload.library, "jvm");
    strcpy(payload.command, "JVM_EnqueueOperation");
    strcpy(payload.pipe, pipe);
    int index;
    for (index = 0; index < 4; index++) {
        strcpy(payload.argument[0], argument[index] == NULL ? "" : argument[index]);
    }
    LPVOID allocation = VirtualAllocEx(process, NULL, sizeof(EnqueueOperation), MEM_COMMIT, PAGE_READWRITE);
    if (allocation == NULL) {
        return NULL;
    } else if (!WriteProcessMemory(process, allocation, &process, sizeof(process), NULL)) {
        VirtualFreeEx(process, NULL, sizeof(EnqueueOperation), MEM_RELEASE);
        return NULL;
    } else {
        return allocation;
    }
}

/**
 * Allocates the remote code that is executed on the target VM.
 * @param env The JNI environment.
 * @param type A reference to the type that defines this native method.
 * @param process A raw pointer to the process to attach to.
 * @return A raw pointer to the process to attach to or {@code 0} if an allocation was not possible.
 */
JNIEXPORT jlong JNICALL Java_net_bytebuddy_agent_VirtualMachine_00024ForHotSpot_00024Connection_00024ForJnaWindowsNamedPipe_allocateRemoteCode
  (JNIEnv *env, jclass type, jlong process)
{
    return (jlong) do_allocate_code((HANDLE) process);
}

/**
 * Allocates the argument that is provided to the remote JVM upon attachment.
 * @param env The JNI environment.
 * @param type A reference to the type that defines this native method.
 * @param process A raw pointer to the process to attach to.
 * @param pipe The name of the pipe to write the attachment result to.
 * @param arguments The arguments to provide to the attachment.
 * @return A raw pointer to the allocated data or {@code 0} if an allocation was not possible.
 */
JNIEXPORT jlong JNICALL Java_net_bytebuddy_agent_VirtualMachine_00024ForHotSpot_00024Connection_00024ForJnaWindowsNamedPipe_allocateRemoteArgument
  (JNIEnv *env, jclass type, jlong process, jstring pipe, jobjectArray arguments)
{
    jsize size = (*env)->GetArrayLength(env, argument);
    if (size > 4) {
        return NULL;
    }
    const char *resolvedName = (*env)->GetStringUTFChars(env, pipe, 0);
    const char *resolvedArgument[4];
    jstring rawArgument[4];
    int index;
    for (index = 0; index < 4; index++) {
        if (index < size) {
            rawArgument[index] = (*env)->GetObjectArrayElement(env, argument, index);
            resolvedArgument[index] = (*env)->GetStringUTFChars(env, rawArgument[index], 0);
        } else {
            rawArgument[index] = NULL;
            resolvedArgument[index] = NULL;
        }
    }
    jlong allocation = (jlong) do_allocate_remote_argument((HANDLE) process, resolvedName, resolvedArgument);
    (*env)->ReleaseStringUTFChars(env, pipe, resolvedName);
    for (index = 0; index < 4; index++) {
        if (resolvedArgument[index] != NULL) {
            (*env)->ReleaseStringUTFChars(env, rawArgument[index], resolvedArgument[index]);
        }
    }
    return allocation;
}

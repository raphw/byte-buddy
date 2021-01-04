/**
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <windows.h>

#define ENQUEUE_ERROR 0xffff
#define CODE_SIZE (SIZE_T) 1024
#define MAX_ARGUMENT 1024

typedef HMODULE (WINAPI *GetModuleHandle_t)(LPCTSTR);
typedef FARPROC (WINAPI *GetProcAddress_t)(HMODULE, LPCSTR);
typedef int (__stdcall *JVM_EnqueueOperation_t)(char *, char *, char *, char *, char *);

typedef struct {
    GetModuleHandle_t GetModuleHandleA;
    GetProcAddress_t GetProcAddress;
    char library[32];
    char command[32];
    char pipe[MAX_PATH];
    char argument[4][MAX_ARGUMENT];
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
LPVOID allocate_remote_code
  (HANDLE process) 
{
    LPVOID code = VirtualAllocEx(process, NULL, CODE_SIZE, MEM_COMMIT, PAGE_EXECUTE_READWRITE);
    if (code == NULL) {
        return NULL;
    } else if (!WriteProcessMemory(process, code, execute_remote_attach, CODE_SIZE, NULL)) {
        VirtualFreeEx(process, code, 0, MEM_RELEASE);
        return NULL;
    } else {
        return code;
    }
}

/**
 * Allocates the argument to the remote execution.
 * @param process A handle to the remote process to which to attach.
 * @param pipe The name of the pipe to which the attachment result is written
 * @param argument0 The first argument to provide to the {@code JVM_EnqueueOperation}.
 * @param argument1 The second argument to provide to the {@code JVM_EnqueueOperation}.
 * @param argument2 The third argument to provide to the {@code JVM_EnqueueOperation}.
 * @param argument3 The forth argument to provide to the {@code JVM_EnqueueOperation}.
 * @return A pointer to the allocated argument or {@code NULL} if the allocation was not possible.
 */
LPVOID allocate_remote_argument
  (HANDLE process, LPCSTR pipe, LPCSTR argument0, LPCSTR argument1, LPCSTR argument2, LPCSTR argument3) 
{
    if (strlen(pipe) >= MAX_PATH
            || argument0 != NULL && strlen(argument0) >= MAX_ARGUMENT
            || argument1 != NULL && strlen(argument1) >= MAX_ARGUMENT
            || argument2 != NULL && strlen(argument2) >= MAX_ARGUMENT
            || argument3 != NULL && strlen(argument3) >= MAX_ARGUMENT) {
        return NULL;
    }
    EnqueueOperation operation;
    operation.GetModuleHandleA = GetModuleHandleA;
    operation.GetProcAddress = GetProcAddress;
    strcpy(operation.library, "jvm");
    strcpy(operation.command, "JVM_EnqueueOperation");
    strcpy(operation.pipe, pipe);
    strcpy(operation.argument[0], argument0 == NULL ? "" : argument0);
    strcpy(operation.argument[1], argument1 == NULL ? "" : argument1);
    strcpy(operation.argument[2], argument2 == NULL ? "" : argument2);
    strcpy(operation.argument[3], argument3 == NULL ? "" : argument3);
    LPVOID allocation = VirtualAllocEx(process, NULL, sizeof(EnqueueOperation), MEM_COMMIT, PAGE_READWRITE);
    if (allocation == NULL) {
        return NULL;
    } else if (!WriteProcessMemory(process, allocation, &operation, sizeof(operation), NULL)) {
        VirtualFreeEx(process, allocation, 0, MEM_RELEASE);
        return NULL;
    } else {
        return allocation;
    }
}

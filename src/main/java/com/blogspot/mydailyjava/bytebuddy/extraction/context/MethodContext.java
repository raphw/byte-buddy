package com.blogspot.mydailyjava.bytebuddy.extraction.context;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;

public class MethodContext {

    private final int access;
    private final String name;
    private final String descriptor;
    private final Type type;
    private final String signature;
    private final String[] exceptions;

    public MethodContext(Method method) {
        access = method.getModifiers();
        name = method.getName();
        type = Type.getType(method);
        descriptor = type.getDescriptor();
        signature = null;
        Class<?>[] exceptionType = method.getExceptionTypes();
        exceptions = new String[method.getExceptionTypes().length];
        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i] = Type.getInternalName(exceptionType[i]);
        }
    }

    public MethodContext(int access, String name, String desc, String signature, String[] exceptions) {
        this.access = access;
        this.name = name;
        this.descriptor = desc;
        this.type = Type.getMethodType(desc);
        this.signature = signature;
        this.exceptions = exceptions == null ? new String[0] : exceptions;
    }

    public int getAccess() {
        return access;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Type getType() {
        return type;
    }

    public String getSignature() {
        return signature;
    }

    public String[] getExceptions() {
        return exceptions;
    }
}

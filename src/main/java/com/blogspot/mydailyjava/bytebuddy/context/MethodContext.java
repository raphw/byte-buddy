package com.blogspot.mydailyjava.bytebuddy.context;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodContext {

    private static List<String> asInternalNameList(Type[] type) {
        List<String> internalNames = new ArrayList<String>(type.length);
        for (Type aType : type) {
            internalNames.add(aType.getInternalName());
        }
        return internalNames;
    }

    private static String[] asInternalNameArray(Class<?>[] type) {
        String[] internalNames = new String[type.length];
        for (int i = 0; i < type.length; i++) {
            internalNames[i] = Type.getInternalName(type[i]);
        }
        return internalNames;
    }

    private final int access;
    private final String name;
    private final String descriptor;
    private final List<String> argumentType;
    private final String returnType;
    private final String signature;
    private final List<String> exceptions;

    public MethodContext(Method method) {
        this(method.getModifiers(), method.getName(), Type.getMethodDescriptor(method), "", asInternalNameArray(method.getExceptionTypes()));
    }

    public MethodContext(int access, String name, String desc, String signature, String[] exception) {
        this.access = access;
        this.name = name;
        this.descriptor = desc;
        this.argumentType = asInternalNameList(Type.getArgumentTypes(descriptor));
        this.returnType = Type.getReturnType(descriptor).getInternalName();
        this.signature = signature == null ? "" : signature;
        this.exceptions = exception == null ? Collections.<String>emptyList() : Arrays.asList(exception);
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

    public List<String> getArgumentType() {
        return argumentType;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getSignature() {
        return signature;
    }

    public List<String> getExceptions() {
        return exceptions;
    }
}

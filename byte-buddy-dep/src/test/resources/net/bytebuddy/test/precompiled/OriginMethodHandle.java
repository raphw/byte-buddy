package net.bytebuddy.test.precompiled;

import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Origin;

import java.lang.invoke.MethodHandle;

public class OriginMethodHandle {

    public static Class<?> TYPE = MethodHandle.class;

    public static Object foo(@Origin MethodHandle methodHandle) {
        return methodHandle;
    }
}

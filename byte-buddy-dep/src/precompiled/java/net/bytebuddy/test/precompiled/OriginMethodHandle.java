package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.invoke.MethodHandle;

public class OriginMethodHandle {

    public static Class<?> TYPE = MethodHandle.class;

    public static Object foo(@Origin MethodHandle methodHandle) {
        return methodHandle;
    }
}

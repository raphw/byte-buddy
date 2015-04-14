package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.invoke.MethodType;

public class OriginMethodType {

    public static Class<?> TYPE = MethodType.class;

    public static Object foo(@Origin MethodType methodType) {
        return methodType;
    }
}

package com.blogspot.mydailyjava.bytebuddy.type.instrumentation;

import com.blogspot.mydailyjava.bytebuddy.TypeManifestation;
import com.blogspot.mydailyjava.bytebuddy.Visibility;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.method.matcher.MethodMatcher;

public interface DynamicProxy {

    static interface Builder {

        static interface LocatedMethodInterception {

            Builder intercept(ByteCodeAppender.Factory byteCodeAppenderFactory);
        }

        Builder implementInterface(Class<?> interfaceType);

        Builder classVersion(int classVersion);

        Builder name(String name);

        Builder visibility(Visibility visibility);

        Builder manifestation(TypeManifestation typeManifestation);

        Builder makeSynthetic(boolean synthetic);

        Builder ignoredMethods(MethodMatcher ignoredMethods);

        LocatedMethodInterception method(MethodMatcher methodMatcher);

        DynamicProxy make();
    }

    byte[] getBytes();

    Class<?> load(ClassLoader classLoader);

    Class<?> loadReflective(ClassLoader classLoader);
}

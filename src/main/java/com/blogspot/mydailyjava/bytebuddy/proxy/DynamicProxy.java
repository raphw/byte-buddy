package com.blogspot.mydailyjava.bytebuddy.proxy;

import com.blogspot.mydailyjava.bytebuddy.TypeManifestation;
import com.blogspot.mydailyjava.bytebuddy.Visibility;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;

import java.util.Collection;

public interface DynamicProxy<T> {

    static interface Builder<T> {

        static interface LocatedMethodInterception<T> {

            Builder<T> intercept(ByteCodeAppender.Factory byteCodeAppenderFactory);
        }

        Builder<T> implementInterface(Class<?> interfaceType);

        Builder<T> classVersion(int classVersion);

        Builder<T> name(String name);

        Builder<T> visibility(Visibility visibility);

        Builder<T> manifestation(TypeManifestation typeManifestation);

        Builder<T> makeSynthetic(boolean synthetic);

        Builder<T> ignoredMethods(MethodMatcher ignoredMethods);

        LocatedMethodInterception<T> method(MethodMatcher methodMatcher);

        DynamicProxy<T> make();
    }

    static interface Loaded<T> {

        Class<? extends T> getProxyClass();

        Collection<Class<?>> getHelperClasses();
    }

    byte[] getBytes();

    Loaded<T> load(ClassLoader classLoader);

    Loaded<T> loadReflective(ClassLoader classLoader);
}

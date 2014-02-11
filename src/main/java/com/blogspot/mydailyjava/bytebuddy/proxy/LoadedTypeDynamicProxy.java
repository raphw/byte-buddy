package com.blogspot.mydailyjava.bytebuddy.proxy;

import java.util.Collection;
import java.util.Collections;

public class LoadedTypeDynamicProxy<T> implements DynamicProxy.Loaded<T> {

    private final Class<? extends T> proxyClass;

    public LoadedTypeDynamicProxy(Class<? extends T> proxyClass) {
        this.proxyClass = proxyClass;
    }

    @Override
    public Class<? extends T> getProxyClass() {
        return proxyClass;
    }

    @Override
    public Collection<Class<?>> getHelperClasses() {
        return Collections.emptySet();
    }
}

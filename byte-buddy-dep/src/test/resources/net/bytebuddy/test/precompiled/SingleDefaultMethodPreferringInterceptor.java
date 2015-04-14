package net.bytebuddy.test.precompiled;

import net.bytebuddy.implementation.bind.annotation.DefaultCall;

import java.util.concurrent.Callable;

public class SingleDefaultMethodPreferringInterceptor {

    public static Object foo(@DefaultCall(targetType = SingleDefaultMethodInterface.class) Callable<?> callable) throws Exception {
        return callable.call();
    }
}

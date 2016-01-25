package net.bytebuddy.agent.builder;

import sun.misc.Unsafe;

import java.lang.invoke.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Foo {

    public static final int FLAG_SERIALIZABLE = 1 << 0;

    public static final int FLAG_MARKERS = 1 << 1;

    public static final int FLAG_BRIDGES = 1 << 2;

    public static CallSite metafactory(MethodHandles.Lookup caller,
                                       String invokedName,
                                       MethodType invokedType,
                                       MethodType samMethodType,
                                       MethodHandle implMethod,
                                       MethodType instantiatedMethodType)
            throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        final Class<?> lambdaClass = unsafe.defineAnonymousClass(caller.lookupClass(),
                (byte[]) ClassLoader.getSystemClassLoader().loadClass("net.bytebuddy.agent.builder.LambdaFactory").getDeclaredMethod("make",
                        Object.class,
                        String.class,
                        Object.class,
                        Object.class,
                        Object.class,
                        Object.class,
                        boolean.class,
                        List.class).invoke(null, caller, invokedName, invokedType, samMethodType, implMethod, instantiatedMethodType, false, Collections.emptyList()),
                null);
        unsafe.ensureClassInitialized(lambdaClass);
        return invokedType.parameterCount() == 0 // lookup() == IMPL_LOOKUP
                ? new ConstantCallSite(MethodHandles.constant(invokedType.returnType(), lambdaClass.getDeclaredConstructors()[0].newInstance()))
                : new ConstantCallSite(MethodHandles.lookup().findStatic(lambdaClass, "get$Lambda", invokedType));
    }

    public static CallSite altMetafactory(MethodHandles.Lookup caller,
                                          String invokedName,
                                          MethodType invokedType,
                                          Object... args) throws Exception {
        int flags = (Integer) args[3];
        Class<?>[] markerInterface;
        if ((flags & FLAG_MARKERS) != 0) {
            int markerCount = (Integer) args[4];
            markerInterface = new Class<?>[markerCount];
            System.arraycopy(args, 5, markerInterface, 0, markerCount);
        } else {
            markerInterface = new Class<?>[0];
        }
        Unsafe unsafe = Unsafe.getUnsafe();
        final Class<?> lambdaClass = unsafe.defineAnonymousClass(caller.lookupClass(),
                (byte[]) ClassLoader.getSystemClassLoader().loadClass("net.bytebuddy.agent.builder.LambdaFactory").getDeclaredMethod("make",
                        Object.class,
                        String.class,
                        Object.class,
                        Object.class,
                        Object.class,
                        Object.class,
                        boolean.class,
                        List.class).invoke(null, caller, invokedName, invokedType, args[0], args[1], args[2], ((flags & FLAG_SERIALIZABLE) != 0), Arrays.asList(markerInterface)),
                null);
        unsafe.ensureClassInitialized(lambdaClass);
        return invokedType.parameterCount() == 0
                ? new ConstantCallSite(MethodHandles.constant(invokedType.returnType(), lambdaClass.getDeclaredConstructors()[0].newInstance()))
                : new ConstantCallSite(MethodHandles.lookup().findStatic(lambdaClass, "get$Lambda", invokedType));
    }

    void qux () {
        Object o = null;
        byte[] b = (byte[]) o;
    }
}

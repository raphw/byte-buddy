package net.bytebuddy.test.precompiled;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class StandardArgumentBootstrap extends ConstantCallSite {

    private static final String FOO = "foo";

    public StandardArgumentBootstrap(Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        super(((MethodHandles.Lookup) args[0]).findStatic(StandardArgumentBootstrap.class, (String) args[1], (MethodType) args[2]));
    }

    public StandardArgumentBootstrap(MethodHandles.Lookup lookup, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        super(lookup.findStatic(StandardArgumentBootstrap.class, (String) args[0], (MethodType) args[1]));
    }

    public StandardArgumentBootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType)
            throws NoSuchMethodException, IllegalAccessException {
        super(lookup.findStatic(StandardArgumentBootstrap.class, methodName, methodType));
    }

    public StandardArgumentBootstrap(MethodHandles.Lookup lookup, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        super(lookup.findStatic(StandardArgumentBootstrap.class, methodName, (MethodType) args[0]));
    }

    public static CallSite bootstrap(Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(((MethodHandles.Lookup) args[0]).findStatic(StandardArgumentBootstrap.class, (String) args[1], (MethodType) args[2]));
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(StandardArgumentBootstrap.class, (String) args[0], (MethodType) args[1]));
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(StandardArgumentBootstrap.class, methodName, methodType));
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(StandardArgumentBootstrap.class, methodName, (MethodType) args[0]));
    }

    public static String foo() {
        return FOO;
    }
}

package net.bytebuddy.test.precompiled;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ParameterBootstrap {

    private static final String FOO = "foo";

    public static Object[] arguments;

    public static CallSite bootstrapArrayArguments(MethodHandles.Lookup lookup,
                                                   String methodName,
                                                   MethodType methodType,
                                                   Object... arguments)
            throws NoSuchMethodException, IllegalAccessException {
        ParameterBootstrap.arguments = arguments;
        return new ConstantCallSite(lookup.findStatic(ParameterBootstrap.class, methodName, methodType));
    }

    public static CallSite bootstrapExplicitArguments(MethodHandles.Lookup lookup,
                                                      String methodName,
                                                      MethodType methodType,
                                                      int arg0,
                                                      int arg1,
                                                      int arg2,
                                                      int arg3,
                                                      int arg4,
                                                      long arg5,
                                                      float arg6,
                                                      double arg7,
                                                      String arg8,
                                                      Class<?> arg9)
            throws NoSuchMethodException, IllegalAccessException {
        ParameterBootstrap.arguments = new Object[]{arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9};
        return new ConstantCallSite(lookup.findStatic(ParameterBootstrap.class, methodName, methodType));
    }

    public static String foo() {
        return FOO;
    }
}

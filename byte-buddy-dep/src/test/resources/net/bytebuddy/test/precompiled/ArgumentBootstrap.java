package net.bytebuddy.test.precompiled;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class ArgumentBootstrap {

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(ArgumentBootstrap.class, methodName, methodType));
    }

    public static String foo(boolean arg0,
                             byte arg1,
                             short arg2,
                             char arg3,
                             int arg4,
                             long arg5,
                             float arg6,
                             double arg7,
                             String arg8,
                             Class<?> arg9,
                             Object arg10) {
        return "" + arg0 + arg1 + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8 + arg9 + arg10;
    }

    public static String bar(Boolean arg0,
                             Byte arg1,
                             Short arg2,
                             Character arg3,
                             Integer arg4,
                             Long arg5,
                             Float arg6,
                             Double arg7,
                             String arg8,
                             Class<?> arg9,
                             Object arg10) {
        return "" + arg0 + arg1 + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8 + arg9 + arg10;
    }

    public static String qux(String arg) {
        return arg;
    }

    public static String baz(Object arg) {
        return arg.toString();
    }
}

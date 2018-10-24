/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.test.precompiled;

import java.lang.invoke.*;

public class DynamicInvokeBootstrap extends ConstantCallSite {

    private static final String FOO = "foo";

    public static Object[] arguments;

    public DynamicInvokeBootstrap(Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        super(((MethodHandles.Lookup) args[0]).findStatic(DynamicInvokeBootstrap.class, (String) args[1], (MethodType) args[2]));
    }

    public DynamicInvokeBootstrap(MethodHandles.Lookup lookup, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        super(lookup.findStatic(DynamicInvokeBootstrap.class, (String) args[0], (MethodType) args[1]));
    }

    public DynamicInvokeBootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType)
            throws NoSuchMethodException, IllegalAccessException {
        super(lookup.findStatic(DynamicInvokeBootstrap.class, methodName, methodType));
    }

    public DynamicInvokeBootstrap(MethodHandles.Lookup lookup, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        super(lookup.findStatic(DynamicInvokeBootstrap.class, methodName, (MethodType) args[0]));
    }

    public static CallSite bootstrap(Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(((MethodHandles.Lookup) args[0]).findStatic(DynamicInvokeBootstrap.class, (String) args[1], (MethodType) args[2]));
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(DynamicInvokeBootstrap.class, (String) args[0], (MethodType) args[1]));
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(DynamicInvokeBootstrap.class, methodName, methodType));
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(DynamicInvokeBootstrap.class, methodName, (MethodType) args[0]));
    }

    public static CallSite bootstrapSimple(MethodHandles.Lookup lookup, String methodName, MethodType methodType)
            throws NoSuchMethodException, IllegalAccessException {
        return new ConstantCallSite(lookup.findStatic(DynamicInvokeBootstrap.class, methodName, methodType));
    }

    public static CallSite bootstrapArrayArguments(MethodHandles.Lookup lookup,
                                                   String methodName,
                                                   MethodType methodType,
                                                   Object... arguments)
            throws NoSuchMethodException, IllegalAccessException {
        DynamicInvokeBootstrap.arguments = arguments;
        return new ConstantCallSite(lookup.findStatic(DynamicInvokeBootstrap.class, methodName, methodType));
    }

    public static CallSite bootstrapExplicitArguments(MethodHandles.Lookup lookup,
                                                      String methodName,
                                                      MethodType methodType,
                                                      int arg0,
                                                      long arg1,
                                                      float arg2,
                                                      double arg3,
                                                      String arg4,
                                                      Class<?> arg5,
                                                      MethodType arg6,
                                                      MethodHandle arg7)
            throws NoSuchMethodException, IllegalAccessException {
        DynamicInvokeBootstrap.arguments = new Object[]{arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7};
        return new ConstantCallSite(lookup.findStatic(DynamicInvokeBootstrap.class, methodName, methodType));
    }

    public static String foo() {
        return FOO;
    }

    public static String foo(boolean arg0,
                             byte arg1,
                             short arg2,
                             char arg3,
                             int arg4,
                             long arg5,
                             float arg6,
                             double arg7,
                             Class<?> arg8,
                             SampleEnum arg9,
                             MethodType arg10,
                             MethodHandle arg11,
                             String arg12,
                             Class<?> arg13,
                             SampleEnum arg14,
                             MethodType arg15,
                             MethodHandle arg16,
                             Object arg17) {
        return "" + arg0 + arg1 + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8 + arg9 + arg10 + arg11 + arg12 + arg13 + arg14 + arg15 + arg16 + arg17;
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
                             SampleEnum arg10,
                             MethodType arg11,
                             MethodHandle arg12,
                             Object arg13) {
        return "" + arg0 + arg1 + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8 + arg9 + arg10 + arg11 + arg12 + arg13;
    }

    public static String qux(String arg) {
        return arg;
    }

    public static String baz(Object arg) {
        return arg.toString();
    }

    public enum SampleEnum {
        INSTANCE;
    }
}

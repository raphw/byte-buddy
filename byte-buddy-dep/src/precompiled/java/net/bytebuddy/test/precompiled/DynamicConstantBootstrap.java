/*
 * Copyright 2014 - Present Rafael Winterhalter
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class DynamicConstantBootstrap {

    public DynamicConstantBootstrap(Object... args) {
        /* empty */
    }

    public DynamicConstantBootstrap(MethodHandles.Lookup lookup, Object... args) {
        /* empty */
    }

    public DynamicConstantBootstrap(MethodHandles.Lookup lookup, String name, Object... args) {
        /* empty */
    }

    public DynamicConstantBootstrap(MethodHandles.Lookup lookup, String name, Class<?> type) {
        /* empty */
    }

    public DynamicConstantBootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, Object... arguments) {
        /* empty */
    }

    public DynamicConstantBootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, DynamicConstantBootstrap argument) {
        if (argument == null) {
            throw new AssertionError();
        }
    }

    public DynamicConstantBootstrap(MethodHandles.Lookup lookup,
                                    String name,
                                    Class<?> type,
                                    int intValue,
                                    long longValue,
                                    float floatValue,
                                    double doubleValue,
                                    String stringValue,
                                    Class<?> typeValue,
                                    MethodHandle methodHandle,
                                    MethodType methodType) {
        if (intValue != 42
                || longValue != 42L
                || floatValue != 42f
                || doubleValue != 42d
                || !stringValue.equals("foo")
                || typeValue != Object.class
                || methodHandle == null
                || methodType == null) {
            throw new AssertionError();
        }
    }

    public DynamicConstantBootstrap(int intValue,
                                    long longValue,
                                    float floatValue,
                                    double doubleValue,
                                    String stringValue,
                                    Class<?> typeValue,
                                    MethodHandle methodHandle,
                                    MethodType methodType) {
        if (intValue != 42
                || longValue != 42L
                || floatValue != 42f
                || doubleValue != 42d
                || !stringValue.equals("foo")
                || typeValue != Object.class
                || methodHandle == null
                || methodType == null) {
            throw new AssertionError();
        }
    }

    public static DynamicConstantBootstrap bootstrap(Object... args) {
        return new DynamicConstantBootstrap();
    }

    public static DynamicConstantBootstrap bootstrap(MethodHandles.Lookup lookup, Object... args) {
        return new DynamicConstantBootstrap();
    }

    public static DynamicConstantBootstrap bootstrap(MethodHandles.Lookup lookup, String name, Object... args) {
        return new DynamicConstantBootstrap();
    }

    public static DynamicConstantBootstrap bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return new DynamicConstantBootstrap();
    }

    public static DynamicConstantBootstrap bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, Object... arguments) {
        return new DynamicConstantBootstrap();
    }

    public static DynamicConstantBootstrap bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, DynamicConstantBootstrap argument) {
        if (argument == null) {
            throw new AssertionError();
        }
        return new DynamicConstantBootstrap();
    }

    public static DynamicConstantBootstrap bootstrap(MethodHandles.Lookup lookup,
                                                     String name,
                                                     Class<?> type,
                                                     int intValue,
                                                     long longValue,
                                                     float floatValue,
                                                     double doubleValue,
                                                     String stringValue,
                                                     Class<?> typeValue,
                                                     MethodHandle methodHandle,
                                                     MethodType methodType) {
        if (intValue != 42
                || longValue != 42L
                || floatValue != 42f
                || doubleValue != 42d
                || !stringValue.equals("foo")
                || typeValue != Object.class
                || methodHandle == null
                || methodType == null) {
            throw new AssertionError();
        }
        return new DynamicConstantBootstrap();
    }

    public static DynamicConstantBootstrap make(int intValue,
                                                long longValue,
                                                float floatValue,
                                                double doubleValue,
                                                String stringValue,
                                                Class<?> typeValue,
                                                MethodHandle methodHandle,
                                                MethodType methodType) {
        if (intValue != 42
                || longValue != 42L
                || floatValue != 42f
                || doubleValue != 42d
                || !stringValue.equals("foo")
                || typeValue != Object.class
                || methodHandle == null
                || methodType == null) {
            throw new AssertionError();
        }
        return new DynamicConstantBootstrap();
    }
}

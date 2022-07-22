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
package net.bytebuddy.test.precompiled.v7;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

public class DynamicSampleBootstrap {

    @SuppressWarnings("unused")
    public static ConstantCallSite callable(MethodHandles.Lookup lookup, String name, MethodType type, final String argument) {
        return new ConstantCallSite(MethodHandles.constant(Callable.class, Proxy.newProxyInstance(
                lookup.lookupClass().getClassLoader(),
                new Class<?>[]{Callable.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object o, Method method, Object[] objects) {
                        return argument;
                    }
                }
        )));
    }
}

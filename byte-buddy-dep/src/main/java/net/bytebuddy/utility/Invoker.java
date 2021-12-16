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
package net.bytebuddy.utility;

import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An invoker is a deliberate indirection to wrap indirect calls. This way, reflective call are never dispatched from Byte Buddy's
 * context but always from a synthetic layer that does not own any privileges. This might not be the case if such security measures
 * are not supported on the current platform, for example on Android. To support the Java module system and other class loader with
 * explicit exports such as OSGi, this interface is placed in an exported package while intended for use with
 * {@link net.bytebuddy.utility.dispatcher.JavaDispatcher}.
 */
public interface Invoker {

    /**
     * Creates a new instance via {@link Constructor#newInstance(Object...)}.
     *
     * @param constructor The constructor to invoke.
     * @param argument    The constructor arguments.
     * @return The constructed instance.
     * @throws InstantiationException    If the instance cannot be constructed.
     * @throws IllegalAccessException    If the constructor is accessed illegally.
     * @throws InvocationTargetException If the invocation causes an error.
     */
    Object newInstance(Constructor<?> constructor, Object[] argument) throws InstantiationException, IllegalAccessException, InvocationTargetException;

    /**
     * Invokes a method via {@link Method#invoke(Object, Object...)}.
     *
     * @param method   The method to invoke.
     * @param instance The instance upon which to invoke the method or {@code null} if the method is static.
     * @param argument The method arguments.
     * @return The return value of the method or {@code null} if the method is {@code void}.
     * @throws IllegalAccessException    If the method is accessed illegally.
     * @throws InvocationTargetException If the invocation causes an error.
     */
    @MaybeNull
    Object invoke(Method method, @MaybeNull Object instance, @MaybeNull Object[] argument) throws IllegalAccessException, InvocationTargetException;
}

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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

/**
 * A dispatcher for creating a proxy that invokes methods of a type that is possibly unknown on the current VM.
 *
 * @param <T> The resolved type.
 */
@HashCodeAndEqualsPlugin.Enhance
public class JavaDispatcher<T> implements PrivilegedAction<T> {

    /**
     * The proxy type.
     */
    private final Class<T> proxy;

    /**
     * The class loader to resolve the proxied type from or {@code null} if the bootstrap loader should be used.
     */
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
    private final ClassLoader classLoader;

    /**
     * Creates a new dispatcher.
     *
     * @param proxy       The proxy type.
     * @param classLoader The class loader to resolve the proxied type from or {@code null} if the bootstrap loader should be used.
     */
    protected JavaDispatcher(Class<T> proxy, ClassLoader classLoader) {
        this.proxy = proxy;
        this.classLoader = classLoader;
    }

    /**
     * Resolves an action for creating a dispatcher for the provided type where the proxied type is resolved from the bootstrap loader.
     *
     * @param type The type for which a dispatcher should be resolved.
     * @param <T>  The resolved type.
     * @return An action for creating an appropriate dispatcher.
     */
    public static <T> PrivilegedAction<T> of(Class<T> type) {
        return of(type, null);
    }

    /**
     * Resolves an action for creating a dispatcher for the provided type.
     *
     * @param type        The type for which a dispatcher should be resolved.
     * @param classLoader The class loader to resolve the proxied type from.
     * @param <T>         The resolved type.
     * @return An action for creating an appropriate dispatcher.
     */
    public static <T> PrivilegedAction<T> of(Class<T> type, ClassLoader classLoader) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException();
        } else if (!type.isAnnotationPresent(Proxied.class)) {
            throw new IllegalArgumentException();
        }
        return new JavaDispatcher<T>(type, classLoader);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T run() {
        Map<Method, ProxiedInvocationHandler.Dispatcher> dispatchers = new HashMap<Method, ProxiedInvocationHandler.Dispatcher>();
        boolean defaults = proxy.isAnnotationPresent(Defaults.class);
        String name = proxy.getAnnotation(Proxied.class).value();
        Class<?> target;
        try {
            target = Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException exception) {
            for (Method method : proxy.getMethods()) {
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }
                if (method.isAnnotationPresent(Instance.class)) {
                    dispatchers.put(method, new ProxiedInvocationHandler.Dispatcher.ForFixedValue(false));
                } else {
                    dispatchers.put(method, defaults || method.isAnnotationPresent(Defaults.class)
                            ? ProxiedInvocationHandler.Dispatcher.ForFixedValue.of(method.getReturnType())
                            : new ProxiedInvocationHandler.Dispatcher.ForUnresolvedMethod("Type not available on current VM: " + exception.getMessage()));
                }
            }
            return (T) Proxy.newProxyInstance(proxy.getClassLoader(),
                    new Class<?>[]{proxy},
                    new ProxiedInvocationHandler(name, dispatchers));
        }
        for (Method method : proxy.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (method.isAnnotationPresent(Instance.class)) {
                if (method.getParameterTypes().length != 1 || !method.getParameterTypes()[0].isAssignableFrom(target)) {
                    throw new IllegalStateException("Instance check requires a single regular-typed argument: " + method);
                } else {
                    dispatchers.put(method, new ProxiedInvocationHandler.Dispatcher.ForInstanceCheck(target));
                }
            } else if (method.isAnnotationPresent(Container.class)) {
                if (method.getParameterTypes().length != 1 || method.getParameterTypes()[0] != int.class) {
                    throw new IllegalStateException("Container creation requires a single int-typed argument: " + method);
                } else {
                    dispatchers.put(method, new ProxiedInvocationHandler.Dispatcher.ForContainerCreation(target));
                }
            } else {
                try {
                    Class<?>[] parameterType = method.getParameterTypes();
                    int offset;
                    if (method.isAnnotationPresent(Static.class)) {
                        offset = 0;
                    } else {
                        offset = 1;
                        if (parameterType.length == 0) {
                            throw new IllegalStateException("Expected self type: " + method);
                        } else if (!parameterType[0].isAssignableFrom(target)) {
                            throw new IllegalStateException("Cannot assign self type: " + target + " on " + method);
                        }
                        Class<?>[] adjusted = new Class<?>[parameterType.length - 1];
                        System.arraycopy(parameterType, 1, adjusted, 0, adjusted.length);
                        parameterType = adjusted;
                    }
                    Annotation[][] parameterAnnotation = method.getParameterAnnotations();
                    for (int index = 0; index < parameterType.length; index++) {
                        for (Annotation annotation : parameterAnnotation[index + offset]) {
                            if (annotation instanceof Proxied) {
                                int arity = 0;
                                while (parameterType[index].isArray()) {
                                    arity += 1;
                                    parameterType[index] = parameterType[index].getComponentType();
                                }
                                if (arity > 0) {
                                    if (parameterType[index].isPrimitive()) {
                                        throw new IllegalStateException("Primitive values are not supposed to be proxied: " + index + " of " + method);
                                    } else if (!parameterType[index].isAssignableFrom(Class.forName(((Proxied) annotation).value(), false, classLoader))) {
                                        throw new IllegalStateException("Cannot resolve to component type: " + ((Proxied) annotation).value() + " at " + index + " of " + method);
                                    }
                                    StringBuilder stringBuilder = new StringBuilder();
                                    while (arity-- > 0) {
                                        stringBuilder.append('[');
                                    }
                                    parameterType[index] = Class.forName(stringBuilder.append('L')
                                            .append(((Proxied) annotation).value())
                                            .append(';')
                                            .toString(), false, classLoader);
                                } else {
                                    Class<?> resolved = Class.forName(((Proxied) annotation).value(), false, classLoader);
                                    if (!parameterType[index].isAssignableFrom(resolved)) {
                                        throw new IllegalStateException("Cannot resolve to type: " + resolved.getName() + " at " + index + " of " + method);
                                    }
                                    parameterType[index] = resolved;
                                }
                                break;
                            }
                        }
                    }
                    Proxied proxied = method.getAnnotation(Proxied.class);
                    Method resolved = target.getMethod(proxied == null ? method.getName() : proxied.value(), parameterType);
                    if (!method.getReturnType().isAssignableFrom(resolved.getReturnType())) {
                        throw new IllegalStateException("Cannot assign " + resolved.getReturnType().getName() + " to " + method);
                    }
                    dispatchers.put(method, Modifier.isStatic(resolved.getModifiers())
                            ? new ProxiedInvocationHandler.Dispatcher.ForStaticMethod(resolved)
                            : new ProxiedInvocationHandler.Dispatcher.ForNonStaticMethod(resolved));
                } catch (ClassNotFoundException exception) {
                    dispatchers.put(method, defaults || method.isAnnotationPresent(Defaults.class)
                            ? ProxiedInvocationHandler.Dispatcher.ForFixedValue.of(method.getReturnType())
                            : new ProxiedInvocationHandler.Dispatcher.ForUnresolvedMethod("Class not available on current VM: " + exception.getMessage()));
                } catch (NoSuchMethodException exception) {
                    dispatchers.put(method, defaults || method.isAnnotationPresent(Defaults.class)
                            ? ProxiedInvocationHandler.Dispatcher.ForFixedValue.of(method.getReturnType())
                            : new ProxiedInvocationHandler.Dispatcher.ForUnresolvedMethod("Method not available on current VM: " + exception.getMessage()));
                } catch (Throwable throwable) {
                    dispatchers.put(method, new ProxiedInvocationHandler.Dispatcher.ForUnresolvedMethod("Unexpected error: " + throwable.getMessage()));
                }
            }
        }
        return (T) Proxy.newProxyInstance(proxy.getClassLoader(),
                new Class<?>[]{proxy},
                new ProxiedInvocationHandler(target.getName(), dispatchers));
    }

    /**
     * Indicates a proxied type's name. This annotation is mandatory for proxied types but can also be used on method's
     * to describe the actual name of the proxied method or on parameters to indicate the parameter's (component) type.
     */
    @Documented
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Proxied {

        /**
         * Returns the binary name of the proxied type.
         *
         * @return The binary name of the proxied type.
         */
        String value();
    }

    /**
     * Indicates that a proxied method is static.
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Static {
        /* empty */
    }

    /**
     * Indicates that a method is supposed to perform an instance check. The annotated method must declare a single argument
     * with a type that is assignable from the proxied type.
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Instance {
        /* empty */
    }

    /**
     * Indicates that the method is supposed to return an array of the proxied type. The annotated method must declare a single,
     * {@code int}-typed argument that represents the array's dimension.
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Container {
        /* empty */
    }

    /**
     * Indicates that a method is supposed to return a default value if a method or type could not be resolved.
     */
    @Documented
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Defaults {
        /* empty */
    }

    /**
     * An invocation handler that invokes given dispatchers.
     */
    protected static class ProxiedInvocationHandler implements InvocationHandler {

        /**
         * The proxied type's name.
         */
        private final String name;

        /**
         * A mapping of proxy type methods to their proxied dispatchers.
         */
        private final Map<Method, Dispatcher> targets;

        /**
         * Creates a new invocation handler for proxying a type.
         *
         * @param name    The proxied type's name.
         * @param targets A mapping of proxy type methods to their proxied dispatchers.
         */
        protected ProxiedInvocationHandler(String name, Map<Method, Dispatcher> targets) {
            this.name = name;
            this.targets = targets;
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Object proxy, Method method, Object[] argument) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("hashCode")) {
                    return hashCode();
                } else if (method.getName().equals("equals")) {
                    return argument[0] != null
                            && Proxy.isProxyClass(argument[0].getClass())
                            && Proxy.getInvocationHandler(argument[0]).equals(this);
                } else if (method.getName().equals("toString")) {
                    return "Call proxy for " + name;
                } else {
                    throw new IllegalStateException("Unexpected object method: " + method);
                }
            }
            Dispatcher dispatcher = targets.get(method);
            try {
                try {
                    if (dispatcher == null) {
                        throw new IllegalStateException("No proxy target found for " + method);
                    } else {
                        return dispatcher.invoke(argument);
                    }
                } catch (InvocationTargetException exception) {
                    throw exception.getCause();
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Throwable throwable) {
                for (Class<?> type : method.getExceptionTypes()) {
                    if (type.isInstance(throwable)) {
                        throw throwable;
                    }
                }
                throw new IllegalStateException("Failed to invoke proxy for " + method, throwable);
            }
        }

        /**
         * A dispatcher for handling a proxied method.
         */
        protected interface Dispatcher {

            /**
             * Invokes the proxied action.
             *
             * @param argument The arguments provided.
             * @return The return value.
             * @throws Throwable If any error occurs.
             */
            Object invoke(Object[] argument) throws Throwable;

            /**
             * A dispatcher that performs an instance check.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForInstanceCheck implements Dispatcher {

                /**
                 * The checked type.
                 */
                private final Class<?> target;

                /**
                 * Creates a dispatcher for an instance check.
                 *
                 * @param target The checked type.
                 */
                protected ForInstanceCheck(Class<?> target) {
                    this.target = target;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) throws Throwable {
                    return target.isInstance(argument[0]);
                }
            }

            /**
             * A dispatcher that creates an array.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForContainerCreation implements Dispatcher {

                /**
                 * The component type.
                 */
                private final Class<?> target;

                /**
                 * Creates a dispatcher for an array creation.
                 *
                 * @param target The component type.
                 */
                protected ForContainerCreation(Class<?> target) {
                    this.target = target;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) throws Throwable {
                    return Array.newInstance(target, (Integer) argument[0]);
                }
            }

            /**
             * A dispatcher that returns a fixed value.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForFixedValue implements Dispatcher {

                /**
                 * The returned value.
                 */
                @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                private final Object value;

                /**
                 * Creates a dispatcher that returns a fixed value.
                 *
                 * @param value The returned value.
                 */
                protected ForFixedValue(Object value) {
                    this.value = value;
                }

                /**
                 * Resolves a fixed value for a given type.
                 *
                 * @param type The type to resolve.
                 * @return An appropriate dispatcher.
                 */
                protected static Dispatcher of(Class<?> type) {
                    if (type == boolean.class) {
                        return new ForFixedValue(false);
                    } else if (type == byte.class) {
                        return new ForFixedValue((byte) 0);
                    } else if (type == short.class) {
                        return new ForFixedValue((short) 0);
                    } else if (type == char.class) {
                        return new ForFixedValue((char) 0);
                    } else if (type == int.class) {
                        return new ForFixedValue(0);
                    } else if (type == long.class) {
                        return new ForFixedValue(0L);
                    } else if (type == float.class) {
                        return new ForFixedValue(0f);
                    } else if (type == double.class) {
                        return new ForFixedValue(0d);
                    } else if (type.isArray()) {
                        return new ForFixedValue(Array.newInstance(type.getComponentType(), 0));
                    } else {
                        return new ForFixedValue(null);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) throws Throwable {
                    return value;
                }
            }

            /**
             * A dispatcher for invoking a static proxied method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForStaticMethod implements Dispatcher {

                /**
                 * The proxied method.
                 */
                private final Method method;

                /**
                 * Creates a dispatcher for invoking a static method.
                 *
                 * @param method The proxied method.
                 */
                protected ForStaticMethod(Method method) {
                    this.method = method;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) throws Throwable {
                    return method.invoke(null, argument);
                }
            }

            /**
             * A dispatcher for invoking a non-static proxied method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForNonStaticMethod implements Dispatcher {

                /**
                 * Indicates a call without arguments.
                 */
                private static final Object[] NO_ARGUMENTS = new Object[0];

                /**
                 * The proxied method.
                 */
                private final Method method;

                /**
                 * Creates a dispatcher for invoking a non-static method.
                 *
                 * @param method The proxied method.
                 */
                protected ForNonStaticMethod(Method method) {
                    this.method = method;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) throws Throwable {
                    Object[] reduced;
                    if (argument.length == 1) {
                        reduced = NO_ARGUMENTS;
                    } else {
                        reduced = new Object[argument.length - 1];
                        System.arraycopy(argument, 1, reduced, 0, reduced.length);
                    }
                    return method.invoke(argument[0], reduced);
                }
            }

            /**
             * A dispatcher for an unresolved method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForUnresolvedMethod implements Dispatcher {

                /**
                 * The message for describing the reason why the method could not be resolved.
                 */
                private final String message;

                /**
                 * Creates a dispatcher for an unresolved method.
                 *
                 * @param message The message for describing the reason why the method could not be resolved.
                 */
                protected ForUnresolvedMethod(String message) {
                    this.message = message;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) throws Throwable {
                    throw new IllegalStateException("Could not invoke proxy: " + message);
                }
            }
        }
    }
}

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
package net.bytebuddy.utility.dispatcher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.utility.GraalImageCode;
import net.bytebuddy.utility.Invoker;
import net.bytebuddy.utility.MethodComparator;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * A dispatcher for creating a proxy that invokes methods of a type that is possibly unknown on the current VM. Dispatchers do not
 * use any of Byte Buddy's regular infrastructure, to avoid bootstrapping issues as these dispatchers are used by Byte Buddy itself.
 * </p>
 * <p>
 * By default, this dispatcher uses the Java {@link Proxy} for creating dispatchers. By setting {@code net.bytebuddy.generate} to
 * {@code true}, Byte Buddy can generate proxies manually as byte code to mostly avoid reflection and boxing of arguments as arrays.
 * </p>
 * <p>
 * If a security manager is active, the <i>net.bytebuddy.createJavaDispatcher</i> runtime permission is required. Any dispatching
 * will be executed from a separate class loader and an unnamed module but with the {@link java.security.ProtectionDomain} of
 * the {@link JavaDispatcher} class. It is not permitted to invoke methods of the {@code java.security.AccessController} class or
 * to resolve a {@code java.lang.invoke.MethodHandle$Lookup}.
 * </p>
 *
 * @param <T> The resolved type.
 */
@HashCodeAndEqualsPlugin.Enhance
public class JavaDispatcher<T> implements PrivilegedAction<T> {

    /**
     * A property to determine, that if {@code true}, dispatcher classes will be generated natively and not by using a {@link Proxy}.
     */
    public static final String GENERATE_PROPERTY = "net.bytebuddy.generate";

    /**
     * If {@code true}, dispatcher classes will be generated natively and not by using a {@link Proxy}.
     */
    private static final boolean GENERATE = Boolean.parseBoolean(doPrivileged(new GetSystemPropertyAction(GENERATE_PROPERTY)));

    /**
     * A resolver to assure that a type's package and module are exported to the created class loader.
     * This should normally always be the case, but if another library is shading Byte Buddy or otherwise
     * manipulates the module graph, this might become necessary.
     */
    private static final DynamicClassLoader.Resolver RESOLVER = doPrivileged(DynamicClassLoader.Resolver.CreationAction.INSTANCE);

    /**
     * Contains an invoker that makes sure that reflective dispatchers make invocations from an isolated {@link ClassLoader} and
     * not from within Byte Buddy's context. This way, no privilege context can be leaked by accident.
     */
    private static final Invoker INVOKER = doPrivileged(new InvokerCreationAction());

    /**
     * The proxy type.
     */
    private final Class<T> proxy;

    /**
     * The class loader to resolve the proxied type from or {@code null} if the bootstrap loader should be used.
     */
    @MaybeNull
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
    private final ClassLoader classLoader;

    /**
     * {@code true} if a proxy class should be manually generated.
     */
    private final boolean generate;

    /**
     * Creates a new dispatcher.
     *
     * @param proxy       The proxy type.
     * @param classLoader The class loader to resolve the proxied type from or {@code null} if the bootstrap loader should be used.
     * @param generate    {@code true} if a proxy class should be manually generated.
     */
    protected JavaDispatcher(Class<T> proxy, @MaybeNull ClassLoader classLoader, boolean generate) {
        this.proxy = proxy;
        this.classLoader = classLoader;
        this.generate = generate;
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
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
    protected static <T> PrivilegedAction<T> of(Class<T> type, @MaybeNull ClassLoader classLoader) {
        return of(type, classLoader, GENERATE);
    }

    /**
     * Resolves an action for creating a dispatcher for the provided type.
     *
     * @param type        The type for which a dispatcher should be resolved.
     * @param classLoader The class loader to resolve the proxied type from.
     * @param generate    {@code true} if a proxy class should be manually generated.
     * @param <T>         The resolved type.
     * @return An action for creating an appropriate dispatcher.
     */
    protected static <T> PrivilegedAction<T> of(Class<T> type, @MaybeNull ClassLoader classLoader, boolean generate) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Expected an interface instead of " + type);
        } else if (!type.isAnnotationPresent(Proxied.class)) {
            throw new IllegalArgumentException("Expected " + type.getName() + " to be annotated with " + Proxied.class.getName());
        } else if (type.getAnnotation(Proxied.class).value().startsWith("java.security.")) {
            throw new IllegalArgumentException("Classes related to Java security cannot be proxied: " + type.getName());
        } else {
            return new JavaDispatcher<T>(type, classLoader, generate);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T run() {
        try {
            Object securityManager = System.class.getMethod("getSecurityManager").invoke(null);
            if (securityManager != null) {
                Class.forName("java.lang.SecurityManager")
                        .getMethod("checkPermission", Permission.class)
                        .invoke(securityManager, new RuntimePermission("net.bytebuddy.createJavaDispatcher"));
            }
        } catch (NoSuchMethodException ignored) {
            /* security manager not available on current VM */
        } catch (ClassNotFoundException ignored) {
            /* security manager not available on current VM */
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getTargetException();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new IllegalStateException("Failed to assert access rights using security manager", cause);
            }
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Failed to access security manager", exception);
        }
        Map<Method, Dispatcher> dispatchers = generate
                ? new LinkedHashMap<Method, Dispatcher>()
                : new HashMap<Method, Dispatcher>();
        boolean defaults = proxy.isAnnotationPresent(Defaults.class);
        String name = proxy.getAnnotation(Proxied.class).value();
        Class<?> target;
        try {
            target = Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException exception) {
            for (Method method : generate
                    ? GraalImageCode.getCurrent().sorted(proxy.getMethods(), MethodComparator.INSTANCE)
                    : proxy.getMethods()) {
                if (method.getDeclaringClass() == Object.class) {
                    continue;
                }
                if (method.isAnnotationPresent(Instance.class)) {
                    if (method.getParameterTypes().length != 1 || method.getParameterTypes()[0].isPrimitive() || method.getParameterTypes()[0].isArray()) {
                        throw new IllegalStateException("Instance check requires a single regular-typed argument: " + method);
                    } else if (method.getReturnType() != boolean.class) {
                        throw new IllegalStateException("Instance check requires a boolean return type: " + method);
                    } else {
                        dispatchers.put(method, Dispatcher.ForDefaultValue.BOOLEAN);
                    }
                } else {
                    dispatchers.put(method, defaults || method.isAnnotationPresent(Defaults.class)
                            ? Dispatcher.ForDefaultValue.of(method.getReturnType())
                            : new Dispatcher.ForUnresolvedMethod("Type not available on current VM: " + exception.getMessage()));
                }
            }
            if (generate) {
                return (T) DynamicClassLoader.proxy(proxy, dispatchers);
            } else {
                return (T) Proxy.newProxyInstance(proxy.getClassLoader(),
                        new Class<?>[]{proxy},
                        new ProxiedInvocationHandler(name, dispatchers));
            }
        }
        boolean generate = this.generate;
        for (Method method : generate
                ? GraalImageCode.getCurrent().sorted(proxy.getMethods(), MethodComparator.INSTANCE)
                : proxy.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (method.isAnnotationPresent(Instance.class)) {
                if (method.getParameterTypes().length != 1 || !method.getParameterTypes()[0].isAssignableFrom(target)) {
                    throw new IllegalStateException("Instance check requires a single regular-typed argument: " + method);
                } else if (method.getReturnType() != boolean.class) {
                    throw new IllegalStateException("Instance check requires a boolean return type: " + method);
                } else {
                    dispatchers.put(method, new Dispatcher.ForInstanceCheck(target));
                }
            } else if (method.isAnnotationPresent(Container.class)) {
                if (method.getParameterTypes().length != 1 || method.getParameterTypes()[0] != int.class) {
                    throw new IllegalStateException("Container creation requires a single int-typed argument: " + method);
                } else if (!method.getReturnType().isArray() || !method.getReturnType().getComponentType().isAssignableFrom(target)) {
                    throw new IllegalStateException("Container creation requires an assignable array as return value: " + method);
                } else {
                    dispatchers.put(method, new Dispatcher.ForContainerCreation(target));
                }
            } else if (target.getName().equals("java.lang.invoke.MethodHandles") && method.getName().equals("lookup")) {
                throw new UnsupportedOperationException("Cannot resolve Byte Buddy lookup via dispatcher");
            } else {
                try {
                    Class<?>[] parameterType = method.getParameterTypes();
                    int offset;
                    if (method.isAnnotationPresent(IsStatic.class) || method.isAnnotationPresent(IsConstructor.class)) {
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
                    if (method.isAnnotationPresent(IsConstructor.class)) {
                        Constructor<?> resolved = target.getConstructor(parameterType);
                        if (!method.getReturnType().isAssignableFrom(target)) {
                            throw new IllegalStateException("Cannot assign " + resolved.getDeclaringClass().getName() + " to " + method);
                        }
                        if ((resolved.getModifiers() & Opcodes.ACC_PUBLIC) == 0 || (target.getModifiers() & Opcodes.ACC_PUBLIC) == 0) {
                            resolved.setAccessible(true);
                            generate = false;
                        }
                        dispatchers.put(method, new Dispatcher.ForConstructor(resolved));
                    } else {
                        Proxied proxied = method.getAnnotation(Proxied.class);
                        Method resolved = target.getMethod(proxied == null ? method.getName() : proxied.value(), parameterType);
                        if (!method.getReturnType().isAssignableFrom(resolved.getReturnType())) {
                            throw new IllegalStateException("Cannot assign " + resolved.getReturnType().getName() + " to " + method);
                        }
                        exceptions:
                        for (Class<?> type : resolved.getExceptionTypes()) {
                            if (RuntimeException.class.isAssignableFrom(type) || Error.class.isAssignableFrom(type)) {
                                continue;
                            }
                            for (Class<?> exception : method.getExceptionTypes()) {
                                if (exception.isAssignableFrom(type)) {
                                    continue exceptions;
                                }
                            }
                            throw new IllegalStateException("Resolved method for " + method + " throws undeclared checked exception " + type.getName());
                        }
                        if ((resolved.getModifiers() & Opcodes.ACC_PUBLIC) == 0 || (resolved.getDeclaringClass().getModifiers() & Opcodes.ACC_PUBLIC) == 0) {
                            resolved.setAccessible(true);
                            generate = false;
                        }
                        if (Modifier.isStatic(resolved.getModifiers())) {
                            if (!method.isAnnotationPresent(IsStatic.class)) {
                                throw new IllegalStateException("Resolved method for " + method + " was expected to be static: " + resolved);
                            }
                            dispatchers.put(method, new Dispatcher.ForStaticMethod(resolved));
                        } else {
                            if (method.isAnnotationPresent(IsStatic.class)) {
                                throw new IllegalStateException("Resolved method for " + method + " was expected to be virtual: " + resolved);
                            }
                            dispatchers.put(method, new Dispatcher.ForNonStaticMethod(resolved));
                        }
                    }
                } catch (ClassNotFoundException exception) {
                    dispatchers.put(method, defaults || method.isAnnotationPresent(Defaults.class)
                            ? Dispatcher.ForDefaultValue.of(method.getReturnType())
                            : new Dispatcher.ForUnresolvedMethod("Class not available on current VM: " + exception.getMessage()));
                } catch (NoSuchMethodException exception) {
                    dispatchers.put(method, defaults || method.isAnnotationPresent(Defaults.class)
                            ? Dispatcher.ForDefaultValue.of(method.getReturnType())
                            : new Dispatcher.ForUnresolvedMethod("Method not available on current VM: " + exception.getMessage()));
                } catch (Throwable throwable) {
                    dispatchers.put(method, new Dispatcher.ForUnresolvedMethod("Unexpected error: " + throwable.getMessage()));
                }
            }
        }
        if (generate) {
            return (T) DynamicClassLoader.proxy(proxy, dispatchers);
        } else {
            return (T) Proxy.newProxyInstance(proxy.getClassLoader(),
                    new Class<?>[]{proxy},
                    new ProxiedInvocationHandler(target.getName(), dispatchers));
        }
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
    public @interface IsStatic {
        /* empty */
    }

    /**
     * Indicates that a proxied method is a constructor.
     */
    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface IsConstructor {
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
     * A privileged action for creating an {@link Invoker}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    private static class InvokerCreationAction implements PrivilegedAction<Invoker> {

        /**
         * {@inheritDoc}
         */
        public Invoker run() {
            return DynamicClassLoader.invoker();
        }
    }

    /**
     * An {@link Invoker} that uses Byte Buddy's invocation context to use if dynamic class loading is not supported, for example on Android,
     * and that do not use secured contexts, where this security measure is obsolete to begin with.
     */
    @HashCodeAndEqualsPlugin.Enhance
    private static class DirectInvoker implements Invoker {

        /**
         * {@inheritDoc}
         */
        public Object newInstance(Constructor<?> constructor, Object[] argument) throws InstantiationException, IllegalAccessException, InvocationTargetException {
            return constructor.newInstance(argument);
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(Method method, @MaybeNull Object instance, @MaybeNull Object[] argument) throws IllegalAccessException, InvocationTargetException {
            return method.invoke(instance, argument);
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
        @MaybeNull
        Object invoke(Object[] argument) throws Throwable;

        /**
         * Implements this dispatcher in a generated proxy.
         *
         * @param methodVisitor The method visitor to implement the method with.
         * @param method        The method being implemented.
         * @return The maximal size of the operand stack.
         */
        int apply(MethodVisitor methodVisitor, Method method);

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
            public Object invoke(Object[] argument) {
                return target.isInstance(argument[0]);
            }

            /**
             * {@inheritDoc}
             */
            public int apply(MethodVisitor methodVisitor, Method method) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
                methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(target));
                methodVisitor.visitInsn(Opcodes.IRETURN);
                return 1;
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
            public Object invoke(Object[] argument) {
                return Array.newInstance(target, (Integer) argument[0]);
            }

            /**
             * {@inheritDoc}
             */
            public int apply(MethodVisitor methodVisitor, Method method) {
                methodVisitor.visitVarInsn(Opcodes.ILOAD, 1);
                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(target));
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return 1;
            }
        }

        /**
         * A dispatcher that returns a fixed value.
         */
        enum ForDefaultValue implements Dispatcher {

            /**
             * A dispatcher for a {@code void} type.
             */
            VOID(null, Opcodes.NOP, Opcodes.RETURN, 0),

            /**
             * A dispatcher for a {@code boolean} type.
             */
            BOOLEAN(false, Opcodes.ICONST_0, Opcodes.IRETURN, 1),

            /**
             * A dispatcher for a {@code boolean} type that returns {@code true}.
             */
            BOOLEAN_REVERSE(true, Opcodes.ICONST_1, Opcodes.IRETURN, 1),

            /**
             * A dispatcher for a {@code byte} type.
             */
            BYTE((byte) 0, Opcodes.ICONST_0, Opcodes.IRETURN, 1),

            /**
             * A dispatcher for a {@code short} type.
             */
            SHORT((short) 0, Opcodes.ICONST_0, Opcodes.IRETURN, 1),

            /**
             * A dispatcher for a {@code char} type.
             */
            CHARACTER((char) 0, Opcodes.ICONST_0, Opcodes.IRETURN, 1),

            /**
             * A dispatcher for an {@code int} type.
             */
            INTEGER(0, Opcodes.ICONST_0, Opcodes.IRETURN, 1),

            /**
             * A dispatcher for a {@code long} type.
             */
            LONG(0L, Opcodes.LCONST_0, Opcodes.LRETURN, 2),

            /**
             * A dispatcher for a {@code float} type.
             */
            FLOAT(0f, Opcodes.FCONST_0, Opcodes.FRETURN, 1),

            /**
             * A dispatcher for a {@code double} type.
             */
            DOUBLE(0d, Opcodes.DCONST_0, Opcodes.DRETURN, 2),

            /**
             * A dispatcher for a reference type.
             */
            REFERENCE(null, Opcodes.ACONST_NULL, Opcodes.ARETURN, 1);

            /**
             * The default value.
             */
            @MaybeNull
            private final Object value;

            /**
             * The opcode to load the default value.
             */
            private final int load;

            /**
             * The opcode to return the default value.
             */
            private final int returned;

            /**
             * The operand stack size of default value.
             */
            private final int size;

            /**
             * Creates a new default value dispatcher.
             *
             * @param value    The default value.
             * @param load     The opcode to load the default value.
             * @param returned The opcode to return the default value.
             * @param size     The operand stack size of default value.
             */
            ForDefaultValue(@MaybeNull Object value, int load, int returned, int size) {
                this.value = value;
                this.load = load;
                this.returned = returned;
                this.size = size;
            }

            /**
             * Resolves a fixed value for a given type.
             *
             * @param type The type to resolve.
             * @return An appropriate dispatcher.
             */
            protected static Dispatcher of(Class<?> type) {
                if (type == void.class) {
                    return VOID;
                } else if (type == boolean.class) {
                    return BOOLEAN;
                } else if (type == byte.class) {
                    return BYTE;
                } else if (type == short.class) {
                    return SHORT;
                } else if (type == char.class) {
                    return CHARACTER;
                } else if (type == int.class) {
                    return INTEGER;
                } else if (type == long.class) {
                    return LONG;
                } else if (type == float.class) {
                    return FLOAT;
                } else if (type == double.class) {
                    return DOUBLE;
                } else if (type.isArray()) {
                    if (type.getComponentType() == boolean.class) {
                        return OfPrimitiveArray.BOOLEAN;
                    } else if (type.getComponentType() == byte.class) {
                        return OfPrimitiveArray.BYTE;
                    } else if (type.getComponentType() == short.class) {
                        return OfPrimitiveArray.SHORT;
                    } else if (type.getComponentType() == char.class) {
                        return OfPrimitiveArray.CHARACTER;
                    } else if (type.getComponentType() == int.class) {
                        return OfPrimitiveArray.INTEGER;
                    } else if (type.getComponentType() == long.class) {
                        return OfPrimitiveArray.LONG;
                    } else if (type.getComponentType() == float.class) {
                        return OfPrimitiveArray.FLOAT;
                    } else if (type.getComponentType() == double.class) {
                        return OfPrimitiveArray.DOUBLE;
                    } else {
                        return OfNonPrimitiveArray.of(type.getComponentType());
                    }
                } else {
                    return REFERENCE;
                }
            }

            /**
             * {@inheritDoc}
             */
            @MaybeNull
            public Object invoke(Object[] argument) {
                return value;
            }

            /**
             * {@inheritDoc}
             */
            public int apply(MethodVisitor methodVisitor, Method method) {
                if (load != Opcodes.NOP) {
                    methodVisitor.visitInsn(load);
                }
                methodVisitor.visitInsn(returned);
                return size;
            }

            /**
             * A dispatcher for returning a default value for a primitive array.
             */
            protected enum OfPrimitiveArray implements Dispatcher {

                /**
                 * A dispatcher for a {@code boolean} array.
                 */
                BOOLEAN(new boolean[0], Opcodes.T_BOOLEAN),

                /**
                 * A dispatcher for a {@code byte} array.
                 */
                BYTE(new byte[0], Opcodes.T_BYTE),

                /**
                 * A dispatcher for a {@code short} array.
                 */
                SHORT(new short[0], Opcodes.T_SHORT),

                /**
                 * A dispatcher for a {@code char} array.
                 */
                CHARACTER(new char[0], Opcodes.T_CHAR),

                /**
                 * A dispatcher for a {@code int} array.
                 */
                INTEGER(new int[0], Opcodes.T_INT),

                /**
                 * A dispatcher for a {@code long} array.
                 */
                LONG(new long[0], Opcodes.T_LONG),

                /**
                 * A dispatcher for a {@code float} array.
                 */
                FLOAT(new float[0], Opcodes.T_FLOAT),

                /**
                 * A dispatcher for a {@code double} array.
                 */
                DOUBLE(new double[0], Opcodes.T_DOUBLE);

                /**
                 * The default value.
                 */
                private final Object value;

                /**
                 * The operand for creating an array of the represented type.
                 */
                private final int operand;

                /**
                 * Creates a new dispatcher for a primitive array.
                 *
                 * @param value   The default value.
                 * @param operand The operand for creating an array of the represented type.
                 */
                OfPrimitiveArray(Object value, int operand) {
                    this.value = value;
                    this.operand = operand;
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) {
                    return value;
                }

                /**
                 * {@inheritDoc}
                 */
                public int apply(MethodVisitor methodVisitor, Method method) {
                    methodVisitor.visitInsn(Opcodes.ICONST_0);
                    methodVisitor.visitIntInsn(Opcodes.NEWARRAY, operand);
                    methodVisitor.visitInsn(Opcodes.ARETURN);
                    return 1;
                }
            }

            /**
             * A dispatcher for a non-primitive array type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class OfNonPrimitiveArray implements Dispatcher {

                /**
                 * The default value.
                 */
                @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.IGNORE)
                private final Object value;

                /**
                 * The represented component type.
                 */
                private final Class<?> componentType;

                /**
                 * Creates a new dispatcher for the default value of a non-primitive array.
                 *
                 * @param value         The default value.
                 * @param componentType The represented component type.
                 */
                protected OfNonPrimitiveArray(Object value, Class<?> componentType) {
                    this.value = value;
                    this.componentType = componentType;
                }

                /**
                 * Creates a new dispatcher.
                 *
                 * @param componentType The represented component type.
                 * @return A dispatcher for the supplied component type.
                 */
                protected static Dispatcher of(Class<?> componentType) {
                    return new OfNonPrimitiveArray(Array.newInstance(componentType, 0), componentType);
                }

                /**
                 * {@inheritDoc}
                 */
                public Object invoke(Object[] argument) {
                    return value;
                }

                /**
                 * {@inheritDoc}
                 */
                public int apply(MethodVisitor methodVisitor, Method method) {
                    methodVisitor.visitInsn(Opcodes.ICONST_0);
                    methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(componentType));
                    methodVisitor.visitInsn(Opcodes.ARETURN);
                    return 1;
                }
            }
        }

        /**
         * A dispatcher for invoking a constructor.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForConstructor implements Dispatcher {

            /**
             * The proxied constructor.
             */
            private final Constructor<?> constructor;

            /**
             * Creates a dispatcher for invoking a constructor.
             *
             * @param constructor The proxied constructor.
             */
            protected ForConstructor(Constructor<?> constructor) {
                this.constructor = constructor;
            }

            /**
             * {@inheritDoc}
             */
            public Object invoke(Object[] argument) throws Throwable {
                return INVOKER.newInstance(constructor, argument);
            }

            /**
             * {@inheritDoc}
             */
            public int apply(MethodVisitor methodVisitor, Method method) {
                Class<?>[] source = method.getParameterTypes(), target = constructor.getParameterTypes();
                methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(constructor.getDeclaringClass()));
                methodVisitor.visitInsn(Opcodes.DUP);
                int offset = 1;
                for (int index = 0; index < source.length; index++) {
                    Type type = Type.getType(source[index]);
                    methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                    if (source[index] != target[index]) {
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(target[index]));
                    }
                    offset += type.getSize();
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                        Type.getInternalName(constructor.getDeclaringClass()),
                        MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                        Type.getConstructorDescriptor(constructor),
                        false);
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return offset + 1;
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
            @MaybeNull
            public Object invoke(Object[] argument) throws Throwable {
                return INVOKER.invoke(method, null, argument);
            }

            /**
             * {@inheritDoc}
             */
            public int apply(MethodVisitor methodVisitor, Method method) {
                Class<?>[] source = method.getParameterTypes(), target = this.method.getParameterTypes();
                int offset = 1;
                for (int index = 0; index < source.length; index++) {
                    Type type = Type.getType(source[index]);
                    methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                    if (source[index] != target[index]) {
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(target[index]));
                    }
                    offset += type.getSize();
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Type.getInternalName(this.method.getDeclaringClass()),
                        this.method.getName(),
                        Type.getMethodDescriptor(this.method),
                        this.method.getDeclaringClass().isInterface());
                methodVisitor.visitInsn(Type.getReturnType(this.method).getOpcode(Opcodes.IRETURN));
                return Math.max(offset - 1, Type.getReturnType(this.method).getSize());
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
                return INVOKER.invoke(method, argument[0], reduced);
            }

            /**
             * {@inheritDoc}
             */
            public int apply(MethodVisitor methodVisitor, Method method) {
                Class<?>[] source = method.getParameterTypes(), target = this.method.getParameterTypes();
                int offset = 1;
                for (int index = 0; index < source.length; index++) {
                    Type type = Type.getType(source[index]);
                    methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                    if (source[index] != (index == 0 ? this.method.getDeclaringClass() : target[index - 1])) {
                        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(index == 0
                                ? this.method.getDeclaringClass()
                                : target[index - 1]));
                    }
                    offset += type.getSize();
                }
                methodVisitor.visitMethodInsn(this.method.getDeclaringClass().isInterface() ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                        Type.getInternalName(this.method.getDeclaringClass()),
                        this.method.getName(),
                        Type.getMethodDescriptor(this.method),
                        this.method.getDeclaringClass().isInterface());
                methodVisitor.visitInsn(Type.getReturnType(this.method).getOpcode(Opcodes.IRETURN));
                return Math.max(offset - 1, Type.getReturnType(this.method).getSize());
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

            /**
             * {@inheritDoc}
             */
            public int apply(MethodVisitor methodVisitor, Method method) {
                methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IllegalStateException.class));
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(message);
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                        Type.getInternalName(IllegalStateException.class),
                        MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class)),
                        false);
                methodVisitor.visitInsn(Opcodes.ATHROW);
                return 3;
            }
        }
    }

    /**
     * An invocation handler that invokes given dispatchers.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ProxiedInvocationHandler implements InvocationHandler {

        /**
         * Indicates that an invocation handler does not provide any arguments.
         */
        private static final Object[] NO_ARGUMENTS = new Object[0];

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
        @MaybeNull
        public Object invoke(Object proxy, Method method, @MaybeNull Object[] argument) throws Throwable {
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
                        return dispatcher.invoke(argument == null
                                ? NO_ARGUMENTS
                                : argument);
                    }
                } catch (InvocationTargetException exception) {
                    throw exception.getTargetException();
                }
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                for (Class<?> type : method.getExceptionTypes()) {
                    if (type.isInstance(throwable)) {
                        throw throwable;
                    }
                }
                throw new IllegalStateException("Failed to invoke proxy for " + method, throwable);
            }
        }
    }

    /**
     * A class loader for loading synthetic classes for implementing a {@link JavaDispatcher}.
     */
    protected static class DynamicClassLoader extends ClassLoader {

        /**
         * The dump folder that is defined by the {@link TypeWriter#DUMP_PROPERTY} property or {@code null} if not set.
         */
        @MaybeNull
        private static final String DUMP_FOLDER;

        /**
         * Indicates that a constructor does not declare any parameters.
         */
        private static final Class<?>[] NO_PARAMETER = new Class<?>[0];

        /**
         * Indicates that a constructor does not require any arguments.
         */
        private static final Object[] NO_ARGUMENT = new Object[0];

        /*
         * Resolves the currently set dump folder.
         */
        static {
            String dumpFolder;
            try {
                dumpFolder = doPrivileged(new GetSystemPropertyAction(TypeWriter.DUMP_PROPERTY));
            } catch (Throwable ignored) {
                dumpFolder = null;
            }
            DUMP_FOLDER = dumpFolder;
        }

        /**
         * Creates a new dynamic class loader.
         *
         * @param target The proxied type.
         */
        protected DynamicClassLoader(Class<?> target) {
            super(target.getClassLoader());
            RESOLVER.accept(this, target);
        }

        /**
         * Creates a new proxied type.
         *
         * @param proxy       The proxy type interface.
         * @param dispatchers The dispatchers to implement.
         * @return An instance of the proxied type.
         */
        @SuppressFBWarnings(value = {"REC_CATCH_EXCEPTION", "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"}, justification = "Expected internal invocation.")
        protected static Object proxy(Class<?> proxy, Map<Method, Dispatcher> dispatchers) {
            ClassWriter classWriter = new ClassWriter(AsmVisitorWrapper.NO_FLAGS);
            classWriter.visit(ClassFileVersion.JAVA_V5.getMinorMajorVersion(),
                    Opcodes.ACC_PUBLIC,
                    Type.getInternalName(proxy) + "$Proxy",
                    null,
                    Type.getInternalName(Object.class),
                    new String[]{Type.getInternalName(proxy)});
            for (Map.Entry<Method, Dispatcher> entry : dispatchers.entrySet()) {
                Class<?>[] exceptionType = entry.getKey().getExceptionTypes();
                String[] exceptionTypeName = new String[exceptionType.length];
                for (int index = 0; index < exceptionType.length; index++) {
                    exceptionTypeName[index] = Type.getInternalName(exceptionType[index]);
                }
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC,
                        entry.getKey().getName(),
                        Type.getMethodDescriptor(entry.getKey()),
                        null,
                        exceptionTypeName);
                methodVisitor.visitCode();
                int offset = (entry.getKey().getModifiers() & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
                for (Class<?> type : entry.getKey().getParameterTypes()) {
                    offset += Type.getType(type).getSize();
                }
                methodVisitor.visitMaxs(entry.getValue().apply(methodVisitor, entry.getKey()), offset);
                methodVisitor.visitEnd();
            }
            MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC,
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE),
                    null,
                    null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    Type.getInternalName(Object.class),
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE),
                    false);
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
            classWriter.visitEnd();
            byte[] binaryRepresentation = classWriter.toByteArray();
            if (DUMP_FOLDER != null) {
                try {
                    OutputStream outputStream = new FileOutputStream(new File(DUMP_FOLDER, proxy.getName() + "$Proxy.class"));
                    try {
                        outputStream.write(binaryRepresentation);
                    } finally {
                        outputStream.close();
                    }
                } catch (Throwable ignored) {
                    /* do nothing */
                }
            }
            try {
                return new DynamicClassLoader(proxy)
                        .defineClass(proxy.getName() + "$Proxy",
                                binaryRepresentation,
                                0,
                                binaryRepresentation.length,
                                JavaDispatcher.class.getProtectionDomain())
                        .getConstructor(NO_PARAMETER)
                        .newInstance(NO_ARGUMENT);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create proxy for " + proxy.getName(), exception);
            }
        }

        /**
         * Resolves a {@link Invoker} for a separate class loader.
         *
         * @return The created {@link Invoker}.
         */
        @SuppressFBWarnings(value = {"REC_CATCH_EXCEPTION", "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"}, justification = "Expected internal invocation.")
        protected static Invoker invoker() {
            ClassWriter classWriter = new ClassWriter(AsmVisitorWrapper.NO_FLAGS);
            classWriter.visit(ClassFileVersion.JAVA_V5.getMinorMajorVersion(),
                    Opcodes.ACC_PUBLIC,
                    Type.getInternalName(Invoker.class) + "$Dispatcher",
                    null,
                    Type.getInternalName(Object.class),
                    new String[]{Type.getInternalName(Invoker.class)});
            for (Method method : GraalImageCode.getCurrent().sorted(Invoker.class.getMethods(), MethodComparator.INSTANCE)) {
                Class<?>[] exceptionType = method.getExceptionTypes();
                String[] exceptionTypeName = new String[exceptionType.length];
                for (int index = 0; index < exceptionType.length; index++) {
                    exceptionTypeName[index] = Type.getInternalName(exceptionType[index]);
                }
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC,
                        method.getName(),
                        Type.getMethodDescriptor(method),
                        null,
                        exceptionTypeName);
                methodVisitor.visitCode();
                int offset = 1;
                Type[] parameter = new Type[method.getParameterTypes().length - 1];
                for (int index = 0; index < method.getParameterTypes().length; index++) {
                    Type type = Type.getType(method.getParameterTypes()[index]);
                    if (index > 0) {
                        parameter[index - 1] = type;
                    }
                    methodVisitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                    offset += type.getSize();
                }
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        Type.getInternalName(method.getParameterTypes()[0]),
                        method.getName(),
                        Type.getMethodDescriptor(Type.getReturnType(method), parameter),
                        false);
                methodVisitor.visitInsn(Type.getReturnType(method).getOpcode(Opcodes.IRETURN));
                methodVisitor.visitMaxs(Math.max(offset - 1, Type.getReturnType(method).getSize()), offset);
                methodVisitor.visitEnd();
            }
            MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC,
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE),
                    null,
                    null);
            methodVisitor.visitCode();
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,
                    Type.getInternalName(Object.class),
                    MethodDescription.CONSTRUCTOR_INTERNAL_NAME,
                    Type.getMethodDescriptor(Type.VOID_TYPE),
                    false);
            methodVisitor.visitInsn(Opcodes.RETURN);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
            classWriter.visitEnd();
            byte[] binaryRepresentation = classWriter.toByteArray();
            try {
                String dumpFolder = System.getProperty(TypeWriter.DUMP_PROPERTY);
                if (dumpFolder != null) {
                    OutputStream outputStream = new FileOutputStream(new File(dumpFolder, Invoker.class.getName() + "$Dispatcher.class"));
                    try {
                        outputStream.write(binaryRepresentation);
                    } finally {
                        outputStream.close();
                    }
                }
            } catch (Throwable ignored) {
                /* do nothing */
            }
            try {
                return (Invoker) new DynamicClassLoader(Invoker.class)
                        .defineClass(Invoker.class.getName() + "$Dispatcher",
                                binaryRepresentation,
                                0,
                                binaryRepresentation.length,
                                JavaDispatcher.class.getProtectionDomain())
                        .getConstructor(NO_PARAMETER)
                        .newInstance(NO_ARGUMENT);
            } catch (UnsupportedOperationException ignored) {
                return new DirectInvoker();
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to create invoker for " + Invoker.class.getName(), exception);
            }
        }

        /**
         * A resolver to make adjustments that are possibly necessary to withhold module graph guarantees.
         */
        protected interface Resolver {

            /**
             * Adjusts a module graph if necessary.
             *
             * @param classLoader The class loader to adjust.
             * @param target      The targeted class for which a proxy is created.
             */
            void accept(@MaybeNull ClassLoader classLoader, Class<?> target);

            /**
             * An action to create a resolver.
             */
            enum CreationAction implements PrivilegedAction<Resolver> {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback.")
                public Resolver run() {
                    try {
                        Class<?> module = Class.forName("java.lang.Module", false, null);
                        return new ForModuleSystem(Class.class.getMethod("getModule"),
                                module.getMethod("isExported", String.class),
                                module.getMethod("addExports", String.class, module),
                                ClassLoader.class.getMethod("getUnnamedModule"));
                    } catch (Exception ignored) {
                        return NoOp.INSTANCE;
                    }
                }
            }

            /**
             * A non-operational resolver for VMs that do not support the module system.
             */
            enum NoOp implements Resolver {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public void accept(@MaybeNull ClassLoader classLoader, Class<?> target) {
                    /* do nothing */
                }
            }

            /**
             * A resolver for VMs that do support the module system.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForModuleSystem implements Resolver {

                /**
                 * The {@code java.lang.Class#getModule} method.
                 */
                private final Method getModule;

                /**
                 * The {@code java.lang.Module#isExported} method.
                 */
                private final Method isExported;

                /**
                 * The {@code java.lang.Module#addExports} method.
                 */
                private final Method addExports;

                /**
                 * The {@code java.lang.ClassLoader#getUnnamedModule} method.
                 */
                private final Method getUnnamedModule;

                /**
                 * Creates a new resolver for a VM that supports the module system.
                 *
                 * @param getModule        The {@code java.lang.Class#getModule} method.
                 * @param isExported       The {@code java.lang.Module#isExported} method.
                 * @param addExports       The {@code java.lang.Module#addExports} method.
                 * @param getUnnamedModule The {@code java.lang.ClassLoader#getUnnamedModule} method.
                 */
                protected ForModuleSystem(Method getModule,
                                          Method isExported,
                                          Method addExports,
                                          Method getUnnamedModule) {
                    this.getModule = getModule;
                    this.isExported = isExported;
                    this.addExports = addExports;
                    this.getUnnamedModule = getUnnamedModule;
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should always be wrapped for clarity.")
                public void accept(@MaybeNull ClassLoader classLoader, Class<?> target) {
                    Package location = target.getPackage();
                    if (location != null) {
                        try {
                            Object module = getModule.invoke(target);
                            if (!(Boolean) isExported.invoke(module, location.getName())) {
                                addExports.invoke(module, location.getName(), getUnnamedModule.invoke(classLoader));
                            }
                        } catch (Exception exception) {
                            throw new IllegalStateException("Failed to adjust module graph for dispatcher", exception);
                        }
                    }
                }
            }
        }
    }
}

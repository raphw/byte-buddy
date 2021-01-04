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
package net.bytebuddy.build;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.scaffold.inline.MethodNameTransformer;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * <p>
 * A plugin that allows for the application of Byte Buddy transformations during a build process. This plugin's
 * transformation is applied to any type matching this plugin's type matcher. Plugin types must be public,
 * non-abstract and must declare a public default constructor to work.
 * </p>
 * <p>
 * A plugin is always used within the scope of a single plugin engine application and is disposed after closing. It might be used
 * concurrently and must assure its own thread-safety if run outside of a {@link Plugin.Engine} or when using a parallel
 * {@link Plugin.Engine.Dispatcher}.
 * </p>
 */
public interface Plugin extends ElementMatcher<TypeDescription>, Closeable {

    /**
     * Applies this plugin.
     *
     * @param builder          The builder to use as a basis for the applied transformation.
     * @param typeDescription  The type being transformed.
     * @param classFileLocator A class file locator that can locate other types in the scope of the project.
     * @return The supplied builder with additional transformations registered.
     */
    DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator);

    /**
     * <p>
     * A plugin that applies a preprocessor, i.e. causes a plugin engine's execution to defer all plugin applications until all types were discovered.
     * </p>
     * <p>
     * <b>Important</b>: The registration of a single plugin with preprocessor causes the deferral of all plugins' application that are registered
     * with a particular plugin engine. This will reduce parallel application if a corresponding {@link Engine.Dispatcher} is used and will increase
     * the engine application's memory consumption. Any alternative application of a plugin outside of a {@link Plugin.Engine} might not be capable
     * of preprocessing where the discovery callback is not invoked.
     * </p>
     */
    interface WithPreprocessor extends Plugin {

        /**
         * Invoked upon the discovery of a type that is not explicitly ignored.
         *
         * @param typeDescription  The discovered type.
         * @param classFileLocator A class file locator that can locate other types in the scope of the project.
         */
        void onPreprocess(TypeDescription typeDescription, ClassFileLocator classFileLocator);
    }

    /**
     * A factory for providing a build plugin.
     */
    interface Factory {

        /**
         * Returns a plugin that can be used for a transformation and which is subsequently closed.
         *
         * @return The plugin to use for type transformations.
         */
        Plugin make();

        /**
         * A simple factory that returns a preconstructed plugin instance..
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Simple implements Factory {

            /**
             * The plugin to provide.
             */
            private final Plugin plugin;

            /**
             * Creates a simple plugin factory.
             *
             * @param plugin The plugin to provide.
             */
            public Simple(Plugin plugin) {
                this.plugin = plugin;
            }

            /**
             * {@inheritDoc}
             */
            public Plugin make() {
                return plugin;
            }
        }

        /**
         * A plugin factory that uses reflection for instantiating a plugin.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class UsingReflection implements Factory {

            /**
             * The plugin type.
             */
            private final Class<? extends Plugin> type;

            /**
             * A list of argument providers that can be used for instantiating the plugin.
             */
            private final List<ArgumentResolver> argumentResolvers;

            /**
             * Creates a plugin factory that uses reflection for creating a plugin.
             *
             * @param type The plugin type.
             */
            public UsingReflection(Class<? extends Plugin> type) {
                this(type, Collections.<ArgumentResolver>emptyList());
            }

            /**
             * Creates a plugin factory that uses reflection for creating a plugin.
             *
             * @param type              The plugin type.
             * @param argumentResolvers A list of argument providers that can be used for instantiating the plugin.
             */
            protected UsingReflection(Class<? extends Plugin> type, List<ArgumentResolver> argumentResolvers) {
                this.type = type;
                this.argumentResolvers = argumentResolvers;
            }

            /**
             * Appends the supplied argument resolvers.
             *
             * @param argumentResolver A list of argument providers that can be used for instantiating the plugin.
             * @return A new plugin factory that uses reflection for creating a plugin that also uses the supplied argument resolvers.
             */
            public UsingReflection with(ArgumentResolver... argumentResolver) {
                return with(Arrays.asList(argumentResolver));
            }

            /**
             * Appends the supplied argument resolvers.
             *
             * @param argumentResolvers A list of argument providers that can be used for instantiating the plugin.
             * @return A new plugin factory that uses reflection for creating a plugin that also uses the supplied argument resolvers.
             */
            public UsingReflection with(List<? extends ArgumentResolver> argumentResolvers) {
                return new UsingReflection(type, CompoundList.of(argumentResolvers, this.argumentResolvers));
            }

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            public Plugin make() {
                Instantiator instantiator = new Instantiator.Unresolved(type);
                candidates:
                for (Constructor<?> constructor : type.getConstructors()) {
                    if (!constructor.isSynthetic()) {
                        List<Object> arguments = new ArrayList<Object>(constructor.getParameterTypes().length);
                        int index = 0;
                        for (Class<?> type : constructor.getParameterTypes()) {
                            boolean resolved = false;
                            for (ArgumentResolver argumentResolver : argumentResolvers) {
                                ArgumentResolver.Resolution resolution = argumentResolver.resolve(index, type);
                                if (resolution.isResolved()) {
                                    arguments.add(resolution.getArgument());
                                    resolved = true;
                                    break;
                                }
                            }
                            if (resolved) {
                                index += 1;
                            } else {
                                break candidates;
                            }
                        }
                        instantiator = instantiator.replaceBy(new Instantiator.Resolved((Constructor<? extends Plugin>) constructor, arguments));
                    }
                }
                return instantiator.instantiate();
            }

            /**
             * An instantiator is responsible for invoking a plugin constructor reflectively.
             */
            protected interface Instantiator {

                /**
                 * Returns either this instantiator or the supplied instantiator, depending on the instances' states.
                 *
                 * @param instantiator The alternative instantiator.
                 * @return The dominant instantiator.
                 */
                Instantiator replaceBy(Resolved instantiator);

                /**
                 * Instantiates the represented plugin.
                 *
                 * @return The instantiated plugin.
                 */
                Plugin instantiate();

                /**
                 * An instantiator that is not resolved for creating an instance.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Unresolved implements Instantiator {

                    /**
                     * The type for which no constructor was yet resolved.
                     */
                    private final Class<? extends Plugin> type;

                    /**
                     * Creates a new unresolved constructor.
                     *
                     * @param type The type for which no constructor was yet resolved.
                     */
                    protected Unresolved(Class<? extends Plugin> type) {
                        this.type = type;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Instantiator replaceBy(Resolved instantiator) {
                        return instantiator;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Plugin instantiate() {
                        throw new IllegalStateException("No constructor available for " + type);
                    }
                }

                /**
                 * An instantiator that is resolved for a given constructor with arguments.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Resolved implements Instantiator {

                    /**
                     * The represented constructor.
                     */
                    private final Constructor<? extends Plugin> constructor;

                    /**
                     * The constructor arguments.
                     */
                    private final List<?> arguments;

                    /**
                     * Creates a new resolved constructor.
                     *
                     * @param constructor The represented constructor.
                     * @param arguments   The constructor arguments.
                     */
                    protected Resolved(Constructor<? extends Plugin> constructor, List<?> arguments) {
                        this.constructor = constructor;
                        this.arguments = arguments;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Instantiator replaceBy(Resolved instantiator) {
                        Priority left = constructor.getAnnotation(Priority.class), right = instantiator.constructor.getAnnotation(Priority.class);
                        int leftPriority = left == null ? Priority.DEFAULT : left.value(), rightPriority = right == null ? Priority.DEFAULT : right.value();
                        if (leftPriority > rightPriority) {
                            return this;
                        } else if (leftPriority < rightPriority) {
                            return instantiator;
                        } else {
                            throw new IllegalStateException("Ambiguous constructors " + constructor + " and " + instantiator.constructor);
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Plugin instantiate() {
                        try {
                            return constructor.newInstance(arguments.toArray(new Object[0]));
                        } catch (InstantiationException exception) {
                            throw new IllegalStateException("Failed to instantiate plugin via " + constructor, exception);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Failed to access " + constructor, exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Error during construction of" + constructor, exception.getCause());
                        }
                    }
                }
            }

            /**
             * Indicates that a constructor should be treated with a given priority if several constructors can be resolved.
             */
            @Documented
            @Target(ElementType.CONSTRUCTOR)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Priority {

                /**
                 * The default priority that is assumed for non-annotated constructors.
                 */
                int DEFAULT = 0;

                /**
                 * Indicates the priority of the annotated constructor.
                 *
                 * @return The priority of the annotated constructor.
                 */
                int value();
            }

            /**
             * Allows to resolve arguments for a {@link Plugin} constructor.
             */
            public interface ArgumentResolver {

                /**
                 * Attempts the resolution of an argument for a given parameter.
                 *
                 * @param index The parameter's index.
                 * @param type  The parameter's type.
                 * @return The resolution for the parameter.
                 */
                Resolution resolve(int index, Class<?> type);

                /**
                 * A resolution provided by an argument provider.
                 */
                interface Resolution {

                    /**
                     * Returns {@code true} if the represented argument is resolved successfully.
                     *
                     * @return {@code true} if the represented argument is resolved successfully.
                     */
                    boolean isResolved();

                    /**
                     * Returns the resolved argument if the resolution was successful.
                     *
                     * @return The resolved argument if the resolution was successful.
                     */
                    Object getArgument();

                    /**
                     * Represents an unresolved argument resolution.
                     */
                    enum Unresolved implements Resolution {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * {@inheritDoc}
                         */
                        public boolean isResolved() {
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Object getArgument() {
                            throw new IllegalStateException("Cannot get the argument for an unresolved parameter");
                        }
                    }

                    /**
                     * Represents a resolved argument resolution.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Resolved implements Resolution {

                        /**
                         * The resolved argument which might be {@code null}.
                         */
                        @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                        private final Object argument;

                        /**
                         * Creates a resolved argument resolution.
                         *
                         * @param argument The resolved argument which might be {@code null}.
                         */
                        public Resolved(Object argument) {
                            this.argument = argument;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public boolean isResolved() {
                            return true;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Object getArgument() {
                            return argument;
                        }
                    }
                }

                /**
                 * An argument resolver that never resolves an argument.
                 */
                enum NoOp implements ArgumentResolver {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(int index, Class<?> type) {
                        return Resolution.Unresolved.INSTANCE;
                    }
                }

                /**
                 * An argument resolver that resolves parameters for a given type.
                 *
                 * @param <T> The type being resolved.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForType<T> implements ArgumentResolver {

                    /**
                     * The type being resolved.
                     */
                    private final Class<? extends T> type;

                    /**
                     * The instance to resolve for the represented type.
                     */
                    private final T value;

                    /**
                     * Creates a new argument resolver for a given type.
                     *
                     * @param type  The type being resolved.
                     * @param value The instance to resolve for the represented type.
                     */
                    protected ForType(Class<? extends T> type, T value) {
                        this.type = type;
                        this.value = value;
                    }

                    /**
                     * Creates an argument resolver for a given type.
                     *
                     * @param type  The type being resolved.
                     * @param value The instance to resolve for the represented type.
                     * @param <S>   The type being resolved.
                     * @return An appropriate argument resolver.
                     */
                    public static <S> ArgumentResolver of(Class<? extends S> type, S value) {
                        return new ForType<S>(type, value);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(int index, Class<?> type) {
                        return type == this.type
                                ? new Resolution.Resolved(value)
                                : Resolution.Unresolved.INSTANCE;
                    }
                }

                /**
                 * An argument resolver that resolves an argument for a specific parameter index.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForIndex implements ArgumentResolver {

                    /**
                     * A mapping of primitive types to their wrapper types.
                     */
                    private static final Map<Class<?>, Class<?>> WRAPPER_TYPES;

                    /*
                     * Creates the primitive to wrapper type mapping.
                     */
                    static {
                        WRAPPER_TYPES = new HashMap<Class<?>, Class<?>>();
                        WRAPPER_TYPES.put(boolean.class, Boolean.class);
                        WRAPPER_TYPES.put(byte.class, Byte.class);
                        WRAPPER_TYPES.put(short.class, Short.class);
                        WRAPPER_TYPES.put(char.class, Character.class);
                        WRAPPER_TYPES.put(int.class, Integer.class);
                        WRAPPER_TYPES.put(long.class, Long.class);
                        WRAPPER_TYPES.put(float.class, Float.class);
                        WRAPPER_TYPES.put(double.class, Double.class);
                    }

                    /**
                     * The index of the parameter to resolve.
                     */
                    private final int index;

                    /**
                     * The value to resolve for the represented index.
                     */
                    private final Object value;

                    /**
                     * Creates an argument resolver for a given index.
                     *
                     * @param index The index of the parameter to resolve.
                     * @param value The value to resolve for the represented index.
                     */
                    public ForIndex(int index, Object value) {
                        this.index = index;
                        this.value = value;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Resolution resolve(int index, Class<?> type) {
                        if (this.index != index) {
                            return Resolution.Unresolved.INSTANCE;
                        } else if (type.isPrimitive()) {
                            return WRAPPER_TYPES.get(type).isInstance(value)
                                    ? new Resolution.Resolved(value)
                                    : Resolution.Unresolved.INSTANCE;
                        } else {
                            return value == null || type.isInstance(value)
                                    ? new Resolution.Resolved(value)
                                    : Resolution.Unresolved.INSTANCE;
                        }
                    }

                    /**
                     * An argument resolver that resolves an argument for a specific parameter index by attempting a conversion via
                     * invoking a static {@code valueOf} method on the target type, if it exists. As an exception, the {@code char}
                     * and {@link Character} types are resolved if the string value represents a single character.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    public static class WithDynamicType implements ArgumentResolver {

                        /**
                         * The index of the parameter to resolve.
                         */
                        private final int index;

                        /**
                         * A string representation of the supplied value.
                         */
                        private final String value;

                        /**
                         * Creates an argument resolver for a specific parameter index and attempts a dynamic resolution.
                         *
                         * @param index The index of the parameter to resolve.
                         * @param value A string representation of the supplied value.
                         */
                        public WithDynamicType(int index, String value) {
                            this.index = index;
                            this.value = value;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Resolution resolve(int index, Class<?> type) {
                            if (this.index != index) {
                                return Resolution.Unresolved.INSTANCE;
                            } else if (type == char.class || type == Character.class) {
                                return value.length() == 1
                                        ? new Resolution.Resolved(value.charAt(0))
                                        : Resolution.Unresolved.INSTANCE;
                            } else if (type == String.class) {
                                return new Resolution.Resolved(value);
                            } else if (type.isPrimitive()) {
                                type = WRAPPER_TYPES.get(type);
                            }
                            try {
                                Method valueOf = type.getMethod("valueOf", String.class);
                                return Modifier.isStatic(valueOf.getModifiers()) && type.isAssignableFrom(valueOf.getReturnType())
                                        ? new Resolution.Resolved(valueOf.invoke(null, value))
                                        : Resolution.Unresolved.INSTANCE;
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException(exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException(exception.getCause());
                            } catch (NoSuchMethodException ignored) {
                                return Resolution.Unresolved.INSTANCE;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * A plugin engine allows the application of one or more plugins on class files found at a {@link Source} which are
     * then transferred and consumed by a {@link Target}.
     */
    interface Engine {

        /**
         * The class file extension.
         */
        String CLASS_FILE_EXTENSION = ".class";

        /**
         * Defines a new Byte Buddy instance for usage for type creation.
         *
         * @param byteBuddy The Byte Buddy instance to use.
         * @return A new plugin engine that is equal to this engine but uses the supplied Byte Buddy instance.
         */
        Engine with(ByteBuddy byteBuddy);

        /**
         * Defines a new type strategy which determines the transformation mode for any instrumented type.
         *
         * @param typeStrategy The type stategy to use.
         * @return A new plugin engine that is equal to this engine but uses the supplied type strategy.
         */
        Engine with(TypeStrategy typeStrategy);

        /**
         * Defines a new pool strategy that determines how types are being described.
         *
         * @param poolStrategy The pool strategy to use.
         * @return A new plugin engine that is equal to this engine but uses the supplied pool strategy.
         */
        Engine with(PoolStrategy poolStrategy);

        /**
         * Appends the supplied class file locator to be queried for class files additionally to any previously registered
         * class file locators.
         *
         * @param classFileLocator The class file locator to append.
         * @return A new plugin engine that is equal to this engine but with the supplied class file locator being appended.
         */
        Engine with(ClassFileLocator classFileLocator);

        /**
         * Appends the supplied listener to this engine.
         *
         * @param listener The listener to append.
         * @return A new plugin engine that is equal to this engine but with the supplied listener being appended.
         */
        Engine with(Listener listener);

        /**
         * Replaces the error handlers of this plugin engine without applying any error handlers.
         *
         * @return A new plugin engine that is equal to this engine but without any error handlers being registered.
         */
        Engine withoutErrorHandlers();

        /**
         * Replaces the error handlers of this plugin engine with the supplied error handlers.
         *
         * @param errorHandler The error handlers to apply.
         * @return A new plugin engine that is equal to this engine but with only the supplied error handlers being applied.
         */
        Engine withErrorHandlers(ErrorHandler... errorHandler);

        /**
         * Replaces the error handlers of this plugin engine with the supplied error handlers.
         *
         * @param errorHandlers The error handlers to apply.
         * @return A new plugin engine that is equal to this engine but with only the supplied error handlers being applied.
         */
        Engine withErrorHandlers(List<? extends ErrorHandler> errorHandlers);

        /**
         * Replaces the dispatcher factory of this plugin engine with a parallel dispatcher factory that uses the given amount of threads.
         *
         * @param threads The amount of threads to use.
         * @return A new plugin engine that is equal to this engine but with a parallel dispatcher factory using the specified amount of threads.
         */
        Engine withParallelTransformation(int threads);

        /**
         * Replaces the dispatcher factory of this plugin engine with the supplied dispatcher factory.
         *
         * @param dispatcherFactory The dispatcher factory to use.
         * @return A new plugin engine that is equal to this engine but with the supplied dispatcher factory being used.
         */
        Engine with(Dispatcher.Factory dispatcherFactory);

        /**
         * Ignores all types that are matched by this matcher or any previously registered ignore matcher.
         *
         * @param matcher The ignore matcher to append.
         * @return A new plugin engine that is equal to this engine but which ignores any type that is matched by the supplied matcher.
         */
        Engine ignore(ElementMatcher<? super TypeDescription> matcher);

        /**
         * Applies this plugin engine onto a given source and target.
         *
         * @param source  The source which is treated as a folder or a jar file, if a folder does not exist.
         * @param target  The target which is treated as a folder or a jar file, if a folder does not exist.
         * @param factory A list of plugin factories to a apply.
         * @return A summary of the applied transformation.
         * @throws IOException If an I/O error occurs.
         */
        Summary apply(File source, File target, Plugin.Factory... factory) throws IOException;

        /**
         * Applies this plugin engine onto a given source and target.
         *
         * @param source    The source which is treated as a folder or a jar file, if a folder does not exist.
         * @param target    The target which is treated as a folder or a jar file, if a folder does not exist.
         * @param factories A list of plugin factories to a apply.
         * @return A summary of the applied transformation.
         * @throws IOException If an I/O error occurs.
         */
        Summary apply(File source, File target, List<? extends Plugin.Factory> factories) throws IOException;

        /**
         * Applies this plugin engine onto a given source and target.
         *
         * @param source  The source to use.
         * @param target  The target to use.
         * @param factory A list of plugin factories to a apply.
         * @return A summary of the applied transformation.
         * @throws IOException If an I/O error occurs.
         */
        Summary apply(Source source, Target target, Plugin.Factory... factory) throws IOException;

        /**
         * Applies this plugin engine onto a given source and target.
         *
         * @param source    The source to use.
         * @param target    The target to use.
         * @param factories A list of plugin factories to a apply.
         * @return A summary of the applied transformation.
         * @throws IOException If an I/O error occurs.
         */
        Summary apply(Source source, Target target, List<? extends Plugin.Factory> factories) throws IOException;

        /**
         * A type strategy determines the transformation that is applied to a type description.
         */
        interface TypeStrategy {

            /**
             * Creates a builder for a given type.
             *
             * @param byteBuddy        The Byte Buddy instance to use.
             * @param typeDescription  The type being transformed.
             * @param classFileLocator A class file locator for finding the type's class file.
             * @return A dynamic type builder for the provided type.
             */
            DynamicType.Builder<?> builder(ByteBuddy byteBuddy, TypeDescription typeDescription, ClassFileLocator classFileLocator);

            /**
             * Default implementations for type strategies.
             */
            enum Default implements TypeStrategy {

                /**
                 * A type strategy that redefines a type's methods.
                 */
                REDEFINE {
                    /**
                     * {@inheritDoc}
                     */
                    public DynamicType.Builder<?> builder(ByteBuddy byteBuddy, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
                        return byteBuddy.redefine(typeDescription, classFileLocator);
                    }
                },

                /**
                 * A type strategy that rebases a type's methods.
                 */
                REBASE {
                    /**
                     * {@inheritDoc}
                     */
                    public DynamicType.Builder<?> builder(ByteBuddy byteBuddy, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
                        return byteBuddy.rebase(typeDescription, classFileLocator);
                    }
                },

                /**
                 * A type strategy that decorates a type.
                 */
                DECORATE {
                    /**
                     * {@inheritDoc}
                     */
                    public DynamicType.Builder<?> builder(ByteBuddy byteBuddy, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
                        return byteBuddy.decorate(typeDescription, classFileLocator);
                    }
                }
            }

            /**
             * A type strategy that represents a given {@link EntryPoint} for a build tool.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForEntryPoint implements TypeStrategy {

                /**
                 * The represented entry point.
                 */
                private final EntryPoint entryPoint;

                /**
                 * A method name transformer to use for rebasements.
                 */
                private final MethodNameTransformer methodNameTransformer;

                /**
                 * Creates a new type stratrgy for an entry point.
                 *
                 * @param entryPoint            The represented entry point.
                 * @param methodNameTransformer A method name transformer to use for rebasements.
                 */
                public ForEntryPoint(EntryPoint entryPoint, MethodNameTransformer methodNameTransformer) {
                    this.entryPoint = entryPoint;
                    this.methodNameTransformer = methodNameTransformer;
                }

                /**
                 * {@inheritDoc}
                 */
                public DynamicType.Builder<?> builder(ByteBuddy byteBuddy, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
                    return entryPoint.transform(typeDescription, byteBuddy, classFileLocator, methodNameTransformer);
                }
            }
        }

        /**
         * A pool strategy determines the creation of a {@link TypePool} for a plugin engine application.
         */
        interface PoolStrategy {

            /**
             * Creates a type pool.
             *
             * @param classFileLocator The class file locator to use.
             * @return An approptiate type pool.
             */
            TypePool typePool(ClassFileLocator classFileLocator);

            /**
             * A default implementation of a pool strategy where type descriptions are resolved lazily.
             */
            enum Default implements PoolStrategy {

                /**
                 * Enables faster class file parsing that does not process debug information of a class file.
                 */
                FAST(TypePool.Default.ReaderMode.FAST),

                /**
                 * Enables extended class file parsing that extracts parameter names from debug information, if available.
                 */
                EXTENDED(TypePool.Default.ReaderMode.EXTENDED);

                /**
                 * This strategy's reader mode.
                 */
                private final TypePool.Default.ReaderMode readerMode;

                /**
                 * Creates a default pool strategy.
                 *
                 * @param readerMode This strategy's reader mode.
                 */
                Default(TypePool.Default.ReaderMode readerMode) {
                    this.readerMode = readerMode;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypePool typePool(ClassFileLocator classFileLocator) {
                    return new TypePool.Default.WithLazyResolution(new TypePool.CacheProvider.Simple(),
                            classFileLocator,
                            readerMode,
                            TypePool.ClassLoading.ofPlatformLoader());
                }
            }

            /**
             * A pool strategy that resolves type descriptions eagerly. This can avoid additional overhead if the
             * majority of types is assumed to be resolved eventually.
             */
            enum Eager implements PoolStrategy {

                /**
                 * Enables faster class file parsing that does not process debug information of a class file.
                 */
                FAST(TypePool.Default.ReaderMode.FAST),

                /**
                 * Enables extended class file parsing that extracts parameter names from debug information, if available.
                 */
                EXTENDED(TypePool.Default.ReaderMode.EXTENDED);

                /**
                 * This strategy's reader mode.
                 */
                private final TypePool.Default.ReaderMode readerMode;

                /**
                 * Creates an eager pool strategy.
                 *
                 * @param readerMode This strategy's reader mode.
                 */
                Eager(TypePool.Default.ReaderMode readerMode) {
                    this.readerMode = readerMode;
                }

                /**
                 * {@inheritDoc}
                 */
                public TypePool typePool(ClassFileLocator classFileLocator) {
                    return new TypePool.Default(new TypePool.CacheProvider.Simple(),
                            classFileLocator,
                            readerMode,
                            TypePool.ClassLoading.ofPlatformLoader());
                }
            }
        }

        /**
         * An error handler that is used during a plugin engine application.
         */
        interface ErrorHandler {

            /**
             * Invoked if an error occured during a plugin's application on a given type.
             *
             * @param typeDescription The type being matched or transformed.
             * @param plugin          The plugin being applied.
             * @param throwable       The throwable that caused the error.
             */
            void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable);

            /**
             * Invoked after the application of all plugins was attempted if at least one error occured during handling a given type.
             *
             * @param typeDescription The type being transformed.
             * @param throwables      The throwables that caused errors during the application.
             */
            void onError(TypeDescription typeDescription, List<Throwable> throwables);

            /**
             * Invoked at the end of the build if at least one type transformation failed.
             *
             * @param throwables A mapping of types that failed during transformation to the errors that were caught.
             */
            void onError(Map<TypeDescription, List<Throwable>> throwables);

            /**
             * Invoked at the end of the build if a plugin could not be closed.
             *
             * @param plugin    The plugin that could not be closed.
             * @param throwable The error that was caused when the plugin was attempted to be closed.
             */
            void onError(Plugin plugin, Throwable throwable);

            /**
             * Invoked if a type transformation implied a live initializer.
             *
             * @param typeDescription The type that was transformed.
             * @param definingType    The type that implies the initializer which might be the type itself or an auxiliary type.
             */
            void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType);

            /**
             * Invoked if a type could not be resolved.
             *
             * @param typeName The name of the unresolved type.
             */
            void onUnresolved(String typeName);

            /**
             * Invoked when a manifest was found or found missing.
             *
             * @param manifest The located manifest or {@code null} if no manifest was found.
             */
            void onManifest(Manifest manifest);

            /**
             * Invoked if a resource that is not a class file is discovered.
             *
             * @param name The name of the discovered resource.
             */
            void onResource(String name);

            /**
             * An implementation of an error handler that fails the plugin engine application.
             */
            enum Failing implements ErrorHandler {

                /**
                 * An error handler that fails the build immediatly on the first error.
                 */
                FAIL_FAST {
                    /**
                     * {@inheritDoc}
                     */
                    public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                        throw new IllegalStateException("Failed to transform " + typeDescription + " using " + plugin, throwable);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                        throw new UnsupportedOperationException("onError");
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                        throw new UnsupportedOperationException("onError");
                    }
                },

                /**
                 * An error handler that fails the build after applying all plugins if at least one plugin failed.
                 */
                FAIL_AFTER_TYPE {
                    /**
                     * {@inheritDoc}
                     */
                    public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                        throw new IllegalStateException("Failed to transform " + typeDescription + ": " + throwables);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                        throw new UnsupportedOperationException("onError");
                    }
                },

                /**
                 * An error handler that fails the build after transforming all types if at least one plugin failed.
                 */
                FAIL_LAST {
                    /**
                     * {@inheritDoc}
                     */
                    public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                        /* do nothing */
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                        throw new IllegalStateException("Failed to transform at least one type: " + throwables);
                    }
                };

                /**
                 * {@inheritDoc}
                 */
                public void onError(Plugin plugin, Throwable throwable) {
                    throw new IllegalStateException("Failed to close plugin " + plugin, throwable);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onUnresolved(String typeName) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onManifest(Manifest manifest) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onResource(String name) {
                    /* do nothing */
                }
            }

            /**
             * An error handler that enforces certain properties of the transformation.
             */
            enum Enforcing implements ErrorHandler {

                /**
                 * Enforces that all types could be resolved.
                 */
                ALL_TYPES_RESOLVED {
                    @Override
                    public void onUnresolved(String typeName) {
                        throw new IllegalStateException("Failed to resolve type description for " + typeName);
                    }
                },

                /**
                 * Enforces that no type has a live initializer.
                 */
                NO_LIVE_INITIALIZERS {
                    @Override
                    public void onLiveInitializer(TypeDescription typeDescription, TypeDescription initializedType) {
                        throw new IllegalStateException("Failed to instrument " + typeDescription + " due to live initializer for " + initializedType);
                    }
                },

                /**
                 * Enforces that a source only produces class files.
                 */
                CLASS_FILES_ONLY {
                    @Override
                    public void onResource(String name) {
                        throw new IllegalStateException("Discovered a resource when only class files were allowed: " + name);
                    }
                },

                /**
                 * Enforces that a manifest is written to a target.
                 */
                MANIFEST_REQUIRED {
                    @Override
                    public void onManifest(Manifest manifest) {
                        if (manifest == null) {
                            throw new IllegalStateException("Required a manifest but no manifest was found");
                        }
                    }
                };

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Plugin plugin, Throwable throwable) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onUnresolved(String typeName) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onManifest(Manifest manifest) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onResource(String name) {
                    /* do nothing */
                }
            }

            /**
             * A compound error handler.
             */
            class Compound implements ErrorHandler {

                /**
                 * The error handlers that are represented by this instance.
                 */
                private final List<ErrorHandler> errorHandlers;

                /**
                 * Creates a new compound error handler.
                 *
                 * @param errorHandler The error handlers that are represented by this instance.
                 */
                public Compound(ErrorHandler... errorHandler) {
                    this(Arrays.asList(errorHandler));
                }

                /**
                 * Creates a new compound error handler.
                 *
                 * @param errorHandlers The error handlers that are represented by this instance.
                 */
                public Compound(List<? extends ErrorHandler> errorHandlers) {
                    this.errorHandlers = new ArrayList<ErrorHandler>();
                    for (ErrorHandler errorHandler : errorHandlers) {
                        if (errorHandler instanceof Compound) {
                            this.errorHandlers.addAll(((Compound) errorHandler).errorHandlers);
                        } else if (!(errorHandler instanceof Listener.NoOp)) {
                            this.errorHandlers.add(errorHandler);
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onError(typeDescription, plugin, throwable);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onError(typeDescription, throwables);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onError(throwables);
                    }

                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Plugin plugin, Throwable throwable) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onError(plugin, throwable);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onLiveInitializer(typeDescription, definingType);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onUnresolved(String typeName) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onUnresolved(typeName);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onManifest(Manifest manifest) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onManifest(manifest);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onResource(String name) {
                    for (ErrorHandler errorHandler : errorHandlers) {
                        errorHandler.onResource(name);
                    }
                }
            }
        }

        /**
         * A listener that is invoked upon any event during a plugin engine application.
         */
        interface Listener extends ErrorHandler {

            /**
             * Invoked upon discovering a type but prior to its resolution.
             *
             * @param typeName The name of the discovered type.
             */
            void onDiscovery(String typeName);

            /**
             * Invoked after a type was transformed using a specific plugin.
             *
             * @param typeDescription The type being transformed.
             * @param plugin          The plugin that was applied.
             */
            void onTransformation(TypeDescription typeDescription, Plugin plugin);

            /**
             * Invoked after a type was transformed using at least one plugin.
             *
             * @param typeDescription The type being transformed.
             * @param plugins         A list of plugins that were applied.
             */
            void onTransformation(TypeDescription typeDescription, List<Plugin> plugins);

            /**
             * Invoked if a type description is ignored by a given plugin. This callback is not invoked,
             * if the ignore type matcher excluded a type from transformation.
             *
             * @param typeDescription The type being transformed.
             * @param plugin          The plugin that ignored the given type.
             */
            void onIgnored(TypeDescription typeDescription, Plugin plugin);

            /**
             * Invoked if one or more plugins did not transform a type. This callback is also invoked if an
             * ignore matcher excluded a type from transformation.
             *
             * @param typeDescription The type being transformed.
             * @param plugins         the plugins that ignored the type.
             */
            void onIgnored(TypeDescription typeDescription, List<Plugin> plugins);

            /**
             * Invoked upon completing handling a type that was either transformed or ignored.
             *
             * @param typeDescription The type that was transformed.
             */
            void onComplete(TypeDescription typeDescription);

            /**
             * A non-operational listener.
             */
            enum NoOp implements Listener {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public void onDiscovery(String typeName) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTransformation(TypeDescription typeDescription, Plugin plugin) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTransformation(TypeDescription typeDescription, List<Plugin> plugins) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onIgnored(TypeDescription typeDescription, Plugin plugin) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onIgnored(TypeDescription typeDescription, List<Plugin> plugins) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Plugin plugin, Throwable throwable) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onComplete(TypeDescription typeDescription) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onUnresolved(String typeName) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onManifest(Manifest manifest) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onResource(String name) {
                    /* do nothing */
                }
            }

            /**
             * An adapter that implements all methods non-operational.
             */
            abstract class Adapter implements Listener {

                /**
                 * {@inheritDoc}
                 */
                public void onDiscovery(String typeName) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTransformation(TypeDescription typeDescription, Plugin plugin) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTransformation(TypeDescription typeDescription, List<Plugin> plugins) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onIgnored(TypeDescription typeDescription, Plugin plugin) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onIgnored(TypeDescription typeDescription, List<Plugin> plugins) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Plugin plugin, Throwable throwable) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onComplete(TypeDescription typeDescription) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onUnresolved(String typeName) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onManifest(Manifest manifest) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void onResource(String name) {
                    /* do nothing */
                }
            }

            /**
             * A listener that forwards significant events of a plugin engine application to a {@link PrintStream}.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class StreamWriting extends Adapter {

                /**
                 * The prefix that is appended to all written messages.
                 */
                protected static final String PREFIX = "[Byte Buddy]";

                /**
                 * The print stream to delegate to.
                 */
                private final PrintStream printStream;

                /**
                 * Creates a new stream writing listener.
                 *
                 * @param printStream The print stream to delegate to.
                 */
                public StreamWriting(PrintStream printStream) {
                    this.printStream = printStream;
                }

                /**
                 * Creates a stream writing listener that prints all events on {@link System#out}.
                 *
                 * @return A listener that writes events to the system output stream.
                 */
                public static StreamWriting toSystemOut() {
                    return new StreamWriting(System.out);
                }

                /**
                 * Creates a stream writing listener that prints all events on {@link System#err}.
                 *
                 * @return A listener that writes events to the system error stream.
                 */
                public static StreamWriting toSystemError() {
                    return new StreamWriting(System.err);
                }

                /**
                 * Returns a new listener that only prints transformation and error events.
                 *
                 * @return A new listener that only prints transformation and error events.
                 */
                public Listener withTransformationsOnly() {
                    return new WithTransformationsOnly(this);
                }

                /**
                 * Returns a new listener that only prints error events.
                 *
                 * @return A new listener that only prints error events.
                 */
                public Listener withErrorsOnly() {
                    return new WithErrorsOnly(this);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onDiscovery(String typeName) {
                    printStream.printf(PREFIX + " DISCOVERY %s", typeName);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTransformation(TypeDescription typeDescription, Plugin plugin) {
                    printStream.printf(PREFIX + " TRANSFORM %s for %s", typeDescription, plugin);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onIgnored(TypeDescription typeDescription, Plugin plugin) {
                    printStream.printf(PREFIX + " IGNORE %s for %s", typeDescription, plugin);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    synchronized (printStream) {
                        printStream.printf(PREFIX + " ERROR %s for %s", typeDescription, plugin);
                        throwable.printStackTrace(printStream);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Plugin plugin, Throwable throwable) {
                    synchronized (printStream) {
                        printStream.printf(PREFIX + " ERROR %s", plugin);
                        throwable.printStackTrace(printStream);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onUnresolved(String typeName) {
                    printStream.printf(PREFIX + " UNRESOLVED %s", typeName);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    printStream.printf(PREFIX + " LIVE %s on %s", typeDescription, definingType);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onComplete(TypeDescription typeDescription) {
                    printStream.printf(PREFIX + " COMPLETE %s", typeDescription);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onManifest(Manifest manifest) {
                    printStream.printf(PREFIX + " MANIFEST %b", manifest != null);
                }

                /**
                 * {@inheritDoc}
                 */
                public void onResource(String name) {
                    printStream.printf(PREFIX + " RESOURCE %s", name);
                }
            }

            /**
             * A decorator for another listener to only print transformation and error events.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class WithTransformationsOnly extends Adapter {

                /**
                 * The delegate to forward events to.
                 */
                private final Listener delegate;

                /**
                 * Creates a new listener decorator that filter any event that is not related to transformation or errors.
                 *
                 * @param delegate The delegate to forward events to.
                 */
                public WithTransformationsOnly(Listener delegate) {
                    this.delegate = delegate;
                }

                @Override
                public void onTransformation(TypeDescription typeDescription, Plugin plugin) {
                    delegate.onTransformation(typeDescription, plugin);
                }

                @Override
                public void onTransformation(TypeDescription typeDescription, List<Plugin> plugins) {
                    delegate.onTransformation(typeDescription, plugins);
                }

                @Override
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    delegate.onError(typeDescription, plugin, throwable);
                }

                @Override
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    delegate.onError(typeDescription, throwables);
                }

                @Override
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    delegate.onError(throwables);
                }

                @Override
                public void onError(Plugin plugin, Throwable throwable) {
                    delegate.onError(plugin, throwable);
                }
            }

            /**
             * A decorator for another listener to only print error events.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class WithErrorsOnly extends Adapter {

                /**
                 * The delegate to forward events to.
                 */
                private final Listener delegate;

                /**
                 * Creates a new listener decorator that filter any event that is not related to errors.
                 *
                 * @param delegate The delegate to forward events to.
                 */
                public WithErrorsOnly(Listener delegate) {
                    this.delegate = delegate;
                }

                @Override
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    delegate.onError(typeDescription, plugin, throwable);
                }

                @Override
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    delegate.onError(typeDescription, throwables);
                }

                @Override
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    delegate.onError(throwables);
                }

                @Override
                public void onError(Plugin plugin, Throwable throwable) {
                    delegate.onError(plugin, throwable);
                }
            }

            /**
             * A listener decorator that forwards events to an error handler if they are applicable.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForErrorHandler extends Adapter {

                /**
                 * The error handler to delegate to.
                 */
                private final ErrorHandler errorHandler;

                /**
                 * Creates a new listener representation for an error handler.
                 *
                 * @param errorHandler The error handler to delegate to.
                 */
                public ForErrorHandler(ErrorHandler errorHandler) {
                    this.errorHandler = errorHandler;
                }

                @Override
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    errorHandler.onError(typeDescription, plugin, throwable);
                }

                @Override
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    errorHandler.onError(typeDescription, throwables);
                }

                @Override
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    errorHandler.onError(throwables);
                }

                @Override
                public void onError(Plugin plugin, Throwable throwable) {
                    errorHandler.onError(plugin, throwable);
                }

                @Override
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    errorHandler.onLiveInitializer(typeDescription, definingType);
                }

                @Override
                public void onUnresolved(String typeName) {
                    errorHandler.onUnresolved(typeName);
                }

                @Override
                public void onManifest(Manifest manifest) {
                    errorHandler.onManifest(manifest);
                }

                @Override
                public void onResource(String name) {
                    errorHandler.onResource(name);
                }
            }

            /**
             * A compound listener.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Compound implements Listener {

                /**
                 * A list of listeners that are represented by this compound instance.
                 */
                private final List<Listener> listeners;

                /**
                 * Creates a new compound listener.
                 *
                 * @param listener A list of listeners that are represented by this compound instance.
                 */
                public Compound(Listener... listener) {
                    this(Arrays.asList(listener));
                }

                /**
                 * Creates a new compound listener.
                 *
                 * @param listeners A list of listeners that are represented by this compound instance.
                 */
                public Compound(List<? extends Listener> listeners) {
                    this.listeners = new ArrayList<Listener>();
                    for (Listener listener : listeners) {
                        if (listener instanceof Listener.Compound) {
                            this.listeners.addAll(((Listener.Compound) listener).listeners);
                        } else if (!(listener instanceof NoOp)) {
                            this.listeners.add(listener);
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onDiscovery(String typeName) {
                    for (Listener listener : listeners) {
                        listener.onDiscovery(typeName);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTransformation(TypeDescription typeDescription, Plugin plugin) {
                    for (Listener listener : listeners) {
                        listener.onTransformation(typeDescription, plugin);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onTransformation(TypeDescription typeDescription, List<Plugin> plugins) {
                    for (Listener listener : listeners) {
                        listener.onTransformation(typeDescription, plugins);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onIgnored(TypeDescription typeDescription, Plugin plugin) {
                    for (Listener listener : listeners) {
                        listener.onIgnored(typeDescription, plugin);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onIgnored(TypeDescription typeDescription, List<Plugin> plugins) {
                    for (Listener listener : listeners) {
                        listener.onIgnored(typeDescription, plugins);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, Plugin plugin, Throwable throwable) {
                    for (Listener listener : listeners) {
                        listener.onError(typeDescription, plugin, throwable);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(TypeDescription typeDescription, List<Throwable> throwables) {
                    for (Listener listener : listeners) {
                        listener.onError(typeDescription, throwables);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Map<TypeDescription, List<Throwable>> throwables) {
                    for (Listener listener : listeners) {
                        listener.onError(throwables);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onError(Plugin plugin, Throwable throwable) {
                    for (Listener listener : listeners) {
                        listener.onError(plugin, throwable);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onLiveInitializer(TypeDescription typeDescription, TypeDescription definingType) {
                    for (Listener listener : listeners) {
                        listener.onLiveInitializer(typeDescription, definingType);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onComplete(TypeDescription typeDescription) {
                    for (Listener listener : listeners) {
                        listener.onComplete(typeDescription);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onUnresolved(String typeName) {
                    for (Listener listener : listeners) {
                        listener.onUnresolved(typeName);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onManifest(Manifest manifest) {
                    for (Listener listener : listeners) {
                        listener.onManifest(manifest);
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void onResource(String name) {
                    for (Listener listener : listeners) {
                        listener.onResource(name);
                    }
                }
            }
        }

        /**
         * A source for a plugin engine provides binary elements to consider for transformation.
         */
        interface Source {

            /**
             * Initiates reading from a source.
             *
             * @return The origin to read from.
             * @throws IOException If an I/O error occurs.
             */
            Origin read() throws IOException;

            /**
             * An origin for elements.
             */
            interface Origin extends Iterable<Element>, Closeable {

                /**
                 * Indicates that no manifest exists.
                 */
                Manifest NO_MANIFEST = null;

                /**
                 * Returns the manifest file of the source location or {@code null} if no manifest exists.
                 *
                 * @return This source's manifest or {@code null}.
                 * @throws IOException If an I/O error occurs.
                 */
                Manifest getManifest() throws IOException;

                /**
                 * Returns a class file locator for the represented source. If the class file locator needs to be closed, it is the responsibility
                 * of this origin to close the locator or its underlying resources.
                 *
                 * @return A class file locator for locating class files of this instance..
                 */
                ClassFileLocator getClassFileLocator();

                /**
                 * An origin implementation for a jar file.
                 */
                class ForJarFile implements Origin {

                    /**
                     * The represented file.
                     */
                    private final JarFile file;

                    /**
                     * Creates a new origin for a jar file.
                     *
                     * @param file The represented file.
                     */
                    public ForJarFile(JarFile file) {
                        this.file = file;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Manifest getManifest() throws IOException {
                        return file.getManifest();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ClassFileLocator getClassFileLocator() {
                        return new ClassFileLocator.ForJarFile(file);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void close() throws IOException {
                        file.close();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Iterator<Element> iterator() {
                        return new JarFileIterator(file.entries());
                    }

                    /**
                     * An iterator for jar file entries.
                     */
                    protected class JarFileIterator implements Iterator<Element> {

                        /**
                         * The represented enumeration.
                         */
                        private final Enumeration<JarEntry> enumeration;

                        /**
                         * Creates a new jar file iterator.
                         *
                         * @param enumeration The represented enumeration.
                         */
                        protected JarFileIterator(Enumeration<JarEntry> enumeration) {
                            this.enumeration = enumeration;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public boolean hasNext() {
                            return enumeration.hasMoreElements();
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Element next() {
                            return new Element.ForJarEntry(file, enumeration.nextElement());
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }
                    }
                }

                /**
                 * An origin that forwards all invocations to a delegate where an {@link ElementMatcher} is applied prior to iteration.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class Filtering implements Origin {

                    /**
                     * The origin to which invocations are delegated.
                     */
                    private final Origin delegate;

                    /**
                     * The element matcher being used to filter elements.
                     */
                    private final ElementMatcher<Element> matcher;

                    /**
                     * {@code true} if the manifest should be retained.
                     */
                    private final boolean manifest;

                    /**
                     * Creates a new filtering origin that retains the delegated origin's manifest.
                     *
                     * @param delegate The origin to which invocations are delegated.
                     * @param matcher  The element matcher being used to filter elements.
                     */
                    public Filtering(Origin delegate, ElementMatcher<Element> matcher) {
                        this(delegate, matcher, true);
                    }

                    /**
                     * Creates a new filtering origin.
                     *
                     * @param delegate The origin to which invocations are delegated.
                     * @param matcher  The element matcher being used to filter elements.
                     * @param manifest {@code true} if the manifest should be retained.
                     */
                    public Filtering(Origin delegate, ElementMatcher<Element> matcher, boolean manifest) {
                        this.delegate = delegate;
                        this.matcher = matcher;
                        this.manifest = manifest;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Manifest getManifest() throws IOException {
                        return manifest ? delegate.getManifest() : NO_MANIFEST;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ClassFileLocator getClassFileLocator() {
                        return delegate.getClassFileLocator();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void close() throws IOException {
                        delegate.close();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Iterator<Element> iterator() {
                        return new FilteringIterator(delegate.iterator(), matcher);
                    }

                    /**
                     * An iterator that applies a filter to observed elements.
                     */
                    private static class FilteringIterator implements Iterator<Element> {

                        /**
                         * The underlying iterator.
                         */
                        private final Iterator<Element> iterator;

                        /**
                         * The element matcher being used to filter elements.
                         */
                        private final ElementMatcher<Element> matcher;

                        /**
                         * The current element or {@code null} if no further elements are available.
                         */
                        private Element current;

                        /**
                         * Creates a new filtering iterator.
                         *
                         * @param iterator The underlying iterator.
                         * @param matcher  The element matcher being used to filter elements.
                         */
                        private FilteringIterator(Iterator<Element> iterator, ElementMatcher<Element> matcher) {
                            this.iterator = iterator;
                            this.matcher = matcher;
                            Element element;
                            while (iterator.hasNext()) {
                                element = iterator.next();
                                if (matcher.matches(element)) {
                                    current = element;
                                    break;
                                }
                            }
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public boolean hasNext() {
                            return current != null;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Element next() {
                            if (current == null) {
                                throw new NoSuchElementException();
                            }
                            try {
                                return current;
                            } finally {
                                current = null;
                                Element element;
                                while (iterator.hasNext()) {
                                    element = iterator.next();
                                    if (matcher.matches(element)) {
                                        current = element;
                                        break;
                                    }
                                }
                            }
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public void remove() {
                            iterator.remove();
                        }
                    }
                }
            }

            /**
             * Represents a binary element found in a source location.
             */
            interface Element {

                /**
                 * Returns the element's relative path and name.
                 *
                 * @return The element's path and name.
                 */
                String getName();

                /**
                 * Returns an input stream to read this element's binary information.
                 *
                 * @return An input stream that represents this element's binary information.
                 * @throws IOException If an I/O error occurs.
                 */
                InputStream getInputStream() throws IOException;

                /**
                 * Resolves this element to a more specialized form if possible. Doing so allows for performance
                 * optimizations if more specialized formats are available.
                 *
                 * @param type The requested spezialized type.
                 * @param <T>  The requested spezialized type.
                 * @return The resolved element or {@code null} if a transformation is impossible.
                 */
                <T> T resolveAs(Class<T> type);

                /**
                 * An element representation for a byte array.
                 */
                @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Not mutating the byte array is part of the class contract.")
                @HashCodeAndEqualsPlugin.Enhance
                class ForByteArray implements Element {

                    /**
                     * The element's name.
                     */
                    private final String name;

                    /**
                     * The element's binary representation.
                     */
                    private final byte[] binaryRepresentation;

                    /**
                     * Creates an element that is represented by a byte array.
                     *
                     * @param name                 The element's name.
                     * @param binaryRepresentation The element's binary representation.
                     */
                    public ForByteArray(String name, byte[] binaryRepresentation) {
                        this.name = name;
                        this.binaryRepresentation = binaryRepresentation;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public String getName() {
                        return name;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(binaryRepresentation);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public <T> T resolveAs(Class<T> type) {
                        return null;
                    }
                }

                /**
                 * An element representation for a file.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForFile implements Element {

                    /**
                     * The root folder of the represented source.
                     */
                    private final File root;

                    /**
                     * The file location of the represented file that is located within the root directory.
                     */
                    private final File file;

                    /**
                     * Creates an element representation for a file.
                     *
                     * @param root The root folder of the represented source.
                     * @param file The file location of the represented file that is located within the root directory.
                     */
                    public ForFile(File root, File file) {
                        this.root = root;
                        this.file = file;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public String getName() {
                        return root.getAbsoluteFile().toURI().relativize(file.getAbsoluteFile().toURI()).getPath();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public InputStream getInputStream() throws IOException {
                        return new FileInputStream(file);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressWarnings("unchecked")
                    public <T> T resolveAs(Class<T> type) {
                        return File.class.isAssignableFrom(type)
                                ? (T) file
                                : null;
                    }
                }

                /**
                 * Represents a jar file entry as an element.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForJarEntry implements Element {

                    /**
                     * The source's underlying jar file.
                     */
                    private final JarFile file;

                    /**
                     * The entry that is represented by this element.
                     */
                    private final JarEntry entry;

                    /**
                     * Creates a new element representation for a jar file entry.
                     *
                     * @param file  The source's underlying jar file.
                     * @param entry The entry that is represented by this element.
                     */
                    public ForJarEntry(JarFile file, JarEntry entry) {
                        this.file = file;
                        this.entry = entry;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public String getName() {
                        return entry.getName();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public InputStream getInputStream() throws IOException {
                        return file.getInputStream(entry);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressWarnings("unchecked")
                    public <T> T resolveAs(Class<T> type) {
                        return JarEntry.class.isAssignableFrom(type)
                                ? (T) entry
                                : null;
                    }
                }
            }

            /**
             * An empty source that does not contain any elements or a manifest.
             */
            enum Empty implements Source, Origin {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Origin read() {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public ClassFileLocator getClassFileLocator() {
                    return ClassFileLocator.NoOp.INSTANCE;
                }

                /**
                 * {@inheritDoc}
                 */
                public Manifest getManifest() {
                    return NO_MANIFEST;
                }

                /**
                 * {@inheritDoc}
                 */
                public Iterator<Element> iterator() {
                    return Collections.<Element>emptySet().iterator();
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    /* do nothing */
                }
            }

            /**
             * A source that represents a collection of in-memory resources that are represented as byte arrays.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class InMemory implements Source, Origin {

                /**
                 * A mapping of resource names to their binary representation.
                 */
                private final Map<String, byte[]> storage;

                /**
                 * Creates a new in-memory source.
                 *
                 * @param storage A mapping of resource names to their binary representation.
                 */
                public InMemory(Map<String, byte[]> storage) {
                    this.storage = storage;
                }

                /**
                 * Represents a collection of types as a in-memory source.
                 *
                 * @param type The types to represent.
                 * @return A source representing the supplied types.
                 */
                public static Source ofTypes(Class<?>... type) {
                    return ofTypes(Arrays.asList(type));
                }

                /**
                 * Represents a collection of types as a in-memory source.
                 *
                 * @param types The types to represent.
                 * @return A source representing the supplied types.
                 */
                public static Source ofTypes(Collection<? extends Class<?>> types) {
                    Map<TypeDescription, byte[]> binaryRepresentations = new HashMap<TypeDescription, byte[]>();
                    for (Class<?> type : types) {
                        binaryRepresentations.put(TypeDescription.ForLoadedType.of(type), ClassFileLocator.ForClassLoader.read(type));
                    }
                    return ofTypes(binaryRepresentations);
                }

                /**
                 * Represents a map of type names to their binary representation as an in-memory source.
                 *
                 * @param binaryRepresentations A mapping of type names to their binary representation.
                 * @return A source representing the supplied types.
                 */
                public static Source ofTypes(Map<TypeDescription, byte[]> binaryRepresentations) {
                    Map<String, byte[]> storage = new HashMap<String, byte[]>();
                    for (Map.Entry<TypeDescription, byte[]> entry : binaryRepresentations.entrySet()) {
                        storage.put(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION, entry.getValue());
                    }
                    return new InMemory(storage);
                }

                /**
                 * {@inheritDoc}
                 */
                public Origin read() {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public ClassFileLocator getClassFileLocator() {
                    return ClassFileLocator.Simple.ofResources(storage);
                }

                /**
                 * {@inheritDoc}
                 */
                public Manifest getManifest() throws IOException {
                    byte[] binaryRepresentation = storage.get(JarFile.MANIFEST_NAME);
                    if (binaryRepresentation == null) {
                        return NO_MANIFEST;
                    } else {
                        return new Manifest(new ByteArrayInputStream(binaryRepresentation));
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Iterator<Element> iterator() {
                    return new MapEntryIterator(storage.entrySet().iterator());
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    /* do nothing */
                }

                /**
                 * An iterator that represents map entries as sources.
                 */
                protected static class MapEntryIterator implements Iterator<Element> {

                    /**
                     * The represented iterator.
                     */
                    private final Iterator<Map.Entry<String, byte[]>> iterator;

                    /**
                     * Creates a new map entry iterator.
                     *
                     * @param iterator The represented iterator.
                     */
                    protected MapEntryIterator(Iterator<Map.Entry<String, byte[]>> iterator) {
                        this.iterator = iterator;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Element next() {
                        Map.Entry<String, byte[]> entry = iterator.next();
                        return new Element.ForByteArray(entry.getKey(), entry.getValue());
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                }
            }

            /**
             * Represents the contents of a folder as class files.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForFolder implements Source, Origin {

                /**
                 * The folder to represent.
                 */
                private final File folder;

                /**
                 * Creates a new source representation for a given folder.
                 *
                 * @param folder The folder to represent.
                 */
                public ForFolder(File folder) {
                    this.folder = folder;
                }

                /**
                 * Initializes a reading from this source.
                 *
                 * @return A source that represents the resource of this origin.
                 */
                public Origin read() {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public ClassFileLocator getClassFileLocator() {
                    return new ClassFileLocator.ForFolder(folder);
                }

                /**
                 * {@inheritDoc}
                 */
                public Manifest getManifest() throws IOException {
                    File file = new File(folder, JarFile.MANIFEST_NAME);
                    if (file.exists()) {
                        InputStream inputStream = new FileInputStream(file);
                        try {
                            return new Manifest(inputStream);
                        } finally {
                            inputStream.close();
                        }
                    } else {
                        return NO_MANIFEST;
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public Iterator<Element> iterator() {
                    return new FolderIterator(folder);
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    /* do nothing */
                }

                /**
                 * An iterator that exposes all files within a folder structure as elements.
                 */
                protected class FolderIterator implements Iterator<Element> {

                    /**
                     * A list of files and folders to process.
                     */
                    private final LinkedList<File> files;

                    /**
                     * Creates a new iterator representation for all files within a folder.
                     *
                     * @param folder The root folder.
                     */
                    protected FolderIterator(File folder) {
                        files = new LinkedList<File>(Collections.singleton(folder));
                        File candidate;
                        do {
                            candidate = files.removeFirst();
                            File[] file = candidate.listFiles();
                            if (file != null) {
                                files.addAll(0, Arrays.asList(file));
                            }
                        } while (!files.isEmpty() && (files.peek().isDirectory() || files.peek().equals(new File(folder, JarFile.MANIFEST_NAME))));
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public boolean hasNext() {
                        return !files.isEmpty();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressFBWarnings(value = "IT_NO_SUCH_ELEMENT", justification = "Exception is thrown by invoking removeFirst on an empty list.")
                    public Element next() {
                        try {
                            return new Element.ForFile(folder, files.removeFirst());
                        } finally {
                            while (!files.isEmpty() && (files.peek().isDirectory() || files.peek().equals(new File(folder, JarFile.MANIFEST_NAME)))) {
                                File folder = files.removeFirst();
                                File[] file = folder.listFiles();
                                if (file != null) {
                                    files.addAll(0, Arrays.asList(file));
                                }
                            }
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                }
            }

            /**
             * Represents a jar file as a source.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJarFile implements Source {

                /**
                 * The jar file being represented by this source.
                 */
                private final File file;

                /**
                 * Creates a new source for a jar file.
                 *
                 * @param file The jar file being represented by this source.
                 */
                public ForJarFile(File file) {
                    this.file = file;
                }

                /**
                 * {@inheritDoc}
                 */
                public Origin read() throws IOException {
                    return new Origin.ForJarFile(new JarFile(file));
                }
            }

            /**
             * A source that applies a filter upon iterating elements.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Filtering implements Source {

                /**
                 * The source to which invocations are delegated.
                 */
                private final Source delegate;

                /**
                 * The element matcher being used to filter elements.
                 */
                private final ElementMatcher<Element> matcher;

                /**
                 * {@code true} if the manifest should be retained.
                 */
                private final boolean manifest;

                /**
                 * Creates a new filtering source that retains the manifest of the delegated source.
                 *
                 * @param delegate The source to which invocations are delegated.
                 * @param matcher  The element matcher being used to filter elements.
                 */
                public Filtering(Source delegate, ElementMatcher<Element> matcher) {
                    this(delegate, matcher, true);
                }

                /**
                 * Creates a new filtering source.
                 *
                 * @param delegate The source to which invocations are delegated.
                 * @param matcher  The element matcher being used to filter elements.
                 * @param manifest {@code true} if the manifest should be retained.
                 */
                public Filtering(Source delegate, ElementMatcher<Element> matcher, boolean manifest) {
                    this.delegate = delegate;
                    this.matcher = matcher;
                    this.manifest = manifest;
                }

                /**
                 * {@inheritDoc}
                 */
                public Origin read() throws IOException {
                    return new Origin.Filtering(delegate.read(), matcher, manifest);
                }
            }
        }

        /**
         * A target for a plugin engine represents a sink container for all elements that are supplied by a {@link Source}.
         */
        interface Target {

            /**
             * Initializes this target prior to writing.
             *
             * @param manifest The manifest for the target or {@code null} if no manifest was found.
             * @return The sink to write to.
             * @throws IOException If an I/O error occurs.
             */
            Sink write(Manifest manifest) throws IOException;

            /**
             * A sink represents an active writing process.
             */
            interface Sink extends Closeable {

                /**
                 * Stores the supplied binary representation of types in this sink.
                 *
                 * @param binaryRepresentations The binary representations to store.
                 * @throws IOException If an I/O error occurs.
                 */
                void store(Map<TypeDescription, byte[]> binaryRepresentations) throws IOException;

                /**
                 * Retains the supplied element in its original form.
                 *
                 * @param element The element to retain.
                 * @throws IOException If an I/O error occurs.
                 */
                void retain(Source.Element element) throws IOException;

                /**
                 * Implements a sink for a jar file.
                 */
                class ForJarOutputStream implements Sink {

                    /**
                     * The output stream to write to.
                     */
                    private final JarOutputStream outputStream;

                    /**
                     * Creates a new sink for a jar file.
                     *
                     * @param outputStream The output stream to write to.
                     */
                    public ForJarOutputStream(JarOutputStream outputStream) {
                        this.outputStream = outputStream;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void store(Map<TypeDescription, byte[]> binaryRepresentations) throws IOException {
                        for (Map.Entry<TypeDescription, byte[]> entry : binaryRepresentations.entrySet()) {
                            outputStream.putNextEntry(new JarEntry(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION));
                            outputStream.write(entry.getValue());
                            outputStream.closeEntry();
                        }
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void retain(Source.Element element) throws IOException {
                        JarEntry entry = element.resolveAs(JarEntry.class);
                        outputStream.putNextEntry(entry == null
                                ? new JarEntry(element.getName())
                                : entry);
                        InputStream inputStream = element.getInputStream();
                        try {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, length);
                            }
                        } finally {
                            inputStream.close();
                        }
                        outputStream.closeEntry();
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void close() throws IOException {
                        outputStream.close();
                    }
                }
            }

            /**
             * A sink that discards any entry.
             */
            enum Discarding implements Target, Sink {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * {@inheritDoc}
                 */
                public Sink write(Manifest manifest) {
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public void store(Map<TypeDescription, byte[]> binaryRepresentations) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void retain(Source.Element element) {
                    /* do nothing */
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    /* do nothing */
                }
            }

            /**
             * A sink that stores all elements in a memory map.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class InMemory implements Target, Sink {

                /**
                 * The map for storing all elements being received.
                 */
                private final Map<String, byte[]> storage;

                /**
                 * Creates a new in-memory storage.
                 */
                public InMemory() {
                    this(new HashMap<String, byte[]>());
                }

                /**
                 * Creates a new in-memory storage.
                 *
                 * @param storage The map for storing all elements being received.
                 */
                public InMemory(Map<String, byte[]> storage) {
                    this.storage = storage;
                }

                /**
                 * {@inheritDoc}
                 */
                public Sink write(Manifest manifest) throws IOException {
                    if (manifest != null) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        try {
                            manifest.write(outputStream);
                        } finally {
                            outputStream.close();
                        }
                        storage.put(JarFile.MANIFEST_NAME, outputStream.toByteArray());
                    }
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public void store(Map<TypeDescription, byte[]> binaryRepresentations) {
                    for (Map.Entry<TypeDescription, byte[]> entry : binaryRepresentations.entrySet()) {
                        storage.put(entry.getKey().getInternalName() + CLASS_FILE_EXTENSION, entry.getValue());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void retain(Source.Element element) throws IOException {
                    String name = element.getName();
                    if (!name.endsWith("/")) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        try {
                            InputStream inputStream = element.getInputStream();
                            try {
                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, length);
                                }
                            } finally {
                                inputStream.close();
                            }
                        } finally {
                            outputStream.close();
                        }
                        storage.put(element.getName(), outputStream.toByteArray());
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    /* do nothing */
                }

                /**
                 * Returns the in-memory storage.
                 *
                 * @return The in-memory storage.
                 */
                public Map<String, byte[]> getStorage() {
                    return storage;
                }

                /**
                 * Returns the in-memory storage as a type-map where all non-class files are discarded.
                 *
                 * @return The in-memory storage as a type map.
                 */
                public Map<String, byte[]> toTypeMap() {
                    Map<String, byte[]> binaryRepresentations = new HashMap<String, byte[]>();
                    for (Map.Entry<String, byte[]> entry : storage.entrySet()) {
                        if (entry.getKey().endsWith(CLASS_FILE_EXTENSION)) {
                            binaryRepresentations.put(entry.getKey()
                                    .substring(0, entry.getKey().length() - CLASS_FILE_EXTENSION.length())
                                    .replace('/', '.'), entry.getValue());
                        }
                    }
                    return binaryRepresentations;
                }
            }

            /**
             * Represents a folder as the target for a plugin engine's application.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForFolder implements Target, Sink {

                /**
                 * A dispatcher for using NIO2 if the current VM supports it.
                 */
                protected static final Dispatcher DISPATCHER = AccessController.doPrivileged(Dispatcher.CreationAction.INSTANCE);

                /**
                 * The folder that is represented by this instance.
                 */
                private final File folder;

                /**
                 * Creates a new target for a folder.
                 *
                 * @param folder The folder that is represented by this instance.
                 */
                public ForFolder(File folder) {
                    this.folder = folder;
                }

                /**
                 * {@inheritDoc}
                 */
                public Sink write(Manifest manifest) throws IOException {
                    if (manifest != null) {
                        File target = new File(folder, JarFile.MANIFEST_NAME);
                        if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                            throw new IOException("Could not create directory: " + target.getParent());
                        }
                        OutputStream outputStream = new FileOutputStream(target);
                        try {
                            manifest.write(outputStream);
                        } finally {
                            outputStream.close();
                        }
                    }
                    return this;
                }

                /**
                 * {@inheritDoc}
                 */
                public void store(Map<TypeDescription, byte[]> binaryRepresentations) throws IOException {
                    for (Map.Entry<TypeDescription, byte[]> entry : binaryRepresentations.entrySet()) {
                        File target = new File(folder, entry.getKey().getInternalName() + CLASS_FILE_EXTENSION);
                        if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                            throw new IOException("Could not create directory: " + target.getParent());
                        }
                        OutputStream outputStream = new FileOutputStream(target);
                        try {
                            outputStream.write(entry.getValue());
                        } finally {
                            outputStream.close();
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void retain(Source.Element element) throws IOException {
                    String name = element.getName();
                    if (!name.endsWith("/")) {
                        File target = new File(folder, name), resolved = element.resolveAs(File.class);
                        if (!target.getCanonicalPath().startsWith(folder.getCanonicalPath())) {
                            throw new IllegalArgumentException(target + " is not a subdirectory of " + folder);
                        } else if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
                            throw new IOException("Could not create directory: " + target.getParent());
                        } else if (DISPATCHER.isAlive() && resolved != null && !resolved.equals(target)) {
                            DISPATCHER.copy(resolved, target);
                        } else if (!target.equals(resolved)) {
                            InputStream inputStream = element.getInputStream();
                            try {
                                OutputStream outputStream = new FileOutputStream(target);
                                try {
                                    byte[] buffer = new byte[1024];
                                    int length;
                                    while ((length = inputStream.read(buffer)) != -1) {
                                        outputStream.write(buffer, 0, length);
                                    }
                                } finally {
                                    outputStream.close();
                                }
                            } finally {
                                inputStream.close();
                            }
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    /* do nothing */
                }

                /**
                 * A dispatcher that allows for file copy operations based on NIO2 if available.
                 */
                protected interface Dispatcher {

                    /**
                     * Returns {@code true} if this dispatcher is alive.
                     *
                     * @return {@code true} if this dispatcher is alive.
                     */
                    boolean isAlive();

                    /**
                     * Copies the source file to the target location.
                     *
                     * @param source The source file.
                     * @param target The target file.
                     * @throws IOException If an I/O error occurs.
                     */
                    void copy(File source, File target) throws IOException;

                    /**
                     * An action for creating a dispatcher.
                     */
                    enum CreationAction implements PrivilegedAction<Dispatcher> {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * {@inheritDoc}
                         */
                        @SuppressWarnings("unchecked")
                        public Dispatcher run() {
                            try {
                                Class<?> path = Class.forName("java.nio.file.Path");
                                Object[] arguments = (Object[]) Array.newInstance(Class.forName("java.nio.file.CopyOption"), 1);
                                arguments[0] = Enum.valueOf((Class) Class.forName("java.nio.file.StandardCopyOption"), "REPLACE_EXISTING");
                                return new ForJava7CapableVm(File.class.getMethod("toPath"),
                                        Class.forName("java.nio.file.Files").getMethod("copy", path, path, arguments.getClass()),
                                        arguments);
                            } catch (Throwable ignored) {
                                return ForLegacyVm.INSTANCE;
                            }
                        }
                    }

                    /**
                     * A legacy dispatcher that is not capable of NIO.
                     */
                    enum ForLegacyVm implements Dispatcher {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * {@inheritDoc}
                         */
                        public boolean isAlive() {
                            return false;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public void copy(File source, File target) {
                            throw new UnsupportedOperationException("Cannot use NIO2 copy on current VM");
                        }
                    }

                    /**
                     * A dispatcher for VMs that are capable of NIO2.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class ForJava7CapableVm implements Dispatcher {

                        /**
                         * The {@code java.io.File#toPath()} method.
                         */
                        private final Method toPath;

                        /**
                         * The {@code java.nio.Files#copy(Path,Path,CopyOption[])} method.
                         */
                        private final Method copy;

                        /**
                         * The copy options to apply.
                         */
                        private final Object[] copyOptions;

                        /**
                         * Creates a new NIO2 capable dispatcher.
                         *
                         * @param toPath      The {@code java.io.File#toPath()} method.
                         * @param copy        The {@code java.nio.Files#copy(Path,Path,CopyOption[])} method.
                         * @param copyOptions The copy options to apply.
                         */
                        protected ForJava7CapableVm(Method toPath, Method copy, Object[] copyOptions) {
                            this.toPath = toPath;
                            this.copy = copy;
                            this.copyOptions = copyOptions;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public boolean isAlive() {
                            return true;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public void copy(File source, File target) throws IOException {
                            try {
                                copy.invoke(null, toPath.invoke(source), toPath.invoke(target), copyOptions);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot access NIO file copy", exception);
                            } catch (InvocationTargetException exception) {
                                Throwable cause = exception.getCause();
                                if (cause instanceof IOException) {
                                    throw (IOException) cause;
                                } else {
                                    throw new IllegalStateException("Cannot execute NIO file copy", cause);
                                }
                            }
                        }
                    }
                }
            }

            /**
             * Represents a jar file as a target.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForJarFile implements Target {

                /**
                 * The jar file that is represented by this target.
                 */
                private final File file;

                /**
                 * Creates a new target for a jar file.
                 *
                 * @param file The jar file that is represented by this target.
                 */
                public ForJarFile(File file) {
                    this.file = file;
                }

                /**
                 * {@inheritDoc}
                 */
                public Sink write(Manifest manifest) throws IOException {
                    return manifest == null
                            ? new Sink.ForJarOutputStream(new JarOutputStream(new FileOutputStream(file)))
                            : new Sink.ForJarOutputStream(new JarOutputStream(new FileOutputStream(file), manifest));
                }
            }
        }

        /**
         * A dispatcher to execute a plugin engine transformation. A dispatcher will receive all work assignments prior to the invocation
         * of complete. After registering and eventually completing the supplied work, the close method will always be called. Any dispatcher
         * will only be used once and from a single thread.
         */
        interface Dispatcher extends Closeable {

            /**
             * Accepts a new work assignment.
             *
             * @param work  The work to handle prefixed by a preprocessing step.
             * @param eager {@code true} if the processing does not need to be deferred until all preprocessing is complete.
             * @throws IOException If an I/O exception occurs.
             */
            void accept(Callable<? extends Callable<? extends Materializable>> work, boolean eager) throws IOException;

            /**
             * Completes the work being handled.
             *
             * @throws IOException If an I/O exception occurs.
             */
            void complete() throws IOException;

            /**
             * The result of a work assignment that needs to be invoked from the main thread that triggers a dispatchers life-cycle methods.
             */
            interface Materializable {

                /**
                 * Materializes this work result and adds any results to the corresponding collection.
                 *
                 * @param sink        The sink to write any work to.
                 * @param transformed A list of all types that are transformed.
                 * @param failed      A mapping of all types that failed during transformation to the exceptions that explain the failure.
                 * @param unresolved  A list of type names that could not be resolved.
                 * @throws IOException If an I/O exception occurs.
                 */
                void materialize(Target.Sink sink,
                                 List<TypeDescription> transformed,
                                 Map<TypeDescription,
                                 List<Throwable>> failed,
                                 List<String> unresolved) throws IOException;

                /**
                 * A materializable for a successfully transformed type.
                 */
                class ForTransformedElement implements Materializable {

                    /**
                     * The type that has been transformed.
                     */
                    private final DynamicType dynamicType;

                    /**
                     * Creates a new materializable for a successfully transformed type.
                     *
                     * @param dynamicType The type that has been transformed.
                     */
                    protected ForTransformedElement(DynamicType dynamicType) {
                        this.dynamicType = dynamicType;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void materialize(Target.Sink sink,
                                            List<TypeDescription> transformed,
                                            Map<TypeDescription,
                                                    List<Throwable>> failed,
                                            List<String> unresolved) throws IOException {
                        sink.store(dynamicType.getAllTypes());
                        transformed.add(dynamicType.getTypeDescription());
                    }
                }

                /**
                 * A materializable for an element that is retained in its original state.
                 */
                class ForRetainedElement implements Materializable {

                    /**
                     * The retained element.
                     */
                    private final Source.Element element;

                    /**
                     * Creates a new materializable for a retained element.
                     *
                     * @param element The retained element.
                     */
                    protected ForRetainedElement(Source.Element element) {
                        this.element = element;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void materialize(Target.Sink sink,
                                            List<TypeDescription> transformed,
                                            Map<TypeDescription,
                                                    List<Throwable>> failed,
                                            List<String> unresolved) throws IOException {
                        sink.retain(element);
                    }
                }

                /**
                 * A materializable for an element that failed to be transformed.
                 */
                class ForFailedElement implements Materializable {

                    /**
                     * The element for which the transformation failed.
                     */
                    private final Source.Element element;

                    /**
                     * The type description for the represented type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * A non-empty list of errors that occurred when attempting the transformation.
                     */
                    private final List<Throwable> errored;

                    /**
                     * Creates a new materializable for an element that failed to be transformed.
                     *
                     * @param element         The element for which the transformation failed.
                     * @param typeDescription The type description for the represented type.
                     * @param errored         A non-empty list of errors that occurred when attempting the transformation.
                     */
                    protected ForFailedElement(Source.Element element, TypeDescription typeDescription, List<Throwable> errored) {
                        this.element = element;
                        this.typeDescription = typeDescription;
                        this.errored = errored;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void materialize(Target.Sink sink,
                                            List<TypeDescription> transformed,
                                            Map<TypeDescription,
                                                    List<Throwable>> failed,
                                            List<String> unresolved) throws IOException {
                        sink.retain(element);
                        failed.put(typeDescription, errored);
                    }
                }

                /**
                 * A materializable for an element that could not be resolved.
                 */
                class ForUnresolvedElement implements Materializable {

                    /**
                     * The element that could not be resolved.
                     */
                    private final Source.Element element;

                    /**
                     * The name of the type that was deducted for this element.
                     */
                    private final String typeName;

                    /**
                     * Creates a new materializable for an element that could not be resolved.
                     *
                     * @param element  The element that could not be resolved.
                     * @param typeName The name of the type that was deducted for this element.
                     */
                    protected ForUnresolvedElement(Source.Element element, String typeName) {
                        this.element = element;
                        this.typeName = typeName;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public void materialize(Target.Sink sink,
                                            List<TypeDescription> transformed,
                                            Map<TypeDescription,
                                                    List<Throwable>> failed,
                                            List<String> unresolved) throws IOException {
                        sink.retain(element);
                        unresolved.add(typeName);
                    }
                }
            }

            /**
             * A factory that is used for creating a dispatcher that is used for a specific plugin engine application.
             */
            interface Factory {

                /**
                 * Creates a new dispatcher.
                 *
                 * @param sink        The sink to write any work to.
                 * @param transformed A list of all types that are transformed.
                 * @param failed      A mapping of all types that failed during transformation to the exceptions that explain the failure.
                 * @param unresolved  A list of type names that could not be resolved.
                 * @return The dispatcher to use.
                 */
                Dispatcher make(Target.Sink sink,
                                List<TypeDescription> transformed,
                                Map<TypeDescription,
                                        List<Throwable>> failed,
                                List<String> unresolved);
            }

            /**
             * A dispatcher that applies transformation upon discovery.
             */
            class ForSerialTransformation implements Dispatcher {

                /**
                 * The sink to write any work to.
                 */
                private final Target.Sink sink;

                /**
                 * A list of all types that are transformed.
                 */
                private final List<TypeDescription> transformed;

                /**
                 * A mapping of all types that failed during transformation to the exceptions that explain the failure.
                 */
                private final Map<TypeDescription, List<Throwable>> failed;

                /**
                 * A list of type names that could not be resolved.
                 */
                private final List<String> unresolved;

                /**
                 * A list of deferred processings.
                 */
                private final List<Callable<? extends Materializable>> preprocessings;

                /**
                 * Creates a dispatcher for a serial transformation.
                 *
                 * @param sink        The sink to write any work to.
                 * @param transformed A list of all types that are transformed.
                 * @param failed      A mapping of all types that failed during transformation to the exceptions that explain the failure.
                 * @param unresolved  A list of type names that could not be resolved.
                 */
                protected ForSerialTransformation(Target.Sink sink,
                                                  List<TypeDescription> transformed,
                                                  Map<TypeDescription, List<Throwable>> failed,
                                                  List<String> unresolved) {
                    this.sink = sink;
                    this.transformed = transformed;
                    this.failed = failed;
                    this.unresolved = unresolved;
                    preprocessings = new ArrayList<Callable<? extends Materializable>>();
                }

                /**
                 * {@inheritDoc}
                 */
                public void accept(Callable<? extends Callable<? extends Materializable>> work, boolean eager) throws IOException {
                    try {
                        Callable<? extends Materializable> preprocessed = work.call();
                        if (eager) {
                            preprocessed.call().materialize(sink, transformed, failed, unresolved);
                        } else {
                            preprocessings.add(preprocessed);
                        }
                    } catch (Exception exception) {
                        if (exception instanceof IOException) {
                            throw (IOException) exception;
                        } else if (exception instanceof RuntimeException) {
                            throw (RuntimeException) exception;
                        } else {
                            throw new IllegalStateException(exception);
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void complete() throws IOException {
                    for (Callable<? extends Materializable> preprocessing : preprocessings) {
                        if (Thread.interrupted()) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted during plugin engine completion");
                        }
                        try {
                            preprocessing.call().materialize(sink, transformed, failed, unresolved);
                        } catch (Exception exception) {
                            if (exception instanceof IOException) {
                                throw (IOException) exception;
                            } else if (exception instanceof RuntimeException) {
                                throw (RuntimeException) exception;
                            } else {
                                throw new IllegalStateException(exception);
                            }
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    /* do nothing */
                }

                /**
                 * A factory for creating a serial dispatcher.
                 */
                public enum Factory implements Dispatcher.Factory {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public Dispatcher make(Target.Sink sink,
                                           List<TypeDescription> transformed,
                                           Map<TypeDescription,
                                                   List<Throwable>> failed,
                                           List<String> unresolved) {
                        return new ForSerialTransformation(sink, transformed, failed, unresolved);
                    }
                }
            }

            /**
             * A dispatcher that applies transformations within one or more threads in parallel to the default transformer.
             */
            class ForParallelTransformation implements Dispatcher {

                /**
                 * The target sink.
                 */
                private final Target.Sink sink;

                /**
                 * A list of all types that are transformed.
                 */
                private final List<TypeDescription> transformed;

                /**
                 * A mapping of all types that failed during transformation to the exceptions that explain the failure.
                 */
                private final Map<TypeDescription, List<Throwable>> failed;

                /**
                 * A list of type names that could not be resolved.
                 */
                private final List<String> unresolved;

                /**
                 * A completion service for all preprocessings.
                 */
                private final CompletionService<Callable<Materializable>> preprocessings;

                /**
                 * A completion service for all materializers.
                 */
                private final CompletionService<Materializable> materializers;

                /**
                 * A count of deferred processings.
                 */
                private int deferred;

                /**
                 * A collection of futures that are currently scheduled.
                 */
                private final Set<Future<?>> futures;

                /**
                 * Creates a new dispatcher that applies transformations in parallel.
                 *
                 * @param executor    The executor to delegate any work to.
                 * @param sink        The target sink.
                 * @param transformed A list of all types that are transformed.
                 * @param failed      A mapping of all types that failed during transformation to the exceptions that explain the failure.
                 * @param unresolved  A list of type names that could not be resolved.
                 */
                protected ForParallelTransformation(Executor executor,
                                                    Target.Sink sink,
                                                    List<TypeDescription> transformed,
                                                    Map<TypeDescription,
                                                    List<Throwable>> failed,
                                                    List<String> unresolved) {
                    this.sink = sink;
                    this.transformed = transformed;
                    this.failed = failed;
                    this.unresolved = unresolved;
                    preprocessings = new ExecutorCompletionService<Callable<Materializable>>(executor);
                    materializers = new ExecutorCompletionService<Materializable>(executor);
                    futures = new HashSet<Future<?>>();
                }

                /**
                 * {@inheritDoc}
                 */
                @SuppressWarnings("unchecked")
                public void accept(Callable<? extends Callable<? extends Materializable>> work, boolean eager) {
                    if (eager) {
                        futures.add(materializers.submit(new EagerWork(work)));
                    } else {
                        deferred += 1;
                        futures.add(preprocessings.submit((Callable<Callable<Materializable>>) work));
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void complete() throws IOException {
                    try {
                        List<Callable<Materializable>> preprocessings = new ArrayList<Callable<Materializable>>(deferred);
                        while (deferred-- > 0) {
                            Future<Callable<Materializable>> future = this.preprocessings.take();
                            futures.remove(future);
                            preprocessings.add(future.get());
                        }
                        for (Callable<Materializable> preprocessing : preprocessings) {
                            futures.add(materializers.submit(preprocessing));
                        }
                        while (!futures.isEmpty()) {
                            Future<Materializable> future = materializers.take();
                            futures.remove(future);
                            future.get().materialize(sink, transformed, failed, unresolved);
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    } catch (ExecutionException exception) {
                        Throwable cause = exception.getCause();
                        if (cause instanceof IOException) {
                            throw (IOException) cause;
                        } else if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        } else if (cause instanceof Error) {
                            throw (Error) cause;
                        } else {
                            throw new IllegalStateException(cause);
                        }
                    }
                }

                /**
                 * {@inheritDoc}
                 */
                public void close() {
                    for (Future<?> future : futures) {
                        future.cancel(true);
                    }
                }

                /**
                 * A parallel dispatcher that shuts down its executor service upon completion of a plugin engine's application.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class WithThrowawayExecutorService extends ForParallelTransformation {

                    /**
                     * The executor service to delegate any work to.
                     */
                    private final ExecutorService executorService;

                    /**
                     * Creates a new dispatcher that applies transformations in parallel and that closes the supplies executor service.
                     *
                     * @param executorService The executor service to delegate any work to.
                     * @param sink            The target sink.
                     * @param transformed     A list of all types that are transformed.
                     * @param failed          A mapping of all types that failed during transformation to the exceptions that explain the failure.
                     * @param unresolved      A list of type names that could not be resolved.
                     */
                    protected WithThrowawayExecutorService(ExecutorService executorService,
                                                           Target.Sink sink,
                                                           List<TypeDescription> transformed,
                                                           Map<TypeDescription, List<Throwable>> failed,
                                                           List<String> unresolved) {
                        super(executorService, sink, transformed, failed, unresolved);
                        this.executorService = executorService;
                    }

                    @Override
                    public void close() {
                        try {
                            super.close();
                        } finally {
                            executorService.shutdown();
                        }
                    }

                    /**
                     * A factory for a parallel executor service that creates a new executor service on each plugin engine application.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    public static class Factory implements Dispatcher.Factory {

                        /**
                         * The amount of threads to create in the throw-away executor service.
                         */
                        private final int threads;

                        /**
                         * Creates a new factory.
                         *
                         * @param threads The amount of threads to create in the throw-away executor service.
                         */
                        public Factory(int threads) {
                            this.threads = threads;
                        }

                        /**
                         * {@inheritDoc}
                         */
                        public Dispatcher make(Target.Sink sink,
                                               List<TypeDescription> transformed,
                                               Map<TypeDescription, List<Throwable>> failed,
                                               List<String> unresolved) {
                            return new WithThrowawayExecutorService(Executors.newFixedThreadPool(threads), sink, transformed, failed, unresolved);
                        }
                    }
                }

                /**
                 * A factory for a dispatcher that uses a given executor service for parallel dispatching.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                public static class Factory implements Dispatcher.Factory {

                    /**
                     * The executor to use.
                     */
                    private final Executor executor;

                    /**
                     * Creates a new dispatcher factory for parallel dispatching using the supplied executor.
                     *
                     * @param executor The executor to use.
                     */
                    public Factory(Executor executor) {
                        this.executor = executor;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Dispatcher make(Target.Sink sink,
                                           List<TypeDescription> transformed,
                                           Map<TypeDescription, List<Throwable>> failed,
                                           List<String> unresolved) {
                        return new ForParallelTransformation(executor, sink, transformed, failed, unresolved);
                    }
                }

                /**
                 * An eager materialization that does not defer processing after preprocessing.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class EagerWork implements Callable<Materializable> {

                    /**
                     * The work to apply.
                     */
                    private final Callable<? extends Callable<? extends Materializable>> work;

                    /**
                     * Creates an eager work resolution.
                     *
                     * @param work The work to apply.
                     */
                    protected EagerWork(Callable<? extends Callable<? extends Materializable>> work) {
                        this.work = work;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Materializable call() throws Exception {
                        return work.call().call();
                    }
                }
            }
        }

        /**
         * A summary of the application of a {@link Engine} to a source and target.
         */
        class Summary {

            /**
             * A list of all types that were transformed.
             */
            private final List<TypeDescription> transformed;

            /**
             * A mapping of all types that failed during transformation to the exceptions that explain the failure.
             */
            private final Map<TypeDescription, List<Throwable>> failed;

            /**
             * A list of type names that could not be resolved.
             */
            private final List<String> unresolved;

            /**
             * Creates a new summary.
             *
             * @param transformed A list of all types that were transformed.
             * @param failed      A mapping of all types that failed during transformation to the exceptions that explain the failure.
             * @param unresolved  A list of type names that could not be resolved.
             */
            public Summary(List<TypeDescription> transformed, Map<TypeDescription, List<Throwable>> failed, List<String> unresolved) {
                this.transformed = transformed;
                this.failed = failed;
                this.unresolved = unresolved;
            }

            /**
             * Returns a list of all types that were transformed.
             *
             * @return A list of all types that were transformed.
             */
            public List<TypeDescription> getTransformed() {
                return transformed;
            }

            /**
             * Returns a mapping of all types that failed during transformation to the exceptions that explain the failure.
             *
             * @return A mapping of all types that failed during transformation to the exceptions that explain the failure.
             */
            public Map<TypeDescription, List<Throwable>> getFailed() {
                return failed;
            }

            /**
             * Returns a list of type names that could not be resolved.
             *
             * @return A list of type names that could not be resolved.
             */
            public List<String> getUnresolved() {
                return unresolved;
            }

            @Override
            public int hashCode() {
                int result = transformed.hashCode();
                result = 31 * result + failed.hashCode();
                result = 31 * result + unresolved.hashCode();
                return result;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (other == null || getClass() != other.getClass()) {
                    return false;
                }
                Summary summary = (Summary) other;
                return transformed.equals(summary.transformed)
                        && failed.equals(summary.failed)
                        && unresolved.equals(summary.unresolved);
            }
        }

        /**
         * An abstract base implementation of a plugin engine.
         */
        abstract class AbstractBase implements Engine {

            /**
             * {@inheritDoc}
             */
            public Engine withErrorHandlers(ErrorHandler... errorHandler) {
                return withErrorHandlers(Arrays.asList(errorHandler));
            }

            /**
             * {@inheritDoc}
             */
            public Engine withParallelTransformation(int threads) {
                if (threads < 1) {
                    throw new IllegalArgumentException("Number of threads must be positive: " + threads);
                }
                return with(new Dispatcher.ForParallelTransformation.WithThrowawayExecutorService.Factory(threads));
            }

            /**
             * {@inheritDoc}
             */
            public Summary apply(File source, File target, Factory... factory) throws IOException {
                return apply(source, target, Arrays.asList(factory));
            }

            /**
             * {@inheritDoc}
             */
            public Summary apply(File source, File target, List<? extends Factory> factories) throws IOException {
                return apply(source.isDirectory()
                        ? new Source.ForFolder(source)
                        : new Source.ForJarFile(source), target.isDirectory()
                        ? new Target.ForFolder(target)
                        : new Target.ForJarFile(target), factories);
            }

            /**
             * {@inheritDoc}
             */
            public Summary apply(Source source, Target target, Factory... factory) throws IOException {
                return apply(source, target, Arrays.asList(factory));
            }
        }

        /**
         * A default implementation of a plugin engine.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Default extends AbstractBase {

            /**
             * The Byte Buddy instance to use.
             */
            private final ByteBuddy byteBuddy;

            /**
             * The type strategy to use.
             */
            private final TypeStrategy typeStrategy;

            /**
             * The pool strategy to use.
             */
            private final PoolStrategy poolStrategy;

            /**
             * The class file locator to use.
             */
            private final ClassFileLocator classFileLocator;

            /**
             * The listener to use.
             */
            private final Listener listener;

            /**
             * The error handler to use.
             */
            private final ErrorHandler errorHandler;

            /**
             * The dispatcher factory to use.
             */
            private final Dispatcher.Factory dispatcherFactory;

            /**
             * A matcher for types to exclude from transformation.
             */
            private final ElementMatcher.Junction<? super TypeDescription> ignoredTypeMatcher;

            /**
             * Creates a new default plugin engine that rebases types and fails fast and on unresolved types and on live initializers.
             */
            public Default() {
                this(new ByteBuddy());
            }

            /**
             * Creates a new default plugin engine that rebases types and fails fast and on unresolved types and on live initializers.
             *
             * @param byteBuddy The Byte Buddy instance to use.
             */
            public Default(ByteBuddy byteBuddy) {
                this(byteBuddy, TypeStrategy.Default.REBASE);
            }

            /**
             * Creates a new default plugin engine.
             *
             * @param byteBuddy    The Byte Buddy instance to use.
             * @param typeStrategy The type strategy to use.
             */
            protected Default(ByteBuddy byteBuddy, TypeStrategy typeStrategy) {
                this(byteBuddy,
                        typeStrategy,
                        PoolStrategy.Default.FAST,
                        ClassFileLocator.NoOp.INSTANCE,
                        Listener.NoOp.INSTANCE,
                        new ErrorHandler.Compound(ErrorHandler.Failing.FAIL_FAST,
                                ErrorHandler.Enforcing.ALL_TYPES_RESOLVED,
                                ErrorHandler.Enforcing.NO_LIVE_INITIALIZERS),
                        Dispatcher.ForSerialTransformation.Factory.INSTANCE,
                        none());
            }

            /**
             * Creates a new default plugin engine.
             *
             * @param byteBuddy          The Byte Buddy instance to use.
             * @param typeStrategy       The type strategy to use.
             * @param poolStrategy       The pool strategy to use.
             * @param classFileLocator   The class file locator to use.
             * @param listener           The listener to use.
             * @param errorHandler       The error handler to use.
             * @param dispatcherFactory  The dispatcher factory to use.
             * @param ignoredTypeMatcher A matcher for types to exclude from transformation.
             */
            protected Default(ByteBuddy byteBuddy,
                              TypeStrategy typeStrategy,
                              PoolStrategy poolStrategy,
                              ClassFileLocator classFileLocator,
                              Listener listener,
                              ErrorHandler errorHandler,
                              Dispatcher.Factory dispatcherFactory,
                              ElementMatcher.Junction<? super TypeDescription> ignoredTypeMatcher) {
                this.byteBuddy = byteBuddy;
                this.typeStrategy = typeStrategy;
                this.poolStrategy = poolStrategy;
                this.classFileLocator = classFileLocator;
                this.listener = listener;
                this.errorHandler = errorHandler;
                this.dispatcherFactory = dispatcherFactory;
                this.ignoredTypeMatcher = ignoredTypeMatcher;
            }

            /**
             * Creates a plugin engine from an {@link EntryPoint}.
             *
             * @param entryPoint            The entry point to resolve into a plugin engine.
             * @param classFileVersion      The class file version to assume.
             * @param methodNameTransformer The method name transformer to use.
             * @return An appropriate plugin engine.
             */
            public static Engine of(EntryPoint entryPoint, ClassFileVersion classFileVersion, MethodNameTransformer methodNameTransformer) {
                return new Default(entryPoint.byteBuddy(classFileVersion), new TypeStrategy.ForEntryPoint(entryPoint, methodNameTransformer));
            }

            /**
             * Runs a plugin engine using the first and second argument as source and target file location and any additional argument as
             * the fully qualified name of any plugin to apply.
             *
             * @param argument The arguments for running the plugin engine.
             * @throws ClassNotFoundException If a plugin class cannot be found on the system class loader.
             * @throws IOException            If an I/O exception occurs.
             */
            @SuppressWarnings("unchecked")
            public static void main(String... argument) throws ClassNotFoundException, IOException {
                if (argument.length < 2) {
                    throw new IllegalArgumentException("Expected arguments: <source> <target> [<plugin>, ...]");
                }
                List<Plugin.Factory> factories = new ArrayList<Factory>(argument.length - 2);
                for (String plugin : Arrays.asList(argument).subList(2, argument.length)) {
                    factories.add(new Factory.UsingReflection((Class<? extends Plugin>) Class.forName(plugin)));
                }
                new Default().apply(new File(argument[0]), new File(argument[1]), factories);
            }

            /**
             * {@inheritDoc}
             */
            public Engine with(ByteBuddy byteBuddy) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        listener,
                        errorHandler,
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine with(TypeStrategy typeStrategy) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        listener,
                        errorHandler,
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine with(PoolStrategy poolStrategy) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        listener,
                        errorHandler,
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine with(ClassFileLocator classFileLocator) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        new ClassFileLocator.Compound(this.classFileLocator, classFileLocator),
                        listener,
                        errorHandler,
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine with(Listener listener) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        new Listener.Compound(this.listener, listener),
                        errorHandler,
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine withoutErrorHandlers() {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        listener,
                        Listener.NoOp.INSTANCE,
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine withErrorHandlers(List<? extends ErrorHandler> errorHandlers) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        listener,
                        new ErrorHandler.Compound(errorHandlers),
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine with(Dispatcher.Factory dispatcherFactory) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        listener,
                        errorHandler,
                        dispatcherFactory,
                        ignoredTypeMatcher);
            }

            /**
             * {@inheritDoc}
             */
            public Engine ignore(ElementMatcher<? super TypeDescription> matcher) {
                return new Default(byteBuddy,
                        typeStrategy,
                        poolStrategy,
                        classFileLocator,
                        listener,
                        errorHandler,
                        dispatcherFactory,
                        ignoredTypeMatcher.<TypeDescription>or(matcher));
            }

            /**
             * {@inheritDoc}
             */
            public Summary apply(Source source, Target target, List<? extends Plugin.Factory> factories) throws IOException {
                Listener listener = new Listener.Compound(this.listener, new Listener.ForErrorHandler(errorHandler));
                List<TypeDescription> transformed = new ArrayList<TypeDescription>();
                Map<TypeDescription, List<Throwable>> failed = new LinkedHashMap<TypeDescription, List<Throwable>>();
                List<String> unresolved = new ArrayList<String>();
                Throwable rethrown = null;
                List<Plugin> plugins = new ArrayList<Plugin>(factories.size());
                List<WithPreprocessor> preprocessors = new ArrayList<WithPreprocessor>();
                try {
                    for (Plugin.Factory factory : factories) {
                        Plugin plugin = factory.make();
                        plugins.add(plugin);
                        if (plugin instanceof WithPreprocessor) {
                            preprocessors.add((WithPreprocessor) plugin);
                        }
                    }
                    Source.Origin origin = source.read();
                    try {
                        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(origin.getClassFileLocator(), this.classFileLocator);
                        TypePool typePool = poolStrategy.typePool(classFileLocator);
                        Manifest manifest = origin.getManifest();
                        listener.onManifest(manifest);
                        Target.Sink sink = target.write(manifest);
                        try {
                            Dispatcher dispatcher = dispatcherFactory.make(sink, transformed, failed, unresolved);
                            try {
                                for (Source.Element element : origin) {
                                    if (Thread.interrupted()) {
                                        Thread.currentThread().interrupt();
                                        throw new IllegalStateException("Thread interrupted during plugin engine application");
                                    }
                                    String name = element.getName();
                                    while (name.startsWith("/")) {
                                        name = name.substring(1);
                                    }
                                    if (name.endsWith(CLASS_FILE_EXTENSION)) {
                                        dispatcher.accept(new Preprocessor(element,
                                                name.substring(0, name.length() - CLASS_FILE_EXTENSION.length()).replace('/', '.'),
                                                classFileLocator,
                                                typePool,
                                                listener,
                                                plugins,
                                                preprocessors), preprocessors.isEmpty());
                                    } else if (!name.equals(JarFile.MANIFEST_NAME)) {
                                        listener.onResource(name);
                                        sink.retain(element);
                                    }
                                }
                                dispatcher.complete();
                            } finally {
                                dispatcher.close();
                            }
                            if (!failed.isEmpty()) {
                                listener.onError(failed);
                            }
                        } finally {
                            sink.close();
                        }
                    } finally {
                        origin.close();
                    }
                } finally {
                    for (Plugin plugin : plugins) {
                        try {
                            plugin.close();
                        } catch (Throwable throwable) {
                            try {
                                listener.onError(plugin, throwable);
                            } catch (Throwable chained) {
                                rethrown = rethrown == null
                                        ? chained
                                        : rethrown;
                            }
                        }
                    }
                }
                if (rethrown == null) {
                    return new Summary(transformed, failed, unresolved);
                } else if (rethrown instanceof IOException) {
                    throw (IOException) rethrown;
                } else if (rethrown instanceof RuntimeException) {
                    throw (RuntimeException) rethrown;
                } else {
                    throw new IllegalStateException(rethrown);
                }
            }

            /**
             * A preprocessor for a parallel plugin engine.
             */
            private class Preprocessor implements Callable<Callable<? extends Dispatcher.Materializable>> {

                /**
                 * The processed element.
                 */
                private final Source.Element element;

                /**
                 * The name of the processed type.
                 */
                private final String typeName;

                /**
                 * The class file locator to use.
                 */
                private final ClassFileLocator classFileLocator;

                /**
                 * The type pool to use.
                 */
                private final TypePool typePool;

                /**
                 * The listener to notify.
                 */
                private final Listener listener;

                /**
                 * The plugins to apply.
                 */
                private final List<Plugin> plugins;

                /**
                 * The plugins with preprocessors to preprocess.
                 */
                private final List<WithPreprocessor> preprocessors;

                /**
                 * Creates a new preprocessor.
                 *
                 * @param element          The processed element.
                 * @param typeName         The name of the processed type.
                 * @param classFileLocator The class file locator to use.
                 * @param typePool         The type pool to use.
                 * @param listener         The listener to notify.
                 * @param plugins          The plugins to apply.
                 * @param preprocessors    The plugins with preprocessors to preprocess.
                 */
                private Preprocessor(Source.Element element,
                                     String typeName,
                                     ClassFileLocator classFileLocator,
                                     TypePool typePool,
                                     Listener listener,
                                     List<Plugin> plugins,
                                     List<WithPreprocessor> preprocessors) {
                    this.element = element;
                    this.typeName = typeName;
                    this.classFileLocator = classFileLocator;
                    this.typePool = typePool;
                    this.listener = listener;
                    this.plugins = plugins;
                    this.preprocessors = preprocessors;
                }

                /**
                 * {@inheritDoc}
                 */
                public Callable<Dispatcher.Materializable> call() throws Exception {
                    listener.onDiscovery(typeName);
                    TypePool.Resolution resolution = typePool.describe(typeName);
                    if (resolution.isResolved()) {
                        TypeDescription typeDescription = resolution.resolve();
                        try {
                            if (!ignoredTypeMatcher.matches(typeDescription)) {
                                for (WithPreprocessor preprocessor : preprocessors) {
                                    preprocessor.onPreprocess(typeDescription, classFileLocator);
                                }
                                return new Resolved(typeDescription);
                            } else {
                                return new Ignored(typeDescription);
                            }
                        } catch (Throwable throwable) {
                            listener.onComplete(typeDescription);
                            if (throwable instanceof Exception) {
                                throw (Exception) throwable;
                            } else if (throwable instanceof Error) {
                                throw (Error) throwable;
                            } else {
                                throw new IllegalStateException(throwable);
                            }
                        }
                    } else {
                        return new Unresolved();
                    }
                }

                /**
                 * A resolved materializable.
                 */
                private class Resolved implements Callable<Dispatcher.Materializable> {

                    /**
                     * A description of the resolved type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new resolved materializable.
                     *
                     * @param typeDescription A description of the resolved type.
                     */
                    private Resolved(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Dispatcher.Materializable call() {
                        List<Plugin> applied = new ArrayList<Plugin>(), ignored = new ArrayList<Plugin>();
                        List<Throwable> errored = new ArrayList<Throwable>();
                        try {
                            DynamicType.Builder<?> builder = typeStrategy.builder(byteBuddy, typeDescription, classFileLocator);
                            for (Plugin plugin : plugins) {
                                try {
                                    if (plugin.matches(typeDescription)) {
                                        builder = plugin.apply(builder, typeDescription, classFileLocator);
                                        listener.onTransformation(typeDescription, plugin);
                                        applied.add(plugin);
                                    } else {
                                        listener.onIgnored(typeDescription, plugin);
                                        ignored.add(plugin);
                                    }
                                } catch (Throwable throwable) {
                                    listener.onError(typeDescription, plugin, throwable);
                                    errored.add(throwable);
                                }
                            }
                            if (!errored.isEmpty()) {
                                listener.onError(typeDescription, errored);
                                return new Dispatcher.Materializable.ForFailedElement(element, typeDescription, errored);
                            } else if (!applied.isEmpty()) {
                                DynamicType dynamicType = builder.make(TypeResolutionStrategy.Disabled.INSTANCE, typePool);
                                listener.onTransformation(typeDescription, applied);
                                for (Map.Entry<TypeDescription, LoadedTypeInitializer> entry : dynamicType.getLoadedTypeInitializers().entrySet()) {
                                    if (entry.getValue().isAlive()) {
                                        listener.onLiveInitializer(typeDescription, entry.getKey());
                                    }
                                }
                                return new Dispatcher.Materializable.ForTransformedElement(dynamicType);
                            } else {
                                listener.onIgnored(typeDescription, ignored);
                                return new Dispatcher.Materializable.ForRetainedElement(element);
                            }
                        } finally {
                            listener.onComplete(typeDescription);
                        }
                    }
                }

                /**
                 * A materializable for an ignored element.
                 */
                private class Ignored implements Callable<Dispatcher.Materializable> {

                    /**
                     * A description of the ignored type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * A materializable for an ignored element.
                     *
                     * @param typeDescription A description of the ignored type.
                     */
                    private Ignored(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Dispatcher.Materializable call() {
                        try {
                            listener.onIgnored(typeDescription, plugins);
                        } finally {
                            listener.onComplete(typeDescription);
                        }
                        return new Dispatcher.Materializable.ForRetainedElement(element);
                    }
                }

                /**
                 * A materializable that represents an unresolved type.
                 */
                private class Unresolved implements Callable<Dispatcher.Materializable> {

                    /**
                     * {@inheritDoc}
                     */
                    public Dispatcher.Materializable call() {
                        listener.onUnresolved(typeName);
                        return new Dispatcher.Materializable.ForUnresolvedElement(element, typeName);
                    }
                }
            }
        }
    }

    /**
     * A non-operational plugin that does not instrument any type. This plugin does not need to be closed.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class NoOp implements Plugin, Plugin.Factory {

        /**
         * {@inheritDoc}
         */
        public Plugin make() {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public boolean matches(TypeDescription target) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new IllegalStateException("Cannot apply non-operational plugin");
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* do nothing */
        }
    }

    /**
     * An abstract base for a {@link Plugin} that matches types by a given {@link ElementMatcher}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    abstract class ForElementMatcher implements Plugin {

        /**
         * The element matcher to apply.
         */
        private final ElementMatcher<? super TypeDescription> matcher;

        /**
         * Creates a new plugin that matches types using an element matcher.
         *
         * @param matcher The element matcher to apply.
         */
        protected ForElementMatcher(ElementMatcher<? super TypeDescription> matcher) {
            this.matcher = matcher;
        }

        /**
         * {@inheritDoc}
         */
        public boolean matches(TypeDescription target) {
            return matcher.matches(target);
        }
    }
}

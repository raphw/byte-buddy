package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * <p>
 * An agent builder provides a convenience API for defining a
 * <a href="http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html">Java agent</a>. By default,
 * this transformation is applied by rebasing the type if not specified otherwise by setting a
 * {@link TypeStrategy}.
 * </p>
 * <p>
 * When defining several {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s, the agent builder always
 * applies the transformers that were supplied with the last applicable matcher. Therefore, more general transformers
 * should be defined first.
 * </p>
 */
public interface AgentBuilder {

    /**
     * Matches a type being loaded in order to apply the supplied
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     *
     * @param matcher A matcher that decides if the entailed
     *                {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for a type that
     *                is being loaded.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when the given {@code matcher}
     * indicates a match.
     */
    Identified type(RawMatcher matcher);

    /**
     * Matches a type being loaded in order to apply the supplied
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     *
     * @param typeMatcher An {@link net.bytebuddy.matcher.ElementMatcher} that is applied on the type being loaded that
     *                    decides if the entailed
     *                    {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for that
     *                    type.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when the given {@code typeMatcher}
     * indicates a match.
     */
    Identified type(ElementMatcher<? super TypeDescription> typeMatcher);

    /**
     * Matches a type being loaded in order to apply the supplied
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     *
     * @param typeMatcher        An {@link net.bytebuddy.matcher.ElementMatcher} that is applied on the type being
     *                           loaded that decides if the entailed
     *                           {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for
     *                           that type.
     * @param classLoaderMatcher An {@link net.bytebuddy.matcher.ElementMatcher} that is applied to the
     *                           {@link java.lang.ClassLoader} that is loading the type being loaded. This matcher
     *                           is always applied first where the type matcher is not applied in case that this
     *                           matcher does not indicate a match.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when both the given
     * {@code typeMatcher} and {@code classLoaderMatcher} indicate a match.
     */
    Identified type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher);

    /**
     * Defines the given {@link net.bytebuddy.ByteBuddy} instance to be used by the created agent.
     *
     * @param byteBuddy The Byte Buddy instance to be used.
     * @return A new instance of this agent builder which makes use of the given {@code byteBuddy} instance.
     */
    AgentBuilder withByteBuddy(ByteBuddy byteBuddy);

    /**
     * Defines the given {@link net.bytebuddy.agent.builder.AgentBuilder.Listener} to be notified by the created agent.
     * The given listener is notified after any other listener that is already registered. If a listener is registered
     * twice, it is also notified twice.
     *
     * @param listener The listener to be notified.
     * @return A new instance of this agent builder which creates an agent that informs the given listener about
     * events.
     */
    AgentBuilder withListener(Listener listener);

    /**
     * Defines the use of the given binary locator for locating binary data to given class names.
     *
     * @param binaryLocator The binary locator to use.
     * @return A new instance of this agent builder which uses the given binary locator for looking up class files.
     */
    AgentBuilder withBinaryLocator(BinaryLocator binaryLocator);

    /**
     * Defines the use of the given definition handler that determines if a type should be rebased or redefined.
     *
     * @param typeStrategy The definition handler to use.
     * @return A new instance of this agent builder which uses the given definition handler.
     */
    AgentBuilder withTypeStrategy(TypeStrategy typeStrategy);

    /**
     * Enables the use of the given native method prefix for instrumented methods. Note that this prefix is also
     * applied when preserving non-native methods. The use of this prefix is also registered when installing the
     * final agent with an {@link java.lang.instrument.Instrumentation}.
     *
     * @param prefix The prefix to be used.
     * @return A new instance of this agent builder which uses the given native method prefix.
     */
    AgentBuilder withNativeMethodPrefix(String prefix);

    /**
     * Disables the use of a native method prefix for instrumented methods.
     *
     * @return A new instance of this agent builder which does not use a native method prefix.
     */
    AgentBuilder withoutNativeMethodPrefix();

    /**
     * Defines classes to be loaded using the given access control context.
     *
     * @param accessControlContext The access control context to be used for loading classes.
     * @return A new instance of this agent builder which uses the given access control context for class loading.
     */
    AgentBuilder withAccessControlContext(AccessControlContext accessControlContext);

    /**
     * Defines a given initialization strategy to be applied to generated types. An initialization strategy is responsible
     * for setting up a type after it was loaded. This initialization must be performed after the transformation because
     * a Java agent is only invoked before loading a type. By default, the initialization logic is added to a class's type
     * initializer which queries a global object for any objects that are to be injected into the generated type.
     *
     * @param initializationStrategy The initialization strategy to use.
     * @return A new instance of this agent builder that applies the given initialization strategy.
     */
    AgentBuilder withInitializationStrategy(InitializationStrategy initializationStrategy);

    /**
     * Specifies a strategy for modifying existing types.
     *
     * @param redefinitionStrategy The redefinition strategy to apply.
     * @return A new instance of this agent builder that applies the given redefinition strategy.
     */
    AgentBuilder withRedefinitionStrategy(RedefinitionStrategy redefinitionStrategy);

    /**
     * Enables class injection of auxiliary classes into the bootstrap class loader.
     *
     * @param folder          The folder in which jar files of the injected classes are to be stored.
     * @param instrumentation The instrumentation instance that is used for appending jar files to the
     *                        bootstrap class path.
     * @return An agent builder with bootstrap class loader class injection enabled.
     */
    AgentBuilder enableBootstrapInjection(File folder, Instrumentation instrumentation);

    /**
     * Disables injection of auxiliary classes into the bootstrap class path.
     *
     * @return An agent builder with bootstrap class loader class injection disbaled.
     */
    AgentBuilder disableBootstrapInjection();

    /**
     * Creates a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of this
     * agent builder.
     *
     * @return A class file transformer that implements the configuration of this agent builder.
     */
    ClassFileTransformer makeRaw();

    /**
     * Creates and installs a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of
     * this agent builder with a given {@link java.lang.instrument.Instrumentation}. If retransformation is enabled,
     * the installation also causes all loaded types to be retransformed.
     *
     * @param instrumentation The instrumentation on which this agent builder's configuration is to be installed.
     * @return The installed class file transformer.
     */
    ClassFileTransformer installOn(Instrumentation instrumentation);

    /**
     * Creates and installs a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of
     * this agent builder with the Byte Buddy-agent which must be installed prior to calling this method.
     *
     * @return The installed class file transformer.
     */
    ClassFileTransformer installOnByteBuddyAgent();

    /**
     * Describes an {@link net.bytebuddy.agent.builder.AgentBuilder} which was handed a matcher for identifying
     * types to instrumented in order to supply one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s.
     */
    interface Identified {

        /**
         * Applies the given transformer for the already supplied matcher.
         *
         * @param transformer The transformer to apply.
         * @return This agent builder with the transformer being applied when the previously supplied matcher
         * identified a type for instrumentation which also allows for the registration of subsequent transformers.
         */
        Extendable transform(Transformer transformer);

        /**
         * This interface is used to allow for optionally providing several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer} to applied when a matcher identifies a type
         * to be instrumented. Any subsequent transformers are applied in the order they are registered.
         */
        interface Extendable extends AgentBuilder, Identified {
            /* this is merely a unionizing interface that does not declare methods */
        }
    }

    /**
     * A matcher that allows to determine if a {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}
     * should be applied during the execution of a {@link java.lang.instrument.ClassFileTransformer} that was
     * generated by an {@link net.bytebuddy.agent.builder.AgentBuilder}.
     */
    interface RawMatcher {

        /**
         * Decides if the given {@code typeDescription} should be instrumented with the entailed
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s.
         *
         * @param typeDescription     A description of the type to be instrumented.
         * @param classLoader         The class loader of the instrumented type. Might be {@code null} if this class
         *                            loader represents the bootstrap class loader.
         * @param classBeingRedefined The class being redefined which is only not {@code null} if a retransformation
         *                            is applied.
         * @param protectionDomain    The protection domain of the type being transformed.
         * @return {@code true} if the entailed {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should
         * be applied for the given {@code typeDescription}.
         */
        boolean matches(TypeDescription typeDescription,
                        ClassLoader classLoader,
                        Class<?> classBeingRedefined,
                        ProtectionDomain protectionDomain);

        /**
         * A raw matcher implementation that checks a {@link TypeDescription}
         * and its {@link java.lang.ClassLoader} against two suitable matchers in order to determine if the matched
         * type should be instrumented.
         */
        class ForElementMatcherPair implements RawMatcher {

            /**
             * The type matcher to apply to a {@link TypeDescription}.
             */
            private final ElementMatcher<? super TypeDescription> typeMatcher;

            /**
             * The class loader to apply to a {@link java.lang.ClassLoader}.
             */
            private final ElementMatcher<? super ClassLoader> classLoaderMatcher;

            /**
             * Creates a new {@link net.bytebuddy.agent.builder.AgentBuilder.RawMatcher} that only matches the
             * supplied {@link TypeDescription} and its
             * {@link java.lang.ClassLoader} against two matcher in order to decied if an instrumentation should
             * be conducted.
             *
             * @param typeMatcher        The type matcher to apply to a
             *                           {@link TypeDescription}.
             * @param classLoaderMatcher The class loader to apply to a {@link java.lang.ClassLoader}.
             */
            public ForElementMatcherPair(ElementMatcher<? super TypeDescription> typeMatcher,
                                         ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                this.typeMatcher = typeMatcher;
                this.classLoaderMatcher = classLoaderMatcher;
            }

            @Override
            public boolean matches(TypeDescription typeDescription,
                                   ClassLoader classLoader,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                return classLoaderMatcher.matches(classLoader) && typeMatcher.matches(typeDescription);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && classLoaderMatcher.equals(((ForElementMatcherPair) other).classLoaderMatcher)
                        && typeMatcher.equals(((ForElementMatcherPair) other).typeMatcher);
            }

            @Override
            public int hashCode() {
                int result = typeMatcher.hashCode();
                result = 31 * result + classLoaderMatcher.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.RawMatcher.ForElementMatcherPair{" +
                        "typeMatcher=" + typeMatcher +
                        ", classLoaderMatcher=" + classLoaderMatcher +
                        '}';
            }
        }
    }

    /**
     * A type strategy is responsible for creating a type builder for a type that is being instrumented.
     */
    interface TypeStrategy {

        /**
         * Creates a type builder for a given type.
         *
         * @param typeDescription       The type being instrumented.
         * @param byteBuddy             The Byte Buddy configuration.
         * @param classFileLocator      The class file locator to use.
         * @param methodNameTransformer The method name transformer to use.
         * @return A type builder for the given arguments.
         */
        DynamicType.Builder<?> builder(TypeDescription typeDescription,
                                       ByteBuddy byteBuddy,
                                       ClassFileLocator classFileLocator,
                                       MethodRebaseResolver.MethodNameTransformer methodNameTransformer);

        /**
         * Default implementations of type strategies.
         */
        enum Default implements TypeStrategy {

            /**
             * A definition handler that performs a rebasing for all types.
             */
            REBASE {
                @Override
                public DynamicType.Builder<?> builder(TypeDescription typeDescription,
                                                      ByteBuddy byteBuddy,
                                                      ClassFileLocator classFileLocator,
                                                      MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
                    return byteBuddy.rebase(typeDescription, classFileLocator, methodNameTransformer);
                }
            },

            /**
             * A definition handler that performs a redefition for all types.
             */
            REDEFINE {
                @Override
                public DynamicType.Builder<?> builder(TypeDescription typeDescription,
                                                      ByteBuddy byteBuddy,
                                                      ClassFileLocator classFileLocator,
                                                      MethodRebaseResolver.MethodNameTransformer methodNameTransformer) {
                    return byteBuddy.redefine(typeDescription, classFileLocator);
                }
            };

            @Override
            public String toString() {
                return "AgentBuilder.TypeStrategy.Default." + name();
            }
        }
    }

    /**
     * A transformer allows to apply modifications to a {@link net.bytebuddy.dynamic.DynamicType}. Such a modification
     * is then applied to any instrumented type that was matched by the preceding matcher.
     */
    interface Transformer {

        /**
         * Allows for a transformation of a {@link net.bytebuddy.dynamic.DynamicType.Builder}.
         *
         * @param builder         The dynamic builder to transform.
         * @param typeDescription The description of the type currently being instrumented.
         * @return A transformed version of the supplied {@code builder}.
         */
        DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription);

        /**
         * A no-op implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer} that does
         * not modify the supplied dynamic type.
         */
        enum NoOp implements Transformer {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
                return builder;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Transformer.NoOp." + name();
            }
        }

        /**
         * A compound transformer that allows to group several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s as a single transformer.
         */
        class Compound implements Transformer {

            /**
             * The transformers to apply in their application order.
             */
            private final Transformer[] transformer;

            /**
             * Creates a new compound transformer.
             *
             * @param transformer The transformers to apply in their application order.
             */
            public Compound(Transformer... transformer) {
                this.transformer = transformer;
            }

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
                for (Transformer transformer : this.transformer) {
                    builder = transformer.transform(builder, typeDescription);
                }
                return builder;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Arrays.equals(transformer, ((Compound) other).transformer);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(transformer);
            }

            @Override
            public String toString() {
                return "AgentBuilder.Transformer.Compound{" +
                        "transformer=" + Arrays.toString(transformer) +
                        '}';
            }
        }
    }

    /**
     * A binary locator allows to specify how binary data is located by an
     * {@link net.bytebuddy.agent.builder.AgentBuilder}.
     */
    interface BinaryLocator {

        /**
         * Creates a class file locator for the given class loader.
         *
         * @param classLoader The class loader for which a class file locator should be created.
         *                    Can be {@code null} to represent the bootstrap class loader.
         * @return An appropriate class file locator.
         */
        ClassFileLocator classFileLocator(ClassLoader classLoader);

        /**
         * Creates a type pool for a given class file locator.
         *
         * @param classFileLocator The class file locator to use.
         * @param classLoader      The class loader for which the class file locator was created.
         * @return A type pool for the supplied class file locator.
         */
        TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader);

        /**
         * A default implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.BinaryLocator} that
         * is using a {@link net.bytebuddy.pool.TypePool.Default} with a
         * {@link net.bytebuddy.pool.TypePool.CacheProvider.Simple} and a
         * {@link net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader}.
         */
        enum Default implements BinaryLocator {

            /**
             * A binary locator that parses the code segment of each method for extracting information about parameter
             * names even if they are not explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#EXTENDED
             */
            EXTENDED(TypePool.Default.ReaderMode.EXTENDED),

            /**
             * A binary locator that skips the code segment of each method and does therefore not extract information
             * about parameter names. Parameter names are still included if they are explicitly included in a class file.
             *
             * @see net.bytebuddy.pool.TypePool.Default.ReaderMode#FAST
             */
            FAST(TypePool.Default.ReaderMode.FAST);

            /**
             * The reader mode to apply by this binary locator.
             */
            private final TypePool.Default.ReaderMode readerMode;

            /**
             * Creates a new binary locator.
             *
             * @param readerMode The reader mode to apply by this binary locator.
             */
            Default(TypePool.Default.ReaderMode readerMode) {
                this.readerMode = readerMode;
            }

            @Override
            public ClassFileLocator classFileLocator(ClassLoader classLoader) {
                return ClassFileLocator.ForClassLoader.of(classLoader);
            }

            @Override
            public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
                return new TypePool.LazyFacade(new TypePool.Default(new TypePool.CacheProvider.Simple(), classFileLocator, readerMode));
            }

            @Override
            public String toString() {
                return "AgentBuilder.BinaryLocator.Default." + name();
            }
        }

        /**
         * A binary locator that loads referenced classes. It is important to never query this binary locator for
         * the currently instrumented type as this will yield a class loading circularity which aborts any instrumentation
         * with an error.
         */
        enum ClassLoading implements BinaryLocator {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public ClassFileLocator classFileLocator(ClassLoader classLoader) {
                return ClassFileLocator.NoOp.INSTANCE;
            }

            @Override
            public TypePool typePool(ClassFileLocator classFileLocator, ClassLoader classLoader) {
                return new TypePool.LazyFacade(TypePool.ClassLoading.of(classFileLocator, classLoader));
            }

            @Override
            public String toString() {
                return "AgentBuilder.BinaryLocator.ClassLoading." + name();
            }
        }
    }

    /**
     * A listener that is informed about events that occur during an instrumentation process.
     */
    interface Listener {

        /**
         * Invoked right before a successful transformation is applied.
         *
         * @param typeDescription The type that is being transformed.
         * @param dynamicType     The dynamic type that was created.
         */
        void onTransformation(TypeDescription typeDescription, DynamicType dynamicType);

        /**
         * Invoked when a type is not transformed but ignored.
         *
         * @param typeDescription The type being ignored.
         */
        void onIgnored(TypeDescription typeDescription);

        /**
         * Invoked when an error has occurred.
         *
         * @param typeName  The binary name of the instrumented type.
         * @param throwable The occurred error.
         */
        void onError(String typeName, Throwable throwable);

        /**
         * Invoked after a class was attempted to be loaded, independently of its treatment.
         *
         * @param typeName The binary name of the instrumented type.
         */
        void onComplete(String typeName);

        /**
         * A no-op implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.Listener}.
         */
        enum NoOp implements Listener {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public void onTransformation(TypeDescription typeDescription, DynamicType dynamicType) {
                /* do nothing */
            }

            @Override
            public void onIgnored(TypeDescription typeDescription) {
                /* do nothing */
            }

            @Override
            public void onError(String typeName, Throwable throwable) {
                /* do nothing */
            }

            @Override
            public void onComplete(String typeName) {
                /* do nothing */
            }

            @Override
            public String toString() {
                return "AgentBuilder.Listener.NoOp." + name();
            }
        }

        /**
         * A compound listener that allows to group several listeners in one instance.
         */
        class Compound implements Listener {

            /**
             * The listeners that are represented by this compound listener in their application order.
             */
            private final List<? extends Listener> listeners;

            /**
             * Creates a new compound listener.
             *
             * @param listener The listeners to apply in their application order.
             */
            public Compound(Listener... listener) {
                this(Arrays.asList(listener));
            }

            /**
             * Creates a new compound listener.
             *
             * @param listeners The listeners to apply in their application order.
             */
            public Compound(List<? extends Listener> listeners) {
                this.listeners = listeners;
            }

            @Override
            public void onTransformation(TypeDescription typeDescription, DynamicType dynamicType) {
                for (Listener listener : listeners) {
                    listener.onTransformation(typeDescription, dynamicType);
                }
            }

            @Override
            public void onIgnored(TypeDescription typeDescription) {
                for (Listener listener : listeners) {
                    listener.onIgnored(typeDescription);
                }
            }

            @Override
            public void onError(String typeName, Throwable throwable) {
                for (Listener listener : listeners) {
                    listener.onError(typeName, throwable);
                }
            }

            @Override
            public void onComplete(String typeName) {
                for (Listener listener : listeners) {
                    listener.onComplete(typeName);
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && listeners.equals(((Compound) other).listeners);
            }

            @Override
            public int hashCode() {
                return listeners.hashCode();
            }

            @Override
            public String toString() {
                return "AgentBuilder.Listener.Compound{" +
                        "listeners=" + listeners +
                        '}';
            }
        }
    }

    /**
     * An initialization strategy which determines the handling of {@link net.bytebuddy.implementation.LoadedTypeInitializer}s
     * and the loading of auxiliary types.
     */
    interface InitializationStrategy {

        /**
         * Creates a new dispatcher for injecting this initialization strategy during a transformation process.
         *
         * @return The dispatcher to be used.
         */
        Dispatcher dispatcher();

        /**
         * A dispatcher for changing a class file to adapt a self-initialization strategy.
         */
        interface Dispatcher {

            /**
             * Transforms the instrumented type to implement an appropriate initialization strategy.
             *
             * @param builder The builder which should implement the initialization strategy.
             * @return The given {@code builder} with the initialization strategy applied.
             */
            DynamicType.Builder<?> apply(DynamicType.Builder<?> builder);

            /**
             * Registers a loaded type initializer for a type name and class loader pair. The loaded type initializer is created lazily
             * and only on demand.
             *
             * @param name            The name of the type for which the loaded type initializer is to be
             *                        registered.
             * @param classLoader     The class loader of the instrumented type. Might be {@code null} if
             *                        this class loader represents the bootstrap class loader.
             * @param lazyInitializer A constructor for a {@link LoadedTypeInitializer} that fully initializes
             *                        the created type and its potential auxiliary types.
             */
            void register(String name, ClassLoader classLoader, LazyInitializer lazyInitializer);

            /**
             * A constructor for creating a {@link LoadedTypeInitializer} if required.
             */
            interface LazyInitializer {

                /**
                 * Creates the {@link LoadedTypeInitializer}.
                 *
                 * @return A loaded type initializer for the built type.
                 */
                LoadedTypeInitializer resolve();

                /**
                 * Loads the auxiliary types that this entry represents. This might cause the instrumentation to fail if
                 * the auxiliary types reference the currently instrumented type.
                 */
                void loadAuxiliaryTypes();

                /**
                 * A simple implementation of a lazy constructor for a {@link LoadedTypeInitializer} that simply returns a given instance.
                 */
                class Simple implements LazyInitializer {

                    /**
                     * The represented instance.
                     */
                    private final LoadedTypeInitializer loadedTypeInitializer;

                    /**
                     * Creates a new simple initializer constructor.
                     *
                     * @param loadedTypeInitializer The represented instance.
                     */
                    public Simple(LoadedTypeInitializer loadedTypeInitializer) {
                        this.loadedTypeInitializer = loadedTypeInitializer;
                    }

                    @Override
                    public LoadedTypeInitializer resolve() {
                        return loadedTypeInitializer;
                    }

                    @Override
                    public void loadAuxiliaryTypes() {
                        /* do nothing */
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && loadedTypeInitializer.equals(((Simple) other).loadedTypeInitializer);
                    }

                    @Override
                    public int hashCode() {
                        return loadedTypeInitializer.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.InitializationStrategy.Dispatcher.LazyInitializer.Simple{" +
                                "loadedTypeInitializer=" + loadedTypeInitializer +
                                '}';
                    }
                }
            }
        }

        /**
         * A non-initializing initialization strategy.
         */
        enum NoOp implements InitializationStrategy, Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Dispatcher dispatcher() {
                return this;
            }

            @Override
            public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                return builder;
            }

            @Override
            public void register(String name, ClassLoader classLoader, LazyInitializer lazyInitializer) {
                /* do nothing */
            }

            @Override
            public String toString() {
                return "AgentBuilder.InitializationStrategy.NoOp." + name();
            }
        }

        /**
         * An initialization strategy that adds a code block to an instrumented type's type initializer which
         * then calls a specific class that is responsible for the explicit initialization.
         */
        enum SelfInjection implements InitializationStrategy {

            /**
             * The singleton instance.
             */
            INSTANCE(new Random());

            /**
             * A generator for random identification values.
             */
            private final Random random;

            /**
             * Creates the self-injection strategy.
             *
             * @param random A generator for random identification values.
             */
            SelfInjection(Random random) {
                this.random = random;
            }

            @Override
            public InitializationStrategy.Dispatcher dispatcher() {
                return new Dispatcher(random.nextInt());
            }

            @Override
            public String toString() {
                return "AgentBuilder.InitializationStrategy.SelfInjection." + name();
            }

            /**
             * A dispatcher for a self-initialization strategy.
             */
            protected static class Dispatcher implements InitializationStrategy.Dispatcher {

                /**
                 * A random identification for the applied self-initialization.
                 */
                private final int identification;

                /**
                 * Creates a new dispatcher.
                 *
                 * @param identification A random identification for the applied self-initialization.
                 */
                protected Dispatcher(int identification) {
                    this.identification = identification;
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                    return builder.initialize(NexusAccessor.INSTANCE.identifiedBy(identification));
                }

                @Override
                public void register(String name, ClassLoader classLoader, LazyInitializer lazyInitializer) {
                    LoadedTypeInitializer loadedTypeInitializer = lazyInitializer.resolve();
                    if (loadedTypeInitializer.isAlive()) {
                        NexusAccessor.INSTANCE.register(name, classLoader, identification, loadedTypeInitializer);
                    }
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && identification == ((Dispatcher) other).identification;
                }

                @Override
                public int hashCode() {
                    return identification;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.InitializationStrategy.SelfInjection.Dispatcher{" +
                            "identification=" + identification +
                            '}';
                }
            }

            /**
             * An accessor for making sure that the accessed {@link net.bytebuddy.agent.builder.Nexus} is the class that is loaded by the system class loader.
             */
            protected enum NexusAccessor {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                /**
                 * The dispatcher for registering type initializers in the {@link Nexus}.
                 */
                private final Dispatcher dispatcher;

                /**
                 * The {@link ClassLoader#getSystemClassLoader()} method.
                 */
                private final MethodDescription.InDefinedShape getSystemClassLoader;

                /**
                 * The {@link java.lang.ClassLoader#loadClass(String)} method.
                 */
                private final MethodDescription.InDefinedShape loadClass;

                /**
                 * The {@link Integer#valueOf(int)} method.
                 */
                private final MethodDescription.InDefinedShape valueOf;

                /**
                 * The {@link java.lang.Class#getDeclaredMethod(String, Class[])} method.
                 */
                private final MethodDescription getDeclaredMethod;

                /**
                 * The {@link java.lang.reflect.Method#invoke(Object, Object...)} method.
                 */
                private final MethodDescription invokeMethod;

                /**
                 * Creates the singleton accessor.
                 */
                NexusAccessor() {
                    Dispatcher dispatcher;
                    try {
                        TypeDescription nexusType = new TypeDescription.ForLoadedType(Nexus.class);
                        dispatcher = new Dispatcher.Available(new ClassInjector.UsingReflection(ClassLoader.getSystemClassLoader())
                                .inject(Collections.singletonMap(nexusType, ClassFileLocator.ForClassLoader.read(Nexus.class).resolve()))
                                .get(nexusType)
                                .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, Object.class));
                    } catch (Exception exception) {
                        try {
                            dispatcher = new Dispatcher.Available(ClassLoader.getSystemClassLoader()
                                    .loadClass(Nexus.class.getName())
                                    .getDeclaredMethod("register", String.class, ClassLoader.class, int.class, Object.class));
                        } catch (Exception ignored) {
                            dispatcher = new Dispatcher.Unavailable(exception);
                        }
                    }
                    this.dispatcher = dispatcher;
                    getSystemClassLoader = new TypeDescription.ForLoadedType(ClassLoader.class).getDeclaredMethods()
                            .filter(named("getSystemClassLoader").and(takesArguments(0))).getOnly();
                    loadClass = new TypeDescription.ForLoadedType(ClassLoader.class).getDeclaredMethods()
                            .filter(named("loadClass").and(takesArguments(String.class))).getOnly();
                    getDeclaredMethod = new TypeDescription.ForLoadedType(Class.class).getDeclaredMethods()
                            .filter(named("getDeclaredMethod").and(takesArguments(String.class, Class[].class))).getOnly();
                    invokeMethod = new TypeDescription.ForLoadedType(Method.class).getDeclaredMethods()
                            .filter(named("invoke").and(takesArguments(Object.class, Object[].class))).getOnly();
                    valueOf = new TypeDescription.ForLoadedType(Integer.class).getDeclaredMethods()
                            .filter(named("valueOf").and(takesArguments(int.class))).getOnly();
                }

                /**
                 * Registers a type initializer with the class loader's nexus.
                 *
                 * @param name            The name of a type for which a loaded type initializer is registered.
                 * @param classLoader     The class loader for which a loaded type initializer is registered.
                 * @param identification  An identification for the initializer to run.
                 * @param typeInitializer The loaded type initializer to be registered.
                 */
                public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer typeInitializer) {
                    dispatcher.register(name, classLoader, identification, typeInitializer);
                }

                /**
                 * Creates a byte code appender for injecting a self-initializing type initializer block into the generated class.
                 *
                 * @param identification The identification of the initialization.
                 * @return An appropriate byte code appender.
                 */
                public ByteCodeAppender identifiedBy(int identification) {
                    return new InitializationAppender(identification);
                }

                @Override
                public String toString() {
                    return "AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor." + name();
                }

                /**
                 * A dispatcher for registering type initializers in the {@link Nexus}.
                 */
                protected interface Dispatcher {

                    /**
                     * Registers a type initializer with the class loader's nexus.
                     *
                     * @param name            The name of a type for which a loaded type initializer is registered.
                     * @param classLoader     The class loader for which a loaded type initializer is registered.
                     * @param identification  An identification for the initializer to run.
                     * @param typeInitializer The loaded type initializer to be registered.
                     */
                    void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer typeInitializer);

                    /**
                     * An enabled dispatcher for registering a type initializer in a {@link Nexus}.
                     */
                    class Available implements Dispatcher {

                        /**
                         * Indicates that a static method is invoked by reflection.
                         */
                        private static final Object STATIC_METHOD = null;

                        /**
                         * The method for registering a type initializer in the system class loader's {@link Nexus}.
                         */
                        private final Method registration;

                        /**
                         * Creates a new dispatcher.
                         *
                         * @param registration The method for registering a type initializer in the system class loader's {@link Nexus}.
                         */
                        protected Available(Method registration) {
                            this.registration = registration;
                        }

                        @Override
                        public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer typeInitializer) {
                            try {
                                registration.invoke(STATIC_METHOD, name, classLoader, identification, typeInitializer);
                            } catch (IllegalAccessException exception) {
                                throw new IllegalStateException("Cannot register type initializer for " + name, exception);
                            } catch (InvocationTargetException exception) {
                                throw new IllegalStateException("Cannot register type initializer for " + name, exception.getCause());
                            }
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && registration.equals(((Available) other).registration);
                        }

                        @Override
                        public int hashCode() {
                            return registration.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Available{" +
                                    "registration=" + registration +
                                    '}';
                        }
                    }

                    /**
                     * A disabled dispatcher where a {@link Nexus} is not available.
                     */
                    class Unavailable implements Dispatcher {

                        /**
                         * The exception that was raised during the dispatcher initialization.
                         */
                        private final Exception exception;

                        /**
                         * Creates a new disabled dispatcher.
                         *
                         * @param exception The exception that was raised during the dispatcher initialization.
                         */
                        protected Unavailable(Exception exception) {
                            this.exception = exception;
                        }

                        @Override
                        public void register(String name, ClassLoader classLoader, int identification, LoadedTypeInitializer typeInitializer) {
                            throw new IllegalStateException("Could not locate registration method", exception);
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && exception.equals(((Unavailable) other).exception);
                        }

                        @Override
                        public int hashCode() {
                            return exception.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.Dispatcher.Unavailable{" +
                                    "exception=" + exception +
                                    '}';
                        }
                    }
                }

                /**
                 * A byte code appender for invoking a Nexus for initializing the instrumented type.
                 */
                protected static class InitializationAppender implements ByteCodeAppender {

                    /**
                     * The identification for the self-initialization to execute.
                     */
                    private final int identification;

                    /**
                     * Creates a new initialization appender.
                     *
                     * @param identification The identification for the self-initialization to execute.
                     */
                    protected InitializationAppender(int identification) {
                        this.identification = identification;
                    }

                    @Override
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        return new ByteCodeAppender.Simple(new StackManipulation.Compound(
                                MethodInvocation.invoke(NexusAccessor.INSTANCE.getSystemClassLoader),
                                new TextConstant(Nexus.class.getName()),
                                MethodInvocation.invoke(NexusAccessor.INSTANCE.loadClass),
                                new TextConstant("initialize"),
                                ArrayFactory.forType(TypeDescription.CLASS)
                                        .withValues(Arrays.asList(
                                                ClassConstant.of(TypeDescription.CLASS),
                                                ClassConstant.of(new TypeDescription.ForLoadedType(int.class)))),
                                MethodInvocation.invoke(NexusAccessor.INSTANCE.getDeclaredMethod),
                                NullConstant.INSTANCE,
                                ArrayFactory.forType(TypeDescription.OBJECT)
                                        .withValues(Arrays.asList(
                                                ClassConstant.of(instrumentedMethod.getDeclaringType().asErasure()),
                                                new StackManipulation.Compound(
                                                        IntegerConstant.forValue(identification),
                                                        MethodInvocation.invoke(INSTANCE.valueOf)))),
                                MethodInvocation.invoke(NexusAccessor.INSTANCE.invokeMethod),
                                Removal.SINGLE
                        )).apply(methodVisitor, implementationContext, instrumentedMethod);
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        InitializationAppender that = (InitializationAppender) other;
                        return identification == that.identification;
                    }

                    @Override
                    public int hashCode() {
                        return identification;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.InitializationStrategy.SelfInjection.NexusAccessor.InitializationAppender{" +
                                "identification=" + identification +
                                '}';
                    }
                }
            }
        }

        /**
         * An initialization strategy that loads all auxiliary types before loading the instrumented type. This can
         * cause the instrumented type to be loaded prematurely (and to fail the instrumentation) if any auxiliary type
         * is a subtype of the instrumented type or causes the instrumented type to be loaded in any other manner.
         */
        enum Premature implements InitializationStrategy, Dispatcher {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Dispatcher dispatcher() {
                return this;
            }

            @Override
            public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                return builder;
            }

            @Override
            public void register(String name, ClassLoader classLoader, LazyInitializer lazyInitializer) {
                lazyInitializer.loadAuxiliaryTypes();
            }

            @Override
            public String toString() {
                return "AgentBuilder.InitializationStrategy.Premature." + name();
            }
        }
    }

    /**
     * A redefinition strategy regulates how already loaded classes are modified by a built agent.
     */
    enum RedefinitionStrategy {

        /**
         * Disables redefinition such that already loaded classes are not affected by the agent.
         */
        DISABLED {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                return false;
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                throw new IllegalStateException("A disabled redefinition strategy cannot create a collector");
            }
        },

        /**
         * <p>
         * Applies a <b>redefinition</b> to all classes that are already loaded and that would have been transformed if
         * the built agent was registered before they were loaded.
         * </p>
         * <p>
         * <b>Important</b>: If a redefined class was previously instrumented, this instrumentation information is lost
         * during the instrumentation. The redefinition is applied upon the original byte code that is provided by a class
         * loader and not upon the code in its currently transformed format. Use
         * {@link net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy#RETRANSFORMATION} if this is a factual or
         * potential limitation.
         * </p>
         */
        REDEFINITION {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                if (!instrumentation.isRedefineClassesSupported()) {
                    throw new IllegalArgumentException("Cannot redefine classes: " + instrumentation);
                }
                return false;
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                return new Collector.ForRedefinition(transformation);
            }
        },

        /**
         * Applies a <b>retransformation</b> to all classes that are already loaded and that would have been transformed if
         * the built agent was registered before they were loaded.
         */
        RETRANSFORMATION {
            @Override
            protected boolean isRetransforming(Instrumentation instrumentation) {
                if (!instrumentation.isRetransformClassesSupported()) {
                    throw new IllegalArgumentException("Cannot retransform classes: " + instrumentation);
                }
                return true;
            }

            @Override
            protected Collector makeCollector(Default.Transformation transformation) {
                return new Collector.ForRetransformation(transformation);
            }
        };

        /**
         * Indicates if this strategy requires a class file transformer to be registered with a hint to apply the
         * transformer for retransformation.
         *
         * @param instrumentation The instrumentation instance used.
         * @return {@code true} if a class file transformer must be registered with a hint for retransformation.
         */
        protected abstract boolean isRetransforming(Instrumentation instrumentation);

        /**
         * Indicates that this redefinition strategy applies a modification of already loaded classes.
         *
         * @return {@code true} if this redefinition strategy applies a modification of already loaded classes.
         */
        protected boolean isEnabled() {
            return this != DISABLED;
        }

        /**
         * Creates a collector instance that is responsible for collecting loaded classes for potential retransformation.
         *
         * @param transformation The transformation that is registered for the agent.
         * @return A new collector for collecting already loaded classes for transformation.
         */
        protected abstract Collector makeCollector(Default.Transformation transformation);

        @Override
        public String toString() {
            return "AgentBuilder.RedefinitionStrategy." + name();
        }

        /**
         * A collector is responsible for collecting classes that are to be considered for modification.
         */
        protected interface Collector {

            /**
             * Considers a loaded class for modification.
             *
             * @param type The type that is to be considered.
             * @return {@code true} if the class is considered to be redefined.
             */
            boolean consider(Class<?> type);

            /**
             * Applies the represented type modification on all collected types.
             *
             * @param instrumentation            The instrumentation to use.
             * @param byteBuddy                  The Byte Buddy configuration to use.
             * @param binaryLocator              The binary locator to use.
             * @param typeStrategy               The type strategy to use.
             * @param listener                   The listener to report to.
             * @param nativeMethodStrategy       The native method strategy to apply.
             * @param accessControlContext       The access control context to use.
             * @param initializationStrategy     The initialization strategy to use.
             * @param bootstrapInjectionStrategy The bootrstrap injection strategy to use.
             * @throws UnmodifiableClassException If an unmodifiable class is attempted to be modified.
             * @throws ClassNotFoundException     If a class cannot be found while redefining another class.
             */
            void apply(Instrumentation instrumentation,
                       ByteBuddy byteBuddy,
                       BinaryLocator binaryLocator,
                       TypeStrategy typeStrategy,
                       Listener listener,
                       Default.NativeMethodStrategy nativeMethodStrategy,
                       AccessControlContext accessControlContext,
                       InitializationStrategy initializationStrategy,
                       Default.BootstrapInjectionStrategy bootstrapInjectionStrategy) throws UnmodifiableClassException, ClassNotFoundException;

            /**
             * A collector that applies a <b>redefinition</b> of already loaded classes.
             */
            class ForRedefinition implements Collector {

                /**
                 * The transformation of the built agent.
                 */
                private final Default.Transformation transformation;

                /**
                 * A list of already collected redefinitions.
                 */
                private final List<Entry> entries;

                /**
                 * Creates a new collector for a redefinition.
                 *
                 * @param transformation The transformation of the built agent.
                 */
                protected ForRedefinition(Default.Transformation transformation) {
                    this.transformation = transformation;
                    entries = new LinkedList<Entry>();
                }

                @Override
                public boolean consider(Class<?> type) {
                    Default.Transformation.Resolution resolution = transformation.resolve(new TypeDescription.ForLoadedType(type),
                            type.getClassLoader(),
                            type,
                            type.getProtectionDomain());
                    return resolution.isResolved() && entries.add(new Entry(type, resolution));
                }

                @Override
                public void apply(Instrumentation instrumentation,
                                  ByteBuddy byteBuddy,
                                  BinaryLocator binaryLocator,
                                  TypeStrategy typeStrategy,
                                  Listener listener,
                                  Default.NativeMethodStrategy nativeMethodStrategy,
                                  AccessControlContext accessControlContext,
                                  InitializationStrategy initializationStrategy,
                                  Default.BootstrapInjectionStrategy bootstrapInjectionStrategy) throws UnmodifiableClassException, ClassNotFoundException {
                    List<ClassDefinition> classDefinitions = new ArrayList<ClassDefinition>(entries.size());
                    for (Entry entry : entries) {
                        try {
                            classDefinitions.add(entry.resolve(initializationStrategy,
                                    binaryLocator.classFileLocator(entry.getType().getClassLoader()),
                                    typeStrategy,
                                    byteBuddy,
                                    nativeMethodStrategy,
                                    bootstrapInjectionStrategy,
                                    accessControlContext,
                                    listener));
                        } catch (Throwable throwable) {
                            listener.onError(entry.getType().getName(), throwable);
                        } finally {
                            listener.onComplete(entry.getType().getName());
                        }
                    }
                    if (!classDefinitions.isEmpty()) {
                        instrumentation.redefineClasses(classDefinitions.toArray(new ClassDefinition[classDefinitions.size()]));
                    }
                }

                @Override
                public String toString() {
                    return "AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition{" +
                            "transformation=" + transformation +
                            ", entries=" + entries +
                            '}';
                }

                /**
                 * An entry describing a type redefinition.
                 */
                protected static class Entry {

                    /**
                     * The type to be redefined.
                     */
                    private final Class<?> type;

                    /**
                     * The resolved transformation for this type.
                     */
                    private final Default.Transformation.Resolution resolution;

                    /**
                     * @param type       The type to be redefined.
                     * @param resolution The resolved transformation for this type.
                     */
                    protected Entry(Class<?> type, Default.Transformation.Resolution resolution) {
                        this.type = type;
                        this.resolution = resolution;
                    }

                    /**
                     * Returns the type that is being redefined.
                     *
                     * @return The type that is being redefined.
                     */
                    public Class<?> getType() {
                        return type;
                    }

                    /**
                     * Resolves this entry into a fully defined class redefinition.
                     *
                     * @param initializationStrategy     The initialization strategy to use.
                     * @param classFileLocator           The class file locator to use.
                     * @param typeStrategy               The type strategy to use.
                     * @param byteBuddy                  The Byte Buddy configuration to use.
                     * @param nativeMethodStrategy       The native method strategy to use.
                     * @param bootstrapInjectionStrategy The bootstrap injection strategy to use.
                     * @param accessControlContext       The access control context to use.
                     * @param listener                   The listener to report to.
                     * @return An appropriate class definition.
                     */
                    protected ClassDefinition resolve(InitializationStrategy initializationStrategy,
                                                      ClassFileLocator classFileLocator,
                                                      TypeStrategy typeStrategy,
                                                      ByteBuddy byteBuddy,
                                                      Default.NativeMethodStrategy nativeMethodStrategy,
                                                      Default.BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                                      AccessControlContext accessControlContext,
                                                      Listener listener) {
                        return new ClassDefinition(type, resolution.apply(initializationStrategy,
                                classFileLocator,
                                typeStrategy,
                                byteBuddy,
                                nativeMethodStrategy,
                                bootstrapInjectionStrategy,
                                accessControlContext,
                                listener));
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Entry entry = (Entry) other;
                        return type.equals(entry.type) && resolution.equals(entry.resolution);
                    }

                    @Override
                    public int hashCode() {
                        int result = type.hashCode();
                        result = 31 * result + resolution.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.RedefinitionStrategy.Collector.ForRedefinition.Entry{" +
                                "type=" + type +
                                ", resolution=" + resolution +
                                '}';
                    }
                }
            }

            /**
             * A collector that applies a <b>retransformation</b> of already loaded classes.
             */
            class ForRetransformation implements Collector {

                /**
                 * The transformation defined by the built agent.
                 */
                private final Default.Transformation transformation;

                /**
                 * The types that were collected for retransformation.
                 */
                private final List<Class<?>> types;

                /**
                 * Creates a new collector for a retransformation.
                 *
                 * @param transformation The transformation defined by the built agent.
                 */
                protected ForRetransformation(Default.Transformation transformation) {
                    this.transformation = transformation;
                    types = new LinkedList<Class<?>>();
                }

                @Override
                public boolean consider(Class<?> type) {
                    return transformation.resolve(new TypeDescription.ForLoadedType(type),
                            type.getClassLoader(), type, type.getProtectionDomain()).isResolved() && types.add(type);
                }

                @Override
                public void apply(Instrumentation instrumentation,
                                  ByteBuddy byteBuddy,
                                  BinaryLocator binaryLocator,
                                  TypeStrategy typeStrategy,
                                  Listener listener,
                                  Default.NativeMethodStrategy nativeMethodStrategy,
                                  AccessControlContext accessControlContext,
                                  InitializationStrategy initializationStrategy,
                                  Default.BootstrapInjectionStrategy bootstrapInjectionStrategy) throws UnmodifiableClassException {
                    if (!types.isEmpty()) {
                        instrumentation.retransformClasses(types.toArray(new Class<?>[types.size()]));
                    }
                }

                @Override
                public String toString() {
                    return "AgentBuilder.RedefinitionStrategy.Collector.ForRetransformation{" +
                            "transformation=" + transformation +
                            ", types=" + types +
                            '}';
                }
            }
        }
    }

    /**
     * The default implementation of an {@link net.bytebuddy.agent.builder.AgentBuilder}.
     */
    class Default implements AgentBuilder {

        /**
         * The name of the Byte Buddy agent class.
         */
        private static final String BYTE_BUDDY_AGENT_TYPE = "net.bytebuddy.agent.ByteBuddyAgent";

        /**
         * The name of the {@code ByteBuddyAgent} class's method for obtaining an instrumentation.
         */
        private static final String GET_INSTRUMENTATION_METHOD = "getInstrumentation";

        /**
         * Base for access to a reflective member to make the code more readable.
         */
        private static final Object STATIC_METHOD = null;

        /**
         * The value that is to be returned from a {@link java.lang.instrument.ClassFileTransformer} to indicate
         * that no class file transformation is to be applied.
         */
        private static final byte[] NO_TRANSFORMATION = null;

        /**
         * The {@link net.bytebuddy.ByteBuddy} instance to be used.
         */
        private final ByteBuddy byteBuddy;

        /**
         * The binary locator to use.
         */
        private final BinaryLocator binaryLocator;

        /**
         * The definition handler to use.
         */
        private final TypeStrategy typeStrategy;

        /**
         * The listener to notify on transformations.
         */
        private final Listener listener;

        /**
         * The native method strategy to use.
         */
        private final NativeMethodStrategy nativeMethodStrategy;

        /**
         * The access control context to use for loading classes.
         */
        private final AccessControlContext accessControlContext;

        /**
         * The initialization strategy to use for creating classes.
         */
        private final InitializationStrategy initializationStrategy;

        /**
         * The redefinition strategy to apply.
         */
        private final RedefinitionStrategy redefinitionStrategy;

        /**
         * The injection strategy for injecting classes into the bootstrap class loader.
         */
        private final BootstrapInjectionStrategy bootstrapInjectionStrategy;

        /**
         * The transformation object for handling type transformations.
         */
        private final Transformation transformation;

        /**
         * Creates a new default agent builder that uses a default {@link net.bytebuddy.ByteBuddy} instance for
         * creating classes.
         */
        public Default() {
            this(new ByteBuddy());
        }

        /**
         * Creates a new agent builder with default settings.
         *
         * @param byteBuddy The Byte Buddy instance to be used.
         */
        public Default(ByteBuddy byteBuddy) {
            this(nonNull(byteBuddy),
                    BinaryLocator.Default.FAST,
                    TypeStrategy.Default.REBASE,
                    Listener.NoOp.INSTANCE,
                    NativeMethodStrategy.Disabled.INSTANCE,
                    AccessController.getContext(),
                    InitializationStrategy.SelfInjection.INSTANCE,
                    RedefinitionStrategy.DISABLED,
                    BootstrapInjectionStrategy.Disabled.INSTANCE,
                    Transformation.Ignored.INSTANCE);
        }

        /**
         * Creates a new default agent builder.
         *
         * @param byteBuddy                  The Byte Buddy instance to be used.
         * @param binaryLocator              The binary locator to use.
         * @param typeStrategy               The definition handler to use.
         * @param listener                   The listener to notify on transformations.
         * @param nativeMethodStrategy       The native method strategy to apply.
         * @param accessControlContext       The access control context to use for loading classes.
         * @param initializationStrategy     The initialization strategy to use for transformed types.
         * @param redefinitionStrategy       The redefinition strategy to apply.
         * @param bootstrapInjectionStrategy The injection strategy for injecting classes into the bootstrap class loader.
         * @param transformation             The transformation object for handling type transformations.
         */
        protected Default(ByteBuddy byteBuddy,
                          BinaryLocator binaryLocator,
                          TypeStrategy typeStrategy,
                          Listener listener,
                          NativeMethodStrategy nativeMethodStrategy,
                          AccessControlContext accessControlContext,
                          InitializationStrategy initializationStrategy,
                          RedefinitionStrategy redefinitionStrategy,
                          BootstrapInjectionStrategy bootstrapInjectionStrategy,
                          Transformation transformation) {
            this.byteBuddy = byteBuddy;
            this.binaryLocator = binaryLocator;
            this.typeStrategy = typeStrategy;
            this.listener = listener;
            this.nativeMethodStrategy = nativeMethodStrategy;
            this.accessControlContext = accessControlContext;
            this.initializationStrategy = initializationStrategy;
            this.redefinitionStrategy = redefinitionStrategy;
            this.bootstrapInjectionStrategy = bootstrapInjectionStrategy;
            this.transformation = transformation;
        }

        @Override
        public Identified type(RawMatcher matcher) {
            return new Matched(nonNull(matcher), Transformer.NoOp.INSTANCE);
        }

        @Override
        public Identified type(ElementMatcher<? super TypeDescription> typeMatcher) {
            return type(typeMatcher, any());
        }

        @Override
        public Identified type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
            return type(new RawMatcher.ForElementMatcherPair(nonNull(typeMatcher), nonNull(classLoaderMatcher)));
        }

        @Override
        public AgentBuilder withByteBuddy(ByteBuddy byteBuddy) {
            return new Default(nonNull(byteBuddy),
                    binaryLocator,
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withListener(Listener listener) {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    new Listener.Compound(this.listener, nonNull(listener)),
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withTypeStrategy(TypeStrategy typeStrategy) {
            return new Default(byteBuddy,
                    binaryLocator,
                    nonNull(typeStrategy),
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withBinaryLocator(BinaryLocator binaryLocator) {
            return new Default(byteBuddy,
                    nonNull(binaryLocator),
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withNativeMethodPrefix(String prefix) {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    NativeMethodStrategy.ForPrefix.of(prefix),
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withoutNativeMethodPrefix() {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    NativeMethodStrategy.Disabled.INSTANCE,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withAccessControlContext(AccessControlContext accessControlContext) {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withRedefinitionStrategy(RedefinitionStrategy redefinitionStrategy) {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    nonNull(redefinitionStrategy),
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder withInitializationStrategy(InitializationStrategy initializationStrategy) {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    nonNull(initializationStrategy),
                    redefinitionStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public AgentBuilder enableBootstrapInjection(File folder, Instrumentation instrumentation) {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    new BootstrapInjectionStrategy.Enabled(nonNull(folder), nonNull(instrumentation)),
                    transformation);
        }

        @Override
        public AgentBuilder disableBootstrapInjection() {
            return new Default(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    redefinitionStrategy,
                    BootstrapInjectionStrategy.Disabled.INSTANCE,
                    transformation);
        }

        @Override
        public ClassFileTransformer makeRaw() {
            return new ExecutingTransformer(byteBuddy,
                    binaryLocator,
                    typeStrategy,
                    listener,
                    nativeMethodStrategy,
                    accessControlContext,
                    initializationStrategy,
                    bootstrapInjectionStrategy,
                    transformation);
        }

        @Override
        public ClassFileTransformer installOn(Instrumentation instrumentation) {
            ClassFileTransformer classFileTransformer = makeRaw();
            instrumentation.addTransformer(classFileTransformer, redefinitionStrategy.isRetransforming(instrumentation));
            if (nativeMethodStrategy.isEnabled(instrumentation)) {
                instrumentation.setNativeMethodPrefix(classFileTransformer, nativeMethodStrategy.getPrefix());
            }
            if (redefinitionStrategy.isEnabled()) {
                RedefinitionStrategy.Collector collector = redefinitionStrategy.makeCollector(transformation);
                for (Class<?> type : instrumentation.getAllLoadedClasses()) {
                    try {
                        if (!instrumentation.isModifiableClass(type) || !collector.consider(type)) {
                            try {
                                try {
                                    listener.onIgnored(new TypeDescription.ForLoadedType(type));
                                } finally {
                                    listener.onComplete(type.getName());
                                }
                            } catch (Throwable ignored) {
                                // Ignore exceptions that are thrown by listeners to mimic the behavior of a transformation.
                            }
                        }
                    } catch (Throwable throwable) {
                        try {
                            try {
                                listener.onError(type.getName(), throwable);
                            } finally {
                                listener.onComplete(type.getName());
                            }
                        } catch (Throwable ignored) {
                            // Ignore exceptions that are thrown by listeners to mimic the behavior of a transformation.
                        }
                    }
                }
                try {
                    collector.apply(instrumentation,
                            byteBuddy,
                            binaryLocator,
                            typeStrategy,
                            listener,
                            nativeMethodStrategy,
                            accessControlContext,
                            initializationStrategy,
                            bootstrapInjectionStrategy);
                } catch (UnmodifiableClassException exception) {
                    throw new IllegalStateException("Cannot modify at least one class: " + collector, exception);
                } catch (ClassNotFoundException exception) {
                    throw new IllegalStateException("Cannot find at least one class class: " + collector, exception);
                }
            }
            return classFileTransformer;
        }

        @Override
        public ClassFileTransformer installOnByteBuddyAgent() {
            try {
                return installOn((Instrumentation) ClassLoader.getSystemClassLoader()
                        .loadClass(BYTE_BUDDY_AGENT_TYPE)
                        .getDeclaredMethod(GET_INSTRUMENTATION_METHOD)
                        .invoke(STATIC_METHOD));
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", exception);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default aDefault = (Default) other;
            return binaryLocator.equals(aDefault.binaryLocator)
                    && byteBuddy.equals(aDefault.byteBuddy)
                    && listener.equals(aDefault.listener)
                    && nativeMethodStrategy.equals(aDefault.nativeMethodStrategy)
                    && typeStrategy.equals(aDefault.typeStrategy)
                    && accessControlContext.equals(aDefault.accessControlContext)
                    && initializationStrategy == aDefault.initializationStrategy
                    && redefinitionStrategy == aDefault.redefinitionStrategy
                    && bootstrapInjectionStrategy.equals(aDefault.bootstrapInjectionStrategy)
                    && transformation.equals(aDefault.transformation);

        }

        @Override
        public int hashCode() {
            int result = byteBuddy.hashCode();
            result = 31 * result + binaryLocator.hashCode();
            result = 31 * result + listener.hashCode();
            result = 31 * result + typeStrategy.hashCode();
            result = 31 * result + nativeMethodStrategy.hashCode();
            result = 31 * result + accessControlContext.hashCode();
            result = 31 * result + initializationStrategy.hashCode();
            result = 31 * result + redefinitionStrategy.hashCode();
            result = 31 * result + bootstrapInjectionStrategy.hashCode();
            result = 31 * result + transformation.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AgentBuilder.Default{" +
                    "byteBuddy=" + byteBuddy +
                    ", binaryLocator=" + binaryLocator +
                    ", typeStrategy=" + typeStrategy +
                    ", listener=" + listener +
                    ", nativeMethodStrategy=" + nativeMethodStrategy +
                    ", accessControlContext=" + accessControlContext +
                    ", initializationStrategy=" + initializationStrategy +
                    ", redefinitionStrategy=" + redefinitionStrategy +
                    ", bootstrapInjectionStrategy=" + bootstrapInjectionStrategy +
                    ", transformation=" + transformation +
                    '}';
        }

        /**
         * An injection strategy for injecting classes into the bootstrap class loader.
         */
        protected interface BootstrapInjectionStrategy {

            /**
             * Creates an injector for the bootstrap class loader.
             *
             * @param protectionDomain The protection domain to be used.
             * @return A class injector for the bootstrap class loader.
             */
            ClassInjector make(ProtectionDomain protectionDomain);

            /**
             * A disabled bootstrap injection strategy.
             */
            enum Disabled implements BootstrapInjectionStrategy {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public ClassInjector make(ProtectionDomain protectionDomain) {
                    throw new IllegalStateException("Injecting classes into the bootstrap class loader was not enabled");
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.BootstrapInjectionStrategy.Disabled." + name();
                }
            }

            /**
             * An enabled bootstrap injection strategy.
             */
            class Enabled implements BootstrapInjectionStrategy {

                /**
                 * The folder in which jar files are to be saved.
                 */
                private final File folder;

                /**
                 * The instrumentation to use for appending jar files.
                 */
                private final Instrumentation instrumentation;

                /**
                 * Creates a new enabled bootstrap class loader injection strategy.
                 *
                 * @param folder          The folder in which jar files are to be saved.
                 * @param instrumentation The instrumentation to use for appending jar files.
                 */
                public Enabled(File folder, Instrumentation instrumentation) {
                    this.folder = folder;
                    this.instrumentation = instrumentation;
                }

                @Override
                public ClassInjector make(ProtectionDomain protectionDomain) {
                    return ClassInjector.UsingInstrumentation.of(folder, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Enabled enabled = (Enabled) other;
                    return folder.equals(enabled.folder) && instrumentation.equals(enabled.instrumentation);
                }

                @Override
                public int hashCode() {
                    int result = folder.hashCode();
                    result = 31 * result + instrumentation.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.BootstrapInjectionStrategy.Enabled{" +
                            "folder=" + folder +
                            ", instrumentation=" + instrumentation +
                            '}';
                }
            }
        }

        /**
         * A strategy for determining if a native method name prefix should be used when rebasing methods.
         */
        protected interface NativeMethodStrategy {

            /**
             * Determines if this strategy enables name prefixing for native methods.
             *
             * @param instrumentation The instrumentation used.
             * @return {@code true} if this strategy indicates that a native method prefix should be used.
             */
            boolean isEnabled(Instrumentation instrumentation);

            /**
             * Resolves the method name transformer for this strategy.
             *
             * @return A method name transformer for this strategy.
             */
            MethodRebaseResolver.MethodNameTransformer resolve();

            /**
             * Returns the method prefix if the strategy is enabled. This method must only be called if this strategy enables prefixing.
             *
             * @return The method prefix.
             */
            String getPrefix();

            /**
             * A native method strategy that suffixes method names with a random suffix and disables native method rebasement.
             */
            enum Disabled implements NativeMethodStrategy {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public MethodRebaseResolver.MethodNameTransformer resolve() {
                    return MethodRebaseResolver.MethodNameTransformer.Suffixing.withRandomSuffix();
                }

                @Override
                public boolean isEnabled(Instrumentation instrumentation) {
                    return false;
                }

                @Override
                public String getPrefix() {
                    throw new IllegalStateException("A disabled native method strategy does not define a method name prefix");
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.NativeMethodStrategy.Disabled." + name();
                }
            }

            /**
             * A native method strategy that prefixes method names with a fixed value for supporting rebasing of native methods.
             */
            class ForPrefix implements NativeMethodStrategy {

                /**
                 * The method name prefix.
                 */
                private final String prefix;

                /**
                 * Creates a new name prefixing native method strategy.
                 *
                 * @param prefix The method name prefix.
                 */
                protected ForPrefix(String prefix) {
                    this.prefix = prefix;
                }

                /**
                 * Creates a new native method strategy for prefixing method names.
                 *
                 * @param prefix The method name prefix.
                 * @return An appropriate native method strategy.
                 */
                protected static NativeMethodStrategy of(String prefix) {
                    if (prefix.length() == 0) {
                        throw new IllegalArgumentException("A method name prefix must not be the empty string");
                    }
                    return new ForPrefix(prefix);
                }

                @Override
                public MethodRebaseResolver.MethodNameTransformer resolve() {
                    return new MethodRebaseResolver.MethodNameTransformer.Prefixing(prefix);
                }

                @Override
                public boolean isEnabled(Instrumentation instrumentation) {
                    if (!instrumentation.isNativeMethodPrefixSupported()) {
                        throw new IllegalArgumentException("A prefix for native methods is not supported: " + instrumentation);
                    }
                    return true;
                }

                @Override
                public String getPrefix() {
                    return prefix;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass()) && prefix.equals(((ForPrefix) other).prefix);
                }

                @Override
                public int hashCode() {
                    return prefix.hashCode();
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.NativeMethodStrategy.ForPrefix{" +
                            "prefix='" + prefix + '\'' +
                            '}';
                }
            }
        }

        /**
         * A transformation serves as a handler for modifying a class.
         */
        protected interface Transformation {

            /**
             * Resolves an attempted transformation to a specific transformation.
             *
             * @param typeDescription     A description of the type that is to be transformed.
             * @param classLoader         The class loader of the type being transformed.
             * @param classBeingRedefined In case of a type redefinition, the loaded type being transformed or {@code null} if that is not the case.
             * @param protectionDomain    The protection domain of the type being transformed.
             * @return A resolution for the given type.
             */
            Resolution resolve(TypeDescription typeDescription,
                               ClassLoader classLoader,
                               Class<?> classBeingRedefined,
                               ProtectionDomain protectionDomain);

            /**
             * A resolution to a transformation.
             */
            interface Resolution {

                /**
                 * Returns {@code true} if this resolution represents an actual type transformation. If this value is {@code false},
                 * this resolution will not attempt to transform a class.
                 *
                 * @return {@code true} if this resolution attempts to transform a type, {@code false} otherwise.
                 */
                boolean isResolved();

                /**
                 * Transforms a type or returns {@code null} if a type is not to be transformed.
                 *
                 * @param initializationStrategy     The initialization strategy to use.
                 * @param classFileLocator           The class file locator to use.
                 * @param typeStrategy               The definition handler to use.
                 * @param byteBuddy                  The Byte Buddy instance to use.
                 * @param methodNameTransformer      The method name transformer to be used.
                 * @param bootstrapInjectionStrategy The bootstrap injection strategy to be used.
                 * @param accessControlContext       The access control context to be used.
                 * @param listener                   The listener to be invoked to inform about an applied or non-applied transformation.
                 * @return The class file of the transformed class or {@code null} if no transformation is attempted.
                 */
                byte[] apply(InitializationStrategy initializationStrategy,
                             ClassFileLocator classFileLocator,
                             TypeStrategy typeStrategy,
                             ByteBuddy byteBuddy,
                             NativeMethodStrategy methodNameTransformer,
                             BootstrapInjectionStrategy bootstrapInjectionStrategy,
                             AccessControlContext accessControlContext,
                             Listener listener);

                /**
                 * A canonical implementation of a non-resolved resolution.
                 */
                class Unresolved implements Resolution {

                    /**
                     * The type that is not transformed.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new unresolved resolution.
                     *
                     * @param typeDescription The type that is not transformed.
                     */
                    protected Unresolved(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    @Override
                    public boolean isResolved() {
                        return false;
                    }

                    @Override
                    public byte[] apply(InitializationStrategy initializationStrategy,
                                        ClassFileLocator classFileLocator,
                                        TypeStrategy typeStrategy,
                                        ByteBuddy byteBuddy,
                                        NativeMethodStrategy methodNameTransformer,
                                        BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                        AccessControlContext accessControlContext,
                                        Listener listener) {
                        listener.onIgnored(typeDescription);
                        return NO_TRANSFORMATION;
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && typeDescription.equals(((Unresolved) other).typeDescription);
                    }

                    @Override
                    public int hashCode() {
                        return typeDescription.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.Transformation.Resolution.Unresolved{" +
                                "typeDescription=" + typeDescription +
                                '}';
                    }
                }
            }

            /**
             * A transformation that does not attempt to transform any type.
             */
            enum Ignored implements Transformation {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Resolution resolve(TypeDescription typeDescription,
                                          ClassLoader classLoader,
                                          Class<?> classBeingRedefined,
                                          ProtectionDomain protectionDomain) {
                    return new Resolution.Unresolved(typeDescription);
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.Transformation.Ignored." + name();
                }
            }

            /**
             * A simple, active transformation.
             */
            class Simple implements Transformation {

                /**
                 * The raw matcher that is represented by this transformation.
                 */
                private final RawMatcher rawMatcher;

                /**
                 * The transformer that is represented by this transformation.
                 */
                private final Transformer transformer;

                /**
                 * Creates a new transformation.
                 *
                 * @param rawMatcher  The raw matcher that is represented by this transformation.
                 * @param transformer The transformer that is represented by this transformation.
                 */
                protected Simple(RawMatcher rawMatcher, Transformer transformer) {
                    this.rawMatcher = rawMatcher;
                    this.transformer = transformer;
                }

                @Override
                public Transformation.Resolution resolve(TypeDescription typeDescription,
                                                         ClassLoader classLoader,
                                                         Class<?> classBeingRedefined,
                                                         ProtectionDomain protectionDomain) {
                    return rawMatcher.matches(typeDescription, classLoader, classBeingRedefined, protectionDomain)
                            ? new Resolution(typeDescription, classLoader, protectionDomain, transformer)
                            : new Transformation.Resolution.Unresolved(typeDescription);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && rawMatcher.equals(((Simple) other).rawMatcher)
                            && transformer.equals(((Simple) other).transformer);
                }

                @Override
                public int hashCode() {
                    int result = rawMatcher.hashCode();
                    result = 31 * result + transformer.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.Transformation.Simple{" +
                            "rawMatcher=" + rawMatcher +
                            ", transformer=" + transformer +
                            '}';
                }

                /**
                 * A resolution that performs a type transformation.
                 */
                protected static class Resolution implements Transformation.Resolution {

                    /**
                     * A description of the transformed type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * The class loader of the transformed type.
                     */
                    private final ClassLoader classLoader;

                    /**
                     * The protection domain of the transformed type.
                     */
                    private final ProtectionDomain protectionDomain;

                    /**
                     * The transformer to be applied.
                     */
                    private final Transformer transformer;

                    /**
                     * Creates a new active transformation.
                     *
                     * @param typeDescription  A description of the transformed type.
                     * @param classLoader      The class loader of the transformed type.
                     * @param protectionDomain The protection domain of the transformed type.
                     * @param transformer      The transformer to be applied.
                     */
                    protected Resolution(TypeDescription typeDescription,
                                         ClassLoader classLoader,
                                         ProtectionDomain protectionDomain,
                                         Transformer transformer) {
                        this.typeDescription = typeDescription;
                        this.classLoader = classLoader;
                        this.protectionDomain = protectionDomain;
                        this.transformer = transformer;
                    }

                    @Override
                    public boolean isResolved() {
                        return true;
                    }

                    @Override
                    public byte[] apply(InitializationStrategy initializationStrategy,
                                        ClassFileLocator classFileLocator,
                                        TypeStrategy typeStrategy,
                                        ByteBuddy byteBuddy,
                                        NativeMethodStrategy methodNameTransformer,
                                        BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                        AccessControlContext accessControlContext,
                                        Listener listener) {
                        InitializationStrategy.Dispatcher dispatcher = initializationStrategy.dispatcher();
                        DynamicType.Unloaded<?> dynamicType = dispatcher.apply(transformer.transform(typeStrategy.builder(typeDescription,
                                byteBuddy,
                                classFileLocator,
                                methodNameTransformer.resolve()), typeDescription)).make();
                        dispatcher.register(typeDescription.getName(), classLoader, AuxiliaryTypeInitializer.of(bootstrapInjectionStrategy,
                                dynamicType,
                                classLoader,
                                protectionDomain,
                                accessControlContext));
                        listener.onTransformation(typeDescription, dynamicType);
                        return dynamicType.getBytes();
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Resolution that = (Resolution) other;
                        return typeDescription.equals(that.typeDescription)
                                && !(classLoader != null ? !classLoader.equals(that.classLoader) : that.classLoader != null)
                                && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null)
                                && transformer.equals(that.transformer);
                    }

                    @Override
                    public int hashCode() {
                        int result = typeDescription.hashCode();
                        result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
                        result = 31 * result + (protectionDomain != null ? protectionDomain.hashCode() : 0);
                        result = 31 * result + transformer.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.Transformation.Simple.Resolution{" +
                                "typeDescription=" + typeDescription +
                                ", classLoader=" + classLoader +
                                ", protectionDomain=" + protectionDomain +
                                ", transformer=" + transformer +
                                '}';
                    }

                    /**
                     * An {@link InitializationStrategy.Dispatcher.LazyInitializer} that initializes the instrumented type and
                     * loads and initializes all auxiliary types after the instrumented type was loaded.
                     */
                    protected static class AuxiliaryTypeInitializer implements InitializationStrategy.Dispatcher.LazyInitializer {

                        /**
                         * The used bootstrap injection strategy.
                         */
                        private final BootstrapInjectionStrategy bootstrapInjectionStrategy;

                        /**
                         * The instrumented type.
                         */
                        private final TypeDescription instrumentedType;

                        /**
                         * The instrumented type's class loader.
                         */
                        private final ClassLoader classLoader;

                        /**
                         * The protection domain of the instrumented type.
                         */
                        private final ProtectionDomain protectionDomain;

                        /**
                         * The used access control context.
                         */
                        private final AccessControlContext accessControlContext;

                        /**
                         * The auxiliary types mapped to their class file representation.
                         */
                        private final Map<TypeDescription, byte[]> rawAuxiliaryTypes;

                        /**
                         * The instrumented types and auxiliary types mapped to their loaded type initializers.
                         */
                        private final Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers;

                        /**
                         * Creates a new auxiliary type initializer.
                         *
                         * @param bootstrapInjectionStrategy The used bootstrap injection strategy.
                         * @param instrumentedType           The instrumented type.
                         * @param classLoader                The instrumented type's class loader.
                         * @param protectionDomain           The protection domain of the instrumented type.
                         * @param accessControlContext       The used access control context.
                         * @param rawAuxiliaryTypes          The auxiliary types mapped to their class file representation.
                         * @param loadedTypeInitializers     The instrumented types and auxiliary types mapped to their loaded type initializers.
                         */
                        protected AuxiliaryTypeInitializer(BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                                           TypeDescription instrumentedType,
                                                           ClassLoader classLoader,
                                                           ProtectionDomain protectionDomain,
                                                           AccessControlContext accessControlContext,
                                                           Map<TypeDescription, byte[]> rawAuxiliaryTypes,
                                                           Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers) {
                            this.bootstrapInjectionStrategy = bootstrapInjectionStrategy;
                            this.instrumentedType = instrumentedType;
                            this.classLoader = classLoader;
                            this.protectionDomain = protectionDomain;
                            this.accessControlContext = accessControlContext;
                            this.rawAuxiliaryTypes = rawAuxiliaryTypes;
                            this.loadedTypeInitializers = loadedTypeInitializers;
                        }

                        /**
                         * Creates an initializer constructor that is capable of initializing the given dynamic type.
                         *
                         * @param bootstrapInjectionStrategy The bootstrap injection strategy to be used.
                         * @param dynamicType                The dynamic type that is being created.
                         * @param classLoader                The class loader that is loading the instrumented type.
                         * @param protectionDomain           The protection domain to be used.
                         * @param accessControlContext       The access control context to be used.
                         * @return An appropriate initializer constructor.
                         */
                        protected static InitializationStrategy.Dispatcher.LazyInitializer of(BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                                                                              DynamicType dynamicType,
                                                                                              ClassLoader classLoader,
                                                                                              ProtectionDomain protectionDomain,
                                                                                              AccessControlContext accessControlContext) {
                            Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = dynamicType.getLoadedTypeInitializers();
                            return loadedTypeInitializers.size() > 1 // There exist auxiliary classes.
                                    ? new AuxiliaryTypeInitializer(bootstrapInjectionStrategy,
                                    dynamicType.getTypeDescription(),
                                    classLoader,
                                    protectionDomain,
                                    accessControlContext,
                                    dynamicType.getRawAuxiliaryTypes(),
                                    dynamicType.getLoadedTypeInitializers())
                                    : new InitializationStrategy.Dispatcher.LazyInitializer.Simple(loadedTypeInitializers.get(dynamicType.getTypeDescription()));
                        }

                        @Override
                        public LoadedTypeInitializer resolve() {
                            return new InjectingInitializer(instrumentedType,
                                    rawAuxiliaryTypes,
                                    loadedTypeInitializers,
                                    classLoader == null
                                            ? bootstrapInjectionStrategy.make(protectionDomain)
                                            : new ClassInjector.UsingReflection(classLoader, protectionDomain, accessControlContext));
                        }

                        @Override
                        public void loadAuxiliaryTypes() {
                            if (!rawAuxiliaryTypes.isEmpty()) {
                                for (Map.Entry<TypeDescription, Class<?>> auxiliary : (classLoader == null
                                        ? bootstrapInjectionStrategy.make(protectionDomain)
                                        : new ClassInjector.UsingReflection(classLoader, protectionDomain, accessControlContext)).inject(rawAuxiliaryTypes).entrySet()) {
                                    loadedTypeInitializers.get(auxiliary.getKey()).onLoad(auxiliary.getValue());
                                }
                            }
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) return true;
                            if (other == null || getClass() != other.getClass()) return false;
                            AuxiliaryTypeInitializer that = (AuxiliaryTypeInitializer) other;
                            return bootstrapInjectionStrategy.equals(that.bootstrapInjectionStrategy)
                                    && instrumentedType.equals(that.instrumentedType)
                                    && !(classLoader != null ? !classLoader.equals(that.classLoader) : that.classLoader != null)
                                    && !(protectionDomain != null ? !protectionDomain.equals(that.protectionDomain) : that.protectionDomain != null)
                                    && accessControlContext.equals(that.accessControlContext)
                                    && rawAuxiliaryTypes.equals(that.rawAuxiliaryTypes)
                                    && loadedTypeInitializers.equals(that.loadedTypeInitializers);
                        }

                        @Override
                        public int hashCode() {
                            int result = bootstrapInjectionStrategy.hashCode();
                            result = 31 * result + instrumentedType.hashCode();
                            result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
                            result = 31 * result + (protectionDomain != null ? protectionDomain.hashCode() : 0);
                            result = 31 * result + accessControlContext.hashCode();
                            result = 31 * result + rawAuxiliaryTypes.hashCode();
                            result = 31 * result + loadedTypeInitializers.hashCode();
                            return result;
                        }

                        @Override
                        public String toString() {
                            return "AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer{" +
                                    "bootstrapInjectionStrategy=" + bootstrapInjectionStrategy +
                                    ", instrumentedType=" + instrumentedType +
                                    ", classLoader=" + classLoader +
                                    ", protectionDomain=" + protectionDomain +
                                    ", accessControlContext=" + accessControlContext +
                                    ", rawAuxiliaryTypes=" + rawAuxiliaryTypes +
                                    ", loadedTypeInitializers=" + loadedTypeInitializers +
                                    '}';
                        }

                        /**
                         * A type initializer that injects all auxiliary types of the instrumented type.
                         */
                        protected static class InjectingInitializer implements LoadedTypeInitializer {

                            /**
                             * The instrumented type.
                             */
                            private final TypeDescription instrumentedType;

                            /**
                             * The auxiliary types mapped to their class file representation.
                             */
                            private final Map<TypeDescription, byte[]> rawAuxiliaryTypes;

                            /**
                             * The instrumented types and auxiliary types mapped to their loaded type initializers.
                             */
                            private final Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers;

                            /**
                             * The class injector to use.
                             */
                            private final ClassInjector classInjector;

                            /**
                             * Creates a new injection initializer.
                             *
                             * @param instrumentedType       The instrumented type.
                             * @param rawAuxiliaryTypes      The auxiliary types mapped to their class file representation.
                             * @param loadedTypeInitializers The instrumented types and auxiliary types mapped to their loaded type initializers.
                             * @param classInjector          The class injector to use.
                             */
                            protected InjectingInitializer(TypeDescription instrumentedType,
                                                           Map<TypeDescription, byte[]> rawAuxiliaryTypes,
                                                           Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers,
                                                           ClassInjector classInjector) {
                                this.instrumentedType = instrumentedType;
                                this.rawAuxiliaryTypes = rawAuxiliaryTypes;
                                this.loadedTypeInitializers = loadedTypeInitializers;
                                this.classInjector = classInjector;
                            }

                            @Override
                            public void onLoad(Class<?> type) {
                                for (Map.Entry<TypeDescription, Class<?>> auxiliary : classInjector.inject(rawAuxiliaryTypes).entrySet()) {
                                    loadedTypeInitializers.get(auxiliary.getKey()).onLoad(auxiliary.getValue());
                                }
                                loadedTypeInitializers.get(instrumentedType).onLoad(type);
                            }

                            @Override
                            public boolean isAlive() {
                                return true;
                            }

                            @Override
                            public boolean equals(Object o) {
                                if (this == o) return true;
                                if (o == null || getClass() != o.getClass()) return false;
                                InjectingInitializer that = (InjectingInitializer) o;
                                return classInjector.equals(that.classInjector)
                                        && instrumentedType.equals(that.instrumentedType)
                                        && rawAuxiliaryTypes.equals(that.rawAuxiliaryTypes)
                                        && loadedTypeInitializers.equals(that.loadedTypeInitializers);
                            }

                            @Override
                            public int hashCode() {
                                int result = instrumentedType.hashCode();
                                result = 31 * result + rawAuxiliaryTypes.hashCode();
                                result = 31 * result + loadedTypeInitializers.hashCode();
                                result = 31 * result + classInjector.hashCode();
                                return result;
                            }

                            @Override
                            public String toString() {
                                return "AgentBuilder.Default.Transformation.Simple.Resolution.AuxiliaryTypeInitializer.InjectingInitializer{" +
                                        "instrumentedType=" + instrumentedType +
                                        ", rawAuxiliaryTypes=" + rawAuxiliaryTypes +
                                        ", loadedTypeInitializers=" + loadedTypeInitializers +
                                        ", classInjector=" + classInjector +
                                        '}';
                            }
                        }
                    }
                }
            }

            /**
             * A compound transformation that applied several transformation in the given order and applies the first active transformation.
             */
            class Compound implements Transformation {

                /**
                 * The list of transformations to apply in their application order.
                 */
                private final List<? extends Transformation> transformations;

                /**
                 * Creates a new compound transformation.
                 *
                 * @param transformation An array of transformations to apply in their application order.
                 */
                protected Compound(Transformation... transformation) {
                    this(Arrays.asList(transformation));
                }

                /**
                 * Creates a new compound transformation.
                 *
                 * @param transformations A list of transformations to apply in their application order.
                 */
                protected Compound(List<? extends Transformation> transformations) {
                    this.transformations = transformations;
                }

                @Override
                public Resolution resolve(TypeDescription typeDescription,
                                          ClassLoader classLoader,
                                          Class<?> classBeingRedefined,
                                          ProtectionDomain protectionDomain) {
                    for (Transformation transformation : transformations) {
                        Resolution resolution = transformation.resolve(typeDescription, classLoader, classBeingRedefined, protectionDomain);
                        if (resolution.isResolved()) {
                            return resolution;
                        }
                    }
                    return new Resolution.Unresolved(typeDescription);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && transformations.equals(((Compound) other).transformations);
                }

                @Override
                public int hashCode() {
                    return transformations.hashCode();
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.Transformation.Compound{" +
                            "transformations=" + transformations +
                            '}';
                }
            }
        }

        /**
         * A {@link java.lang.instrument.ClassFileTransformer} that implements the enclosing agent builder's
         * configuration.
         */
        protected static class ExecutingTransformer implements ClassFileTransformer {

            /**
             * The Byte Buddy instance to be used.
             */
            private final ByteBuddy byteBuddy;

            /**
             * The binary locator to use.
             */
            private final BinaryLocator binaryLocator;

            /**
             * The definition handler to use.
             */
            private final TypeStrategy typeStrategy;

            /**
             * The listener to notify on transformations.
             */
            private final Listener listener;

            /**
             * The native method strategy to apply.
             */
            private final NativeMethodStrategy nativeMethodStrategy;

            /**
             * The access control context to use for loading classes.
             */
            private final AccessControlContext accessControlContext;

            /**
             * The initialization strategy to use for transformed types.
             */
            private final InitializationStrategy initializationStrategy;

            /**
             * The injection strategy for injecting classes into the bootstrap class loader.
             */
            private final BootstrapInjectionStrategy bootstrapInjectionStrategy;

            /**
             * The transformation object for handling type transformations.
             */
            private final Transformation transformation;

            /**
             * Creates a new class file transformer.
             *
             * @param byteBuddy                  The Byte Buddy instance to be used.
             * @param binaryLocator              The binary locator to use.
             * @param typeStrategy               The definition handler to use.
             * @param listener                   The listener to notify on transformations.
             * @param nativeMethodStrategy       The native method strategy to apply.
             * @param accessControlContext       The access control context to use for loading classes.
             * @param initializationStrategy     The initialization strategy to use for transformed types.
             * @param bootstrapInjectionStrategy The injection strategy for injecting classes into the bootstrap class loader.
             * @param transformation             The transformation object for handling type transformations.
             */
            public ExecutingTransformer(ByteBuddy byteBuddy,
                                        BinaryLocator binaryLocator,
                                        TypeStrategy typeStrategy,
                                        Listener listener,
                                        NativeMethodStrategy nativeMethodStrategy,
                                        AccessControlContext accessControlContext,
                                        InitializationStrategy initializationStrategy,
                                        BootstrapInjectionStrategy bootstrapInjectionStrategy,
                                        Transformation transformation) {
                this.binaryLocator = binaryLocator;
                this.initializationStrategy = initializationStrategy;
                this.typeStrategy = typeStrategy;
                this.byteBuddy = byteBuddy;
                this.nativeMethodStrategy = nativeMethodStrategy;
                this.bootstrapInjectionStrategy = bootstrapInjectionStrategy;
                this.accessControlContext = accessControlContext;
                this.listener = listener;
                this.transformation = transformation;
            }

            @Override
            public byte[] transform(ClassLoader classLoader,
                                    String internalTypeName,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) {
                String binaryTypeName = internalTypeName.replace('/', '.');
                try {
                    ClassFileLocator classFileLocator = ClassFileLocator.Simple.of(binaryTypeName,
                            binaryRepresentation,
                            binaryLocator.classFileLocator(classLoader));
                    return transformation.resolve(classBeingRedefined == null
                                    ? binaryLocator.typePool(classFileLocator, classLoader).describe(binaryTypeName).resolve()
                                    : new TypeDescription.ForLoadedType(classBeingRedefined),
                            classLoader,
                            classBeingRedefined,
                            protectionDomain).apply(initializationStrategy,
                            classFileLocator,
                            typeStrategy,
                            byteBuddy,
                            nativeMethodStrategy,
                            bootstrapInjectionStrategy,
                            accessControlContext,
                            listener);
                } catch (Throwable throwable) {
                    listener.onError(binaryTypeName, throwable);
                    return NO_TRANSFORMATION;
                } finally {
                    listener.onComplete(binaryTypeName);
                }
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                ExecutingTransformer that = (ExecutingTransformer) other;
                return byteBuddy.equals(that.byteBuddy)
                        && binaryLocator.equals(that.binaryLocator)
                        && typeStrategy.equals(that.typeStrategy)
                        && initializationStrategy.equals(that.initializationStrategy)
                        && listener.equals(that.listener)
                        && nativeMethodStrategy.equals(that.nativeMethodStrategy)
                        && bootstrapInjectionStrategy.equals(that.bootstrapInjectionStrategy)
                        && accessControlContext.equals(that.accessControlContext)
                        && transformation.equals(that.transformation);
            }

            @Override
            public int hashCode() {
                int result = byteBuddy.hashCode();
                result = 31 * result + binaryLocator.hashCode();
                result = 31 * result + typeStrategy.hashCode();
                result = 31 * result + initializationStrategy.hashCode();
                result = 31 * result + listener.hashCode();
                result = 31 * result + nativeMethodStrategy.hashCode();
                result = 31 * result + bootstrapInjectionStrategy.hashCode();
                result = 31 * result + accessControlContext.hashCode();
                result = 31 * result + transformation.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default.ExecutingTransformer{" +
                        "byteBuddy=" + byteBuddy +
                        ", binaryLocator=" + binaryLocator +
                        ", typeStrategy=" + typeStrategy +
                        ", initializationStrategy=" + initializationStrategy +
                        ", listener=" + listener +
                        ", nativeMethodStrategy=" + nativeMethodStrategy +
                        ", bootstrapInjectionStrategy=" + bootstrapInjectionStrategy +
                        ", accessControlContext=" + accessControlContext +
                        ", transformation=" + transformation +
                        '}';
            }
        }

        /**
         * A helper class that describes a {@link net.bytebuddy.agent.builder.AgentBuilder.Default} after supplying
         * a {@link net.bytebuddy.agent.builder.AgentBuilder.RawMatcher} such that one or several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s can be supplied.
         */
        protected class Matched implements Identified.Extendable {

            /**
             * The supplied raw matcher.
             */
            private final RawMatcher rawMatcher;

            /**
             * The supplied transformer.
             */
            private final Transformer transformer;

            /**
             * Creates a new matched default agent builder.
             *
             * @param rawMatcher  The supplied raw matcher.
             * @param transformer The supplied transformer.
             */
            protected Matched(RawMatcher rawMatcher, Transformer transformer) {
                this.rawMatcher = rawMatcher;
                this.transformer = transformer;
            }

            @Override
            public Identified.Extendable transform(Transformer transformer) {
                return new Matched(rawMatcher, new Transformer.Compound(this.transformer, nonNull(transformer)));
            }

            @Override
            public Identified type(RawMatcher matcher) {
                return materialize().type(matcher);
            }

            @Override
            public Identified type(ElementMatcher<? super TypeDescription> typeMatcher) {
                return materialize().type(typeMatcher);
            }

            @Override
            public Identified type(ElementMatcher<? super TypeDescription> typeMatcher, ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return materialize().type(typeMatcher, classLoaderMatcher);
            }

            @Override
            public AgentBuilder withByteBuddy(ByteBuddy byteBuddy) {
                return materialize().withByteBuddy(byteBuddy);
            }

            @Override
            public AgentBuilder withListener(Listener listener) {
                return materialize().withListener(listener);
            }

            @Override
            public AgentBuilder withTypeStrategy(TypeStrategy typeStrategy) {
                return materialize().withTypeStrategy(typeStrategy);
            }

            @Override
            public AgentBuilder withBinaryLocator(BinaryLocator binaryLocator) {
                return materialize().withBinaryLocator(binaryLocator);
            }

            @Override
            public AgentBuilder withNativeMethodPrefix(String prefix) {
                return materialize().withNativeMethodPrefix(prefix);
            }

            @Override
            public AgentBuilder withoutNativeMethodPrefix() {
                return materialize().withoutNativeMethodPrefix();
            }

            @Override
            public AgentBuilder withAccessControlContext(AccessControlContext accessControlContext) {
                return materialize().withAccessControlContext(accessControlContext);
            }

            @Override
            public AgentBuilder withInitializationStrategy(InitializationStrategy initializationStrategy) {
                return materialize().withInitializationStrategy(initializationStrategy);
            }

            @Override
            public AgentBuilder withRedefinitionStrategy(RedefinitionStrategy redefinitionStrategy) {
                return materialize().withRedefinitionStrategy(redefinitionStrategy);
            }

            @Override
            public AgentBuilder enableBootstrapInjection(File folder, Instrumentation instrumentation) {
                return materialize().enableBootstrapInjection(folder, instrumentation);
            }

            @Override
            public AgentBuilder disableBootstrapInjection() {
                return materialize().disableBootstrapInjection();
            }

            @Override
            public ClassFileTransformer makeRaw() {
                return materialize().makeRaw();
            }

            @Override
            public ClassFileTransformer installOn(Instrumentation instrumentation) {
                return materialize().installOn(instrumentation);
            }

            @Override
            public ClassFileTransformer installOnByteBuddyAgent() {
                return materialize().installOnByteBuddyAgent();
            }

            /**
             * Materializes the currently described {@link net.bytebuddy.agent.builder.AgentBuilder.Default.Transformation}.
             *
             * @return An agent builder that represents the currently described entry of this instance.
             */
            protected AgentBuilder materialize() {
                return new Default(byteBuddy,
                        binaryLocator,
                        typeStrategy,
                        listener,
                        nativeMethodStrategy,
                        accessControlContext,
                        initializationStrategy,
                        redefinitionStrategy,
                        bootstrapInjectionStrategy,
                        new Transformation.Compound(new Transformation.Simple(rawMatcher, transformer), transformation));
            }

            /**
             * Returns the outer instance.
             *
             * @return The outer instance.
             */
            private Default getOuter() {
                return Default.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && rawMatcher.equals(((Matched) other).rawMatcher)
                        && transformer.equals(((Matched) other).transformer)
                        && Default.this.equals(((Matched) other).getOuter());
            }

            @Override
            public int hashCode() {
                int result = rawMatcher.hashCode();
                result = 31 * result + transformer.hashCode();
                result = 31 * result + Default.this.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default.Matched{" +
                        "rawMatcher=" + rawMatcher +
                        ", transformer=" + transformer +
                        ", agentBuilder=" + Default.this +
                        '}';
            }
        }
    }
}

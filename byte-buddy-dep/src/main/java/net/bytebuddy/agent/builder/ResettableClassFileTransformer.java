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
package net.bytebuddy.agent.builder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Iterator;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A class file transformer that can reset its transformation.
 */
public interface ResettableClassFileTransformer extends ClassFileTransformer {

    /**
     * Creates an iterator over the transformers that are applied for a given type.
     *
     * @param typeDescription     A description of a type.
     * @param classLoader         The type's class loader or {@code null} if the boot loader.
     * @param module              The type's module or {@code null} if the module system is not supported by the current VM.
     * @param classBeingRedefined The class being redefined or {@code null} if the type is not yet loaded.
     * @param protectionDomain    The type's protection domain or {@code null} if not available.
     * @return An iterator over the transformers that are applied by this class file transformer if the given type is discovered.
     */
    Iterator<AgentBuilder.Transformer> iterator(TypeDescription typeDescription,
                                                @MaybeNull ClassLoader classLoader,
                                                @MaybeNull JavaModule module,
                                                @MaybeNull Class<?> classBeingRedefined,
                                                @MaybeNull ProtectionDomain protectionDomain);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation      The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy The redefinition to apply.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation            The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy       The redefinition to apply.
     * @param redefinitionBatchAllocator The batch allocator to use.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    @SuppressWarnings("overloads")
    boolean reset(Instrumentation instrumentation,
                  AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                  AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation               The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy          The redefinition to apply.
     * @param redefinitionDiscoveryStrategy The discovery strategy for the types to reset.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    @SuppressWarnings("overloads")
    boolean reset(Instrumentation instrumentation,
                  AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                  AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation               The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy          The redefinition to apply.
     * @param redefinitionDiscoveryStrategy The discovery strategy for the types to reset.
     * @param redefinitionBatchAllocator    The batch allocator to use.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    boolean reset(Instrumentation instrumentation,
                  AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                  AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                  AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation               The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy          The redefinition to apply.
     * @param redefinitionDiscoveryStrategy The discovery strategy for the types to reset.
     * @param redefinitionListener          The redefinition listener to apply.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    @SuppressWarnings("overloads")
    boolean reset(Instrumentation instrumentation,
                  AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                  AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                  AgentBuilder.RedefinitionStrategy.Listener redefinitionListener);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation            The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy       The redefinition to apply.
     * @param redefinitionBatchAllocator The batch allocator to use.
     * @param redefinitionListener       The redefinition listener to apply.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    @SuppressWarnings("overloads")
    boolean reset(Instrumentation instrumentation,
                  AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                  AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                  AgentBuilder.RedefinitionStrategy.Listener redefinitionListener);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation               The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy          The redefinition to apply.
     * @param redefinitionDiscoveryStrategy The discovery strategy for the types to reset.
     * @param redefinitionBatchAllocator    The batch allocator to use.
     * @param redefinitionListener          The redefinition listener to apply.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    boolean reset(Instrumentation instrumentation,
                  AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                  AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                  AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                  AgentBuilder.RedefinitionStrategy.Listener redefinitionListener);


    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled. If it is not enabled,
     * only the {@link net.bytebuddy.agent.builder.AgentBuilder.InstallationListener} is informed about the
     * resetting without undoing any code changes.
     * </p>
     * <p>
     * <b>Note</b>: A reset class file transformer should not be reinstalled. Instead, the {@link AgentBuilder}
     * which built the transformer should be asked to install a new transformer.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation               The instrumentation instance from which to deregister the transformer.
     * @param classFileTransformer          The actual class file transformer to deregister which might be {@code this} instance or any wrapper.
     * @param redefinitionStrategy          The redefinition to apply.
     * @param redefinitionDiscoveryStrategy The discovery strategy for the types to reset.
     * @param redefinitionBatchAllocator    The batch allocator to use.
     * @param redefinitionListener          The redefinition listener to apply.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
    boolean reset(Instrumentation instrumentation,
                  ResettableClassFileTransformer classFileTransformer,
                  AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                  AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                  AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                  AgentBuilder.RedefinitionStrategy.Listener redefinitionListener);

    /**
     * A {@link ResettableClassFileTransformer} which allows for substitution the actual class file transformer without
     * changes in the order of the transformer chain.
     */
    interface Substitutable extends ResettableClassFileTransformer {

        /**
         * Substitutes the current class file transformer.
         *
         * @param classFileTransformer The class file transformer to use.
         */
        void substitute(ResettableClassFileTransformer classFileTransformer);

        /**
         * Returns the underlying non-substitutable class file transformer.
         *
         * @return The underlying non-substitutable class file transformer.
         */
        ResettableClassFileTransformer unwrap();
    }

    /**
     * An abstract base implementation of a {@link ResettableClassFileTransformer}.
     */
    abstract class AbstractBase implements ResettableClassFileTransformer {

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
            return reset(instrumentation, redefinitionStrategy, AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE);
        }

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator) {
            return reset(instrumentation, redefinitionStrategy, redefinitionBatchAllocator, AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        }

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy) {
            return reset(instrumentation, redefinitionStrategy, redefinitionDiscoveryStrategy, AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        }

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy) {
            return reset(instrumentation, redefinitionStrategy, redefinitionDiscoveryStrategy, redefinitionBatchAllocator, AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        }

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                             AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
            return reset(instrumentation, redefinitionStrategy, redefinitionDiscoveryStrategy, AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE, redefinitionListener);
        }

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                             AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
            return reset(instrumentation, redefinitionStrategy, AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE, redefinitionBatchAllocator, redefinitionListener);
        }

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                             AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
            return reset(instrumentation, this, redefinitionStrategy, redefinitionDiscoveryStrategy, redefinitionBatchAllocator, redefinitionListener);
        }
    }

    /**
     * Implements a resettable class file transformer that allows for the delegation of a transformation. Typically implemented
     * when using a {@link net.bytebuddy.agent.builder.AgentBuilder.TransformerDecorator}.
     */
    @HashCodeAndEqualsPlugin.Enhance
    abstract class WithDelegation extends AbstractBase {

        /**
         * The class file transformer to delegate to.
         */
        protected final ResettableClassFileTransformer classFileTransformer;

        /**
         * Creates a new delegating resettable class file transformer.
         *
         * @param classFileTransformer The class file transformer to delegate to.
         */
        protected WithDelegation(ResettableClassFileTransformer classFileTransformer) {
            this.classFileTransformer = classFileTransformer;
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
         * Creates a resettable class file transformer that wraps another transformer and adds a callback to the
         * beginning and end of each transformation. If the module system is supported on the current JVM, the supplied
         * module is retained.
         *
         * @param classFileTransformer The class file transformer to delegate to.
         * @param callback             The callback to invoke.
         * @return A resettable class file transformer that delegates while also invoking the callback.
         */
        public static ResettableClassFileTransformer of(ResettableClassFileTransformer classFileTransformer, Callback<?> callback) {
            return WithCallback.DISPATCHER.make(classFileTransformer, callback);
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<AgentBuilder.Transformer> iterator(TypeDescription typeDescription,
                                                           @MaybeNull ClassLoader classLoader,
                                                           @MaybeNull JavaModule module,
                                                           @MaybeNull Class<?> classBeingRedefined,
                                                           @MaybeNull ProtectionDomain protectionDomain) {
            return classFileTransformer.iterator(typeDescription, classLoader, module, classBeingRedefined, protectionDomain);
        }

        /**
         * {@inheritDoc}
         */
        public boolean reset(Instrumentation instrumentation,
                             ResettableClassFileTransformer classFileTransformer,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                             AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
            return this.classFileTransformer.reset(instrumentation,
                    classFileTransformer,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    redefinitionListener);
        }

        /**
         * A standard implementation of a substitutable {@link ResettableClassFileTransformer}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Substitutable extends AbstractBase implements ResettableClassFileTransformer.Substitutable {

            /**
             * A dispatcher for invoking the correct transformer method.
             */
            private static final Factory DISPATCHER = doPrivileged(Factory.CreationAction.INSTANCE);

            /**
             * The class file transformer to delegate to.
             */
            protected volatile ResettableClassFileTransformer classFileTransformer;

            /**
             * Creates a new delegating resettable class file transformer.
             *
             * @param classFileTransformer The class file transformer to delegate to.
             */
            protected Substitutable(ResettableClassFileTransformer classFileTransformer) {
                this.classFileTransformer = classFileTransformer;
            }

            /**
             * Creates a new substitutable class file transformer of another class file transformer.
             *
             * @param classFileTransformer The class file transformer to wrap.
             * @return A substitutable version of the supplied class file transformer.
             */
            public static Substitutable of(ResettableClassFileTransformer classFileTransformer) {
                return DISPATCHER.make(classFileTransformer);
            }

            /**
             * {@inheritDoc}
             */
            public void substitute(ResettableClassFileTransformer classFileTransformer) {
                while (classFileTransformer instanceof Substitutable) {
                    classFileTransformer = ((Substitutable) classFileTransformer).unwrap();
                }
                this.classFileTransformer = classFileTransformer;
            }

            /**
             * {@inheritDoc}
             */
            public ResettableClassFileTransformer unwrap() {
                return classFileTransformer;
            }

            /**
             * {@inheritDoc}
             */
            public Iterator<AgentBuilder.Transformer> iterator(TypeDescription typeDescription,
                                                               @MaybeNull ClassLoader classLoader,
                                                               @MaybeNull JavaModule module,
                                                               @MaybeNull Class<?> classBeingRedefined,
                                                               @MaybeNull ProtectionDomain protectionDomain) {
                return classFileTransformer.iterator(typeDescription, classLoader, module, classBeingRedefined, protectionDomain);
            }

            /**
             * {@inheritDoc}
             */
            public boolean reset(Instrumentation instrumentation,
                                 ResettableClassFileTransformer classFileTransformer,
                                 AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                                 AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                                 AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                                 AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
                return this.classFileTransformer.reset(instrumentation,
                        classFileTransformer,
                        redefinitionStrategy,
                        redefinitionDiscoveryStrategy,
                        redefinitionBatchAllocator,
                        redefinitionListener);
            }

            /**
             * {@inheritDoc}
             */
            public byte[] transform(@MaybeNull ClassLoader classLoader,
                                    @MaybeNull String internalName,
                                    @MaybeNull Class<?> classBeingRedefined,
                                    @MaybeNull ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) throws IllegalClassFormatException {
                return classFileTransformer.transform(classLoader, internalName, classBeingRedefined, protectionDomain, binaryRepresentation);
            }

            /**
             * A factory for creating a subclass of {@link WithDelegation.Substitutable} that supports the module system, if available.
             */
            interface Factory {

                /**
                 * Creates a new substitutable class file transformer.
                 *
                 * @param classFileTransformer The class file transformer to wrap.
                 * @return The wrapping class file transformer.
                 */
                Substitutable make(ResettableClassFileTransformer classFileTransformer);

                /**
                 * An action to create a suitable factory.
                 */
                enum CreationAction implements PrivilegedAction<Factory> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback.")
                    public Factory run() {
                        try {
                            return new ForJava9CapableVm(new ByteBuddy()
                                    .with(TypeValidation.DISABLED)
                                    .subclass(WithDelegation.Substitutable.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                                    .name(WithDelegation.Substitutable.class.getName() + "$ByteBuddy$ModuleSupport")
                                    .method(named("transform").and(takesArgument(0, JavaType.MODULE.load())))
                                    .intercept(MethodCall.invoke(ClassFileTransformer.class.getMethod("transform",
                                            JavaType.MODULE.load(),
                                            ClassLoader.class,
                                            String.class,
                                            Class.class,
                                            ProtectionDomain.class,
                                            byte[].class)).onField("classFileTransformer").withAllArguments())
                                    .make()
                                    .load(WithDelegation.Substitutable.class.getClassLoader(),
                                            ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.with(WithDelegation.Substitutable.class.getProtectionDomain()))
                                    .getLoaded()
                                    .getConstructor(ResettableClassFileTransformer.class));
                        } catch (Exception ignored) {
                            return ForLegacyVm.INSTANCE;
                        }
                    }
                }

                /**
                 * A factory for creating a substitutable class file transformer when the module system is supported.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForJava9CapableVm implements Factory {

                    /**
                     * The constructor to invoke.
                     */
                    private final Constructor<? extends Substitutable> substitutable;

                    /**
                     * Creates a new Java 9 capable factory.
                     *
                     * @param substitutable The constructor to invoke.
                     */
                    protected ForJava9CapableVm(Constructor<? extends Substitutable> substitutable) {
                        this.substitutable = substitutable;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Substitutable make(ResettableClassFileTransformer classFileTransformer) {
                        try {
                            return substitutable.newInstance(classFileTransformer);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access " + substitutable, exception);
                        } catch (InstantiationException exception) {
                            throw new IllegalStateException("Cannot instantiate " + substitutable.getDeclaringClass(), exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Cannot invoke " + substitutable, exception.getTargetException());
                        }
                    }
                }

                /**
                 * A factory for a substitutable class file transformer when the module system is not supported.
                 */
                enum ForLegacyVm implements Factory {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public Substitutable make(ResettableClassFileTransformer classFileTransformer) {
                        return new WithDelegation.Substitutable(classFileTransformer);
                    }
                }
            }
        }

        /**
         * A class file transformer with a callback.
         *
         * @param <T> The type of the value that is passed between the callback methods.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class WithCallback<T> extends WithDelegation {

            /**
             * The dispatcher that creates the wrapper instance.
             */
            private static final Factory DISPATCHER = doPrivileged(Factory.CreationAction.INSTANCE);

            /**
             * The callback to invoke.
             */
            private final Callback<T> callback;

            /**
             * Creates a delegating class file transformer with callback.
             *
             * @param classFileTransformer The class file transformer to delegate to.
             * @param callback             The callback to invoke.
             */
            protected WithCallback(ResettableClassFileTransformer classFileTransformer, Callback<T> callback) {
                super(classFileTransformer);
                this.callback = callback;
            }

            /**
             * {@inheritDoc}
             */
            @MaybeNull
            public byte[] transform(@MaybeNull ClassLoader classLoader,
                                    @MaybeNull String internalName,
                                    @MaybeNull Class<?> classBeingRedefined,
                                    @MaybeNull ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) throws IllegalClassFormatException {
                T value = callback.onBeforeTransform(null, classLoader, internalName, classBeingRedefined, protectionDomain, binaryRepresentation);
                try {
                    return classFileTransformer.transform(classLoader, internalName, classBeingRedefined, protectionDomain, binaryRepresentation);
                } finally {
                    callback.onAfterTransform(value, null, classLoader, internalName, classBeingRedefined, protectionDomain, binaryRepresentation);
                }
            }

            /**
             * A method to delegate to when the module system is supported on the current JVM.
             *
             * @param rawModule            The instrumented class's {@code java.lang.Module}.
             * @param classLoader          The type's class loader or {@code null} if the type is loaded by the bootstrap loader.
             * @param internalName         The internal name of the instrumented class.
             * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The instrumented type's protection domain or {@code null} if not available.
             * @param binaryRepresentation The class file of the instrumented class in its current state.
             * @return The binary representation of the transformed class file or {@code null} if no transformation should be applied.
             * @throws IllegalClassFormatException If the class file was found invalid.
             */
            @MaybeNull
            protected byte[] transform(Object rawModule,
                                       @MaybeNull ClassLoader classLoader,
                                       @MaybeNull String internalName,
                                       @MaybeNull Class<?> classBeingRedefined,
                                       @MaybeNull ProtectionDomain protectionDomain,
                                       byte[] binaryRepresentation) throws IllegalClassFormatException {
                JavaModule module = JavaModule.of(rawModule);
                T value = callback.onBeforeTransform(module, classLoader, internalName, classBeingRedefined, protectionDomain, binaryRepresentation);
                try {
                    return doTransform(rawModule, classLoader, internalName, classBeingRedefined, protectionDomain, binaryRepresentation);
                } finally {
                    callback.onAfterTransform(value, module, classLoader, internalName, classBeingRedefined, protectionDomain, binaryRepresentation);
                }
            }

            /**
             * Callback between the transformation callbacks.
             *
             * @param rawModule            The instrumented class's {@code java.lang.Module}.
             * @param classLoader          The type's class loader or {@code null} if the type is loaded by the bootstrap loader.
             * @param internalName         The internal name of the instrumented class.
             * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The instrumented type's protection domain or {@code null} if not available.
             * @param binaryRepresentation The class file of the instrumented class in its current state.
             * @return The binary representation of the transformed class file or {@code null} if no transformation should be applied.
             * @throws IllegalClassFormatException If the class file was found invalid.
             */
            @MaybeNull
            protected byte[] doTransform(Object rawModule,
                                         @MaybeNull ClassLoader classLoader,
                                         @MaybeNull String internalName,
                                         @MaybeNull Class<?> classBeingRedefined,
                                         @MaybeNull ProtectionDomain protectionDomain,
                                         byte[] binaryRepresentation) throws IllegalClassFormatException {
                throw new UnsupportedOperationException();
            }

            /**
             * A factory that creates a resettable class file transformer depending on the availability of the module system.
             */
            interface Factory {

                /**
                 * Creates a new substitutable class file transformer.
                 *
                 * @param classFileTransformer The class file transformer to wrap.
                 * @param callback             The callback to invoke.
                 * @return The wrapping class file transformer.
                 */
                ResettableClassFileTransformer make(ResettableClassFileTransformer classFileTransformer, Callback<?> callback);

                /**
                 * An action to create a suitable factory.
                 */
                enum CreationAction implements PrivilegedAction<Factory> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback.")
                    public Factory run() {
                        try {
                            return new Factory.ForJava9CapableVm(new ByteBuddy()
                                    .with(TypeValidation.DISABLED)
                                    .subclass(WithCallback.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                                    .name(WithCallback.class.getName() + "$ByteBuddy$ModuleSupport")
                                    .method(named("transform").and(takesArguments(JavaType.MODULE.load(),
                                            ClassLoader.class,
                                            String.class,
                                            Class.class,
                                            ProtectionDomain.class,
                                            byte[].class)))
                                    .intercept(MethodCall.invoke(WithCallback.class.getDeclaredMethod("transform",
                                            Object.class,
                                            ClassLoader.class,
                                            String.class,
                                            Class.class,
                                            ProtectionDomain.class,
                                            byte[].class)).withAllArguments())
                                    .method(named("doTransform").and(takesArguments(Object.class,
                                            ClassLoader.class,
                                            String.class,
                                            Class.class,
                                            ProtectionDomain.class,
                                            byte[].class)))
                                    .intercept(MethodCall.invoke(ClassFileTransformer.class.getMethod("transform",
                                            JavaType.MODULE.load(),
                                            ClassLoader.class,
                                            String.class,
                                            Class.class,
                                            ProtectionDomain.class,
                                            byte[].class)).onField("classFileTransformer").withAllArguments().withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                                    .make()
                                    .load(WithCallback.class.getClassLoader(),
                                            ClassLoadingStrategy.Default.WRAPPER_PERSISTENT.with(WithCallback.class.getProtectionDomain()))
                                    .getLoaded()
                                    .getConstructor(ResettableClassFileTransformer.class, Callback.class));
                        } catch (Exception ignored) {
                            return Factory.ForLegacyVm.INSTANCE;
                        }
                    }
                }

                /**
                 * A factory for creating a substitutable class file transformer when the module system is supported.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForJava9CapableVm implements Factory {

                    /**
                     * The constructor to invoke.
                     */
                    private final Constructor<? extends ResettableClassFileTransformer> withCallback;

                    /**
                     * Creates a new Java 9 capable factory.
                     *
                     * @param withCallback The constructor to invoke.
                     */
                    protected ForJava9CapableVm(Constructor<? extends ResettableClassFileTransformer> withCallback) {
                        this.withCallback = withCallback;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public ResettableClassFileTransformer make(ResettableClassFileTransformer classFileTransformer, Callback<?> callback) {
                        try {
                            return withCallback.newInstance(classFileTransformer, callback);
                        } catch (IllegalAccessException exception) {
                            throw new IllegalStateException("Cannot access " + withCallback, exception);
                        } catch (InstantiationException exception) {
                            throw new IllegalStateException("Cannot instantiate " + withCallback.getDeclaringClass(), exception);
                        } catch (InvocationTargetException exception) {
                            throw new IllegalStateException("Cannot invoke " + withCallback, exception.getTargetException());
                        }
                    }
                }

                /**
                 * A factory for a substitutable class file transformer when the module system is not supported.
                 */
                enum ForLegacyVm implements Factory {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    /**
                     * {@inheritDoc}
                     */
                    public ResettableClassFileTransformer make(ResettableClassFileTransformer classFileTransformer,
                                                               Callback<?> callback) {
                        return new WithCallback<>(classFileTransformer, callback);
                    }
                }
            }
        }

        /**
         * A callback that is invoked upon class file transformation.
         *
         * @param <T> The type of the value that is passed between the callback methods.
         */
        public interface Callback<T> {

            /**
             * Invoked before the transformation is applied.
             *
             * @param module               The instrumented class's Java module or {@code null} if the module system is not supported.
             * @param classLoader          The type's class loader or {@code null} if the type is loaded by the bootstrap loader.
             * @param internalName         The internal name of the instrumented class.
             * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The instrumented type's protection domain or {@code null} if not available.
             * @param binaryRepresentation The class file of the instrumented class in its current state.
             * @return A value to pass to the method that is invoked after transformation or {@code null}.
             * @throws IllegalClassFormatException If the class file was found invalid.
             */
            @MaybeNull
            T onBeforeTransform(@MaybeNull JavaModule module,
                                @MaybeNull ClassLoader classLoader,
                                @MaybeNull String internalName,
                                @MaybeNull Class<?> classBeingRedefined,
                                @MaybeNull ProtectionDomain protectionDomain,
                                byte[] binaryRepresentation) throws IllegalClassFormatException;

            /**
             * Invoked after the transformation is applied.
             *
             * @param value                The value that was returned before transformation.
             * @param module               The instrumented class's Java module or {@code null} if the module system is not supported.
             * @param classLoader          The type's class loader or {@code null} if the type is loaded by the bootstrap loader.
             * @param internalName         The internal name of the instrumented class.
             * @param classBeingRedefined  The loaded {@link Class} being redefined or {@code null} if no such class exists.
             * @param protectionDomain     The instrumented type's protection domain or {@code null} if not available.
             * @param binaryRepresentation The class file of the instrumented class in its current state.
             * @throws IllegalClassFormatException If the class file was found invalid.
             */
            void onAfterTransform(@MaybeNull T value,
                                  @MaybeNull JavaModule module,
                                  @MaybeNull ClassLoader classLoader,
                                  @MaybeNull String internalName,
                                  @MaybeNull Class<?> classBeingRedefined,
                                  @MaybeNull ProtectionDomain protectionDomain,
                                  byte[] binaryRepresentation) throws IllegalClassFormatException;
        }
    }
}

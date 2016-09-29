package net.bytebuddy.agent.builder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Map;

/**
 * A class file transformer that can reset its transformation.
 */
public interface ResettableClassFileTransformer extends ClassFileTransformer {

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled.
     * </p>
     * <p>
     * <b>Important</b>: Most JVMs do not support changes of a class's structure after a class was already
     * loaded. Therefore, it is typically required that this class file transformer was built while enabling
     * {@link AgentBuilder#disableClassFormatChanges()}.
     * </p>
     *
     * @param instrumentation      The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy The redefinition to apply.
     * @return A representation of the result of resetting this transformer.
     */
    Reset reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled.
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
     * @return A representation of the result of resetting this transformer.
     */
    Reset reset(Instrumentation instrumentation,
                AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator);

    /**
     * <p>
     * Deregisters this class file transformer and redefines any transformed class to its state without this
     * class file transformer applied, if the supplied redefinition strategy is enabled.
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
     * @return A representation of the result of resetting this transformer.
     */
    Reset reset(Instrumentation instrumentation,
                AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                AgentBuilder.RedefinitionStrategy.Listener redefinitionListener);

    /**
     * An abstract base implementation of a {@link ResettableClassFileTransformer}.
     */
    abstract class AbstractBase implements ResettableClassFileTransformer {

        @Override
        public Reset reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE);
        }

        @Override
        public Reset reset(Instrumentation instrumentation,
                           AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                           AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    redefinitionBatchAllocator,
                    AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        }
    }


    /**
     * A result of a class file transformer reset by a {@link ResettableClassFileTransformer}.
     */
    interface Reset {

        /**
         * Determines if this result did apply a deregistration, i.e. the reset class file transformer was not removed previously.
         *
         * @return {@code true} if the represented reset was applied.
         */
        boolean isApplied();

        /**
         * Returns a mapping of classes that could not be reset to the errors occured when attempting the reset. If no
         * errors occurred, this map is empty. This map is also empty, if this result is not alive.
         *
         * @return A map containing all errors that occured during a reset.
         */
        Map<Class<?>, Throwable> getErrors();

        /**
         * A simple result without errors.
         */
        enum Simple implements Reset {

            /**
             * An active result without errors.
             */
            ACTIVE(true),

            /**
             * An inactive result without errors.
             */
            INACTIVE(false);

            /**
             * {@code true} if this result is alive.
             */
            private final boolean alive;

            /**
             * Creates a new simple result.
             *
             * @param alive {@code true} if this result is alive.
             */
            Simple(boolean alive) {
                this.alive = alive;
            }

            @Override
            public boolean isApplied() {
                return alive;
            }

            @Override
            public Map<Class<?>, Throwable> getErrors() {
                return Collections.emptyMap();
            }

            @Override
            public String toString() {
                return "ResettableClassFileTransformer.Reset.Simple." + name();
            }
        }

        /**
         * A result with a map of errors.
         */
        class WithErrors implements Reset {

            /**
             * A map of errors occurred during a class file transformer reset.
             */
            private final Map<Class<?>, Throwable> failures;

            /**
             * Creates a new result with errors.
             *
             * @param failures A map of errors occurred during a class file transformer reset.
             */
            protected WithErrors(Map<Class<?>, Throwable> failures) {
                this.failures = failures;
            }

            /**
             * Creates a result of a potentially empty error mapping.
             *
             * @param failures A map of errors that occurred during a reset.
             * @return An appropriate result.
             */
            public static Reset ofPotentiallyErroneous(Map<Class<?>, Throwable> failures) {
                return failures.isEmpty()
                        ? Simple.ACTIVE
                        : new WithErrors(failures);
            }

            @Override
            public boolean isApplied() {
                return true;
            }

            @Override
            public Map<Class<?>, Throwable> getErrors() {
                return failures;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                WithErrors that = (WithErrors) object;
                return failures.equals(that.failures);
            }

            @Override
            public int hashCode() {
                return failures.hashCode();
            }

            @Override
            public String toString() {
                return "ResettableClassFileTransformer.Reset.WithErrors{" +
                        "failures=" + failures +
                        '}';
            }
        }
    }
}

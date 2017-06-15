package net.bytebuddy.agent.builder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

/**
 * A class file transformer that can reset its transformation.
 */
public interface ResettableClassFileTransformer extends ClassFileTransformer {

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
     * @param instrumentation               The instrumentation instance from which to deregister the transformer.
     * @param redefinitionStrategy          The redefinition to apply.
     * @param redefinitionBatchAllocator    The batch allocator to use.
     * @param redefinitionListener          The redefinition listener to apply.
     * @return {@code true} if a reset was applied and this transformer was not previously removed.
     */
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
     * An abstract base implementation of a {@link ResettableClassFileTransformer}.
     */
    abstract class AbstractBase implements ResettableClassFileTransformer {

        @Override
        public boolean reset(Instrumentation instrumentation, AgentBuilder.RedefinitionStrategy redefinitionStrategy) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE);
        }

        @Override
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    redefinitionBatchAllocator,
                    AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        }

        @Override
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        }

        @Override
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    redefinitionBatchAllocator,
                    AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        }

        @Override
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.DiscoveryStrategy redefinitionDiscoveryStrategy,
                             AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    redefinitionDiscoveryStrategy,
                    AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE,
                    redefinitionListener);
        }

        @Override
        public boolean reset(Instrumentation instrumentation,
                             AgentBuilder.RedefinitionStrategy redefinitionStrategy,
                             AgentBuilder.RedefinitionStrategy.BatchAllocator redefinitionBatchAllocator,
                             AgentBuilder.RedefinitionStrategy.Listener redefinitionListener) {
            return reset(instrumentation,
                    redefinitionStrategy,
                    AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE,
                    redefinitionBatchAllocator,
                    redefinitionListener);
        }
    }
}

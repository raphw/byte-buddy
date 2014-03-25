package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes if a method is supposed to be synchronized.
 */
public enum SynchronizationState implements ModifierContributor.ForMethod {

    PLAIN(EMPTY_MASK),
    SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED);

    /**
     * Creates a synchronization state from a boolean value indicating if a method is supposed to be synchronized.
     *
     * @param isSynchronized {@code true} if the state is supposed to be synchronized.
     * @return The corresponding synthetic state.
     */
    public static SynchronizationState is(boolean isSynchronized) {
        return isSynchronized ? SYNCHRONIZED : PLAIN;
    }

    private final int mask;

    private SynchronizationState(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    /**
     * Checks if the current state describes the synchronized state.
     *
     * @return {@code true} if the current state is synchronized.
     */
    public boolean isSynchronized() {
        return this == SYNCHRONIZED;
    }
}

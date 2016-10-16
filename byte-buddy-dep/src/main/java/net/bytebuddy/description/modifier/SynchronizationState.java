package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes if a method is supposed to be synchronized.
 */
public enum SynchronizationState implements ModifierContributor.ForMethod {

    /**
     * Modifier for non-synchronized method. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a synchronized method.
     */
    SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new synchronization state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    SynchronizationState(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_SYNCHRONIZED;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if the current state describes the synchronized state.
     *
     * @return {@code true} if the current state is synchronized.
     */
    public boolean isSynchronized() {
        return this == SYNCHRONIZED;
    }

    @Override
    public String toString() {
        return "SynchronizationState." + name();
    }
}

package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Defines if a type or member is supposed to be marked as synthetic.
 */
public enum SyntheticState implements ModifierContributor.ForType, ModifierContributor.ForMethod, ModifierContributor.ForField {

    SYNTHETIC(Opcodes.ACC_SYNTHETIC),
    NON_SYNTHETIC(EMPTY_MASK);
    private final int mask;

    private SyntheticState(int mask) {
        this.mask = mask;
    }

    /**
     * Creates a synthetic state from a boolean value indicating if a type or member is supposed to be synthetic.
     *
     * @param synthetic {@code true} if the state is supposed to be synthetic.
     * @return The corresponding synthetic state.
     */
    public static SyntheticState is(boolean synthetic) {
        return synthetic ? SYNTHETIC : NON_SYNTHETIC;
    }

    @Override
    public int getMask() {
        return mask;
    }

    /**
     * Checks if the current state describes the synthetic state.
     *
     * @return {@code true} if the current state is synthetic.
     */
    public boolean isSynthetic() {
        return this == SYNTHETIC;
    }
}

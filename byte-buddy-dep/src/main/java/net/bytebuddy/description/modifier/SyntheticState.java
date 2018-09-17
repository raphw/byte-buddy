package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Defines if a type or member is supposed to be marked as synthetic.
 */
public enum SyntheticState implements ModifierContributor.ForType,
        ModifierContributor.ForMethod,
        ModifierContributor.ForField,
        ModifierContributor.ForParameter {

    /**
     * Modifier for not marking a type member as synthetic. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for marking a type member as synthetic.
     */
    SYNTHETIC(Opcodes.ACC_SYNTHETIC);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new synthetic state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    SyntheticState(int mask) {
        this.mask = mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getMask() {
        return mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getRange() {
        return Opcodes.ACC_SYNTHETIC;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
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

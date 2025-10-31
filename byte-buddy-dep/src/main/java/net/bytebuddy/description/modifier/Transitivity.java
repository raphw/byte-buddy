package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Defines if a module requires another module also for its users.
 */
public enum Transitivity implements ModifierContributor.ForModule.OfRequire {

    /**
     * Modifier for not marking a type member as synthetic. (This is the default modifier.)
     */
    NONE(EMPTY_MASK),

    /**
     * Modifier for marking a type member as transitive.
     */
    TRANSITIVE(Opcodes.ACC_TRANSITIVE);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new synthetic state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    Transitivity(int mask) {
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
        return Opcodes.ACC_TRANSITIVE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == NONE;
    }

    /**
     * Checks if the current state describes transitivity.
     *
     * @return {@code true} if the current state is transitive.
     */
    public boolean isTransitive() {
        return this == TRANSITIVE;
    }
}
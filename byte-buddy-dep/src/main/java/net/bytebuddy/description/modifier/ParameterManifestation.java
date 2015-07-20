package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Indicates whether a parameter was denoted as {@code final} or not.
 */
public enum ParameterManifestation implements ModifierContributor.ForParameter {

    /**
     * A non-final parameter. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * A final parameter.
     */
    FINAL(Opcodes.ACC_FINAL);

    /**
     * The mask of this parameter manifestation.
     */
    private final int mask;

    /**
     * Creates a new parameter.
     *
     * @param mask The mask of this parameter.
     */
    ParameterManifestation(int mask) {
        this.mask = mask;
    }

    /**
     * Creates a new parameter manifestation from a boolean value which indicates if the returned state should represent a final modifier.
     *
     * @param finalState {@code true} if the returned state should indicate a final parameter.
     * @return A corresponding parameter manifestation.
     */
    public static ParameterManifestation is(boolean finalState) {
        return finalState ? FINAL : PLAIN;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_FINAL;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if this instance represents a final state.
     *
     * @return {@code true} if this instance represents a final state.
     */
    public boolean isFinal() {
        return this == FINAL;
    }

    @Override
    public String toString() {
        return "ParameterManifestation." + name();
    }
}

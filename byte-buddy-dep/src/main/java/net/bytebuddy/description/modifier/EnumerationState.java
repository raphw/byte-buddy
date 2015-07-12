package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Determines if a type describes an enumeration. Note that enumerations must never also be interfaces.
 */
public enum EnumerationState implements ModifierContributor.ForType, ModifierContributor.ForField {

    /**
     * Modifier for marking a type as a non-enumeration. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for marking a type as an enumeration.
     */
    ENUMERATION(Opcodes.ACC_ENUM);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new enumeration state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    EnumerationState(int mask) {
        this.mask = mask;
    }

    /**
     * Creates an enumeration state from a boolean value indicating if a type or member is supposed to be synthetic.
     *
     * @param enumeration {@code true} if the state is supposed to describe an enumeration.
     * @return The corresponding synthetic state.
     */
    public static EnumerationState is(boolean enumeration) {
        return enumeration ? ENUMERATION : PLAIN;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_ENUM;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if the current state describes the enum state.
     *
     * @return {@code true} if the current state describes an enumeration.
     */
    public boolean isEnumeration() {
        return this == ENUMERATION;
    }

    @Override
    public String toString() {
        return "EnumerationState." + name();
    }
}

package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Determines if a type describes an enumeration. Note that enumerations must never also be interfaces.
 */
public enum EnumerationState implements ModifierContributor.ForType {

    /**
     * Modifier for marking a type as an enumeration.
     */
    ENUMERATION(Opcodes.ACC_ENUM),

    /**
     * Modifier for marking a type as a non-enumeration. (This is the default modifier.)
     */
    NON_ENUMERATION(EMPTY_MASK);

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
        return enumeration ? ENUMERATION : NON_ENUMERATION;
    }

    @Override
    public int getMask() {
        return mask;
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

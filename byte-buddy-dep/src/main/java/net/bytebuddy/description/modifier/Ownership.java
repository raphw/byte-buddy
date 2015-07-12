package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Determines the ownership of a field or method, i.e. if a member is defined in as {@code static}
 * and belongs to a class or in contrast to an instance.
 */
public enum Ownership implements ModifierContributor.ForField, ModifierContributor.ForMethod, ModifierContributor.ForType {

    /**
     * Modifier for a instance ownership of a type member. (This is the default modifier.)
     */
    MEMBER(EMPTY_MASK),

    /**
     * Modifier for type ownership of a type member.
     */
    STATIC(Opcodes.ACC_STATIC);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new ownership representation.
     *
     * @param mask The modifier mask of this instance.
     */
    Ownership(int mask) {
        this.mask = mask;
    }

    /**
     * Creates a member ownership state from a {@code boolean} value indicating if a member is supposed to be
     * {@code static}.
     *
     * @param isStatic {@code true} if the member is {@code static}.
     * @return The corresponding member ownership.
     */
    public static Ownership isStatic(boolean isStatic) {
        return isStatic ? STATIC : MEMBER;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_STATIC;
    }

    @Override
    public boolean isDefault() {
        return this == MEMBER;
    }

    /**
     * Checks if the current state describes a {@code static} member.
     *
     * @return {@code true} if this ownership representation represents a {@code static} member.
     */
    public boolean isStatic() {
        return this == STATIC;
    }

    @Override
    public String toString() {
        return "Ownership." + name();
    }
}

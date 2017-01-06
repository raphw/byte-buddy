package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes if a method allows varargs arguments.
 */
public enum MethodArguments implements ModifierContributor.ForMethod {

    /**
     * Describes a method that does not permit varargs.
     */
    PLAIN(EMPTY_MASK),

    /**
     * Describes a method that permits varargs.
     */
    VARARGS(Opcodes.ACC_VARARGS);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new method arguments representation.
     *
     * @param mask The mask of this instance.
     */
    MethodArguments(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_VARARGS;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if the current state describes a varargs methods.
     *
     * @return {@code true} if the current state represents a varargs method.
     */
    public boolean isVarArgs() {
        return this == VARARGS;
    }
}

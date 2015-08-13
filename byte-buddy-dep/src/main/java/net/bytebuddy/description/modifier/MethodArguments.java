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

    /**
     * Creates a method argument state from a {@code boolean} value indicating if a method should support varargs.
     *
     * @param varargs {@code true} if the method is supposed to support varargs.
     * @return The corresponding method argument state.
     */
    public static MethodArguments isVarArgs(boolean varargs) {
        return varargs ? VARARGS : PLAIN;
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

    @Override
    public String toString() {
        return "MethodArguments." + name();
    }
}

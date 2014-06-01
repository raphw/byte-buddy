package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Determines if the super flag should be set for a given type. This is done by the Java compiler
 * for all recent Java versions and the flag is ignored for all byte code version other then version 1.1. This flag
 * should normally always be set.
 */
public enum SuperFlag implements ModifierContributor.ForType {

    /**
     * Modifier for enabling virtual super method dispatch. This flag should always be set when
     * specifying a type modifier manually.
     */
    DEFINED(Opcodes.ACC_SUPER),

    /**
     * Modifier for disabling virtual super method dispatch. This flag is however ignored for any byte code version
     * other then 1.1.
     */
    UNDEFINED(EMPTY_MASK);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new super flag representation.
     *
     * @param mask The modifier mask of this instance.
     */
    private SuperFlag(int mask) {
        this.mask = mask;
    }

    /**
     * Creates a super flag from a boolean value indicating if a type should carry this flag.
     *
     * @param isSuperFlag {@code true} if the type is supposed to carry the flag.
     * @return The corresponding super flag state.
     */
    public static SuperFlag is(boolean isSuperFlag) {
        return isSuperFlag ? DEFINED : UNDEFINED;
    }

    @Override
    public int getMask() {
        return mask;
    }

    /**
     * Checks if the super flag is set.
     *
     * @return {@code true} if the state is set.
     */
    public boolean isSuperFlag() {
        return this == DEFINED;
    }
}

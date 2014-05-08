package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Determines the ownership of a field or method, i.e. if a member is defined in as {@code static}
 * and belongs to a class or in contrast to an instance.
 */
public enum Ownership implements ModifierContributor.ForField, ModifierContributor.ForMethod {

    /**
     * Modifier for a instance ownership of a type member. (This is the default modifier.)
     */
    MEMBER(EMPTY_MASK),

    /**
     * Modifier for type ownership of a type member.
     */
    STATIC(Opcodes.ACC_STATIC);

    private final int mask;

    private Ownership(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes a field's or a method's visibility.
 */
public enum MemberVisibility implements ModifierContributor.ForMethod, ModifierContributor.ForField {

    /**
     * Modifier for a public type member.
     */
    PUBLIC(Opcodes.ACC_PUBLIC),

    /**
     * Modifier for a package-private type member. (This is the default modifier.)
     */
    PACKAGE_PRIVATE(EMPTY_MASK),

    /**
     * Modifier for a protected type member.
     */
    PROTECTED(Opcodes.ACC_PROTECTED),

    /**
     * Modifier for a private type member.
     */
    PRIVATE(Opcodes.ACC_PRIVATE);

    private final int mask;

    private MemberVisibility(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

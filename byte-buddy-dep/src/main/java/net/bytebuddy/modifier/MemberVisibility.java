package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes a field's or a method's visibility.
 */
public enum MemberVisibility implements ModifierContributor.ForMethod, ModifierContributor.ForField {

    PUBLIC(Opcodes.ACC_PUBLIC),
    PACKAGE_PRIVATE(EMPTY_MASK),
    PROTECTED(Opcodes.ACC_PROTECTED),
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

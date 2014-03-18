package com.blogspot.mydailyjava.bytebuddy.modifier;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Determines the ownership of a field or method, i.e. if a member is defined in as {@code static}
 * and belongs to a class or in contrast to an instance.
 */
public enum Ownership implements ModifierContributor.ForField, ModifierContributor.ForMethod {
    MEMBER(EMPTY_MASK),
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

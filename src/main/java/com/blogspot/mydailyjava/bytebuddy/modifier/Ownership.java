package com.blogspot.mydailyjava.bytebuddy.modifier;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

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

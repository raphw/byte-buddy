package com.blogspot.mydailyjava.bytebuddy.modifier;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum Visibility implements ModifierContributor.ForType, ModifierContributor.ForMethod, ModifierContributor.ForField {

    PUBLIC(Opcodes.ACC_PUBLIC),
    PACKAGE_PRIVATE(EMPTY_MASK),
    PROTECTED(Opcodes.ACC_PROTECTED),
    PRIVATE(Opcodes.ACC_PRIVATE);

    private final int mask;

    private Visibility(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

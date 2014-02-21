package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum MethodManifestation implements ModifierContributor.ForMethod {

    PLAIN(EMPTY_MASK),
    NATIVE(Opcodes.ACC_NATIVE),
    ABSTRACT(Opcodes.ACC_ABSTRACT);

    private final int mask;

    private MethodManifestation(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

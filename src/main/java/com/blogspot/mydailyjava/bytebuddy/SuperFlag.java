package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum SuperFlag implements ModifierContributor.ForType {

    DEFINED(Opcodes.ACC_SUPER),
    UNDEFINED(EMPTY_MASK);

    private final int mask;

    private SuperFlag(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

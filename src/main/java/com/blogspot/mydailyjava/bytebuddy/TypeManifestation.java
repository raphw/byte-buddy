package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum TypeManifestation implements ModifierContributor {

    CONCRETE(0),
    FINAL(Opcodes.ACC_FINAL),
    ABSTRACT(Opcodes.ACC_ABSTRACT),
    INTERFACE(Opcodes.ACC_INTERFACE);

    private final int mask;

    private TypeManifestation(int mask) {
        this.mask = mask;
    }

    public int getMask() {
        return mask;
    }
}

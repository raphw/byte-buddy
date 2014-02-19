package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum TypeManifestation implements ModifierContributor.ForType {

    PLAIN(0),
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

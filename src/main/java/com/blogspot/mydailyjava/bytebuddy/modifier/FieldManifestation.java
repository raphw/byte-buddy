package com.blogspot.mydailyjava.bytebuddy.modifier;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum FieldManifestation implements ModifierContributor.ForField {

    PLAIN(EMPTY_MASK),
    FINAL(Opcodes.ACC_FINAL),
    VOLATILE(Opcodes.ACC_VOLATILE);

    private final int mask;

    private FieldManifestation(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

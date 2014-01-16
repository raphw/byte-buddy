package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.type.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum Visibility implements ModifierContributor {

    PUBLIC(Opcodes.ACC_PUBLIC),
    PROTECTED(Opcodes.ACC_PROTECTED),
    PACKAGE_PRIVATE(0),
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

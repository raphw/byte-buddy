package com.blogspot.mydailyjava.bytebuddy.modifier;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum SynchronizationState implements ModifierContributor.ForMethod {

    PLAIN(EMPTY_MASK),
    SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED);

    private final int mask;

    private SynchronizationState(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

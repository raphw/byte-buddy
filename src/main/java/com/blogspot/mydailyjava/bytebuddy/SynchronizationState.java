package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum SynchronizationState implements ModifierContributor.ForMethod {

    PLAIN(0),
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

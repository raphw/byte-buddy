package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum SyntheticState implements ModifierContributor {

    SYNTHETIC(Opcodes.ACC_SYNTHETIC),
    NON_SYNTHETIC(0);

    public static SyntheticState is(boolean synthetic) {
        return synthetic ? SYNTHETIC : NON_SYNTHETIC;
    }

    private final int mask;

    private SyntheticState(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

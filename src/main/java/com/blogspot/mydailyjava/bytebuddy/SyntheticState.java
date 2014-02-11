package com.blogspot.mydailyjava.bytebuddy;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

public enum SyntheticState implements ModifierContributor {

    SYNTHETIC(Opcodes.ACC_SYNTHETIC, true),
    NON_SYNTHETIC(0, false);

    public static SyntheticState is(boolean synthetic) {
        return synthetic ? SYNTHETIC : NON_SYNTHETIC;
    }

    private final int mask;
    private final boolean synthetic;

    private SyntheticState(int mask, boolean synthetic) {
        this.mask = mask;
        this.synthetic = synthetic;
    }

    @Override
    public int getMask() {
        return mask;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}

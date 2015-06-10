package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

public enum Mandate implements ModifierContributor.ForParameter {

    MANDATED(Opcodes.ACC_MANDATED),

    NON_MANDATED(EMPTY_MASK);

    private final int mask;

    Mandate(int mask) {
        this.mask = mask;
    }

    public static Mandate is(boolean mandated) {
        return mandated ? MANDATED : NON_MANDATED;
    }

    @Override
    public int getMask() {
        return mask;
    }

    public boolean isMandated() {
        return this == MANDATED;
    }
}

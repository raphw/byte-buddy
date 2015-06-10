package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

public enum ProvisioningState implements ModifierContributor.ForParameter {

    MANDATED(Opcodes.ACC_MANDATED),

    NON_MANDATED(EMPTY_MASK);

    private final int mask;

    ProvisioningState(int mask) {
        this.mask = mask;
    }

    public static ProvisioningState is(boolean mandated) {
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

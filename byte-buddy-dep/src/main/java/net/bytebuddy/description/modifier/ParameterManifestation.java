package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

public enum ParameterManifestation implements ModifierContributor.ForParameter {

    PLAIN(EMPTY_MASK),

    FINAL(Opcodes.ACC_FINAL);

    private final int mask;

    ParameterManifestation(int mask) {
        this.mask = mask;
    }

    public static ParameterManifestation is(boolean finalState) {
        return finalState ? FINAL : PLAIN;
    }

    @Override
    public int getMask() {
        return mask;
    }

    public boolean isFinal() {
        return this == FINAL;
    }
}

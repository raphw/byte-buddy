package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * A modifier contributor to determine the use of {@code strictfp} on a method.
 */
public enum MethodStrictness implements ModifierContributor.ForMethod {

    /**
     * Modifier for a non-strict method. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a method that applies strict floating-point computation.
     */
    STRICT(Opcodes.ACC_STRICT);

    /**
     * The modifier contributors mask.
     */
    private final int mask;

    /**
     * Creates a new modifier contributor for a method.
     *
     * @param mask The modifier contributors mask.
     */
    MethodStrictness(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_STRICT;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this modifier contributor indicates strict floating-point computation.
     *
     * @return {@code true} if this modifier contributor indicates strict floating-point computation.
     */
    public boolean isStrict() {
        return this == STRICT;
    }
}

package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes when another module is considered as a requirement.
 */
public enum RequiredPhase implements ModifierContributor.ForModule.OfRequire,
        ModifierContributor.ForModule.OfExport,
        ModifierContributor.ForModule.OfOpen {

    /**
     * Modifier for requiring another module during all phases. (This is the default modifier.)
     */
    NONE(EMPTY_MASK),

    /**
     * Modifier for requiring another module only during assembly.
     */
    STATIC(Opcodes.ACC_STATIC_PHASE);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new synthetic state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    RequiredPhase(int mask) {
        this.mask = mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getMask() {
        return mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getRange() {
        return Opcodes.ACC_STATIC_PHASE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == NONE;
    }

    /**
     * Checks if the current state describes static phase requirement.
     *
     * @return {@code true} if the current state is a static phase requirement.
     */
    public boolean isStatic() {
        return this == STATIC;
    }
}
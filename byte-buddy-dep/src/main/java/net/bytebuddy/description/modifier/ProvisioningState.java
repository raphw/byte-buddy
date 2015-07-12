package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes if a method parameter is mandated, i.e. not explicitly specified in the source code.
 */
public enum ProvisioningState implements ModifierContributor.ForParameter {

    /**
     * Defines a parameter to not be mandated. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Defines a parameter to be mandated.
     */
    MANDATED(Opcodes.ACC_MANDATED);

    /**
     * The mask of this provisioning state.
     */
    private final int mask;

    /**
     * Creates a new provisioning state.
     *
     * @param mask The mask of this provisioning state.
     */
    ProvisioningState(int mask) {
        this.mask = mask;
    }

    /**
     * Creates a new provisioning state from a boolean value which indicates if the returned state should represent a final modifier.
     *
     * @param mandated {@code true} if the returned state should indicate a mandated parameter.
     * @return A corresponding provisioning state.
     */
    public static ProvisioningState is(boolean mandated) {
        return mandated ? MANDATED : PLAIN;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_MANDATED;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if this instance represents a mandated parameter or not.
     *
     * @return {@code true} if this instance represents a mandated parameter.
     */
    public boolean isMandated() {
        return this == MANDATED;
    }

    @Override
    public String toString() {
        return "ProvisioningState." + name();
    }
}

package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes the persistence of a field, i.e. if it is {@code transient}.
 */
public enum FieldPersistence implements ModifierContributor.ForField {

    /**
     * Modifier for a non-transient field. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a transient field.
     */
    TRANSIENT(Opcodes.ACC_TRANSIENT);

    /**
     * The modifier mask for this persistence type.
     */
    private final int mask;

    /**
     * Creates a new field persistence description.
     *
     * @param mask The modifier mask for this persistence type.
     */
    FieldPersistence(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_TRANSIENT;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this manifestation represents a field that is {@code transient}.
     *
     * @return {@code true} if this manifestation represents a field that is {@code transient}.
     */
    public boolean isTransient() {
        return (mask & Opcodes.ACC_TRANSIENT) != 0;
    }
}

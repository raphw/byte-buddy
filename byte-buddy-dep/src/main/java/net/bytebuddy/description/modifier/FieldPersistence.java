package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes if a field is {@code transient}.
 */
public enum FieldPersistence implements ModifierContributor.ForField {

    /**
     * Modifier for a non-transient field. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a {@code transient} field.
     */
    TRANSIENT(Opcodes.ACC_TRANSIENT);

    /**
     * This modifier contributor's mask.
     */
    private final int mask;

    /**
     * Creates a new modifier contributor for field persistence.
     *
     * @param mask This modifier contributor's mask.
     */
    FieldPersistence(int mask) {
        this.mask = mask;
    }

    /**
     * Returns a field persistence modifier contributor.
     *
     * @param isTransient {@code true} if a field is supposed to be {@code transient}
     * @return An appropriate field persistence modifier contributor.
     */
    public static FieldPersistence isTransient(boolean isTransient) {
        return isTransient
                ? TRANSIENT
                : PLAIN;
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
     * Checks if this field persistence modifier contributor is transient.
     *
     * @return {@code true} if this field persisitent modifier contributor is transient.
     */
    public boolean isTransient() {
        return this == TRANSIENT;
    }

    @Override
    public String toString() {
        return "FieldPersistence." + name();
    }
}

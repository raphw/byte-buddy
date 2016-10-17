package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes the manifestation of a class's field, i.e. if a field is final, volatile or neither.
 */
public enum FieldManifestation implements ModifierContributor.ForField {

    /**
     * Modifier for a non-final, non-volatile field. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a final field.
     */
    FINAL(Opcodes.ACC_FINAL),

    /**
     * Modifier for a volatile field.
     */
    VOLATILE(Opcodes.ACC_VOLATILE),

    /**
     * Modifier for a transient field.
     */
    TRANSIENT(Opcodes.ACC_TRANSIENT),

    /**
     * Modifier for a volatile, transient field.
     */
    VOLATILE_TRANSIENT(Opcodes.ACC_VOLATILE | Opcodes.ACC_TRANSIENT);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new field manifestation.
     *
     * @param mask The modifier mask of this instance.
     */
    FieldManifestation(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    @Override
    public int getRange() {
        return Opcodes.ACC_FINAL | Opcodes.ACC_VOLATILE | Opcodes.ACC_TRANSIENT;
    }

    @Override
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this manifestation represents a {@code final} type.
     *
     * @return {@code true} if this manifestation represents a {@code final} type.
     */
    public boolean isFinal() {
        return (mask & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * Returns {@code true} if this manifestation represents a {@code volatile} type.
     *
     * @return {@code true} if this manifestation represents a {@code volatile} type.
     */
    public boolean isVolatile() {
        return (mask & Opcodes.ACC_VOLATILE) != 0;
    }

    /**
     * Returns {@code true} if this manifestation represents a field that is {@code transient}.
     *
     * @return {@code true} if this manifestation represents a field that is {@code transient}.
     */
    public boolean isTransient() {
        return (mask & Opcodes.ACC_TRANSIENT) != 0;
    }

    /**
     * Returns {@code true} if this manifestation represents a field that is neither {@code final}, {@code transient} or {@code volatile}.
     *
     * @return {@code true} if this manifestation represents a field that is neither {@code final}, {@code transient} or {@code volatile}.
     */
    public boolean isPlain() {
        return !(isFinal() || isVolatile() || isTransient());
    }

    @Override
    public String toString() {
        return "FieldManifestation." + name();
    }
}

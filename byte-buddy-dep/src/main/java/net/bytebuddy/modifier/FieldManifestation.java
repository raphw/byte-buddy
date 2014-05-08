package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
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
    VOLATILE(Opcodes.ACC_VOLATILE);

    private final int mask;

    private FieldManifestation(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

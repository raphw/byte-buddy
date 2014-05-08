package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes a type's manifestation, i.e. if a type is final, abstract, an interface or neither.
 */
public enum TypeManifestation implements ModifierContributor.ForType {

    /**
     * Modifier for a non-final, non-abstract, non-interface, non-enum type. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a final class.
     */
    FINAL(Opcodes.ACC_FINAL),

    /**
     * Modifier for an abstract class.
     */
    ABSTRACT(Opcodes.ACC_ABSTRACT),

    /**
     * Modifier for an interface.
     */
    INTERFACE(Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT),

    /**
     * Modifier for a final enum type.
     */
    ENUM(Opcodes.ACC_ENUM | Opcodes.ACC_FINAL),

    /**
     * Modifier for an abstract enum type.
     */
    ABSTRACT_ENUM(Opcodes.ACC_ENUM | Opcodes.ACC_ABSTRACT);

    private final int mask;

    private TypeManifestation(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

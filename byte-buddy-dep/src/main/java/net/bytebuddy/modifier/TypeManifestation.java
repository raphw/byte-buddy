package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes a type's manifestation, i.e. if a type is final, abstract, an interface or neither.
 */
public enum TypeManifestation implements ModifierContributor.ForType {

    PLAIN(EMPTY_MASK),
    FINAL(Opcodes.ACC_FINAL),
    ABSTRACT(Opcodes.ACC_ABSTRACT),
    INTERFACE(Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT),
    ENUM(Opcodes.ACC_ENUM | Opcodes.ACC_FINAL),
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

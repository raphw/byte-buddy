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
    INTERFACE(Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new type manifestation.
     *
     * @param mask The modifier mask of this instance.
     */
    private TypeManifestation(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }

    /**
     * Returns {@code true} if a type represents a {@code final} type.
     *
     * @return {@code true} if a type represents a {@code final} type.
     */
    public boolean isFinal() {
        return (mask & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * Returns {@code true} if a type represents an {@code abstract} type but not an interface type.
     *
     * @return {@code true} if a type represents an {@code abstract} type but not an interface type.
     */
    public boolean isAbstract() {
        return (mask & Opcodes.ACC_ABSTRACT) != 0 && !isInterface();
    }

    /**
     * Returns {@code true} if a type represents an interface type.
     *
     * @return {@code true} if a type represents an interface type.
     */
    public boolean isInterface() {
        return (mask & Opcodes.ACC_INTERFACE) != 0;
    }
}

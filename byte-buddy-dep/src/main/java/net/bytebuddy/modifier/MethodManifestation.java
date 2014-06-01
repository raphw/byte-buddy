package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes the manifestation of a method, i.e. if a method is final, abstract or native.
 * Note that an {@code abstract} method must never be static and can only be declared for an
 * {@code abstract} type.
 */
public enum MethodManifestation implements ModifierContributor.ForMethod {

    /**
     * Modifier for a non-native, non-abstract, non-final method. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a native method.
     */
    NATIVE(Opcodes.ACC_NATIVE),

    /**
     * Modifier for an abstract method.
     */
    ABSTRACT(Opcodes.ACC_ABSTRACT),

    /**
     * Modifier for a final method.
     */
    FINAL(Opcodes.ACC_FINAL),

    /**
     * Modifier for a native and final method.
     */
    FINAL_NATIVE(Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE);

    /**
     * A mask for checking if a method implementation is not implemented in byte code.
     */
    public static final int ABSTRACTION_MASK = Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE;

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new method manifestation.
     *
     * @param mask The modifier mask of this instance.
     */
    private MethodManifestation(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

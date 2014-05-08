package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes a type's visibility.
 */
public enum TypeVisibility implements ModifierContributor.ForType {

    /**
     * Modifier for a public type.
     */
    PUBLIC(Opcodes.ACC_PUBLIC),

    /**
     * Modifier for a package-private type. (This is the default modifier.)
     */
    PACKAGE_PRIVATE(EMPTY_MASK);

    private final int mask;

    private TypeVisibility(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

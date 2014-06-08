package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes a type's, field's or a method's visibility.
 */
public enum Visibility implements ModifierContributor.ForType,
        ModifierContributor.ForMethod,
        ModifierContributor.ForField {

    /**
     * A modifier contributor for {@code public} visibility.
     */
    PUBLIC(Opcodes.ACC_PUBLIC),

    /**
     * Modifier for a package-private visibility. (This is the default modifier.)
     */
    PACKAGE_PRIVATE(EMPTY_MASK),

    /**
     * A modifier contributor for {@code protected} visibility.
     */
    PROTECTED(Opcodes.ACC_PROTECTED),

    /**
     * A modifier contributor for {@code private} visibility.
     */
    PRIVATE(Opcodes.ACC_PRIVATE);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new visibility representation.
     *
     * @param mask The modifier mask of this instance.
     */
    private Visibility(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

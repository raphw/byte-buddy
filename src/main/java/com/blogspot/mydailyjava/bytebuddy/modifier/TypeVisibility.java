package com.blogspot.mydailyjava.bytebuddy.modifier;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Describes a type's visibility.
 */
public enum TypeVisibility implements ModifierContributor.ForType {

    PUBLIC(Opcodes.ACC_PUBLIC),
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

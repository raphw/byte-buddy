package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.objectweb.asm.Opcodes;

/**
 * Determines if the {@code super} flag should be set for a given type. This is done by the Java compiler
 * for all recent Java versions and the flag is ignored for modern JVMs. This flag should always be set for
 * modern Java applications.
 */
public enum SuperFlag implements ModifierContributor.ForType {

    DEFINED(Opcodes.ACC_SUPER),
    UNDEFINED(EMPTY_MASK);

    private final int mask;

    private SuperFlag(int mask) {
        this.mask = mask;
    }

    @Override
    public int getMask() {
        return mask;
    }
}

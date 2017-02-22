package net.bytebuddy.dynamic.loading;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * A class file transformer that does not apply a transformation.
 */
public enum NoOpClassFileTransformer implements ClassFileTransformer {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * Indicates that no transformation is to applied.
     */
    private static final byte[] NO_TRANSFORMATION = null;

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        return NO_TRANSFORMATION;
    }
}

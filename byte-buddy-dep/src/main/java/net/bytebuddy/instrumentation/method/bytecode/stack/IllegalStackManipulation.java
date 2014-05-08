package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import org.objectweb.asm.MethodVisitor;

/**
 * An impossible manipulation of the operand stack that must not be applied.
 */
public enum IllegalStackManipulation implements StackManipulation {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        throw new IllegalStateException("An illegal stack manipulation cannot be applied");
    }
}

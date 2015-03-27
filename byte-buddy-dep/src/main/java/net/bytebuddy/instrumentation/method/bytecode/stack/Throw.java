package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Throws a {@link java.lang.Throwable} which must lie on top of the stack when this stack manipulation is called.
 */
public enum Throw implements StackManipulation {

    /**
     * The singleton instance.
     */
    INSTANCE;

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(Opcodes.ATHROW);
        return StackSize.SINGLE.toDecreasingSize();
    }

    @Override
    public String toString() {
        return "Throw." + name();
    }
}

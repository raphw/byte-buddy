package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Represents a stack manipulation to load a {@code null} pointer onto the operand stack.
 */
public enum NullConstant implements StackManipulation {
    INSTANCE;

    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        return SIZE;
    }
}

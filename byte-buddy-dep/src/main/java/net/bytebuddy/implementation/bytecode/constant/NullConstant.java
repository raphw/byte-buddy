package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Represents a stack manipulation to load a {@code null} pointer onto the operand stack.
 */
public enum NullConstant implements StackManipulation {

    /**
     * The singleton instance.
     */
    INSTANCE(StackSize.SINGLE);

    /**
     * The size impact of loading the {@code null} reference onto the operand stack.
     */
    private final Size size;

    /**
     * Creates a null constant.
     *
     * @param size The size of the constant on the operand stack.
     */
    NullConstant(StackSize size) {
        this.size = size.toIncreasingSize();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        return size;
    }
}

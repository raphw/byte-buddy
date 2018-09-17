package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation that adds to numbers on the operand stack.
 */
public enum Addition implements StackManipulation {

    /**
     * Adds two integers or integer-compatible values.
     */
    INTEGER(Opcodes.IADD, StackSize.SINGLE),

    /**
     * Adds two longs.
     */
    LONG(Opcodes.LADD, StackSize.DOUBLE),

    /**
     * Adds two floats.
     */
    FLOAT(Opcodes.FADD, StackSize.SINGLE),

    /**
     * Adds two doubles.
     */
    DOUBLE(Opcodes.DADD, StackSize.DOUBLE);

    /**
     * The opcode to apply.
     */
    private final int opcode;

    /**
     * The stack size of the added primitive.
     */
    private final StackSize stackSize;

    /**
     * Creates a new addition.
     *
     * @param opcode    The opcode to apply.
     * @param stackSize The stack size of the added primitive.
     */
    Addition(int opcode, StackSize stackSize) {
        this.opcode = opcode;
        this.stackSize = stackSize;
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
        methodVisitor.visitInsn(opcode);
        return stackSize.toDecreasingSize();
    }
}

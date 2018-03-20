package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation that multiplies to numbers on the operand stack.
 */
public enum Multiplication implements StackManipulation {

    /**
     * Multiplies two integers or integer-compatible values.
     */
    INTEGER(Opcodes.IMUL, StackSize.SINGLE),

    /**
     * Multiplies two longs.
     */
    LONG(Opcodes.LMUL, StackSize.DOUBLE),

    /**
     * Multiplies two floats.
     */
    FLOAT(Opcodes.FMUL, StackSize.SINGLE),

    /**
     * Multiplies two doubles.
     */
    DOUBLE(Opcodes.DMUL, StackSize.DOUBLE);

    /**
     * The opcode to apply.
     */
    private final int opcode;

    /**
     * The stack size of the multiplied primitive.
     */
    private final StackSize stackSize;

    /**
     * Creates a new multiplication type.
     *
     * @param opcode    The opcode to apply.
     * @param stackSize The stack size of the multiplied primitive.
     */
    Multiplication(int opcode, StackSize stackSize) {
        this.opcode = opcode;
        this.stackSize = stackSize;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(opcode);
        return stackSize.toDecreasingSize();
    }
}

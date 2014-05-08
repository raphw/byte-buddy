package net.bytebuddy.instrumentation.method.bytecode.stack;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Duplicates a value that is lying on top of the stack.
 */
public enum Duplication implements StackManipulation {

    /**
     * A duplication of no values. This corresponds a no-op instruction.
     */
    ZERO(StackSize.ZERO, Opcodes.NOP),

    /**
     * A duplication of a single-sized stack values.
     */
    SINGLE(StackSize.SINGLE, Opcodes.DUP),

    /**
     * A duplication of a double-sized stack value.
     */
    DOUBLE(StackSize.DOUBLE, Opcodes.DUP2);
    private final Size size;
    private final int opcode;

    private Duplication(StackSize stackSize, int opcode) {
        size = stackSize.toIncreasingSize();
        this.opcode = opcode;
    }

    /**
     * Duplicates a value given its type.
     *
     * @param typeDescription The type to be duplicated.
     * @return A stack manipulation that duplicates the given type.
     */
    public static StackManipulation duplicate(TypeDescription typeDescription) {
        switch (typeDescription.getStackSize()) {
            case SINGLE:
                return SINGLE;
            case DOUBLE:
                return DOUBLE;
            case ZERO:
                return ZERO;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(opcode);
        return size;
    }
}

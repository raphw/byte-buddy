package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Duplicates a value that is lying on top of the stack.
 */
public enum Duplication implements StackManipulation {

    /**
     * A duplication of no values. This corresponds a no-op instruction.
     */
    ZERO(StackSize.ZERO, Opcodes.NOP) {
        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return new Size(0, 0);
        }
    },

    /**
     * A duplication of a single-sized stack values.
     */
    SINGLE(StackSize.SINGLE, Opcodes.DUP),

    /**
     * A duplication of a double-sized stack value.
     */
    DOUBLE(StackSize.DOUBLE, Opcodes.DUP2);

    /**
     * The size representing the impact of applying the duplication onto the operand stack.
     */
    private final Size size;

    /**
     * The opcode that represents the manipulation.
     */
    private final int opcode;

    /**
     * Creates a new duplication.
     *
     * @param stackSize The size representing the impact of applying the duplication onto the operand stack.
     * @param opcode    The opcode that represents the manipulation.
     */
    Duplication(StackSize stackSize, int opcode) {
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
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(opcode);
        return size;
    }

    @Override
    public String toString() {
        return "Duplication." + name();
    }
}

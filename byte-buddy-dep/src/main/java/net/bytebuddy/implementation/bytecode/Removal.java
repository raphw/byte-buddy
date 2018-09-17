package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Removes a value from the operand stack.
 */
public enum Removal implements StackManipulation {

    /**
     * A removal of no value. This corresponds a no-op instruction.
     */
    ZERO(StackSize.ZERO, Opcodes.NOP) {
        /** {@inheritDoc} */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return new Size(0, 0);
        }
    },

    /**
     * A removal of a single-sized value.
     */
    SINGLE(StackSize.SINGLE, Opcodes.POP),

    /**
     * A removal of a double-sized value.
     */
    DOUBLE(StackSize.DOUBLE, Opcodes.POP2);

    /**
     * The size impact of the removal onto the operand stack.
     */
    private final Size size;

    /**
     * The opcode to execute for the removal.
     */
    private final int opcode;

    /**
     * Creates a new removal stack manipulation.
     *
     * @param stackSize The size impact of the removal onto the operand stack.
     * @param opcode    The opcode to execute for the removal.
     */
    Removal(StackSize stackSize, int opcode) {
        size = stackSize.toDecreasingSize();
        this.opcode = opcode;
    }

    /**
     * Removes a value from the operand stack dependant of its size.
     *
     * @param typeDefinition The type to remove from the stack.
     * @return A stack manipulation that represents the removal.
     */
    public static StackManipulation of(TypeDefinition typeDefinition) {
        switch (typeDefinition.getStackSize()) {
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
        return size;
    }
}


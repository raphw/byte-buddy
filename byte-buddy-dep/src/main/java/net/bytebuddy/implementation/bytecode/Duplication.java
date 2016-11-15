package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.description.type.TypeDefinition;
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
            return size;
        }

        @Override
        public StackManipulation flipOver(TypeDefinition typeDefinition) {
            throw new IllegalStateException("Cannot flip zero duplication");
        }
    },

    /**
     * A duplication of a single-sized stack values.
     */
    SINGLE(StackSize.SINGLE, Opcodes.DUP) {
        @Override
        public StackManipulation flipOver(TypeDefinition typeDefinition) {
            switch (typeDefinition.getStackSize()) {
                case SINGLE:
                    return FlipDuplication.SINGLE_SINGLE;
                case DOUBLE:
                    return FlipDuplication.SINGLE_DOUBLE;
                default:
                    throw new IllegalArgumentException();
            }
        }
    },

    /**
     * A duplication of a double-sized stack value.
     */
    DOUBLE(StackSize.DOUBLE, Opcodes.DUP2) {
        @Override
        public StackManipulation flipOver(TypeDefinition typeDefinition) {
            switch (typeDefinition.getStackSize()) {
                case SINGLE:
                    return FlipDuplication.DOUBLE_SINGLE;
                case DOUBLE:
                    return FlipDuplication.DOUBLE_DOUBLE;
                default:
                    throw new IllegalArgumentException();
            }
        }
    };

    /**
     * The size representing the impact of applying the duplication onto the operand stack.
     */
    protected final Size size;

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
    public static Duplication duplicate(TypeDefinition typeDefinition) {
        switch (typeDefinition.getStackSize()) {
            case SINGLE:
                return SINGLE;
            case DOUBLE:
                return DOUBLE;
            case ZERO:
                return ZERO;
            default:
                throw new AssertionError("Unexpected type: " + typeDefinition);
        }
    }

    public abstract StackManipulation flipOver(TypeDefinition typeDefinition);

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

    protected enum FlipDuplication implements StackManipulation {

        SINGLE_SINGLE(Opcodes.DUP_X1, StackSize.SINGLE),

        SINGLE_DOUBLE(Opcodes.DUP_X2, StackSize.SINGLE),

        DOUBLE_SINGLE(Opcodes.DUP2_X1, StackSize.DOUBLE),

        DOUBLE_DOUBLE(Opcodes.DUP2_X2, StackSize.DOUBLE);

        private final int opcode;

        private final StackSize stackSize;

        FlipDuplication(int opcode, StackSize stackSize) {
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
            return stackSize.toIncreasingSize();
        }
    }
}

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
            throw new IllegalStateException("Cannot flip zero value");
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
                    return WithFlip.SINGLE_SINGLE;
                case DOUBLE:
                    return WithFlip.SINGLE_DOUBLE;
                default:
                    throw new IllegalArgumentException("Cannot flip: " + typeDefinition);
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
                    return WithFlip.DOUBLE_SINGLE;
                case DOUBLE:
                    return WithFlip.DOUBLE_DOUBLE;
                default:
                    throw new IllegalArgumentException("Cannot flip: " + typeDefinition);
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
     * @param typeDefinition The type to be duplicated.
     * @return A stack manipulation that duplicates the given type.
     */
    public static Duplication of(TypeDefinition typeDefinition) {
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

    /**
     * Creates a duplication that flips the stack's top value over the second stack element.
     *
     * @param typeDefinition The type of the second element on the operand stack.
     * @return A stack manipulation that represents such a duplication flip.
     */
    public abstract StackManipulation flipOver(TypeDefinition typeDefinition);

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

    /**
     * A duplication that flips a value over the second value on the operand stack.
     */
    protected enum WithFlip implements StackManipulation {

        /**
         * A flip instruction that flips a single-sized element over another single-size element.
         */
        SINGLE_SINGLE(Opcodes.DUP_X1, StackSize.SINGLE),

        /**
         * A flip instruction that flips a double-sized element over a single-size element.
         */
        SINGLE_DOUBLE(Opcodes.DUP_X2, StackSize.SINGLE),

        /**
         * A flip instruction that flips a single-sized element over a double-size element.
         */
        DOUBLE_SINGLE(Opcodes.DUP2_X1, StackSize.DOUBLE),

        /**
         * A flip instruction that flips a double-sized element over another double-size element.
         */
        DOUBLE_DOUBLE(Opcodes.DUP2_X2, StackSize.DOUBLE);

        /**
         * The opcode to apply.
         */
        private final int opcode;

        /**
         * The size that is added to the operand stack.
         */
        private final StackSize stackSize;

        /**
         * Creates a flip duplication.
         *
         * @param opcode    The opcode to apply.
         * @param stackSize The size that is added to the operand stack.
         */
        WithFlip(int opcode, StackSize stackSize) {
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
            return stackSize.toIncreasingSize();
        }
    }
}

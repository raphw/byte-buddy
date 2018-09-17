package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class is responsible for loading any {@code float} constant onto the operand stack.
 */
public enum FloatConstant implements StackManipulation {

    /**
     * A {@code float} constant of value {@code 0.0f}.
     */
    ZERO(Opcodes.FCONST_0),

    /**
     * A {@code float} constant of value {@code 1.0f}.
     */
    ONE(Opcodes.FCONST_1),

    /**
     * A {@code float} constant of value {@code 2.0f}.
     */
    TWO(Opcodes.FCONST_2);

    /**
     * The size impact of loading a {@code float} constant onto the operand stack.
     */
    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    /**
     * The shortcut opcode for loading a {@code float} constant.
     */
    private final int opcode;

    /**
     * Creates a new shortcut operation for loading a common {@code float} onto the operand stack.
     *
     * @param opcode The shortcut opcode for loading a {@code float} constant.
     */
    FloatConstant(int opcode) {
        this.opcode = opcode;
    }

    /**
     * Creates a stack manipulation for loading a {@code float} value onto the operand stack.
     * <p>&nbsp;</p>
     * This is achieved either by invoking a specific opcode, if any, or by creating a constant pool entry.
     *
     * @param value The {@code float} value to load onto the stack.
     * @return A stack manipulation for loading the given {@code float} value.
     */
    public static StackManipulation forValue(float value) {
        if (value == 0f) {
            return ZERO;
        } else if (value == 1f) {
            return ONE;
        } else if (value == 2f) {
            return TWO;
        } else {
            return new ConstantPool(value);
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
        return SIZE;
    }

    /**
     * A stack manipulation for loading a {@code float} value from a class's constant pool onto the operand stack.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ConstantPool implements StackManipulation {

        /**
         * The {@code float} value to be loaded onto the operand stack.
         */
        private final float value;

        /**
         * Creates a new constant pool load operation.
         *
         * @param value The {@code float} value to be loaded onto the operand stack.
         */
        protected ConstantPool(float value) {
            this.value = value;
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
            methodVisitor.visitLdcInsn(value);
            return SIZE;
        }
    }
}

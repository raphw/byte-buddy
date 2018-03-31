package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class is responsible for loading any {@code long} constant onto the operand stack.
 */
public enum LongConstant implements StackManipulation {

    /**
     * A {@code long} constant of value {@code 0L}.
     */
    ZERO(Opcodes.LCONST_0),

    /**
     * A {@code long} constant of value {@code 1L}.
     */
    ONE(Opcodes.LCONST_1);

    /**
     * The size impact of loading a {@code double} constant onto the operand stack.
     */
    private static final Size SIZE = StackSize.DOUBLE.toIncreasingSize();

    /**
     * The shortcut opcode for loading a {@code long} constant.
     */
    private final int opcode;

    /**
     * Creates a new shortcut operation for loading a common {@code long} onto the operand stack.
     *
     * @param opcode The shortcut opcode for loading a {@code long} constant.
     */
    LongConstant(int opcode) {
        this.opcode = opcode;
    }

    /**
     * Creates a stack manipulation for loading a {@code long} value onto the operand stack.
     * <p>&nbsp;</p>
     * This is achieved either by invoking a specific opcode, if any, or by creating a constant pool entry.
     *
     * @param value The {@code long} value to load onto the stack.
     * @return A stack manipulation for loading the given {@code long} value.
     */
    public static StackManipulation forValue(long value) {
        if (value == 0L) {
            return ZERO;
        } else if (value == 1L) {
            return ONE;
        } else {
            return new ConstantPool(value);
        }
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitInsn(opcode);
        return SIZE;
    }

    /**
     * A stack manipulation for loading a {@code long} value from a class's constant pool onto the operand stack.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ConstantPool implements StackManipulation {

        /**
         * The {@code long} value to be loaded onto the operand stack.
         */
        private final long value;

        /**
         * Creates a new constant pool load operation.
         *
         * @param value The {@code long} value to be loaded onto the operand stack.
         */
        protected ConstantPool(long value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitLdcInsn(value);
            return SIZE;
        }
    }
}

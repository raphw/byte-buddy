package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
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

    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();
    private final int opcode;

    private FloatConstant(int opcode) {
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

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitInsn(opcode);
        return SIZE;
    }

    private static class ConstantPool implements StackManipulation {

        private final float value;

        private ConstantPool(float value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitLdcInsn(value);
            return SIZE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Float.compare(((ConstantPool) other).value, value) == 0;
        }

        @Override
        public int hashCode() {
            return (value != +0.0f ? Float.floatToIntBits(value) : 0);
        }

        @Override
        public String toString() {
            return "ConstantPool.ConstantPool{value=" + value + '}';
        }
    }
}

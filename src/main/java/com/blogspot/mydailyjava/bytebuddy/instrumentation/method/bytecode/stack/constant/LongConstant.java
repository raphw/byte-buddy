package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class is responsible for loading any {@code long} constant onto the operand stack.
 */
public enum LongConstant implements StackManipulation {

    ZERO(Opcodes.LCONST_0),
    ONE(Opcodes.LCONST_1);

    private static final Size SIZE = StackSize.DOUBLE.toIncreasingSize();

    private static class ConstantPool implements StackManipulation {

        private final long value;

        private ConstantPool(long value) {
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
                    && value == ((ConstantPool) other).value;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }

        @Override
        public String toString() {
            return "LongConstant.ConstantPool{value=" + value + '}';
        }
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

    private final int opcode;

    private LongConstant(int opcode) {
        this.opcode = opcode;
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
}

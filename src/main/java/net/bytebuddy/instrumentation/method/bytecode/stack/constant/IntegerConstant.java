package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * This class is responsible for loading any {@code int} constant onto the operand stack. Note that within the JVM,
 * {@code boolean}, {@code byte}, {@code short} and {@code char} values are represented by integers and can therefore
 * be loaded by using this class.
 */
public enum IntegerConstant implements StackManipulation {

    MINUS_ONE(Opcodes.ICONST_M1),
    ZERO(Opcodes.ICONST_0),
    ONE(Opcodes.ICONST_1),
    TWO(Opcodes.ICONST_2),
    THREE(Opcodes.ICONST_3),
    FOUR(Opcodes.ICONST_4),
    FIVE(Opcodes.ICONST_5);

    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    private static class SingleBytePush implements StackManipulation {

        private final byte value;

        private SingleBytePush(byte value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, value);
            return SIZE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && value == ((SingleBytePush) other).value;
        }

        @Override
        public int hashCode() {
            return (int) value;
        }

        @Override
        public String toString() {
            return "IntegerConstant.SingleBytePush{value=" + value + '}';
        }
    }

    private static class TwoBytePush implements StackManipulation {

        private final short value;

        private TwoBytePush(short value) {
            this.value = value;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitIntInsn(Opcodes.SIPUSH, value);
            return SIZE;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && value == ((TwoBytePush) other).value;
        }

        @Override
        public int hashCode() {
            return (int) value;
        }

        @Override
        public String toString() {
            return "IntegerConstant.TwoBytePush{value=" + value + '}';
        }
    }

    private static class ConstantPoolValue implements StackManipulation {

        private final int value;

        private ConstantPoolValue(int value) {
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
                    && value == ((ConstantPoolValue) other).value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public String toString() {
            return "IntegerConstant.ConstantPoolValue{value=" + value + '}';
        }
    }

    /**
     * Creates a stack manipulation for loading a boolean value onto the stack.
     *
     * @param value The {@code boolean} to load onto the stack.
     * @return The stack manipulation for loading this {@code boolean}.
     */
    public static StackManipulation forValue(boolean value) {
        return value ? ONE : ZERO;
    }

    /**
     * Creates a stack manipulation for loading an {@code int} value onto the stack.
     * <p>&nbsp;</p>
     * This is achieved either by invoking a constant opcode, if any, or by creating a binary push operation.
     *
     * @param value The {@code int} (or {@code byte}, {@code short}, {@code char}) value to load onto the stack.
     * @return A stack manipulation for loading the given value.
     */
    public static StackManipulation forValue(int value) {
        switch (value) {
            case -1:
                return MINUS_ONE;
            case 0:
                return ZERO;
            case 1:
                return ONE;
            case 2:
                return TWO;
            case 3:
                return THREE;
            case 4:
                return FOUR;
            case 5:
                return FIVE;
            default:
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    return new SingleBytePush((byte) value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    return new TwoBytePush((short) value);
                } else {
                    return new ConstantPoolValue(value);
                }
        }
    }

    private final int opcode;

    private IntegerConstant(int opcode) {
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

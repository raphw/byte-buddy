package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.IllegalStackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveWideningDelegate {

    BOOLEAN(LegalTrivialStackManipulation.INSTANCE,                                                // to boolean
            IllegalStackManipulation.INSTANCE,                                                     // to byte
            IllegalStackManipulation.INSTANCE,                                                     // to short
            IllegalStackManipulation.INSTANCE,                                                     // to character
            IllegalStackManipulation.INSTANCE,                                                     // to integer
            IllegalStackManipulation.INSTANCE,                                                     // to long
            IllegalStackManipulation.INSTANCE,                                                     // to float
            IllegalStackManipulation.INSTANCE),                                                    // to double

    BYTE(IllegalStackManipulation.INSTANCE,                                                        // to boolean
            LegalTrivialStackManipulation.INSTANCE,                                                // to byte
            LegalTrivialStackManipulation.INSTANCE,                                                // to short
            IllegalStackManipulation.INSTANCE,                                                     // to character
            LegalTrivialStackManipulation.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize())),       // to double

    SHORT(IllegalStackManipulation.INSTANCE,                                                       // to boolean
            IllegalStackManipulation.INSTANCE,                                                     // to byte
            LegalTrivialStackManipulation.INSTANCE,                                                // to short
            IllegalStackManipulation.INSTANCE,                                                     // to character
            LegalTrivialStackManipulation.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    CHARACTER(IllegalStackManipulation.INSTANCE,                                                   // to boolean
            IllegalStackManipulation.INSTANCE,                                                     // to byte
            IllegalStackManipulation.INSTANCE,                                                     // to short
            LegalTrivialStackManipulation.INSTANCE,                                                // to character
            LegalTrivialStackManipulation.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    INTEGER(IllegalStackManipulation.INSTANCE,                                                     // to boolean
            IllegalStackManipulation.INSTANCE,                                                     // to byte
            IllegalStackManipulation.INSTANCE,                                                     // to short
            IllegalStackManipulation.INSTANCE,                                                     // to character
            LegalTrivialStackManipulation.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    LONG(IllegalStackManipulation.INSTANCE,                                                        // to boolean
            IllegalStackManipulation.INSTANCE,                                                     // to byte
            IllegalStackManipulation.INSTANCE,                                                     // to short
            IllegalStackManipulation.INSTANCE,                                                     // to character
            IllegalStackManipulation.INSTANCE,                                                     // to integer
            LegalTrivialStackManipulation.INSTANCE,                                                // to long
            new WideningStackManipulation(Opcodes.L2F, StackSize.SINGLE.toDecreasingSize()),        // to float
            new WideningStackManipulation(Opcodes.L2D, StackSize.ZERO.toIncreasingSize())),         // to double

    FLOAT(IllegalStackManipulation.INSTANCE,                                                       // to boolean
            IllegalStackManipulation.INSTANCE,                                                     // to byte
            IllegalStackManipulation.INSTANCE,                                                     // to short
            IllegalStackManipulation.INSTANCE,                                                     // to character
            IllegalStackManipulation.INSTANCE,                                                     // to integer
            IllegalStackManipulation.INSTANCE,                                                     // to long
            LegalTrivialStackManipulation.INSTANCE,                                                // to float
            new WideningStackManipulation(Opcodes.F2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    DOUBLE(IllegalStackManipulation.INSTANCE,                                                      // to boolean
            IllegalStackManipulation.INSTANCE,                                                     // to byte
            IllegalStackManipulation.INSTANCE,                                                     // to short
            IllegalStackManipulation.INSTANCE,                                                     // to character
            IllegalStackManipulation.INSTANCE,                                                     // to integer
            IllegalStackManipulation.INSTANCE,                                                     // to long
            IllegalStackManipulation.INSTANCE,                                                     // to float
            LegalTrivialStackManipulation.INSTANCE);                                               // to double

    private static class WideningStackManipulation implements StackManipulation {

        private final int conversionInstruction;
        private final Size size;

        public WideningStackManipulation(int conversionInstruction, Size size) {
            this.conversionInstruction = conversionInstruction;
            this.size = size;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitInsn(conversionInstruction);
            return size;
        }
    }

    public static PrimitiveWideningDelegate forPrimitive(TypeDescription typeDescription) {
        if (typeDescription.represents(boolean.class)) {
            return BOOLEAN;
        } else if (typeDescription.represents(byte.class)) {
            return BYTE;
        } else if (typeDescription.represents(short.class)) {
            return SHORT;
        } else if (typeDescription.represents(char.class)) {
            return CHARACTER;
        } else if (typeDescription.represents(int.class)) {
            return INTEGER;
        } else if (typeDescription.represents(long.class)) {
            return LONG;
        } else if (typeDescription.represents(float.class)) {
            return FLOAT;
        } else if (typeDescription.represents(double.class)) {
            return DOUBLE;
        } else {
            throw new IllegalArgumentException("Not a primitive, non-void type: " + typeDescription);
        }
    }

    private final StackManipulation toBooleanStackManipulation;
    private final StackManipulation toByteStackManipulation;
    private final StackManipulation toShortStackManipulation;
    private final StackManipulation toCharacterStackManipulation;
    private final StackManipulation toIntegerStackManipulation;
    private final StackManipulation toLongStackManipulation;
    private final StackManipulation toFloatStackManipulation;
    private final StackManipulation toDoubleStackManipulation;

    private PrimitiveWideningDelegate(StackManipulation toBooleanStackManipulation,
                                      StackManipulation toByteStackManipulation,
                                      StackManipulation toShortStackManipulation,
                                      StackManipulation toCharacterStackManipulation,
                                      StackManipulation toIntegerStackManipulation,
                                      StackManipulation toLongStackManipulation,
                                      StackManipulation toFloatStackManipulation,
                                      StackManipulation toDoubleStackManipulation) {
        this.toBooleanStackManipulation = toBooleanStackManipulation;
        this.toByteStackManipulation = toByteStackManipulation;
        this.toShortStackManipulation = toShortStackManipulation;
        this.toCharacterStackManipulation = toCharacterStackManipulation;
        this.toIntegerStackManipulation = toIntegerStackManipulation;
        this.toLongStackManipulation = toLongStackManipulation;
        this.toFloatStackManipulation = toFloatStackManipulation;
        this.toDoubleStackManipulation = toDoubleStackManipulation;
    }

    public StackManipulation widenTo(TypeDescription typeDescription) {
        if (typeDescription.represents(boolean.class)) {
            return toBooleanStackManipulation;
        } else if (typeDescription.represents(byte.class)) {
            return toByteStackManipulation;
        } else if (typeDescription.represents(short.class)) {
            return toShortStackManipulation;
        } else if (typeDescription.represents(char.class)) {
            return toCharacterStackManipulation;
        } else if (typeDescription.represents(int.class)) {
            return toIntegerStackManipulation;
        } else if (typeDescription.represents(long.class)) {
            return toLongStackManipulation;
        } else if (typeDescription.represents(float.class)) {
            return toFloatStackManipulation;
        } else if (typeDescription.represents(double.class)) {
            return toDoubleStackManipulation;
        } else {
            throw new IllegalArgumentException("Not a primitive non-void type: " + typeDescription);
        }
    }
}

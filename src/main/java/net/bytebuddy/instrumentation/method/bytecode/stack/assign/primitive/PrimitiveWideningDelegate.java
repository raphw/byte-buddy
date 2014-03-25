package net.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.IllegalStackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This delegate is responsible for widening a primitive type to represent a <i>larger</i> primitive type. The
 * rules for this widening are equivalent to those in the <a href="http://docs.oracle.com/javase/specs/">JLS</a>.
 */
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

    /**
     * Locates the delegate that is capable of widening the given type into another type.
     *
     * @param typeDescription A non-void primitive type that is to be widened into another type.
     * @return A delegate for the given type.
     */
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

    /**
     * Attempts to widen the represented type into another type.
     *
     * @param typeDescription A non-void primitive type that is the expected result of the widening operation.
     * @return A widening instruction or an illegal stack manipulation if such widening is not legitimate.
     */
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

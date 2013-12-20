package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.LegalTrivialAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.utility.TypeSymbol;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveWideningAssigner {

    BOOLEAN(new LegalTrivialAssignment(1),              // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            IllegalAssignment.INSTANCE,                 // to long
            IllegalAssignment.INSTANCE,                 // to float
            IllegalAssignment.INSTANCE),                // to double

    BYTE(IllegalAssignment.INSTANCE,                    // to boolean
            new LegalTrivialAssignment(1),              // to byte
            new LegalTrivialAssignment(1),              // to short
            IllegalAssignment.INSTANCE,                 // to character
            new LegalTrivialAssignment(1),              // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2L, 2, 2)), // to double

    SHORT(IllegalAssignment.INSTANCE,                   // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            new LegalTrivialAssignment(1),              // to short
            IllegalAssignment.INSTANCE,                 // to character
            new LegalTrivialAssignment(1),              // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2D, 2, 2)), // to double

    CHARACTER(IllegalAssignment.INSTANCE,               // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            new LegalTrivialAssignment(1),              // to character
            new LegalTrivialAssignment(1),              // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2D, 2, 2)), // to double

    INTEGER(IllegalAssignment.INSTANCE,                 // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            new LegalTrivialAssignment(1),              // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2D, 2, 2)), // to double

    LONG(IllegalAssignment.INSTANCE,                    // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            new LegalTrivialAssignment(2),              // to long
            new WideningAssignment(Opcodes.L2F, 1, 2),  // to float
            new WideningAssignment(Opcodes.L2D, 2, 2)), // to double

    FLOAT(IllegalAssignment.INSTANCE,                   // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            IllegalAssignment.INSTANCE,                 // to long
            new LegalTrivialAssignment(1),              // to float
            new WideningAssignment(Opcodes.F2L, 2, 2)), // to double

    DOUBLE(IllegalAssignment.INSTANCE,                  // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            IllegalAssignment.INSTANCE,                 // to long
            IllegalAssignment.INSTANCE,                 // to float
            new LegalTrivialAssignment(2));             // to double

    private static class WideningAssignment implements Assignment {

        private final int conversionInstruction;
        private final int finalOperandStackSize, maximalOperandStackSize;

        public WideningAssignment(int conversionInstruction, int finalOperandStackSize, int maximalOperandStackSize) {
            this.conversionInstruction = conversionInstruction;
            this.finalOperandStackSize = finalOperandStackSize;
            this.maximalOperandStackSize = maximalOperandStackSize;
        }

        @Override
        public boolean isAssignable() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitInsn(conversionInstruction);
            return new Size(finalOperandStackSize, maximalOperandStackSize);
        }
    }

    public static PrimitiveWideningAssigner of(String typeName) {
        switch (typeName.charAt(0)) {
            case TypeSymbol.BOOLEAN:
                return BOOLEAN;
            case TypeSymbol.BYTE:
                return BYTE;
            case TypeSymbol.SHORT:
                return SHORT;
            case TypeSymbol.CHAR:
                return CHARACTER;
            case TypeSymbol.INT:
                return INTEGER;
            case TypeSymbol.LONG:
                return LONG;
            case TypeSymbol.FLOAT:
                return FLOAT;
            case TypeSymbol.DOUBLE:
                return DOUBLE;
            default:
                throw new IllegalStateException("Not a primitive type: " + typeName);
        }
    }

    public static PrimitiveWideningAssigner of(Class<?> type) {
        if (type == boolean.class) {
            return BOOLEAN;
        } else if (type == byte.class) {
            return BYTE;
        } else if (type == short.class) {
            return SHORT;
        } else if (type == char.class) {
            return CHARACTER;
        } else if (type == int.class) {
            return INTEGER;
        } else if (type == long.class) {
            return LONG;
        } else if (type == float.class) {
            return FLOAT;
        } else if (type == double.class) {
            return DOUBLE;
        } else {
            throw new IllegalStateException("Not a primitive type: " + type);
        }
    }

    private final Assignment toBooleanAssignment;
    private final Assignment toByteAssignment;
    private final Assignment toShortAssignment;
    private final Assignment toCharacterAssignment;
    private final Assignment toIntegerAssignment;
    private final Assignment toLongAssignment;
    private final Assignment toFloatAssignment;
    private final Assignment toDoubleAssignment;

    private PrimitiveWideningAssigner(Assignment toBooleanAssignment, Assignment toByteAssignment,
                                      Assignment toShortAssignment, Assignment toCharacterAssignment,
                                      Assignment toIntegerAssignment, Assignment toLongAssignment,
                                      Assignment toFloatAssignment, Assignment toDoubleAssignment) {
        this.toBooleanAssignment = toBooleanAssignment;
        this.toByteAssignment = toByteAssignment;
        this.toShortAssignment = toShortAssignment;
        this.toCharacterAssignment = toCharacterAssignment;
        this.toIntegerAssignment = toIntegerAssignment;
        this.toLongAssignment = toLongAssignment;
        this.toFloatAssignment = toFloatAssignment;
        this.toDoubleAssignment = toDoubleAssignment;
    }

    public Assignment widenTo(Class<?> type) {
        if (type == boolean.class) {
            return toBooleanAssignment;
        } else if (type == byte.class) {
            return toByteAssignment;
        } else if (type == short.class) {
            return toShortAssignment;
        } else if (type == char.class) {
            return toCharacterAssignment;
        } else if (type == int.class) {
            return toIntegerAssignment;
        } else if (type == long.class) {
            return toLongAssignment;
        } else if (type == float.class) {
            return toFloatAssignment;
        } else if (type == double.class) {
            return toDoubleAssignment;
        } else {
            throw new IllegalStateException("Not a primitive type: " + type);
        }
    }

    public Assignment widenTo(String typeName) {
        switch (typeName.charAt(0)) {
            case TypeSymbol.BOOLEAN:
                return toBooleanAssignment;
            case TypeSymbol.BYTE:
                return toByteAssignment;
            case TypeSymbol.SHORT:
                return toShortAssignment;
            case TypeSymbol.CHAR:
                return toCharacterAssignment;
            case TypeSymbol.INT:
                return toIntegerAssignment;
            case TypeSymbol.LONG:
                return toLongAssignment;
            case TypeSymbol.FLOAT:
                return toFloatAssignment;
            case TypeSymbol.DOUBLE:
                return toDoubleAssignment;
            default:
                throw new IllegalStateException("Not a primitive type: " + typeName);
        }
    }
}

package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.primitive;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.IllegalAssignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.LegalTrivialAssignment;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveWideningAssigner {

    BOOLEAN(LegalTrivialAssignment.INSTANCE,            // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            IllegalAssignment.INSTANCE,                 // to long
            IllegalAssignment.INSTANCE,                 // to float
            IllegalAssignment.INSTANCE),                // to double

    BYTE(IllegalAssignment.INSTANCE,                    // to boolean
            LegalTrivialAssignment.INSTANCE,            // to byte
            LegalTrivialAssignment.INSTANCE,            // to short
            IllegalAssignment.INSTANCE,                 // to character
            LegalTrivialAssignment.INSTANCE,            // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2L, 2, 2)), // to double

    SHORT(IllegalAssignment.INSTANCE,                   // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            LegalTrivialAssignment.INSTANCE,            // to short
            IllegalAssignment.INSTANCE,                 // to character
            LegalTrivialAssignment.INSTANCE,            // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2D, 2, 2)), // to double

    CHARACTER(IllegalAssignment.INSTANCE,               // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            LegalTrivialAssignment.INSTANCE,            // to character
            LegalTrivialAssignment.INSTANCE,            // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2D, 2, 2)), // to double

    INTEGER(IllegalAssignment.INSTANCE,                 // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            LegalTrivialAssignment.INSTANCE,            // to integer
            new WideningAssignment(Opcodes.I2L, 2, 2),  // to long
            new WideningAssignment(Opcodes.I2F, 1, 1),  // to float
            new WideningAssignment(Opcodes.I2D, 2, 2)), // to double

    LONG(IllegalAssignment.INSTANCE,                    // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            LegalTrivialAssignment.INSTANCE,            // to long
            new WideningAssignment(Opcodes.L2F, 1, 2),  // to float
            new WideningAssignment(Opcodes.L2D, 2, 2)), // to double

    FLOAT(IllegalAssignment.INSTANCE,                   // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            IllegalAssignment.INSTANCE,                 // to long
            LegalTrivialAssignment.INSTANCE,            // to float
            new WideningAssignment(Opcodes.F2L, 2, 2)), // to double

    DOUBLE(IllegalAssignment.INSTANCE,                  // to boolean
            IllegalAssignment.INSTANCE,                 // to byte
            IllegalAssignment.INSTANCE,                 // to short
            IllegalAssignment.INSTANCE,                 // to character
            IllegalAssignment.INSTANCE,                 // to integer
            IllegalAssignment.INSTANCE,                 // to long
            IllegalAssignment.INSTANCE,                 // to float
            LegalTrivialAssignment.INSTANCE);           // to double

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
        public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
            methodVisitor.visitInsn(conversionInstruction);
            return new Size(finalOperandStackSize, maximalOperandStackSize);
        }
    }

    private static final char BOOLEAN_TYPE = 'Z';
    private static final char BYTE_TYPE = 'B';
    private static final char SHORT_TYPE = 'S';
    private static final char CHARACTER_TYPE = 'C';
    private static final char INTEGER_TYPE = 'I';
    private static final char LONG_TYPE = 'J';
    private static final char FLOAT_TYPE = 'F';
    private static final char DOUBLE_TYPE = 'D';

    public static PrimitiveWideningAssigner of(String typeName) {
        switch (typeName.charAt(0)) {
            case BOOLEAN_TYPE:
                return BOOLEAN;
            case BYTE_TYPE:
                return BYTE;
            case SHORT_TYPE:
                return SHORT;
            case CHARACTER_TYPE:
                return CHARACTER;
            case INTEGER_TYPE:
                return INTEGER;
            case LONG_TYPE:
                return LONG;
            case FLOAT_TYPE:
                return FLOAT;
            case DOUBLE_TYPE:
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

    PrimitiveWideningAssigner(Assignment toBooleanAssignment, Assignment toByteAssignment,
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
            case BOOLEAN_TYPE:
                return toBooleanAssignment;
            case BYTE_TYPE:
                return toByteAssignment;
            case SHORT_TYPE:
                return toShortAssignment;
            case CHARACTER_TYPE:
                return toCharacterAssignment;
            case INTEGER_TYPE:
                return toIntegerAssignment;
            case LONG_TYPE:
                return toLongAssignment;
            case FLOAT_TYPE:
                return toFloatAssignment;
            case DOUBLE_TYPE:
                return toDoubleAssignment;
            default:
                throw new IllegalStateException("Not a primitive type: " + typeName);
        }
    }
}

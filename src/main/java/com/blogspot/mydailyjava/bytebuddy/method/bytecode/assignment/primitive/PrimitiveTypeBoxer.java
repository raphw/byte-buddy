package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.primitive;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assignment.AssignmentExaminer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveTypeBoxer {

    BOOLEAN("java/lang/Boolean", 1, Boolean.class, boolean.class, "valueOf", "(Z)Ljava/lang/Boolean;", "booleanValue", "()Z"),
    BYTE("java/lang/Byte", 1, Byte.class, byte.class, "valueOf", "(B)Ljava/lang/Byte;", "byteValue", "()B"),
    SHORT("java/lang/Short", 1, Short.class, short.class, "valueOf", "(S)Ljava/lang/Short;", "shortValue", "()S"),
    CHARACTER("java/lang/Character", 1, Character.class, char.class, "valueOf", "(C)Ljava/lang/Character;", "charValue", "()C"),
    INTEGER("java/lang/Integer", 1, Integer.class, int.class, "valueOf", "(I)Ljava/lang/Integer;", "intValue", "()I"),
    LONG("java/lang/Long", 2, Long.class, long.class, "valueOf", "(J)Ljava/lang/Long;", "longValue", "()J"),
    FLOAT("java/lang/Float", 1, Float.class, float.class, "valueOf", "(F)Ljava/lang/Float;", "floatValue", "()F"),
    DOUBLE("java/lang/Double", 2, Double.class, double.class, "valueOf", "(D)Ljava/lang/Double;", "doubleValue", "()D");

    private static final char BOOLEAN_TYPE = 'Z';
    private static final char BYTE_TYPE = 'B';
    private static final char SHORT_TYPE = 'S';
    private static final char CHARACTER_TYPE = 'C';
    private static final char INTEGER_TYPE = 'I';
    private static final char LONG_TYPE = 'J';
    private static final char FLOAT_TYPE = 'F';
    private static final char DOUBLE_TYPE = 'D';

    public static PrimitiveTypeBoxer of(String typeName) {
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

    public static PrimitiveTypeBoxer of(Class<?> type) {
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

    private final String wrapperTypeName;
    private final int operandStackSize;
    private final Class<?> wrapperType;
    private final Class<?> primitiveType;
    private final String boxingMethodName;
    private final String boxingMethodDescriptor;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;

    private PrimitiveTypeBoxer(String wrapperTypeName, int operandStackSize,
                               Class<?> wrapperType, Class<?> primitiveType,
                               String boxingMethodName, String boxingMethodDescriptor,
                               String unboxingMethodName, String unboxingMethodDescriptor) {
        this.wrapperTypeName = wrapperTypeName;
        this.operandStackSize = operandStackSize;
        this.wrapperType = wrapperType;
        this.primitiveType = primitiveType;
        this.boxingMethodName = boxingMethodName;
        this.boxingMethodDescriptor = boxingMethodDescriptor;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
    }

    private class BoxingAssignment implements Assignment {

        private final Assignment assignment;

        public BoxingAssignment(Assignment assignment) {
            this.assignment = assignment;
        }

        @Override
        public boolean isAssignable() {
            return assignment.isAssignable();
        }

        @Override
        public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperTypeName, boxingMethodName, boxingMethodDescriptor);
            return assignment.load(methodVisitor, classContext, methodContext).consume(1, 1);
        }
    }

    private class UnboxingAssignment implements Assignment {

        private final Assignment assignment;

        public UnboxingAssignment(Assignment assignment) {
            this.assignment = assignment;
        }

        @Override
        public boolean isAssignable() {
            return assignment.isAssignable();
        }

        @Override
        public Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKEDYNAMIC, wrapperTypeName, unboxingMethodName, unboxingMethodDescriptor);
            return assignment.load(methodVisitor, classContext, methodContext).consume(operandStackSize - 1, operandStackSize - 1);
        }
    }

    public Assignment boxingAssignmentBefore(AssignmentExaminer assignmentExaminer, Class<?> subType) {
        return new BoxingAssignment(assignmentExaminer.assign(wrapperTypeName, subType));
    }

    public Assignment boxingAssignmentBefore(AssignmentExaminer assignmentExaminer, String subTypeName) {
        return new BoxingAssignment(assignmentExaminer.assign(wrapperType, subTypeName));
    }

    public Assignment unboxingAssignmentTo(Class<?> subType) {
        return new UnboxingAssignment(PrimitiveWideningAssigner.of(primitiveType).widenTo(subType));
    }

    public Assignment unboxingAssignmentTo(String subTypeName) {
        return new UnboxingAssignment(PrimitiveWideningAssigner.of(primitiveType).widenTo(subTypeName));
    }
}

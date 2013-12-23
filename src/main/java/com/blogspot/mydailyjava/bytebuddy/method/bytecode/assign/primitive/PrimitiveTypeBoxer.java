package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.AssignmentExaminer;
import com.blogspot.mydailyjava.bytebuddy.method.utility.MethodDescriptor;
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

    public static PrimitiveTypeBoxer of(String typeName) {
        switch (typeName.charAt(0)) {
            case MethodDescriptor.BOOLEAN_SYMBOL:
                return BOOLEAN;
            case MethodDescriptor.BYTE_SYMBOL:
                return BYTE;
            case MethodDescriptor.SHORT_SYMBOL:
                return SHORT;
            case MethodDescriptor.CHAR_SYMBOL:
                return CHARACTER;
            case MethodDescriptor.INT_SYMBOL:
                return INTEGER;
            case MethodDescriptor.LONG_SYMBOL:
                return LONG;
            case MethodDescriptor.FLOAT_SYMBOL:
                return FLOAT;
            case MethodDescriptor.DOUBLE_SYMBOL:
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
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperTypeName, boxingMethodName, boxingMethodDescriptor);
            return assignment.apply(methodVisitor).aggregateLeftFirst(operandStackSize - 1);
        }
    }

    private class UnboxingAssignment implements Assignment {

        private final Assignment referenceAssignment;
        private final Assignment wideningAssignment;

        public UnboxingAssignment(Assignment referenceAssignment, Assignment wideningAssignment) {
            this.referenceAssignment = referenceAssignment;
            this.wideningAssignment = wideningAssignment;
        }

        @Override
        public boolean isAssignable() {
            return wideningAssignment.isAssignable() && referenceAssignment.isAssignable();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            Size size = referenceAssignment.apply(methodVisitor);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEDYNAMIC, wrapperTypeName, unboxingMethodName, unboxingMethodDescriptor);
            return size.aggregateLeftFirst(wideningAssignment.apply(methodVisitor).aggregateRightFirst(operandStackSize - 1));
        }
    }

    public Assignment boxAndAssignTo(Class<?> subType, AssignmentExaminer assignmentExaminer, boolean considerRuntimeType) {
        return new BoxingAssignment(assignmentExaminer.assign(wrapperTypeName, subType, considerRuntimeType));
    }

    public Assignment boxAndAssignTo(String subTypeName, AssignmentExaminer assignmentExaminer, boolean considerRuntimeType) {
        return new BoxingAssignment(assignmentExaminer.assign(wrapperType, subTypeName, considerRuntimeType));
    }

    public Assignment unboxAndAssignTo(Class<?> superType, AssignmentExaminer assignmentExaminer) {
        return new UnboxingAssignment(null, PrimitiveWideningAssigner.of(primitiveType).widenTo(superType));
    }

    public Assignment unboxAndAssignTo(String superTypeName, AssignmentExaminer assignmentExaminer) {
        return new UnboxingAssignment(null, PrimitiveWideningAssigner.of(primitiveType).widenTo(superTypeName));
    }
}

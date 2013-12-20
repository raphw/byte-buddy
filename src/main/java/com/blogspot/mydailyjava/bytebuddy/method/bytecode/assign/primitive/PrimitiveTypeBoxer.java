package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.AssignmentExaminer;
import com.blogspot.mydailyjava.bytebuddy.method.utility.TypeSymbol;
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

    private static final int REFERENCE_SIZE = 1;

    public static PrimitiveTypeBoxer of(String typeName) {
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
            return assignment.apply(methodVisitor).withMaximum(REFERENCE_SIZE);
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
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKEDYNAMIC, wrapperTypeName, unboxingMethodName, unboxingMethodDescriptor);
            return assignment.apply(methodVisitor).withMaximum(operandStackSize);
        }
    }

    public Assignment boxAndAssignTo(Class<?> subType, AssignmentExaminer assignmentExaminer) {
        return new BoxingAssignment(assignmentExaminer.assign(wrapperTypeName, subType));
    }

    public Assignment boxAndAssignTo(String subTypeName, AssignmentExaminer assignmentExaminer) {
        return new BoxingAssignment(assignmentExaminer.assign(wrapperType, subTypeName));
    }

    public Assignment unboxAndAssignTo(Class<?> subType) {
        return new UnboxingAssignment(PrimitiveWideningAssigner.of(primitiveType).widenTo(subType));
    }

    public Assignment unboxAndAssignTo(String subTypeName) {
        return new UnboxingAssignment(PrimitiveWideningAssigner.of(primitiveType).widenTo(subTypeName));
    }
}

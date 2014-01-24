package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveBoxingDelegate {

    BOOLEAN("java/lang/Boolean", TypeSize.NONE, Boolean.class, "valueOf", "(Z)Ljava/lang/Boolean;"),
    BYTE("java/lang/Byte", TypeSize.NONE, Byte.class, "valueOf", "(B)Ljava/lang/Byte;"),
    SHORT("java/lang/Short", TypeSize.NONE, Short.class, "valueOf", "(S)Ljava/lang/Short;"),
    CHARACTER("java/lang/Character", TypeSize.NONE, Character.class, "valueOf", "(C)Ljava/lang/Character;"),
    INTEGER("java/lang/Integer", TypeSize.NONE, Integer.class, "valueOf", "(I)Ljava/lang/Integer;"),
    LONG("java/lang/Long", TypeSize.SINGLE, Long.class, "valueOf", "(J)Ljava/lang/Long;"),
    FLOAT("java/lang/Float", TypeSize.NONE, Float.class, "valueOf", "(F)Ljava/lang/Float;"),
    DOUBLE("java/lang/Double", TypeSize.SINGLE, Double.class, "valueOf", "(D)Ljava/lang/Double;");

    public static PrimitiveBoxingDelegate forPrimitive(Class<?> type) {
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
            throw new IllegalArgumentException("Not a non-void, primitive type: " + type);
        }
    }

    private final String wrapperTypeName;
    private final Assignment.Size size;
    private final Class<?> wrapperType;
    private final String boxingMethodName;
    private final String boxingMethodDescriptor;

    private PrimitiveBoxingDelegate(String wrapperTypeName,
                                    TypeSize sizeDecrease,
                                    Class<?> wrapperType,
                                    String boxingMethodName,
                                    String boxingMethodDescriptor) {
        this.wrapperTypeName = wrapperTypeName;
        this.size = sizeDecrease.toDecreasingSize();
        this.wrapperType = wrapperType;
        this.boxingMethodName = boxingMethodName;
        this.boxingMethodDescriptor = boxingMethodDescriptor;
    }

    private class BoxingAssignment implements Assignment {

        private final Assignment assignment;

        public BoxingAssignment(Assignment assignment) {
            this.assignment = assignment;
        }

        @Override
        public boolean isValid() {
            return assignment.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperTypeName, boxingMethodName, boxingMethodDescriptor);
            return size.aggregate(assignment.apply(methodVisitor));
        }
    }

    public Assignment assignBoxedTo(Class<?> subType, Assigner chainedAssigner, boolean considerRuntimeType) {
        return new BoxingAssignment(chainedAssigner.assign(wrapperType, subType, considerRuntimeType));
    }
}

package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveBoxingDelegate {

    BOOLEAN("java/lang/Boolean", TypeSize.ZERO, Boolean.class, "valueOf", "(Z)Ljava/lang/Boolean;"),
    BYTE("java/lang/Byte", TypeSize.ZERO, Byte.class, "valueOf", "(B)Ljava/lang/Byte;"),
    SHORT("java/lang/Short", TypeSize.ZERO, Short.class, "valueOf", "(S)Ljava/lang/Short;"),
    CHARACTER("java/lang/Character", TypeSize.ZERO, Character.class, "valueOf", "(C)Ljava/lang/Character;"),
    INTEGER("java/lang/Integer", TypeSize.ZERO, Integer.class, "valueOf", "(I)Ljava/lang/Integer;"),
    LONG("java/lang/Long", TypeSize.SINGLE, Long.class, "valueOf", "(J)Ljava/lang/Long;"),
    FLOAT("java/lang/Float", TypeSize.ZERO, Float.class, "valueOf", "(F)Ljava/lang/Float;"),
    DOUBLE("java/lang/Double", TypeSize.SINGLE, Double.class, "valueOf", "(D)Ljava/lang/Double;");

    public static PrimitiveBoxingDelegate forPrimitive(TypeDescription typeDescription) {
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
            throw new IllegalArgumentException("Not a non-void, primitive type: " + typeDescription);
        }
    }

    private final String wrapperTypeName;
    private final Assignment.Size size;
    private final TypeDescription wrapperType;
    private final String boxingMethodName;
    private final String boxingMethodDescriptor;

    private PrimitiveBoxingDelegate(String wrapperTypeName,
                                    TypeSize sizeDecrease,
                                    Class<?> wrapperType,
                                    String boxingMethodName,
                                    String boxingMethodDescriptor) {
        this.wrapperTypeName = wrapperTypeName;
        this.size = sizeDecrease.toDecreasingSize();
        this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
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

    public Assignment assignBoxedTo(TypeDescription subType, Assigner chainedAssigner, boolean considerRuntimeType) {
        return new BoxingAssignment(chainedAssigner.assign(wrapperType, subType, considerRuntimeType));
    }
}

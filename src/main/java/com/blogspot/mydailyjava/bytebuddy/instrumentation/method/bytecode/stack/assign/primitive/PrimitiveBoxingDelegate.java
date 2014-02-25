package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveBoxingDelegate {

    BOOLEAN("java/lang/Boolean", StackSize.ZERO, Boolean.class, "valueOf", "(Z)Ljava/lang/Boolean;"),
    BYTE("java/lang/Byte", StackSize.ZERO, Byte.class, "valueOf", "(B)Ljava/lang/Byte;"),
    SHORT("java/lang/Short", StackSize.ZERO, Short.class, "valueOf", "(S)Ljava/lang/Short;"),
    CHARACTER("java/lang/Character", StackSize.ZERO, Character.class, "valueOf", "(C)Ljava/lang/Character;"),
    INTEGER("java/lang/Integer", StackSize.ZERO, Integer.class, "valueOf", "(I)Ljava/lang/Integer;"),
    LONG("java/lang/Long", StackSize.SINGLE, Long.class, "valueOf", "(J)Ljava/lang/Long;"),
    FLOAT("java/lang/Float", StackSize.ZERO, Float.class, "valueOf", "(F)Ljava/lang/Float;"),
    DOUBLE("java/lang/Double", StackSize.SINGLE, Double.class, "valueOf", "(D)Ljava/lang/Double;");

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
    private final StackManipulation.Size size;
    private final TypeDescription wrapperType;
    private final String boxingMethodName;
    private final String boxingMethodDescriptor;

    private PrimitiveBoxingDelegate(String wrapperTypeName,
                                    StackSize sizeDecrease,
                                    Class<?> wrapperType,
                                    String boxingMethodName,
                                    String boxingMethodDescriptor) {
        this.wrapperTypeName = wrapperTypeName;
        this.size = sizeDecrease.toDecreasingSize();
        this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
        this.boxingMethodName = boxingMethodName;
        this.boxingMethodDescriptor = boxingMethodDescriptor;
    }

    private class BoxingStackManipulation implements StackManipulation {

        private final StackManipulation stackManipulation;

        public BoxingStackManipulation(StackManipulation stackManipulation) {
            this.stackManipulation = stackManipulation;
        }

        @Override
        public boolean isValid() {
            return stackManipulation.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperTypeName, boxingMethodName, boxingMethodDescriptor);
            return size.aggregate(stackManipulation.apply(methodVisitor, instrumentationContext));
        }
    }

    public StackManipulation assignBoxedTo(TypeDescription subType, Assigner chainedAssigner, boolean considerRuntimeType) {
        return new BoxingStackManipulation(chainedAssigner.assign(wrapperType, subType, considerRuntimeType));
    }
}

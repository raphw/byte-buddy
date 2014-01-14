package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveUnboxingDelegate implements UnboxingResponsible {

    BOOLEAN("java/lang/Boolean", TypeSize.SINGLE, Boolean.class, boolean.class, "valueOf", "(Z)Ljava/lang/Boolean;", "booleanValue", "()Z"),
    BYTE("java/lang/Byte", TypeSize.SINGLE, Byte.class, byte.class, "valueOf", "(B)Ljava/lang/Byte;", "byteValue", "()B"),
    SHORT("java/lang/Short", TypeSize.SINGLE, Short.class, short.class, "valueOf", "(S)Ljava/lang/Short;", "shortValue", "()S"),
    CHARACTER("java/lang/Character", TypeSize.SINGLE, Character.class, char.class, "valueOf", "(C)Ljava/lang/Character;", "charValue", "()C"),
    INTEGER("java/lang/Integer", TypeSize.SINGLE, Integer.class, int.class, "valueOf", "(I)Ljava/lang/Integer;", "intValue", "()I"),
    LONG("java/lang/Long", TypeSize.DOUBLE, Long.class, long.class, "valueOf", "(J)Ljava/lang/Long;", "longValue", "()J"),
    FLOAT("java/lang/Float", TypeSize.SINGLE, Float.class, float.class, "valueOf", "(F)Ljava/lang/Float;", "floatValue", "()F"),
    DOUBLE("java/lang/Double", TypeSize.DOUBLE, Double.class, double.class, "valueOf", "(D)Ljava/lang/Double;", "doubleValue", "()D");

    public static PrimitiveUnboxingDelegate forPrimitive(Class<?> type) {
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

    public static UnboxingResponsible forNonPrimitive(Class<?> type) {
        if (type == Boolean.class) {
            return BOOLEAN;
        } else if (type == Byte.class) {
            return BYTE;
        } else if (type == Short.class) {
            return SHORT;
        } else if (type == Character.class) {
            return CHARACTER;
        } else if (type == Integer.class) {
            return INTEGER;
        } else if (type == Long.class) {
            return LONG;
        } else if (type == Float.class) {
            return FLOAT;
        } else if (type == Double.class) {
            return DOUBLE;
        } else {
            return new ImplicitUnboxingResponsible(type);
        }
    }

    private static class ImplicitUnboxingResponsible implements UnboxingResponsible {

        private final Class<?> type;

        private ImplicitUnboxingResponsible(Class<?> type) {
            this.type = type;
        }

        @Override
        public Assignment unboxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
            PrimitiveUnboxingDelegate delegate = PrimitiveUnboxingDelegate.forPrimitive(subType);
            return delegate.new ImplicitlyTypedUnboxingAssignment(assigner.assign(type, delegate.wrapperType, considerRuntimeType));
        }
    }

    private final String wrapperTypeName;
    private final TypeSize typeSize;
    private final Class<?> wrapperType;
    private final Class<?> primitiveType;
    private final String boxingMethodName;
    private final String boxingMethodDescriptor;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;

    private PrimitiveUnboxingDelegate(String wrapperTypeName, TypeSize typeSize,
                                      Class<?> wrapperType, Class<?> primitiveType,
                                      String boxingMethodName, String boxingMethodDescriptor,
                                      String unboxingMethodName, String unboxingMethodDescriptor) {
        this.wrapperTypeName = wrapperTypeName;
        this.typeSize = typeSize;
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
            return assignment.apply(methodVisitor).aggregateLeftFirst(typeSize.getSize() - 1);
        }
    }

    private class ExplicitlyTypedUnboxingAssignment implements Assignment {

        private final Assignment wideningAssignment;

        public ExplicitlyTypedUnboxingAssignment(Assignment wideningAssignment) {
            this.wideningAssignment = wideningAssignment;
        }

        @Override
        public boolean isAssignable() {
            return wideningAssignment.isAssignable();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKEDYNAMIC, wrapperTypeName, unboxingMethodName, unboxingMethodDescriptor);
            return wideningAssignment.apply(methodVisitor).aggregateRightFirst(typeSize.getSize() - 1);
        }
    }

    private class ImplicitlyTypedUnboxingAssignment implements Assignment {

        private final Assignment referenceTypeAdjustmentAssignment;

        private ImplicitlyTypedUnboxingAssignment(Assignment referenceTypeAdjustmentAssignment) {
            this.referenceTypeAdjustmentAssignment = referenceTypeAdjustmentAssignment;
        }

        @Override
        public boolean isAssignable() {
            return referenceTypeAdjustmentAssignment.isAssignable();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            Size size = referenceTypeAdjustmentAssignment.apply(methodVisitor);
            methodVisitor.visitMethodInsn(Opcodes.INVOKEDYNAMIC, wrapperTypeName, unboxingMethodName, unboxingMethodDescriptor);
            return size.aggregateLeftFirst(typeSize.getSize() - 1);
        }
    }

    public Assignment boxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
        return new BoxingAssignment(assigner.assign(wrapperType, subType, considerRuntimeType));
    }

    @Override
    public Assignment unboxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
        return new ExplicitlyTypedUnboxingAssignment(PrimitiveWideningDelegate.forPrimitive(primitiveType).widenTo(subType));
    }
}

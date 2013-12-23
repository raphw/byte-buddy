package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.method.utility.MethodDescriptor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public enum PrimitiveUnboxingDelegate implements UnboxingResponsible {

    BOOLEAN("java/lang/Boolean", 1, Boolean.class, boolean.class, "valueOf", "(Z)Ljava/lang/Boolean;", "booleanValue", "()Z"),
    BYTE("java/lang/Byte", 1, Byte.class, byte.class, "valueOf", "(B)Ljava/lang/Byte;", "byteValue", "()B"),
    SHORT("java/lang/Short", 1, Short.class, short.class, "valueOf", "(S)Ljava/lang/Short;", "shortValue", "()S"),
    CHARACTER("java/lang/Character", 1, Character.class, char.class, "valueOf", "(C)Ljava/lang/Character;", "charValue", "()C"),
    INTEGER("java/lang/Integer", 1, Integer.class, int.class, "valueOf", "(I)Ljava/lang/Integer;", "intValue", "()I"),
    LONG("java/lang/Long", 2, Long.class, long.class, "valueOf", "(J)Ljava/lang/Long;", "longValue", "()J"),
    FLOAT("java/lang/Float", 1, Float.class, float.class, "valueOf", "(F)Ljava/lang/Float;", "floatValue", "()F"),
    DOUBLE("java/lang/Double", 2, Double.class, double.class, "valueOf", "(D)Ljava/lang/Double;", "doubleValue", "()D");

    public static PrimitiveUnboxingDelegate forPrimitive(String typeName) {
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

    public static UnboxingResponsible forType(String typeName) {
        if (BOOLEAN_TYPE_NAME.equals(typeName)) {
            return BOOLEAN;
        } else if (BYTE_TYPE_NAME.equals(typeName)) {
            return BYTE;
        } else if (SHORT_TYPE_NAME.equals(typeName)) {
            return SHORT;
        } else if (CHARACTER_TYPE_NAME.equals(typeName)) {
            return CHARACTER;
        } else if (INTEGER_TYPE_NAME.equals(typeName)) {
            return INTEGER;
        } else if (LONG_TYPE_NAME.equals(typeName)) {
            return LONG;
        } else if (FLOAT_TYPE_NAME.equals(typeName)) {
            return FLOAT;
        } else if (DOUBLE_TYPE_NAME.equals(typeName)) {
            return DOUBLE;
        } else {
            return new ImplicitUnboxingResponsible(typeName);
        }
    }

    public static UnboxingResponsible forType(Class<?> type) {
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

        private final String typeName;

        private ImplicitUnboxingResponsible(Class<?> type) {
            this.typeName = Type.getInternalName(type);
        }

        private ImplicitUnboxingResponsible(String typeName) {
            this.typeName = typeName;
        }

        @Override
        public Assignment unboxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
            PrimitiveUnboxingDelegate delegate = PrimitiveUnboxingDelegate.forPrimitive(subType);
            return delegate.new ImplicitlyTypedUnboxingAssignment(assigner.assign(typeName, delegate.wrapperType, considerRuntimeType));
        }

        @Override
        public Assignment unboxAndAssignTo(String subTypeName, Assigner assigner, boolean considerRuntimeType) {
            PrimitiveUnboxingDelegate delegate = PrimitiveUnboxingDelegate.forPrimitive(subTypeName);
            return delegate.new ImplicitlyTypedUnboxingAssignment(assigner.assign(typeName, delegate.wrapperType, considerRuntimeType));
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

    private PrimitiveUnboxingDelegate(String wrapperTypeName, int operandStackSize,
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
            return wideningAssignment.apply(methodVisitor).aggregateRightFirst(operandStackSize - 1);
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
            return size.aggregateLeftFirst(operandStackSize - 1);
        }
    }

    public Assignment boxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
        return new BoxingAssignment(assigner.assign(wrapperTypeName, subType, considerRuntimeType));
    }

    public Assignment boxAndAssignTo(String subTypeName, Assigner assigner, boolean considerRuntimeType) {
        return new BoxingAssignment(assigner.assign(wrapperType, subTypeName, considerRuntimeType));
    }

    @Override
    public Assignment unboxAndAssignTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
        return new ExplicitlyTypedUnboxingAssignment(PrimitiveWideningDelegate.forPrimitive(primitiveType).widenTo(subType));
    }

    @Override
    public Assignment unboxAndAssignTo(String subTypeName, Assigner assigner, boolean considerRuntimeType) {
        return new ExplicitlyTypedUnboxingAssignment(PrimitiveWideningDelegate.forPrimitive(primitiveType).widenTo(subTypeName));
    }
}

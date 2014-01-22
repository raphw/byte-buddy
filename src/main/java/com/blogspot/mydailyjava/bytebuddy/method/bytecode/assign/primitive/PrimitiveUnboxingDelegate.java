package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.method.bytecode.TypeSize;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign.Assignment;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveUnboxingDelegate implements Assignment {

    BOOLEAN("java/lang/Boolean", TypeSize.SINGLE, Boolean.class, boolean.class, "booleanValue", "()Z"),
    BYTE("java/lang/Byte", TypeSize.SINGLE, Byte.class, byte.class, "byteValue", "()B"),
    SHORT("java/lang/Short", TypeSize.SINGLE, Short.class, short.class, "shortValue", "()S"),
    CHARACTER("java/lang/Character", TypeSize.SINGLE, Character.class, char.class, "charValue", "()C"),
    INTEGER("java/lang/Integer", TypeSize.SINGLE, Integer.class, int.class, "intValue", "()I"),
    LONG("java/lang/Long", TypeSize.DOUBLE, Long.class, long.class, "longValue", "()J"),
    FLOAT("java/lang/Float", TypeSize.SINGLE, Float.class, float.class, "floatValue", "()F"),
    DOUBLE("java/lang/Double", TypeSize.DOUBLE, Double.class, double.class, "doubleValue", "()D");

    public static interface UnboxingResponsible {

        Assignment assignUnboxedTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType);
    }

    private static enum ExplicitlyTypedUnboxingResponsible implements UnboxingResponsible {

        BOOLEAN(PrimitiveUnboxingDelegate.BOOLEAN),
        BYTE(PrimitiveUnboxingDelegate.BYTE),
        SHORT(PrimitiveUnboxingDelegate.SHORT),
        CHARACTER(PrimitiveUnboxingDelegate.CHARACTER),
        INTEGER(PrimitiveUnboxingDelegate.INTEGER),
        LONG(PrimitiveUnboxingDelegate.LONG),
        FLOAT(PrimitiveUnboxingDelegate.FLOAT),
        DOUBLE(PrimitiveUnboxingDelegate.DOUBLE);

        private final PrimitiveUnboxingDelegate primitiveUnboxingDelegate;

        private ExplicitlyTypedUnboxingResponsible(PrimitiveUnboxingDelegate primitiveUnboxingDelegate) {
            this.primitiveUnboxingDelegate = primitiveUnboxingDelegate;
        }

        @Override
        public Assignment assignUnboxedTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
            return new Compound(primitiveUnboxingDelegate, assigner.assign(primitiveUnboxingDelegate.primitiveType, subType, considerRuntimeType));
        }
    }

    private static PrimitiveUnboxingDelegate forPrimitive(Class<?> type) {
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
            throw new IllegalArgumentException("Expected non-void primitive type instead of " + type);
        }
    }

    private static class ImplicitlyTypedUnboxingResponsible implements UnboxingResponsible {

        private final Class<?> originalType;

        private ImplicitlyTypedUnboxingResponsible(Class<?> originalType) {
            this.originalType = originalType;
        }

        @Override
        public Assignment assignUnboxedTo(Class<?> subType, Assigner assigner, boolean considerRuntimeType) {
            PrimitiveUnboxingDelegate primitiveUnboxingDelegate = PrimitiveUnboxingDelegate.forPrimitive(subType);
            return new Compound(assigner.assign(originalType, primitiveUnboxingDelegate.wrapperType, considerRuntimeType), primitiveUnboxingDelegate);
        }
    }

    public static UnboxingResponsible forReferenceType(Class<?> type) {
        if (type.isPrimitive()) {
            throw new IllegalArgumentException("Expected reference type instead of " + type);
        } else if (type == Boolean.class) {
            return ExplicitlyTypedUnboxingResponsible.BOOLEAN;
        } else if (type == Byte.class) {
            return ExplicitlyTypedUnboxingResponsible.BYTE;
        } else if (type == Short.class) {
            return ExplicitlyTypedUnboxingResponsible.SHORT;
        } else if (type == Character.class) {
            return ExplicitlyTypedUnboxingResponsible.CHARACTER;
        } else if (type == Integer.class) {
            return ExplicitlyTypedUnboxingResponsible.INTEGER;
        } else if (type == Long.class) {
            return ExplicitlyTypedUnboxingResponsible.LONG;
        } else if (type == Float.class) {
            return ExplicitlyTypedUnboxingResponsible.FLOAT;
        } else if (type == Double.class) {
            return ExplicitlyTypedUnboxingResponsible.DOUBLE;
        } else {
            return new ImplicitlyTypedUnboxingResponsible(type);
        }
    }

    private final String wrapperTypeName;
    private final Size size;
    private final Class<?> wrapperType;
    private final Class<?> primitiveType;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;

    private PrimitiveUnboxingDelegate(String wrapperTypeName,
                                      TypeSize sizeIncrease,
                                      Class<?> wrapperType,
                                      Class<?> primitiveType,
                                      String unboxingMethodName,
                                      String unboxingMethodDescriptor) {
        this.wrapperTypeName = wrapperTypeName;
        this.size = sizeIncrease.toIncreasingSize();
        this.wrapperType = wrapperType;
        this.primitiveType = primitiveType;
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
    }

    @Override
    public boolean isAssignable() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEDYNAMIC, wrapperTypeName, unboxingMethodName, unboxingMethodDescriptor);
        return size;
    }
}

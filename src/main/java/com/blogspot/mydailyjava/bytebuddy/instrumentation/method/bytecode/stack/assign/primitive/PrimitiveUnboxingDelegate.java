package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public enum PrimitiveUnboxingDelegate implements StackManipulation {

    BOOLEAN("java/lang/Boolean", StackSize.ZERO, Boolean.class, boolean.class, "booleanValue", "()Z"),
    BYTE("java/lang/Byte", StackSize.ZERO, Byte.class, byte.class, "byteValue", "()B"),
    SHORT("java/lang/Short", StackSize.ZERO, Short.class, short.class, "shortValue", "()S"),
    CHARACTER("java/lang/Character", StackSize.ZERO, Character.class, char.class, "charValue", "()C"),
    INTEGER("java/lang/Integer", StackSize.ZERO, Integer.class, int.class, "intValue", "()I"),
    LONG("java/lang/Long", StackSize.SINGLE, Long.class, long.class, "longValue", "()J"),
    FLOAT("java/lang/Float", StackSize.ZERO, Float.class, float.class, "floatValue", "()F"),
    DOUBLE("java/lang/Double", StackSize.SINGLE, Double.class, double.class, "doubleValue", "()D");

    public static interface UnboxingResponsible {

        StackManipulation assignUnboxedTo(TypeDescription subType, Assigner assigner, boolean considerRuntimeType);
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
        public StackManipulation assignUnboxedTo(TypeDescription subType, Assigner assigner, boolean considerRuntimeType) {
            return new Compound(primitiveUnboxingDelegate, PrimitiveWideningDelegate.forPrimitive(primitiveUnboxingDelegate.primitiveType).widenTo(subType));
        }
    }

    private static PrimitiveUnboxingDelegate forPrimitive(TypeDescription typeDescription) {
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
            throw new IllegalArgumentException("Expected non-void primitive type instead of " + typeDescription);
        }
    }

    private static class ImplicitlyTypedUnboxingResponsible implements UnboxingResponsible {

        private final TypeDescription originalType;

        private ImplicitlyTypedUnboxingResponsible(TypeDescription originalType) {
            this.originalType = originalType;
        }

        @Override
        public StackManipulation assignUnboxedTo(TypeDescription subType, Assigner assigner, boolean considerRuntimeType) {
            PrimitiveUnboxingDelegate primitiveUnboxingDelegate = PrimitiveUnboxingDelegate.forPrimitive(subType);
            return new Compound(assigner.assign(originalType, primitiveUnboxingDelegate.wrapperType, considerRuntimeType), primitiveUnboxingDelegate);
        }
    }

    public static UnboxingResponsible forReferenceType(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            throw new IllegalArgumentException("Expected reference type instead of " + typeDescription);
        } else if (typeDescription.represents(Boolean.class)) {
            return ExplicitlyTypedUnboxingResponsible.BOOLEAN;
        } else if (typeDescription.represents(Byte.class)) {
            return ExplicitlyTypedUnboxingResponsible.BYTE;
        } else if (typeDescription.represents(Short.class)) {
            return ExplicitlyTypedUnboxingResponsible.SHORT;
        } else if (typeDescription.represents(Character.class)) {
            return ExplicitlyTypedUnboxingResponsible.CHARACTER;
        } else if (typeDescription.represents(Integer.class)) {
            return ExplicitlyTypedUnboxingResponsible.INTEGER;
        } else if (typeDescription.represents(Long.class)) {
            return ExplicitlyTypedUnboxingResponsible.LONG;
        } else if (typeDescription.represents(Float.class)) {
            return ExplicitlyTypedUnboxingResponsible.FLOAT;
        } else if (typeDescription.represents(Double.class)) {
            return ExplicitlyTypedUnboxingResponsible.DOUBLE;
        } else {
            return new ImplicitlyTypedUnboxingResponsible(typeDescription);
        }
    }

    private final String wrapperTypeName;
    private final Size size;
    private final TypeDescription wrapperType;
    private final TypeDescription primitiveType;
    private final String unboxingMethodName;
    private final String unboxingMethodDescriptor;

    private PrimitiveUnboxingDelegate(String wrapperTypeName,
                                      StackSize sizeIncrease,
                                      Class<?> wrapperType,
                                      Class<?> primitiveType,
                                      String unboxingMethodName,
                                      String unboxingMethodDescriptor) {
        this.wrapperTypeName = wrapperTypeName;
        this.size = sizeIncrease.toIncreasingSize();
        this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
        this.primitiveType = new TypeDescription.ForLoadedType(primitiveType);
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapperTypeName, unboxingMethodName, unboxingMethodDescriptor);
        return size;
    }
}

package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This delegate is responsible for unboxing a wrapper type to their primitive equivalents.
 */
public enum PrimitiveUnboxingDelegate implements StackManipulation {

    BOOLEAN("java/lang/Boolean", StackSize.ZERO, Boolean.class, boolean.class, "booleanValue", "()Z"),
    BYTE("java/lang/Byte", StackSize.ZERO, Byte.class, byte.class, "byteValue", "()B"),
    SHORT("java/lang/Short", StackSize.ZERO, Short.class, short.class, "shortValue", "()S"),
    CHARACTER("java/lang/Character", StackSize.ZERO, Character.class, char.class, "charValue", "()C"),
    INTEGER("java/lang/Integer", StackSize.ZERO, Integer.class, int.class, "intValue", "()I"),
    LONG("java/lang/Long", StackSize.SINGLE, Long.class, long.class, "longValue", "()J"),
    FLOAT("java/lang/Float", StackSize.ZERO, Float.class, float.class, "floatValue", "()F"),
    DOUBLE("java/lang/Double", StackSize.SINGLE, Double.class, double.class, "doubleValue", "()D");

    /**
     * Implementations represent an unboxing delegate that is able to perform the unboxing operation.
     */
    public static interface UnboxingResponsible {

        /**
         * Attempts to unbox the represented type in order to assign the unboxed value to the given target type
         * while using the assigner that is provided by the method call.
         *
         * @param targetType          The type that is the desired outcome of the assignment.
         * @param assigner            The assigner used to assign the unboxed type to the target type.
         * @param considerRuntimeType If {@code true}, unsafe castings are allowed for this assignment.
         * @return A stack manipulation representing this assignment if such an assignment is possible. An illegal
         * assignment otherwise.
         */
        StackManipulation assignUnboxedTo(TypeDescription targetType, Assigner assigner, boolean considerRuntimeType);
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
        public StackManipulation assignUnboxedTo(TypeDescription targetType, Assigner assigner, boolean considerRuntimeType) {
            return new Compound(primitiveUnboxingDelegate, PrimitiveWideningDelegate.forPrimitive(primitiveUnboxingDelegate.primitiveType).widenTo(targetType));
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
        public StackManipulation assignUnboxedTo(TypeDescription targetType, Assigner assigner, boolean considerRuntimeType) {
            PrimitiveUnboxingDelegate primitiveUnboxingDelegate = PrimitiveUnboxingDelegate.forPrimitive(targetType);
            return new Compound(assigner.assign(originalType, primitiveUnboxingDelegate.wrapperType, considerRuntimeType), primitiveUnboxingDelegate);
        }
    }

    /**
     * Creates an unboxing responsible that is capable of unboxing a wrapper type.
     * <ol>
     * <li>If the reference type represents a wrapper type, the wrapper type will simply be unboxed.</li>
     * <li>If the reference type does not represent a wrapper type, the wrapper type will be inferred by the primitive target
     * type that is later given to the
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.assign.primitive.PrimitiveUnboxingDelegate.UnboxingResponsible}
     * in order to then check if the given type is assignable to the inferred wrapper type.</li>
     * </ol>
     *
     * @param typeDescription A non-primitive type.
     * @return An unboxing responsible capable of performing an unboxing operation while considering a further assignment
     * of the unboxed value.
     */
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

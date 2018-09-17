package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This delegate is responsible for unboxing a wrapper type to their primitive equivalents.
 */
public enum PrimitiveUnboxingDelegate implements StackManipulation {

    /**
     * The unboxing delegate for {@code Boolean} types.
     */
    BOOLEAN(Boolean.class, boolean.class, StackSize.ZERO, "booleanValue", "()Z"),

    /**
     * The unboxing delegate for {@code Byte} types.
     */
    BYTE(Byte.class, byte.class, StackSize.ZERO, "byteValue", "()B"),

    /**
     * The unboxing delegate for {@code Short} types.
     */
    SHORT(Short.class, short.class, StackSize.ZERO, "shortValue", "()S"),

    /**
     * The unboxing delegate for {@code Character} types.
     */
    CHARACTER(Character.class, char.class, StackSize.ZERO, "charValue", "()C"),

    /**
     * The unboxing delegate for {@code Integer} types.
     */
    INTEGER(Integer.class, int.class, StackSize.ZERO, "intValue", "()I"),

    /**
     * The unboxing delegate for {@code Long} types.
     */
    LONG(Long.class, long.class, StackSize.SINGLE, "longValue", "()J"),

    /**
     * The unboxing delegate for {@code Float} types.
     */
    FLOAT(Float.class, float.class, StackSize.ZERO, "floatValue", "()F"),

    /**
     * The unboxing delegate for {@code Double} types.
     */
    DOUBLE(Double.class, double.class, StackSize.SINGLE, "doubleValue", "()D");

    /**
     * The wrapper type of the represented primitive type.
     */
    private final TypeDescription wrapperType;

    /**
     * The represented primitive type.
     */
    private final TypeDescription primitiveType;

    /**
     * The size increase after a wrapper type was unwrapped.
     */
    private final Size size;

    /**
     * The name of the method for unboxing a wrapper value to its primitive value.
     */
    private final String unboxingMethodName;

    /**
     * The descriptor of the method for unboxing a wrapper value to its primitive value.
     */
    private final String unboxingMethodDescriptor;

    /**
     * Creates a new primitive unboxing delegate.
     *
     * @param wrapperType              The wrapper type of the represented primitive type.
     * @param primitiveType            The represented primitive type.
     * @param sizeDifference           The size difference between the wrapper type and its primitive value.
     * @param unboxingMethodName       The name of the method for unboxing a wrapper value to its primitive value.
     * @param unboxingMethodDescriptor The descriptor of the method for unboxing a wrapper value to its primitive value.
     */
    PrimitiveUnboxingDelegate(Class<?> wrapperType,
                              Class<?> primitiveType,
                              StackSize sizeDifference,
                              String unboxingMethodName,
                              String unboxingMethodDescriptor) {
        this.size = sizeDifference.toIncreasingSize();
        this.wrapperType = TypeDescription.ForLoadedType.of(wrapperType);
        this.primitiveType = TypeDescription.ForLoadedType.of(primitiveType);
        this.unboxingMethodName = unboxingMethodName;
        this.unboxingMethodDescriptor = unboxingMethodDescriptor;
    }

    /**
     * Locates a primitive unboxing delegate for a given primitive type.
     *
     * @param typeDescription A description of the primitive type.
     * @return A corresponding primitive unboxing delegate.
     */
    protected static PrimitiveUnboxingDelegate forPrimitive(TypeDescription.Generic typeDescription) {
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

    /**
     * Creates an unboxing responsible that is capable of unboxing a wrapper type.
     * <ol>
     * <li>If the reference type represents a wrapper type, the wrapper type will simply be unboxed.</li>
     * <li>If the reference type does not represent a wrapper type, the wrapper type will be inferred by the primitive target
     * type that is later given to the
     * {@link net.bytebuddy.implementation.bytecode.assign.primitive.PrimitiveUnboxingDelegate.UnboxingResponsible}
     * in order to then check if the given type is assignable to the inferred wrapper type.</li>
     * </ol>
     *
     * @param typeDefinition A non-primitive type.
     * @return An unboxing responsible capable of performing an unboxing operation while considering a further assignment
     * of the unboxed value.
     */
    public static UnboxingResponsible forReferenceType(TypeDefinition typeDefinition) {
        if (typeDefinition.isPrimitive()) {
            throw new IllegalArgumentException("Expected reference type instead of " + typeDefinition);
        } else if (typeDefinition.represents(Boolean.class)) {
            return ExplicitlyTypedUnboxingResponsible.BOOLEAN;
        } else if (typeDefinition.represents(Byte.class)) {
            return ExplicitlyTypedUnboxingResponsible.BYTE;
        } else if (typeDefinition.represents(Short.class)) {
            return ExplicitlyTypedUnboxingResponsible.SHORT;
        } else if (typeDefinition.represents(Character.class)) {
            return ExplicitlyTypedUnboxingResponsible.CHARACTER;
        } else if (typeDefinition.represents(Integer.class)) {
            return ExplicitlyTypedUnboxingResponsible.INTEGER;
        } else if (typeDefinition.represents(Long.class)) {
            return ExplicitlyTypedUnboxingResponsible.LONG;
        } else if (typeDefinition.represents(Float.class)) {
            return ExplicitlyTypedUnboxingResponsible.FLOAT;
        } else if (typeDefinition.represents(Double.class)) {
            return ExplicitlyTypedUnboxingResponsible.DOUBLE;
        } else {
            return new ImplicitlyTypedUnboxingResponsible(typeDefinition.asGenericType());
        }
    }

    /**
     * Returns the wrapper type that this unboxing delegate represents.
     *
     * @return A generic version of this delegate's wrapper type.
     */
    protected TypeDescription.Generic getWrapperType() {
        return wrapperType.asGenericType();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                wrapperType.asErasure().getInternalName(),
                unboxingMethodName,
                unboxingMethodDescriptor,
                false);
        return size;
    }

    /**
     * An explicitly types unboxing responsible is applied for directly unboxing a wrapper type.
     */
    protected enum ExplicitlyTypedUnboxingResponsible implements UnboxingResponsible {

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Boolean} type.
         */
        BOOLEAN(PrimitiveUnboxingDelegate.BOOLEAN),

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Byte} type.
         */
        BYTE(PrimitiveUnboxingDelegate.BYTE),

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Short} type.
         */
        SHORT(PrimitiveUnboxingDelegate.SHORT),

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Character} type.
         */
        CHARACTER(PrimitiveUnboxingDelegate.CHARACTER),

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Integer} type.
         */
        INTEGER(PrimitiveUnboxingDelegate.INTEGER),

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Long} type.
         */
        LONG(PrimitiveUnboxingDelegate.LONG),

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Float} type.
         */
        FLOAT(PrimitiveUnboxingDelegate.FLOAT),

        /**
         * An unboxing responsible for unboxing a {@link java.lang.Double} type.
         */
        DOUBLE(PrimitiveUnboxingDelegate.DOUBLE);

        /**
         * The primitive unboxing delegate for handling the given wrapper type.
         */
        private final PrimitiveUnboxingDelegate primitiveUnboxingDelegate;

        /**
         * Creates a new explicitly typed unboxing responsible.
         *
         * @param primitiveUnboxingDelegate The primitive unboxing delegate for handling the given wrapper type.
         */
        ExplicitlyTypedUnboxingResponsible(PrimitiveUnboxingDelegate primitiveUnboxingDelegate) {
            this.primitiveUnboxingDelegate = primitiveUnboxingDelegate;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation assignUnboxedTo(TypeDescription.Generic targetType, Assigner assigner, Assigner.Typing typing) {
            return new Compound(
                    primitiveUnboxingDelegate,
                    PrimitiveWideningDelegate.forPrimitive(primitiveUnboxingDelegate.primitiveType).widenTo(targetType));
        }
    }

    /**
     * Implementations represent an unboxing delegate that is able to perform the unboxing operation.
     */
    public interface UnboxingResponsible {

        /**
         * Attempts to unbox the represented type in order to assign the unboxed value to the given target type
         * while using the assigner that is provided by the method call.
         *
         * @param target   The type that is the desired outcome of the assignment.
         * @param assigner The assigner used to assign the unboxed type to the target type.
         * @param typing   Determines if a type-casting should be attempted for incompatible types.
         * @return A stack manipulation representing this assignment if such an assignment is possible. An illegal
         * assignment otherwise.
         */
        StackManipulation assignUnboxedTo(TypeDescription.Generic target, Assigner assigner, Assigner.Typing typing);
    }

    /**
     * An unboxing responsible for an implicitly typed value. This implementation is applied for source types that
     * were not found to be of a given wrapper type. Instead, this unboxing responsible tries to assign the
     * source type to the primitive target type's wrapper type before performing an unboxing operation.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ImplicitlyTypedUnboxingResponsible implements UnboxingResponsible {

        /**
         * The original type which should be unboxed but is not of any known wrapper type.
         */
        private final TypeDescription.Generic originalType;

        /**
         * Creates a new implicitly typed unboxing responsible.
         *
         * @param originalType The original type which should be unboxed but is not of any known wrapper type.
         */
        protected ImplicitlyTypedUnboxingResponsible(TypeDescription.Generic originalType) {
            this.originalType = originalType;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation assignUnboxedTo(TypeDescription.Generic target, Assigner assigner, Assigner.Typing typing) {
            PrimitiveUnboxingDelegate primitiveUnboxingDelegate = PrimitiveUnboxingDelegate.forPrimitive(target);
            return new Compound(
                    assigner.assign(originalType, primitiveUnboxingDelegate.getWrapperType(), typing),
                    primitiveUnboxingDelegate);
        }
    }
}

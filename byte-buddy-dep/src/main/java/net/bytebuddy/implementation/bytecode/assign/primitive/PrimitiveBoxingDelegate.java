package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This delegate is responsible for boxing a primitive types to their wrapper equivalents.
 */
public enum PrimitiveBoxingDelegate {

    /**
     * The boxing delegate for {@code boolean} values.
     */
    BOOLEAN(Boolean.class, StackSize.ZERO, "valueOf", "(Z)Ljava/lang/Boolean;"),

    /**
     * The boxing delegate for {@code byte} values.
     */
    BYTE(Byte.class, StackSize.ZERO, "valueOf", "(B)Ljava/lang/Byte;"),

    /**
     * The boxing delegate for {@code short} values.
     */
    SHORT(Short.class, StackSize.ZERO, "valueOf", "(S)Ljava/lang/Short;"),

    /**
     * The boxing delegate for {@code char} values.
     */
    CHARACTER(Character.class, StackSize.ZERO, "valueOf", "(C)Ljava/lang/Character;"),

    /**
     * The boxing delegate for {@code int} values.
     */
    INTEGER(Integer.class, StackSize.ZERO, "valueOf", "(I)Ljava/lang/Integer;"),

    /**
     * The boxing delegate for {@code long} values.
     */
    LONG(Long.class, StackSize.SINGLE, "valueOf", "(J)Ljava/lang/Long;"),

    /**
     * The boxing delegate for {@code float} values.
     */
    FLOAT(Float.class, StackSize.ZERO, "valueOf", "(F)Ljava/lang/Float;"),

    /**
     * The boxing delegate for {@code double} values.
     */
    DOUBLE(Double.class, StackSize.SINGLE, "valueOf", "(D)Ljava/lang/Double;");

    /**
     * A description of a wrapper type.
     */
    private final TypeDescription wrapperType;

    /**
     * The size decrease after a primitive type was wrapped.
     */
    private final StackManipulation.Size size;

    /**
     * The name of the method for boxing a primitive value as its wrapper type.
     */
    private final String boxingMethodName;

    /**
     * The descriptor of the method for boxing a primitive value as its wrapper type.
     */
    private final String boxingMethodDescriptor;

    /**
     * Creates a new primitive boxing delegate.
     *
     * @param wrapperType            A description of a wrapper type.
     * @param sizeDifference         The size difference between a primitive type and its wrapper type.
     * @param boxingMethodName       The name of the method for boxing a primitive value as its wrapper type.
     * @param boxingMethodDescriptor The descriptor of the method for boxing a primitive value as its wrapper type.
     */
    PrimitiveBoxingDelegate(Class<?> wrapperType,
                            StackSize sizeDifference,
                            String boxingMethodName,
                            String boxingMethodDescriptor) {
        this.wrapperType = new TypeDescription.ForLoadedType(wrapperType);
        this.size = sizeDifference.toDecreasingSize();
        this.boxingMethodName = boxingMethodName;
        this.boxingMethodDescriptor = boxingMethodDescriptor;
    }

    /**
     * Locates a boxing delegate for a given primitive type.
     *
     * @param typeDescription A non-void primitive type.
     * @return A delegate capable of boxing the given primitve type.
     */
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

    /**
     * Creates a stack manipulation that boxes the represented primitive type and applies a chained assignment
     * to the result of this boxing operation.
     *
     * @param targetType      The type that is target of the assignment operation.
     * @param chainedAssigner The assigner that is to be used to perform the chained assignment.
     * @param typing          Determines if an assignment to an incompatible type should be enforced by a casting.
     * @return A stack manipulation that represents the described assignment operation.
     */
    public StackManipulation assignBoxedTo(TypeDescription targetType, Assigner chainedAssigner, Assigner.Typing typing) {
        return new BoxingStackManipulation(chainedAssigner.assign(wrapperType, targetType, typing));
    }

    @Override
    public String toString() {
        return "PrimitiveBoxingDelegate." + name();
    }

    /**
     * A stack manipulation for boxing a primitive type into its wrapper type.
     */
    private class BoxingStackManipulation implements StackManipulation {

        /**
         * A stack manipulation that is applied after the boxing of the top-most value on the operand stack.
         */
        private final StackManipulation stackManipulation;

        /**
         * Creates a new boxing stack manipulation.
         *
         * @param stackManipulation A stack manipulation that is applied after the boxing of the top-most value on
         *                          the operand stack.
         */
        public BoxingStackManipulation(StackManipulation stackManipulation) {
            this.stackManipulation = stackManipulation;
        }

        @Override
        public boolean isValid() {
            return stackManipulation.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    wrapperType.getInternalName(),
                    boxingMethodName,
                    boxingMethodDescriptor,
                    false);
            return size.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
        }
    }
}

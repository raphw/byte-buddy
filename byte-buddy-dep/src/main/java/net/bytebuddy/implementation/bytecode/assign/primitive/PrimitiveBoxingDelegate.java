package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.description.type.TypeDefinition;
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
        this.wrapperType = TypeDescription.ForLoadedType.of(wrapperType);
        this.size = sizeDifference.toDecreasingSize();
        this.boxingMethodName = boxingMethodName;
        this.boxingMethodDescriptor = boxingMethodDescriptor;
    }

    /**
     * Locates a boxing delegate for a given primitive type.
     *
     * @param typeDefinition A non-void primitive type.
     * @return A delegate capable of boxing the given primitive type.
     */
    public static PrimitiveBoxingDelegate forPrimitive(TypeDefinition typeDefinition) {
        if (typeDefinition.represents(boolean.class)) {
            return BOOLEAN;
        } else if (typeDefinition.represents(byte.class)) {
            return BYTE;
        } else if (typeDefinition.represents(short.class)) {
            return SHORT;
        } else if (typeDefinition.represents(char.class)) {
            return CHARACTER;
        } else if (typeDefinition.represents(int.class)) {
            return INTEGER;
        } else if (typeDefinition.represents(long.class)) {
            return LONG;
        } else if (typeDefinition.represents(float.class)) {
            return FLOAT;
        } else if (typeDefinition.represents(double.class)) {
            return DOUBLE;
        } else {
            throw new IllegalArgumentException("Not a non-void, primitive type: " + typeDefinition);
        }
    }

    /**
     * Creates a stack manipulation that boxes the represented primitive type and applies a chained assignment
     * to the result of this boxing operation.
     *
     * @param target          The type that is target of the assignment operation.
     * @param chainedAssigner The assigner that is to be used to perform the chained assignment.
     * @param typing          Determines if an assignment to an incompatible type should be enforced by a casting.
     * @return A stack manipulation that represents the described assignment operation.
     */
    public StackManipulation assignBoxedTo(TypeDescription.Generic target, Assigner chainedAssigner, Assigner.Typing typing) {
        return new BoxingStackManipulation(chainedAssigner.assign(wrapperType.asGenericType(), target, typing));
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

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return stackManipulation.isValid();
        }

        /**
         * {@inheritDoc}
         */
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

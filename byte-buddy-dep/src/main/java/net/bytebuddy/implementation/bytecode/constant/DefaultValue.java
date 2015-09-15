package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.objectweb.asm.MethodVisitor;

/**
 * Represents a stack assignment that loads the default value of a given type onto the stack.
 */
public enum DefaultValue implements StackManipulation {

    /**
     * The default value of a JVM integer which covers Java's {@code int}, {@code boolean}, {@code byte},
     * {@code short} and {@code char} values.
     */
    INTEGER(IntegerConstant.ZERO),

    /**
     * The default value of a {@code long}.
     */
    LONG(LongConstant.ZERO),

    /**
     * The default value of a {@code float}.
     */
    FLOAT(FloatConstant.ZERO),

    /**
     * The default value of a {@code double}.
     */
    DOUBLE(DoubleConstant.ZERO),

    /**
     * The default value of a {@code void} which resembles a no-op manipulation.
     */
    VOID(Trivial.INSTANCE),

    /**
     * The default value of a reference type which resembles the {@code null} reference.
     */
    ANY_REFERENCE(NullConstant.INSTANCE);

    /**
     * The stack manipulation that represents the loading of a given default value onto the operand stack.
     */
    private final StackManipulation stackManipulation;

    /**
     * Creates a new default value load operation.
     *
     * @param stackManipulation The stack manipulation that represents the loading of a given default value onto the
     *                          operand stack.
     */
    DefaultValue(StackManipulation stackManipulation) {
        this.stackManipulation = stackManipulation;
    }

    /**
     * Creates a stack assignment that loads the default value for a given type.
     *
     * @param typeDescription The type for which a default value should be loaded onto the operand stack.
     * @return A stack manipulation loading the default value for the given type.
     */
    public static StackManipulation of(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            if (typeDescription.represents(long.class)) {
                return LONG;
            } else if (typeDescription.represents(double.class)) {
                return DOUBLE;
            } else if (typeDescription.represents(float.class)) {
                return FLOAT;
            } else if (typeDescription.represents(void.class)) {
                return VOID;
            } else {
                return INTEGER;
            }
        } else {
            return ANY_REFERENCE;
        }
    }

    @Override
    public boolean isValid() {
        return stackManipulation.isValid();
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        return stackManipulation.apply(methodVisitor, implementationContext);
    }

    @Override
    public String toString() {
        return "DefaultValue." + name();
    }
}

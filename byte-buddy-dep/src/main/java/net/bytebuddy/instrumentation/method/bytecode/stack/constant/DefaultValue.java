package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
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
    VOID(StackManipulation.LegalTrivial.INSTANCE),

    /**
     * The default value of a reference type which resembles the {@code null} reference.
     */
    ANY_REFERENCE(NullConstant.INSTANCE);

    private final StackManipulation stackManipulation;

    private DefaultValue(StackManipulation stackManipulation) {
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
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        return stackManipulation.apply(methodVisitor, instrumentationContext);
    }
}

package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.constant;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.LegalTrivialStackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

/**
 * Represents a stack assignment that loads the default value of a given type onto the stack.
 */
public enum DefaultValue implements StackManipulation {

    INTEGER(IntegerConstant.ZERO),
    LONG(LongConstant.ZERO),
    FLOAT(FloatConstant.ZERO),
    DOUBLE(DoubleConstant.ZERO),
    VOID(LegalTrivialStackManipulation.INSTANCE),
    ANY_REFERENCE(NullConstant.INSTANCE);

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

    private final StackManipulation stackManipulation;

    private DefaultValue(StackManipulation stackManipulation) {
        this.stackManipulation = stackManipulation;
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

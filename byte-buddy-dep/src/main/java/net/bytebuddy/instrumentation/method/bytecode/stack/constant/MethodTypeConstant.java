package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * A constant for a Java 7 {@link java.lang.invoke.MethodType}.
 */
public class MethodTypeConstant implements StackManipulation {

    /**
     * The type description for a method type. This way, we avoid a compile time dependency to this
     * type which allows us to run Byte Buddy in Java 6.
     */
    private static final String METHOD_TYPE_TYPE_INTERNAL_NAME = "java/lang/invoke/MethodType";
    /**
     * The size of a {@link java.lang.invoke.MethodType} on the operand stack.
     */
    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();
    /**
     * The represented method type.
     */
    private final Type methodType;

    /**
     * Creates a new method type constant.
     *
     * @param methodDescription The method description of the represented type.
     */
    public MethodTypeConstant(MethodDescription methodDescription) {
        methodType = Type.getMethodType(methodDescription.getDescriptor());
    }

    /**
     * Checks if a type represents the {@link java.lang.invoke.MethodType} type.
     *
     * @param typeDescription The type to be checked.
     * @return {@code true} if the given type represents a {@link java.lang.invoke.MethodType}.
     */
    public static boolean isRepresentedBy(TypeDescription typeDescription) {
        return typeDescription.getInternalName().equals(METHOD_TYPE_TYPE_INTERNAL_NAME);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitLdcInsn(methodType);
        return SIZE;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && methodType.equals(((MethodTypeConstant) other).methodType);
    }

    @Override
    public int hashCode() {
        return methodType.hashCode();
    }

    @Override
    public String toString() {
        return "MethodTypeConstant{methodType=" + methodType + '}';
    }
}

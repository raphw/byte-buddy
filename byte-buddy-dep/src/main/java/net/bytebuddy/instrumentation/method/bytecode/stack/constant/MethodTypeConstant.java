package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.JavaInstance;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * A constant for a Java 7 {@code java.lang.invoke.MethodType}.
 */
public class MethodTypeConstant implements StackManipulation {

    /**
     * The size of a {@code java.lang.invoke.MethodType} on the operand stack.
     */
    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    /**
     * The represented method type.
     */
    private final Type methodType;

    public static StackManipulation of(MethodDescription methodDescription) {
        return new MethodTypeConstant(Type.getMethodType(methodDescription.getDescriptor()));
    }

    public static StackManipulation of(JavaInstance.MethodType methodType) {
        Type[] parameterType = new Type[methodType.getParameterTypes().size()];
        int index = 0;
        for (TypeDescription typeDescription : methodType.getParameterTypes()) {
            parameterType[index++] = Type.getType(typeDescription.getDescriptor());
        }
        return new MethodTypeConstant(Type.getMethodType(Type.getType(methodType.getReturnType().getDescriptor()), parameterType));
    }

    public MethodTypeConstant(Type methodType) {
        this.methodType = methodType;
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

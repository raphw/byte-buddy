package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
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

    /**
     * Creates a method type constant for the given method type.
     *
     * @param methodType The represented method type.
     */
    protected MethodTypeConstant(Type methodType) {
        this.methodType = methodType;
    }

    /**
     * Transforms the given method into a stack manipulation that loads its type onto the operand stack.
     *
     * @param methodDescription The method of which the method type should be loaded onto the operand stack.
     * @return A stack manipulation that loads the method type of the given method onto the operand stack.
     */
    public static StackManipulation of(MethodDescription.InDefinedShape methodDescription) {
        return new MethodTypeConstant(Type.getMethodType(methodDescription.getDescriptor()));
    }

    /**
     * Transforms the given method type into a stack manipulation that loads its type onto the operand stack.
     *
     * @param methodType The method type that should be loaded onto the operand stack.
     * @return A stack manipulation that loads the given method type.
     */
    public static StackManipulation of(JavaInstance.MethodType methodType) {
        Type[] parameterType = new Type[methodType.getParameterTypes().size()];
        int index = 0;
        for (TypeDescription typeDescription : methodType.getParameterTypes()) {
            parameterType[index++] = Type.getType(typeDescription.getDescriptor());
        }
        return new MethodTypeConstant(Type.getMethodType(Type.getType(methodType.getReturnType().getDescriptor()), parameterType));
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
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

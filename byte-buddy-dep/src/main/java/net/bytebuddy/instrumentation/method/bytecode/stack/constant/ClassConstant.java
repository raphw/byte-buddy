package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Represents a constant representing a loaded Java {@link java.lang.Class}.
 */
public class ClassConstant implements StackManipulation {

    private final TypeDescription typeDescription;

    /**
     * Creates a stack manipulation that represents loading a class constant onto the stack.
     *
     * @param typeDescription A description of the class to load onto the stack.
     */
    public ClassConstant(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        methodVisitor.visitLdcInsn(Type.getType(typeDescription.getDescriptor()));
        return StackSize.SINGLE.toIncreasingSize();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && typeDescription.equals(((ClassConstant) other).typeDescription);
    }

    @Override
    public int hashCode() {
        return typeDescription.hashCode();
    }

    @Override
    public String toString() {
        return "ClassConstant{typeDescription=" + typeDescription + '}';
    }
}

package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.JavaConstant;
import org.objectweb.asm.MethodVisitor;

/**
 * A constant representing a {@link JavaConstant}.
 */
public class JavaConstantValue implements StackManipulation {

    /**
     * The instance to load onto the operand stack.
     */
    private final JavaConstant javaConstant;

    /**
     * Creates a constant pool value representing a {@link JavaConstant}.
     *
     * @param javaConstant The instance to load onto the operand stack.
     */
    public JavaConstantValue(JavaConstant javaConstant) {
        this.javaConstant = javaConstant;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitLdcInsn(javaConstant.asConstantPoolValue());
        return StackSize.SINGLE.toIncreasingSize();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && javaConstant.equals(((JavaConstantValue) other).javaConstant);
    }

    @Override
    public int hashCode() {
        return javaConstant.hashCode();
    }

    @Override
    public String toString() {
        return "JavaConstantValue{javaConstant=" + javaConstant + '}';
    }
}

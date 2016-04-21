package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.JavaInstance;
import org.objectweb.asm.MethodVisitor;

/**
 * A constant representing a {@link JavaInstance}.
 */
public class JavaInstanceConstant implements StackManipulation {

    /**
     * The instance to load onto the operand stack.
     */
    private final JavaInstance javaInstance;

    /**
     * Creates a constant pool value representing a {@link JavaInstance}.
     *
     * @param javaInstance The instance to load onto the operand stack.
     */
    public JavaInstanceConstant(JavaInstance javaInstance) {
        this.javaInstance = javaInstance;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitLdcInsn(javaInstance.asConstantPoolValue());
        return StackSize.SINGLE.toIncreasingSize();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && javaInstance.equals(((JavaInstanceConstant) other).javaInstance);
    }

    @Override
    public int hashCode() {
        return javaInstance.hashCode();
    }

    @Override
    public String toString() {
        return "JavaInstanceConstant{javaInstance=" + javaInstance + '}';
    }
}

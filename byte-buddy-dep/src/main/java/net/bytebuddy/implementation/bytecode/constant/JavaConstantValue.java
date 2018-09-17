package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.JavaConstant;
import org.objectweb.asm.MethodVisitor;

/**
 * A constant representing a {@link JavaConstant}.
 */
@HashCodeAndEqualsPlugin.Enhance
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

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitLdcInsn(javaConstant.asConstantPoolValue());
        return StackSize.SINGLE.toIncreasingSize();
    }
}

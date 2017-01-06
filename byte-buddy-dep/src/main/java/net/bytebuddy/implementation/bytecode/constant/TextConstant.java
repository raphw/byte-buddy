package net.bytebuddy.implementation.bytecode.constant;

import lombok.EqualsAndHashCode;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;

/**
 * Represents a {@link java.lang.String} value that is stored in a type's constant pool.
 */
@EqualsAndHashCode
public class TextConstant implements StackManipulation {

    /**
     * The text value to load onto the operand stack.
     */
    private final String text;

    /**
     * Creates a new stack manipulation to load a {@code String} constant onto the operand stack.
     *
     * @param text The value of the {@code String} to be loaded.
     */
    public TextConstant(String text) {
        this.text = text;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitLdcInsn(text);
        return StackSize.SINGLE.toIncreasingSize();
    }
}

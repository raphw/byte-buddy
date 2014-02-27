package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import org.objectweb.asm.MethodVisitor;

/**
 * A trivial stack manipulation that does not require any manipulation of the operand stack.
 */
public enum LegalTrivialStackManipulation implements StackManipulation {
    INSTANCE(new Size(0, 0));

    private final Size size;

    private LegalTrivialStackManipulation(Size size) {
        this.size = size;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        return size;
    }
}

package com.blogspot.mydailyjava.bytebuddy.extraction.method.appender;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import org.objectweb.asm.MethodVisitor;

public interface ByteCodeAppender {

    static final class Size {

        private final int operandStackSize;
        private final int localVariableSize;

        public Size(int operandStackSize, int localVariableSize) {
            this.operandStackSize = operandStackSize;
            this.localVariableSize = localVariableSize;
        }

        public int getOperandStackSize() {
            return operandStackSize;
        }

        public int getLocalVariableSize() {
            return localVariableSize;
        }

        public Size merge(Size other) {
            return new Size(Math.max(operandStackSize, other.operandStackSize), localVariableSize + other.localVariableSize);
        }
    }

    Size apply(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext);
}

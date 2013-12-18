package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import org.objectweb.asm.MethodVisitor;

public interface ByteCodeBlockAppender {

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
            return new Size(Math.max(operandStackSize, other.operandStackSize), Math.max(localVariableSize, other.localVariableSize));
        }

        public Size merge(int operandStackSize, int localVariableSize) {
            return new Size(Math.max(this.operandStackSize, operandStackSize), Math.max(this.localVariableSize, localVariableSize));
        }
    }

    Size apply(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext);
}

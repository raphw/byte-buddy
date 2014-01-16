package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

public interface ByteCodeAppender {

    static interface Factory {

        ByteCodeAppender make(TypeDescription typeDescription);
    }

    static class Size {

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

    Size apply(MethodVisitor methodVisitor, MethodDescription methodDescription);
}

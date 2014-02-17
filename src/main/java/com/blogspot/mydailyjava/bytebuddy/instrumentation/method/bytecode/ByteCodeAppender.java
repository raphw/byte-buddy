package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
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

        @Override
        public String toString() {
            return "ByteCodeAppender.Size{operandStackSize=" + operandStackSize + ", localVariableSize=" + localVariableSize + '}';
        }
    }

    Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription instrumentedMethod);
}

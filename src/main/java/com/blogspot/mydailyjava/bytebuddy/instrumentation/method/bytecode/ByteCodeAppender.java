package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import org.objectweb.asm.MethodVisitor;

public interface ByteCodeAppender {

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

    static class Compound implements ByteCodeAppender {

        private final ByteCodeAppender[] byteCodeAppender;

        public Compound(ByteCodeAppender... byteCodeAppender) {
            this.byteCodeAppender = byteCodeAppender;
        }

        @Override
        public boolean appendsCode() {
            for (ByteCodeAppender byteCodeAppender : this.byteCodeAppender) {
                if (byteCodeAppender.appendsCode()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription instrumentedMethod) {
            Size size = new Size(0, instrumentedMethod.getStackSize());
            for (ByteCodeAppender byteCodeAppender : this.byteCodeAppender) {
                size = size.merge(byteCodeAppender.apply(methodVisitor, instrumentationContext, instrumentedMethod));
            }
            return size;
        }
    }

    boolean appendsCode();

    Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext, MethodDescription instrumentedMethod);
}

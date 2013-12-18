package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.context.MethodContext;
import org.objectweb.asm.MethodVisitor;

public interface OperandStackValueLoader {

    static final class Size {

        private final int finalSize;
        private final int maximalSize;

        public Size(int finalSize, int maximalSize) {
            this.finalSize = finalSize;
            this.maximalSize = maximalSize;
        }

        public int getFinalSize() {
            return finalSize;
        }

        public int getMaximalSize() {
            return maximalSize;
        }

        public Size merge(Size other) {
            return merge(other.finalSize, other.maximalSize);
        }

        public Size merge(int finalSize, int maximalSize) {
            return new Size(this.finalSize + finalSize, Math.max(this.maximalSize + finalSize, maximalSize));
        }
        public Size consume(Size other) {
            return consume(other.finalSize, other.maximalSize);
        }

        public Size consume(int finalSize, int maximalSize) {
            return new Size(this.finalSize, Math.max(this.maximalSize, maximalSize));
        }
    }

    Size load(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext);
}

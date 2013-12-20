package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.objectweb.asm.MethodVisitor;

public interface Assignment {

    static class Size {

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

        public Size withMaximum(int maximalSize) {
            return new Size(this.finalSize, Math.max(this.maximalSize, maximalSize));
        }
    }

    boolean isAssignable();

    Size apply(MethodVisitor methodVisitor);
}

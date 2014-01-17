package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.objectweb.asm.MethodVisitor;

public interface Assignment {

    static class Size {

        private final int size;
        private final int maximalSize;

        public Size(int size, int maximalSize) {
            this.size = size;
            this.maximalSize = maximalSize;
        }

        public int getSize() {
            return size;
        }

        public int getMaximalSize() {
            return maximalSize;
        }

        public Size aggregate(Size other) {
            return aggregate(other.size, other.maximalSize);
        }

        public Size aggregate(int sizeChange, int interimMaximalSize) {
            return new Size(size + sizeChange, Math.max(maximalSize, size + interimMaximalSize));
        }
    }

    boolean isAssignable();

    Size apply(MethodVisitor methodVisitor);
}

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

        public Size aggregateRightFirst(Size currentSize) {
            return aggregateRightFirst(currentSize.getSize(), currentSize.getSize());
        }

        public Size aggregateRightFirst(int currentSize) {
            return aggregateRightFirst(currentSize, currentSize);
        }

        public Size aggregateRightFirst(int currentSize, int currentMaximalSize) {
            return aggregate(currentSize, currentMaximalSize, size, maximalSize);
        }

        public Size aggregateLeftFirst(Size sizeChange) {
            return aggregateLeftFirst(sizeChange.getSize(), sizeChange.getMaximalSize());
        }

        public Size aggregateLeftFirst(int sizeChange) {
            return aggregateLeftFirst(sizeChange, sizeChange);
        }

        public Size aggregateLeftFirst(int sizeChange, int interimMaximalSize) {
            return aggregate(size, maximalSize, sizeChange, interimMaximalSize);
        }

        private static Size aggregate(int baseSize, int baseMaximalSize, int aggregateSize, int aggregateMaximalSize) {
            return new Size(baseSize + aggregateSize, Math.max(baseSize + aggregateMaximalSize, baseMaximalSize));
        }
    }

    boolean isAssignable();

    Size apply(MethodVisitor methodVisitor);
}

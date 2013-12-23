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

        public Size aggregateRightFirst(Size currentSize) {
            return aggregateRightFirst(currentSize.getFinalSize(), currentSize.getFinalSize());
        }

        public Size aggregateRightFirst(int currentSize) {
            return aggregateRightFirst(currentSize, currentSize);
        }

        public Size aggregateRightFirst(int currentFinalSize, int currentMaximalSize) {
            return aggregate(currentFinalSize, currentMaximalSize, finalSize, maximalSize);
        }

        public Size aggregateLeftFirst(Size sizeChange) {
            return aggregateLeftFirst(sizeChange.getFinalSize(), sizeChange.getMaximalSize());
        }

        public Size aggregateLeftFirst(int sizeChange) {
            return aggregateLeftFirst(sizeChange, sizeChange);
        }

        public Size aggregateLeftFirst(int finalSizeChange, int interimMaximalSize) {
            return aggregate(finalSize, maximalSize, finalSizeChange, interimMaximalSize);
        }

        private static Size aggregate(int baseFinalSize, int baseMaximalSize, int aggregateFinalSize, int aggregateMaximalSize) {
            return new Size(baseFinalSize + aggregateFinalSize, Math.max(baseFinalSize + aggregateMaximalSize, baseMaximalSize));
        }
    }

    boolean isAssignable();

    Size apply(MethodVisitor methodVisitor);
}

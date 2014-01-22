package com.blogspot.mydailyjava.bytebuddy.method.bytecode.assign;

import org.objectweb.asm.MethodVisitor;

public interface Assignment {

    static class Size {

        private final int sizeImpact;
        private final int maximalSize;

        public Size(int sizeImpact, int maximalSize) {
            this.sizeImpact = sizeImpact;
            this.maximalSize = maximalSize;
        }

        public int getSizeImpact() {
            return sizeImpact;
        }

        public int getMaximalSize() {
            return maximalSize;
        }

        public Size aggregate(Size other) {
            return aggregate(other.sizeImpact, other.maximalSize);
        }

        public Size aggregate(int sizeChange, int interimMaximalSize) {
            return new Size(sizeImpact + sizeChange, Math.max(maximalSize, sizeImpact + interimMaximalSize));
        }
    }

    static class Compound implements Assignment {

        private final Assignment first;
        private final Assignment second;

        public Compound(Assignment first, Assignment second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean isAssignable() {
            return first.isAssignable() && second.isAssignable();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            return first.apply(methodVisitor).aggregate(second.apply(methodVisitor));
        }
    }

    boolean isAssignable();

    Size apply(MethodVisitor methodVisitor);
}

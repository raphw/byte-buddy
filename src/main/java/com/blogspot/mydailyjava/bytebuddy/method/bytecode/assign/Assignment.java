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

        private Size aggregate(int sizeChange, int interimMaximalSize) {
            return new Size(sizeImpact + sizeChange, Math.max(maximalSize, sizeImpact + interimMaximalSize));
        }

        @Override
        public String toString() {
            return "Assignment.Size{sizeImpact=" + sizeImpact + ", maximalSize=" + maximalSize + '}';
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
        public boolean isValid() {
            return first.isValid() && second.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor) {
            return first.apply(methodVisitor).aggregate(second.apply(methodVisitor));
        }

        @Override
        public String toString() {
            return "Assignment.Compound{first=" + first + ", second=" + second + '}';
        }
    }

    boolean isValid();

    Size apply(MethodVisitor methodVisitor);
}

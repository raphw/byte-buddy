package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import org.objectweb.asm.MethodVisitor;

public interface StackManipulation {

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
            return "StackManipulation.Size{sizeImpact=" + sizeImpact + ", maximalSize=" + maximalSize + '}';
        }
    }

    static class Compound implements StackManipulation {

        private final StackManipulation first;
        private final StackManipulation second;

        public Compound(StackManipulation first, StackManipulation second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean isValid() {
            return first.isValid() && second.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            return first.apply(methodVisitor, instrumentationContext).aggregate(second.apply(methodVisitor, instrumentationContext));
        }

        @Override
        public String toString() {
            return "StackManipulation.Compound{first=" + first + ", second=" + second + '}';
        }
    }

    boolean isValid();

    Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext);
}

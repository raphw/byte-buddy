package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

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

        private final StackManipulation[] stackManipulation;

        public Compound(StackManipulation... stackManipulation) {
            this.stackManipulation = stackManipulation;
        }

        @Override
        public boolean isValid() {
            for (StackManipulation stackManipulation : this.stackManipulation) {
                if (!stackManipulation.isValid()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
            Size size = new Size(0, 0);
            for (StackManipulation stackManipulation : this.stackManipulation) {
                size = size.aggregate(stackManipulation.apply(methodVisitor, instrumentationContext));
            }
            return size;
        }

        @Override
        public String toString() {
            return "StackManipulation.Compound{" + Arrays.asList(stackManipulation) + "}";
        }
    }

    boolean isValid();

    Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext);
}

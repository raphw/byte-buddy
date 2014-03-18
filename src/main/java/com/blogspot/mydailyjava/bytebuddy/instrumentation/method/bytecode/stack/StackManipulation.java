package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

/**
 * Describes a manipulation of a method's operand stack that does not affect the frame's variable array.
 */
public interface StackManipulation {

    /**
     * A description of the size change that is imposed by some
     * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation}.
     */
    static class Size {

        private final int sizeImpact;
        private final int maximalSize;

        /**
         * Creates an immutable descriptor of the size change that is implied by some stack manipulation.
         *
         * @param sizeImpact  The change of the size of the operand stack that is implied by some stack manipulation.
         * @param maximalSize The maximal stack size that is required for executing this stack manipulation. Should
         *                    never be negative number.
         */
        public Size(int sizeImpact, int maximalSize) {
            this.sizeImpact = sizeImpact;
            this.maximalSize = maximalSize;
        }

        /**
         * Returns the size change on the operand stack that is represented by this instance.
         *
         * @return The size change on the operand stack that is represented by this instance.
         */
        public int getSizeImpact() {
            return sizeImpact;
        }

        /**
         * Returns the maximal interim size of the operand stack that is represented by this instance.
         *
         * @return The maximal interim size of the operand stack that is represented by this instance.
         */
        public int getMaximalSize() {
            return maximalSize;
        }

        /**
         * Concatenates this size representation with another size representation in order to represent the size
         * change that is represented by both alterations of the operand stack size.
         *
         * @param other The other size representation.
         * @return A new size representation representing both stack size requirements.
         */
        public Size aggregate(Size other) {
            return aggregate(other.sizeImpact, other.maximalSize);
        }

        private Size aggregate(int sizeChange, int interimMaximalSize) {
            return new Size(sizeImpact + sizeChange, Math.max(maximalSize, sizeImpact + interimMaximalSize));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && maximalSize == ((Size) other).maximalSize
                    && sizeImpact == ((Size) other).sizeImpact;
        }

        @Override
        public int hashCode() {
            return 31 * sizeImpact + maximalSize;
        }

        @Override
        public String toString() {
            return "StackManipulation.Size{sizeImpact=" + sizeImpact + ", maximalSize=" + maximalSize + '}';
        }
    }

    /**
     * An immutable stack manipulation that aggregates a sequence of other stack manipulations.
     */
    static class Compound implements StackManipulation {

        private final StackManipulation[] stackManipulation;

        /**
         * Creates a new compound stack manipulation.
         *
         * @param stackManipulation The stack manipulations to be composed in the order of their composition.
         */
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
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(stackManipulation, ((Compound) other).stackManipulation);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(stackManipulation);
        }

        @Override
        public String toString() {
            return "StackManipulation.Compound{" + Arrays.asList(stackManipulation) + "}";
        }
    }

    /**
     * Determines if this stack manipulation is valid.
     *
     * @return If {@code false}, this manipulation cannot be applied and should throw an exception.
     */
    boolean isValid();

    /**
     * Applies the stack manipulation that is described by this instance.
     *
     * @param methodVisitor          The method visitor used to write the method implementation to.
     * @param instrumentationContext The context of the current instrumentation.
     * @return The changes to the size of the operand stack that are implied by this stack manipulation.
     */
    Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext);
}

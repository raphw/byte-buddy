package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Describes a manipulation of a method's operand stack that does not affect the frame's variable array.
 */
public interface StackManipulation {

    /**
     * Determines if this stack manipulation is valid.
     *
     * @return If {@code false}, this manipulation cannot be applied and should throw an exception.
     */
    boolean isValid();

    /**
     * Applies the stack manipulation that is described by this instance.
     *
     * @param methodVisitor         The method visitor used to write the method implementation to.
     * @param implementationContext The context of the current implementation.
     * @return The changes to the size of the operand stack that are implied by this stack manipulation.
     */
    Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext);

    /**
     * Canonical representation of an illegal stack manipulation.
     */
    enum Illegal implements StackManipulation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            throw new IllegalStateException("An illegal stack manipulation must not be applied");
        }

        @Override
        public String toString() {
            return "StackManipulation.Illegal." + name();
        }
    }

    /**
     * Canonical representation of a legal stack manipulation which does not require any action.
     */
    enum Trivial implements StackManipulation {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return StackSize.ZERO.toIncreasingSize();
        }

        @Override
        public String toString() {
            return "StackManipulation.Trivial." + name();
        }
    }

    /**
     * A description of the size change that is imposed by some
     * {@link StackManipulation}.
     */
    class Size {

        /**
         * The impact of any size operation onto the operand stack. This value can be negative if more values
         * were consumed from the stack than added to it.
         */
        private final int sizeImpact;

        /**
         * The maximal size of stack slots this stack manipulation ever requires. If an operation for example pushes
         * five values onto the stack and subsequently consumes three operations, this value should still be five
         * to express that a stack operation requires at least five slots in order to be applicable.
         */
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

        /**
         * Aggregates a size change with this stack manipulation size.
         *
         * @param sizeChange         The change in size the other operation implies.
         * @param interimMaximalSize The interim maximal size of the operand stack that the other operation requires
         *                           at least to function.
         * @return The aggregated size.
         */
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
    class Compound implements StackManipulation {

        /**
         * The stack manipulations this compound operation represents in their application order.
         */
        private final List<StackManipulation> stackManipulations;

        /**
         * Creates a new compound stack manipulation.
         *
         * @param stackManipulation The stack manipulations to be composed in the order of their composition.
         */
        public Compound(StackManipulation... stackManipulation) {
            this(Arrays.asList(stackManipulation));
        }

        /**
         * Creates a new compound stack manipulation.
         *
         * @param stackManipulations The stack manipulations to be composed in the order of their composition.
         */
        public Compound(List<? extends StackManipulation> stackManipulations) {
            this.stackManipulations = new ArrayList<StackManipulation>();
            for (StackManipulation stackManipulation : stackManipulations) {
                if (stackManipulation instanceof Compound) {
                    this.stackManipulations.addAll(((Compound) stackManipulation).stackManipulations);
                } else {
                    this.stackManipulations.add(stackManipulation);
                }
            }
        }

        @Override
        public boolean isValid() {
            for (StackManipulation stackManipulation : stackManipulations) {
                if (!stackManipulation.isValid()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            Size size = new Size(0, 0);
            for (StackManipulation stackManipulation : stackManipulations) {
                size = size.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
            }
            return size;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && stackManipulations.equals(((Compound) other).stackManipulations);
        }

        @Override
        public int hashCode() {
            return stackManipulations.hashCode();
        }

        @Override
        public String toString() {
            return "StackManipulation.Compound{stackManipulations=" + stackManipulations + "}";
        }
    }
}

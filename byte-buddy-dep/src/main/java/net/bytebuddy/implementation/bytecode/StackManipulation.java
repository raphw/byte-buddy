/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
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

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            throw new IllegalStateException("An illegal stack manipulation must not be applied");
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

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return StackSize.ZERO.toIncreasingSize();
        }
    }

    /**
     * A description of the size change that is imposed by some
     * {@link StackManipulation}.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
    }

    /**
     * An immutable stack manipulation that aggregates a sequence of other stack manipulations.
     */
    @HashCodeAndEqualsPlugin.Enhance
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
                } else if (!(stackManipulation instanceof Trivial)) {
                    this.stackManipulations.add(stackManipulation);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean isValid() {
            for (StackManipulation stackManipulation : stackManipulations) {
                if (!stackManipulation.isValid()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            Size size = new Size(0, 0);
            for (StackManipulation stackManipulation : stackManipulations) {
                size = size.aggregate(stackManipulation.apply(methodVisitor, implementationContext));
            }
            return size;
        }
    }
}

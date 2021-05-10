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

import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation that shifts right two numbers on the operand stack.
 */
public enum ShiftRight implements StackManipulation {

    /**
     * Shifts right two integers or integer-compatible values.
     */
    INTEGER(Opcodes.ISHR, StackSize.SINGLE, Unsigned.INTEGER),

    /**
     * Shifts right two longs.
     */
    LONG(Opcodes.LSHR, StackSize.DOUBLE, Unsigned.LONG);

    /**
     * The opcode to apply.
     */
    private final int opcode;

    /**
     * The stack size of the shift right primitive.
     */
    private final StackSize stackSize;

    /**
     * The unsigned equivalent of this operation.
     */
    private final StackManipulation unsigned;

    /**
     * Creates a new shift right.
     *
     * @param opcode    The opcode to apply.
     * @param stackSize The stack size of the shift right primitive.
     * @param unsigned  The unsigned equivalent of this operation.
     */
    ShiftRight(int opcode, StackSize stackSize, StackManipulation unsigned) {
        this.opcode = opcode;
        this.stackSize = stackSize;
        this.unsigned = unsigned;
    }

    /**
     * Returns the unsigned equivalent of this operation.
     *
     * @return The unsigned equivalent of this operation.
     */
    public StackManipulation toUnsigned() {
        return unsigned;
    }

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
        methodVisitor.visitInsn(opcode);
        return stackSize.toDecreasingSize();
    }

    /**
     * A stack manipulation that shifts right unsigned two numbers on the operand stack.
     */
    protected enum Unsigned implements StackManipulation {

        /**
         * Shifts right unsigned two integers or integer-compatible values.
         */
        INTEGER(Opcodes.IUSHR, StackSize.SINGLE),

        /**
         * Shifts right unsigned two longs.
         */
        LONG(Opcodes.LUSHR, StackSize.DOUBLE);

        /**
         * The opcode to apply.
         */
        private final int opcode;

        /**
         * The stack size of the shift right unsigned primitive.
         */
        private final StackSize stackSize;

        /**
         * Creates a new shift right unsigned.
         *
         * @param opcode    The opcode to apply.
         * @param stackSize The stack size of the shift right unsigned primitive.
         */
        Unsigned(int opcode, StackSize stackSize) {
            this.opcode = opcode;
            this.stackSize = stackSize;
        }

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
            methodVisitor.visitInsn(opcode);
            return stackSize.toDecreasingSize();
        }
    }
}

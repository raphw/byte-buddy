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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation that compares two primitive values on the operand stack.
 */
public enum PrimitiveComparison implements StackManipulation {

    /**
     * Compares two integer values on the operand stack
     * and pushes true (1) onto the stack if they are equal, or false (0) otherwise.
     */
    INTEGER_EQUALS(Opcodes.NOP, Opcodes.IF_ICMPNE, 1),
    /**
     * Compares two integer values on the operand stack
     * and pushes true (1) onto the stack if they are not equal, or false (0) otherwise.
     */
    INTEGER_NOT_EQUALS(Opcodes.NOP, Opcodes.IF_ICMPEQ, 1),
    /**
     * Compares two integer values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than the second value, or false (0) otherwise.
     */
    INTEGER_LESS_THAN(Opcodes.NOP, Opcodes.IF_ICMPGE, 1),
    /**
     * Compares two integer values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than or equal to the second value, or false (0) otherwise.
     */
    INTEGER_LESS_THAN_OR_EQUALS(Opcodes.NOP, Opcodes.IF_ICMPGT, 1),
    /**
     * Compares two integer values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than the second value, or false (0) otherwise.
     */
    INTEGER_GREATER_THAN(Opcodes.NOP, Opcodes.IF_ICMPLE, 1),
    /**
     * Compares two integer values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than or equal to the second value, or false (0) otherwise.
     */
    INTEGER_GREATER_THAN_OR_EQUALS(Opcodes.NOP, Opcodes.IF_ICMPLT, 1),

    /**
     * Compares two long values on the operand stack
     * and pushes true (1) onto the stack if they are equal, or false (0) otherwise.
     */
    LONG_EQUALS(Opcodes.LCMP, Opcodes.IFNE, 3),
    /**
     * Compares two long values on the operand stack
     * and pushes true (1) onto the stack if they are not equal, or false (0) otherwise.
     */
    LONG_NOT_EQUALS(Opcodes.LCMP, Opcodes.IFEQ, 3),
    /**
     * Compares two long values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than the second value, or false (0) otherwise.
     */
    LONG_LESS_THAN(Opcodes.LCMP, Opcodes.IFGE, 3),
    /**
     * Compares two long values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than or equal to the second value, or false (0) otherwise.
     */
    LONG_LESS_THAN_OR_EQUALS(Opcodes.LCMP, Opcodes.IFGT, 3),
    /**
     * Compares two long values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than the second value, or false (0) otherwise.
     */
    LONG_GREATER_THAN(Opcodes.LCMP, Opcodes.IFLE, 3),
    /**
     * Compares two long values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than or equal to the second value, or false (0) otherwise.
     */
    LONG_GREATER_THAN_OR_EQUALS(Opcodes.LCMP, Opcodes.IFLT, 3),

    /**
     * Compares two float values on the operand stack
     * and pushes true (1) onto the stack if they are equal, or false (0) otherwise.
     */
    FLOAT_EQUALS(Opcodes.FCMPL, Opcodes.IFNE, 1),
    /**
     * Compares two float values on the operand stack
     * and pushes true (1) onto the stack if they are not equal, or false (0) otherwise.
     */
    FLOAT_NOT_EQUALS(Opcodes.FCMPL, Opcodes.IFEQ, 1),
    /**
     * Compares two float values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than the second value, or false (0) otherwise.
     */
    FLOAT_LESS_THAN(Opcodes.FCMPG, Opcodes.IFGE, 1),
    /**
     * Compares two float values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than or equal to the second value, or false (0) otherwise.
     */
    FLOAT_LESS_THAN_OR_EQUALS(Opcodes.FCMPG, Opcodes.IFGT, 1),
    /**
     * Compares two float values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than the second value, or false (0) otherwise.
     */
    FLOAT_GREATER_THAN(Opcodes.FCMPL, Opcodes.IFLE, 1),
    /**
     * Compares two float values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than or equal to the second value, or false (0) otherwise.
     */
    FLOAT_GREATER_THAN_OR_EQUALS(Opcodes.FCMPL, Opcodes.IFLT, 1),

    /**
     * Compares two double values on the operand stack
     * and pushes true (1) onto the stack if they are equal, or false (0) otherwise.
     */
    DOUBLE_EQUALS(Opcodes.DCMPL, Opcodes.IFNE, 3),
    /**
     * Compares two double values on the operand stack
     * and pushes true (1) onto the stack if they are not equal, or false (0) otherwise.
     */
    DOUBLE_NOT_EQUALS(Opcodes.DCMPL, Opcodes.IFEQ, 3),
    /**
     * Compares two double values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than the second value, or false (0) otherwise.
     */
    DOUBLE_LESS_THAN(Opcodes.DCMPG, Opcodes.IFGE, 3),
    /**
     * Compares two double values on the operand stack
     * and pushes true (1) onto the stack if the first value is less than or equal to the second value, or false (0) otherwise.
     */
    DOUBLE_LESS_THAN_OR_EQUALS(Opcodes.DCMPG, Opcodes.IFGT, 3),
    /**
     * Compares two double values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than the second value, or false (0) otherwise.
     */
    DOUBLE_GREATER_THAN(Opcodes.DCMPL, Opcodes.IFLE, 3),
    /**
     * Compares two double values on the operand stack
     * and pushes true (1) onto the stack if the first value is greater than or equal to the second value, or false (0) otherwise.
     */
    DOUBLE_GREATER_THAN_OR_EQUALS(Opcodes.DCMPL, Opcodes.IFLT, 3);

    /**
     * The comparison opcode to apply.
     */
    private final int opcodeCmp;

    /**
     * The if opcode to apply.
     */
    private final int opcodeIf;

    /**
     * The stack size to decrease.
     */
    private final int stackDecreasingSize;

    /**
     * Creates a new comparison type.
     *
     * @param opcodeCmp           The comparison opcode to apply.
     * @param opcodeIf            The if opcode to apply.
     * @param stackDecreasingSize The stack size to decrease.
     */
    PrimitiveComparison(int opcodeCmp, int opcodeIf, int stackDecreasingSize) {
        this.opcodeCmp = opcodeCmp;
        this.opcodeIf = opcodeIf;
        this.stackDecreasingSize = stackDecreasingSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StackManipulation.Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        Label elseLabel = new Label();
        Label endLabel = new Label();

        if (opcodeCmp != Opcodes.NOP) methodVisitor.visitInsn(opcodeCmp);
        methodVisitor.visitJumpInsn(opcodeIf, elseLabel);

        // then block
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

        // else block
        methodVisitor.visitLabel(elseLabel);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitInsn(Opcodes.ICONST_0);

        methodVisitor.visitLabel(endLabel);
        methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER});

        return new StackManipulation.Size(-stackDecreasingSize, 0);
    }
}

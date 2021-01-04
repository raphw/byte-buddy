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
 * A stack manipulation that multiplies two numbers on the operand stack.
 */
public enum Multiplication implements StackManipulation {

    /**
     * Multiplies two integers or integer-compatible values.
     */
    INTEGER(Opcodes.IMUL, StackSize.SINGLE),

    /**
     * Multiplies two longs.
     */
    LONG(Opcodes.LMUL, StackSize.DOUBLE),

    /**
     * Multiplies two floats.
     */
    FLOAT(Opcodes.FMUL, StackSize.SINGLE),

    /**
     * Multiplies two doubles.
     */
    DOUBLE(Opcodes.DMUL, StackSize.DOUBLE);

    /**
     * The opcode to apply.
     */
    private final int opcode;

    /**
     * The stack size of the multiplied primitive.
     */
    private final StackSize stackSize;

    /**
     * Creates a new multiplication type.
     *
     * @param opcode    The opcode to apply.
     * @param stackSize The stack size of the multiplied primitive.
     */
    Multiplication(int opcode, StackSize stackSize) {
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

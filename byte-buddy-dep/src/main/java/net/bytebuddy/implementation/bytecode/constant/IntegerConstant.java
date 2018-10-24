/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * This class is responsible for loading any {@code int} constant onto the operand stack. Note that within the JVM,
 * {@code boolean}, {@code byte}, {@code short} and {@code char} values are represented by integers and can therefore
 * be loaded by using this class.
 */
public enum IntegerConstant implements StackManipulation {

    /**
     * A JVM-type {@code int} constant of value {@code -1}.
     */
    MINUS_ONE(Opcodes.ICONST_M1),

    /**
     * A JVM-type {@code int} constant of value {@code 0}.
     */
    ZERO(Opcodes.ICONST_0),

    /**
     * A JVM-type {@code int} constant of value {@code 1}.
     */
    ONE(Opcodes.ICONST_1),

    /**
     * A JVM-type {@code int} constant of value {@code 2}.
     */
    TWO(Opcodes.ICONST_2),

    /**
     * A JVM-type {@code int} constant of value {@code 3}.
     */
    THREE(Opcodes.ICONST_3),

    /**
     * A JVM-type {@code int} constant of value {@code 4}.
     */
    FOUR(Opcodes.ICONST_4),

    /**
     * A JVM-type {@code int} constant of value {@code 5}.
     */
    FIVE(Opcodes.ICONST_5);

    /**
     * The size impact of loading an {@code int} value onto the operand stack.
     */
    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    /**
     * The shortcut opcode for loading a common {@code int}-compatible JVM value onto the operand stack.
     */
    private final int opcode;

    /**
     * Creates a new JVM-integer constant loading stack manipulation for a given shortcut opcode.
     *
     * @param opcode The shortcut opcode for loading a common {@code int}-compatible JVM value onto the operand stack.
     */
    IntegerConstant(int opcode) {
        this.opcode = opcode;
    }

    /**
     * Creates a stack manipulation for loading a boolean value onto the stack.
     *
     * @param value The {@code boolean} to load onto the stack.
     * @return The stack manipulation for loading this {@code boolean}.
     */
    public static StackManipulation forValue(boolean value) {
        return value ? ONE : ZERO;
    }

    /**
     * Creates a stack manipulation for loading an {@code int} value onto the stack.
     * <p>&nbsp;</p>
     * This is achieved either by invoking a constant opcode, if any, or by creating a binary push operation.
     *
     * @param value The {@code int} (or {@code byte}, {@code short}, {@code char}) value to load onto the stack.
     * @return A stack manipulation for loading the given value.
     */
    public static StackManipulation forValue(int value) {
        switch (value) {
            case -1:
                return MINUS_ONE;
            case 0:
                return ZERO;
            case 1:
                return ONE;
            case 2:
                return TWO;
            case 3:
                return THREE;
            case 4:
                return FOUR;
            case 5:
                return FIVE;
            default:
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    return new SingleBytePush((byte) value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    return new TwoBytePush((short) value);
                } else {
                    return new ConstantPool(value);
                }
        }
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
        return SIZE;
    }

    /**
     * A stack manipulation that loads a JVM-integer value by a {@code BIPUSH} operation which is
     * legal for single byte integer values.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class SingleBytePush implements StackManipulation {

        /**
         * The single byte value to be loaded onto the operand stack.
         */
        private final byte value;

        /**
         * Creates a new {@code BIPUSH} stack manipulation for the given value.
         *
         * @param value The single byte value to be loaded onto the operand stack.
         */
        protected SingleBytePush(byte value) {
            this.value = value;
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
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, value);
            return SIZE;
        }
    }

    /**
     * A stack manipulation that loads a JVM-integer value by a {@code SIPUSH} operation which is
     * legal for up to two byte integer values.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class TwoBytePush implements StackManipulation {

        /**
         * The two byte value to be loaded onto the operand stack.
         */
        private final short value;

        /**
         * Creates a new {@code SIPUSH} stack manipulation for the given value.
         *
         * @param value The two byte value to be loaded onto the operand stack.
         */
        protected TwoBytePush(short value) {
            this.value = value;
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
            methodVisitor.visitIntInsn(Opcodes.SIPUSH, value);
            return SIZE;
        }
    }

    /**
     * A stack manipulation that loads a JVM-integer value from a constant pool value onto the operand stack.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class ConstantPool implements StackManipulation {

        /**
         * The JVM-integer value to load onto the operand stack.
         */
        private final int value;

        /**
         * Creates a new constant pool loading operation for a given JVM-integer.
         *
         * @param value The JVM-integer value to load onto the operand stack.
         */
        protected ConstantPool(int value) {
            this.value = value;
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
            methodVisitor.visitLdcInsn(value);
            return SIZE;
        }
    }
}

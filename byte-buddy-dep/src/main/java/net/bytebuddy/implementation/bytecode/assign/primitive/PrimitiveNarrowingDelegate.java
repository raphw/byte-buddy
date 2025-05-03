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
package net.bytebuddy.implementation.bytecode.assign.primitive;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This delegate is responsible for narrowing a primitive type to represent a <i>smaller</i> primitive type. The
 * rules for this narrowing are equivalent to those in the <a href="http://docs.oracle.com/javase/specs/">JLS</a>.
 * This class also includes the byte-to-char conversion in widening and narrowing primitive conversions.
 */
public enum PrimitiveNarrowingDelegate {

    /**
     * The narrowing delegate for {@code boolean} values.
     */
    BOOLEAN(StackManipulation.Trivial.INSTANCE,                                                     // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The narrowing delegate for {@code byte} values.
     */
    BYTE(StackManipulation.Illegal.INSTANCE,                                                        // to boolean
            StackManipulation.Trivial.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2C}, StackSize.ZERO.toDecreasingSize()),                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The narrowing delegate for {@code short} values.
     */
    SHORT(StackManipulation.Illegal.INSTANCE,                                                       // to boolean
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2B}, StackSize.ZERO.toDecreasingSize()),                     // to byte
            StackManipulation.Trivial.INSTANCE,                                                     // to short
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2C}, StackSize.ZERO.toDecreasingSize()),                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The narrowing delegate for {@code char} values.
     */
    CHARACTER(StackManipulation.Illegal.INSTANCE,                                                   // to boolean
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2B}, StackSize.ZERO.toDecreasingSize()),                     // to byte
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2S}, StackSize.ZERO.toDecreasingSize()),                     // to short
            StackManipulation.Trivial.INSTANCE,                                                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The narrowing delegate for {@code int} values.
     */
    INTEGER(StackManipulation.Illegal.INSTANCE,                                                     // to boolean
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2B}, StackSize.ZERO.toDecreasingSize()),                     // to byte
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2S}, StackSize.ZERO.toDecreasingSize()),                     // to short
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.I2C}, StackSize.ZERO.toDecreasingSize()),                     // to character
            StackManipulation.Trivial.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The narrowing delegate for {@code long} values.
     */
    LONG(StackManipulation.Illegal.INSTANCE,                                                        // to boolean
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.L2I, Opcodes.I2B}, StackSize.SINGLE.toDecreasingSize()),      // to byte
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.L2I, Opcodes.I2S}, StackSize.SINGLE.toDecreasingSize()),      // to short
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.L2I, Opcodes.I2C}, StackSize.SINGLE.toDecreasingSize()),      // to character
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.L2I}, StackSize.SINGLE.toDecreasingSize()),                   // to integer
            StackManipulation.Trivial.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The narrowing delegate for {@code float} values.
     */
    FLOAT(StackManipulation.Illegal.INSTANCE,                                                       // to boolean
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.F2I, Opcodes.I2B}, StackSize.ZERO.toDecreasingSize()),        // to byte
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.F2I, Opcodes.I2S}, StackSize.ZERO.toDecreasingSize()),        // to short
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.F2I, Opcodes.I2C}, StackSize.ZERO.toDecreasingSize()),        // to character
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.F2I}, StackSize.ZERO.toDecreasingSize()),                     // to integer
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.F2L}, StackSize.SINGLE.toIncreasingSize()),                   // to long
            StackManipulation.Trivial.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The narrowing delegate for {@code double} values.
     */
    DOUBLE(StackManipulation.Illegal.INSTANCE,                                                      // to boolean
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.D2I, Opcodes.I2B}, StackSize.SINGLE.toDecreasingSize()),      // to byte
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.D2I, Opcodes.I2S}, StackSize.SINGLE.toDecreasingSize()),      // to short
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.D2I, Opcodes.I2C}, StackSize.SINGLE.toDecreasingSize()),      // to character
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.D2I}, StackSize.SINGLE.toDecreasingSize()),                   // to integer
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.D2L}, StackSize.ZERO.toDecreasingSize()),                     // to long
            new PrimitiveNarrowingDelegate.NarrowingStackManipulation(
                    new int[]{Opcodes.D2F}, StackSize.SINGLE.toDecreasingSize()),                   // to float
            StackManipulation.Trivial.INSTANCE);                                                    // to double

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code boolean}.
     */
    private final StackManipulation toBooleanStackManipulation;

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code byte}.
     */
    private final StackManipulation toByteStackManipulation;

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code short}.
     */
    private final StackManipulation toShortStackManipulation;

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code char}.
     */
    private final StackManipulation toCharacterStackManipulation;

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code int}.
     */
    private final StackManipulation toIntegerStackManipulation;

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code long}.
     */
    private final StackManipulation toLongStackManipulation;

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code float}.
     */
    private final StackManipulation toFloatStackManipulation;

    /**
     * A stack manipulation that narrows the type that is represented by this instance to a {@code double}.
     */
    private final StackManipulation toDoubleStackManipulation;

    /**
     * Creates a new primitive narrowing delegate.
     *
     * @param toBooleanStackManipulation   A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code boolean}.
     * @param toByteStackManipulation      A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code byte}.
     * @param toShortStackManipulation     A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code short}.
     * @param toCharacterStackManipulation A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code char}.
     * @param toIntegerStackManipulation   A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code int}.
     * @param toLongStackManipulation      A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code long}.
     * @param toFloatStackManipulation     A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code float}.
     * @param toDoubleStackManipulation    A stack manipulation that narrows the type that is represented by this
     *                                     instance to a {@code double}.
     */
    PrimitiveNarrowingDelegate(StackManipulation toBooleanStackManipulation,
                               StackManipulation toByteStackManipulation,
                               StackManipulation toShortStackManipulation,
                               StackManipulation toCharacterStackManipulation,
                               StackManipulation toIntegerStackManipulation,
                               StackManipulation toLongStackManipulation,
                               StackManipulation toFloatStackManipulation,
                               StackManipulation toDoubleStackManipulation) {
        this.toBooleanStackManipulation = toBooleanStackManipulation;
        this.toByteStackManipulation = toByteStackManipulation;
        this.toShortStackManipulation = toShortStackManipulation;
        this.toCharacterStackManipulation = toCharacterStackManipulation;
        this.toIntegerStackManipulation = toIntegerStackManipulation;
        this.toLongStackManipulation = toLongStackManipulation;
        this.toFloatStackManipulation = toFloatStackManipulation;
        this.toDoubleStackManipulation = toDoubleStackManipulation;
    }

    /**
     * Locates the delegate that is capable of narrowing the given type into another type.
     *
     * @param typeDefinition A non-void primitive type that is to be narrowed into another type.
     * @return A delegate for the given type.
     */
    public static PrimitiveNarrowingDelegate forPrimitive(TypeDefinition typeDefinition) {
        if (typeDefinition.represents(boolean.class)) {
            return BOOLEAN;
        } else if (typeDefinition.represents(byte.class)) {
            return BYTE;
        } else if (typeDefinition.represents(short.class)) {
            return SHORT;
        } else if (typeDefinition.represents(char.class)) {
            return CHARACTER;
        } else if (typeDefinition.represents(int.class)) {
            return INTEGER;
        } else if (typeDefinition.represents(long.class)) {
            return LONG;
        } else if (typeDefinition.represents(float.class)) {
            return FLOAT;
        } else if (typeDefinition.represents(double.class)) {
            return DOUBLE;
        } else {
            throw new IllegalArgumentException("Not a primitive, non-void type: " + typeDefinition);
        }
    }

    /**
     * Attempts to narrow the represented type into another type.
     *
     * @param typeDefinition A non-void primitive type that is the expected result of the narrowing operation.
     * @return A narrowing instruction or an illegal stack manipulation if such narrowing is not legitimate.
     */
    public StackManipulation narrowTo(TypeDefinition typeDefinition) {
        if (typeDefinition.represents(boolean.class)) {
            return toBooleanStackManipulation;
        } else if (typeDefinition.represents(byte.class)) {
            return toByteStackManipulation;
        } else if (typeDefinition.represents(short.class)) {
            return toShortStackManipulation;
        } else if (typeDefinition.represents(char.class)) {
            return toCharacterStackManipulation;
        } else if (typeDefinition.represents(int.class)) {
            return toIntegerStackManipulation;
        } else if (typeDefinition.represents(long.class)) {
            return toLongStackManipulation;
        } else if (typeDefinition.represents(float.class)) {
            return toFloatStackManipulation;
        } else if (typeDefinition.represents(double.class)) {
            return toDoubleStackManipulation;
        } else {
            throw new IllegalArgumentException("Not a primitive non-void type: " + typeDefinition);
        }
    }

    /**
     * A stack manipulation that narrows a primitive type into a smaller primitive type.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class NarrowingStackManipulation extends StackManipulation.AbstractBase {

        /**
         * The opcode for executing the conversion.
         */
        private final int[] conversionOpcodes;

        /**
         * The size change of applying the conversion.
         */
        private final Size size;

        /**
         * Creates a new narrowing stack manipulation.
         *
         * @param conversionOpcodes The opcodes for executing the conversion.
         * @param size              The size change of applying the conversion.
         */
        protected NarrowingStackManipulation(int[] conversionOpcodes, Size size) {
            this.conversionOpcodes = conversionOpcodes;
            this.size = size;
        }

        /**
         * {@inheritDoc}
         */
        public Size apply(MethodVisitor methodVisitor, Implementation.Context context) {
            for (int conversionOpcode : conversionOpcodes) {
                methodVisitor.visitInsn(conversionOpcode);
            }
            return size;
        }
    }
}

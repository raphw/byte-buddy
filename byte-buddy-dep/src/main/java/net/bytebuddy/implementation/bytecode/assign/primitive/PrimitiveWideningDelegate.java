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
 * This delegate is responsible for widening a primitive type to represent a <i>larger</i> primitive type. The
 * rules for this widening are equivalent to those in the <a href="http://docs.oracle.com/javase/specs/">JLS</a>.
 */
public enum PrimitiveWideningDelegate {

    /**
     * The widening delegate for {@code boolean} values.
     */
    BOOLEAN(StackManipulation.Trivial.INSTANCE,                                                // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Illegal.INSTANCE),                                                    // to double

    /**
     * The widening delegate for {@code byte} values.
     */
    BYTE(StackManipulation.Illegal.INSTANCE,                                                        // to boolean
            StackManipulation.Trivial.INSTANCE,                                                // to byte
            StackManipulation.Trivial.INSTANCE,                                                // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Trivial.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize())),       // to double

    /**
     * The widening delegate for {@code short} values.
     */
    SHORT(StackManipulation.Illegal.INSTANCE,                                                       // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Trivial.INSTANCE,                                                // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Trivial.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    /**
     * The widening delegate for {@code char} values.
     */
    CHARACTER(StackManipulation.Illegal.INSTANCE,                                                   // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            StackManipulation.Trivial.INSTANCE,                                                // to character
            StackManipulation.Trivial.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    /**
     * The widening delegate for {@code int} values.
     */
    INTEGER(StackManipulation.Illegal.INSTANCE,                                                     // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Trivial.INSTANCE,                                                // to integer
            new WideningStackManipulation(Opcodes.I2L, StackSize.SINGLE.toIncreasingSize()),        // to long
            new WideningStackManipulation(Opcodes.I2F, StackSize.ZERO.toIncreasingSize()),          // to float
            new WideningStackManipulation(Opcodes.I2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    /**
     * The widening delegate for {@code long} values.
     */
    LONG(StackManipulation.Illegal.INSTANCE,                                                        // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Trivial.INSTANCE,                                                // to long
            new WideningStackManipulation(Opcodes.L2F, StackSize.SINGLE.toDecreasingSize()),        // to float
            new WideningStackManipulation(Opcodes.L2D, StackSize.ZERO.toIncreasingSize())),         // to double

    /**
     * The widening delegate for {@code float} values.
     */
    FLOAT(StackManipulation.Illegal.INSTANCE,                                                       // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Trivial.INSTANCE,                                                // to float
            new WideningStackManipulation(Opcodes.F2D, StackSize.SINGLE.toIncreasingSize())),       // to double

    /**
     * The widening delegate for {@code double} values.
     */
    DOUBLE(StackManipulation.Illegal.INSTANCE,                                                      // to boolean
            StackManipulation.Illegal.INSTANCE,                                                     // to byte
            StackManipulation.Illegal.INSTANCE,                                                     // to short
            StackManipulation.Illegal.INSTANCE,                                                     // to character
            StackManipulation.Illegal.INSTANCE,                                                     // to integer
            StackManipulation.Illegal.INSTANCE,                                                     // to long
            StackManipulation.Illegal.INSTANCE,                                                     // to float
            StackManipulation.Trivial.INSTANCE);                                               // to double

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code boolean}.
     */
    private final StackManipulation toBooleanStackManipulation;

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code byte}.
     */
    private final StackManipulation toByteStackManipulation;

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code short}.
     */
    private final StackManipulation toShortStackManipulation;

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code char}.
     */
    private final StackManipulation toCharacterStackManipulation;

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code int}.
     */
    private final StackManipulation toIntegerStackManipulation;

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code long}.
     */
    private final StackManipulation toLongStackManipulation;

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code float}.
     */
    private final StackManipulation toFloatStackManipulation;

    /**
     * A stack manipulation that widens the type that is represented by this instance to a {@code double}.
     */
    private final StackManipulation toDoubleStackManipulation;

    /**
     * Creates a new primitive widening delegate.
     *
     * @param toBooleanStackManipulation   A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code boolean}.
     * @param toByteStackManipulation      A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code byte}.
     * @param toShortStackManipulation     A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code short}.
     * @param toCharacterStackManipulation A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code char}.
     * @param toIntegerStackManipulation   A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code int}.
     * @param toLongStackManipulation      A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code long}.
     * @param toFloatStackManipulation     A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code float}.
     * @param toDoubleStackManipulation    A stack manipulation that widens the type that is represented by this
     *                                     instance to a {@code double}.
     */
    PrimitiveWideningDelegate(StackManipulation toBooleanStackManipulation,
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
     * Locates the delegate that is capable of widening the given type into another type.
     *
     * @param typeDefinition A non-void primitive type that is to be widened into another type.
     * @return A delegate for the given type.
     */
    public static PrimitiveWideningDelegate forPrimitive(TypeDefinition typeDefinition) {
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
     * Attempts to widen the represented type into another type.
     *
     * @param typeDefinition A non-void primitive type that is the expected result of the widening operation.
     * @return A widening instruction or an illegal stack manipulation if such widening is not legitimate.
     */
    public StackManipulation widenTo(TypeDefinition typeDefinition) {
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
     * A stack manipulation that widens a primitive type into a more general primitive type.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class WideningStackManipulation implements StackManipulation {

        /**
         * The opcode for executing the conversion.
         */
        private final int conversionOpcode;

        /**
         * The size change of applying the conversion.
         */
        private final Size size;

        /**
         * Creates a new widening stack manipulation.
         *
         * @param conversionOpcode The opcode for executing the conversion.
         * @param size             The size change of applying the conversion.
         */
        protected WideningStackManipulation(int conversionOpcode, Size size) {
            this.conversionOpcode = conversionOpcode;
            this.size = size;
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
            methodVisitor.visitInsn(conversionOpcode);
            return size;
        }
    }
}

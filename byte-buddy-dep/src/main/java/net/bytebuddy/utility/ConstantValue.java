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
package net.bytebuddy.utility;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.ClassConstant;
import net.bytebuddy.implementation.bytecode.constant.IntegerConstant;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import net.bytebuddy.utility.nullability.MaybeNull;

/**
 * Represents a value that can be represented as a constant expression or constant pool value.
 */
public interface ConstantValue {

    /**
     * Returns a description of the type of this constant.
     *
     * @return A description of the type of this constant.
     */
    TypeDescription getTypeDescription();

    /**
     * Returns a stack manipulation loading this value.
     *
     * @return A stack manipulation loading this value.
     */
    StackManipulation toStackManipulation();

    /**
     * A simple representation of a constant value.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Simple implements ConstantValue {

        /**
         * A stack manipulation that loads a constant.
         */
        private final StackManipulation stackManipulation;

        /**
         * The description of the constant value's type.
         */
        private final TypeDescription typeDescription;

        /**
         * Creates a simple constant value.
         *
         * @param stackManipulation A stack manipulation that loads a constant.
         * @param typeDescription   The description of the constant value's type.
         */
        protected Simple(StackManipulation stackManipulation, TypeDescription typeDescription) {
            this.stackManipulation = stackManipulation;
            this.typeDescription = typeDescription;
        }

        /**
         * Returns a constant value for the supplied constant value.
         *
         * @param value The value to represent as a constant.
         * @return An appropriate representation of the constant value.
         */
        public static ConstantValue wrap(Object value) {
            ConstantValue constant = wrapOrNull(value);
            if (constant == null) {
                throw new IllegalArgumentException("Not a constant value: " + value);
            } else {
                return constant;
            }
        }

        /**
         * Returns a constant value for the supplied constant value.
         *
         * @param value The value to represent as a constant.
         * @return An appropriate representation of the constant value or {@code null} if the
         * supplied value is not representable as a compile-time constant.
         */
        @MaybeNull
        public static ConstantValue wrapOrNull(Object value) {
            if (value instanceof ConstantValue) {
                return (ConstantValue) value;
            } else if (value instanceof TypeDescription) {
                return ((TypeDescription) value).isPrimitive()
                        ? new Simple(ClassConstant.of(((TypeDescription) value)), TypeDescription.ForLoadedType.of(Class.class))
                        : JavaConstant.Simple.of((TypeDescription) value);
            } else if (value instanceof EnumerationDescription) {
                return new Simple(FieldAccess.forEnumeration((EnumerationDescription) value), ((EnumerationDescription) value).getEnumerationType());
            } else if (value instanceof Boolean) {
                return new Simple(IntegerConstant.forValue((Boolean) value), TypeDescription.ForLoadedType.of(boolean.class));
            } else if (value instanceof Byte) {
                return new Simple(IntegerConstant.forValue((Byte) value), TypeDescription.ForLoadedType.of(byte.class));
            } else if (value instanceof Short) {
                return new Simple(IntegerConstant.forValue((Short) value), TypeDescription.ForLoadedType.of(short.class));
            } else if (value instanceof Character) {
                return new Simple(IntegerConstant.forValue((Character) value), TypeDescription.ForLoadedType.of(char.class));
            } else if (value instanceof Class<?>) {
                return ((Class<?>) value).isPrimitive()
                        ? new Simple(ClassConstant.of(TypeDescription.ForLoadedType.of((Class<?>) value)), TypeDescription.ForLoadedType.of(Class.class))
                        : JavaConstant.Simple.of(TypeDescription.ForLoadedType.of((Class<?>) value));
            } else if (value instanceof Enum<?>) {
                return new Simple(FieldAccess.forEnumeration(new EnumerationDescription.ForLoadedEnumeration((Enum<?>) value)), TypeDescription.ForLoadedType.of(((Enum<?>) value).getDeclaringClass()));
            } else {
                return JavaConstant.Simple.ofLoadedOrNull(value);
            }
        }

        /**
         * {@inheritDoc}
         */
        public TypeDescription getTypeDescription() {
            return typeDescription;
        }

        /**
         * {@inheritDoc}
         */
        public StackManipulation toStackManipulation() {
            return stackManipulation;
        }
    }
}

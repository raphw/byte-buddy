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
package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.objectweb.asm.MethodVisitor;

/**
 * Represents a stack assignment that loads the default value of a given type onto the stack.
 */
public enum DefaultValue implements StackManipulation {

    /**
     * The default value of a JVM integer which covers Java's {@code int}, {@code boolean}, {@code byte},
     * {@code short} and {@code char} values.
     */
    INTEGER(IntegerConstant.ZERO),

    /**
     * The default value of a {@code long}.
     */
    LONG(LongConstant.ZERO),

    /**
     * The default value of a {@code float}.
     */
    FLOAT(FloatConstant.ZERO),

    /**
     * The default value of a {@code double}.
     */
    DOUBLE(DoubleConstant.ZERO),

    /**
     * The default value of a {@code void} which resembles a no-op manipulation.
     */
    VOID(Trivial.INSTANCE),

    /**
     * The default value of a reference type which resembles the {@code null} reference.
     */
    REFERENCE(NullConstant.INSTANCE);

    /**
     * The stack manipulation that represents the loading of a given default value onto the operand stack.
     */
    private final StackManipulation stackManipulation;

    /**
     * Creates a new default value load operation.
     *
     * @param stackManipulation The stack manipulation that represents the loading of a given default value onto the
     *                          operand stack.
     */
    DefaultValue(StackManipulation stackManipulation) {
        this.stackManipulation = stackManipulation;
    }

    /**
     * Creates a stack assignment that loads the default value for a given type.
     *
     * @param typeDefinition The type for which a default value should be loaded onto the operand stack.
     * @return A stack manipulation loading the default value for the given type.
     */
    public static StackManipulation of(TypeDefinition typeDefinition) {
        if (typeDefinition.isPrimitive()) {
            if (typeDefinition.represents(long.class)) {
                return LONG;
            } else if (typeDefinition.represents(double.class)) {
                return DOUBLE;
            } else if (typeDefinition.represents(float.class)) {
                return FLOAT;
            } else if (typeDefinition.represents(void.class)) {
                return VOID;
            } else {
                return INTEGER;
            }
        } else {
            return REFERENCE;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return stackManipulation.isValid();
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        return stackManipulation.apply(methodVisitor, implementationContext);
    }
}

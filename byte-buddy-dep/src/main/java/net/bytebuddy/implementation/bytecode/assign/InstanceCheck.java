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
package net.bytebuddy.implementation.bytecode.assign;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Implements an {@code instanceof} check.
 */
@HashCodeAndEqualsPlugin.Enhance
public class InstanceCheck implements StackManipulation {

    /**
     * The type to apply the instance check against.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new instance check.
     *
     * @param typeDescription The type to apply the instance check against.
     */
    protected InstanceCheck(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Creates a new instance check.
     *
     * @param typeDescription The type to apply the instance check against.
     * @return An appropriate stack manipulation.
     */
    public static StackManipulation of(TypeDescription typeDescription) {
        if (typeDescription.isPrimitive()) {
            throw new IllegalArgumentException("Cannot check an instance against a primitive type: " + typeDescription);
        }
        return new InstanceCheck(typeDescription);
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
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, typeDescription.getInternalName());
        return new Size(0, 0);
    }
}

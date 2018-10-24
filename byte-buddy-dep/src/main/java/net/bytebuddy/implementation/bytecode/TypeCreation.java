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
package net.bytebuddy.implementation.bytecode;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation for creating an <i>undefined</i> type on which a constructor is to be called.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TypeCreation implements StackManipulation {

    /**
     * The type that is being created.
     */
    private final TypeDescription typeDescription;

    /**
     * Constructs a new type creation.
     *
     * @param typeDescription The type to be create.
     */
    protected TypeCreation(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Creates a type creation for the given type.
     *
     * @param typeDescription The type to be create.
     * @return A stack manipulation that represents the creation of the given type.
     */
    public static StackManipulation of(TypeDescription typeDescription) {
        if (typeDescription.isArray() || typeDescription.isPrimitive() || typeDescription.isAbstract()) {
            throw new IllegalArgumentException(typeDescription + " is not instantiable");
        }
        return new TypeCreation(typeDescription);
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
        methodVisitor.visitTypeInsn(Opcodes.NEW, typeDescription.getInternalName());
        return new Size(1, 1);
    }
}

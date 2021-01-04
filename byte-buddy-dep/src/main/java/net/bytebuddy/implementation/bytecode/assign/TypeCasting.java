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
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A stack manipulation for a type down casting. Such castings are not implicit but must be performed explicitly.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TypeCasting implements StackManipulation {

    /**
     * The type description to which a value should be casted.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new type casting.
     *
     * @param typeDescription The type description to which a value should be casted.
     */
    protected TypeCasting(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * Creates a casting to the given, non-primitive type.
     *
     * @param typeDefinition The type to which a value should be casted.
     * @return A stack manipulation that represents the casting.
     */
    public static StackManipulation to(TypeDefinition typeDefinition) {
        if (typeDefinition.isPrimitive()) {
            throw new IllegalArgumentException("Cannot cast to primitive type: " + typeDefinition);
        }
        return new TypeCasting(typeDefinition.asErasure());
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
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, typeDescription.getInternalName());
        return StackSize.ZERO.toIncreasingSize();
    }
}

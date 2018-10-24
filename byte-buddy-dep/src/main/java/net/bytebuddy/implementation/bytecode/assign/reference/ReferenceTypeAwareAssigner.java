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
package net.bytebuddy.implementation.bytecode.assign.reference;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.assign.TypeCasting;

/**
 * A simple assigner that is capable of handling the casting of reference types. Primitives can only be assigned to
 * each other if they represent the same type.
 */
public enum ReferenceTypeAwareAssigner implements Assigner {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
        if (source.isPrimitive() || target.isPrimitive()) {
            return source.equals(target)
                    ? StackManipulation.Trivial.INSTANCE
                    : StackManipulation.Illegal.INSTANCE;

        } else if (source.asErasure().isAssignableTo(target.asErasure())) {
            return StackManipulation.Trivial.INSTANCE;
        } else if (typing.isDynamic()) {
            return TypeCasting.to(target);
        } else {
            return StackManipulation.Illegal.INSTANCE;
        }
    }
}

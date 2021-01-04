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
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * This assigner is able to handle non-{@code void}, primitive types. This means:
 * <ol>
 * <li>If a primitive type is assigned to a non-primitive type, it will attempt to widen the source type into the
 * target type.</li>
 * <li>If a primitive type is assigned to a non-primitive type, it will attempt to box the source type and then
 * query the chained assigner for assigning the boxed type to the target type.</li>
 * <li>If a non-primitive type is assigned to a primitive type, it will unbox the source type and then attempt a
 * widening of the unboxed type into the target type. If the source type does not represent a wrapper type, it will
 * attempt to infer the boxing type from the target type and check if the source type is assignable to this wrapper
 * type.</li>
 * <li>If two non-primitive types are subject of the assignment, it will delegate the assignment to its chained
 * assigner.</li>
 * </ol>
 */
@HashCodeAndEqualsPlugin.Enhance
public class PrimitiveTypeAwareAssigner implements Assigner {

    /**
     * Another assigner that is aware of assigning reference types. This assigner is queried for assigning
     * non-primitive types or for assigning a boxed type to another non-primitive type.
     */
    private final Assigner referenceTypeAwareAssigner;

    /**
     * Creates a new assigner with the given delegate.
     *
     * @param referenceTypeAwareAssigner A chained assigner that is queried for assignments not involving primitive
     *                                   types.
     */
    public PrimitiveTypeAwareAssigner(Assigner referenceTypeAwareAssigner) {
        this.referenceTypeAwareAssigner = referenceTypeAwareAssigner;
    }

    /**
     * {@inheritDoc}
     */
    public StackManipulation assign(TypeDescription.Generic source, TypeDescription.Generic target, Typing typing) {
        if (source.isPrimitive() && target.isPrimitive()) {
            return PrimitiveWideningDelegate.forPrimitive(source).widenTo(target);
        } else if (source.isPrimitive() /* && !target.isPrimitive() */) {
            return PrimitiveBoxingDelegate.forPrimitive(source).assignBoxedTo(target, referenceTypeAwareAssigner, typing);
        } else if (/* !source.isPrimitive() && */ target.isPrimitive()) {
            return PrimitiveUnboxingDelegate.forReferenceType(source).assignUnboxedTo(target, referenceTypeAwareAssigner, typing);
        } else /* !source.isPrimitive() && !target.isPrimitive()) */ {
            return referenceTypeAwareAssigner.assign(source, target, typing);
        }
    }
}

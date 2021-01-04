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
package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * This ambiguity resolver matches that method out of two methods that is declared by the more specific type. If two
 * methods are declared by the same type or by two unrelated types, this resolver returns an ambiguous result.
 */
public enum DeclaringTypeResolver implements MethodDelegationBinder.AmbiguityResolver {

    /**
     * The singleton instance.
     */
    INSTANCE;

    /**
     * {@inheritDoc}
     */
    public Resolution resolve(MethodDescription source,
                              MethodDelegationBinder.MethodBinding left,
                              MethodDelegationBinder.MethodBinding right) {
        TypeDescription leftType = left.getTarget().getDeclaringType().asErasure();
        TypeDescription rightType = right.getTarget().getDeclaringType().asErasure();
        if (leftType.equals(rightType)) {
            return Resolution.AMBIGUOUS;
        } else if (leftType.isAssignableFrom(rightType)) {
            return Resolution.RIGHT;
        } else if (leftType.isAssignableTo(rightType)) {
            return Resolution.LEFT;
        } else {
            return Resolution.AMBIGUOUS;
        }
    }
}

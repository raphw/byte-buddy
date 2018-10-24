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
package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;

/**
 * Implementation of an
 * {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver}
 * that resolves conflicting bindings by considering equality of a target method's internalName as an indicator for a dominant
 * binding.
 * <p>&nbsp;</p>
 * For example, if method {@code source.foo} can be bound to methods {@code targetA.foo} and {@code targetB.bar},
 * {@code targetA.foo} will be considered as dominant.
 */
public enum MethodNameEqualityResolver implements MethodDelegationBinder.AmbiguityResolver {

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
        boolean leftEquals = left.getTarget().getName().equals(source.getName());
        boolean rightEquals = right.getTarget().getName().equals(source.getName());
        if (leftEquals ^ rightEquals) {
            return leftEquals ? Resolution.LEFT : Resolution.RIGHT;
        } else {
            return Resolution.AMBIGUOUS;
        }
    }
}

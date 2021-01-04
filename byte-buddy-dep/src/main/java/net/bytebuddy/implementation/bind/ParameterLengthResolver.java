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

/**
 * This {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.AmbiguityResolver} selects
 * the method with more arguments. If two methods have equally many arguments, the resolution is ambiguous.
 */
public enum ParameterLengthResolver implements MethodDelegationBinder.AmbiguityResolver {

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
        int leftLength = left.getTarget().getParameters().size();
        int rightLength = right.getTarget().getParameters().size();
        if (leftLength == rightLength) {
            return Resolution.AMBIGUOUS;
        } else if (leftLength < rightLength) {
            return Resolution.RIGHT;
        } else {
            return Resolution.LEFT;
        }
    }
}

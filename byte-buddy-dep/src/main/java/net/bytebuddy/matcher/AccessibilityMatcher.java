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
package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that validates that a given byte code element is accessible to a given type.
 *
 * @param <T>The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class AccessibilityMatcher<T extends ByteCodeElement> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type that is to be checked for its viewing rights.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a matcher that validates that a byte code element can be seen by a given type.
     *
     * @param typeDescription The type that is to be checked for its viewing rights.
     */
    public AccessibilityMatcher(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target.isAccessibleTo(typeDescription);
    }

    @Override
    public String toString() {
        return "isAccessibleTo(" + typeDescription + ")";
    }
}

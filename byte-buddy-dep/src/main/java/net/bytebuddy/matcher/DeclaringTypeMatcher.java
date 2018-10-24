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
import net.bytebuddy.description.DeclaredByType;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches the declaring type of another element, only if this element is actually declared
 * in a type.
 *
 * @param <T> The exact type of the element being matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DeclaringTypeMatcher<T extends DeclaredByType> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type matcher to be applied if the target element is declared in a type.
     */
    private final ElementMatcher<? super TypeDescription.Generic> matcher;

    /**
     * Creates a new matcher for the declaring type of an element.
     *
     * @param matcher The type matcher to be applied if the target element is declared in a type.
     */
    public DeclaringTypeMatcher(ElementMatcher<? super TypeDescription.Generic> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        TypeDefinition declaringType = target.getDeclaringType();
        return declaringType != null && matcher.matches(declaringType.asGenericType());
    }

    @Override
    public String toString() {
        return "declaredBy(" + matcher + ")";
    }
}

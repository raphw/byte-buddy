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
import net.bytebuddy.description.type.TypeDefinition;

/**
 * An element matcher that validates that a given generic type description represents a type of a given name.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TypeSortMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * An element matcher to be applied to the type's sort.
     */
    private final ElementMatcher<? super TypeDefinition.Sort> matcher;

    /**
     * Creates a new type sort matcher.
     *
     * @param matcher An element matcher to be applied to the type's sort.
     */
    public TypeSortMatcher(ElementMatcher<? super TypeDefinition.Sort> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getSort());
    }

    @Override
    public String toString() {
        return "ofSort(" + matcher + ')';
    }
}

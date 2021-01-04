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
package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDefinition;

/**
 * An element matcher that checks if a type description declares methods of a given property.
 *
 * @param <T> The exact type of the annotated element that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DeclaringMethodMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The field matcher to apply to the declared fields of the matched type description.
     */
    private final ElementMatcher<? super MethodList<?>> matcher;

    /**
     * Creates a new matcher for a type's declared methods.
     *
     * @param matcher The method matcher to apply to the declared methods of the matched type description.
     */
    public DeclaringMethodMatcher(ElementMatcher<? super MethodList<? extends MethodDescription>> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getDeclaredMethods());
    }

    @Override
    public String toString() {
        return "declaresMethods(" + matcher + ")";
    }
}

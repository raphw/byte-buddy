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
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDefinition;

/**
 * An element matcher that checks if a type description declares fields of a given property.
 *
 * @param <T> The exact type of the annotated element that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DeclaringFieldMatcher<T extends TypeDefinition> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The field matcher to apply to the declared fields of the matched type description.
     */
    private final ElementMatcher<? super FieldList<?>> matcher;

    /**
     * Creates a new matcher for a type's declared fields.
     *
     * @param matcher The field matcher to apply to the declared fields of the matched type description.
     */
    public DeclaringFieldMatcher(ElementMatcher<? super FieldList<? extends FieldDescription>> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matcher.matches(target.getDeclaredFields());
    }

    @Override
    public String toString() {
        return "declaresFields(" + matcher + ")";
    }
}

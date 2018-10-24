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
import net.bytebuddy.description.type.TypeDescription;

/**
 * An element matcher that matches its argument for being another type's subtype.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class SubTypeMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The type to be matched being a super type of the matched type.
     */
    private final TypeDescription typeDescription;

    /**
     * Creates a new matcher for matching its input for being a sub type of the given {@code typeDescription}.
     *
     * @param typeDescription The type to be matched being a super type of the matched type.
     */
    public SubTypeMatcher(TypeDescription typeDescription) {
        this.typeDescription = typeDescription;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target.isAssignableTo(typeDescription);
    }

    @Override
    public String toString() {
        return "isSubTypeOf(" + typeDescription + ')';
    }
}


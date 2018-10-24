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
 * An element matcher that matches an object's type.
 *
 * @param <T> The exact type of the object that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class InstanceTypeMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply to the object's type.
     */
    private final ElementMatcher<? super TypeDescription> matcher;

    /**
     * Creates a new instance type matcher.
     *
     * @param matcher The matcher to apply to the object's type.
     */
    public InstanceTypeMatcher(ElementMatcher<? super TypeDescription> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target != null && matcher.matches(TypeDescription.ForLoadedType.of(target.getClass()));
    }

    @Override
    public String toString() {
        return "ofType(" + matcher + ")";
    }
}

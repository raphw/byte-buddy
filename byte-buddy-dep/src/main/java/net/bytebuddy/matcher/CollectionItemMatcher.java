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

/**
 * A list item matcher matches any element of a collection to a given matcher and assures that at least one
 * element matches the supplied iterable condition.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CollectionItemMatcher<T> extends ElementMatcher.Junction.AbstractBase<Iterable<? extends T>> {

    /**
     * The element matcher to apply to each element of a collection.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * Creates a new matcher that applies another matcher to each element of a matched iterable collection.
     *
     * @param matcher The element matcher to apply to each element of a iterable collection.
     */
    public CollectionItemMatcher(ElementMatcher<? super T> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(Iterable<? extends T> target) {
        for (T value : target) {
            if (matcher.matches(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "whereOne(" + matcher + ")";
    }
}

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

import java.util.Iterator;

/**
 * A matcher that matches a given element of a collection. If no such element is contained by the matched iterable, the matcher
 * returns {@code false}.
 *
 * @param <T> The type of the elements contained by the collection.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CollectionElementMatcher<T> extends ElementMatcher.Junction.AbstractBase<Iterable<? extends T>> {

    /**
     * The index of the matched element.
     */
    private final int index;

    /**
     * The matcher for the given element, if it exists.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * Creates a new matcher for an element in a collection.
     *
     * @param index   The index of the matched element.
     * @param matcher The matcher for the given element, if it exists.
     */
    public CollectionElementMatcher(int index, ElementMatcher<? super T> matcher) {
        this.index = index;
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(Iterable<? extends T> target) {
        Iterator<? extends T> iterator = target.iterator();
        for (int index = 0; index < this.index; index++) {
            if (iterator.hasNext()) {
                iterator.next();
            } else {
                return false;
            }
        }
        return iterator.hasNext() && matcher.matches(iterator.next());
    }

    @Override
    public String toString() {
        return "with(" + index + " matches " + matcher + ")";
    }
}

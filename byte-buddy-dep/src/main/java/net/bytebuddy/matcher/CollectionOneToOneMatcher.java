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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An element matcher that matches a given iterable collection to a list of matchers on a per-element basis. For a
 * successful match, any element of the matched iterable collection must be successfully matched by a next
 * matcher of the supplied list of element matchers. For this to be possible, the matched iterable collection
 * and the supplied list of element matchers contain the same number of elements.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CollectionOneToOneMatcher<T> extends ElementMatcher.Junction.AbstractBase<Iterable<? extends T>> {

    /**
     * The list of element matchers to match any elements of the matched iterable collection against.
     */
    private final List<? extends ElementMatcher<? super T>> matchers;

    /**
     * Creates a new matcher that compares a matched iterable collection against a list of element matchers.
     *
     * @param matchers The list of element matchers to match any elements of the matched iterable collection
     *                 against.
     */
    public CollectionOneToOneMatcher(List<? extends ElementMatcher<? super T>> matchers) {
        this.matchers = matchers;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(Iterable<? extends T> target) {
        if ((target instanceof Collection) && ((Collection<?>) target).size() != matchers.size()) {
            return false;
        }
        Iterator<? extends ElementMatcher<? super T>> iterator = matchers.iterator();
        for (T value : target) {
            if (!iterator.hasNext() || !iterator.next().matches(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("containing(");
        boolean first = true;
        for (Object value : matchers) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(", ");
            }
            stringBuilder.append(value);
        }
        return stringBuilder.append(')').toString();
    }
}

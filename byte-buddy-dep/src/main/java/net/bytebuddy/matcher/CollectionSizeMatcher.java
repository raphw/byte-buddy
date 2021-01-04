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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.util.Collection;

/**
 * An element matcher that matches a collection by its size.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class CollectionSizeMatcher<T extends Iterable<?>> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The expected size of the matched collection.
     */
    private final int size;

    /**
     * Creates a new matcher that matches the size of a matched collection.
     *
     * @param size The expected size of the matched collection.
     */
    public CollectionSizeMatcher(int size) {
        this.size = size;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value = "DLS_DEAD_LOCAL_STORE", justification = "Iteration required to count size of an iterable")
    public boolean matches(T target) {
        if (target instanceof Collection) {
            return ((Collection<?>) target).size() == size;
        } else {
            int size = 0;
            for (Object ignored : target) {
                size++;
            }
            return size == this.size;
        }
    }

    @Override
    public String toString() {
        return "ofSize(" + size + ')';
    }
}

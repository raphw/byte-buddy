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
 * An element matcher that matches the {@code null} value.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class NullMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * An instance of this matcher.
     */
    private static final NullMatcher<?> INSTANCE = new NullMatcher<Object>();

    /**
     * Returns a matcher that only matches {@code null}.
     *
     * @param <T> The type of the matched entity.
     * @return A matcher that only matches {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> ElementMatcher.Junction<T> make() {
        return (ElementMatcher.Junction<T>) INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return target == null;
    }

    @Override
    public String toString() {
        return "isNull()";
    }
}

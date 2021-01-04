/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
 * An element matcher that returns a fixed result.
 *
 * @param <T> The actual matched type of this matcher.
 */
@HashCodeAndEqualsPlugin.Enhance
public class BooleanMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    private static final BooleanMatcher<?> TRUE = new BooleanMatcher<Object>(true);
    private static final BooleanMatcher<?> FALSE = new BooleanMatcher<Object>(false);

    /**
     * Returns boolean element matcher.
     *
     * @param matches The predefined result.
     */
    @SuppressWarnings("unchecked")
    public static <T> BooleanMatcher<T> of(boolean matches) {
        return (BooleanMatcher<T>) (matches ? TRUE : FALSE);
    }

    /**
     * The predefined result.
     */
    private final boolean matches;

    /**
     * Creates a new boolean element matcher.
     *
     * @param matches The predefined result.
     */
    public BooleanMatcher(boolean matches) {
        this.matches = matches;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return matches;
    }

    @Override
    public String toString() {
        return Boolean.toString(matches);
    }
}

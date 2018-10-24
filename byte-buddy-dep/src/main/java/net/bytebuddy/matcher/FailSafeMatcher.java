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

/**
 * A fail-safe matcher catches exceptions that are thrown by a delegate matcher and returns an alternative value.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class FailSafeMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The delegate matcher that might throw an exception.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * The fallback value in case of an exception.
     */
    private final boolean fallback;

    /**
     * Creates a new fail-safe element matcher.
     *
     * @param matcher  The delegate matcher that might throw an exception.
     * @param fallback The fallback value in case of an exception.
     */
    public FailSafeMatcher(ElementMatcher<? super T> matcher, boolean fallback) {
        this.matcher = matcher;
        this.fallback = fallback;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        try {
            return matcher.matches(target);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public String toString() {
        return "failSafe(try(" + matcher + ") or " + fallback + ")";
    }
}

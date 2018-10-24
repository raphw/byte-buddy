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
 * An element matcher that reverses the matching result of another matcher.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class NegatingMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The element matcher to be negated.
     */
    private final ElementMatcher<? super T> matcher;

    /**
     * Creates a new negating element matcher.
     *
     * @param matcher The element matcher to be negated.
     */
    public NegatingMatcher(ElementMatcher<? super T> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return !matcher.matches(target);
    }

    @Override
    public String toString() {
        return "not(" + matcher + ')';
    }
}

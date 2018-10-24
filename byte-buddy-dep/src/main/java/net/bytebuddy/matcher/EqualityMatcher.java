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
 * An element matcher that checks an object's equality to another object.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class EqualityMatcher<T> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The object that is checked to be equal to the matched value.
     */
    private final Object value;

    /**
     * Creates an element matcher that tests for equality.
     *
     * @param value The object that is checked to be equal to the matched value.
     */
    public EqualityMatcher(Object value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return value.equals(target);
    }

    @Override
    public String toString() {
        return "is(" + value + ")";
    }
}

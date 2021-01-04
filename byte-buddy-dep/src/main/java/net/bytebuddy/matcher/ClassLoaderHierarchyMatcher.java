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
 * An element matcher that matches all {@link java.lang.ClassLoader}s in the matched class loaders hierarchy
 * against a given matcher.
 *
 * @param <T> The exact type of the class loader that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ClassLoaderHierarchyMatcher<T extends ClassLoader> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The matcher to apply on each class loader in the hierarchy.
     */
    private final ElementMatcher<? super ClassLoader> matcher;

    /**
     * Creates a new class loader hierarchy matcher.
     *
     * @param matcher The matcher to apply on each class loader in the hierarchy.
     */
    public ClassLoaderHierarchyMatcher(ElementMatcher<? super ClassLoader> matcher) {
        this.matcher = matcher;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        ClassLoader current = target;
        while (current != null) {
            if (matcher.matches(current)) {
                return true;
            }
            current = current.getParent();
        }
        return matcher.matches(null);
    }

    @Override
    public String toString() {
        return "hasChild(" + matcher + ')';
    }
}

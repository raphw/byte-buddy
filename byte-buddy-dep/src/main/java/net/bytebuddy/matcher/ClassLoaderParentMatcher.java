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
 * An element matcher that matches a class loader for being a parent of the given class loader.
 *
 * @param <T> The exact type of the class loader that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ClassLoaderParentMatcher<T extends ClassLoader> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The class loader that is matched for being a child of the matched class loader.
     */
    private final ClassLoader classLoader;

    /**
     * Creates a class loader parent element matcher.
     *
     * @param classLoader The class loader that is matched for being a child of the matched class loader.
     */
    public ClassLoaderParentMatcher(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        ClassLoader current = classLoader;
        while (current != null) {
            if (current == target) {
                return true;
            }
            current = current.getParent();
        }
        return target == null;
    }

    @Override
    public String toString() {
        return "isParentOf(" + classLoader + ')';
    }
}

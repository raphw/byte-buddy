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
import net.bytebuddy.description.method.MethodDescription;

/**
 * Matches a method description by its general characteristics which are represented as a
 * {@link net.bytebuddy.matcher.MethodSortMatcher.Sort}.
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodSortMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * Returns an element matcher that matches a specific sort of method description.
     *
     * @param <T>  The type of the matched entity.
     * @param sort The sort of method description to be matched by this element matcher.
     * @return A matcher that matches methods of the provided sort.
     */
    @SuppressWarnings("unchecked")
    public static <T extends MethodDescription> ElementMatcher.Junction<T> of(Sort sort) {
        return (ElementMatcher.Junction<T>) sort.getMatcher();
    }

    /**
     * The sort of method description to be matched by this element matcher.
     */
    private final Sort sort;

    /**
     * Creates a new element matcher that matches a specific sort of method description.
     *
     * @param sort The sort of method description to be matched by this element matcher.
     */
    public MethodSortMatcher(Sort sort) {
        this.sort = sort;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        return sort.isSort(target);
    }

    @Override
    public String toString() {
        return sort.getDescription();
    }

    /**
     * Represents a specific characteristic of a method description.
     */
    public enum Sort {

        /**
         * Matches method descriptions that represent methods, not constructors or the type initializer.
         */
        METHOD("isMethod()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isMethod();
            }
        },

        /**
         * Matches method descriptions that represent constructors, not methods or the type initializer.
         */
        CONSTRUCTOR("isConstructor()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isConstructor();
            }
        },

        /**
         * Matches method descriptions that represent the type initializers.
         */
        TYPE_INITIALIZER("isTypeInitializer()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isTypeInitializer();
            }
        },

        /**
         * Matches method descriptions that are overridable.
         */
        VIRTUAL("isVirtual()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isVirtual();
            }
        },

        /**
         * Matches method descriptions that represent Java 8 default methods.
         */
        DEFAULT_METHOD("isDefaultMethod()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isDefaultMethod();
            }
        };

        /**
         * A textual representation of the method sort that is represented by this instance.
         */
        private final String description;

        /**
         * A reusable matcher for this sort.
         */
        private final MethodSortMatcher<?> matcher;

        /**
         * Creates a new method sort representation.
         *
         * @param description A textual representation of the method sort that is represented by this instance.
         */
        Sort(String description) {
            this.description = description;
            matcher = new MethodSortMatcher<MethodDescription>(this);
        }

        /**
         * Determines if a method description is of the represented method sort.
         *
         * @param target A textual representation of the method sort that is represented by this instance.
         * @return {@code true} if the given method if of the method sort that is represented by this instance.
         */
        protected abstract boolean isSort(MethodDescription target);

        /**
         * Returns a textual representation of this instance's method sort.
         *
         * @return A textual representation of this instance's method sort.
         */
        protected String getDescription() {
            return description;
        }

        /**
         * Returns the predefined matcher for this method sort.
         *
         * @return The predefined matcher for this method sort.
         */
        protected MethodSortMatcher<?> getMatcher() {
            return matcher;
        }
    }
}

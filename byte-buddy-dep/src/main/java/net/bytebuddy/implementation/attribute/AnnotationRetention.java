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
package net.bytebuddy.implementation.attribute;

/**
 * An annotation retention strategy decides if annotations that are contained within a class file are preserved upon redefining
 * or rebasing a method. When annotations are retained, it is important not to define annotations explicitly that are already
 * defined. When annotations are retained, they are retained in their original format, i.e. default values that were not included
 * in the class file are not added or skipped as determined by a {@link AnnotationValueFilter}.
 */
public enum AnnotationRetention {

    /**
     * Enables annotation retention, i.e. annotations within an existing class files are preserved as they are.
     */
    ENABLED(true),

    /**
     * Disables annotation retention, i.e. annotations within an existing class files are discarded.
     */
    DISABLED(false);

    /**
     * {@code true} if annotation retention is enabled.
     */
    private final boolean enabled;

    /**
     * Creates an annotation retention strategy.
     *
     * @param enabled {@code true} if annotation retention is enabled.
     */
    AnnotationRetention(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Resolves an annotation retention from a boolean value.
     *
     * @param enabled {@code true} if annotation retention is enabled.
     * @return An enabled annotation retention if the value is {@code true}.
     */
    public static AnnotationRetention of(boolean enabled) {
        return enabled
                ? ENABLED
                : DISABLED;
    }

    /**
     * Returns {@code true} if annotation retention is enabled.
     *
     * @return {@code true} if annotation retention is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}

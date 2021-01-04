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
package net.bytebuddy.description.annotation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Describes a declaration source for annotations.
 */
public interface AnnotationSource {

    /**
     * Returns a list of annotations that are declared by this instance.
     *
     * @return A list of declared annotations.
     */
    AnnotationList getDeclaredAnnotations();

    /**
     * An annotation source that does not declare any annotations.
     */
    enum Empty implements AnnotationSource {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }
    }

    /**
     * An annotation source that declares a given list of annotations.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Explicit implements AnnotationSource {

        /**
         * The represented annotations.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new explicit annotation source.
         *
         * @param annotation The represented annotations.
         */
        public Explicit(AnnotationDescription... annotation) {
            this(Arrays.asList(annotation));
        }

        /**
         * Creates a new explicit annotation source.
         *
         * @param annotations The represented annotations.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this.annotations = annotations;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(annotations);
        }
    }
}

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

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/**
 * An annotation value filter is responsible for determining which values should be skipped and rather be represented as an
 * annotation type's default values when writing an annotation to a class file.
 */
public interface AnnotationValueFilter {

    /**
     * Checks if the given annotation value should be written as the value of the provided annotation property.
     *
     * @param annotationDescription The annotation value that is being written.
     * @param methodDescription     The annotation method of which a value is being written.
     * @return {@code true} if the value should be written to the annotation.
     */
    boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription);

    /**
     * A factory for creating an annotation value filter for an annotation's target.
     */
    interface Factory {

        /**
         * Creates an annotation value filter for writing annotations on an instrumented type.
         *
         * @param instrumentedType The instrumented type onto which the annotations are written.
         * @return An annotation value filter to be used when writing annotations onto the given type.
         */
        AnnotationValueFilter on(TypeDescription instrumentedType);

        /**
         * Creates an annotation value filter for writing annotations on a field.
         *
         * @param fieldDescription The field onto which the annotations are written.
         * @return An annotation value filter to be used when writing annotations onto the given field.
         */
        AnnotationValueFilter on(FieldDescription fieldDescription);

        /**
         * Creates an annotation value filter for writing annotations on a method.
         *
         * @param methodDescription The method onto which the annotations are written.
         * @return An annotation value filter to be used when writing annotations onto the given method.
         */
        AnnotationValueFilter on(MethodDescription methodDescription);
    }

    /**
     * A default implementation of an annotation value filter that applies the same strategy for any type, field or method.
     */
    enum Default implements AnnotationValueFilter, Factory {

        /**
         * An annotation value filter where default values are skipped and not included in the class file.
         */
        SKIP_DEFAULTS {
            /** {@inheritDoc} */
            public boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription) {
                Object defaultValue = methodDescription.getDefaultValue();
                return defaultValue == null || !defaultValue.equals(annotationDescription.getValue(methodDescription));
            }
        },

        /**
         * An annotation value filter where default values are included in the class file.
         */
        APPEND_DEFAULTS {
            /** {@inheritDoc} */
            public boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription) {
                return true;
            }
        };

        /**
         * {@inheritDoc}
         */
        public AnnotationValueFilter on(TypeDescription instrumentedType) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValueFilter on(FieldDescription fieldDescription) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public AnnotationValueFilter on(MethodDescription methodDescription) {
            return this;
        }
    }
}

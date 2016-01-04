package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

public interface AnnotationValueFilter {

    /**
     * Checks if the given annotation value should be written as the value of the provided annotation property.
     *
     * @param annotationDescription The annotation value that is being written.
     * @param methodDescription     The annotation method of which a value is being written.
     * @return {@code true} if the value should be written to the annotation.
     */
    boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription);

    interface Factory {

        AnnotationValueFilter on(TypeDescription instrumentedType);

        AnnotationValueFilter on(FieldDescription fieldDescription);

        AnnotationValueFilter on(MethodDescription methodDescription);
    }

    enum Default implements AnnotationValueFilter, Factory {

        SKIP_DEFAULTS {
            @Override
            public boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription) {
                Object defaultValue = methodDescription.getDefaultValue();
                return defaultValue == null || !defaultValue.equals(annotationDescription.getValue(methodDescription));
            }
        },

        APPEND_DEFAULTS {
            @Override
            public boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription) {
                return true;
            }
        };

        @Override
        public AnnotationValueFilter on(TypeDescription instrumentedType) {
            return this;
        }

        @Override
        public AnnotationValueFilter on(FieldDescription fieldDescription) {
            return this;
        }

        @Override
        public AnnotationValueFilter on(MethodDescription methodDescription) {
            return this;
        }

        @Override
        public String toString() {
            return "AnnotationValueFilter.Default." + name();
        }
    }
}

package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.FieldVisitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.FieldVisitor}.
 */
public interface FieldAttributeAppender {

    /**
     * A factory that creates field attribute appenders for a given type.
     */
    static interface Factory {

        /**
         * A field attribute appender factory that combines several field attribute appender factories to be
         * represented as a single factory.
         */
        static class Compound implements Factory {

            private final Factory[] factory;

            /**
             * Creates a new compound field attribute appender factory.
             *
             * @param factory The factories that are to be combined by this compound factory in the order of their
             *                application.
             */
            public Compound(Factory... factory) {
                this.factory = factory;
            }

            @Override
            public FieldAttributeAppender make(TypeDescription typeDescription) {
                FieldAttributeAppender[] fieldAttributeAppender = new FieldAttributeAppender[factory.length];
                int index = 0;
                for (Factory factory : this.factory) {
                    fieldAttributeAppender[index++] = factory.make(typeDescription);
                }
                return new FieldAttributeAppender.Compound(fieldAttributeAppender);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Arrays.equals(factory, ((Compound) other).factory);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(factory);
            }

            @Override
            public String toString() {
                return "FieldAttributeAppender.Factory.Compound{" + Arrays.toString(factory) + '}';
            }
        }

        /**
         * Returns a field attribute appender that is applicable for a given type description.
         *
         * @param typeDescription The type for which a field attribute appender is to be applied for.
         * @return The field attribute appender which should be applied for the given type.
         */
        FieldAttributeAppender make(TypeDescription typeDescription);
    }

    /**
     * A field attribute appender that does not append any attributes.
     */
    static enum NoOp implements FieldAttributeAppender, Factory {
        INSTANCE;

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            /* do nothing */
        }
    }

    /**
     * Appends an annotation to a field. The visibility of the annotation is determined by the annotation type's
     * {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    static class ForAnnotation implements FieldAttributeAppender, Factory {

        private final Annotation annotation;

        /**
         * Creates a new field annotation appender.
         *
         * @param annotation The annotation to be appended.
         */
        public ForAnnotation(Annotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || !(o == null || getClass() != o.getClass())
                    && annotation.equals(((ForAnnotation) o).annotation);
        }

        @Override
        public int hashCode() {
            return annotation.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.ForAnnotation{annotation=" + annotation + '}';
        }
    }

    /**
     * Writes all annotations that are found on a field that belongs to a loaded type of the JVM as visible
     * annotations.
     */
    static class ForLoadedField implements FieldAttributeAppender, Factory {

        private final Field field;

        /**
         * Creates a new field attribute appender that appends all annotations that are found on a loaded field.
         *
         * @param field The field from which the annotations to append are read.
         */
        public ForLoadedField(Field field) {
            this.field = field;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            for (Annotation annotation : field.getAnnotations()) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.RUNTIME);
            }
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && field.equals(((ForLoadedField) other).field);
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.ForLoadedField{field=" + field + '}';
        }
    }

    /**
     * A field attribute appender that combines several method attribute appenders to be represented as a single
     * field attribute appender.
     */
    static class Compound implements FieldAttributeAppender {

        private final FieldAttributeAppender[] fieldAttributeAppender;

        /**
         * Creates a new compound field attribute appender.
         *
         * @param fieldAttributeAppender The field attribute appenders that are to be combined by this compound appender
         *                               in the order of their application.
         */
        public Compound(FieldAttributeAppender... fieldAttributeAppender) {
            this.fieldAttributeAppender = fieldAttributeAppender;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            for (FieldAttributeAppender fieldAttributeAppender : this.fieldAttributeAppender) {
                fieldAttributeAppender.apply(fieldVisitor, fieldDescription);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(fieldAttributeAppender, ((Compound) other).fieldAttributeAppender);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(fieldAttributeAppender);
        }

        @Override
        public String toString() {
            return "FieldAttributeAppenderCompound{" + Arrays.toString(fieldAttributeAppender) + '}';
        }
    }

    /**
     * Applies this attribute appender to a given field visitor.
     *
     * @param fieldVisitor     The field visitor to which the attributes that are represented by this attribute appender
     *                         are written to.
     * @param fieldDescription The description of the field to which the field visitor belongs to.
     */
    void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription);
}

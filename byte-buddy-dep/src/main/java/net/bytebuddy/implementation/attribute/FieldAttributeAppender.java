package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.FieldVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.FieldVisitor}.
 */
public interface FieldAttributeAppender {

    /**
     * Applies this attribute appender to a given field visitor.
     *
     * @param fieldVisitor          The field visitor to which the attributes that are represented by this attribute appender are written to.
     * @param fieldDescription      The description of the field to which the field visitor belongs to.
     * @param annotationValueFilter The annotation value filter to apply when writing annotations.
     */
    void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter);

    /**
     * A field attribute appender that does not append any attributes.
     */
    enum NoOp implements FieldAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            /* do nothing */
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.NoOp." + name();
        }
    }

    /**
     * A factory that creates field attribute appenders for a given type.
     */
    interface Factory {

        /**
         * Returns a field attribute appender that is applicable for a given type description.
         *
         * @param typeDescription The type for which a field attribute appender is to be applied for.
         * @return The field attribute appender which should be applied for the given type.
         */
        FieldAttributeAppender make(TypeDescription typeDescription);

        /**
         * A field attribute appender factory that combines several field attribute appender factories to be
         * represented as a single factory.
         */
        class Compound implements Factory {

            /**
             * The factories that this compound factory represents in their application order.
             */
            private final List<? extends Factory> factories;

            /**
             * Creates a new compound field attribute appender factory.
             *
             * @param factory The factories to represent in the order of their application.
             */
            public Compound(Factory... factory) {
                this(Arrays.asList(factory));
            }

            /**
             * Creates a new compound field attribute appender factory.
             *
             * @param factories The factories to represent in the order of their application.
             */
            public Compound(List<? extends Factory> factories) {
                this.factories = factories;
            }

            @Override
            public FieldAttributeAppender make(TypeDescription typeDescription) {
                List<FieldAttributeAppender> fieldAttributeAppenders = new ArrayList<FieldAttributeAppender>(factories.size());
                for (Factory factory : factories) {
                    fieldAttributeAppenders.add(factory.make(typeDescription));
                }
                return new FieldAttributeAppender.Compound(fieldAttributeAppenders);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && factories.equals(((Compound) other).factories);
            }

            @Override
            public int hashCode() {
                return factories.hashCode();
            }

            @Override
            public String toString() {
                return "FieldAttributeAppender.Factory.Compound{factories=" + factories + '}';
            }
        }
    }

    /**
     * An attribute appender that writes all annotations that are declared on a field.
     */
    enum ForInstrumentedField implements FieldAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            annotationAppender = fieldDescription.getType().accept(AnnotationAppender.ForTypeAnnotations.ofFieldType(annotationAppender, annotationValueFilter));
            for (AnnotationDescription annotation : fieldDescription.getDeclaredAnnotations()) {
                annotationAppender = annotationAppender.append(annotation, annotationValueFilter);
            }
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.ForInstrumentedField." + name();
        }
    }

    /**
     * Appends an annotation to a field. The visibility of the annotation is determined by the annotation type's
     * {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    class Explicit implements FieldAttributeAppender, Factory {

        /**
         * The annotations that this appender appends.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new annotation attribute appender for explicit annotation values. All values, including default values, are copied.
         *
         * @param annotations The annotations to be appended to the field.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this.annotations = annotations;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            for (AnnotationDescription annotation : annotations) {
                appender = appender.append(annotation, annotationValueFilter);
            }
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotations.equals(((Explicit) other).annotations);
        }

        @Override
        public int hashCode() {
            return annotations.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.Explicit{" +
                    "annotations=" + annotations +
                    '}';
        }
    }

    /**
     * A field attribute appender that combines several method attribute appenders to be represented as a single
     * field attribute appender.
     */
    class Compound implements FieldAttributeAppender {

        /**
         * The field attribute appenders this appender represents in their application order.
         */
        private final List<? extends FieldAttributeAppender> fieldAttributeAppenders;

        /**
         * Creates a new compound field attribute appender.
         *
         * @param fieldAttributeAppender The field attribute appenders that are to be combined by this compound appender
         *                               in the order of their application.
         */
        public Compound(FieldAttributeAppender... fieldAttributeAppender) {
            this(Arrays.asList(fieldAttributeAppender));
        }

        /**
         * Creates a new compound field attribute appender.
         *
         * @param fieldAttributeAppenders The field attribute appenders that are to be combined by this compound appender
         *                                in the order of their application.
         */
        public Compound(List<? extends FieldAttributeAppender> fieldAttributeAppenders) {
            this.fieldAttributeAppenders = fieldAttributeAppenders;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            for (FieldAttributeAppender fieldAttributeAppender : fieldAttributeAppenders) {
                fieldAttributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilter);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && fieldAttributeAppenders.equals(((Compound) other).fieldAttributeAppenders);
        }

        @Override
        public int hashCode() {
            return fieldAttributeAppenders.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.Compound{fieldAttributeAppenders=" + fieldAttributeAppenders + '}';
        }
    }
}

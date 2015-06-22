package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.FieldVisitor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.FieldVisitor}.
 */
public interface FieldAttributeAppender {

    /**
     * Applies this attribute appender to a given field visitor.
     *
     * @param fieldVisitor     The field visitor to which the attributes that are represented by this attribute appender
     *                         are written to.
     * @param fieldDescription The description of the field to which the field visitor belongs to.
     */
    void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription);

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
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
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
                return "FieldAttributeAppender.Factory.Compound{factory=" + Arrays.toString(factory) + '}';
            }
        }
    }

    /**
     * Appends an annotation to a field. The visibility of the annotation is determined by the annotation type's
     * {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    class ForAnnotation implements FieldAttributeAppender, Factory {

        /**
         * The annotations that this appender appends.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * A matcher to identify default properties.
         */
        private final ElementMatcher<? super MethodDescription> defaultProperties;

        /**
         * Creates a new field annotation appender.
         *
         * @param annotations The annotations to be appended to the field.
         */
        public ForAnnotation(List<? extends AnnotationDescription> annotations) {
            this(annotations, none());
        }

        /**
         * Creates a new annotation attribute appender for explicit annotation values. All values, including default values, are copied.
         *
         * @param annotations       The annotations to be appended to the field.
         * @param defaultProperties A matcher to identify default properties.
         */
        public ForAnnotation(List<? extends AnnotationDescription> annotations, ElementMatcher<? super MethodDescription> defaultProperties) {
            this.annotations = annotations;
            this.defaultProperties = defaultProperties;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            for (AnnotationDescription annotation : annotations) {
                annotationAppender.append(annotation, defaultProperties, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotations.equals(((ForAnnotation) other).annotations)
                    && defaultProperties.equals(((ForAnnotation) other).defaultProperties);
        }

        @Override
        public int hashCode() {
            return annotations.hashCode() + 31 * defaultProperties.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.ForAnnotation{" +
                    "annotations=" + annotations +
                    "defaultProperties=" + defaultProperties +
                    '}';
        }
    }

    /**
     * Writes all annotations that are found on a field that belongs to a loaded type of the JVM as visible
     * annotations.
     */
    class ForField implements FieldAttributeAppender, Factory {

        /**
         * The field from which the annotations should be copied.
         */
        private final FieldDescription fieldDescription;

        /**
         * A matcher to identify default properties.
         */
        private final ElementMatcher<? super MethodDescription> defaultProperties;

        /**
         * Creates a new field attribute appender that appends all annotations that are found on a field.
         *
         * @param field The field from which the annotations to append are read.
         */
        public ForField(Field field) {
            this(new FieldDescription.ForLoadedField(field));
        }

        /**
         * Creates a new field attribute appender that appends all annotations that are found on a field.
         *
         * @param fieldDescription The field from which the annotations to append are read.
         */
        public ForField(FieldDescription fieldDescription) {
            this(fieldDescription, none());
        }

        /**
         * Creates a new field attribute appender that appends all annotations that are found on a field.
         *
         * @param field             The field from which the annotations to append are read.
         * @param defaultProperties A matcher to identify default properties.
         */
        public ForField(Field field, ElementMatcher<? super MethodDescription> defaultProperties) {
            this(new FieldDescription.ForLoadedField(field), defaultProperties);
        }

        /**
         * Creates a new field attribute appender that appends all annotations that are found on a field.
         *
         * @param fieldDescription  The field from which the annotations to append are read.
         * @param defaultProperties A matcher to identify default properties.
         */
        public ForField(FieldDescription fieldDescription, ElementMatcher<? super MethodDescription> defaultProperties) {
            this.fieldDescription = fieldDescription;
            this.defaultProperties = defaultProperties;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            for (AnnotationDescription annotation : this.fieldDescription.getDeclaredAnnotations()) {
                annotationAppender.append(annotation, defaultProperties, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && fieldDescription.equals(((ForField) other).fieldDescription)
                    && defaultProperties.equals(((ForField) other).defaultProperties);
        }

        @Override
        public int hashCode() {
            return fieldDescription.hashCode() + 31 * defaultProperties.hashCode();
        }

        @Override
        public String toString() {
            return "FieldAttributeAppender.ForField{" +
                    "fieldDescription=" + fieldDescription +
                    ", defaultProperties=" + defaultProperties +
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
            return "FieldAttributeAppender.Compound{fieldAttributeAppender=" + Arrays.toString(fieldAttributeAppender) + '}';
        }
    }
}

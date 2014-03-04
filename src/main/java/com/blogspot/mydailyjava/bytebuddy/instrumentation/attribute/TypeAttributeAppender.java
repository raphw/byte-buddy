package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;

import java.lang.annotation.Annotation;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.ClassVisitor}.
 */
public interface TypeAttributeAppender {

    /**
     * A type attribute appender that does not append any attributes.
     */
    static enum NoOp implements TypeAttributeAppender {
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            /* do nothing */
        }
    }

    /**
     * An attribute appender that appends a single annotation to a given type. The visibility for the annotation
     * will be inferred from the annotation's {@link java.lang.annotation.RetentionPolicy}.
     */
    static class ForAnnotation implements TypeAttributeAppender {

        private final Annotation annotation;

        /**
         * Creates a new single annotation attribute appender.
         *
         * @param annotation The annotation to append.
         */
        public ForAnnotation(Annotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotation.equals(((ForAnnotation) other).annotation);
        }

        @Override
        public int hashCode() {
            return annotation.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForAnnotation{annotation=" + annotation + '}';
        }
    }

    /**
     * Writes all annotations that are found on a given loaded Java type as visible annotations to the target type.
     */
    static class ForLoadedType implements TypeAttributeAppender {

        private final Class<?> type;

        /**
         * Creates a new attribute appender that writes all annotations found on a given loaded type.
         *
         * @param type The loaded type
         */
        public ForLoadedType(Class<?> type) {
            this.type = type;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (Annotation annotation : type.getAnnotations()) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.RUNTIME);
            }
        }
    }

    /**
     * An attribute appender that writes all annotations that are found on a given target type to the
     * instrumented type this type attribute appender is applied onto. The visibility for the annotation
     * will be inferred from the annotations' {@link java.lang.annotation.RetentionPolicy}.
     */
    static enum ForSuperType implements TypeAttributeAppender {
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender =
                    new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (Annotation annotation : typeDescription.getSupertype().getAnnotations()) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }
    }

    /**
     * A compound type attribute appender that concatenates a number of other attribute appenders.
     */
    static class Compound implements TypeAttributeAppender {

        private final TypeAttributeAppender[] typeAttributeAppender;

        /**
         * Creates a new compound attribute appender.
         *
         * @param typeAttributeAppender The type attribute appenders to concatenate in the order of their application.
         */
        public Compound(TypeAttributeAppender... typeAttributeAppender) {
            this.typeAttributeAppender = typeAttributeAppender;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            for (TypeAttributeAppender typeAttributeAppender : this.typeAttributeAppender) {
                typeAttributeAppender.apply(classVisitor, typeDescription);
            }
        }
    }

    /**
     * Applies this type attribute appender.
     *
     * @param classVisitor    The class visitor to which the annotations of this visitor should be written to.
     * @param typeDescription A description of the instrumented type that is target of the ongoing instrumentation.
     */
    void apply(ClassVisitor classVisitor, TypeDescription typeDescription);
}

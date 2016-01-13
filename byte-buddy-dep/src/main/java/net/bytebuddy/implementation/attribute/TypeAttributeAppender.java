package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.ClassVisitor}.
 */
public interface TypeAttributeAppender {

    /**
     * Applies this type attribute appender.
     *
     * @param classVisitor          The class visitor to which the annotations of this visitor should be written to.
     * @param instrumentedType      A description of the instrumented type that is target of the ongoing instrumentation.
     * @param annotationValueFilter The annotation value filter to apply when writing annotations.
     */
    void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter);

    /**
     * A type attribute appender that does not append any attributes.
     */
    enum NoOp implements TypeAttributeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            /* do nothing */
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.NoOp." + name();
        }
    }

    /**
     * An attribute appender that writes all annotations that are found on a given target type to the
     * instrumented type this type attribute appender is applied onto. The visibility for the annotation
     * will be inferred from the annotations' {@link java.lang.annotation.RetentionPolicy}.
     */
    enum ForInstrumentedType implements TypeAttributeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            annotationAppender = AnnotationAppender.ForTypeAnnotations.ofTypeVariable(annotationAppender,
                    annotationValueFilter,
                    AnnotationAppender.ForTypeAnnotations.VARIABLE_ON_TYPE,
                    instrumentedType.getTypeVariables());
            TypeDescription.Generic superType = instrumentedType.getSuperClass();
            if (superType != null) {
                annotationAppender = superType.accept(AnnotationAppender.ForTypeAnnotations.ofSuperClass(annotationAppender, annotationValueFilter));
            }
            int interfaceIndex = 0;
            for (TypeDescription.Generic interfaceType : instrumentedType.getInterfaces()) {
                annotationAppender = interfaceType.accept(AnnotationAppender.ForTypeAnnotations.ofInterfaceType(annotationAppender,
                        annotationValueFilter,
                        interfaceIndex));
            }
            for (AnnotationDescription annotation : instrumentedType.getDeclaredAnnotations()) {
                annotationAppender = annotationAppender.append(annotation, annotationValueFilter);
            }
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForInstrumentedType." + name();
        }

        /**
         * A type attribute appender that writes all annotations of the instrumented types but those that are explicitly excluded.
         */
        public static class Excluding implements TypeAttributeAppender {

            /**
             * A set of the annotations that are excluded.
             */
            private final Set<? extends AnnotationDescription> excludedAnnotations;

            /**
             * Creates a new excluding type annotation appender.
             *
             * @param typeDescription A type whose annotations are excluded from writing.
             */
            public Excluding(TypeDescription typeDescription) {
                this(new HashSet<AnnotationDescription>(typeDescription.getDeclaredAnnotations()));
            }

            /**
             * Creates a new excluding type annotation appender.
             *
             * @param excludedAnnotations A set of the annotations that are excluded.
             */
            public Excluding(Set<? extends AnnotationDescription> excludedAnnotations) {
                this.excludedAnnotations = excludedAnnotations;
            }

            @Override
            public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
                AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
                for (AnnotationDescription annotation : instrumentedType.getDeclaredAnnotations()) {
                    if (!excludedAnnotations.contains(annotation)) {
                        appender = appender.append(annotation, annotationValueFilter);
                    }
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && excludedAnnotations.equals(((Excluding) other).excludedAnnotations);
            }

            @Override
            public int hashCode() {
                return excludedAnnotations.hashCode();
            }

            @Override
            public String toString() {
                return "TypeAttributeAppender.ForInstrumentedType.Excluding{" +
                        "excludedAnnotations=" + excludedAnnotations +
                        '}';
            }
        }
    }

    /**
     * An attribute appender that appends a single annotation to a given type. The visibility for the annotation
     * will be inferred from the annotation's {@link java.lang.annotation.RetentionPolicy}.
     */
    class Explicit implements TypeAttributeAppender {

        /**
         * The annotations to write to the given type.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new annotation attribute appender for explicit annotation values.
         *
         * @param annotations The annotations to write to the given type.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this.annotations = annotations;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (AnnotationDescription annotation : annotations) {
                appender = appender.append(annotation, annotationValueFilter);
            }
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
            return "TypeAttributeAppender.Explicit{" +
                    "annotations=" + annotations +
                    '}';
        }
    }

    /**
     * A compound type attribute appender that concatenates a number of other attribute appenders.
     */
    class Compound implements TypeAttributeAppender {

        /**
         * The type attribute appenders this compound appender represents in their application order.
         */
        private final List<? extends TypeAttributeAppender> typeAttributeAppenders;

        /**
         * Creates a new compound attribute appender.
         *
         * @param typeAttributeAppender The type attribute appenders to concatenate in the order of their application.
         */
        public Compound(TypeAttributeAppender... typeAttributeAppender) {
            this(Arrays.asList(typeAttributeAppender));
        }

        /**
         * Creates a new compound attribute appender.
         *
         * @param typeAttributeAppenders The type attribute appenders to concatenate in the order of their application.
         */
        public Compound(List<? extends TypeAttributeAppender> typeAttributeAppenders) {
            this.typeAttributeAppenders = typeAttributeAppenders;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            for (TypeAttributeAppender typeAttributeAppender : typeAttributeAppenders) {
                typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilter);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && typeAttributeAppenders.equals(((Compound) other).typeAttributeAppenders);
        }

        @Override
        public int hashCode() {
            return typeAttributeAppenders.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.Compound{typeAttributeAppenders=" + typeAttributeAppenders + '}';
        }
    }
}

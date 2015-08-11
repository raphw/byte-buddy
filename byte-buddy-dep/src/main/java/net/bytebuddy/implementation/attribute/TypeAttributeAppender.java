package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.objectweb.asm.ClassVisitor;

import java.util.Arrays;
import java.util.List;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.ClassVisitor}.
 */
public interface TypeAttributeAppender {

    /**
     * Applies this type attribute appender.
     *
     * @param classVisitor     The class visitor to which the annotations of this visitor should be written to.
     * @param instrumentedType A description of the instrumented type that is target of the ongoing instrumentation.
     * @param targetType       The target type of the instrumentation, i.e. the super class type for a super class creation
     *                         or the type being redefined.
     */
    void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, GenericTypeDescription targetType);

    /**
     * A type attribute appender that does not append any attributes.
     */
    enum NoOp implements TypeAttributeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, GenericTypeDescription targetType) {
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
    class ForInstrumentedType implements TypeAttributeAppender {

        /**
         * The value filter to apply for discovering which values of an annotation should be written.
         */
        private final AnnotationAppender.ValueFilter valueFilter;

        /**
         * Creates an attribute appender that copies the super type's annotations to the instrumented type.
         *
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForInstrumentedType(AnnotationAppender.ValueFilter valueFilter) {
            this.valueFilter = valueFilter;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, GenericTypeDescription targetType) {
            if (!instrumentedType.getSuperType().equals(targetType)) {
                return; // Takes into account that types can be renamed. This check is more reliable.
            }
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor), valueFilter);
            for (AnnotationDescription annotation : targetType.asErasure().getDeclaredAnnotations()) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass()) && valueFilter.equals(((ForInstrumentedType) other).valueFilter);
        }

        @Override
        public int hashCode() {
            return valueFilter.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForInstrumentedType{valueFilter=" + valueFilter + "}";
        }
    }

    /**
     * Writes all annotations that are declared for a given Java type to the target type.
     */
    class ForType implements TypeAttributeAppender {

        /**
         * The class of which the declared annotations are to be copied.
         */
        private final TypeDescription typeDescription;

        /**
         * The value filter to apply for discovering which values of an annotation should be written.
         */
        private final AnnotationAppender.ValueFilter valueFilter;

        /**
         * Creates a new attribute appender that writes all annotations declared for the given loaded type.
         *
         * @param type        The loaded type.
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForType(Class<?> type, AnnotationAppender.ValueFilter valueFilter) {
            this(new TypeDescription.ForLoadedType(type), valueFilter);
        }

        /**
         * Creates a new attribute appender that writes all annotations declared for the given type description.
         *
         * @param typeDescription The type description.
         * @param valueFilter     The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForType(TypeDescription typeDescription, AnnotationAppender.ValueFilter valueFilter) {
            this.typeDescription = typeDescription;
            this.valueFilter = valueFilter;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, GenericTypeDescription targetType) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor), valueFilter);
            for (AnnotationDescription annotation : this.typeDescription.getDeclaredAnnotations()) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && typeDescription.equals(((ForType) other).typeDescription)
                    && valueFilter.equals(((ForType) other).valueFilter);
        }

        @Override
        public int hashCode() {
            return typeDescription.hashCode() + 31 * valueFilter.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForType{" +
                    "typeDescription=" + typeDescription +
                    ", valueFilter=" + valueFilter +
                    '}';
        }
    }

    /**
     * An attribute appender that appends a single annotation to a given type. The visibility for the annotation
     * will be inferred from the annotation's {@link java.lang.annotation.RetentionPolicy}.
     */
    class ForAnnotation implements TypeAttributeAppender {

        /**
         * The annotations to write to the given type.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * The value filter to apply for discovering which values of an annotation should be written.
         */
        private final AnnotationAppender.ValueFilter valueFilter;

        /**
         * Creates a new annotation attribute appender for explicit annotation values.
         *
         * @param annotations The annotations to write to the given type.
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public ForAnnotation(List<? extends AnnotationDescription> annotations, AnnotationAppender.ValueFilter valueFilter) {
            this.annotations = annotations;
            this.valueFilter = valueFilter;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, GenericTypeDescription targetType) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor), valueFilter);
            for (AnnotationDescription annotation : annotations) {
                annotationAppender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation));
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotations.equals(((ForAnnotation) other).annotations)
                    && valueFilter.equals(((ForAnnotation) other).valueFilter);
        }

        @Override
        public int hashCode() {
            return annotations.hashCode() + 31 * valueFilter.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForAnnotation{" +
                    "annotations=" + annotations +
                    ", valueFilter=" + valueFilter +
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
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, GenericTypeDescription targetType) {
            for (TypeAttributeAppender typeAttributeAppender : this.typeAttributeAppender) {
                typeAttributeAppender.apply(classVisitor, instrumentedType, targetType);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && Arrays.equals(typeAttributeAppender, ((Compound) other).typeAttributeAppender);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(typeAttributeAppender);
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.Compound{typeAttributeAppender=" + Arrays.toString(typeAttributeAppender) + '}';
        }
    }
}

package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;

/**
 * Annotation appenders are capable of writing annotations to a specified target.
 */
public interface AnnotationAppender {

    /**
     * A constant for informing ASM over ignoring a given name.
     */
    String NO_NAME = null;

    /**
     * Terminally writes the given annotation to the specified target.
     *
     * @param annotation           The annotation to be written.
     * @param annotationVisibility Determines the annotation visibility for the given annotation.
     * @return Usually {@code this} or any other annotation appender capable of writing another annotation to
     * the specified target.
     */
    AnnotationAppender append(AnnotationDescription annotation, AnnotationVisibility annotationVisibility);

    /**
     * Determines if an annotation should be written to a specified target and if the annotation should be marked
     * as being visible at runtime.
     */
    enum AnnotationVisibility {

        /**
         * The annotation is preserved in the compiled class and visible at runtime.
         */
        RUNTIME(true, false),

        /**
         * The annotation is preserved in the compiled class but not visible at runtime.
         */
        CLASS_FILE(false, false),

        /**
         * The annotation is ignored.
         */
        INVISIBLE(false, true);

        /**
         * {@code true} if this annotation is visible at runtime.
         */
        private final boolean visible;

        /**
         * {@code true} if this annotation is added to a compiled class.
         */
        private final boolean suppressed;

        /**
         * Creates a new annotation visibility representation.
         *
         * @param visible    {@code true} if this annotation is visible at runtime.
         * @param suppressed {@code true} if this annotation is added to a compiled class.
         */
        AnnotationVisibility(boolean visible, boolean suppressed) {
            this.visible = visible;
            this.suppressed = suppressed;
        }

        /**
         * Finds the annotation visibility that is declared for a given annotation.
         *
         * @param annotation The annotation of interest.
         * @return The annotation visibility of a given annotation. Annotations with a non-defined visibility or an
         * visibility of type {@link java.lang.annotation.RetentionPolicy#SOURCE} will be silently ignored.
         */
        public static AnnotationVisibility of(AnnotationDescription annotation) {
            AnnotationDescription.Loadable<Retention> retention = annotation.getAnnotationType()
                    .getDeclaredAnnotations()
                    .ofType(Retention.class);
            if (retention == null || retention.loadSilent().value() == RetentionPolicy.SOURCE) {
                return INVISIBLE;
            } else if (retention.loadSilent().value() == RetentionPolicy.CLASS) {
                return CLASS_FILE;
            } else {
                return RUNTIME;
            }
        }

        /**
         * Checks if this instance represents an annotation that is visible at runtime, i.e. if this instance is
         * {@link AnnotationAppender.AnnotationVisibility#RUNTIME}.
         *
         * @return {@code true} if this instance represents an annotation to be visible at runtime.
         */
        public boolean isVisible() {
            return visible;
        }

        /**
         * Checks if this instance represents an annotation that is not to be embedded into Java byte code, i.e.
         * if this instance is
         * {@link AnnotationAppender.AnnotationVisibility#INVISIBLE}.
         *
         * @return {@code true} if this instance represents an annotation to be suppressed from the byte code output.
         */
        public boolean isSuppressed() {
            return suppressed;
        }

        @Override
        public String toString() {
            return "AnnotationAppender.AnnotationVisibility." + name();
        }
    }

    /**
     * Represents a target for an annotation writing process.
     */
    interface Target {

        /**
         * Creates an annotation visitor that is going to consume an annotation writing.
         *
         * @param annotationTypeDescriptor The type descriptor for the annotation to be written.
         * @param visible                  {@code true} if the annotation is to be visible at runtime.
         * @return An annotation visitor that is going to consume an annotation that is written to the latter
         * by the caller of this method.
         */
        AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible);

        /**
         * Target for an annotation that is written to a Java type.
         */
        class OnType implements Target {

            /**
             * The class visitor to write the annotation to.
             */
            private final ClassVisitor classVisitor;

            /**
             * Creates a new wrapper for a Java type.
             *
             * @param classVisitor The ASM class visitor to which the annotations are appended to.
             */
            public OnType(ClassVisitor classVisitor) {
                this.classVisitor = classVisitor;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return classVisitor.visitAnnotation(annotationTypeDescriptor, visible);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && classVisitor.equals(((OnType) other).classVisitor);
            }

            @Override
            public int hashCode() {
                return classVisitor.hashCode();
            }

            @Override
            public String toString() {
                return "AnnotationAppender.Target.OnType{classVisitor=" + classVisitor + '}';
            }
        }

        /**
         * Target for an annotation that is written to a Java method or constructor.
         */
        class OnMethod implements Target {

            /**
             * The method visitor to write the annotation to.
             */
            private final MethodVisitor methodVisitor;

            /**
             * Creates a new wrapper for a Java method or constructor.
             *
             * @param methodVisitor The ASM method visitor to which the annotations are appended to.
             */
            public OnMethod(MethodVisitor methodVisitor) {
                this.methodVisitor = methodVisitor;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return methodVisitor.visitAnnotation(annotationTypeDescriptor, visible);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodVisitor.equals(((OnMethod) other).methodVisitor);
            }

            @Override
            public int hashCode() {
                return methodVisitor.hashCode();
            }

            @Override
            public String toString() {
                return "AnnotationAppender.Target.OnMethod{methodVisitor=" + methodVisitor + '}';
            }
        }

        /**
         * Target for an annotation that is written to a Java method or constructor parameter.
         */
        class OnMethodParameter implements Target {

            /**
             * The method visitor to write the annotation to.
             */
            private final MethodVisitor methodVisitor;

            /**
             * The method parameter index to write the annotation to.
             */
            private final int parameterIndex;

            /**
             * Creates a new wrapper for a Java method or constructor.
             *
             * @param methodVisitor  The ASM method visitor to which the annotations are appended to.
             * @param parameterIndex The index of the method parameter.
             */
            public OnMethodParameter(MethodVisitor methodVisitor, int parameterIndex) {
                this.methodVisitor = methodVisitor;
                this.parameterIndex = parameterIndex;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return methodVisitor.visitParameterAnnotation(parameterIndex, annotationTypeDescriptor, visible);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && parameterIndex == ((OnMethodParameter) other).parameterIndex
                        && methodVisitor.equals(((OnMethodParameter) other).methodVisitor);
            }

            @Override
            public int hashCode() {
                return methodVisitor.hashCode() + 31 * parameterIndex;
            }

            @Override
            public String toString() {
                return "AnnotationAppender.Target.OnMethodParameter{" +
                        "methodVisitor=" + methodVisitor +
                        ", parameterIndex=" + parameterIndex +
                        '}';
            }
        }

        /**
         * Target for an annotation that is written to a Java field.
         */
        class OnField implements Target {

            /**
             * The field visitor to write the annotation to.
             */
            private final FieldVisitor fieldVisitor;

            /**
             * Creates a new wrapper for a Java field.
             *
             * @param fieldVisitor The ASM field visitor to which the annotations are appended to.
             */
            public OnField(FieldVisitor fieldVisitor) {
                this.fieldVisitor = fieldVisitor;
            }

            @Override
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible) {
                return fieldVisitor.visitAnnotation(annotationTypeDescriptor, visible);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && fieldVisitor.equals(((OnField) other).fieldVisitor);
            }

            @Override
            public int hashCode() {
                return fieldVisitor.hashCode();
            }

            @Override
            public String toString() {
                return "AnnotationAppender.Target.OnField{" +
                        "fieldVisitor=" + fieldVisitor +
                        '}';
            }
        }
    }

    /**
     * A value filter allows to skip values while writing an annotation value such that these missing values are represented
     * by the annotation type's default value instead.
     */
    interface ValueFilter {

        /**
         * Checks if the given annotation value should be written as the value of the provided annotation property.
         *
         * @param annotationDescription The annotation value that is being written.
         * @param methodDescription     The annotation method of which a value is being written.
         * @return {@code true} if the value should be written to the annotation.
         */
        boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription);

        /**
         * A value filter that skips all annotation values that represent the default value of the annotation. Note that it is not possible
         * at runtime to determine if a value is actually representing the default value of an annotation or only has the same value as the
         * default value. Therefore, this value filter skips any value that equals an annotation property's default value even if it was
         * explicitly defined.
         */
        enum SkipDefaults implements ValueFilter {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription) {
                Object defaultValue = methodDescription.getDefaultValue();
                return defaultValue != null && defaultValue.equals(annotationDescription.getValue(methodDescription));
            }

            @Override
            public String toString() {
                return "AnnotationAppender.ValueFilter.SkipDefaults." + name();
            }
        }

        /**
         * A value filter that does not skip any values.
         */
        enum AppendDefaults implements ValueFilter {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isRelevant(AnnotationDescription annotationDescription, MethodDescription.InDefinedShape methodDescription) {
                return true;
            }

            @Override
            public String toString() {
                return "AnnotationAppender.ValueFilter.AppendDefaults." + name();
            }
        }
    }

    /**
     * A default implementation for an annotation appender that writes annotations to a given byte consumer
     * represented by an ASM {@link org.objectweb.asm.AnnotationVisitor}.
     */
    class Default implements AnnotationAppender {

        /**
         * The target onto which an annotation write process is to be applied.
         */
        private final Target target;

        /**
         * The value filter to apply for discovering which values of an annotation should be written.
         */
        private final ValueFilter valueFilter;

        /**
         * Creates a default annotation appender.
         *
         * @param target      The target to which annotations are written to.
         * @param valueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        public Default(Target target, ValueFilter valueFilter) {
            this.target = target;
            this.valueFilter = valueFilter;
        }

        /**
         * Handles the writing of a single annotation to an annotation visitor.
         *
         * @param annotationVisitor The annotation visitor the write process is to be applied on.
         * @param annotation        The annotation to be written.
         * @param valueFilter       The value filter to apply for discovering which values of an annotation should be written.
         */
        private static void handle(AnnotationVisitor annotationVisitor, AnnotationDescription annotation, ValueFilter valueFilter) {
            for (MethodDescription.InDefinedShape methodDescription : annotation.getAnnotationType().getDeclaredMethods()) {
                if (valueFilter.isRelevant(annotation, methodDescription)) {
                    apply(annotationVisitor, methodDescription.getReturnType().asErasure(), methodDescription.getName(), annotation.getValue(methodDescription));
                }
            }
            annotationVisitor.visitEnd();
        }

        /**
         * Performs the writing of a given annotation value to an annotation visitor.
         *
         * @param annotationVisitor The annotation visitor the write process is to be applied on.
         * @param valueType         The type of the annotation value.
         * @param name              The name of the annotation type.
         * @param value             The annotation's value.
         */
        public static void apply(AnnotationVisitor annotationVisitor, TypeDescription valueType, String name, Object value) {
            if (valueType.isAnnotation()) {
                handle(annotationVisitor.visitAnnotation(name, valueType.getDescriptor()), (AnnotationDescription) value, ValueFilter.AppendDefaults.INSTANCE);
            } else if (valueType.isEnum()) {
                annotationVisitor.visitEnum(name, valueType.getDescriptor(), ((EnumerationDescription) value).getValue());
            } else if (valueType.isAssignableFrom(Class.class)) {
                annotationVisitor.visit(name, Type.getType(((TypeDescription) value).getDescriptor()));
            } else if (valueType.isArray()) {
                AnnotationVisitor arrayVisitor = annotationVisitor.visitArray(name);
                int length = Array.getLength(value);
                TypeDescription componentType = valueType.getComponentType();
                for (int index = 0; index < length; index++) {
                    apply(arrayVisitor, componentType, NO_NAME, Array.get(value, index));
                }
                arrayVisitor.visitEnd();
            } else {
                annotationVisitor.visit(name, value);
            }
        }

        @Override
        public AnnotationAppender append(AnnotationDescription annotation, AnnotationVisibility annotationVisibility) {
            if (!annotationVisibility.isSuppressed()) {
                doAppend(annotation, annotationVisibility.isVisible());
            }
            return this;
        }

        /**
         * Tries to append a given annotation by reflectively reading an annotation.
         *
         * @param annotation The annotation to be written.
         * @param visible    {@code true} if this annotation should be treated as visible at runtime.
         */
        private void doAppend(AnnotationDescription annotation, boolean visible) {
            handle(target.visit(annotation.getAnnotationType().getDescriptor(), visible), annotation, valueFilter);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && target.equals(((Default) other).target)
                    && valueFilter.equals(((Default) other).valueFilter);
        }

        @Override
        public int hashCode() {
            return target.hashCode() + 31 * valueFilter.hashCode();
        }

        @Override
        public String toString() {
            return "AnnotationAppender.Default{target=" + target + ", valueFilter=" + valueFilter + '}';
        }
    }
}

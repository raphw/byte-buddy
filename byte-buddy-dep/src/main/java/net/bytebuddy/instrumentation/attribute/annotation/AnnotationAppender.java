package net.bytebuddy.instrumentation.attribute.annotation;

import org.objectweb.asm.*;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Annotation appenders are capable of writing annotations to a specified target.
 */
public interface AnnotationAppender {

    /**
     * Terminally writes the given annotation to the specified target.
     *
     * @param annotation           The annotation to be written.
     * @param annotationVisibility Determines the annotation visibility for the given annotation.
     * @return Usually {@code this} or any other annotation appender capable of writing another annotation to
     * the specified target.
     */
    AnnotationAppender append(Annotation annotation, AnnotationVisibility annotationVisibility);

    /**
     * Determines if an annotation should be written to a specified target and if the annotation should be marked
     * as being visible at runtime.
     */
    static enum AnnotationVisibility {

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

        private final boolean visible;
        private final boolean suppressed;

        private AnnotationVisibility(boolean visible, boolean suppressed) {
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
        public static AnnotationVisibility of(Annotation annotation) {
            Retention retention = annotation.annotationType().getAnnotation(Retention.class);
            if (retention == null || retention.value() == RetentionPolicy.SOURCE) {
                return INVISIBLE;
            } else if (retention.value() == RetentionPolicy.CLASS) {
                return CLASS_FILE;
            } else {
                return RUNTIME;
            }
        }

        /**
         * Checks if this instance represents an annotation that is visible at runtime, i.e. if this instance is
         * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender.AnnotationVisibility#RUNTIME}.
         *
         * @return {@code true} if this instance represents an annotation to be visible at runtime.
         */
        public boolean isVisible() {
            return visible;
        }

        /**
         * Checks if this instance represents an annotation that is not to be embedded into Java byte code, i.e.
         * if this instance is
         * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender.AnnotationVisibility#INVISIBLE}.
         *
         * @return {@code true} if this instance represents an annotation to be suppressed from the byte code output.
         */
        public boolean isSuppressed() {
            return suppressed;
        }
    }

    /**
     * Represents a target for an annotation writing process.
     */
    static interface Target {

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
        static class OnType implements Target {

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
            public String toString() {
                return "OnType{" +
                        "classVisitor=" + classVisitor +
                        '}';
            }
        }

        /**
         * Target for an annotation that is written to a Java method or constructor.
         */
        static class OnMethod implements Target {

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
            public String toString() {
                return "OnMethod{" +
                        "methodVisitor=" + methodVisitor +
                        '}';
            }
        }

        /**
         * Target for an annotation that is written to a Java method or constructor parameter.
         */
        static class OnMethodParameter implements Target {

            private final MethodVisitor methodVisitor;
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
            public String toString() {
                return "OnMethodParameter{" +
                        "methodVisitor=" + methodVisitor +
                        ", parameterIndex=" + parameterIndex +
                        '}';
            }
        }

        /**
         * Target for an annotation that is written to a Java field.
         */
        static class OnField implements Target {

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
            public String toString() {
                return "OnField{" +
                        "fieldVisitor=" + fieldVisitor +
                        '}';
            }
        }
    }

    /**
     * A default implementation for an annotation appender that writes annotations to a given byte consumer
     * represented by an ASM {@link org.objectweb.asm.AnnotationVisitor}.
     */
    static class Default implements AnnotationAppender {

        private static final String ASM_IGNORE_NAME = null;

        private final Target target;

        /**
         * Creates a default annotation appender.
         *
         * @param target The target to which annotations are written to.
         */
        public Default(Target target) {
            this.target = target;
        }

        @Override
        public AnnotationAppender append(Annotation annotation, AnnotationVisibility annotationVisibility) {
            if (!annotationVisibility.isSuppressed()) {
                try {
                    tryAppend(annotation, annotationVisibility.isVisible());
                } catch (InvocationTargetException e) {
                    throw new IllegalStateException("Could not invoke method of " + annotation + " reflectively", e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Could not access method of " + annotation, e);
                }
            }
            return this;
        }

        private void tryAppend(Annotation annotation, boolean visible)
                throws InvocationTargetException, IllegalAccessException {
            handle(target.visit(Type.getDescriptor(annotation.annotationType()), visible), annotation);
        }

        private void handle(AnnotationVisitor annotationVisitor, Annotation annotation)
                throws InvocationTargetException, IllegalAccessException {
            for (Method method : annotation.annotationType().getDeclaredMethods()) {
                apply(annotationVisitor, method.getReturnType(), method.getName(), method.invoke(annotation));
            }
            annotationVisitor.visitEnd();
        }

        private void apply(AnnotationVisitor annotationVisitor, Class<?> valueType, String name, Object value)
                throws InvocationTargetException, IllegalAccessException {
            if (valueType.isAnnotation()) {
                handle(annotationVisitor.visitAnnotation(name, Type.getDescriptor(valueType)), (Annotation) value);
            } else if (valueType.isEnum()) {
                annotationVisitor.visitEnum(name, Type.getDescriptor(valueType), ((Enum<?>) value).name());
            } else if (valueType.isArray()) {
                AnnotationVisitor arrayVisitor = annotationVisitor.visitArray(name);
                int length = Array.getLength(value);
                Class<?> componentType = valueType.getComponentType();
                for (int index = 0; index < length; index++) {
                    apply(arrayVisitor, componentType, ASM_IGNORE_NAME, Array.get(value, index));
                }
                arrayVisitor.visitEnd();
            } else if (valueType == Class.class) {
                annotationVisitor.visit(name, Type.getType((Class<?>) value));
            } else {
                annotationVisitor.visit(name, value);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && target.equals(((Default) other).target);
        }

        @Override
        public int hashCode() {
            return target.hashCode();
        }

        @Override
        public String toString() {
            return "AnnotationAppender.Default{target=" + target + '}';
        }
    }
}

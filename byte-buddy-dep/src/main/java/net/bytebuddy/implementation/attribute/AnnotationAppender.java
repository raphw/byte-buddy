package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.*;

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
     * @param annotationDescription The annotation to be written.
     * @param annotationValueFilter The annotation value filter to use.
     * @return Usually {@code this} or any other annotation appender capable of writing another annotation to the specified target.
     */
    AnnotationAppender append(AnnotationDescription annotationDescription, AnnotationValueFilter annotationValueFilter);

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
     * A default implementation for an annotation appender that writes annotations to a given byte consumer
     * represented by an ASM {@link org.objectweb.asm.AnnotationVisitor}.
     */
    class Default implements AnnotationAppender {

        /**
         * The target onto which an annotation write process is to be applied.
         */
        private final Target target;

        /**
         * Creates a default annotation appender.
         *
         * @param target The target to which annotations are written to.
         */
        public Default(Target target) {
            this.target = target;
        }

        /**
         * Handles the writing of a single annotation to an annotation visitor.
         *
         * @param annotationVisitor     The annotation visitor the write process is to be applied on.
         * @param annotation            The annotation to be written.
         * @param annotationValueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        private static void handle(AnnotationVisitor annotationVisitor, AnnotationDescription annotation, AnnotationValueFilter annotationValueFilter) {
            for (MethodDescription.InDefinedShape methodDescription : annotation.getAnnotationType().getDeclaredMethods()) {
                if (annotationValueFilter.isRelevant(annotation, methodDescription)) {
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
                handle(annotationVisitor.visitAnnotation(name, valueType.getDescriptor()), (AnnotationDescription) value, AnnotationValueFilter.Default.APPEND_DEFAULTS);
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
        public AnnotationAppender append(AnnotationDescription annotationDescription, AnnotationValueFilter annotationValueFilter) {
            switch (annotationDescription.getRetention()) {
                case RUNTIME:
                    doAppend(annotationDescription, true, annotationValueFilter);
                    break;
                case CLASS:
                    doAppend(annotationDescription, false, annotationValueFilter);
                    break;
                case SOURCE:
                    break;
                default:
                    throw new IllegalStateException("Unexpected retention policy: " + annotationDescription.getRetention());
            }
            return this;
        }

        /**
         * Tries to append a given annotation by reflectively reading an annotation.
         *
         * @param annotation            The annotation to be written.
         * @param visible               {@code true} if this annotation should be treated as visible at runtime.
         * @param annotationValueFilter The annotation value filter to apply.
         */
        private void doAppend(AnnotationDescription annotation, boolean visible, AnnotationValueFilter annotationValueFilter) {
            handle(target.visit(annotation.getAnnotationType().getDescriptor(), visible), annotation, annotationValueFilter);
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

    class ForTypeAnnotations implements TypeDescription.Generic.Visitor<AnnotationAppender> {

        private final AnnotationAppender annotationAppender;

        private final AnnotationValueFilter annotationValueFilter;

        private final int typeReference;

        private final String typePath;

        protected ForTypeAnnotations(AnnotationAppender annotationAppender,
                                     AnnotationValueFilter annotationValueFilter,
                                     int typeReference,
                                     String typePath) {
            this.annotationAppender = annotationAppender;
            this.annotationValueFilter = annotationValueFilter;
            this.typeReference = typeReference;
            this.typePath = typePath;
        }

        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofFieldType(AnnotationAppender annotationAppender,
                                                                                      AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.FIELD, "");
        }

        @Override
        public AnnotationAppender onGenericArray(TypeDescription.Generic genericArray) {
            return null;
        }

        @Override
        public AnnotationAppender onWildcard(TypeDescription.Generic wildcard) {
            return null;
        }

        @Override
        public AnnotationAppender onParameterizedType(TypeDescription.Generic parameterizedType) {
            return null;
        }

        @Override
        public AnnotationAppender onTypeVariable(TypeDescription.Generic typeVariable) {
            return null;
        }

        @Override
        public AnnotationAppender onNonGenericType(TypeDescription.Generic typeDescription) {
            return null;
        }

        private AnnotationAppender apply(AnnotationAppender annotationAppender, TypeDescription.Generic typeDescription) {
            for (AnnotationDescription annotationDescription : typeDescription.getDeclaredAnnotations()) {
                //annotationAppender = annotationAppender.append(annotationDescription, AnnotationVisibility.of(annotationDescription), annotationValueFilter);
            }
            return annotationAppender;
        }
    }
}

package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.objectweb.asm.*;

import java.lang.reflect.Array;
import java.util.List;

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

    AnnotationAppender append(AnnotationDescription annotationDescription, AnnotationValueFilter annotationValueFilter, int typeReference, String typePath);

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

        AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible, int typeReference, String typePath);

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
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible, int typeReference, String typePath) {
                return classVisitor.visitTypeAnnotation(typeReference, TypePath.fromString(typePath), annotationTypeDescriptor, visible);
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
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible, int typeReference, String typePath) {
                return methodVisitor.visitTypeAnnotation(typeReference, TypePath.fromString(typePath), annotationTypeDescriptor, visible);
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
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible, int typeReference, String typePath) {
                return methodVisitor.visitTypeAnnotation(typeReference, TypePath.fromString(typePath), annotationTypeDescriptor, visible);
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
            public AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible, int typeReference, String typePath) {
                return fieldVisitor.visitTypeAnnotation(typeReference, TypePath.fromString(typePath), annotationTypeDescriptor, visible);
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
        public AnnotationAppender append(AnnotationDescription annotationDescription, AnnotationValueFilter annotationValueFilter, int typeReference, String typePath) {
            switch (annotationDescription.getRetention()) {
                case RUNTIME:
                    doAppend(annotationDescription, true, annotationValueFilter, typeReference, typePath);
                    break;
                case CLASS:
                    doAppend(annotationDescription, false, annotationValueFilter, typeReference, typePath);
                    break;
                case SOURCE:
                    break;
                default:
                    throw new IllegalStateException("Unexpected retention policy: " + annotationDescription.getRetention());
            }
            return this;
        }

        private void doAppend(AnnotationDescription annotation,
                              boolean visible,
                              AnnotationValueFilter annotationValueFilter,
                              int typeReference,
                              String typePath) {
            handle(target.visit(annotation.getAnnotationType().getDescriptor(), visible, typeReference, typePath), annotation, annotationValueFilter);
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

        public static final boolean VARIABLE_ON_TYPE = true;

        public static final boolean VARIABLE_ON_INVOKEABLE = false;

        /**
         * Represents an empty type path.
         */
        private static final String EMPTY_TYPE_PATH = "";

        /**
         * Represents a step to a component type within a type path.
         */
        private static final char COMPONENT_TYPE_PATH = '[';

        /**
         * Represents a wildcard type step within a type path.
         */
        private static final char WILDCARD_TYPE_PATH = '*';

        /**
         * Represents a owner type step within a type path.
         */
        private static final char OWNER_TYPE_PATH = '.';

        /**
         * Represents an index tzpe delimiter within a type path.
         */
        private static final char INDEXED_TYPE_DELIMITER = ';';

        private static final int SUPER_CLASS_INDEX = -1;

        private final AnnotationAppender annotationAppender;

        private final AnnotationValueFilter annotationValueFilter;

        private final int typeReference;

        private final String typePath;

        protected ForTypeAnnotations(AnnotationAppender annotationAppender, AnnotationValueFilter annotationValueFilter, TypeReference typeReference) {
            this(annotationAppender, annotationValueFilter, typeReference.getValue(), EMPTY_TYPE_PATH);
        }

        protected ForTypeAnnotations(AnnotationAppender annotationAppender, AnnotationValueFilter annotationValueFilter, int typeReference, String typePath) {
            this.annotationAppender = annotationAppender;
            this.annotationValueFilter = annotationValueFilter;
            this.typeReference = typeReference;
            this.typePath = typePath;
        }

        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofSuperClass(AnnotationAppender annotationAppender,
                                                                                       AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newSuperTypeReference(SUPER_CLASS_INDEX));
        }

        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofInterfaceType(AnnotationAppender annotationAppender,
                                                                                          AnnotationValueFilter annotationValueFilter,
                                                                                          int index) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newSuperTypeReference(index));
        }

        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofFieldType(AnnotationAppender annotationAppender,
                                                                                      AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newTypeReference(TypeReference.FIELD));
        }

        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofMethodReturnType(AnnotationAppender annotationAppender,
                                                                                             AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newTypeReference(TypeReference.METHOD_RETURN));
        }

        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofMethodParameterType(AnnotationAppender annotationAppender,
                                                                                                AnnotationValueFilter annotationValueFilter,
                                                                                                int index) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newFormalParameterReference(index));
        }

        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofExceptionType(AnnotationAppender annotationAppender,
                                                                                          AnnotationValueFilter annotationValueFilter,
                                                                                          int index) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newExceptionReference(index));
        }

        public static AnnotationAppender ofTypeVariable(AnnotationAppender annotationAppender,
                                                        AnnotationValueFilter annotationValueFilter,
                                                        boolean variableOnType,
                                                        List<? extends TypeDescription.Generic> typeVariables) {
            return ofTypeVariable(annotationAppender, annotationValueFilter, variableOnType, 0, typeVariables);
        }
        public static AnnotationAppender ofTypeVariable(AnnotationAppender annotationAppender,
                                                        AnnotationValueFilter annotationValueFilter,
                                                        boolean variableOnType,
                                                        int subListIndex,
                                                        List<? extends TypeDescription.Generic> typeVariables) {
            int typeVariableIndex = subListIndex, variableBaseReference, variableBoundBaseBase;
            if (variableOnType) {
                variableBaseReference = TypeReference.CLASS_TYPE_PARAMETER;
                variableBoundBaseBase = TypeReference.CLASS_TYPE_PARAMETER_BOUND;
            } else {
                variableBaseReference = TypeReference.METHOD_TYPE_PARAMETER;
                variableBoundBaseBase = TypeReference.METHOD_TYPE_PARAMETER_BOUND;
            }
            for (TypeDescription.Generic typeVariable : typeVariables.subList(subListIndex, typeVariables.size())) {
                int typeReference = TypeReference.newTypeParameterReference(variableBaseReference, typeVariableIndex).getValue();
                for (AnnotationDescription annotationDescription : typeVariable.getDeclaredAnnotations()) {
                    annotationAppender = annotationAppender.append(annotationDescription, annotationValueFilter, typeReference, EMPTY_TYPE_PATH);
                }
                int boundIndex = !typeVariable.getUpperBounds().get(0).getSort().isTypeVariable() && typeVariable.getUpperBounds().get(0).asErasure().isInterface()
                        ? 1
                        : 0;
                for (TypeDescription.Generic typeBound : typeVariable.getUpperBounds()) {
                    annotationAppender = typeBound.accept(new ForTypeAnnotations(annotationAppender,
                            annotationValueFilter,
                            TypeReference.newTypeParameterBoundReference(variableBoundBaseBase, typeVariableIndex, boundIndex++)));
                }
                typeVariableIndex++;
            }
            return annotationAppender;
        }

        @Override
        public AnnotationAppender onGenericArray(TypeDescription.Generic genericArray) {
            return genericArray.getComponentType().accept(new ForTypeAnnotations(apply(genericArray),
                    annotationValueFilter,
                    typeReference,
                    typePath + COMPONENT_TYPE_PATH));
        }

        @Override
        public AnnotationAppender onWildcard(TypeDescription.Generic wildcard) {
            TypeList.Generic lowerBounds = wildcard.getLowerBounds();
            return (lowerBounds.isEmpty()
                    ? wildcard.getUpperBounds().getOnly()
                    : lowerBounds.getOnly()).accept(new ForTypeAnnotations(apply(wildcard), annotationValueFilter, typeReference, typePath + WILDCARD_TYPE_PATH));
        }

        @Override
        public AnnotationAppender onParameterizedType(TypeDescription.Generic parameterizedType) {
            AnnotationAppender annotationAppender = apply(parameterizedType);
            TypeDescription.Generic ownerType = parameterizedType.getOwnerType();
            if (ownerType != null) {
                annotationAppender = ownerType.accept(new ForTypeAnnotations(annotationAppender,
                        annotationValueFilter,
                        typeReference,
                        typePath + OWNER_TYPE_PATH));
            }
            int index = 0;
            for (TypeDescription.Generic typeArgument : parameterizedType.getTypeArguments()) {
                annotationAppender = typeArgument.accept(new ForTypeAnnotations(annotationAppender,
                        annotationValueFilter,
                        typeReference,
                        typePath + index++ + INDEXED_TYPE_DELIMITER));
            }
            return annotationAppender;
        }

        @Override
        public AnnotationAppender onTypeVariable(TypeDescription.Generic typeVariable) {
            return apply(typeVariable);
        }

        @Override
        public AnnotationAppender onNonGenericType(TypeDescription.Generic typeDescription) {
            AnnotationAppender annotationAppender = apply(typeDescription);
            if (typeDescription.isArray()) {
                annotationAppender = typeDescription.getComponentType().accept(new ForTypeAnnotations(annotationAppender,
                        annotationValueFilter,
                        typeReference,
                        typePath + COMPONENT_TYPE_PATH));
            }
            return annotationAppender;
        }

        private AnnotationAppender apply(TypeDescription.Generic typeDescription) {
            AnnotationAppender annotationAppender = this.annotationAppender;
            for (AnnotationDescription annotationDescription : typeDescription.getDeclaredAnnotations()) {
                annotationAppender = annotationAppender.append(annotationDescription, annotationValueFilter, typeReference, typePath);
            }
            return annotationAppender;
        }
    }
}

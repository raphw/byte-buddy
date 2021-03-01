package net.bytebuddy.implementation.attribute;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.objectweb.asm.*;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Annotation appenders are capable of writing annotations to a specified target. 注释附加器能够将注释写入指定的目标  代表了字节码的属性
 */
public interface AnnotationAppender {

    /**
     * A constant for informing ASM over ignoring a given name. 用于通知ASM忽略给定名称的常量
     */
    String NO_NAME = null;

    /**
     * Writes the given annotation to the target that this appender represents. 将给定的注释写入此追加程序代表的目标
     *
     * @param annotationDescription The annotation to be written.
     * @param annotationValueFilter The annotation value filter to use.
     * @return Usually {@code this} or any other annotation appender capable of writing another annotation to the specified target.
     */
    AnnotationAppender append(AnnotationDescription annotationDescription, AnnotationValueFilter annotationValueFilter);

    /**
     * Writes the given type annotation to the target that this appender represents.
     *
     * @param annotationDescription The annotation to be written.
     * @param annotationValueFilter The annotation value filter to use.
     * @param typeReference         The type variable's type reference.
     * @param typePath              The type variable's type path.
     * @return Usually {@code this} or any other annotation appender capable of writing another annotation to the specified target.
     */
    AnnotationAppender append(AnnotationDescription annotationDescription, AnnotationValueFilter annotationValueFilter, int typeReference, String typePath);

    /**
     * Represents a target for an annotation writing process. 表示注解编写过程的目标
     */
    interface Target {

        /**
         * Creates an annotation visitor for writing the specified annotation.
         *
         * @param annotationTypeDescriptor The type descriptor for the annotation to be written.
         * @param visible                  {@code true} if the annotation is to be visible at runtime.
         * @return An annotation visitor for consuming the specified annotation.
         */
        AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible);

        /**
         * Creates an annotation visitor for writing the specified type annotation.
         *
         * @param annotationTypeDescriptor The type descriptor for the annotation to be written.
         * @param visible                  {@code true} if the annotation is to be visible at runtime.
         * @param typeReference            The type annotation's type reference.
         * @param typePath                 The type annotation's type path.
         * @return An annotation visitor for consuming the specified annotation.
         */
        AnnotationVisitor visit(String annotationTypeDescriptor, boolean visible, int typeReference, String typePath);

        /**
         * Target for an annotation that is written to a Java type. 写入Java类型注解的目标
         */
        @HashCodeAndEqualsPlugin.Enhance
        class OnType implements Target {

            /**
             * The class visitor to write the annotation to. 编写注解的类访问者
             */
            private final ClassVisitor classVisitor;

            /**
             * Creates a new wrapper for a Java type. 为Java类型创建一个新的包装
             *
             * @param classVisitor The ASM class visitor to which the annotations are appended to. 注解附加的ASM类访问者
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
        }

        /**
         * Target for an annotation that is written to a Java method or constructor.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
        }

        /**
         * Target for an annotation that is written to a Java method or constructor parameter.
         */
        @HashCodeAndEqualsPlugin.Enhance
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
        }

        /**
         * Target for an annotation that is written to a Java field. 写入Java字段注解的目标
         */
        @HashCodeAndEqualsPlugin.Enhance
        class OnField implements Target {

            /**
             * The field visitor to write the annotation to. 借助 fieldVisitor 完成对应字段属性的写入
             */
            private final FieldVisitor fieldVisitor;

            /**
             * Creates a new wrapper for a Java field.
             *
             * @param fieldVisitor The ASM field visitor to which the annotations are appended to. 附加注释的 ASM 字段访问器
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
        }
    }

    /**
     * A default implementation for an annotation appender that writes annotations to a given byte consumer
     * represented by an ASM {@link org.objectweb.asm.AnnotationVisitor}. 注释追加器的默认实现，它将注释写入由 ASM AnnotationVisitor 表示的给定字节使用者
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Default implements AnnotationAppender {

        /**
         * The target onto which an annotation write process is to be applied. 要对其应用注释写入过程的目标
         */
        private final Target target;

        /**
         * Creates a default annotation appender. 创建默认注解附加器
         *
         * @param target The target to which annotations are written to.
         */
        public Default(Target target) {
            this.target = target;
        }

        /**
         * Handles the writing of a single annotation to an annotation visitor. 处理向注释访问者写入单个注释的操作
         *
         * @param annotationVisitor     The annotation visitor the write process is to be applied on.
         * @param annotation            The annotation to be written.
         * @param annotationValueFilter The value filter to apply for discovering which values of an annotation should be written.
         */
        private static void handle(AnnotationVisitor annotationVisitor, AnnotationDescription annotation, AnnotationValueFilter annotationValueFilter) {
            for (MethodDescription.InDefinedShape methodDescription : annotation.getAnnotationType().getDeclaredMethods()) {
                if (annotationValueFilter.isRelevant(annotation, methodDescription)) {
                    apply(annotationVisitor, methodDescription.getReturnType().asErasure(), methodDescription.getName(), annotation.getValue(methodDescription).resolve());
                }
            }
            annotationVisitor.visitEnd();
        }

        /**
         * Performs the writing of a given annotation value to an annotation visitor. 将给定的注释值写入注释访问者
         *
         * @param annotationVisitor The annotation visitor the write process is to be applied on.
         * @param valueType         The type of the annotation value.
         * @param name              The name of the annotation type.
         * @param value             The annotation's value.
         */
        public static void apply(AnnotationVisitor annotationVisitor, TypeDescription valueType, String name, Object value) {
            if (valueType.isArray()) { // The Android emulator reads annotation arrays as annotation types. Therefore, this check needs to come first.
                AnnotationVisitor arrayVisitor = annotationVisitor.visitArray(name);
                int length = Array.getLength(value);
                TypeDescription componentType = valueType.getComponentType();
                for (int index = 0; index < length; index++) {
                    apply(arrayVisitor, componentType, NO_NAME, Array.get(value, index));
                }
                arrayVisitor.visitEnd();
            } else if (valueType.isAnnotation()) {
                handle(annotationVisitor.visitAnnotation(name, valueType.getDescriptor()), (AnnotationDescription) value, AnnotationValueFilter.Default.APPEND_DEFAULTS);
            } else if (valueType.isEnum()) {
                annotationVisitor.visitEnum(name, valueType.getDescriptor(), ((EnumerationDescription) value).getValue());
            } else if (valueType.represents(Class.class)) {
                annotationVisitor.visit(name, Type.getType(((TypeDescription) value).getDescriptor()));
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
         * Tries to append a given annotation by reflectively reading an annotation. 尝试通过反射读取注解来附加给定注解
         *
         * @param annotation            The annotation to be written. 要写的注解
         * @param visible               {@code true} if this annotation should be treated as visible at runtime. {@code true} 如果此注释应在运行时视为可见
         * @param annotationValueFilter The annotation value filter to apply. 要应用的注解值过滤器
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

        /**
         * Tries to append a given annotation by reflectively reading an annotation.
         *
         * @param annotation            The annotation to be written.
         * @param visible               {@code true} if this annotation should be treated as visible at runtime.
         * @param annotationValueFilter The annotation value filter to apply.
         * @param typeReference         The type annotation's type reference.
         * @param typePath              The type annotation's type path.
         */
        private void doAppend(AnnotationDescription annotation,
                              boolean visible,
                              AnnotationValueFilter annotationValueFilter,
                              int typeReference,
                              String typePath) {
            handle(target.visit(annotation.getAnnotationType().getDescriptor(), visible, typeReference, typePath), annotation, annotationValueFilter);
        }
    }

    /**
     * A type visitor that visits all type annotations of a generic type and writes any discovered annotation to a
     * supplied {@link AnnotationAppender}. 类型访问者，该类型访问者访问通用类型的所有类型注释，并将所有发现的注释写入提供的{@link AnnotationAppender}中
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ForTypeAnnotations implements TypeDescription.Generic.Visitor<AnnotationAppender> {

        /**
         * Indicates that type variables type annotations are written on a Java type.
         */
        public static final boolean VARIABLE_ON_TYPE = true;

        /**
         * Indicates that type variables type annotations are written on a Java method or constructor.
         */
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
         * Represents a (reversed) type step to an inner class within a type path.
         */
        private static final char INNER_CLASS_PATH = '.';

        /**
         * Represents an index type delimiter within a type path.
         */
        private static final char INDEXED_TYPE_DELIMITER = ';';

        /**
         * The index that indicates that super type type annotations are written onto a super class.
         */
        private static final int SUPER_CLASS_INDEX = -1;

        /**
         * The annotation appender to use.
         */
        private final AnnotationAppender annotationAppender;

        /**
         * The annotation value filter to use.
         */
        private final AnnotationValueFilter annotationValueFilter;

        /**
         * The type reference to use.
         */
        private final int typeReference;

        /**
         * The type path to use.
         */
        private final String typePath;

        /**
         * Creates a new type annotation appending visitor for an empty type path. 为空的类型路径创建一个新的类型注释附加访问者
         *
         * @param annotationAppender    The annotation appender to use.
         * @param annotationValueFilter The annotation value filter to use.
         * @param typeReference         The type reference to use.
         */
        protected ForTypeAnnotations(AnnotationAppender annotationAppender, AnnotationValueFilter annotationValueFilter, TypeReference typeReference) {
            this(annotationAppender, annotationValueFilter, typeReference.getValue(), EMPTY_TYPE_PATH);
        }

        /**
         * Creates a new type annotation appending visitor.
         *
         * @param annotationAppender    The annotation appender to use.
         * @param annotationValueFilter The annotation value filter to use.
         * @param typeReference         The type reference to use.
         * @param typePath              The type path to use.
         */
        protected ForTypeAnnotations(AnnotationAppender annotationAppender, AnnotationValueFilter annotationValueFilter, int typeReference, String typePath) {
            this.annotationAppender = annotationAppender;
            this.annotationValueFilter = annotationValueFilter;
            this.typeReference = typeReference;
            this.typePath = typePath;
        }

        /**
         * Creates a type annotation appender for a type annotations of a super class type. 为超类的类型注释创建类型注释追加器
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @return A visitor for appending type annotations of a super class.
         */
        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofSuperClass(AnnotationAppender annotationAppender,
                                                                                       AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newSuperTypeReference(SUPER_CLASS_INDEX));
        }

        /**
         * Creates a type annotation appender for type annotations of an interface type. 为接口类型的类型注解创建类型注解附加器
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @param index                 The index of the interface type.
         * @return A visitor for appending type annotations of an interface type.
         */
        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofInterfaceType(AnnotationAppender annotationAppender,
                                                                                          AnnotationValueFilter annotationValueFilter,
                                                                                          int index) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newSuperTypeReference(index));
        }

        /**
         * Creates a type annotation appender for type annotations of a field's type. 为字段类型的类注解创建类注解附加器
         *
         * @param annotationAppender    The annotation appender to write any type annotation to. 要向其写入任何类注解的注解附加器
         * @param annotationValueFilter The annotation value filter to apply.  要应用的注释值过滤器
         * @return A visitor for appending type annotations of a field's type. 用于附加字段类型的类注解访问者
         */
        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofFieldType(AnnotationAppender annotationAppender,
                                                                                      AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newTypeReference(TypeReference.FIELD));
        }

        /**
         * Creates a type annotation appender for type annotations of a method's return type.
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @return A visitor for appending type annotations of a method's return type.
         */
        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofMethodReturnType(AnnotationAppender annotationAppender,
                                                                                             AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newTypeReference(TypeReference.METHOD_RETURN));
        }

        /**
         * Creates a type annotation appender for type annotations of a method's parameter type.
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @param index                 The parameter index.
         * @return A visitor for appending type annotations of a method's parameter type.
         */
        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofMethodParameterType(AnnotationAppender annotationAppender,
                                                                                                AnnotationValueFilter annotationValueFilter,
                                                                                                int index) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newFormalParameterReference(index));
        }

        /**
         * Creates a type annotation appender for type annotations of a method's exception type.
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @param index                 The exception type's index.
         * @return A visitor for appending type annotations of a method's exception type.
         */
        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofExceptionType(AnnotationAppender annotationAppender,
                                                                                          AnnotationValueFilter annotationValueFilter,
                                                                                          int index) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newExceptionReference(index));
        }

        /**
         * Creates a type annotation appender for type annotations of a method's receiver type.
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @return A visitor for appending type annotations of a method's receiver type.
         */
        public static TypeDescription.Generic.Visitor<AnnotationAppender> ofReceiverType(AnnotationAppender annotationAppender,
                                                                                         AnnotationValueFilter annotationValueFilter) {
            return new ForTypeAnnotations(annotationAppender, annotationValueFilter, TypeReference.newTypeReference(TypeReference.METHOD_RECEIVER));
        }

        /**
         * Appends all supplied type variables to the supplied method appender. 将所有提供的类型变量附加到提供的方法附加器中
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @param variableOnType        {@code true} if the type variables are declared by a type, {@code false} if they are declared by a method.
         * @param typeVariables         The type variables to append.
         * @return The resulting annotation appender.
         */
        public static AnnotationAppender ofTypeVariable(AnnotationAppender annotationAppender,
                                                        AnnotationValueFilter annotationValueFilter,
                                                        boolean variableOnType,
                                                        List<? extends TypeDescription.Generic> typeVariables) {
            return ofTypeVariable(annotationAppender, annotationValueFilter, variableOnType, 0, typeVariables);
        }

        /**
         * Appends all supplied type variables to the supplied method appender. 将提供的所有类型变量附加到提供的方法附加器
         *
         * @param annotationAppender    The annotation appender to write any type annotation to.
         * @param annotationValueFilter The annotation value filter to apply.
         * @param variableOnType        {@code true} if the type variables are declared by a type, {@code false} if they are declared by a method.
         * @param subListIndex          The index of the first type variable to append. All previous type variables are ignored.
         * @param typeVariables         The type variables to append.
         * @return The resulting annotation appender.
         */
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
                int boundIndex = !typeVariable.getUpperBounds().get(0).getSort().isTypeVariable() && typeVariable.getUpperBounds().get(0).isInterface()
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
            return genericArray.getComponentType().accept(new ForTypeAnnotations(apply(genericArray, typePath),
                    annotationValueFilter,
                    typeReference,
                    typePath + COMPONENT_TYPE_PATH));
        }

        @Override
        public AnnotationAppender onWildcard(TypeDescription.Generic wildcard) {
            TypeList.Generic lowerBounds = wildcard.getLowerBounds();
            return (lowerBounds.isEmpty()
                    ? wildcard.getUpperBounds().getOnly()
                    : lowerBounds.getOnly()).accept(new ForTypeAnnotations(apply(wildcard, typePath), annotationValueFilter, typeReference, typePath + WILDCARD_TYPE_PATH));
        }

        @Override
        public AnnotationAppender onParameterizedType(TypeDescription.Generic parameterizedType) {
            StringBuilder typePath = new StringBuilder(this.typePath);
            for (int index = 0; index < parameterizedType.asErasure().getInnerClassCount(); index++) {
                typePath = typePath.append(INNER_CLASS_PATH);
            }
            AnnotationAppender annotationAppender = apply(parameterizedType, typePath.toString());
            TypeDescription.Generic ownerType = parameterizedType.getOwnerType();
            if (ownerType != null) {
                annotationAppender = ownerType.accept(new ForTypeAnnotations(annotationAppender,
                        annotationValueFilter,
                        typeReference,
                        this.typePath));
            }
            int index = 0;
            for (TypeDescription.Generic typeArgument : parameterizedType.getTypeArguments()) {
                annotationAppender = typeArgument.accept(new ForTypeAnnotations(annotationAppender,
                        annotationValueFilter,
                        typeReference,
                        typePath.toString() + index++ + INDEXED_TYPE_DELIMITER));
            }
            return annotationAppender;
        }

        @Override
        public AnnotationAppender onTypeVariable(TypeDescription.Generic typeVariable) {
            return apply(typeVariable, typePath);
        }

        @Override
        public AnnotationAppender onNonGenericType(TypeDescription.Generic typeDescription) {
            StringBuilder typePath = new StringBuilder(this.typePath);
            for (int index = 0; index < typeDescription.asErasure().getInnerClassCount(); index++) {
                typePath = typePath.append(INNER_CLASS_PATH);
            }
            AnnotationAppender annotationAppender = apply(typeDescription, typePath.toString());
            if (typeDescription.isArray()) {
                annotationAppender = typeDescription.getComponentType().accept(new ForTypeAnnotations(annotationAppender,
                        annotationValueFilter,
                        typeReference,
                        this.typePath + COMPONENT_TYPE_PATH)); // Impossible to be inner class
            }
            return annotationAppender;
        }

        /**
         * Writes all annotations of the supplied type to this instance's annotation appender. 将所提供类型的所有注解写入此实例的批注附加器
         *
         * @param typeDescription The type of what all annotations should be written of.
         * @param typePath        The type path to use.
         * @return The resulting annotation appender.
         */
        private AnnotationAppender apply(TypeDescription.Generic typeDescription, String typePath) {
            AnnotationAppender annotationAppender = this.annotationAppender;
            for (AnnotationDescription annotationDescription : typeDescription.getDeclaredAnnotations()) {
                annotationAppender = annotationAppender.append(annotationDescription, annotationValueFilter, typeReference, typePath);
            }
            return annotationAppender;
        }
    }
}

// 比如method的字节码结构，它的属性就是一个表
// method_info {
//    u2             access_flags;
//    u2             name_index;
//    u2             descriptor_index;
//    // 这里
//    u2             attributes_count;
//    attribute_info attributes[attributes_count];
//    // 结束
// }
/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.attribute;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        /**
         * {@inheritDoc}
         */
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            /* do nothing */
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

        /**
         * {@inheritDoc}
         */
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            annotationAppender = AnnotationAppender.ForTypeAnnotations.ofTypeVariable(annotationAppender,
                    annotationValueFilter,
                    AnnotationAppender.ForTypeAnnotations.VARIABLE_ON_TYPE,
                    instrumentedType.getTypeVariables());
            TypeDescription.Generic superClass = instrumentedType.getSuperClass();
            if (superClass != null) {
                annotationAppender = superClass.accept(AnnotationAppender.ForTypeAnnotations.ofSuperClass(annotationAppender, annotationValueFilter));
            }
            int interfaceIndex = 0;
            for (TypeDescription.Generic interfaceType : instrumentedType.getInterfaces()) {
                annotationAppender = interfaceType.accept(AnnotationAppender.ForTypeAnnotations.ofInterfaceType(annotationAppender,
                        annotationValueFilter,
                        interfaceIndex++));
            }
            for (AnnotationDescription annotation : instrumentedType.getDeclaredAnnotations()) {
                annotationAppender = annotationAppender.append(annotation, annotationValueFilter);
            }
        }

        /**
         * A type attribute appender that writes all annotations of the instrumented but excludes annotations up to
         * a given index.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public static class Differentiating implements TypeAttributeAppender {

            /**
             * The index of the first annotations that should be directly written onto the type.
             */
            private final int annotationIndex;

            /**
             * The index of the first type variable for which type annotations should be directly written onto the type.
             */
            private final int typeVariableIndex;

            /**
             * The index of the first interface type for which type annotations should be directly written onto the type.
             */
            private final int interfaceTypeIndex;

            /**
             * Creates a new differentiating type attribute appender.
             *
             * @param typeDescription The type for which to resolve all exclusion indices.
             */
            public Differentiating(TypeDescription typeDescription) {
                this(typeDescription.getDeclaredAnnotations().size(), typeDescription.getTypeVariables().size(), typeDescription.getInterfaces().size());
            }

            /**
             * Creates a new differentiating type attribute appender.
             *
             * @param annotationIndex    The index of the first annotations that should be directly written onto the type.
             * @param typeVariableIndex  The index of the first interface type for which type annotations should be directly written onto the type.
             * @param interfaceTypeIndex The index of the first interface type for which type annotations should be directly written onto the type.
             */
            protected Differentiating(int annotationIndex, int typeVariableIndex, int interfaceTypeIndex) {
                this.annotationIndex = annotationIndex;
                this.typeVariableIndex = typeVariableIndex;
                this.interfaceTypeIndex = interfaceTypeIndex;
            }

            /**
             * {@inheritDoc}
             */
            public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
                AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
                AnnotationAppender.ForTypeAnnotations.ofTypeVariable(annotationAppender,
                        annotationValueFilter,
                        AnnotationAppender.ForTypeAnnotations.VARIABLE_ON_TYPE,
                        typeVariableIndex,
                        instrumentedType.getTypeVariables());
                TypeList.Generic interfaceTypes = instrumentedType.getInterfaces();
                int interfaceTypeIndex = this.interfaceTypeIndex;
                for (TypeDescription.Generic interfaceType : interfaceTypes.subList(this.interfaceTypeIndex, interfaceTypes.size())) {
                    annotationAppender = interfaceType.accept(AnnotationAppender.ForTypeAnnotations.ofInterfaceType(annotationAppender,
                            annotationValueFilter,
                            interfaceTypeIndex++));
                }
                AnnotationList declaredAnnotations = instrumentedType.getDeclaredAnnotations();
                for (AnnotationDescription annotationDescription : declaredAnnotations.subList(annotationIndex, declaredAnnotations.size())) {
                    annotationAppender = annotationAppender.append(annotationDescription, annotationValueFilter);
                }
            }
        }
    }

    /**
     * An attribute appender that appends a single annotation to a given type. The visibility for the annotation
     * will be inferred from the annotation's {@link java.lang.annotation.RetentionPolicy}.
     */
    @HashCodeAndEqualsPlugin.Enhance
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

        /**
         * {@inheritDoc}
         */
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (AnnotationDescription annotation : annotations) {
                appender = appender.append(annotation, annotationValueFilter);
            }
        }
    }

    /**
     * A compound type attribute appender that concatenates a number of other attribute appenders.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements TypeAttributeAppender {

        /**
         * The type attribute appenders this compound appender represents in their application order.
         */
        private final List<TypeAttributeAppender> typeAttributeAppenders;

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
            this.typeAttributeAppenders = new ArrayList<TypeAttributeAppender>();
            for (TypeAttributeAppender typeAttributeAppender : typeAttributeAppenders) {
                if (typeAttributeAppender instanceof Compound) {
                    this.typeAttributeAppenders.addAll(((Compound) typeAttributeAppender).typeAttributeAppenders);
                } else if (!(typeAttributeAppender instanceof NoOp)) {
                    this.typeAttributeAppenders.add(typeAttributeAppender);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            for (TypeAttributeAppender typeAttributeAppender : typeAttributeAppenders) {
                typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilter);
            }
        }
    }
}

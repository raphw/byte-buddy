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
package net.bytebuddy.asm;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A visitor that adds attributes to a class member.
 *
 * @param <T> The type of the attribute appender factory.
 */
@HashCodeAndEqualsPlugin.Enhance
public abstract class MemberAttributeExtension<T> {

    /**
     * The annotation value filter factory to apply.
     */
    protected final AnnotationValueFilter.Factory annotationValueFilterFactory;

    /**
     * The attribute appender factory to use.
     */
    protected final T attributeAppenderFactory;

    /**
     * Creates a new member attribute extension.
     *
     * @param annotationValueFilterFactory The annotation value filter factory to apply.
     * @param attributeAppenderFactory     The attribute appender factory to use.
     */
    protected MemberAttributeExtension(AnnotationValueFilter.Factory annotationValueFilterFactory, T attributeAppenderFactory) {
        this.annotationValueFilterFactory = annotationValueFilterFactory;
        this.attributeAppenderFactory = attributeAppenderFactory;
    }

    /**
     * A visitor that adds attributes to a field.
     */
    public static class ForField extends MemberAttributeExtension<FieldAttributeAppender.Factory> implements AsmVisitorWrapper.ForDeclaredFields.FieldVisitorWrapper {

        /**
         * Creates a field attribute extension that appends default values of annotations.
         */
        public ForField() {
            this(AnnotationValueFilter.Default.APPEND_DEFAULTS);
        }

        /**
         * Creates a field attribute extension.
         *
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         */
        public ForField(AnnotationValueFilter.Factory annotationValueFilterFactory) {
            this(annotationValueFilterFactory, FieldAttributeAppender.NoOp.INSTANCE);
        }

        /**
         * Creates a field attribute extension.
         *
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param attributeAppenderFactory     The field attribute appender factory to use.
         */
        protected ForField(AnnotationValueFilter.Factory annotationValueFilterFactory, FieldAttributeAppender.Factory attributeAppenderFactory) {
            super(annotationValueFilterFactory, attributeAppenderFactory);
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotation The annotations to append.
         * @return A new field attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForField annotate(Annotation... annotation) {
            return annotate(Arrays.asList(annotation));
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotations The annotations to append.
         * @return A new field attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForField annotate(List<? extends Annotation> annotations) {
            return annotate(new AnnotationList.ForLoadedAnnotations(annotations));
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotation The annotations to append.
         * @return A new field attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForField annotate(AnnotationDescription... annotation) {
            return annotate(Arrays.asList(annotation));
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotations The annotations to append.
         * @return A new field attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForField annotate(Collection<? extends AnnotationDescription> annotations) {
            return attribute(new FieldAttributeAppender.Explicit(new ArrayList<AnnotationDescription>(annotations)));
        }

        /**
         * Appends the supplied attribute appender factory.
         *
         * @param attributeAppenderFactory The attribute appender factory to append.
         * @return A new field attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForField attribute(FieldAttributeAppender.Factory attributeAppenderFactory) {
            return new ForField(annotationValueFilterFactory, new FieldAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        /**
         * {@inheritDoc}
         */
        public FieldVisitor wrap(TypeDescription instrumentedType, FieldDescription.InDefinedShape fieldDescription, FieldVisitor fieldVisitor) {
            return new FieldAttributeVisitor(fieldVisitor,
                    fieldDescription,
                    attributeAppenderFactory.make(instrumentedType),
                    annotationValueFilterFactory.on(fieldDescription));
        }

        /**
         * Applies this attribute extension on any field that matches the supplied matcher.
         *
         * @param matcher The matcher that decides what fields the represented extension is applied to.
         * @return An appropriate ASM visitor wrapper.
         */
        public AsmVisitorWrapper on(ElementMatcher<? super FieldDescription.InDefinedShape> matcher) {
            return new AsmVisitorWrapper.ForDeclaredFields().field(matcher, this);
        }

        /**
         * A field visitor to apply an field attribute appender.
         */
        private static class FieldAttributeVisitor extends FieldVisitor {

            /**
             * The field to add annotations to.
             */
            private final FieldDescription fieldDescription;

            /**
             * The field attribute appender to apply.
             */
            private final FieldAttributeAppender fieldAttributeAppender;

            /**
             * The annotation value filter to apply.
             */
            private final AnnotationValueFilter annotationValueFilter;

            /**
             * Creates a new field attribute visitor.
             *
             * @param fieldVisitor           The field visitor to apply changes to.
             * @param fieldDescription       The field to add annotations to.
             * @param fieldAttributeAppender The field attribute appender to apply.
             * @param annotationValueFilter  The annotation value filter to apply.
             */
            private FieldAttributeVisitor(FieldVisitor fieldVisitor,
                                          FieldDescription fieldDescription,
                                          FieldAttributeAppender fieldAttributeAppender,
                                          AnnotationValueFilter annotationValueFilter) {
                super(OpenedClassReader.ASM_API, fieldVisitor);
                this.fieldDescription = fieldDescription;
                this.fieldAttributeAppender = fieldAttributeAppender;
                this.annotationValueFilter = annotationValueFilter;
            }

            @Override
            public void visitEnd() {
                fieldAttributeAppender.apply(fv, fieldDescription, annotationValueFilter);
                super.visitEnd();
            }
        }
    }

    /**
     * A visitor that adds attributes to a method.
     */
    public static class ForMethod extends MemberAttributeExtension<MethodAttributeAppender.Factory> implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

        /**
         * Creates a method attribute extension.
         */
        public ForMethod() {
            this(AnnotationValueFilter.Default.APPEND_DEFAULTS);
        }

        /**
         * Creates a method attribute extension.
         *
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         */
        public ForMethod(AnnotationValueFilter.Factory annotationValueFilterFactory) {
            this(annotationValueFilterFactory, MethodAttributeAppender.NoOp.INSTANCE);
        }

        /**
         * Creates a method attribute extension.
         *
         * @param annotationValueFilterFactory The annotation value filter factory to apply.
         * @param attributeAppenderFactory     The method attribute appender factory to use.
         */
        protected ForMethod(AnnotationValueFilter.Factory annotationValueFilterFactory, MethodAttributeAppender.Factory attributeAppenderFactory) {
            super(annotationValueFilterFactory, attributeAppenderFactory);
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotation The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateMethod(Annotation... annotation) {
            return annotateMethod(Arrays.asList(annotation));
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotations The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateMethod(List<? extends Annotation> annotations) {
            return annotateMethod(new AnnotationList.ForLoadedAnnotations(annotations));
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotation The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateMethod(AnnotationDescription... annotation) {
            return annotateMethod(Arrays.asList(annotation));
        }

        /**
         * Appends the supplied annotations.
         *
         * @param annotations The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateMethod(Collection<? extends AnnotationDescription> annotations) {
            return attribute(new MethodAttributeAppender.Explicit(new ArrayList<AnnotationDescription>(annotations)));
        }

        /**
         * Appends the supplied annotations to the parameter at the given index.
         *
         * @param index      The parameter index.
         * @param annotation The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateParameter(int index, Annotation... annotation) {
            return annotateParameter(index, Arrays.asList(annotation));
        }

        /**
         * Appends the supplied annotations to the parameter at the given index.
         *
         * @param index       The parameter index.
         * @param annotations The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateParameter(int index, List<? extends Annotation> annotations) {
            return annotateParameter(index, new AnnotationList.ForLoadedAnnotations(annotations));
        }

        /**
         * Appends the supplied annotations to the parameter at the given index.
         *
         * @param index      The parameter index.
         * @param annotation The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateParameter(int index, AnnotationDescription... annotation) {
            return annotateParameter(index, Arrays.asList(annotation));
        }

        /**
         * Appends the supplied annotations to the parameter at the given index.
         *
         * @param index       The parameter index.
         * @param annotations The annotations to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod annotateParameter(int index, Collection<? extends AnnotationDescription> annotations) {
            if (index < 0) {
                throw new IllegalArgumentException("Parameter index cannot be negative: " + index);
            }
            return attribute(new MethodAttributeAppender.Explicit(index, new ArrayList<AnnotationDescription>(annotations)));
        }


        /**
         * Appends the supplied method attribute appender factory.
         *
         * @param attributeAppenderFactory The attribute appender factory to append.
         * @return A new method attribute extension that appends any previously registered attributes and the supplied annotations.
         */
        public ForMethod attribute(MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new ForMethod(annotationValueFilterFactory, new MethodAttributeAppender.Factory.Compound(this.attributeAppenderFactory, attributeAppenderFactory));
        }

        /**
         * {@inheritDoc}
         */
        public MethodVisitor wrap(TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod,
                                  MethodVisitor methodVisitor,
                                  Implementation.Context implementationContext,
                                  TypePool typePool,
                                  int writerFlags,
                                  int readerFlags) {
            return new AttributeAppendingMethodVisitor(methodVisitor,
                    instrumentedMethod,
                    attributeAppenderFactory.make(instrumentedType),
                    annotationValueFilterFactory.on(instrumentedMethod));
        }

        /**
         * Applies this attribute extension on any method or constructor that matches the supplied matcher.
         *
         * @param matcher The matcher that decides what methods or constructors the represented extension is applied to.
         * @return An appropriate ASM visitor wrapper.
         */
        public AsmVisitorWrapper on(ElementMatcher<? super MethodDescription> matcher) {
            return new AsmVisitorWrapper.ForDeclaredMethods().invokable(matcher, this);
        }

        /**
         * A method visitor to apply a method attribute appender.
         */
        private static class AttributeAppendingMethodVisitor extends MethodVisitor {

            /**
             * The instrumented method.
             */
            private final MethodDescription methodDescription;

            /**
             * The field to add annotations to.
             */
            private final MethodAttributeAppender methodAttributeAppender;

            /**
             * The annotation value filter to apply.
             */
            private final AnnotationValueFilter annotationValueFilter;

            /**
             * {@code true} if the attribute appender was not yet applied.
             */
            private boolean applicable;

            /**
             * @param methodVisitor           The method visitor to apply changes to.
             * @param methodDescription       The method to add annotations to.
             * @param methodAttributeAppender The annotation value filter to apply.
             * @param annotationValueFilter   The annotation value filter to apply.
             */
            private AttributeAppendingMethodVisitor(MethodVisitor methodVisitor,
                                                    MethodDescription methodDescription,
                                                    MethodAttributeAppender methodAttributeAppender,
                                                    AnnotationValueFilter annotationValueFilter) {
                super(OpenedClassReader.ASM_API, methodVisitor);
                this.methodDescription = methodDescription;
                this.methodAttributeAppender = methodAttributeAppender;
                this.annotationValueFilter = annotationValueFilter;
                applicable = true;
            }

            @Override
            public void visitCode() {
                if (applicable) {
                    methodAttributeAppender.apply(mv, methodDescription, annotationValueFilter);
                    applicable = false;
                }
                super.visitCode();
            }

            @Override
            public void visitEnd() {
                if (applicable) {
                    methodAttributeAppender.apply(mv, methodDescription, annotationValueFilter);
                    applicable = false;
                }
                super.visitEnd();
            }
        }
    }
}

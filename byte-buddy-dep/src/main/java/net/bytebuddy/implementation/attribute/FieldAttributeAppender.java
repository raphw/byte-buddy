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
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.FieldVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.FieldVisitor}.
 */
public interface FieldAttributeAppender {

    /**
     * Applies this attribute appender to a given field visitor.
     *
     * @param fieldVisitor          The field visitor to which the attributes that are represented by this attribute appender are written to.
     * @param fieldDescription      The description of the field to which the field visitor belongs to.
     * @param annotationValueFilter The annotation value filter to apply when writing annotations.
     */
    void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter);

    /**
     * A field attribute appender that does not append any attributes.
     */
    enum NoOp implements FieldAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            /* do nothing */
        }
    }

    /**
     * A factory that creates field attribute appenders for a given type.
     */
    interface Factory {

        /**
         * Returns a field attribute appender that is applicable for a given type description.
         *
         * @param typeDescription The type for which a field attribute appender is to be applied for.
         * @return The field attribute appender which should be applied for the given type.
         */
        FieldAttributeAppender make(TypeDescription typeDescription);

        /**
         * A field attribute appender factory that combines several field attribute appender factories to be
         * represented as a single factory.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Compound implements Factory {

            /**
             * The factories that this compound factory represents in their application order.
             */
            private final List<Factory> factories;

            /**
             * Creates a new compound field attribute appender factory.
             *
             * @param factory The factories to represent in the order of their application.
             */
            public Compound(Factory... factory) {
                this(Arrays.asList(factory));
            }

            /**
             * Creates a new compound field attribute appender factory.
             *
             * @param factories The factories to represent in the order of their application.
             */
            public Compound(List<? extends Factory> factories) {
                this.factories = new ArrayList<Factory>();
                for (Factory factory : factories) {
                    if (factory instanceof Compound) {
                        this.factories.addAll(((Compound) factory).factories);
                    } else if (!(factory instanceof NoOp)) {
                        this.factories.add(factory);
                    }
                }
            }

            /**
             * {@inheritDoc}
             */
            public FieldAttributeAppender make(TypeDescription typeDescription) {
                List<FieldAttributeAppender> fieldAttributeAppenders = new ArrayList<FieldAttributeAppender>(factories.size());
                for (Factory factory : factories) {
                    fieldAttributeAppenders.add(factory.make(typeDescription));
                }
                return new FieldAttributeAppender.Compound(fieldAttributeAppenders);
            }
        }
    }

    /**
     * An attribute appender that writes all annotations that are declared on a field.
     */
    enum ForInstrumentedField implements FieldAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            annotationAppender = fieldDescription.getType().accept(AnnotationAppender.ForTypeAnnotations.ofFieldType(annotationAppender, annotationValueFilter));
            for (AnnotationDescription annotation : fieldDescription.getDeclaredAnnotations()) {
                annotationAppender = annotationAppender.append(annotation, annotationValueFilter);
            }
        }

        /**
         * {@inheritDoc}
         */
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    /**
     * Appends an annotation to a field. The visibility of the annotation is determined by the annotation type's
     * {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Explicit implements FieldAttributeAppender, Factory {

        /**
         * The annotations that this appender appends.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new annotation attribute appender for explicit annotation values. All values, including default values, are copied.
         *
         * @param annotations The annotations to be appended to the field.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this.annotations = annotations;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnField(fieldVisitor));
            for (AnnotationDescription annotation : annotations) {
                appender = appender.append(annotation, annotationValueFilter);
            }
        }

        /**
         * {@inheritDoc}
         */
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    /**
     * A field attribute appender that combines several method attribute appenders to be represented as a single
     * field attribute appender.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements FieldAttributeAppender {

        /**
         * The field attribute appenders this appender represents in their application order.
         */
        private final List<FieldAttributeAppender> fieldAttributeAppenders;

        /**
         * Creates a new compound field attribute appender.
         *
         * @param fieldAttributeAppender The field attribute appenders that are to be combined by this compound appender
         *                               in the order of their application.
         */
        public Compound(FieldAttributeAppender... fieldAttributeAppender) {
            this(Arrays.asList(fieldAttributeAppender));
        }

        /**
         * Creates a new compound field attribute appender.
         *
         * @param fieldAttributeAppenders The field attribute appenders that are to be combined by this compound appender
         *                                in the order of their application.
         */
        public Compound(List<? extends FieldAttributeAppender> fieldAttributeAppenders) {
            this.fieldAttributeAppenders = new ArrayList<FieldAttributeAppender>();
            for (FieldAttributeAppender fieldAttributeAppender : fieldAttributeAppenders) {
                if (fieldAttributeAppender instanceof Compound) {
                    this.fieldAttributeAppenders.addAll(((Compound) fieldAttributeAppender).fieldAttributeAppenders);
                } else if (!(fieldAttributeAppender instanceof NoOp)) {
                    this.fieldAttributeAppenders.add(fieldAttributeAppender);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription, AnnotationValueFilter annotationValueFilter) {
            for (FieldAttributeAppender fieldAttributeAppender : fieldAttributeAppenders) {
                fieldAttributeAppender.apply(fieldVisitor, fieldDescription, annotationValueFilter);
            }
        }
    }
}

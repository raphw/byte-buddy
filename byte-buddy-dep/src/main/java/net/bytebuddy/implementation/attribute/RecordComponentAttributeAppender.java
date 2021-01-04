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
import net.bytebuddy.description.type.RecordComponentDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.RecordComponentVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An appender that writes attributes or annotations to a given ASM {@link RecordComponentVisitor}.
 */
public interface RecordComponentAttributeAppender {

    /**
     * Applies this attribute appender to a given record component visitor.
     *
     * @param recordComponentVisitor     The record component visitor to which the attributes that are represented by this attribute appender are written to.
     * @param recordComponentDescription The description of the record component to which the record component visitor belongs to.
     * @param annotationValueFilter      The annotation value filter to apply when writing annotations.
     */
    void apply(RecordComponentVisitor recordComponentVisitor, RecordComponentDescription recordComponentDescription, AnnotationValueFilter annotationValueFilter);

    /**
     * A record component attribute appender that does not append any attributes.
     */
    enum NoOp implements RecordComponentAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public RecordComponentAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(RecordComponentVisitor recordComponentVisitor, RecordComponentDescription recordComponentDescription, AnnotationValueFilter annotationValueFilter) {
            /* do nothing */
        }
    }

    /**
     * A factory that creates record component attribute appenders for a given type.
     */
    interface Factory {

        /**
         * Returns a record component attribute appender that is applicable for a given type description.
         *
         * @param typeDescription The type for which a record component attribute appender is to be applied for.
         * @return The record component attribute appender which should be applied for the given type.
         */
        RecordComponentAttributeAppender make(TypeDescription typeDescription);

        /**
         * A record component attribute appender factory that combines several record component attribute appender factories to be
         * represented as a single factory.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Compound implements Factory {

            /**
             * The factories that this compound factory represents in their application order.
             */
            private final List<Factory> factories;

            /**
             * Creates a new compound record component attribute appender factory.
             *
             * @param factory The factories to represent in the order of their application.
             */
            public Compound(Factory... factory) {
                this(Arrays.asList(factory));
            }

            /**
             * Creates a new compound record component attribute appender factory.
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
            public RecordComponentAttributeAppender make(TypeDescription typeDescription) {
                List<RecordComponentAttributeAppender> recordComponentAttributeAppenders = new ArrayList<RecordComponentAttributeAppender>(factories.size());
                for (Factory factory : factories) {
                    recordComponentAttributeAppenders.add(factory.make(typeDescription));
                }
                return new RecordComponentAttributeAppender.Compound(recordComponentAttributeAppenders);
            }
        }
    }

    /**
     * An attribute appender that writes all annotations that are declared on a record component.
     */
    enum ForInstrumentedRecordComponent implements RecordComponentAttributeAppender, Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public void apply(RecordComponentVisitor recordComponentVisitor, RecordComponentDescription recordComponentDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnRecordComponent(recordComponentVisitor));
            annotationAppender = recordComponentDescription.getType().accept(AnnotationAppender.ForTypeAnnotations.ofFieldType(annotationAppender, annotationValueFilter));
            for (AnnotationDescription annotation : recordComponentDescription.getDeclaredAnnotations()) {
                annotationAppender = annotationAppender.append(annotation, annotationValueFilter);
            }
        }

        /**
         * {@inheritDoc}
         */
        public RecordComponentAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    /**
     * Appends an annotation to a record component. The visibility of the annotation is determined by the annotation type's
     * {@link java.lang.annotation.RetentionPolicy} annotation.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Explicit implements RecordComponentAttributeAppender, Factory {

        /**
         * The annotations that this appender appends.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new annotation attribute appender for explicit annotation values. All values, including default values, are copied.
         *
         * @param annotations The annotations to be appended to the record component.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this.annotations = annotations;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(RecordComponentVisitor recordComponentVisitor, RecordComponentDescription recordComponentDescription, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnRecordComponent(recordComponentVisitor));
            for (AnnotationDescription annotation : annotations) {
                appender = appender.append(annotation, annotationValueFilter);
            }
        }

        /**
         * {@inheritDoc}
         */
        public RecordComponentAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    /**
     * A record component attribute appender that combines several method attribute appenders to be represented as a single
     * record component attribute appender.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Compound implements RecordComponentAttributeAppender {

        /**
         * The record component attribute appenders this appender represents in their application order.
         */
        private final List<RecordComponentAttributeAppender> recordComponentAttributeAppenders;

        /**
         * Creates a new compound record component attribute appender.
         *
         * @param recordComponentAttributeAppender The record component attribute appenders that are to be combined by this compound appender
         *                                         in the order of their application.
         */
        public Compound(RecordComponentAttributeAppender... recordComponentAttributeAppender) {
            this(Arrays.asList(recordComponentAttributeAppender));
        }

        /**
         * Creates a new compound record component attribute appender.
         *
         * @param recordComponentAttributeAppenders The record component attribute appenders that are to be combined by this compound appender
         *                                          in the order of their application.
         */
        public Compound(List<? extends RecordComponentAttributeAppender> recordComponentAttributeAppenders) {
            this.recordComponentAttributeAppenders = new ArrayList<RecordComponentAttributeAppender>();
            for (RecordComponentAttributeAppender recordComponentAttributeAppender : recordComponentAttributeAppenders) {
                if (recordComponentAttributeAppender instanceof Compound) {
                    this.recordComponentAttributeAppenders.addAll(((Compound) recordComponentAttributeAppender).recordComponentAttributeAppenders);
                } else if (!(recordComponentAttributeAppender instanceof NoOp)) {
                    this.recordComponentAttributeAppenders.add(recordComponentAttributeAppender);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void apply(RecordComponentVisitor recordComponentVisitor, RecordComponentDescription recordComponentDescription, AnnotationValueFilter annotationValueFilter) {
            for (RecordComponentAttributeAppender recordComponentAttributeAppender : recordComponentAttributeAppenders) {
                recordComponentAttributeAppender.apply(recordComponentVisitor, recordComponentDescription, annotationValueFilter);
            }
        }
    }
}

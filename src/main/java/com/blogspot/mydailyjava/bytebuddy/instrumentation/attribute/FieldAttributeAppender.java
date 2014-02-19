package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.FieldVisitor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public interface FieldAttributeAppender {

    static interface Factory {

        static class Compound implements Factory {

            private final Factory[] factory;

            public Compound(Factory... factory) {
                this.factory = factory;
            }

            @Override
            public FieldAttributeAppender make(TypeDescription typeDescription) {
                FieldAttributeAppender[] fieldAttributeAppender = new FieldAttributeAppender[factory.length];
                int index = 0;
                for (Factory factory : this.factory) {
                    fieldAttributeAppender[index++] = factory.make(typeDescription);
                }
                return new FieldAttributeAppender.Compound(fieldAttributeAppender);
            }
        }

        FieldAttributeAppender make(TypeDescription typeDescription);
    }

    static enum NoOp implements FieldAttributeAppender, Factory {
        INSTANCE;

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            /* do nothing */
        }
    }

    static class ForAnnotation implements FieldAttributeAppender, Factory {

        private final Annotation annotation;

        public ForAnnotation(Annotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(
                    new AnnotationAppender.Target.OnField(fieldVisitor), AnnotationAppender.Visibility.VISIBLE);
            annotationAppender.append(annotation);
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static class ForLoadedField implements FieldAttributeAppender, Factory {

        private final Field field;

        public ForLoadedField(Field field) {
            this.field = field;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            ForInstrumentedField.INSTANCE.apply(fieldVisitor, new FieldDescription.ForLoadedField(field));
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static enum ForInstrumentedField implements FieldAttributeAppender, Factory {
        INSTANCE;

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(
                    new AnnotationAppender.Target.OnField(fieldVisitor), AnnotationAppender.Visibility.VISIBLE);
            for (Annotation annotation : fieldDescription.getAnnotations()) {
                annotationAppender.append(annotation);
            }
        }

        @Override
        public FieldAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    static class Compound implements FieldAttributeAppender {

        private final FieldAttributeAppender[] fieldAttributeAppender;

        public Compound(FieldAttributeAppender... fieldAttributeAppender) {
            this.fieldAttributeAppender = fieldAttributeAppender;
        }

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {
            for (FieldAttributeAppender fieldAttributeAppender : this.fieldAttributeAppender) {
                fieldAttributeAppender.apply(fieldVisitor, fieldDescription);
            }
        }
    }

    void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription);
}

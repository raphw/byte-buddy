package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.FieldVisitor;

import java.lang.annotation.Annotation;

public interface FieldAnnotationAppender {

    static interface Factory {

        FieldAnnotationAppender make(TypeDescription typeDescription);
    }

    static class ForAnnotation implements FieldAnnotationAppender {

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
    }

    static class ForInstrumentedField implements FieldAnnotationAppender, Factory {

        @Override
        public void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription) {

        }

        @Override
        public FieldAnnotationAppender make(TypeDescription typeDescription) {
            return this;
        }
    }

    void apply(FieldVisitor fieldVisitor, FieldDescription fieldDescription);
}

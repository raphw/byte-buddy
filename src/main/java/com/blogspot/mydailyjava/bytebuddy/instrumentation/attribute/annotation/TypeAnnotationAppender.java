package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;

import java.lang.annotation.Annotation;

public interface TypeAnnotationAppender {

    static class ForAnnotation implements TypeAnnotationAppender {

        private final Annotation annotation;

        public ForAnnotation(Annotation annotation) {
            this.annotation = annotation;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(
                    new AnnotationAppender.Target.OnType(classVisitor), AnnotationAppender.Visibility.VISIBLE);
            annotationAppender.append(annotation);
        }
    }

    static class ForLoadedType implements TypeAnnotationAppender {

        private final TypeDescription typeDescription;

        public ForLoadedType(Class<?> type) {
            typeDescription = new TypeDescription.ForLoadedType(type);
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            ForInstrumentedType.INSTANCE.apply(classVisitor, this.typeDescription);
        }
    }

    static enum ForSuperType implements TypeAnnotationAppender {
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            ForInstrumentedType.INSTANCE.apply(classVisitor, typeDescription.getSupertype());
        }
    }

    static enum ForInstrumentedType implements TypeAnnotationAppender {
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            AnnotationAppender annotationAppender = new AnnotationAppender.Default(
                    new AnnotationAppender.Target.OnType(classVisitor), AnnotationAppender.Visibility.VISIBLE);
            for (Annotation annotation : typeDescription.getAnnotations()) {
                annotationAppender.append(annotation);
            }
        }
    }

    void apply(ClassVisitor classVisitor, TypeDescription typeDescription);
}

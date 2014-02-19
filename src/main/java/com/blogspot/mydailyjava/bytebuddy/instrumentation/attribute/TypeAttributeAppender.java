package com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.annotation.AnnotationAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;

import java.lang.annotation.Annotation;

public interface TypeAttributeAppender {

    static class ForAnnotation implements TypeAttributeAppender {

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

    static class ForLoadedType implements TypeAttributeAppender {

        private final TypeDescription typeDescription;

        public ForLoadedType(Class<?> type) {
            typeDescription = new TypeDescription.ForLoadedType(type);
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            ForInstrumentedType.INSTANCE.apply(classVisitor, this.typeDescription);
        }
    }

    static enum ForSuperType implements TypeAttributeAppender {
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            ForInstrumentedType.INSTANCE.apply(classVisitor, typeDescription.getSupertype());
        }
    }

    static enum ForInstrumentedType implements TypeAttributeAppender {
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

    static class Compound implements TypeAttributeAppender {

        private final TypeAttributeAppender[] typeAttributeAppender;

        public Compound(TypeAttributeAppender... typeAttributeAppender) {
            this.typeAttributeAppender = typeAttributeAppender;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription typeDescription) {
            for (TypeAttributeAppender typeAttributeAppender : this.typeAttributeAppender) {
                typeAttributeAppender.apply(classVisitor, typeDescription);
            }
        }
    }

    void apply(ClassVisitor classVisitor, TypeDescription typeDescription);
}

package net.bytebuddy.instrumentation.attribute.annotation;

public interface AnnotatedElement {

    AnnotationList getDeclaredAnnotations();

    AnnotationList getAnnotations();
}

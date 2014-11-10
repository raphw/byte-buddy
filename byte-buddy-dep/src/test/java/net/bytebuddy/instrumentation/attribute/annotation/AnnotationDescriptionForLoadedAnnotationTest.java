package net.bytebuddy.instrumentation.attribute.annotation;

import java.lang.annotation.Annotation;

public class AnnotationDescriptionForLoadedAnnotationTest extends AbstractAnnotationDescriptionTest {

    @Override
    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        return AnnotationDescription.ForLoadedAnnotation.of(annotation);
    }
}

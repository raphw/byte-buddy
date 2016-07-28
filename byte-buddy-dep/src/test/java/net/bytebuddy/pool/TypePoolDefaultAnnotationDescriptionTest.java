package net.bytebuddy.pool;

import net.bytebuddy.description.annotation.AbstractAnnotationDescriptionTest;
import net.bytebuddy.description.annotation.AnnotationDescription;
import org.junit.After;
import org.junit.Before;

import java.lang.annotation.Annotation;

public class TypePoolDefaultAnnotationDescriptionTest extends AbstractAnnotationDescriptionTest {

    @Override
    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        TypePool typePool = TypePool.Default.of(declaringType.getClassLoader());
        try {
            return typePool.describe(declaringType.getName()).resolve()
                    .getDeclaredAnnotations().ofType(annotation.annotationType());
        } finally {
            typePool.clear();
        }
    }
}

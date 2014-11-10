package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.attribute.annotation.AbstractAnnotationDescriptionTest;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import org.junit.After;
import org.junit.Before;

import java.lang.annotation.Annotation;

public class TypePoolDefaultAnnotationDescription extends AbstractAnnotationDescriptionTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        return typePool.describe(declaringType.getName()).getDeclaredAnnotations().ofType(annotation.annotationType());
    }
}

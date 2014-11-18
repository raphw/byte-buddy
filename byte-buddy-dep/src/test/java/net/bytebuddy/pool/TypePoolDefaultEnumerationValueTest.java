package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.attribute.annotation.AbstractEnumerationValueTest;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.After;
import org.junit.Before;

public class TypePoolDefaultEnumerationValueTest extends AbstractEnumerationValueTest {

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
    protected AnnotationDescription.EnumerationValue describe(Enum<?> enumeration,
                                                              Class<?> carrierType,
                                                              MethodDescription annotationMethod) {
        TypeDescription typeDescription = typePool.describe(carrierType.getName());
        for (AnnotationDescription annotationDescription : typeDescription.getDeclaredAnnotations()) {
            if (annotationDescription.getAnnotationType().equals(annotationDescription.getAnnotationType())) {
                return annotationDescription.getValue(annotationMethod, AnnotationDescription.EnumerationValue.class);
            }
        }
        throw new AssertionError();
    }
}

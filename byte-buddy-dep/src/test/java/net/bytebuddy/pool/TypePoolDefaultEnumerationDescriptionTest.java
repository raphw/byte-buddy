package net.bytebuddy.pool;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.AbstractEnumerationDescriptionTest;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.After;
import org.junit.Before;

public class TypePoolDefaultEnumerationDescriptionTest extends AbstractEnumerationDescriptionTest {

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
    protected EnumerationDescription describe(Enum<?> enumeration,
                                              Class<?> carrierType,
                                              MethodDescription.InDefinedShape annotationMethod) {
        TypeDescription typeDescription = typePool.describe(carrierType.getName()).resolve();
        for (AnnotationDescription annotationDescription : typeDescription.getDeclaredAnnotations()) {
            if (annotationDescription.getAnnotationType().equals(annotationDescription.getAnnotationType())) {
                return annotationDescription.getValue(annotationMethod, EnumerationDescription.class);
            }
        }
        throw new AssertionError();
    }
}

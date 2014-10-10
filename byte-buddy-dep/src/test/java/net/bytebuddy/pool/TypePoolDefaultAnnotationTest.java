package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class TypePoolDefaultAnnotationTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @Test
    public void testSimpleAnnotationExtraction() throws Exception {
        TypeDescription typeDescription = typePool.describe(SimpleAnnotatedType.class.getName());
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Empty {
        /* */
    }

    @Empty
    private static class SimpleAnnotatedType {
        /* empty */
    }
}

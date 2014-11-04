package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolDefaultAnnotationTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @Test
    public void testSimpleAnnotationExtraction() throws Exception {
        TypeDescription typeDescription = typePool.describe(SimpleAnnotatedType.class.getName());
        assertThat(typeDescription.getDeclaredAnnotations().isAnnotationPresent(Simple.class), is(true));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Simple {
        /* */
    }

    @Simple
    private static class SimpleAnnotatedType {
        /* empty */
    }
}

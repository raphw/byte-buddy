package net.bytebuddy.description.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationDescriptionForLoadedAnnotationTest extends AbstractAnnotationDescriptionTest {

    private static final String FOO = "foo";

    @Override
    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        return AnnotationDescription.ForLoadedAnnotation.of(annotation);
    }

    @Test
    public void testAnnotationNonVisible() throws Exception {
        assertThat(describe(Carrier.class.getAnnotation(PrivateAnnotation.class), Carrier.class)
                .getValue(new MethodDescription.ForLoadedMethod(PrivateAnnotation.class.getDeclaredMethod("value")),
                        String.class), is(FOO));
    }

    @Test
    public void testAnnotationNonVisibleAccessible() throws Exception {
        Method method = PrivateAnnotation.class.getDeclaredMethod("value");
        method.setAccessible(true);
        assertThat(describe(Carrier.class.getAnnotation(PrivateAnnotation.class), Carrier.class)
                .getValue(new MethodDescription.ForLoadedMethod(method), String.class), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testInoperational() throws Exception {
        describe(PrivateAnnotation.Defect.INSTANCE, Carrier.class)
                .getValue(new MethodDescription.ForLoadedMethod(PrivateAnnotation.class.getDeclaredMethod("value")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadAnnotationWrongClassLoader() throws Exception {
        describe(Carrier.class.getAnnotation(PrivateAnnotation.class), Carrier.class).prepare(PrivateAnnotation.class).load(ClassLoadingStrategy.BOOTSTRAP_LOADER);
    }

    @Test
    public void testLoadAnnotationSubClassLoader() throws Exception {
        assertThat(describe(Carrier.class.getAnnotation(PrivateAnnotation.class), Carrier.class).prepare(PrivateAnnotation.class)
                .load(new URLClassLoader(new URL[0], getClass().getClassLoader())), is(Carrier.class.getAnnotation(PrivateAnnotation.class)));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface PrivateAnnotation {

        String value();

        enum Defect implements PrivateAnnotation {

            INSTANCE;

            @Override
            public String value() {
                throw new RuntimeException();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return PrivateAnnotation.class;
            }
        }
    }

    @PrivateAnnotation(FOO)
    private static class Carrier {

    }
}

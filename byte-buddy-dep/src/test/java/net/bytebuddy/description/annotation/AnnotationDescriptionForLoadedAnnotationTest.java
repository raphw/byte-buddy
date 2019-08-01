package net.bytebuddy.description.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationDescriptionForLoadedAnnotationTest extends AbstractAnnotationDescriptionTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    protected AnnotationDescription describe(Annotation annotation, Class<?> declaringType) {
        return AnnotationDescription.ForLoadedAnnotation.of(annotation);
    }

    @Test
    public void testAnnotationNonVisible() throws Exception {
        assertThat(describe(Carrier.class.getAnnotation(PrivateAnnotation.class), Carrier.class)
                .getValue(new MethodDescription.ForLoadedMethod(PrivateAnnotation.class.getDeclaredMethod("value"))).resolve(String.class), is(FOO));
    }

    @Test
    public void testAnnotationNonVisibleAccessible() throws Exception {
        Method method = PrivateAnnotation.class.getDeclaredMethod("value");
        method.setAccessible(true);
        assertThat(describe(Carrier.class.getAnnotation(PrivateAnnotation.class), Carrier.class)
                .getValue(new MethodDescription.ForLoadedMethod(method)).resolve(String.class), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testInoperational() throws Exception {
        describe(PrivateAnnotation.Defect.INSTANCE, Carrier.class)
                .getValue(new MethodDescription.ForLoadedMethod(PrivateAnnotation.class.getDeclaredMethod("value")));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface PrivateAnnotation {

        String value();

        enum Defect implements PrivateAnnotation {

            INSTANCE;

            public String value() {
                throw new RuntimeException();
            }

            public Class<? extends Annotation> annotationType() {
                return PrivateAnnotation.class;
            }
        }
    }

    @PrivateAnnotation(FOO)
    private static class Carrier {

    }
}

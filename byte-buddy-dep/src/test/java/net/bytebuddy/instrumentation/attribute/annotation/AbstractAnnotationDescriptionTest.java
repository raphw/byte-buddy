package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.utility.PropertyDispatcher;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public abstract class AbstractAnnotationDescriptionTest<T extends Annotation> {

    protected abstract T first();

    protected abstract T second();

    protected abstract AnnotationDescription firstValue();

    protected abstract AnnotationDescription secondValue();

    @Test
    public void testPrecondition() throws Exception {
        assertThat(first(), not(equalTo(second())));
        assertThat(first(), equalTo(first()));
        assertThat(second(), equalTo(second()));
        assertEquals(second().annotationType(), first().annotationType());
        assertThat(firstValue().getAnnotationType(), equalTo(secondValue().getAnnotationType()));
        assertThat(firstValue().getAnnotationType().represents(first().annotationType()), is(true));
        assertThat(secondValue().getAnnotationType().represents(second().annotationType()), is(true));
    }

    @Test
    public void assertToString() throws Exception {
        assertToString(firstValue().toString(), first());
        assertToString(secondValue().toString(), second());
    }

    private void assertToString(String actual, T loaded) throws Exception {
        assertThat(actual, startsWith("@" + loaded.annotationType().getName()));
        for (Method method : loaded.annotationType().getDeclaredMethods()) {
            assertThat(actual, containsString(method.getName() + "="
                    + PropertyDispatcher.of(method.getReturnType()).toString(method.invoke(loaded))));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMethod() throws Exception {
        firstValue().getValue(new MethodDescription.ForLoadedMethod(Object.class.getMethod("toString")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalPreparation() throws Exception {
        firstValue().prepare(Annotation.class);
    }
}

package net.bytebuddy.instrumentation.attribute.annotation;

import org.junit.Test;

import java.lang.annotation.Annotation;

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
        assertThat(firstValue().toString(), is(first().toString()));
        assertThat(secondValue().toString(), is(second().toString()));
    }

}

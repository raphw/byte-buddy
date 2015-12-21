package net.bytebuddy.implementation.bind.annotation;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractAnnotationTest<T extends Annotation> {

    protected final Class<T> annotationType;

    protected AbstractAnnotationTest(Class<T> annotationType) {
        this.annotationType = annotationType;
    }

    @Test
    public void testAnnotationVisibility() throws Exception {
        assertThat(annotationType.isAnnotationPresent(Retention.class), is(true));
        assertThat(annotationType.getAnnotation(Retention.class).value(), is(RetentionPolicy.RUNTIME));
    }
}

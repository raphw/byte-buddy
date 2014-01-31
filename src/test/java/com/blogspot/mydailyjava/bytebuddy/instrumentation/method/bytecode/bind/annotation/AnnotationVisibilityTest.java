package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AnnotationVisibilityTest {

    @Test
    public void testRuntimeVisibility() throws Exception {
        testRuntimeVisibilityOf(RuntimeType.class);
        testRuntimeVisibilityOf(IgnoreForBinding.class);
        testRuntimeVisibilityOf(Argument.class);
        testRuntimeVisibilityOf(AllArguments.class);
        testRuntimeVisibilityOf(This.class);
    }

    private static void testRuntimeVisibilityOf(Class<? extends Annotation> annotationType)  {
        assertThat(annotationType.isAnnotationPresent(Retention.class), is(true));
        assertThat(annotationType.getAnnotation(Retention.class).value(), is(RetentionPolicy.RUNTIME));
    }
}

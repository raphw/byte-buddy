package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class AnnotationVisibilityTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {AllArguments.class},
                {Argument.class},
                {BindingPriority.class},
                {IgnoreForBinding.class},
                {Origin.class},
                {RuntimeType.class},
                {Super.class},
                {SuperCall.class},
                {This.class}
        });
    }

    private final Class<?> annotationType;

    public AnnotationVisibilityTest(Class<?> annotationType) {
        this.annotationType = annotationType;
    }

    @Test
    public void testRuntimeVisibility() {
        assertThat(annotationType.isAnnotationPresent(Retention.class), is(true));
        assertThat(annotationType.getAnnotation(Retention.class).value(), is(RetentionPolicy.RUNTIME));
    }
}

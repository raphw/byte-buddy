package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MemberAttributeExtensionTest {

    private static final String FOO = "foo";

    @Test
    public void testFieldAnnotation() throws Exception {
        assertThat(new ByteBuddy()
                .redefine(SampleClass.class)
                .name(SampleClass.class.getName() + "$renamed")
                .visit(new MemberAttributeExtension.ForField().annotate(new SampleAnnotation.Instance()).on(named(FOO)))
                .make()
                .load(SampleClass.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getField(FOO)
                .isAnnotationPresent(SampleAnnotation.class), is(true));
    }

    @Test
    public void testMethodAnnotation() throws Exception {
        assertThat(new ByteBuddy()
                .redefine(SampleClass.class)
                .name(SampleClass.class.getName() + "$renamed")
                .visit(new MemberAttributeExtension.ForMethod().annotateMethod(new SampleAnnotation.Instance()).on(named(FOO)))
                .make()
                .load(SampleClass.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getMethod(FOO, Void.class)
                .isAnnotationPresent(SampleAnnotation.class), is(true));
    }

    @Test
    public void testMethodParameterAnnotation() throws Exception {
        assertThat(new ByteBuddy()
                .redefine(SampleClass.class)
                .name(SampleClass.class.getName() + "$renamed")
                .visit(new MemberAttributeExtension.ForMethod().annotateParameter(0, new SampleAnnotation.Instance()).on(named(FOO)))
                .make()
                .load(SampleClass.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getMethod(FOO, Void.class)
                .getParameterAnnotations()[0][0], instanceOf(SampleAnnotation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalParameterIndex() throws Exception {
        new MemberAttributeExtension.ForMethod().annotateParameter(-1, new SampleAnnotation.Instance());
    }

    public static class SampleClass {

        public Object foo;

        public void foo(Void foo) {
            /* empty */
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface SampleAnnotation {

        class Instance implements SampleAnnotation {

            @Override
            public Class<? extends Annotation> annotationType() {
                return SampleAnnotation.class;
            }
        }
    }
}

package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationRemovalTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testRemoval() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(0));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(0));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(0));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(0));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(0));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(0));
    }

    @Test
    public void testRemovalOnType() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onType())
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(0));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(1));
    }

    @Test
    public void testRemovalOnField() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onFields(named(FOO)))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(0));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(1));
    }

    @Test
    public void testRemovalOnConstructor() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onConstructors(any()))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(0));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(1));
    }

    @Test
    public void testRemovalOnConstructorParameters() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onConstructorParameters(any()))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(0));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(1));
    }

    @Test
    public void testRemovalOnConstructorParameter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onConstructorParameter(any(), 0))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(0));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(1));
    }

    @Test
    public void testRemovalOnConstructorWithParameter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onConstructorsAndParameters(any()))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(0));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(0));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(1));
    }

    @Test
    public void testRemovalOnMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onMethods(named(BAR)))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(0));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(1));
    }

    @Test
    public void testRemovalOnMethodParameters() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onMethodParameters(named(BAR)))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(0));
    }

    @Test
    public void testRemovalOnMethodParameter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onMethodParameter(named(BAR), 0))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(0));
    }

    @Test
    public void testRemovalOnMethodAndParameters() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(AnnotationRemoval.strip(ElementMatchers.annotationType(named(SampleAnnotation.class.getName()))).onMethodsAndParameters(named(BAR)))
                .make()
                .load(SampleAnnotation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST_PERSISTENT)
                .getLoaded();
        assertThat(type.getAnnotations().length, is(1));
        assertThat(type.getDeclaredField(FOO).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getAnnotations().length, is(1));
        assertThat(type.getDeclaredConstructor(Void.class).getParameterAnnotations()[0].length, is(1));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getAnnotations().length, is(0));
        assertThat(type.getDeclaredMethod(BAR, Void.class).getParameterAnnotations()[0].length, is(0));
    }

    @SampleAnnotation
    private static class Sample {

        @SampleAnnotation
        private Void foo;

        @SampleAnnotation
        private Sample(@SampleAnnotation Void ignored) {
        }

        @SampleAnnotation
        private void bar(@SampleAnnotation Void ignored) {
            /* empty */
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SampleAnnotation {
        /* empty */
    }
}

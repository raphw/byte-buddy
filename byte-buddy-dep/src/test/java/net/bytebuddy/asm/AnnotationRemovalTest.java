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
        assertThat(type.getDeclaredConstructor().getAnnotations().length, is(0));
        assertThat(type.getDeclaredMethod(BAR).getAnnotations().length, is(0));
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
        assertThat(type.getDeclaredConstructor().getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR).getAnnotations().length, is(1));
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
        assertThat(type.getDeclaredConstructor().getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR).getAnnotations().length, is(1));
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
        assertThat(type.getDeclaredConstructor().getAnnotations().length, is(0));
        assertThat(type.getDeclaredMethod(BAR).getAnnotations().length, is(1));
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
        assertThat(type.getDeclaredConstructor().getAnnotations().length, is(1));
        assertThat(type.getDeclaredMethod(BAR).getAnnotations().length, is(0));
    }

    @SampleAnnotation
    private static class Sample {

        @SampleAnnotation
        private Void foo;

        @SampleAnnotation
        private Sample() {
        }

        private void foo(@SampleAnnotation Void ignored) {
            /* empty */
        }

        @SampleAnnotation
        private void bar() {
            /* empty */
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface SampleAnnotation {
        /* empty */
    }
}

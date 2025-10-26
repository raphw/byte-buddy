package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class SafeVarargsPluginTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(8)
    public void testSafeVarargs() throws Exception {
        Class<?> transformed = new SafeVarargsPlugin().apply(new ByteBuddy().redefine(Sample.class),
                        TypeDescription.ForLoadedType.of(Sample.class),
                        ClassFileLocator.ForClassLoader.of(Sample.class.getClassLoader()))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> safeVarargs = (Class<? extends Annotation>) Class.forName("java.lang.SafeVarargs");
        assertThat(transformed.getMethod(FOO, Object[].class).getAnnotation(safeVarargs), notNullValue(Annotation.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testSafeVarargsOnNoVarargs() {
        new SafeVarargsPlugin().apply(new ByteBuddy().redefine(NoVarargs.class),
                        TypeDescription.ForLoadedType.of(NoVarargs.class),
                        ClassFileLocator.ForClassLoader.of(NoVarargs.class.getClassLoader()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testSafeVarargsOnNonFinal() {
        new SafeVarargsPlugin().apply(new ByteBuddy().redefine(NotFinal.class),
                        TypeDescription.ForLoadedType.of(NotFinal.class),
                        ClassFileLocator.ForClassLoader.of(NotFinal.class.getClassLoader()))
                .make();
    }

    public static class Sample<T> {

        @SafeVarargsPlugin.Enhance
        @SuppressWarnings("unchecked")
        public final void foo(T... ignored) {
            // empty
        }
    }

    public static class NoVarargs {

        @SafeVarargsPlugin.Enhance
        public final void foo() {
            // empty
        }
    }

    public static class NotFinal<T> {

        @SafeVarargsPlugin.Enhance
        @SuppressWarnings("unchecked")
        public void foo(T... ignored) {
            // empty
        }
    }
}

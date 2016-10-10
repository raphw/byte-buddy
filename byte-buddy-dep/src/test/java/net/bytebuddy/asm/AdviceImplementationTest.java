package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AdviceImplementationTest {

    private static final String FOO = "foo";

    @Test
    public void testAbstractMethod() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(Advice.to(Foo.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .foo(), is(FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testActualMethod() throws Exception {
        new ByteBuddy()
                .subclass(Bar.class)
                .method(named(FOO))
                .intercept(Advice.to(Bar.class))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .foo();
    }

    @Test
    public void testActualMethodReplaced() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Qux.class)
                .method(named(FOO))
                .intercept(Advice.to(Qux.class).replaceExisting())
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .foo(), is(FOO));
    }

    public abstract static class Foo {

        public abstract String foo();

        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) String returned) {
            returned = FOO;
        }
    }

    public static class Bar {

        public void foo() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static void exit(@Advice.Thrown(readOnly = false) Throwable throwable) {
            if (!(throwable instanceof RuntimeException)) {
                throw new AssertionError();
            }
            throwable = new IllegalStateException();
        }
    }

    public static class Qux {

        public String foo() {
            throw new AssertionError();
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) String returned) {
            if (returned != null) {
                throw new AssertionError();
            }
            returned = FOO;
        }
    }
}

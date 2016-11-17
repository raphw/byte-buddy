package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.StubMethod;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceImplementationTest {

    private static final String FOO = "foo";

    @Test(expected = IllegalStateException.class)
    public void testAbstractMethod() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(named(FOO))
                .intercept(Advice.to(Foo.class))
                .make();
    }

    @Test
    public void testActualMethod() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Bar.class)
                .method(named(FOO))
                .intercept(Advice.to(Bar.class))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .foo(), is((Object) FOO));
    }

    @Test
    public void testExplicitWrap() throws Exception {
        assertThat(new ByteBuddy()
                .subclass(Qux.class)
                .method(named(FOO))
                .intercept(Advice.to(Qux.class).wrap(StubMethod.INSTANCE))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .foo(), is(FOO));
    }

    @Test
    public void testExplicitWrapMultiple() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Baz.class)
                .method(named(FOO))
                .intercept(Advice.to(Baz.class).wrap(Advice.to(Baz.class).wrap(StubMethod.INSTANCE)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object baz = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredMethod(FOO).invoke(baz), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).getInt(null), is(2));
    }

    public abstract static class Foo {

        public abstract String foo();

        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) String returned) {
            returned = FOO;
        }
    }

    public static class Bar {

        public Object foo() {
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        public static void exit(@Advice.Thrown(readOnly = false) Throwable throwable, @Advice.Return(readOnly = false) Object returned) {
            if (!(throwable instanceof RuntimeException)) {
                throw new AssertionError();
            }
            throwable = null;
            returned = FOO;
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

    public static class Baz {

        public static int foo;

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit
        public static void exit() {
            foo += 1;
        }
    }
}

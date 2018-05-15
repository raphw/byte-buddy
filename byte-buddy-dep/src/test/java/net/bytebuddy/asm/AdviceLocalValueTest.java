package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceLocalValueTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String ENTER = "enter", EXIT = "exit";

    @Test
    public void testAdviceWithLocalValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(LocalValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithTwoLocalValues() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(LocalValueTwoParametersAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @SuppressWarnings("unused")
    public static class Sample {

        public static int enter, exit;

        public String foo() {
            return FOO;
        }
    }

    @SuppressWarnings("unused")
    public static class LocalValueAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Local(FOO) Object foo) {
            if (foo != null) {
                throw new AssertionError();
            }
            foo = FOO;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Local(FOO) Object foo) {
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
            foo = BAR;
            if (!foo.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class LocalValueTwoParametersAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Local(FOO) Object foo, @Advice.Local(BAR) Object bar) {
            if (foo != null || bar != null) {
                throw new AssertionError();
            }
            foo = FOO;
            bar = BAR;
            if (!foo.equals(FOO) || !bar.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Local(FOO) Object foo, @Advice.Local(BAR) Object bar) {
            if (!foo.equals(FOO) || !bar.equals(BAR)) {
                throw new AssertionError();
            }
            foo = BAR;
            bar = FOO;
            if (!foo.equals(BAR) || !bar.equals(FOO)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

}

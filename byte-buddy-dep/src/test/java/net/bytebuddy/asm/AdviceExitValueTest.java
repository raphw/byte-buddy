package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceExitValueTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String EXIT = "exit";

    private static final int VALUE = 42;

    @Test
    public void testAdviceWithEnterValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExitValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testEnterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ExitSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalEnterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalExitSubstitutionAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableEnterValueWritable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonAssignableExitWriteAdvice.class).on(named(FOO)))
                .make();
    }

    @SuppressWarnings("unused")
    public static class Sample {

        public static int exit;

        public String foo() {
            return FOO;
        }

        public String bar(String argument) {
            return argument;
        }
    }

    @SuppressWarnings("unused")
    public static class ExitValueAdvice {

        @Advice.OnMethodExit
        private static int exit(@Advice.Exit int value) {
            if (value != 0) {
                throw new AssertionError();
            }
            Sample.exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ExitSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static String exit(@Advice.Exit(readOnly = false) String value) {
            value = BAR;
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
            return FOO;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalExitSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static String exit(@Advice.Exit String value) {
            value = BAR;
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableExitWriteAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Exit(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonEqualExitAdvice {

        @Advice.OnMethodExit
        private static String exit(@Advice.Exit(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

}

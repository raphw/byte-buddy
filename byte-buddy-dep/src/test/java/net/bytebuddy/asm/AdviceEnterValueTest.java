package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceEnterValueTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final String ENTER = "enter", EXIT = "exit";

    private static final int VALUE = 42;

    @Test
    public void testAdviceWithEnterValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EnterValueAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testVariableMappingAdviceLarger() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AdviceWithVariableValues.class).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.getDeclaredConstructor().newInstance(), FOO + BAR + QUX + BAZ), is((Object) (FOO + BAR + QUX + BAZ)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testVariableMappingInstrumentedLarger() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AdviceWithVariableValues.class).on(named(QUX + BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX + BAZ).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR + QUX + BAZ)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testEnterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EnterSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalEnterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalEnterSubstitutionAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBindEnterToEnter() throws Exception {
        Advice.to(EnterToEnterAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableEnterValue() throws Exception {
        Advice.to(NonAssignableEnterAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableEnterValueWritable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonAssignableEnterWriteAdvice.class).on(named(FOO)))
                .make();
    }

    @SuppressWarnings("unused")
    public static class Sample {

        private Object object;

        public static int enter, exit;

        public static Throwable throwable;

        public String foo() {
            return FOO;
        }

        public String bar(String argument) {
            return argument;
        }

        public String quxbaz() {
            String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;
            return foo + bar + qux + baz;
        }
    }

    @SuppressWarnings("unused")
    public static class AdviceWithVariableValues {

        @Advice.OnMethodEnter
        private static int enter() {
            int foo = VALUE, bar = VALUE * 2;
            Sample.enter++;
            return foo + bar;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter int enter, @Advice.Return String value) {
            int foo = VALUE, bar = VALUE * 2;
            if (foo + bar != enter || !value.equals(FOO + BAR + QUX + BAZ)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class EnterValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter int value) {
            if (value != VALUE) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class EnterSubstitutionAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            return FOO;
        }

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Enter(readOnly = false) String value) {
            value = BAR;
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalEnterSubstitutionAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            return FOO;
        }

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Enter String value) {
            value = BAR;
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class EnterToEnterAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Enter Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableEnterAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableEnterWriteAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            throw new AssertionError();
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonEqualEnterAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

}

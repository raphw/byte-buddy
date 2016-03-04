package net.bytebuddy.asm.advice;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testTrivialAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(FOO), Advice.to(TrivialAdvice.class)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
    }

    @Test
    public void testAdviceWithImplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(BAR), Advice.to(ArgumentAdvice.class)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.newInstance(), BAR), is((Object) BAR));
    }

    @Test
    public void testAdviceWithExplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(QUX), Advice.to(ArgumentAdviceExplicit.class)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX, String.class, String.class).invoke(type.newInstance(), FOO, BAR), is((Object) (FOO + BAR)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdviceWithoutAnnotations() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(QUX), Advice.to(Object.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableArgument() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(QUX), Advice.to(IllegalAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonExistantArgument() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().method(named(FOO), Advice.to(IllegalAdvice.class)))
                .make();
    }

    public static class Sample {

        public String foo() {
            return FOO;
        }

        public String bar(String argument) {
            return argument;
        }

        public String qux(String arg1, String arg2) {
            return arg1 + arg2;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdvice {

        @Advice.OnMethodEnter
        private static void enter() {
            System.out.println("foo");
        }

        @Advice.OnMethodExit
        private static void exit() {
            System.out.println("bar");
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdvice {

        @Advice.OnMethodEnter
        private static void enter(String argument) {
            System.out.println(argument);
        }

        @Advice.OnMethodExit
        private static void exit(String argument) {
            System.out.println(argument);
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdviceExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(1) String argument) {
            System.out.println(argument);
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Argument(1) String argument) {
            System.out.println(argument);
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(1) Integer argument) {
            System.out.println(argument);
        }
    }
}

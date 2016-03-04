package net.bytebuddy.asm.advice;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceTest {

    private static final String FOO = "foo";

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

    public static class Sample {

        public String foo() {
            return FOO;
        }
    }

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
}
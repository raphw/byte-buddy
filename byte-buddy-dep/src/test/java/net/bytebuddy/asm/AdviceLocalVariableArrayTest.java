package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class AdviceLocalVariableArrayTest {

    @Test
    public void testDebuggingSymbols() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Foo.class)
                .visit(Advice.to(ShiftVariablesAdvice.class).on(named("method")))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        try {
            type.getMethod("method", long.class, Void.class, Void.class).invoke(type.getConstructor().newInstance(),
                    0L,
                    null,
                    null);
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getTargetException(), instanceOf(NullPointerException.class));
            String message = exception.getTargetException().getMessage();
            if (message != null) {
                assertThat(message, containsString("\"b\""));
            }
        }
    }

    public static class Foo {

        @SuppressWarnings("unused")
        public void method(long a, Void b, Void c) {
            b.hashCode();
        }
    }

    public static class ShiftVariablesAdvice {

        @Advice.OnMethodEnter
        static void enter(@Advice.Argument(1) Void ignored) {
            /* empty */
        }

        @Advice.OnMethodExit
        static void exit(@Advice.Argument(1) Void ignored) {
            /* empty */
        }
    }
}

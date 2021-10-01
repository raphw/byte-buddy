package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceAssignReturnedTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Test
    public void testAssignReturnedToArgumentScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToArgumentScalar.class)
                        .on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    public static class Sample {

        public String foo(String value) {
            return value;
        }
    }

    public static class ToArgumentScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String enter(@Advice.Argument(0) String arg) {
            if (!FOO.equals(arg)) {
                throw new AssertionError();
            }
            return BAR;
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String exit(@Advice.Argument(0) String arg) {
            if (!BAR.equals(arg)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }
}

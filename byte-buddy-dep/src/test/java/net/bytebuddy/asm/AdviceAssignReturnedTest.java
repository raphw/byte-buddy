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

    @Test
    public void testAssignReturnedToArgumentArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToArgumentArray.class)
                        .on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    public void testAssignReturnedToFieldScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldScalar.class)
                        .on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        Object instance = type.getConstructor().newInstance();
        type.getField(FOO).set(instance, FOO);
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(instance), is((Object) QUX));
    }

    @Test
    public void testAssignReturnedToFieldArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldArray.class)
                        .on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        Object instance = type.getConstructor().newInstance();
        type.getField(FOO).set(instance, FOO);
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(instance), is((Object) QUX));
    }

    @Test
    public void testAssignReturnedToReturnedScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToReturnedScalar.class)
                        .on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    public void testAssignReturnedToReturnedArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToReturnedArray.class)
                        .on(named(FOO)))
                .make()
                .load(Sample.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    public static class Sample {

        public String foo;

        public String foo(String value) {
            return value;
        }
    }

    public static class ToArgumentScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return BAR;
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String exit(@Advice.Argument(0) String argument) {
            if (!BAR.equals(argument)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }

    public static class ToArgumentArray {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(value = 0, index = 0))
        public static String[] enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return new String[]{BAR};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(value = 0, index = 0))
        public static String[] exit(@Advice.Argument(0) String argument) {
            if (!BAR.equals(argument)) {
                throw new AssertionError();
            }
            return new String[]{QUX};
        }
    }

    public static class ToFieldScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(FOO))
        public static String enter(@Advice.FieldValue(FOO) String field) {
            if (!FOO.equals(field)) {
                throw new AssertionError();
            }
            return BAR;
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(FOO))
        public static String exit(@Advice.FieldValue(FOO) String field) {
            if (!BAR.equals(field)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }

    public static class ToFieldArray {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(value = FOO, index = 0))
        public static String[] enter(@Advice.FieldValue(FOO) String field) {
            if (!FOO.equals(field)) {
                throw new AssertionError();
            }
            return new String[]{BAR};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(value = FOO, index = 0))
        public static String[] exit(@Advice.FieldValue(FOO) String field) {
            if (!BAR.equals(field)) {
                throw new AssertionError();
            }
            return new String[]{QUX};
        }
    }

    public static class ToReturnedScalar {

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToReturned
        public static String exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return BAR;
        }
    }

    public static class ToReturnedArray {

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToReturned(index = 0)
        public static String[] exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return new String[]{BAR};
        }
    }
}

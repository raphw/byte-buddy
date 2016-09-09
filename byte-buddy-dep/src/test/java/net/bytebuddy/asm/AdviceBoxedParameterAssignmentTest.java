package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.DebuggingWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceBoxedParameterAssignmentTest {

    private static final String FOO = "foo";

    private static final byte NUMERIC_VALUE = 42;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {VoidAssignment.class, null, new Object[0], new Class<?>[0]},
                {BooleanAssignment.class, true, new Object[]{false}, new Class<?>[]{boolean.class}},
                {ByteAssignment.class, NUMERIC_VALUE, new Object[]{(byte) 0}, new Class<?>[]{byte.class}},
                {ShortAssignment.class, (short) NUMERIC_VALUE, new Object[]{(short) 0}, new Class<?>[]{short.class}},
                {CharacterAssignment.class, (char) NUMERIC_VALUE, new Object[]{(char) 0}, new Class<?>[]{char.class}},
                {IntegerAssignment.class, (int) NUMERIC_VALUE, new Object[]{0}, new Class<?>[]{int.class}},
                {LongAssignment.class, (long) NUMERIC_VALUE, new Object[]{(long) 0}, new Class<?>[]{long.class}},
                {FloatAssignment.class, (float) NUMERIC_VALUE, new Object[]{(float) 0}, new Class<?>[]{float.class}},
                {DoubleAssignment.class, (double) NUMERIC_VALUE, new Object[]{(double) 0}, new Class<?>[]{double.class}},
                {ReferenceAssignment.class, FOO, new Object[]{null}, new Class<?>[]{String.class}},
                {ReferenceAssignmentNoCast.class, FOO, new Object[]{null}, new Class<?>[]{Object.class}},
        });
    }

    private final Class<?> type;

    private final Object expected;

    private final Object[] provided;

    private final Class<?>[] parameterTypes;

    public AdviceBoxedParameterAssignmentTest(Class<?> type, Object expected, Object[] provided, Class<?>[] parameterTypes) {
        this.type = type;
        this.expected = expected;
        this.provided = provided;
        this.parameterTypes = parameterTypes;
    }

    @Test
    public void testAssignment() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(type).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(FOO, parameterTypes).invoke(dynamicType.getDeclaredConstructor().newInstance(), provided), is(expected));
    }

    @SuppressWarnings("all")
    public static class VoidAssignment {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[0];
        }
    }

    @SuppressWarnings("all")
    public static class BooleanAssignment {

        public boolean foo(boolean value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{true};
        }
    }

    @SuppressWarnings("all")
    public static class ByteAssignment {

        public byte foo(byte value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{(byte) NUMERIC_VALUE};
        }
    }

    @SuppressWarnings("all")
    public static class ShortAssignment {

        public short foo(short value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{(short) NUMERIC_VALUE};
        }
    }

    @SuppressWarnings("all")
    public static class CharacterAssignment {

        public char foo(char value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{(char) NUMERIC_VALUE};
        }
    }

    @SuppressWarnings("all")
    public static class IntegerAssignment {

        public int foo(int value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{(int) NUMERIC_VALUE};
        }
    }

    @SuppressWarnings("all")
    public static class LongAssignment {

        public long foo(long value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{(long) NUMERIC_VALUE};
        }
    }

    @SuppressWarnings("all")
    public static class FloatAssignment {

        public float foo(float value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{(float) NUMERIC_VALUE};
        }
    }

    @SuppressWarnings("all")
    public static class DoubleAssignment {

        public double foo(double value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{(double) NUMERIC_VALUE};
        }
    }

    @SuppressWarnings("all")
    public static class ReferenceAssignment {

        public String foo(String value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{FOO};
        }
    }

    @SuppressWarnings("all")
    public static class ReferenceAssignmentNoCast {

        public Object foo(Object value) {
            return value;
        }

        @Advice.OnMethodEnter
        private static void enter(@Advice.BoxedArguments(readOnly = false) Object[] value) {
            value = new Object[]{FOO};
        }
    }
}

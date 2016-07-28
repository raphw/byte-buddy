package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceBoxedAssignmentTest {

    private static final String FOO = "foo";

    private static final byte NUMERIC_VALUE = 42;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {VoidAssignment.class, null},
                {BooleanAssignment.class, true},
                {ByteAssignment.class, NUMERIC_VALUE},
                {ShortAssignment.class, (short) NUMERIC_VALUE},
                {CharacterAssignment.class, (char) NUMERIC_VALUE},
                {IntegerAssignment.class, (int) NUMERIC_VALUE},
                {LongAssignment.class, (long) NUMERIC_VALUE},
                {FloatAssignment.class, (float) NUMERIC_VALUE},
                {DoubleAssignment.class, (double) NUMERIC_VALUE},
                {ReferenceAssignment.class, FOO},
        });
    }

    private final Class<?> type;

    private final Object expected;

    public AdviceBoxedAssignmentTest(Class<?> type, Object expected) {
        this.type = type;
        this.expected = expected;
    }

    @Test
    public void testAssignment() throws Exception {
        Class<?> dynamicType = new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(type).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredMethod(FOO).invoke(dynamicType.getConstructor().newInstance()), is(expected));
    }

    @SuppressWarnings("all")
    public static class VoidAssignment {

        public void foo() {
            /* empty */
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = FOO;
        }
    }

    @SuppressWarnings("all")
    public static class BooleanAssignment {

        public boolean foo() {
            return false;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = true;
        }
    }

    @SuppressWarnings("all")
    public static class ByteAssignment {

        public byte foo() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = NUMERIC_VALUE;
        }
    }

    @SuppressWarnings("all")
    public static class ShortAssignment {

        public short foo() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = (short) NUMERIC_VALUE;
        }
    }

    @SuppressWarnings("all")
    public static class CharacterAssignment {

        public char foo() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = (char) NUMERIC_VALUE;
        }
    }

    @SuppressWarnings("all")
    public static class IntegerAssignment {

        public int foo() {
            return 0;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = (int) NUMERIC_VALUE;
        }
    }

    @SuppressWarnings("all")
    public static class LongAssignment {

        public long foo() {
            return 0L;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = (long) NUMERIC_VALUE;
        }
    }

    @SuppressWarnings("all")
    public static class FloatAssignment {

        public float foo() {
            return 0f;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = (float) NUMERIC_VALUE;
        }
    }

    @SuppressWarnings("all")
    public static class DoubleAssignment {

        public double foo() {
            return 0d;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = (double) NUMERIC_VALUE;
        }
    }

    @SuppressWarnings("all")
    public static class ReferenceAssignment {

        public String foo() {
            return null;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object value) {
            value = FOO;
        }
    }
}

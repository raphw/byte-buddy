package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.fail;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceTypeTest {

    private static final String FOO = "foo", BAR = "bar", ENTER = "enter", EXIT = "exit", exception = "exception";

    private static final String FIELD = "field", MUTATED = "mutated", STATIC_FIELD = "staticField", MUTATED_STATIC_FIELD = "mutatedStatic";

    private static final byte VALUE = 42;

    private static final boolean BOOLEAN = true;

    private static final byte NUMERIC_DEFAULT = 0;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {VoidAdvice.class, null},
                {BooleanAdvice.class, BOOLEAN},
                {ByteAdvice.class, VALUE},
                {ShortAdvice.class, (short) VALUE},
                {CharacterAdvice.class, (char) VALUE},
                {IntegerAdvice.class, (int) VALUE},
                {LongAdvice.class, (long) VALUE},
                {FloatAdvice.class, (float) VALUE},
                {DoubleAdvice.class, (double) VALUE},
                {ReferenceAdvice.class, FOO}
        });
    }

    private final Class<?> advice;

    private final Object expected;

    public AdviceTypeTest(Class<?> advice, Object expected) {
        this.advice = advice;
        this.expected = expected;
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(Advice.to(advice).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is(expected));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithException() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(Advice.to(advice).on(named(BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredField(exception).set(null, true);
        try {
            assertThat(type.getDeclaredMethod(BAR).invoke(type.newInstance()), is(expected));
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @SuppressWarnings("unused")
    public static class VoidAdvice {

        public static int enter, exit;

        public static boolean exception;

        public void foo() {
            /* empty */
        }

        public void bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static void enter() {
            enter++;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Thrown Throwable throwable) {
            if (!(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanAdvice {

        public static int enter, exit;

        public static boolean exception;

        private boolean field = BOOLEAN, mutated = BOOLEAN;

        private static boolean staticField = BOOLEAN, mutatedStatic = BOOLEAN;

        public boolean foo() {
            return BOOLEAN;
        }

        public boolean bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static boolean enter(@Advice.Ignored boolean value,
                                    @Advice.FieldValue(FIELD) boolean field,
                                    @Advice.FieldValue(STATIC_FIELD) boolean staticField,
                                    @Advice.FieldValue(value = MUTATED, readOnly = false) boolean mutated,
                                    @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) boolean mutatedStatic) {
            if (value) {
                throw new AssertionError();
            }
            value = BOOLEAN;
            if (value) {
                throw new AssertionError();
            }
            if (!field || !mutated || !staticField || !mutatedStatic) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = false;
            enter++;
            return BOOLEAN;
        }

        @Advice.OnMethodExit
        public static boolean exit(@Advice.Return boolean result,
                                   @Advice.Enter boolean enter,
                                   @Advice.Thrown Throwable throwable,
                                   @Advice.FieldValue(FIELD) boolean field,
                                   @Advice.FieldValue(STATIC_FIELD) boolean staticField,
                                   @Advice.FieldValue(value = MUTATED, readOnly = false) boolean mutated,
                                   @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) boolean mutatedStatic) {
            if (result == exception
                    || !enter
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!field || mutated || !staticField || mutatedStatic) {
                throw new AssertionError();
            }
            exit++;
            return BOOLEAN;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteAdvice {

        public static int enter, exit;

        public static boolean exception;

        private byte field = NUMERIC_DEFAULT, mutated = NUMERIC_DEFAULT;

        private static byte staticField = NUMERIC_DEFAULT, mutatedStatic = NUMERIC_DEFAULT;

        public byte foo() {
            return VALUE;
        }

        public byte bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static byte enter(@Advice.Ignored byte value,
                                 @Advice.FieldValue(FIELD) byte field,
                                 @Advice.FieldValue(STATIC_FIELD) byte staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) byte mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) byte mutatedStatic) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT || staticField != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = NUMERIC_DEFAULT * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static byte exit(@Advice.Return byte result,
                                @Advice.Enter byte enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.FieldValue(FIELD) byte field,
                                @Advice.FieldValue(STATIC_FIELD) byte staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) byte mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) byte mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT * 2 || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortAdvice {

        public static int enter, exit;

        public static boolean exception;

        private short field = NUMERIC_DEFAULT, mutated = NUMERIC_DEFAULT;

        private static short staticField = NUMERIC_DEFAULT, mutatedStatic = NUMERIC_DEFAULT;

        public short foo() {
            return VALUE;
        }

        public short bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static short enter(@Advice.Ignored short value,
                                  @Advice.FieldValue(FIELD) short field,
                                  @Advice.FieldValue(STATIC_FIELD) short staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) short mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) short mutatedStatic) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = NUMERIC_DEFAULT * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static short exit(@Advice.Return short result,
                                 @Advice.Enter short enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.FieldValue(FIELD) short field,
                                 @Advice.FieldValue(STATIC_FIELD) short staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) short mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) short mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT * 2 || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterAdvice {

        public static int enter, exit;

        public static boolean exception;

        private char field = NUMERIC_DEFAULT, mutated = NUMERIC_DEFAULT;

        private static char staticField = NUMERIC_DEFAULT, mutatedStatic = NUMERIC_DEFAULT;

        public char foo() {
            return VALUE;
        }

        public char bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static char enter(@Advice.Ignored char value,
                                 @Advice.FieldValue(FIELD) char field,
                                 @Advice.FieldValue(STATIC_FIELD) char staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) char mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) char mutatedStatic) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = NUMERIC_DEFAULT * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static char exit(@Advice.Return char result,
                                @Advice.Enter char enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.FieldValue(FIELD) char field,
                                @Advice.FieldValue(STATIC_FIELD) char staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) char mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) char mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT * 2 || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerAdvice {

        public static int enter, exit;

        public static boolean exception;

        private int field = NUMERIC_DEFAULT, mutated = NUMERIC_DEFAULT;

        private static int staticField = NUMERIC_DEFAULT, mutatedStatic = NUMERIC_DEFAULT;

        public int foo() {
            return VALUE;
        }

        public int bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static int enter(@Advice.Ignored int value,
                                @Advice.FieldValue(FIELD) int field,
                                @Advice.FieldValue(STATIC_FIELD) int staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) int mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) int mutatedStatic) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = NUMERIC_DEFAULT * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static int exit(@Advice.Return int result,
                               @Advice.Enter int enter,
                               @Advice.Thrown Throwable throwable,
                               @Advice.FieldValue(FIELD) int field,
                               @Advice.FieldValue(STATIC_FIELD) int staticField,
                               @Advice.FieldValue(value = MUTATED, readOnly = false) int mutated,
                               @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) int mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT * 2 || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class LongAdvice {

        public static int enter, exit;

        public static boolean exception;

        private long field = NUMERIC_DEFAULT, mutated = NUMERIC_DEFAULT;

        private static long staticField = NUMERIC_DEFAULT, mutatedStatic = NUMERIC_DEFAULT;

        public long foo() {
            return VALUE;
        }

        public long bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static long enter(@Advice.Ignored long value,
                                 @Advice.FieldValue(FIELD) long field,
                                 @Advice.FieldValue(STATIC_FIELD) long staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) long mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) long mutatedStatic) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = NUMERIC_DEFAULT * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static long exit(@Advice.Return long result,
                                @Advice.Enter long enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.FieldValue(FIELD) long field,
                                @Advice.FieldValue(STATIC_FIELD) long staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) long mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) long mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT * 2 || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatAdvice {

        public static int enter, exit;

        public static boolean exception;

        private float field = NUMERIC_DEFAULT, mutated = NUMERIC_DEFAULT;

        private static float staticField = NUMERIC_DEFAULT, mutatedStatic = NUMERIC_DEFAULT;

        public float foo() {
            return VALUE;
        }

        public float bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static float enter(@Advice.Ignored float value,
                                  @Advice.FieldValue(FIELD) float field,
                                  @Advice.FieldValue(STATIC_FIELD) float staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) float mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) float mutatedStatic) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = NUMERIC_DEFAULT * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static float exit(@Advice.Return float result,
                                 @Advice.Enter float enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.FieldValue(FIELD) float field,
                                 @Advice.FieldValue(STATIC_FIELD) float staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) float mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) float mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT * 2 || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleAdvice {

        public static int enter, exit;

        public static boolean exception;

        private double field = NUMERIC_DEFAULT, mutated = NUMERIC_DEFAULT;

        private static double staticField = NUMERIC_DEFAULT, mutatedStatic = NUMERIC_DEFAULT;

        public double foo() {
            return VALUE;
        }

        public double bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static double enter(@Advice.Ignored double value,
                                   @Advice.FieldValue(FIELD) double field,
                                   @Advice.FieldValue(STATIC_FIELD) double staticField,
                                   @Advice.FieldValue(value = MUTATED, readOnly = false) double mutated,
                                   @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) double mutatedStatic) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = NUMERIC_DEFAULT * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static double exit(@Advice.Return double result,
                                  @Advice.Enter double enter,
                                  @Advice.Thrown Throwable throwable,
                                  @Advice.FieldValue(FIELD) double field,
                                  @Advice.FieldValue(STATIC_FIELD) double staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) double mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) double mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (field != NUMERIC_DEFAULT || mutated != NUMERIC_DEFAULT * 2 || staticField != NUMERIC_DEFAULT || mutatedStatic != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceAdvice {

        public static int enter, exit;

        public static boolean exception;

        private Object field = FOO, mutated = FOO;

        private static Object staticField = FOO, mutatedStatic = FOO;

        public Object foo() {
            return FOO;
        }

        public Object bar() {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static Object enter(@Advice.Ignored Object value,
                                   @Advice.FieldValue(FIELD) Object field,
                                   @Advice.FieldValue(STATIC_FIELD) Object staticField,
                                   @Advice.FieldValue(value = MUTATED, readOnly = false) Object mutated,
                                   @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) Object mutatedStatic) {
            if (value != null) {
                throw new AssertionError();
            }
            value = null;
            if (value != null) {
                throw new AssertionError();
            }
            if (!field.equals(FOO) || !mutated.equals(FOO) || !staticField.equals(FOO) || !mutatedStatic.equals(FOO)) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = BAR;
            enter++;
            return FOO + BAR;
        }

        @Advice.OnMethodExit
        public static Object exit(@Advice.Return Object result,
                                  @Advice.Enter Object enter,
                                  @Advice.Thrown Throwable throwable,
                                  @Advice.FieldValue(FIELD) Object field,
                                  @Advice.FieldValue(STATIC_FIELD) Object staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) Object mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) Object mutatedStatic) {
            if ((exception ? result != null : !result.equals(FOO))
                    || !enter.equals(FOO + BAR)
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!field.equals(FOO) || !mutated.equals(BAR) || !staticField.equals(FOO) || !mutatedStatic.equals(BAR)) {
                throw new AssertionError();
            }
            exit++;
            return FOO;
        }
    }
}

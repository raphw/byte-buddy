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
                {VoidAdvice.class, Void.class, null},
                {BooleanAdvice.class, boolean.class, BOOLEAN},
                {ByteAdvice.class, byte.class, VALUE},
                {ShortAdvice.class, short.class, (short) VALUE},
                {CharacterAdvice.class, char.class, (char) VALUE},
                {IntegerAdvice.class, int.class, (int) VALUE},
                {LongAdvice.class, long.class, (long) VALUE},
                {FloatAdvice.class, float.class, (float) VALUE},
                {DoubleAdvice.class, double.class, (double) VALUE},
                {ReferenceAdvice.class, Object.class, FOO}
        });
    }

    private final Class<?> advice;

    private final Class<?> type;

    private final Object value;

    public AdviceTypeTest(Class<?> advice, Class<?> type, Object value) {
        this.advice = advice;
        this.type = type;
        this.value = value;
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(Advice.to(advice).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, this.type, this.type).invoke(type.newInstance(), value, value), is(value));
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
            assertThat(type.getDeclaredMethod(BAR, this.type, this.type).invoke(type.newInstance(), value, value), is(value));
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

        public void foo(Void ignoredArgument, Void ignoredMutableArgument) {
            /* empty */
        }

        public void bar(Void ignoredArgument, Void ignoredMutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.BoxedArguments Object[] boxed) {
            if (boxed.length != 2 || boxed[0] != null || boxed[1] != null) {
                throw new AssertionError();
            }
            enter++;
        }

        @Advice.OnMethodExit
        public static void exit(@Advice.Thrown Throwable throwable,
                                @Advice.BoxedArguments Object[] boxed) {
            if (!(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || boxed[0] != null || boxed[1] != null) {
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

        public boolean foo(boolean argument, boolean mutableArgument) {
            return argument;
        }

        public boolean bar(boolean argument, boolean mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static boolean enter(@Advice.Ignored boolean value,
                                    @Advice.Argument(0) boolean argument,
                                    @Advice.Argument(value = 1, readOnly = false) boolean mutableArgument,
                                    @Advice.BoxedArguments Object[] boxed,
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
            if (!argument || !mutableArgument) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(BOOLEAN) || !boxed[1].equals(BOOLEAN)) {
                throw new AssertionError();
            }
            mutableArgument = false;
            if (boxed.length != 2 || !boxed[0].equals(BOOLEAN) || !boxed[1].equals(false)) {
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
                                   @Advice.Argument(0) boolean argument,
                                   @Advice.BoxedArguments Object[] boxed,
                                   @Advice.Argument(value = 1, readOnly = false) boolean mutableArgument,
                                   @Advice.FieldValue(FIELD) boolean field,
                                   @Advice.FieldValue(STATIC_FIELD) boolean staticField,
                                   @Advice.FieldValue(value = MUTATED, readOnly = false) boolean mutated,
                                   @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) boolean mutatedStatic) {
            if (result == exception
                    || !enter
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!argument || mutableArgument) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(BOOLEAN) || !boxed[1].equals(false)) {
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

        private byte field = VALUE, mutated = VALUE;

        private static byte staticField = VALUE, mutatedStatic = VALUE;

        public byte foo(byte argument, byte mutableArgument) {
            return argument;
        }

        public byte bar(byte argument, byte mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static byte enter(@Advice.Ignored byte value,
                                 @Advice.Argument(0) byte argument,
                                 @Advice.Argument(value = 1, readOnly = false) byte mutableArgument,
                                 @Advice.BoxedArguments Object[] boxed,
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
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(VALUE) || !boxed[1].equals(VALUE)) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            if (boxed.length != 2 || !boxed[0].equals(VALUE) || !boxed[1].equals((byte) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE  || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static byte exit(@Advice.Return byte result,
                                @Advice.Enter byte enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Argument(0) byte argument,
                                @Advice.Argument(value = 1, readOnly = false) byte mutableArgument,
                                @Advice.BoxedArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) byte field,
                                @Advice.FieldValue(STATIC_FIELD) byte staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) byte mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) byte mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(VALUE) || !boxed[1].equals((byte) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 || staticField != VALUE || mutatedStatic != VALUE * 2) {
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

        private short field = VALUE, mutated = VALUE;

        private static short staticField = VALUE, mutatedStatic = VALUE;

        public short foo(short argument, short mutableArgument) {
            return argument;
        }

        public short bar(short argument, short mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static short enter(@Advice.Ignored short value,
                                  @Advice.Argument(0) short argument,
                                  @Advice.Argument(value = 1, readOnly = false) short mutableArgument,
                                  @Advice.BoxedArguments Object[] boxed,
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
            if (argument != VALUE || mutableArgument != VALUE) {
                //throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((short) VALUE) || !boxed[1].equals((short) VALUE)) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            if (boxed.length != 2 || !boxed[0].equals((short) VALUE) || !boxed[1].equals((short) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static short exit(@Advice.Return short result,
                                 @Advice.Enter short enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.Argument(0) short argument,
                                 @Advice.Argument(value = 1, readOnly = false) short mutableArgument,
                                 @Advice.BoxedArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) short field,
                                 @Advice.FieldValue(STATIC_FIELD) short staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) short mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) short mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2) {
               throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((short) VALUE) || !boxed[1].equals((short) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 || staticField != VALUE || mutatedStatic != VALUE * 2) {
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

        private char field = VALUE, mutated = VALUE;

        private static char staticField = VALUE, mutatedStatic = VALUE;

        public char foo(char argument, char mutableArgument) {
            return argument;
        }

        public char bar(char argument, char mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static char enter(@Advice.Ignored char value,
                                 @Advice.Argument(0) char argument,
                                 @Advice.Argument(value = 1, readOnly = false) char mutableArgument,
                                 @Advice.BoxedArguments Object[] boxed,
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
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((char) VALUE) || !boxed[1].equals((char) VALUE)) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            if (boxed.length != 2 || !boxed[0].equals((char) VALUE) || !boxed[1].equals((char) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static char exit(@Advice.Return char result,
                                @Advice.Enter char enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Argument(0) char argument,
                                @Advice.Argument(value = 1, readOnly = false) char mutableArgument,
                                @Advice.BoxedArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) char field,
                                @Advice.FieldValue(STATIC_FIELD) char staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) char mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) char mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((char) VALUE) || !boxed[1].equals((char) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 || staticField != VALUE || mutatedStatic != VALUE * 2) {
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

        private int field = VALUE, mutated = VALUE;

        private static int staticField = VALUE, mutatedStatic = VALUE;

        public int foo(int argument, int mutableArgument) {
            return argument;
        }

        public int bar(int argument, int mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static int enter(@Advice.Ignored int value,
                                @Advice.Argument(0) int argument,
                                @Advice.Argument(value = 1, readOnly = false) int mutableArgument,
                                @Advice.BoxedArguments Object[] boxed,
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
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals((int) (VALUE))) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals(VALUE * 2)) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static int exit(@Advice.Return int result,
                               @Advice.Enter int enter,
                               @Advice.Thrown Throwable throwable,
                               @Advice.Argument(0) int argument,
                               @Advice.Argument(value = 1, readOnly = false) int mutableArgument,
                               @Advice.BoxedArguments Object[] boxed,
                               @Advice.FieldValue(FIELD) int field,
                               @Advice.FieldValue(STATIC_FIELD) int staticField,
                               @Advice.FieldValue(value = MUTATED, readOnly = false) int mutated,
                               @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) int mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals(VALUE * 2)) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 || staticField != VALUE || mutatedStatic != VALUE * 2) {
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

        private long field = VALUE, mutated = VALUE;

        private static long staticField = VALUE, mutatedStatic = VALUE;

        public long foo(long argument, long mutableArgument) {
            return argument;
        }

        public long bar(long argument, long mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static long enter(@Advice.Ignored long value,
                                 @Advice.Argument(0) long argument,
                                 @Advice.Argument(value = 1, readOnly = false) long mutableArgument,
                                 @Advice.BoxedArguments Object[] boxed,
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
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((long) VALUE) || !boxed[1].equals((long) (VALUE))) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            if (boxed.length != 2 || !boxed[0].equals((long) VALUE) || !boxed[1].equals((long) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static long exit(@Advice.Return long result,
                                @Advice.Enter long enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Argument(0) long argument,
                                @Advice.Argument(value = 1, readOnly = false) long mutableArgument,
                                @Advice.BoxedArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) long field,
                                @Advice.FieldValue(STATIC_FIELD) long staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) long mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) long mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((long) VALUE) || !boxed[1].equals((long) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 || staticField != VALUE || mutatedStatic != VALUE * 2) {
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

        private float field = VALUE, mutated = VALUE;

        private static float staticField = VALUE, mutatedStatic = VALUE;

        public float foo(float argument, float mutableArgument) {
            return argument;
        }

        public float bar(float argument, float mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static float enter(@Advice.Ignored float value,
                                  @Advice.Argument(0) float argument,
                                  @Advice.Argument(value = 1, readOnly = false) float mutableArgument,
                                  @Advice.BoxedArguments Object[] boxed,
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
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((float) VALUE) || !boxed[1].equals((float) (VALUE))) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            if (boxed.length != 2 || !boxed[0].equals((float) VALUE) || !boxed[1].equals((float) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static float exit(@Advice.Return float result,
                                 @Advice.Enter float enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.Argument(0) float argument,
                                 @Advice.Argument(value = 1, readOnly = false) float mutableArgument,
                                 @Advice.BoxedArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) float field,
                                 @Advice.FieldValue(STATIC_FIELD) float staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) float mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) float mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((float) VALUE) || !boxed[1].equals((float) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 || staticField != VALUE || mutatedStatic != VALUE * 2) {
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

        private double field = VALUE, mutated = VALUE;

        private static double staticField = VALUE, mutatedStatic = VALUE;

        public double foo(double argument, double mutableArgument) {
            return argument;
        }

        public double bar(double argument, double mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static double enter(@Advice.Ignored double value,
                                   @Advice.Argument(0) double argument,
                                   @Advice.Argument(value = 1, readOnly = false) double mutableArgument,
                                   @Advice.BoxedArguments Object[] boxed,
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
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((double) VALUE) || !boxed[1].equals((double) (VALUE))) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            if (boxed.length != 2 || !boxed[0].equals((double) VALUE) || !boxed[1].equals((double) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit
        public static double exit(@Advice.Return double result,
                                  @Advice.Enter double enter,
                                  @Advice.Thrown Throwable throwable,
                                  @Advice.Argument(0) double argument,
                                  @Advice.Argument(value = 1, readOnly = false) double mutableArgument,
                                  @Advice.BoxedArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) double field,
                                  @Advice.FieldValue(STATIC_FIELD) double staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) double mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) double mutatedStatic) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((double) VALUE) || !boxed[1].equals((double) (VALUE * 2))) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 || staticField != VALUE || mutatedStatic != VALUE * 2) {
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

        public Object foo(Object argument, Object mutableArgument) {
            return argument;
        }

        public Object bar(Object argument, Object mutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static Object enter(@Advice.Ignored Object value,
                                   @Advice.Argument(0) Object argument,
                                   @Advice.Argument(value = 1, readOnly = false) Object mutableArgument,
                                   @Advice.BoxedArguments Object[] boxed,
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
            if (!argument.equals(FOO) || !mutableArgument.equals(FOO)) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(FOO) || !boxed[1].equals(FOO)) {
                throw new AssertionError();
            }
            mutableArgument = BAR;
            if (boxed.length != 2 || !boxed[0].equals(FOO) || !boxed[1].equals(BAR)) {
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
                                  @Advice.Argument(0) Object argument,
                                  @Advice.Argument(value = 1, readOnly = false) Object mutableArgument,
                                  @Advice.BoxedArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) Object field,
                                  @Advice.FieldValue(STATIC_FIELD) Object staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) Object mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) Object mutatedStatic) {
            if ((exception ? result != null : !result.equals(FOO))
                    || !enter.equals(FOO + BAR)
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!argument.equals(FOO) || !mutableArgument.equals(BAR)) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(FOO) || !boxed[1].equals(BAR)) {
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

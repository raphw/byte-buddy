package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.pool.TypePool;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

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
                {VoidInlineAdvice.class, Void.class, null},
                {VoidDelegatingAdvice.class, Void.class, null},
                {BooleanInlineAdvice.class, boolean.class, BOOLEAN},
                {BooleanDelegationAdvice.class, boolean.class, BOOLEAN},
                {ByteInlineAdvice.class, byte.class, VALUE},
                {ByteDelegationAdvice.class, byte.class, VALUE},
                {ShortInlineAdvice.class, short.class, (short) VALUE},
                {ShortDelegationAdvice.class, short.class, (short) VALUE},
                {CharacterInlineAdvice.class, char.class, (char) VALUE},
                {CharacterDelegationAdvice.class, char.class, (char) VALUE},
                {IntegerInlineAdvice.class, int.class, (int) VALUE},
                {IntegerDelegationAdvice.class, int.class, (int) VALUE},
                {LongInlineAdvice.class, long.class, (long) VALUE},
                {LongDelegationAdvice.class, long.class, (long) VALUE},
                {FloatInlineAdvice.class, float.class, (float) VALUE},
                {FloatDelegationAdvice.class, float.class, (float) VALUE},
                {DoubleInlineAdvice.class, double.class, (double) VALUE},
                {DoubleDelegationAdvice.class, double.class, (double) VALUE},
                {ReferenceInlineAdvice.class, Object.class, FOO},
                {ReferenceDelegationAdvice.class, Object.class, FOO}
        });
    }

    private final Class<?> advice;

    private final Class<?> type;

    private final Serializable value;

    public AdviceTypeTest(Class<?> advice, Class<?> type, Serializable value) {
        this.advice = advice;
        this.type = type;
        this.value = value;
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(new SerializationAssertion())
                .visit(Advice.withCustomMapping()
                        .bind(CustomAnnotation.class, value)
                        .to(advice)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, this.type, this.type).invoke(type.getDeclaredConstructor().newInstance(), value, value), is((Object) value));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithException() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(new SerializationAssertion())
                .visit(Advice.withCustomMapping()
                        .bind(CustomAnnotation.class, value)
                        .to(advice)
                        .on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredField(exception).set(null, true);
        try {
            assertThat(type.getDeclaredMethod(BAR, this.type, this.type).invoke(type.getDeclaredConstructor().newInstance(), value, value), is((Object) value));
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithProperty() throws Exception {
        if (type == Void.class) {
            return; // No void property on annotations.
        }
        Class<?> type = new ByteBuddy()
                .redefine(advice)
                .visit(new SerializationAssertion())
                .visit(Advice.withCustomMapping()
                        .bindProperty(CustomAnnotation.class, this.type.getSimpleName().toLowerCase(Locale.US) + "Value")
                        .to(advice)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, this.type, this.type).invoke(type.getDeclaredConstructor().newInstance(), value, value), is((Object) value));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @SuppressWarnings("unused")
    public static class VoidInlineAdvice {

        public static int enter, exit;

        public static boolean exception;

        public void foo(Void ignoredArgument, Void ignoredMutableArgument) {
            /* empty */
        }

        public void bar(Void ignoredArgument, Void ignoredMutableArgument) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter
        public static void enter(@Advice.AllArguments Object[] boxed,
                                 @Advice.StubValue Object stubValue,
                                 @CustomAnnotation Void custom) {
            if (boxed.length != 2 || boxed[0] != null || boxed[1] != null) {
                throw new AssertionError();
            }
            if (stubValue != null) {
                throw new AssertionError();
            }
            if (custom != null) {
                throw new AssertionError();
            }
            enter++;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static void exit(@Advice.Thrown Throwable throwable,
                                @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object boxedReturn,
                                @Advice.AllArguments Object[] boxed,
                                @CustomAnnotation Void custom) {
            if (!(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (boxedReturn != null) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || boxed[0] != null || boxed[1] != null) {
                throw new AssertionError();
            }
            if (custom != null) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class VoidDelegatingAdvice {

        public static int enter, exit;

        public static boolean exception;

        public void foo(Void ignoredArgument, Void ignored) {
            /* empty */
        }

        public void bar(Void ignoredArgument, Void ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static void enter(@Advice.AllArguments Object[] boxed,
                                 @Advice.StubValue Object stubValue,
                                 @CustomAnnotation Void custom) {
            if (boxed.length != 2 || boxed[0] != null || boxed[1] != null) {
                throw new AssertionError();
            }
            if (stubValue != null) {
                throw new AssertionError();
            }
            if (custom != null) {
                throw new AssertionError();
            }
            enter++;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static void exit(@Advice.Thrown Throwable throwable,
                                @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object boxedReturn,
                                @Advice.AllArguments Object[] boxed,
                                @CustomAnnotation Void custom) {
            if (!(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (boxedReturn != null) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || boxed[0] != null || boxed[1] != null) {
                throw new AssertionError();
            }
            if (custom != null) {
                throw new AssertionError();
            }
            exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanInlineAdvice {

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
        public static boolean enter(@Advice.Unused boolean value,
                                    @Advice.StubValue Object stubValue,
                                    @Advice.Argument(0) boolean argument,
                                    @Advice.Argument(value = 1, readOnly = false) boolean mutableArgument,
                                    @Advice.AllArguments Object[] boxed,
                                    @Advice.FieldValue(FIELD) boolean field,
                                    @Advice.FieldValue(STATIC_FIELD) boolean staticField,
                                    @Advice.FieldValue(value = MUTATED, readOnly = false) boolean mutated,
                                    @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) boolean mutatedStatic,
                                    @CustomAnnotation boolean custom) {
            if (value) {
                throw new AssertionError();
            }
            value = BOOLEAN;
            if (value) {
                throw new AssertionError();
            }
            if ((Boolean) stubValue) {
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
            if (!custom) {
                throw new AssertionError();
            }
            enter++;
            return BOOLEAN;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static boolean exit(@Advice.Return boolean result,
                                   @Advice.StubValue Object stubValue,
                                   @Advice.Enter boolean enter,
                                   @Advice.Thrown Throwable throwable,
                                   @Advice.Return Object boxedReturn,
                                   @Advice.Argument(0) boolean argument,
                                   @Advice.AllArguments Object[] boxed,
                                   @Advice.Argument(value = 1, readOnly = false) boolean mutableArgument,
                                   @Advice.FieldValue(FIELD) boolean field,
                                   @Advice.FieldValue(STATIC_FIELD) boolean staticField,
                                   @Advice.FieldValue(value = MUTATED, readOnly = false) boolean mutated,
                                   @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) boolean mutatedStatic,
                                   @CustomAnnotation boolean custom) {
            if (result == exception
                    || !enter
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (boxedReturn.equals(exception)) {
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
            if (!custom) {
                throw new AssertionError();
            }
            exit++;
            return BOOLEAN;
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private boolean field = BOOLEAN;

        private static boolean staticField = BOOLEAN;

        public boolean foo(boolean argument, boolean ignored) {
            return argument;
        }

        public boolean bar(boolean argument, boolean ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static boolean enter(@Advice.Unused boolean value,
                                    @Advice.StubValue Object stubValue,
                                    @Advice.Argument(0) boolean argument,
                                    @Advice.AllArguments Object[] boxed,
                                    @Advice.FieldValue(FIELD) boolean field,
                                    @Advice.FieldValue(STATIC_FIELD) boolean staticField,
                                    @CustomAnnotation boolean custom) {
            if (value) {
                throw new AssertionError();
            }
            if ((Boolean) stubValue) {
                throw new AssertionError();
            }
            if (!argument) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(BOOLEAN) || !boxed[1].equals(BOOLEAN)) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(BOOLEAN) || !boxed[1].equals(BOOLEAN)) {
                throw new AssertionError();
            }
            if (!field || !staticField) {
                throw new AssertionError();
            }
            if (!custom) {
                throw new AssertionError();
            }
            enter++;
            return BOOLEAN;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static boolean exit(@Advice.Return boolean result,
                                   @Advice.Enter boolean enter,
                                   @Advice.Thrown Throwable throwable,
                                   @Advice.Return Object boxedReturn,
                                   @Advice.Argument(0) boolean argument,
                                   @Advice.AllArguments Object[] boxed,
                                   @Advice.FieldValue(FIELD) boolean field,
                                   @Advice.FieldValue(STATIC_FIELD) boolean staticField,
                                   @CustomAnnotation boolean custom) {
            if (result == exception
                    || !enter
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (boxedReturn.equals(exception)) {
                throw new AssertionError();
            }
            if (!argument) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(BOOLEAN) || !boxed[1].equals(BOOLEAN)) {
                throw new AssertionError();
            }
            if (!field || !staticField) {
                throw new AssertionError();
            }
            if (!custom) {
                throw new AssertionError();
            }
            exit++;
            return BOOLEAN;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteInlineAdvice {

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
        public static byte enter(@Advice.Unused byte value,
                                 @Advice.StubValue Object stubValue,
                                 @Advice.Argument(0) byte argument,
                                 @Advice.Argument(value = 1, readOnly = false) byte mutableArgument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) byte field,
                                 @Advice.FieldValue(STATIC_FIELD) byte staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) byte mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) byte mutatedStatic,
                                 @CustomAnnotation byte custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Byte) stubValue != NUMERIC_DEFAULT) {
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
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static byte exit(@Advice.Return byte result,
                                @Advice.Enter byte enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Return Object boxedReturn,
                                @Advice.Argument(0) byte argument,
                                @Advice.Argument(value = 1, readOnly = false) byte mutableArgument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) byte field,
                                @Advice.FieldValue(STATIC_FIELD) byte staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) byte mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) byte mutatedStatic,
                                @CustomAnnotation byte custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals(exception ? NUMERIC_DEFAULT : VALUE)) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ByteDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private byte field = VALUE;

        private static byte staticField = VALUE;

        public byte foo(byte argument, byte ignored) {
            return argument;
        }

        public byte bar(byte argument, byte ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static byte enter(@Advice.Unused byte value,
                                 @Advice.StubValue Object stubValue,
                                 @Advice.Argument(0) byte argument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) byte field,
                                 @Advice.FieldValue(STATIC_FIELD) byte staticField,
                                 @CustomAnnotation byte custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Byte) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(VALUE) || !boxed[1].equals(VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static byte exit(@Advice.Return byte result,
                                @Advice.Enter byte enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Return Object boxedReturn,
                                @Advice.Argument(0) byte argument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) byte field,
                                @Advice.FieldValue(STATIC_FIELD) byte staticField,
                                @CustomAnnotation byte custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals(exception ? NUMERIC_DEFAULT : VALUE)) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(VALUE) || !boxed[1].equals(VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortInlineAdvice {

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
        public static short enter(@Advice.Unused short value,
                                  @Advice.StubValue Object stubValue,
                                  @Advice.Argument(0) short argument,
                                  @Advice.Argument(value = 1, readOnly = false) short mutableArgument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) short field,
                                  @Advice.FieldValue(STATIC_FIELD) short staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) short mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) short mutatedStatic,
                                  @CustomAnnotation short custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Short) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static short exit(@Advice.Return short result,
                                 @Advice.Enter short enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.Return Object boxedReturn,
                                 @Advice.Argument(0) short argument,
                                 @Advice.Argument(value = 1, readOnly = false) short mutableArgument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) short field,
                                 @Advice.FieldValue(STATIC_FIELD) short staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) short mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) short mutatedStatic,
                                 @CustomAnnotation short custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((short) (exception ? NUMERIC_DEFAULT : VALUE))) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ShortDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private short field = VALUE;

        private static short staticField = VALUE;

        public short foo(short argument, short ignored) {
            return argument;
        }

        public short bar(short argument, short ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static short enter(@Advice.Unused short value,
                                  @Advice.StubValue Object stubValue,
                                  @Advice.Argument(0) short argument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) short field,
                                  @Advice.FieldValue(STATIC_FIELD) short staticField,
                                  @CustomAnnotation short custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Short) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((short) VALUE) || !boxed[1].equals((short) VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static short exit(@Advice.Return short result,
                                 @Advice.Enter short enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.Return Object boxedReturn,
                                 @Advice.Argument(0) short argument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) short field,
                                 @Advice.FieldValue(STATIC_FIELD) short staticField,
                                 @CustomAnnotation short custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((short) (exception ? NUMERIC_DEFAULT : VALUE))) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((short) VALUE) || !boxed[1].equals((short) VALUE)) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterInlineAdvice {

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
        public static char enter(@Advice.Unused char value,
                                 @Advice.StubValue Object stubValue,
                                 @Advice.Argument(0) char argument,
                                 @Advice.Argument(value = 1, readOnly = false) char mutableArgument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) char field,
                                 @Advice.FieldValue(STATIC_FIELD) char staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) char mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) char mutatedStatic,
                                 @CustomAnnotation char custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Character) stubValue != NUMERIC_DEFAULT) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static char exit(@Advice.Return char result,
                                @Advice.Enter char enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Return Object boxedReturn,
                                @Advice.Argument(0) char argument,
                                @Advice.Argument(value = 1, readOnly = false) char mutableArgument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) char field,
                                @Advice.FieldValue(STATIC_FIELD) char staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) char mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) char mutatedStatic,
                                @CustomAnnotation char custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((char) (exception ? NUMERIC_DEFAULT : VALUE))) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class CharacterDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private char field = VALUE;

        private static char staticField = VALUE;

        public char foo(char argument, char ignored) {
            return argument;
        }

        public char bar(char argument, char ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static char enter(@Advice.Unused char value,
                                 @Advice.StubValue Object stubValue,
                                 @Advice.Argument(0) char argument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) char field,
                                 @Advice.FieldValue(STATIC_FIELD) char staticField,
                                 @CustomAnnotation char custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Character) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((char) VALUE) || !boxed[1].equals((char) VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static char exit(@Advice.Return char result,
                                @Advice.Enter char enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Return Object boxedReturn,
                                @Advice.Argument(0) char argument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) char field,
                                @Advice.FieldValue(STATIC_FIELD) char staticField,
                                @CustomAnnotation char custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((char) (exception ? NUMERIC_DEFAULT : VALUE))) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((char) VALUE) || !boxed[1].equals((char) VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerInlineAdvice {

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
        public static int enter(@Advice.Unused int value,
                                @Advice.StubValue Object stubValue,
                                @Advice.Argument(0) int argument,
                                @Advice.Argument(value = 1, readOnly = false) int mutableArgument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) int field,
                                @Advice.FieldValue(STATIC_FIELD) int staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) int mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) int mutatedStatic,
                                @CustomAnnotation int custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Integer) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals((int) (VALUE))) {
                throw new AssertionError();
            }
            mutableArgument = VALUE * 2;
            mutableArgument++;
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals(VALUE * 2 + 1)) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE || staticField != VALUE || mutatedStatic != VALUE) {
                throw new AssertionError();
            }
            mutated = mutatedStatic = VALUE * 2;
            mutated++;
            mutatedStatic++;
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static int exit(@Advice.Return int result,
                               @Advice.Enter int enter,
                               @Advice.Thrown Throwable throwable,
                               @Advice.Return Object boxedReturn,
                               @Advice.Argument(0) int argument,
                               @Advice.Argument(value = 1, readOnly = false) int mutableArgument,
                               @Advice.AllArguments Object[] boxed,
                               @Advice.FieldValue(FIELD) int field,
                               @Advice.FieldValue(STATIC_FIELD) int staticField,
                               @Advice.FieldValue(value = MUTATED, readOnly = false) int mutated,
                               @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) int mutatedStatic,
                               @CustomAnnotation int custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((int) (exception ? NUMERIC_DEFAULT : VALUE))) {
                throw new AssertionError();
            }
            if (argument != VALUE || mutableArgument != VALUE * 2 + 1) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals(VALUE * 2 + 1)) {
                throw new AssertionError();
            }
            if (field != VALUE || mutated != VALUE * 2 + 1 || staticField != VALUE || mutatedStatic != VALUE * 2 + 1) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class IntegerDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private int field = VALUE;

        private static int staticField = VALUE;

        public int foo(int argument, int ignored) {
            return argument;
        }

        public int bar(int argument, int ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static int enter(@Advice.Unused int value,
                                @Advice.StubValue Object stubValue,
                                @Advice.Argument(0) int argument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) int field,
                                @Advice.FieldValue(STATIC_FIELD) int staticField,
                                @CustomAnnotation int custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Integer) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals((int) (VALUE))) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static int exit(@Advice.Return int result,
                               @Advice.Enter int enter,
                               @Advice.Thrown Throwable throwable,
                               @Advice.Return Object boxedReturn,
                               @Advice.Argument(0) int argument,
                               @Advice.AllArguments Object[] boxed,
                               @Advice.FieldValue(FIELD) int field,
                               @Advice.FieldValue(STATIC_FIELD) int staticField,
                               @CustomAnnotation int custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((int) (exception ? NUMERIC_DEFAULT : VALUE))) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((int) VALUE) || !boxed[1].equals((int) VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class LongInlineAdvice {

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
        public static long enter(@Advice.Unused long value,
                                 @Advice.StubValue Object stubValue,
                                 @Advice.Argument(0) long argument,
                                 @Advice.Argument(value = 1, readOnly = false) long mutableArgument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) long field,
                                 @Advice.FieldValue(STATIC_FIELD) long staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) long mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) long mutatedStatic,
                                 @CustomAnnotation long custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Long) stubValue != NUMERIC_DEFAULT) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static long exit(@Advice.Return long result,
                                @Advice.Enter long enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Return Object boxedReturn,
                                @Advice.Argument(0) long argument,
                                @Advice.Argument(value = 1, readOnly = false) long mutableArgument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) long field,
                                @Advice.FieldValue(STATIC_FIELD) long staticField,
                                @Advice.FieldValue(value = MUTATED, readOnly = false) long mutated,
                                @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) long mutatedStatic,
                                @CustomAnnotation long custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((long) (exception ? NUMERIC_DEFAULT : VALUE))) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class LongDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private long field = VALUE;

        private static long staticField = VALUE;

        public long foo(long argument, long ignored) {
            return argument;
        }

        public long bar(long argument, long ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static long enter(@Advice.Unused long value,
                                 @Advice.StubValue Object stubValue,
                                 @Advice.Argument(0) long argument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) long field,
                                 @Advice.FieldValue(STATIC_FIELD) long staticField,
                                 @CustomAnnotation long custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Long) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((long) VALUE) || !boxed[1].equals((long) (VALUE))) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static long exit(@Advice.Return long result,
                                @Advice.Enter long enter,
                                @Advice.Thrown Throwable throwable,
                                @Advice.Return Object boxedReturn,
                                @Advice.Argument(0) long argument,
                                @Advice.AllArguments Object[] boxed,
                                @Advice.FieldValue(FIELD) long field,
                                @Advice.FieldValue(STATIC_FIELD) long staticField,
                                @CustomAnnotation long custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((long) (exception ? NUMERIC_DEFAULT : VALUE))) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((long) VALUE) || !boxed[1].equals((long) VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatInlineAdvice {

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
        public static float enter(@Advice.Unused float value,
                                  @Advice.StubValue Object stubValue,
                                  @Advice.Argument(0) float argument,
                                  @Advice.Argument(value = 1, readOnly = false) float mutableArgument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) float field,
                                  @Advice.FieldValue(STATIC_FIELD) float staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) float mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) float mutatedStatic,
                                  @CustomAnnotation float custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Float) stubValue != NUMERIC_DEFAULT) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static float exit(@Advice.Return float result,
                                 @Advice.Enter float enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.Return Object boxedReturn,
                                 @Advice.Argument(0) float argument,
                                 @Advice.Argument(value = 1, readOnly = false) float mutableArgument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) float field,
                                 @Advice.FieldValue(STATIC_FIELD) float staticField,
                                 @Advice.FieldValue(value = MUTATED, readOnly = false) float mutated,
                                 @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) float mutatedStatic,
                                 @CustomAnnotation float custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((float) (exception ? NUMERIC_DEFAULT : VALUE))) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class FloatDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private float field = VALUE;

        private static float staticField = VALUE;

        public float foo(float argument, float ignored) {
            return argument;
        }

        public float bar(float argument, float ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static float enter(@Advice.Unused float value,
                                  @Advice.StubValue Object stubValue,
                                  @Advice.Argument(0) float argument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) float field,
                                  @Advice.FieldValue(STATIC_FIELD) float staticField,
                                  @CustomAnnotation float custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Float) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((float) VALUE) || !boxed[1].equals((float) (VALUE))) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static float exit(@Advice.Return float result,
                                 @Advice.Enter float enter,
                                 @Advice.Thrown Throwable throwable,
                                 @Advice.Return Object boxedReturn,
                                 @Advice.Argument(0) float argument,
                                 @Advice.AllArguments Object[] boxed,
                                 @Advice.FieldValue(FIELD) float field,
                                 @Advice.FieldValue(STATIC_FIELD) float staticField,
                                 @CustomAnnotation float custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((float) (exception ? NUMERIC_DEFAULT : VALUE))) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((float) VALUE) || !boxed[1].equals((float) VALUE)) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleInlineAdvice {

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
        public static double enter(@Advice.Unused double value,
                                   @Advice.StubValue Object stubValue,
                                   @Advice.Argument(0) double argument,
                                   @Advice.Argument(value = 1, readOnly = false) double mutableArgument,
                                   @Advice.AllArguments Object[] boxed,
                                   @Advice.FieldValue(FIELD) double field,
                                   @Advice.FieldValue(STATIC_FIELD) double staticField,
                                   @Advice.FieldValue(value = MUTATED, readOnly = false) double mutated,
                                   @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) double mutatedStatic,
                                   @CustomAnnotation double custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            value = VALUE;
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Double) stubValue != NUMERIC_DEFAULT) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static double exit(@Advice.Return double result,
                                  @Advice.Enter double enter,
                                  @Advice.Thrown Throwable throwable,
                                  @Advice.Return Object boxedReturn,
                                  @Advice.Argument(0) double argument,
                                  @Advice.Argument(value = 1, readOnly = false) double mutableArgument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) double field,
                                  @Advice.FieldValue(STATIC_FIELD) double staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) double mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) double mutatedStatic,
                                  @CustomAnnotation double custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((double) (exception ? NUMERIC_DEFAULT : VALUE))) {
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
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private double field = VALUE;

        private static double staticField = VALUE;

        public double foo(double argument, double ignored) {
            return argument;
        }

        public double bar(double argument, double ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static double enter(@Advice.Unused double value,
                                   @Advice.StubValue Object stubValue,
                                   @Advice.Argument(0) double argument,
                                   @Advice.AllArguments Object[] boxed,
                                   @Advice.FieldValue(FIELD) double field,
                                   @Advice.FieldValue(STATIC_FIELD) double staticField,
                                   @CustomAnnotation double custom) {
            if (value != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if ((Double) stubValue != NUMERIC_DEFAULT) {
                throw new AssertionError();
            }
            if (argument != VALUE) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((double) VALUE) || !boxed[1].equals((double) (VALUE))) {
                throw new AssertionError();
            }
            if (field != VALUE || staticField != VALUE) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            enter++;
            return VALUE * 2;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static double exit(@Advice.Return double result,
                                  @Advice.Enter double enter,
                                  @Advice.Thrown Throwable throwable,
                                  @Advice.Return Object boxedReturn,
                                  @Advice.Argument(0) double argument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) double field,
                                  @Advice.FieldValue(STATIC_FIELD) double staticField,
                                  @CustomAnnotation double custom) {
            if (result != (exception ? 0 : VALUE)
                    || enter != VALUE * 2
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (!boxedReturn.equals((double) (exception ? NUMERIC_DEFAULT : VALUE))) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals((double) VALUE) || !boxed[1].equals((double) VALUE)) {
                throw new AssertionError();
            }
            if (custom != VALUE) {
                throw new AssertionError();
            }
            exit++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceInlineAdvice {

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
        public static Object enter(@Advice.Unused Object value,
                                   @Advice.StubValue Object stubValue,
                                   @Advice.Argument(0) Object argument,
                                   @Advice.Argument(value = 1, readOnly = false) Object mutableArgument,
                                   @Advice.AllArguments Object[] boxed,
                                   @Advice.FieldValue(FIELD) Object field,
                                   @Advice.FieldValue(STATIC_FIELD) Object staticField,
                                   @Advice.FieldValue(value = MUTATED, readOnly = false) Object mutated,
                                   @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) Object mutatedStatic,
                                   @CustomAnnotation String custom) {
            if (value != null) {
                throw new AssertionError();
            }
            value = FOO;
            if (value != null) {
                throw new AssertionError();
            }
            if (stubValue != null) {
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
            if (!custom.equals(FOO)) {
                throw new AssertionError();
            }
            enter++;
            return FOO + BAR;
        }

        @Advice.OnMethodExit(onThrowable = Exception.class)
        public static Object exit(@Advice.Return Object result,
                                  @Advice.Enter Object enter,
                                  @Advice.Thrown Throwable throwable,
                                  @Advice.Return Object boxedReturn,
                                  @Advice.Argument(0) Object argument,
                                  @Advice.Argument(value = 1, readOnly = false) Object mutableArgument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) Object field,
                                  @Advice.FieldValue(STATIC_FIELD) Object staticField,
                                  @Advice.FieldValue(value = MUTATED, readOnly = false) Object mutated,
                                  @Advice.FieldValue(value = MUTATED_STATIC_FIELD, readOnly = false) Object mutatedStatic,
                                  @CustomAnnotation String custom) {
            if ((exception ? result != null : !result.equals(FOO))
                    || !enter.equals(FOO + BAR)
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (exception ? boxedReturn != null : !boxedReturn.equals(FOO)) {
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
            if (!custom.equals(FOO)) {
                throw new AssertionError();
            }
            exit++;
            return FOO;
        }
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegationAdvice {

        public static int enter, exit;

        public static boolean exception;

        private Object field = FOO;

        private static Object staticField = FOO;

        public Object foo(Object argument, Object ignored) {
            return argument;
        }

        public Object bar(Object argument, Object ignored) {
            throw new RuntimeException();
        }

        @Advice.OnMethodEnter(inline = false)
        public static Object enter(@Advice.Unused Object value,
                                   @Advice.StubValue Object stubValue,
                                   @Advice.Argument(0) Object argument,
                                   @Advice.AllArguments Object[] boxed,
                                   @Advice.FieldValue(FIELD) Object field,
                                   @Advice.FieldValue(STATIC_FIELD) Object staticField,
                                   @CustomAnnotation String custom) {
            if (value != null) {
                throw new AssertionError();
            }
            if (stubValue != null) {
                throw new AssertionError();
            }
            if (!argument.equals(FOO)) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(FOO) || !boxed[1].equals(FOO)) {
                throw new AssertionError();
            }
            if (!field.equals(FOO) || !staticField.equals(FOO)) {
                throw new AssertionError();
            }
            if (!custom.equals(FOO)) {
                throw new AssertionError();
            }
            enter++;
            return FOO + BAR;
        }

        @Advice.OnMethodExit(inline = false, onThrowable = Exception.class)
        public static Object exit(@Advice.Return Object result,
                                  @Advice.Enter Object enter,
                                  @Advice.Thrown Throwable throwable,
                                  @Advice.Return Object boxedReturn,
                                  @Advice.Argument(0) Object argument,
                                  @Advice.AllArguments Object[] boxed,
                                  @Advice.FieldValue(FIELD) Object field,
                                  @Advice.FieldValue(STATIC_FIELD) Object staticField,
                                  @CustomAnnotation String custom) {
            if ((exception ? result != null : !result.equals(FOO))
                    || !enter.equals(FOO + BAR)
                    || !(exception ? throwable instanceof RuntimeException : throwable == null)) {
                throw new AssertionError();
            }
            if (exception ? boxedReturn != null : !boxedReturn.equals(FOO)) {
                throw new AssertionError();
            }
            if (!argument.equals(FOO)) {
                throw new AssertionError();
            }
            if (boxed.length != 2 || !boxed[0].equals(FOO) || !boxed[1].equals(FOO)) {
                throw new AssertionError();
            }
            if (!field.equals(FOO) || !staticField.equals(FOO)) {
                throw new AssertionError();
            }
            if (!custom.equals(FOO)) {
                throw new AssertionError();
            }
            exit++;
            return FOO;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @SuppressWarnings("unused")
    private @interface CustomAnnotation {

        boolean booleanValue() default BOOLEAN;

        byte byteValue() default VALUE;

        short shortValue() default VALUE;

        char charValue() default VALUE;

        int intValue() default VALUE;

        long longValue() default VALUE;

        float floatValue() default VALUE;

        double doubleValue() default VALUE;

        String objectValue() default FOO;
    }

    private static class SerializationAssertion extends AsmVisitorWrapper.AbstractBase {

        @Override
        public ClassVisitor wrap(TypeDescription instrumentedType,
                                 ClassVisitor classVisitor,
                                 Implementation.Context implementationContext,
                                 TypePool typePool,
                                 FieldList<FieldDescription.InDefinedShape> fields,
                                 MethodList<?> methods,
                                 int writerFlags,
                                 int readerFlags) {
            return new SerializationClassVisitor(classVisitor);
        }

        private static class SerializationClassVisitor extends ClassVisitor {

            public SerializationClassVisitor(ClassVisitor classVisitor) {
                super(Opcodes.ASM6, classVisitor);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new SerializationMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
            }
        }

        private static class SerializationMethodVisitor extends MethodVisitor {

            public SerializationMethodVisitor(MethodVisitor methodVisitor) {
                super(Opcodes.ASM6, methodVisitor);
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                if (type.equals(Type.getInternalName(ObjectInputStream.class))) {
                    throw new AssertionError("Unexpected deserialization");
                }
                super.visitTypeInsn(opcode, type);
            }
        }
    }
}

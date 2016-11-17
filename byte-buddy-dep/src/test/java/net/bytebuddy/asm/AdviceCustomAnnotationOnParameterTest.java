package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceCustomAnnotationOnParameterTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanValue.class, boolean.class, false},
                {ByteValue.class, byte.class, (byte) 0},
                {ShortValue.class, short.class, (short) 0},
                {CharacterValue.class, char.class, (char) 0},
                {IntegerValue.class, int.class, 0},
                {LongValue.class, long.class, 0L},
                {FloatValue.class, float.class, 0f},
                {DoubleValue.class, double.class, 0d},
                {ReferenceValue.class, String.class, FOO},
        });
    }

    private final Class<?> target;

    private final Class<?> argumentType;

    private final Object expected;

    public AdviceCustomAnnotationOnParameterTest(Class<?> target, Class<?> argumentType, Object expected) {
        this.target = target;
        this.argumentType = argumentType;
        this.expected = expected;
    }

    @Test
    public void testPrimitiveField() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(target)
                .visit(Advice.withCustomMapping()
                        .bind(ArgumentValue.class, new MethodDescription.ForLoadedMethod(target.getDeclaredMethod(FOO, argumentType)).getParameters().getOnly())
                        .to(target)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, argumentType).invoke(type.getDeclaredConstructor().newInstance(), expected), is(expected));
    }

    @Test
    public void testBoxedField() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(target)
                .visit(Advice.withCustomMapping()
                        .bind(ArgumentValue.class, new MethodDescription.ForLoadedMethod(target.getDeclaredMethod(FOO, argumentType)).getParameters().getOnly())
                        .to(BoxedFieldAdvice.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, argumentType).invoke(type.getDeclaredConstructor().newInstance(), expected), is(expected));
    }

    public static class BoxedFieldAdvice {

        @Advice.OnMethodExit
        static void exit(@ArgumentValue Object value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ArgumentValue {
        /* empty */
    }

    public static class BooleanValue {

        public Object foo(boolean value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue boolean value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class ByteValue {

        public Object foo(byte value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue byte value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class ShortValue {

        public Object foo(short value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue short value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class CharacterValue {

        public Object foo(char value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue char value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class IntegerValue {

        public Object foo(int value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue int value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class LongValue {

        public Object foo(long value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue long value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class FloatValue {

        public Object foo(float value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue float value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class DoubleValue {

        public Object foo(double value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue double value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class ReferenceValue {

        public Object foo(String value) {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@ArgumentValue String value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }
}

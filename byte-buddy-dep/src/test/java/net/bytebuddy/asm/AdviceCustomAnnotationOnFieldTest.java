package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.field.FieldDescription;
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
public class AdviceCustomAnnotationOnFieldTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanValue.class, false},
                {ByteValue.class, (byte) 0},
                {ShortValue.class, (short) 0},
                {CharacterValue.class, (char) 0},
                {IntegerValue.class, 0},
                {LongValue.class, 0L},
                {FloatValue.class, 0f},
                {DoubleValue.class, 0d},
                {ReferenceValue.class, FOO},
        });
    }

    private final Class<?> target;

    private final Object expected;

    public AdviceCustomAnnotationOnFieldTest(Class<?> target, Object expected) {
        this.target = target;
        this.expected = expected;
    }

    @Test
    public void testPrimitiveField() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(target)
                .visit(Advice.withCustomMapping()
                        .bind(FieldValue.class, new FieldDescription.ForLoadedField(target.getDeclaredField(FOO)))
                        .to(target)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is(expected));
    }

    @Test
    public void testBoxedField() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(target)
                .visit(Advice.withCustomMapping()
                        .bind(FieldValue.class, new FieldDescription.ForLoadedField(target.getDeclaredField(FOO)))
                        .to(BoxedFieldAdvice.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is(expected));
    }

    public static class BoxedFieldAdvice {

        @Advice.OnMethodExit
        static void exit(@FieldValue Object value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface FieldValue {
        /* empty */
    }

    public static class BooleanValue {

        boolean foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue boolean value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class ByteValue {

        byte foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue byte value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class ShortValue {

        short foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue short value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class CharacterValue {

        char foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue char value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class IntegerValue {

        int foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue int value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class LongValue {

        long foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue long value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class FloatValue {

        float foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue float value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class DoubleValue {

        double foo;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue double value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }

    public static class ReferenceValue {

        String foo = FOO;

        public Object foo() {
            return null;
        }

        @Advice.OnMethodExit
        static void exit(@FieldValue String value, @Advice.Return(readOnly = false) Object returned) {
            returned = value;
        }
    }
}

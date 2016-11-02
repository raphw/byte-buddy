package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class AdviceBoxedFieldTest {

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

    public AdviceBoxedFieldTest(Class<?> target, Object expected) {
        this.target = target;
        this.expected = expected;
    }

    @Test
    public void testFieldValueAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(target)
                .visit(Advice.withCustomMapping()
                        .bind(FieldValue.class, target.getDeclaredField(FOO))
                        .to(FieldAdvice.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is(expected));
    }

    @Test
    public void testFieldDescriptionValueAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(target)
                .visit(Advice.withCustomMapping()
                        .bind(FieldValue.class, new FieldDescription.ForLoadedField(target.getDeclaredField(FOO)))
                        .to(FieldAdvice.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is(expected));
    }

    public static class FieldAdvice {

        @Advice.OnMethodExit
        static void foo(@FieldValue Object value, @Advice.Return(readOnly = false) Object returned) {
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
    }

    public static class ByteValue {

        byte foo;

        public Object foo() {
            return null;
        }
    }

    public static class ShortValue {

        short foo;

        public Object foo() {
            return null;
        }
    }

    public static class CharacterValue {

        char foo;

        public Object foo() {
            return null;
        }
    }

    public static class IntegerValue {

        int foo;

        public Object foo() {
            return null;
        }
    }

    public static class LongValue {

        long foo;

        public Object foo() {
            return null;
        }
    }

    public static class FloatValue {

        float foo;

        public Object foo() {
            return null;
        }
    }

    public static class DoubleValue {

        double foo;

        public Object foo() {
            return null;
        }
    }

    public static class ReferenceValue {

        Object foo = FOO;

        public Object foo() {
            return null;
        }
    }
}

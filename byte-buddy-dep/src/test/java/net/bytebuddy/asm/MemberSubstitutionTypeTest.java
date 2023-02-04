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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class MemberSubstitutionTypeTest {

    private static final String FOO = "foo", BAR = "bar", RUN = "run";

    private final Class<?> type;

    private final Object value, replacement;

    public MemberSubstitutionTypeTest(Class<?> type, Object value, Object replacement) {
        this.type = type;
        this.value = value;
        this.replacement = replacement;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanSample.class, false, true},
                {ByteSample.class, (byte) 42, (byte) 84},
                {ShortSample.class, (short) 42, (short) 84},
                {CharacterSample.class, (char) 42, (char) 84},
                {IntegerSample.class, 42, 84},
                {LongSample.class, 42L, 84L},
                {FloatSample.class, 42f, 84f},
                {DoubleSample.class, 42d, 84d},
                {ReferenceSample.class, FOO, BAR}
        });
    }

    @Test
    public void testSubstitutionReplacement() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWithConstant(replacement).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is(value));
        assertThat(type.getDeclaredField(BAR).get(instance), is(value));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is(value));
        assertThat(type.getDeclaredField(BAR).get(instance), is(replacement));
    }

    @Test
    public void testSubstitutionChainSimple() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(MemberSubstitution.strict().field(named(FOO)).replaceWithChain(MemberSubstitution.Substitution.Chain.Step.Simple.of(replacement)).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is(value));
        assertThat(type.getDeclaredField(BAR).get(instance), is(value));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is(value));
        assertThat(type.getDeclaredField(BAR).get(instance), is(replacement));
    }

    @Test
    public void testSubstitutionChainReplaceArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(MemberSubstitution.strict().field(named(BAR)).replaceWithChain(
                        MemberSubstitution.Substitution.Chain.Step.ForArgumentSubstitution.of(replacement, 1),
                        MemberSubstitution.Substitution.Chain.Step.OfOriginalExpression.INSTANCE).on(named(RUN)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        assertThat(type.getDeclaredField(FOO).get(instance), is(value));
        assertThat(type.getDeclaredField(BAR).get(instance), is(value));
        assertThat(type.getDeclaredMethod(RUN).invoke(instance), nullValue(Object.class));
        assertThat(type.getDeclaredField(FOO).get(instance), is(value));
        assertThat(type.getDeclaredField(BAR).get(instance), is(replacement));
    }

    public static class BooleanSample {

        public boolean foo = false, bar = false;

        public void run() {
            bar = foo;
        }
    }

    public static class ByteSample {

        public byte foo = 42, bar = 42;

        public void run() {
            bar = foo;
        }
    }

    public static class ShortSample {

        public short foo = 42, bar = 42;

        public void run() {
            bar = foo;
        }
    }

    public static class CharacterSample {

        public char foo = 42, bar = 42;

        public void run() {
            bar = foo;
        }
    }

    public static class IntegerSample {

        public int foo = 42, bar = 42;

        public void run() {
            bar = foo;
        }
    }

    public static class LongSample {

        public long foo = 42, bar = 42;

        public void run() {
            bar = foo;
        }
    }

    public static class FloatSample {

        public float foo = 42, bar = 42;

        public void run() {
            bar = foo;
        }
    }

    public static class DoubleSample {

        public double foo = 42, bar = 42;

        public void run() {
            bar = foo;
        }
    }

    public static class ReferenceSample {

        public String foo = FOO, bar = FOO;

        public void run() {
            bar = foo;
        }
    }
}

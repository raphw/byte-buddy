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
public class AdviceAssignReturnedTypeTest {

    private static final String FOO = "foo";

    private final Class<?> sample,
            skipAdvice,
            noSkipAdvice,
            skipSuppressionAdvice,
            noSkipSuppressionAdvice,
            skipDelegationAdvice,
            noSkipDelegationAdvice,
            skipSuppressionDelegationAdvice,
            noSkipSuppressionDelegationAdvice;

    private final Object skipReturn, noSkipReturn;

    public AdviceAssignReturnedTypeTest(Class<?> sample,
                                        Class<?> skipAdvice,
                                        Class<?> noSkipAdvice,
                                        Class<?> skipSuppressionAdvice,
                                        Class<?> noSkipSuppressionAdvice,
                                        Class<?> skipDelegationAdvice,
                                        Class<?> noSkipDelegationAdvice,
                                        Class<?> skipSuppressionDelegationAdvice,
                                        Class<?> noSkipSuppressionDelegationAdvice,
                                        Object skipReturn,
                                        Object noSkipReturn) {
        this.sample = sample;
        this.skipAdvice = skipAdvice;
        this.noSkipAdvice = noSkipAdvice;
        this.skipSuppressionAdvice = skipSuppressionAdvice;
        this.noSkipSuppressionAdvice = noSkipSuppressionAdvice;
        this.skipDelegationAdvice = skipDelegationAdvice;
        this.noSkipDelegationAdvice = noSkipDelegationAdvice;
        this.skipSuppressionDelegationAdvice = skipSuppressionDelegationAdvice;
        this.noSkipSuppressionDelegationAdvice = noSkipSuppressionDelegationAdvice;
        this.skipReturn = skipReturn;
        this.noSkipReturn = noSkipReturn;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanSample.class,
                        BooleanSkipAdvice.class,
                        BooleanNoSkipAdvice.class,
                        BooleanSkipSuppressAdvice.class,
                        BooleanNoSkipSuppressAdvice.class,
                        BooleanSkipDelegationAdvice.class,
                        BooleanNoSkipDelegationAdvice.class,
                        BooleanSkipSuppressDelegationAdvice.class,
                        BooleanNoSkipSuppressDelegationAdvice.class,
                        true,
                        false},
                {ByteSample.class,
                        ByteSkipAdvice.class,
                        ByteNoSkipAdvice.class,
                        ByteSkipSuppressionAdvice.class,
                        ByteNoSkipSuppressionAdvice.class,
                        ByteSkipDelegationAdvice.class,
                        ByteNoSkipDelegationAdvice.class,
                        ByteSkipSuppressionDelegationAdvice.class,
                        ByteNoSkipSuppressionDelegationAdvice.class,
                        (byte) 1,
                        (byte) 0},
                {ShortSample.class,
                        ShortSkipAdvice.class,
                        ShortNoSkipAdvice.class,
                        ShortSkipSuppressionAdvice.class,
                        ShortNoSkipSuppressionAdvice.class,
                        ShortSkipDelegationAdvice.class,
                        ShortNoSkipDelegationAdvice.class,
                        ShortSkipSuppressionDelegationAdvice.class,
                        ShortNoSkipSuppressionDelegationAdvice.class,
                        (short) 1,
                        (short) 0},
                {CharSample.class,
                        CharSkipAdvice.class,
                        CharNoSkipAdvice.class,
                        CharSkipSuppressionAdvice.class,
                        CharNoSkipSuppressionAdvice.class,
                        CharSkipDelegationAdvice.class,
                        CharNoSkipDelegationAdvice.class,
                        CharSkipSuppressionDelegationAdvice.class,
                        CharNoSkipSuppressionDelegationAdvice.class,
                        (char) 1,
                        (char) 0},
                {IntSample.class,
                        IntSkipAdvice.class,
                        IntNoSkipAdvice.class,
                        IntSkipSuppressionAdvice.class,
                        IntNoSkipSuppressionAdvice.class,
                        IntSkipDelegationAdvice.class,
                        IntNoSkipDelegationAdvice.class,
                        IntSkipSuppressionDelegationAdvice.class,
                        IntNoSkipSuppressionDelegationAdvice.class,
                        1,
                        0},
                {LongSample.class,
                        LongSkipAdvice.class,
                        LongNoSkipAdvice.class,
                        LongSkipSuppressionAdvice.class,
                        LongNoSkipSuppressionAdvice.class,
                        LongSkipDelegationAdvice.class,
                        LongNoSkipDelegationAdvice.class,
                        LongSkipSuppressionDelegationAdvice.class,
                        LongNoSkipSuppressionDelegationAdvice.class,
                        1L,
                        0L},
                {FloatSample.class,
                        FloatSkipAdvice.class,
                        FloatNoSkipAdvice.class,
                        FloatSkipSuppressionAdvice.class,
                        FloatNoSkipSuppressionAdvice.class,
                        FloatSkipDelegationAdvice.class,
                        FloatNoSkipDelegationAdvice.class,
                        FloatSkipSuppressionDelegationAdvice.class,
                        FloatNoSkipSuppressionDelegationAdvice.class,
                        1f,
                        0f},
                {DoubleSample.class,
                        DoubleSkipAdvice.class,
                        DoubleNoSkipAdvice.class,
                        DoubleSkipSuppressionAdvice.class,
                        DoubleNoSkipSuppressionAdvice.class,
                        DoubleSkipDelegationAdvice.class,
                        DoubleNoSkipDelegationAdvice.class,
                        DoubleSkipSuppressionDelegationAdvice.class,
                        DoubleNoSkipSuppressionDelegationAdvice.class,
                        1d,
                        0d},
                {ReferenceSample.class,
                        ReferenceSkipAdvice.class,
                        ReferenceNoSkipAdvice.class,
                        ReferenceSkipSuppressionAdvice.class,
                        ReferenceNoSkipSuppressionAdvice.class,
                        ReferenceSkipDelegationAdvice.class,
                        ReferenceNoSkipDelegationAdvice.class,
                        ReferenceSkipSuppressionDelegationAdvice.class,
                        ReferenceNoSkipSuppressionDelegationAdvice.class,
                        FOO,
                        null}
        });
    }

    @Test
    public void testSkip() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(skipAdvice)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(skipReturn));
    }

    @Test
    public void testNoSkip() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(noSkipAdvice)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(noSkipReturn));
    }

    @Test
    public void testSkipSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(skipSuppressionAdvice)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(skipReturn));
    }

    @Test
    public void testNoSkipSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(noSkipSuppressionAdvice)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(noSkipReturn));
    }

    @Test
    public void testSkipDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(skipDelegationAdvice)
                        .on(named(FOO)))
                .make()
                .load(skipDelegationAdvice.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(skipReturn));
    }

    @Test
    public void testNoSkipDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(noSkipDelegationAdvice)
                        .on(named(FOO)))
                .make()
                .load(noSkipDelegationAdvice.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(noSkipReturn));
    }

    @Test
    public void testSkipSuppressionDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(skipSuppressionDelegationAdvice)
                        .on(named(FOO)))
                .make()
                .load(skipSuppressionDelegationAdvice.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(skipReturn));
    }

    @Test
    public void testNoSkipSuppressionDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(sample)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(noSkipSuppressionDelegationAdvice)
                        .on(named(FOO)))
                .make()
                .load(noSkipSuppressionDelegationAdvice.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO).invoke(type.getConstructor().newInstance()), is(noSkipReturn));
    }

    public static class BooleanSample {

        public boolean foo() {
            return true;
        }
    }

    public static class BooleanSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class BooleanNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class BooleanSkipSuppressAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class BooleanNoSkipSuppressAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class BooleanSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class BooleanNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class BooleanSkipSuppressDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class BooleanNoSkipSuppressDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static boolean enter() {
            return false;
        }
    }

    public static class ByteSample {

        public byte foo() {
            return 1;
        }
    }

    public static class ByteSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ByteNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ByteSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ByteNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ByteSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ByteNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ByteSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ByteNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static byte enter() {
            return 0;
        }
    }

    public static class ShortSample {

        public short foo() {
            return 1;
        }
    }

    public static class ShortSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class ShortNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class ShortSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class ShortNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class ShortSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class ShortNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class ShortSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class ShortNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static short enter() {
            return 0;
        }
    }

    public static class CharSample {

        public char foo() {
            return 1;
        }
    }

    public static class CharSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class CharNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class CharSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class CharNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class CharSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class CharNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class CharSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class CharNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static char enter() {
            return 0;
        }
    }

    public static class IntSample {

        public int foo() {
            return 1;
        }
    }

    public static class IntSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class IntNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class IntSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class IntNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class IntSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class IntNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class IntSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class IntNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static int enter() {
            return 0;
        }
    }

    public static class LongSample {

        public long foo() {
            return 1;
        }
    }

    public static class LongSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class LongNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class LongSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class LongNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class LongSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class LongNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class LongSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class LongNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static long enter() {
            return 0;
        }
    }

    public static class FloatSample {

        public float foo() {
            return 1;
        }
    }

    public static class FloatSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class FloatNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class FloatSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class FloatNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class FloatSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class FloatNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class FloatSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class FloatNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static float enter() {
            return 0;
        }
    }

    public static class DoubleSample {

        public double foo() {
            return 1;
        }
    }

    public static class DoubleSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class DoubleNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class DoubleSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class DoubleNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class DoubleSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class DoubleNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class DoubleSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class DoubleNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static double enter() {
            return 0;
        }
    }

    public static class ReferenceSample {

        public Object foo() {
            return FOO;
        }
    }

    public static class ReferenceSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }

    public static class ReferenceNoSkipAdvice {

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }

    public static class ReferenceSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }

    public static class ReferenceNoSkipSuppressionAdvice {

        @Advice.OnMethodExit(suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }

    public static class ReferenceSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }

    public static class ReferenceNoSkipDelegationAdvice {

        @Advice.OnMethodExit(inline = false)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }

    public static class ReferenceSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }

    public static class ReferenceNoSkipSuppressionDelegationAdvice {

        @Advice.OnMethodExit(inline = false, suppress = Throwable.class)
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        @Advice.AssignReturned.ToReturned
        public static Object enter() {
            return null;
        }
    }
}

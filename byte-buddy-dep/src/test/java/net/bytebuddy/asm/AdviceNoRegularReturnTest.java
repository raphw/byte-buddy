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
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceNoRegularReturnTest {

    private static final String FOO = "foo";

    private final Class<?> type;

    public AdviceNoRegularReturnTest(Class<?> type) {
        this.type = type;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {VoidSample.class},
                {BooleanSample.class},
                {ByteSample.class},
                {ShortSample.class},
                {CharacterSample.class},
                {IntegerSample.class},
                {LongSample.class},
                {FloatSample.class},
                {DoubleSample.class},
                {ReferenceSample.class}
        });
    }

    @Test
    public void testNoRegularReturnWithSkip() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(Advice.to(EnterAdviceSkip.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
    }

    @Test
    public void testNoRegularReturnWithoutHandler() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(Advice.to(ExitAdviceWithoutHandler.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
    }

    @Test
    public void testNoRegularReturnWithHandler() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .visit(Advice.to(ExitAdviceWithHandler.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
    }

    private static class EnterAdviceSkip {

        @Advice.OnMethodEnter(skipIfTrue = true)
        private static boolean enter() {
            return false;
        }
    }

    private static class ExitAdviceWithoutHandler {

        @Advice.OnMethodExit
        private static void exit() {
            /* empty */
        }
    }

    private static class ExitAdviceWithHandler {

        @Advice.OnMethodExit(onThrowable = RuntimeException.class)
        private static void exit() {
            /* empty */
        }
    }

    public static class VoidSample {

        public void foo() {
            throw new RuntimeException();
        }
    }

    public static class BooleanSample {

        public boolean foo() {
            throw new RuntimeException();
        }
    }

    public static class ByteSample {

        public byte foo() {
            throw new RuntimeException();
        }
    }

    public static class ShortSample {

        public short foo() {
            throw new RuntimeException();
        }
    }

    public static class CharacterSample {

        public char foo() {
            throw new RuntimeException();
        }
    }

    public static class IntegerSample {

        public int foo() {
            throw new RuntimeException();
        }
    }

    public static class LongSample {

        public long foo() {
            throw new RuntimeException();
        }
    }

    public static class FloatSample {

        public float foo() {
            throw new RuntimeException();
        }
    }

    public static class DoubleSample {

        public double foo() {
            throw new RuntimeException();
        }
    }

    public static class ReferenceSample {

        public Object foo() {
            throw new RuntimeException();
        }
    }
}

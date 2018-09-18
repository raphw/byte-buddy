package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class CachedReturnPluginTest {

    private static final String FOO = "foo";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {BooleanSample.class, true, boolean.class},
                {ByteSample.class, (byte) 42, byte.class},
                {ShortSample.class, (short) 42, short.class},
                {CharacterSample.class, (char) 42, char.class},
                {IntegerSample.class, 42, int.class},
                {LongSample.class, 42L, long.class},
                {FloatSample.class, 42f, float.class},
                {DoubleSample.class, 42d, double.class},
                {ReferenceSample.class, FOO, Object.class},
                {ReferenceStaticSample.class, FOO, Object.class},
                {ReferenceNamedSample.class, FOO, Object.class}
        });
    }

    private final Class<?> type;

    private final Object value;

    private final Class<?> adviceArgument;

    public CachedReturnPluginTest(Class<?> type, Object value, Class<?> adviceArgument) {
        this.type = type;
        this.value = value;
        this.adviceArgument = adviceArgument;
    }

    private Plugin plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new CachedReturnPlugin();
    }

    @Test
    public void testMatches() throws Exception {
        assertThat(plugin.matches(TypeDescription.ForLoadedType.of(type)), is(true));
    }

    @Test
    public void testCachedValue() throws Exception {
        Class<?> transformed = plugin.apply(new ByteBuddy().redefine(type), TypeDescription.ForLoadedType.of(type), ClassFileLocator.ForClassLoader.of(type.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(transformed.getDeclaredFields().length, is(2));
        Object instance = transformed.getConstructor().newInstance();
        assertThat(transformed.getMethod(FOO).invoke(instance), is(value));
        assertThat(transformed.getMethod(FOO).invoke(instance), is(value));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCannotConstructAdvice() throws Exception {
        Constructor<?> constructor = Class.forName(CachedReturnPlugin.class.getName() + "$" + adviceArgument.getSimpleName(),
                true,
                CachedReturnPlugin.class.getClassLoader()).getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    public static class BooleanSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public boolean foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return true;
        }
    }

    public static class ByteSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public byte foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return 42;
        }
    }

    public static class ShortSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public short foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return 42;
        }
    }

    public static class CharacterSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public char foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return 42;
        }
    }

    public static class IntegerSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public int foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return 42;
        }
    }

    public static class LongSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public long foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return 42;
        }
    }

    public static class FloatSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public float foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return 42f;
        }
    }

    public static class DoubleSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public double foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return 42d;
        }
    }

    public static class ReferenceSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance
        public String foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return FOO;
        }
    }

    public static class ReferenceStaticSample {

        private static boolean executed;

        @CachedReturnPlugin.Enhance
        public static String foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return FOO;
        }
    }

    public static class ReferenceNamedSample {

        private boolean executed;

        @CachedReturnPlugin.Enhance(FOO)
        public String foo() {
            if (executed) {
                throw new AssertionError();
            }
            executed = true;
            return FOO;
        }
    }
}
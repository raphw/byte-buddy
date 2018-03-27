package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class EqualsMethodTest {

    private static final String FOO = "foo", BAR = "bar";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, false, true},
                {byte.class, (byte) 0, (byte) 1},
                {short.class, (short) 0, (short) 1},
                {char.class, (char) 0, (char) 1},
                {int.class, 0, 1},
                {long.class, 0L, 1L},
                {float.class, 0f, 1f},
                {double.class, 0d, 1d},
                {String.class, FOO, BAR},
                {String.class, null, BAR},
                {String.class, FOO, null},
                {boolean[].class, new boolean[]{false}, new boolean[]{true}},
                {byte[].class, new byte[]{0}, new byte[]{1}},
                {short[].class, new short[]{0}, new short[]{1}},
                {char[].class, new char[]{0}, new char[]{1}},
                {int[].class, new int[]{0}, new int[]{1}},
                {long[].class, new long[]{0}, new long[]{1}},
                {float[].class, new float[]{0}, new float[]{1}},
                {double[].class, new double[]{0}, new double[]{1}},
                {String[].class, new String[]{FOO}, new String[]{BAR}},
                {String[].class, null, new String[]{BAR}},
                {String[].class, new String[]{null}, new String[]{BAR}},
                {String[][].class, new String[][]{{FOO}}, new String[][]{{BAR}}},
                {String[][].class, new String[][]{{null}}, new String[][]{{BAR}}},
                {String[][].class, new String[1][], new String[][]{{BAR}}}
        });
    }

    private final Class<?> type;

    private final Object value, alternative;

    public EqualsMethodTest(Class<?> type, Object value, Object alternative) {
        this.type = type;
        this.value = value;
        this.alternative = alternative;
    }

    @Test
    public void testEqualsTrue() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, type, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, value);
        right.getClass().getDeclaredField(FOO).set(right, value);
        assertThat(left, is(right));
    }

    @Test
    public void testEqualsFalse() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, type, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object first = loaded.getLoaded().getDeclaredConstructor().newInstance();
        Object second = loaded.getLoaded().getDeclaredConstructor().newInstance();
        first.getClass().getDeclaredField(FOO).set(first, value);
        second.getClass().getDeclaredField(FOO).set(second, alternative);
        assertThat(first, not(second));
    }
}

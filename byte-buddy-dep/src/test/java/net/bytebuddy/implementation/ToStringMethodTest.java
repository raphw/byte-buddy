package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.DebuggingWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ToStringMethodTest {

    private static final String FOO = "foo", BAR = "bar";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, false, "false"},
                {byte.class, (byte) 0, "0"},
                {short.class, (short) 0, "0"},
                {char.class, (char) 0, String.valueOf((char) 0)},
                {int.class, 0, "0"},
                {long.class, 0L, "0"},
                {float.class, 0f, "0.0"},
                {double.class, 0d, "0.0"},
                {Object.class, FOO, FOO},
                {Object.class, null, "null"},
                {CharSequence.class, null, "null"},
                {CharSequence.class, FOO, FOO},
                {String.class, FOO, FOO},
                {String.class, null, "null"},
                {boolean[].class, new boolean[]{false}, "[false]"},
                {byte[].class, new byte[]{0}, "[0]"},
                {short[].class, new short[]{0}, "[0]"},
                {char[].class, new char[]{0}, "[" + String.valueOf((char) 0) + "]"},
                {int[].class, new int[]{0}, "[0]"},
                {long[].class, new long[]{0}, "[0]"},
                {float[].class, new float[]{0}, "[0.0]"},
                {double[].class, new double[]{0}, "[0.0]"},
                {String[].class, new String[]{FOO}, "[" + FOO + "]"},
                {String[].class, null, "null"},
                {String[].class, new String[]{null}, "[null]"},
                {String[][].class, null, "null"},
                {String[][].class, new String[][]{{FOO}}, "[[" + FOO + "]]"},
                {String[][].class, new String[][]{{null}}, "[[null]]"},
                {String[][].class, new String[1][], "[null]"}
        });
    }

    private final Class<?> type;

    private final Object value;

    private final String string;

    public ToStringMethodTest(Class<?> type, Object value, String string) {
        this.type = type;
        this.value = value;
        this.string = string;
    }

    @Test
    public void testEqualsTrue() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, type, Visibility.PUBLIC)
                .method(isToString())
                .intercept(ToStringMethod.prefixedBy(FOO))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, value);
        assertThat(instance.toString(), is(FOO + "{" + FOO + "=" + string + "}"));
    }
}

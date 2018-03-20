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

import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class HashCodeMethodTest {

    private static final String FOO = "foo", BAR = "bar";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, false, 0},
                {boolean.class, true, 1},
                {byte.class, (byte) 0, 0},
                {byte.class, (byte) 1, 1},
                {short.class, (short) 0, 0},
                {short.class, (short) 1, 1},
                {char.class, (char) 0, 0},
                {char.class, (char) 1, 1},
                {int.class, 0, 0},
                {int.class, 1, 1},
                {long.class, 0L, 0},
                {long.class, 1L, 1},
                {float.class, 0f, 0},
                {float.class, 1f, 1065353216},
                {double.class, 0d, 0},
                {double.class, 1d, 1072693248},
                {Object.class, FOO, FOO.hashCode()},
                {Object.class, BAR, BAR.hashCode()},
                {Object.class, null, 0}
        });
    }

    private final Class<?> type;

    private final Object value;

    private final int hashOffset;

    public HashCodeMethodTest(Class<?> type, Object value, int hashOffset) {
        this.type = type;
        this.value = value;
        this.hashOffset = hashOffset;
    }

    @Test
    public void testHashCode() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, type, Visibility.PUBLIC)
                .method(isHashCode())
                .intercept(HashCodeMethod.usingOffset(0))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, value);
        assertThat(instance.hashCode(), is(hashOffset));
    }
}
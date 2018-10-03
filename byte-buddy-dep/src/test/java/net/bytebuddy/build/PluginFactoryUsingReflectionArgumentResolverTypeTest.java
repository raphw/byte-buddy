package net.bytebuddy.build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class PluginFactoryUsingReflectionArgumentResolverTypeTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {boolean.class, Boolean.class, true},
                {byte.class, Byte.class, (byte) 42},
                {short.class, Short.class, (short) 42},
                {char.class, Character.class, (char) 42},
                {int.class, Integer.class, 42},
                {long.class, Long.class, 42L},
                {float.class, Float.class, 42f},
                {double.class, Double.class, 42d},
                {String.class, String.class, "foo"}
        });
    }

    private final Class<?> type, wrapperType;

    private final Object value;

    public PluginFactoryUsingReflectionArgumentResolverTypeTest(Class<?> type, Class<?> wrapperType, Object value) {
        this.type = type;
        this.wrapperType = wrapperType;
        this.value = value;
    }

    @Test
    public void testCanResolveByTypePrimitive() {
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex(0, value).resolve(0, type);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), is(value));
    }

    @Test
    public void testCanResolveByTypeBoxed() {
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex(0, value).resolve(0, wrapperType);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), is(value));
    }

    @Test
    public void testCanResolveByTypeUnresolved() {
        assertThat(new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex(0, value).resolve(0, Void.class).isResolved(), is(false));
    }

    @Test
    public void testCanResolveByTypePrimitiveDynamic() {
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex.WithDynamicType(0, value.toString()).resolve(0, type);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), is(value));
    }

    @Test
    public void testCanResolveByTypeBoxedDynamic() {
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex.WithDynamicType(0, value.toString()).resolve(0, wrapperType);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), is(value));
    }

    @Test
    public void testCanResolveByTypeBoxedUnresolved() {
        assertThat(new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex.WithDynamicType(0, value.toString()).resolve(0, Void.class).isResolved(), is(false));
    }
}

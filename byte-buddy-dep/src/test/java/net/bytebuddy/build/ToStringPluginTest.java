package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ToStringPluginTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testPluginMatches() throws Exception {
        Plugin plugin = new ToStringPlugin();
        assertThat(plugin.matches(TypeDescription.ForLoadedType.of(SimpleSample.class)), is(true));
        assertThat(plugin.matches(TypeDescription.OBJECT), is(false));
    }

    @Test
    public void testPluginEnhance() throws Exception {
        Class<?> type = new ToStringPlugin()
                .apply(new ByteBuddy().redefine(SimpleSample.class), TypeDescription.ForLoadedType.of(SimpleSample.class), ClassFileLocator.ForClassLoader.of(SimpleSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        type.getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.toString(), is("SimpleSample{foo=foo}"));
    }

    @Test
    public void testPluginEnhanceRedundant() throws Exception {
        Class<?> type = new ToStringPlugin()
                .apply(new ByteBuddy().redefine(RedundantSample.class), TypeDescription.ForLoadedType.of(RedundantSample.class), ClassFileLocator.ForClassLoader.of(RedundantSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredConstructor().newInstance().toString(), is(BAR));
    }

    @Test
    public void testPluginEnhanceIgnore() throws Exception {
        Class<?> type = new ToStringPlugin()
                .apply(new ByteBuddy().redefine(IgnoredFieldSample.class), TypeDescription.ForLoadedType.of(IgnoredFieldSample.class), ClassFileLocator.ForClassLoader.of(IgnoredFieldSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getDeclaredConstructor().newInstance();
        type.getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.toString(), is("IgnoredFieldSample{}"));
    }

    @ToStringPlugin.Enhance
    public static class SimpleSample {

        public String foo;
    }

    @ToStringPlugin.Enhance
    public static class IgnoredFieldSample {

        @ToStringPlugin.Exclude
        public String foo;
    }

    @ToStringPlugin.Enhance
    public static class RedundantSample {

        public String toString() {
            return BAR;
        }
    }
}
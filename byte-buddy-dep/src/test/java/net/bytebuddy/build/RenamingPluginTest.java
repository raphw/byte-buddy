package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class RenamingPluginTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testPluginEnhance() throws Exception {
        Class<?> type = new RenamingPlugin(RenamingPluginTest.class.getSimpleName() + "\\$" + Foo.class.getSimpleName(), RenamingPluginTest.class.getSimpleName() + "\\$" + Bar.class.getSimpleName())
                .apply(new ByteBuddy().redefine(Wrapper.class), TypeDescription.ForLoadedType.of(Wrapper.class), ClassFileLocator.ForClassLoader.of(Wrapper.class.getClassLoader()))
                .make()
                .load(Wrapper.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getField(FOO).getType().getName(), is(Bar.class.getName()));
        assertThat(type.getField(BAR).getType().getName(), is(Void.class.getName()));
    }

    @Test
    public void testPluginNoDuplicates() throws Exception {
        Class<?> type = new RenamingPlugin(new UniqueApplicationRenaming())
                .apply(new ByteBuddy().redefine(Wrapper.class), TypeDescription.ForLoadedType.of(Wrapper.class), ClassFileLocator.ForClassLoader.of(Wrapper.class.getClassLoader()))
                .make()
                .load(Wrapper.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getField(FOO).getType().getName(), is(Foo.class.getName()));
        assertThat(type.getField(BAR).getType().getName(), is(Void.class.getName()));
    }

    @SuppressWarnings("unused")
    public static class Wrapper {

        public Foo foo;

        public Void bar;
    }

    public static class Foo { }

    public static class Bar { }

    private static class UniqueApplicationRenaming implements RenamingPlugin.Renaming {

        private final Set<String> names = new HashSet<String>();

        public String apply(String name) {
            assertThat(names.add(name), is(true));
            return name;
        }
    }
}
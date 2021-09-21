package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DispatcherAnnotationPluginTest {

    @Test
    public void testAnnotationAppending() throws Exception {
        Class<?> transformed = new DispatcherAnnotationPlugin().apply(new ByteBuddy().redefine(Sample.class),
                        TypeDescription.ForLoadedType.of(Sample.class),
                        ClassFileLocator.ForClassLoader.of(Sample.class.getClassLoader()))
                .name(Sample.class.getName() + "$Substitute")
                .make()
                .load(JavaDispatcher.Proxied.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(transformed.getMethod("foo").isAnnotationPresent(JavaDispatcher.Proxied.class), is(true));
        assertThat(transformed.getMethod("foo").getAnnotation(JavaDispatcher.Proxied.class).value(), is("foo"));
        assertThat(transformed.getMethod("bar").isAnnotationPresent(JavaDispatcher.Proxied.class), is(true));
        assertThat(transformed.getMethod("bar").getAnnotation(JavaDispatcher.Proxied.class).value(), is("qux"));
    }

    @JavaDispatcher.Proxied("baz")
    public interface Sample {

        void foo();

        @JavaDispatcher.Proxied("qux")
        void bar();
    }
}

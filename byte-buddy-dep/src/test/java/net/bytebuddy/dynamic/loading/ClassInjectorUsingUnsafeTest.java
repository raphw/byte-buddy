package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ClassFileExtraction;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class ClassInjectorUsingUnsafeTest {

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
    }

    @Test
    public void testUnsafeInjection() throws Exception {
        assertThat(new ClassInjector.UsingUnsafe(classLoader)
                .inject(Collections.singletonMap(new TypeDescription.ForLoadedType(Foo.class), ClassFileExtraction.extract(Foo.class)))
                .get(new TypeDescription.ForLoadedType(Foo.class)), notNullValue(Class.class));
        assertThat(Class.forName(Foo.class.getName(), false, classLoader).getName(), is(Foo.class.getName()));
    }

    @Test
    public void testAvailability() throws Exception {
        assertThat(ClassInjector.UsingUnsafe.isAvailable(), is(true));
        assertThat(new ClassInjector.UsingUnsafe.Dispatcher.Disabled(null).isAvailable(), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void testUnavailableThrowsException() throws Exception {
        new ClassInjector.UsingUnsafe.Dispatcher.Disabled(new RuntimeException()).initialize();
    }

    @Test
    public void testHelperMethods() throws Exception {
        assertThat(ClassInjector.UsingUnsafe.ofBootstrapLoader(), is((ClassInjector) new ClassInjector.UsingUnsafe(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(ClassInjector.UsingUnsafe.ofClassPath(), is((ClassInjector) new ClassInjector.UsingUnsafe(ClassLoader.getSystemClassLoader())));
    }

    private static class Foo {
        /* empty */
    }
}

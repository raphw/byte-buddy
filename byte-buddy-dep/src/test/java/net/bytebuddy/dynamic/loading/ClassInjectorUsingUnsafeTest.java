package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.ClassUnsafeInjectionAvailableRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassInjectorUsingUnsafeTest {

    @Rule
    public MethodRule classUnsafeInjectionAvailableRule = new ClassUnsafeInjectionAvailableRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
    }

    @Test
    @ClassUnsafeInjectionAvailableRule.Enforce
    public void testUnsafeInjection() throws Exception {
        assertThat(new ClassInjector.UsingUnsafe(classLoader)
                .inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(Foo.class), ClassFileLocator.ForClassLoader.read(Foo.class)))
                .get(TypeDescription.ForLoadedType.of(Foo.class)), notNullValue(Class.class));
        assertThat(Class.forName(Foo.class.getName(), false, classLoader).getName(), is(Foo.class.getName()));
    }

    @Test
    @ClassUnsafeInjectionAvailableRule.Enforce
    public void testAvailability() throws Exception {
        assertThat(ClassInjector.UsingUnsafe.isAvailable(), is(true));
        assertThat(new ClassInjector.UsingUnsafe(ClassLoader.getSystemClassLoader()).isAlive(), is(true));
        assertThat(new ClassInjector.UsingUnsafe.Dispatcher.Unavailable(null).isAvailable(), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void testUnavailableThrowsException() throws Exception {
        new ClassInjector.UsingUnsafe.Dispatcher.Unavailable("foo").initialize();
    }

    @Test
    public void testHelperMethods() throws Exception {
        assertThat(ClassInjector.UsingUnsafe.ofBootLoader(), hasPrototype((ClassInjector) new ClassInjector.UsingUnsafe(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(ClassInjector.UsingUnsafe.ofPlatformLoader(), hasPrototype((ClassInjector) new ClassInjector.UsingUnsafe(ClassLoader.getSystemClassLoader().getParent())));
        assertThat(ClassInjector.UsingUnsafe.ofSystemLoader(), hasPrototype((ClassInjector) new ClassInjector.UsingUnsafe(ClassLoader.getSystemClassLoader())));
    }

    private static class Foo {
        /* empty */
    }
}

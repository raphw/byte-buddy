package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.ClassJnaInjectionAvailableRule;
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

public class ClassInjectorUsingJnaTest {

    @Rule
    public MethodRule classJnaInjectionAvailableRule = new ClassJnaInjectionAvailableRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
    }

    @Test
    @ClassJnaInjectionAvailableRule.Enforce
    public void testJnaInjection() throws Exception {
        assertThat(new ClassInjector.UsingJna(classLoader)
                .inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(Foo.class), ClassFileLocator.ForClassLoader.read(Foo.class)))
                .get(TypeDescription.ForLoadedType.of(Foo.class)), notNullValue(Class.class));
        assertThat(Class.forName(Foo.class.getName(), false, classLoader).getName(), is(Foo.class.getName()));
    }

    @Test
    @ClassJnaInjectionAvailableRule.Enforce
    public void testJnaInjectionWithProtectionDomain() throws Exception {
        assertThat(new ClassInjector.UsingJna(classLoader, ClassInjectorUsingJnaTest.class.getProtectionDomain())
                .inject(Collections.singletonMap(TypeDescription.ForLoadedType.of(Foo.class), ClassFileLocator.ForClassLoader.read(Foo.class)))
                .get(TypeDescription.ForLoadedType.of(Foo.class)), notNullValue(Class.class));
        assertThat(Class.forName(Foo.class.getName(), false, classLoader).getName(), is(Foo.class.getName()));
        assertThat(Class.forName(Foo.class.getName(), false, classLoader).getProtectionDomain(), is(ClassInjectorUsingJnaTest.class.getProtectionDomain()));
    }

    @Test
    @ClassJnaInjectionAvailableRule.Enforce
    public void testAvailability() throws Exception {
        assertThat(ClassInjector.UsingJna.isAvailable(), is(true));
        assertThat(new ClassInjector.UsingJna(ClassLoader.getSystemClassLoader()).isAlive(), is(true));
        assertThat(new ClassInjector.UsingJna.Dispatcher.Unavailable(null).isAvailable(), is(false));
    }

    @Test(expected = RuntimeException.class)
    public void testUnavailableThrowsException() throws Exception {
        new ClassInjector.UsingJna.Dispatcher.Unavailable("foo").defineClass(null, null, null, null);
    }

    @Test
    public void testHelperMethods() throws Exception {
        assertThat(ClassInjector.UsingJna.ofBootLoader(), hasPrototype((ClassInjector) new ClassInjector.UsingJna(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(ClassInjector.UsingJna.ofPlatformLoader(), hasPrototype((ClassInjector) new ClassInjector.UsingJna(ClassLoader.getSystemClassLoader().getParent())));
        assertThat(ClassInjector.UsingJna.ofSystemLoader(), hasPrototype((ClassInjector) new ClassInjector.UsingJna(ClassLoader.getSystemClassLoader())));
    }

    private static class Foo {
        /* empty */
    }
}

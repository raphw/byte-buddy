package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ClassInjectorUsingReflectionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassInjector mockInjector;

    @Mock
    private TypeDescription typeDescription;

    private ClassLoader classLoader;

    private ClassInjector classInjector;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null /* null represents the bootstrap class loader */);
        classInjector = new ClassInjector.UsingReflection(classLoader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBootstrapClassLoader() throws Exception {
        new ClassInjector.UsingReflection(null);
    }

    @Test
    public void testInjection() throws Exception {
        classInjector.inject(Collections.<TypeDescription, byte[]>singletonMap(new TypeDescription.ForLoadedType(Foo.class), ClassFileExtraction.extract(Foo.class)));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test(expected = IllegalStateException.class)
    public void testFaultyReflectionStoreClassMethod() throws Exception {
        new ClassInjector.UsingReflection.ReflectionStore.Faulty(new Exception()).getFindLoadedClassMethod();
    }

    @Test(expected = IllegalStateException.class)
    public void testFaultyReflectionStoreLoadByteArray() throws Exception {
        new ClassInjector.UsingReflection.ReflectionStore.Faulty(new Exception()).getLoadByteArrayMethod();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.class).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.ClassLoadingAction.class)
                .ignoreFields("accessControlContext")
                .apply();
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.ReflectionStore.Resolved.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassInjector.UsingReflection.ReflectionStore.Faulty.class).apply();
    }

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }
}

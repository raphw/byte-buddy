package net.bytebuddy.dynamic.loading;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ClassFileExtraction;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
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
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ClassLoaderByteArrayInjectorTest {

    private static final String FOO = "foo";

    private static final byte[] BYTE_ARRAY = new byte[42];

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassLoaderByteArrayInjector mockInjector;

    @Mock
    private TypeDescription typeDescription;

    private ClassLoader classLoader;
    private ClassLoaderByteArrayInjector classLoaderByteArrayInjector;

    @Before
    public void setUp() throws Exception {
        classLoader = new URLClassLoader(new URL[0], null /* null represents the bootstrap class loader */);
        classLoaderByteArrayInjector = new ClassLoaderByteArrayInjector(classLoader);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBootstrapClassLoader() throws Exception {
        new ClassLoaderByteArrayInjector(null);
    }

    @Test
    public void testInjection() throws Exception {
        classLoaderByteArrayInjector.inject(Foo.class.getName(), ClassFileExtraction.extract(Foo.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getClassLoader(), is(classLoader));
    }

    @Test
    public void testInjectionApplication() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO);
        doReturn(Object.class).when(mockInjector).inject(FOO, BYTE_ARRAY);
        Map<TypeDescription, Class<?>> result = ClassLoaderByteArrayInjector.inject(mockInjector, Collections.singletonMap(typeDescription, BYTE_ARRAY));
        assertThat(result.size(), is(1));
        assertThat(result.entrySet().iterator().next().getKey(), is(typeDescription));
        assertEquals(Object.class, result.entrySet().iterator().next().getValue());
        verify(mockInjector).inject(FOO, BYTE_ARRAY);
        verifyNoMoreInteractions(mockInjector);
    }

    @Test(expected = RuntimeException.class)
    public void testFaultyReflectionStoreClassMethod() throws Exception {
        new ClassLoaderByteArrayInjector.ReflectionStore.Faulty(new Exception()).getFindLoadedClassMethod();
    }

    @Test(expected = RuntimeException.class)
    public void testFaultyReflectionStoreLoadByteArray() throws Exception {
        new ClassLoaderByteArrayInjector.ReflectionStore.Faulty(new Exception()).getLoadByteArrayMethod();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassLoaderByteArrayInjector.class)
                .apply(new ClassLoaderByteArrayInjector(mock(ClassLoader.class)));
        ObjectPropertyAssertion.of(ClassLoaderByteArrayInjector.ClassLoadingAction.class)
                .ignoreFields("accessControlContext")
                .apply();
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ClassLoaderByteArrayInjector.ReflectionStore.Resolved.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassLoaderByteArrayInjector.ReflectionStore.Faulty.class).apply();
    }

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }
}

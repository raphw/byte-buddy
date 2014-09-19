package net.bytebuddy.dynamic.loading;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ClassFileExtraction;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
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

    private static class Foo {
        /* Note: Foo is know to the system class loader but not to the bootstrap class loader */
    }
}

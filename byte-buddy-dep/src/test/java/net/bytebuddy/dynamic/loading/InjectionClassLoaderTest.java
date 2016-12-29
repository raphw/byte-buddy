package net.bytebuddy.dynamic.loading;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InjectionClassLoaderTest {

    private static final String FOO = "foo";

    @Test(expected = IllegalArgumentException.class)
    public void testBootrap() throws Exception {
        InjectionClassLoader.Strategy.INSTANCE.load(null, Collections.<TypeDescription, byte[]>emptyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInjection() throws Exception {
        InjectionClassLoader classLoader = mock(InjectionClassLoader.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        byte[] binaryRepresentation = new byte[0];
        when(typeDescription.getName()).thenReturn(FOO);
        when(classLoader.defineClass(FOO, binaryRepresentation)).thenReturn((Class) Object.class);
        assertThat(InjectionClassLoader.Strategy.INSTANCE.load(classLoader, Collections.singletonMap(typeDescription, binaryRepresentation)),
                is(Collections.<TypeDescription, Class<?>>singletonMap(typeDescription, Object.class)));
        verify(classLoader).defineClass(FOO, binaryRepresentation);
        verifyNoMoreInteractions(classLoader);
    }

    @Test(expected = IllegalStateException.class)
    public void testInjectionException() throws Exception {
        InjectionClassLoader classLoader = mock(InjectionClassLoader.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        byte[] binaryRepresentation = new byte[0];
        when(typeDescription.getName()).thenReturn(FOO);
        when(classLoader.defineClass(FOO, binaryRepresentation)).thenThrow(new ClassNotFoundException(FOO));
        InjectionClassLoader.Strategy.INSTANCE.load(classLoader, Collections.singletonMap(typeDescription, binaryRepresentation));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InjectionClassLoader.Strategy.class).apply();
    }
}

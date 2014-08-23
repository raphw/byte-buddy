package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassFileLocatorDefaultTest {

    @Test
    public void testClassFileLocator() throws Exception {
        InputStream inputStream = ClassFileLocator.Default.CLASS_PATH.classFileFor(new TypeDescription.ForLoadedType(getClass()));
        assertThat(inputStream, notNullValue());
        inputStream.close();
    }

    @Test
    public void testAttachedLocator() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        ClassLoader classLoader = new URLClassLoader(new URL[]{getClass().getProtectionDomain().getCodeSource().getLocation()});
        when(typeDescription.getClassLoader()).thenReturn(classLoader);
        when(typeDescription.getInternalName()).thenReturn(getClass().getName().replace('.', '/'));
        InputStream inputStream = ClassFileLocator.Default.ATTACHED.classFileFor(typeDescription);
        assertThat(inputStream, notNullValue());
        inputStream.close();
    }
}

package net.bytebuddy.utility;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JavaModuleTest {

    private static final String FOO = "foo";

    @Test
    public void testSupportsDisabledThrowException() throws Exception {
        assertThat(JavaModule.Dispatcher.Disabled.INSTANCE.isAlive(), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractModule() throws Exception {
        JavaModule.of(mock(Object.class));
    }

    @Test
    public void testUnwrap() throws Exception {
        AnnotatedElement object = mock(AnnotatedElement.class);
        JavaModule module = new JavaModule(object);
        assertThat(module.unwrap(), sameInstance((Object) object));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsNamedDisabledThrowException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.isNamed(mock(Object.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetNameDisabledThrowException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.getName(mock(Object.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetClassLoaderDisabledThrowException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.getClassLoader(mock(Object.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCanReadThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.canRead(mock(Object.class), mock(Object.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetResourceAsStreamThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.getResourceAsStream(mock(Object.class), FOO);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsExportedThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.isExported(mock(Object.class), mock(Object.class), null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsOpenedThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.isOpened(mock(Object.class), mock(Object.class), null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModifyThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.modify(mock(Instrumentation.class),
                mock(Object.class),
                Collections.emptySet(),
                Collections.<String, Set<Object>>emptyMap(),
                Collections.<String, Set<Object>>emptyMap(),
                Collections.<Class<?>>emptySet(),
                Collections.<Class<?>, List<Class<?>>>emptyMap());
    }

    @Test
    public void testDisabledModuleIsNull() throws Exception {
        assertThat(JavaModule.Dispatcher.Disabled.INSTANCE.moduleOf(Object.class), nullValue(JavaModule.class));
    }
}
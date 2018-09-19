package net.bytebuddy.utility;

import org.junit.Test;

import java.lang.instrument.Instrumentation;

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
        Object object = new Object();
        JavaModule module = new JavaModule(object);
        assertThat(module.unwrap(), sameInstance(object));
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
    public void testAddReadsThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.addReads(mock(Instrumentation.class), mock(Object.class), mock(Object.class));
    }

    @Test
    public void testDisabledModuleIsNull() throws Exception {
        assertThat(JavaModule.Dispatcher.Disabled.INSTANCE.moduleOf(Object.class), nullValue(JavaModule.class));
    }
}
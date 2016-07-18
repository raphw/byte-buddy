package net.bytebuddy.utility;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JavaModuleTest {

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

    @Test(expected = IllegalStateException.class)
    public void testIsNamedDisabledThrowException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.isNamed(mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetNameDisabledThrowException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.getName(mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetClassLoaderDisabledThrowException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.getClassLoader(mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testCanReadThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.canRead(mock(Object.class), mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddReadsThrowsException() throws Exception {
        JavaModule.Dispatcher.Disabled.INSTANCE.addReads(mock(Instrumentation.class), mock(Object.class), mock(Object.class));
    }

    @Test
    public void testDisabledModuleIsNull() throws Exception {
        assertThat(JavaModule.Dispatcher.Disabled.INSTANCE.moduleOf(Object.class), nullValue(JavaModule.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(JavaModule.class).skipToString().apply();
        Object object = new Object();
        assertThat(new JavaModule(object).hashCode(), is(object.hashCode()));
        assertThat(new JavaModule(object).toString(), is(object.toString()));
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(JavaModule.Dispatcher.Enabled.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return iterator.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(JavaModule.Dispatcher.Disabled.class).apply();
    }
}
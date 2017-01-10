package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderListenerResubmittingTest {

    @Test
    public void testResubmission() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        AgentBuilder.Listener.Resubmitting resubmitting = new AgentBuilder.Listener.Resubmitting(instrumentation);
        resubmitting.onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        resubmitting.run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verify(instrumentation).retransformClasses(Foo.class);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testResubmissionNonModifiable() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        AgentBuilder.Listener.Resubmitting resubmitting = new AgentBuilder.Listener.Resubmitting(instrumentation);
        resubmitting.onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(false);
        resubmitting.run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testResubmissionCollected() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        ClassLoader classLoader = mock(ClassLoader.class);
        AgentBuilder.Listener.Resubmitting resubmitting = new AgentBuilder.Listener.Resubmitting(instrumentation);
        resubmitting.onError(Foo.class.getName(), classLoader, JavaModule.ofType(Foo.class), false, new Throwable());
        classLoader = null; // Make GC eligible.
        System.gc();
        resubmitting.run();
        verifyZeroInteractions(instrumentation);
    }

    @Test
    public void testResubmissionNonLoadable() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        AgentBuilder.Listener.Resubmitting resubmitting = new AgentBuilder.Listener.Resubmitting(instrumentation);
        resubmitting.onError("foo", Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        resubmitting.run();
        verifyZeroInteractions(instrumentation);
    }

    @Test
    public void testResubmissionAlreadyLoaded() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        AgentBuilder.Listener.Resubmitting resubmitting = new AgentBuilder.Listener.Resubmitting(instrumentation);
        resubmitting.onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), true, new Throwable());
        resubmitting.run();
        verifyZeroInteractions(instrumentation);
    }

    @Test
    public void testResubmissionSeveralTypes() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        AgentBuilder.Listener.Resubmitting resubmitting = new AgentBuilder.Listener.Resubmitting(instrumentation);
        resubmitting.onError(Foo.class.getName(), Foo.class.getClassLoader(), JavaModule.ofType(Foo.class), false, new Throwable());
        resubmitting.onError(Bar.class.getName(), Bar.class.getClassLoader(), JavaModule.ofType(Bar.class), false, new Throwable());
        resubmitting.onError(Object.class.getName(), Object.class.getClassLoader(), JavaModule.ofType(Object.class), false, new Throwable());
        when(instrumentation.isModifiableClass(Foo.class)).thenReturn(true);
        when(instrumentation.isModifiableClass(Bar.class)).thenReturn(true);
        when(instrumentation.isModifiableClass(Object.class)).thenReturn(true);
        resubmitting.run();
        verify(instrumentation).isModifiableClass(Foo.class);
        verify(instrumentation).isModifiableClass(Bar.class);
        verify(instrumentation).isModifiableClass(Object.class);
        verify(instrumentation).retransformClasses((Class<?>[]) anyVararg());
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testLookupKeyBootstrapLoaderReference() throws Exception {
        AgentBuilder.Listener.Resubmitting.LookupKey key = new AgentBuilder.Listener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        assertThat(key.hashCode(), is(0));
        AgentBuilder.Listener.Resubmitting.LookupKey other = new AgentBuilder.Listener.Resubmitting.LookupKey(new URLClassLoader(new URL[0]));
        System.gc();
        assertThat(key, not(is(other)));
        assertThat(key, is(new AgentBuilder.Listener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, is((Object) new AgentBuilder.Listener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, not(is((Object) new AgentBuilder.Listener.Resubmitting.StorageKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testLookupKeyNonBootstrapReference() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        AgentBuilder.Listener.Resubmitting.LookupKey key = new AgentBuilder.Listener.Resubmitting.LookupKey(classLoader);
        assertThat(key, is(new AgentBuilder.Listener.Resubmitting.LookupKey(classLoader)));
        assertThat(key.hashCode(), is(classLoader.hashCode()));
        assertThat(key, not(is(new AgentBuilder.Listener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER))));
        assertThat(key, not(is((Object) new AgentBuilder.Listener.Resubmitting.StorageKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testStorageKeyBootstrapLoaderReference() throws Exception {
        AgentBuilder.Listener.Resubmitting.StorageKey key = new AgentBuilder.Listener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        assertThat(key.isBootstrapLoader(), is(true));
        assertThat(key.hashCode(), is(0));
        assertThat(key.get(), nullValue(ClassLoader.class));
        AgentBuilder.Listener.Resubmitting.StorageKey other = new AgentBuilder.Listener.Resubmitting.StorageKey(new URLClassLoader(new URL[0]));
        System.gc();
        assertThat(other.get(), nullValue(ClassLoader.class));
        assertThat(key, not(is(other)));
        assertThat(key, is(new AgentBuilder.Listener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, is((Object) new AgentBuilder.Listener.Resubmitting.LookupKey(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
        assertThat(key, not(is((Object) new AgentBuilder.Listener.Resubmitting.LookupKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
    }

    @Test
    public void testStorageKeyNonBootstrapReference() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        AgentBuilder.Listener.Resubmitting.StorageKey key = new AgentBuilder.Listener.Resubmitting.StorageKey(classLoader);
        assertThat(key.isBootstrapLoader(), is(false));
        assertThat(key, is(new AgentBuilder.Listener.Resubmitting.StorageKey(classLoader)));
        assertThat(key.hashCode(), is(classLoader.hashCode()));
        assertThat(key.get(), is(classLoader));
        classLoader = null; // Make GC eligible.
        System.gc();
        assertThat(key.get(), nullValue(ClassLoader.class));
        assertThat(key, not(is(new AgentBuilder.Listener.Resubmitting.StorageKey(ClassLoadingStrategy.BOOTSTRAP_LOADER))));
        assertThat(key, not(is((Object) new AgentBuilder.Listener.Resubmitting.LookupKey(new URLClassLoader(new URL[0])))));
        assertThat(key, is(key));
        assertThat(key, not(is(new Object())));
        assertThat(key.isBootstrapLoader(), is(false));
    }

    private static class Foo {
        /* empty */
    }

    private static class Bar {
        /* empty */
    }
}
